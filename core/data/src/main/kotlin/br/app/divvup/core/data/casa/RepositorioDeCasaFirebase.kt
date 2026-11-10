package br.app.divvup.core.data.casa

import br.app.divvup.core.data.backend.ClienteBackend
import br.app.divvup.core.data.backend.ErroDoBackend
import br.app.divvup.core.domain.casa.ErroDeCasa
import br.app.divvup.core.domain.casa.RepositorioDeCasa
import br.app.divvup.core.domain.casa.ResultadoCasa
import br.app.divvup.core.domain.casa.normalizarNomeDaCasa
import br.app.divvup.core.model.Casa
import br.app.divvup.core.model.TipoDeCasa
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositorioDeCasaFirebase @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val backend: ClienteBackend,
) : RepositorioDeCasa {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val casaAtual: Flow<Casa?> = uidAtual()
        .flatMapLatest { uid ->
            if (uid == null) flowOf(null) else observar(firestore.collection("users").document(uid)).map { it?.getString("currentHouseId") }
        }
        .distinctUntilChanged()
        .flatMapLatest { casaId ->
            if (casaId == null) flowOf(null) else observar(firestore.collection("houses").document(casaId)).map { it?.paraCasa() }
        }
        .distinctUntilChanged()

    override suspend fun criarCasa(nome: String, tipo: TipoDeCasa, anunciar: Boolean): ResultadoCasa<String> = try {
        val corpo = buildJsonObject {
            put("nome", JsonPrimitive(normalizarNomeDaCasa(nome)))
            put("tipo", JsonPrimitive(tipo.name))
            put("anunciar", JsonPrimitive(anunciar))
        }
        val resposta = backend.chamar("criarCasa", corpo)
        ResultadoCasa.Ok(checkNotNull(resposta["casaId"]).jsonPrimitive.content)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ResultadoCasa.Falha(erroDeCasa(e))
    }

    private fun uidAtual(): Flow<String?> = callbackFlow {
        val ouvinte = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(ouvinte)
        awaitClose { auth.removeAuthStateListener(ouvinte) }
    }.distinctUntilChanged()

    /** Documento em tempo real; `null` se não existe ou se a leitura é negada (ex.: saiu da casa). */
    private fun observar(ref: DocumentReference): Flow<DocumentSnapshot?> = callbackFlow {
        val registro = ref.addSnapshotListener { doc, erro ->
            trySend(if (erro != null || doc == null || !doc.exists()) null else doc)
        }
        awaitClose { registro.remove() }
    }

    private fun DocumentSnapshot.paraCasa(): Casa? {
        val tipo = getString("type")?.let { t -> TipoDeCasa.entries.firstOrNull { it.name == t } } ?: return null
        return Casa(id = id, nome = getString("name").orEmpty(), tipo = tipo, temporada = getString("currentSeason").orEmpty())
    }
}

/** Traduz a falha do backend (campo `erro`) ou da rede. */
internal fun erroDeCasa(e: Exception): ErroDeCasa = when {
    e is ErroDoBackend && e.codigo == "ja-em-casa" -> ErroDeCasa.JaEmCasa
    e is ErroDoBackend -> ErroDeCasa.Desconhecido
    e is IOException -> ErroDeCasa.SemConexao
    else -> ErroDeCasa.Desconhecido
}
