package br.app.divvup.core.data.notificacao

import br.app.divvup.core.data.backend.ClienteBackend
import br.app.divvup.core.data.casa.CasaAtualFirebase
import br.app.divvup.core.data.casa.observar
import br.app.divvup.core.domain.notificacao.RepositorioDeNotificacoes
import br.app.divvup.core.domain.notificacao.VALIDADE_DOS_AVISOS
import br.app.divvup.core.model.Notificacao
import br.app.divvup.core.model.TipoDeNotificacao
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import br.app.divvup.core.domain.notificacao.Preferencias
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/** F1: lê `users/{uid}/notifications` em tempo real; só o campo `read` é gravado pelo app. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class RepositorioDeNotificacoesFirebase @Inject constructor(
    private val atual: CasaAtualFirebase,
    private val backend: ClienteBackend,
    private val app: FirebaseApp,
) : RepositorioDeNotificacoes {

    private fun caixa(uid: String) = atual.firestore.collection("users").document(uid).collection("notifications")

    override val avisos: Flow<List<Notificacao>> = atual.uid.flatMapLatest { uid ->
        if (uid == null) {
            flowOf(emptyList())
        } else {
            val desde = Timestamp(Date.from(Instant.now().minus(VALIDADE_DOS_AVISOS)))
            observar(caixa(uid).whereGreaterThan("createdAt", desde).orderBy("createdAt", Query.Direction.DESCENDING).limit(LIMITE))
                .map { q -> q?.documents.orEmpty().mapNotNull { it.paraNotificacao() } }
        }
    }

    override suspend fun marcarLida(id: String) {
        val uid = atual.uid.first() ?: return
        runCatching { caixa(uid).document(id).update("read", true).await() }
    }

    override suspend fun marcarTodas() {
        val uid = atual.uid.first() ?: return
        val naoLidas = avisos.first().filterNot { it.lida }
        if (naoLidas.isEmpty()) return
        runCatching {
            val lote = atual.firestore.batch()
            naoLidas.forEach { lote.update(caixa(uid).document(it.id), "read", true) }
            lote.commit().await()
        }
    }

    // `getToken()` aparece como obsoleto no 26.x, mas segue sendo o jeito de obter o token FCM.
    @Suppress("DEPRECATION")
    override suspend fun registrarAparelho(token: String?) {
        try {
            val t = token ?: app.get(FirebaseMessaging::class.java).token.await()
            backend.chamar("registrarToken", buildJsonObject { put("token", JsonPrimitive(t)) })
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Sem rede ou sem Google Play: tenta de novo na próxima abertura.
        }
    }

    override val preferencias: Flow<Preferencias> = atual.uid.flatMapLatest { uid ->
        if (uid == null) {
            flowOf(Preferencias())
        } else {
            observar(atual.firestore.collection("users").document(uid)).map { doc ->
                val tipos = (doc?.get("notifPrefs") as? Map<*, *>).orEmpty().mapNotNull { (k, v) ->
                    val tipo = TipoDeNotificacao.entries.firstOrNull { it.name == k } ?: return@mapNotNull null
                    tipo to (v as? Boolean ?: return@mapNotNull null)
                }.toMap()
                val padrao = Preferencias()
                Preferencias(
                    tipos = tipos,
                    silencioLigado = doc?.getBoolean("quietHours.enabled") ?: padrao.silencioLigado,
                    inicio = doc?.getString("quietHours.start")?.let(::horario) ?: padrao.inicio,
                    fim = doc?.getString("quietHours.end")?.let(::horario) ?: padrao.fim,
                )
            }
        }
    }

    override suspend fun salvar(preferencias: Preferencias): Boolean = try {
        backend.chamar(
            "salvarPreferencias",
            buildJsonObject {
                put("tipos", buildJsonObject { TipoDeNotificacao.entries.forEach { put(it.name, JsonPrimitive(preferencias.ligado(it))) } })
                put(
                    "silencio",
                    buildJsonObject {
                        put("ligado", JsonPrimitive(preferencias.silencioLigado))
                        put("inicio", JsonPrimitive(preferencias.inicio.format(HHMM)))
                        put("fim", JsonPrimitive(preferencias.fim.format(HHMM)))
                    },
                )
            },
        )
        true
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }

    private companion object {
        const val LIMITE = 100L
        val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

private fun DocumentSnapshot.paraNotificacao(): Notificacao? {
    val tipo = TipoDeNotificacao.entries.firstOrNull { it.name == getString("type") } ?: return null
    return Notificacao(
        id = id,
        tipo = tipo,
        titulo = getString("title").orEmpty(),
        corpo = getString("body").orEmpty(),
        lida = getBoolean("read") == true,
        quando = getTimestamp("createdAt")?.toDate()?.toInstant() ?: return null,
        tarefaId = getString("refs.taskId"),
    )
}

private fun horario(texto: String): LocalTime? = runCatching { LocalTime.parse(texto) }.getOrNull()
