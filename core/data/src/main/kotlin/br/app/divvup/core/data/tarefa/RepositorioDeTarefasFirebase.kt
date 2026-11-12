package br.app.divvup.core.data.tarefa

import br.app.divvup.core.data.backend.ClienteBackend
import br.app.divvup.core.data.backend.ErroDoBackend
import br.app.divvup.core.data.casa.CasaAtualFirebase
import br.app.divvup.core.domain.tarefa.ErroDeTarefa
import br.app.divvup.core.domain.tarefa.EstadoDasTarefas
import br.app.divvup.core.domain.tarefa.FormularioTarefa
import br.app.divvup.core.domain.tarefa.RepositorioDeTarefas
import br.app.divvup.core.domain.tarefa.ResultadoDaConclusao
import br.app.divvup.core.domain.tarefa.ResultadoTarefa
import br.app.divvup.core.domain.tarefa.Swipe
import br.app.divvup.core.data.casa.observar
import br.app.divvup.core.model.Categoria
import br.app.divvup.core.model.SituacaoDaTarefa
import br.app.divvup.core.model.Tarefa
import br.app.divvup.core.model.ViaDeAtribuicao
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.time.Instant
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
        val corpo = buildJsonObject { campos(formulario) }
        ResultadoTarefa.Ok(checkNotNull(backend.chamar("criarTarefa", corpo)["tarefaId"]).jsonPrimitive.content)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ResultadoTarefa.Falha(erroDeTarefa(e))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val passes: Flow<Map<String, Instant>> = combine(atual.uid, atual.casaId) { uid, casa -> uid to casa }
        .flatMapLatest { (uid, casa) ->
            if (uid == null || casa == null) {
                flowOf(emptyMap())
            } else {
                observar(atual.firestore.collection("houses").document(casa).collection("members").document(uid).collection("passes"))
                    .map { q -> q?.documents.orEmpty().mapNotNull { d -> d.getTimestamp("expiresAt")?.toInstant()?.let { d.id to it } }.toMap() }
            }
        }

    override suspend fun assumir(tarefaId: String): ResultadoTarefa<Unit> = operacao("assumirTarefa", tarefaId)

    override suspend fun passar(tarefaId: String): ResultadoTarefa<Unit> = operacao("passarTarefa", tarefaId)

    override suspend fun desfazer(tarefaId: String, acao: Swipe): ResultadoTarefa<Unit> = operacao("desfazer", tarefaId) {
        put("acao", JsonPrimitive(acao.name))
    }

    override suspend fun reverPassadas(): ResultadoTarefa<Unit> = operacao("reverPassadas", null)

    private suspend fun operacao(
        nome: String,
        tarefaId: String?,
        extra: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit = {},
    ): ResultadoTarefa<Unit> = try {
        backend.chamar(nome, buildJsonObject { tarefaId?.let { put("tarefaId", JsonPrimitive(it)) }; extra() })
        ResultadoTarefa.Ok(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ResultadoTarefa.Falha(erroDeTarefa(e))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val minhas: Flow<List<Tarefa>> = combine(atual.uid, atual.casaId) { uid, casa -> uid to casa }
        .flatMapLatest { (uid, casa) ->
            if (uid == null || casa == null) {
                flowOf(emptyList())
            } else {
                observar(atual.firestore.collection("houses").document(casa).collection("tasks").whereEqualTo("assigneeId", uid))
                    .map { q -> q?.documents.orEmpty().mapNotNull { it.paraTarefa() } }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun tarefa(id: String): Flow<Tarefa?> = atual.casaId.flatMapLatest { casa ->
        if (casa == null) flowOf(null) else observar(atual.firestore.collection("houses").document(casa).collection("tasks").document(id)).map { it?.paraTarefa() }
    }

    override suspend fun concluir(id: String): ResultadoTarefa<ResultadoDaConclusao> = try {
        val r = backend.chamar("concluirTarefa", buildJsonObject { put("tarefaId", JsonPrimitive(id)) })
        fun inteiro(c: String) = checkNotNull(r[c]).jsonPrimitive.int
        ResultadoTarefa.Ok(
            ResultadoDaConclusao(
                pontos = inteiro("pontos"),
                noPrazo = checkNotNull(r["noPrazo"]).jsonPrimitive.boolean,
                posicaoAntes = inteiro("posicaoAntes"),
                posicaoDepois = inteiro("posicaoDepois"),
                ultrapassou = r["ultrapassou"]?.jsonPrimitive?.contentOrNull,
                sequencia = inteiro("sequencia"),
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ResultadoTarefa.Falha(erroDeTarefa(e))
    }

    override suspend fun devolver(id: String): ResultadoTarefa<Unit> = operacao("devolverTarefa", id)

    override suspend fun cancelar(id: String): ResultadoTarefa<Unit> = operacao("cancelarTarefa", id)

    override suspend fun editar(id: String, formulario: FormularioTarefa): ResultadoTarefa<Unit> = operacao("editarTarefa", id) { campos(formulario) }

    override suspend fun excluir(id: String): ResultadoTarefa<Unit> = operacao("excluirTarefa", id)

    private fun kotlinx.serialization.json.JsonObjectBuilder.campos(formulario: FormularioTarefa) {
        put("titulo", JsonPrimitive(formulario.titulo.trim()))
        put("descricao", JsonPrimitive(formulario.descricao.trim()))
        put("categoria", JsonPrimitive(formulario.categoria.name))
        put("peso", JsonPrimitive(formulario.peso.name))
        put("prazo", JsonPrimitive(formulario.prazo.toInstant().toString()))
    }

    private companion object {
        val EM_JOGO = listOf(SituacaoDaTarefa.OPEN.name, SituacaoDaTarefa.ASSIGNED.name, SituacaoDaTarefa.LATE.name)
    }
}

internal fun erroDeTarefa(e: Exception): ErroDeTarefa = when {
    e is ErroDoBackend && e.codigo == "prazo-no-passado" -> ErroDeTarefa.PrazoNoPassado
    e is ErroDoBackend && e.codigo == "ja-assumida" -> ErroDeTarefa.JaAssumida(e.detalhe.orEmpty())
    e is ErroDoBackend && e.codigo == "limite" -> ErroDeTarefa.Limite
    e is ErroDoBackend && e.codigo == "nao-da-para-desfazer" -> ErroDeTarefa.NaoDaParaDesfazer
    e is ErroDoBackend && e.codigo == "prazo-curto" -> ErroDeTarefa.PrazoCurto
    e is ErroDoBackend && e.codigo in setOf("nao-e-sua", "sem-permissao", "tem-dono", "nao-admin") -> ErroDeTarefa.SemPermissao
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
        criadaEm = getTimestamp("createdAt")?.toInstant(),
        via = getString("assignedVia")?.let { v -> ViaDeAtribuicao.entries.firstOrNull { it.name == v } },
        concluidaEm = getTimestamp("completedAt")?.toInstant(),
        pontosGanhos = getLong("pointsAwarded")?.toInt(),
    )
}
