package br.app.divvup.core.data.conta

import br.app.divvup.core.data.backend.ClienteBackend
import br.app.divvup.core.domain.conta.ErroDeConta
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositorioDeContaFirebase @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val backend: ClienteBackend,
) : RepositorioDeConta {

    override val sessao: Flow<Sessao> = callbackFlow {
        val ouvinte = FirebaseAuth.AuthStateListener { a ->
            trySend(a.currentUser?.let { Sessao.Conectado(it.uid) } ?: Sessao.Desconectado)
        }
        auth.addAuthStateListener(ouvinte)
        awaitClose { auth.removeAuthStateListener(ouvinte) }
    }.distinctUntilChanged()

    override suspend fun entrar(email: String, senha: String): ResultadoConta<Unit> = executar {
        auth.signInWithEmailAndPassword(email.trim(), senha).await()
    }

    override suspend fun criarConta(nome: String, email: String, senha: String): ResultadoConta<Unit> = executar {
        auth.createUserWithEmailAndPassword(email.trim(), senha).await()
        criarPerfil(nome)
    }

    override suspend fun enviarRecuperacao(email: String): ResultadoConta<Unit> = try {
        auth.sendPasswordResetEmail(email.trim()).await()
        ResultadoConta.Ok(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (_: FirebaseAuthInvalidUserException) {
        // A5: mesma confirmação mesmo se a conta não existe (não revelar contas).
        ResultadoConta.Ok(Unit)
    } catch (e: Exception) {
        when (val erro = erroDeConta(e)) {
            ErroDeConta.ContaNaoEncontrada -> ResultadoConta.Ok(Unit)
            else -> ResultadoConta.Falha(erro)
        }
    }

    override suspend fun sair() = auth.signOut()

    override suspend fun perfil(uid: String): ResultadoConta<Usuario?> = executar {
        val doc = firestore.collection("users").document(uid).get().await()
        if (!doc.exists()) {
            null
        } else {
            Usuario(
                uid = uid,
                nome = doc.getString("displayName").orEmpty(),
                email = doc.getString("email"),
                avatarId = doc.getString("avatarId"),
                casaAtualId = doc.getString("currentHouseId"),
            )
        }
    }

    /** Cria `users/{uid}` pelo backend (idempotente). */
    suspend fun criarPerfil(nome: String) {
        backend.chamar("criarPerfil", buildJsonObject { put("nome", JsonPrimitive(nome)) })
    }

    private inline fun <T> executar(bloco: () -> T): ResultadoConta<T> = try {
        ResultadoConta.Ok(bloco())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ResultadoConta.Falha(erroDeConta(e))
    }
}
