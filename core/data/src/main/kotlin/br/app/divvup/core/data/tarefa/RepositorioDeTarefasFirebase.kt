package br.app.divvup.core.data.tarefa

import br.app.divvup.core.data.backend.ClienteBackend
import br.app.divvup.core.data.backend.ErroDoBackend
import br.app.divvup.core.data.casa.CasaAtualFirebase
import br.app.divvup.core.domain.tarefa.ErroDeTarefa
import br.app.divvup.core.domain.tarefa.EstadoDasTarefas
import br.app.divvup.core.domain.tarefa.FormularioTarefa
import br.app.divvup.core.domain.tarefa.RepositorioDeTarefas
import br.app.divvup.core.domain.tarefa.ResultadoTarefa
import br.app.divvup.core.model.Categoria
import br.app.divvup.core.model.SituacaoDaTarefa
import br.app.divvup.core.model.Tarefa
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositorioDeTarefasFirebase @Inject constructor(
    private val atual: CasaAtualFirebase,
    private val backend: ClienteBackend,
) : RepositorioDeTarefas {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val tarefas: Flow<EstadoDasTarefas> = atual.casaId.flatMapLatest { casaId ->
        if (casaId == null) flowOf(EstadoDasTarefas.Carregadas(emptyList())) else observar(casaId)
    }

    /** Ocorrências em jogo da casa; o cache offline do Firestore entra como `doCache`. */
    private fun observar(casaId: String): Flow<EstadoDasTarefas> = callbackFlow {
        trySend(EstadoDasTarefas.Carregando)
        val registro = atual.firestore.collection("houses").document(casaId).collection("tasks")
            .whereIn("status", EM_JOGO)
            .addSnapshotListener(MetadataChanges.INCLUDE) { q, erro ->
                when {
                    erro != null -> trySend(EstadoDasTarefas.Falhou)
                    q != null -> trySend(EstadoDasTarefas.Carregadas(q.documents.mapNotNull { it.paraTarefa() }, q.metadata.isFromCache))
                }
            }
        awaitClose { registro.remove() }
    }

    override suspend fun criar(formulario: FormularioTarefa): ResultadoTarefa<String> = try {
        val corpo = buildJsonObject {
            put("titulo", JsonPrimitive(formulario.titulo.trim()))
            put("descricao", JsonPrimitive(formulario.descricao.trim()))
            put("categoria", JsonPrimitive(formulario.categoria.name))
            put("peso", JsonPrimitive(formulario.peso.name))
            put("prazo", JsonPrimitive(formulario.prazo.toInstant().toString()))
        }
        ResultadoTarefa.Ok(checkNotNull(backend.chamar("criarTarefa", corpo)["tarefaId"]).jsonPrimitive.content)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ResultadoTarefa.Falha(erroDeTarefa(e))
    }

    private companion object {
        val EM_JOGO = listOf(SituacaoDaTarefa.OPEN.name, SituacaoDaTarefa.ASSIGNED.name, SituacaoDaTarefa.LATE.name)
    }
}

internal fun erroDeTarefa(e: Exception): ErroDeTarefa = when {
    e is ErroDoBackend && e.codigo == "prazo-no-passado" -> ErroDeTarefa.PrazoNoPassado
    e is ErroDoBackend -> ErroDeTarefa.Desconhecido
    e is IOException -> ErroDeTarefa.SemConexao
    else -> ErroDeTarefa.Desconhecido
}

internal fun DocumentSnapshot.paraTarefa(): Tarefa? {
    val categoria = getString("category")?.let { c -> Categoria.entries.firstOrNull { it.name == c } } ?: return null
    val situacao = getString("status")?.let { s -> SituacaoDaTarefa.entries.firstOrNull { it.name == s } } ?: return null
    val prazo = getTimestamp("dueAt")?.toInstant() ?: return null
    return Tarefa(
        id = id,
        titulo = getString("title").orEmpty(),
        descricao = getString("description"),
        categoria = categoria,
        pontos = getLong("points")?.toInt() ?: 0,
        prazo = prazo,
        situacao = situacao,
        donoId = getString("assigneeId"),
        criadaPor = getString("createdBy").orEmpty(),
        criadaPorNome = getString("createdByName").orEmpty(),
        criadaPorAvatar = getString("createdByAvatar"),
    )
}
