package br.app.divvup.feature.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.data.conexao.MonitorDeConexao
import br.app.divvup.core.domain.casa.RepositorioDeConvites
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.domain.tarefa.ErroDeTarefa
import br.app.divvup.core.domain.tarefa.EstadoDasTarefas
import br.app.divvup.core.domain.tarefa.LIMITE_DE_ABERTAS
import br.app.divvup.core.domain.tarefa.RepositorioDeTarefas
import br.app.divvup.core.domain.tarefa.ResultadoTarefa
import br.app.divvup.core.domain.tarefa.Swipe
import br.app.divvup.core.domain.tarefa.abertasDe
import br.app.divvup.core.domain.tarefa.deckSemPassadas
import br.app.divvup.core.domain.tarefa.passadasAbertas
import br.app.divvup.core.model.Tarefa
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** O que o deck mostra no lugar da pilha. */
enum class SituacaoDoDeck { Carregando, Erro, Vazio, Passadas, Cartoes }

/** Aviso que toma o lugar da faixa de status por 3 s depois de um swipe (C1). */
sealed interface AvisoDoDeck {
    data class Assumida(val pontos: Int) : AvisoDoDeck
    data object Passada : AvisoDoDeck

    /** R-15: outra pessoa levou antes. */
    data class JaPegaram(val porQuem: String) : AvisoDoDeck
    data object Falhou : AvisoDoDeck
}

/** Estado de C1. */
data class DeckEstado(
    val situacao: SituacaoDoDeck = SituacaoDoDeck.Carregando,
    val cartoes: List<Tarefa> = emptyList(),
    /** R-17: passadas que ainda estão abertas (fim do deck: "você passou X tarefas"). */
    val passadas: List<Tarefa> = emptyList(),
    /** Sem conexão: cartões do cache, sem swipe (C1 `DeckOffline`). */
    val offline: Boolean = false,
    val pontosDoMes: Int = 0,
    val sequencia: Int = 0,
    val abertas: Int = 0,
    val agora: Instant = Instant.EPOCH,
    val aviso: AvisoDoDeck? = null,
    /** Pode desfazer o último swipe (R-18). */
    val podeDesfazer: Boolean = false,
) {
    /** R-20: no limite, assumir fica bloqueado (C1 `DeckLimite`). */
    val noLimite: Boolean get() = abertas >= LIMITE_DE_ABERTAS
}

private data class Local(
    /** Cartões que saíram por swipe e ainda não sumiram do Firestore. */
    val ocultas: Set<String> = emptySet(),
    /** Assumidas agora há pouco, para a faixa já contar antes do Firestore. */
    val assumidasAgora: Set<String> = emptySet(),
    val aviso: AvisoDoDeck? = null,
    val ultimo: Pair<String, Swipe>? = null,
    val sequencia: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DeckViewModel @Inject constructor(
    private val tarefas: RepositorioDeTarefas,
    convites: RepositorioDeConvites,
    private val conta: RepositorioDeConta,
    conexao: MonitorDeConexao,
) : ViewModel() {
    private val local = MutableStateFlow(Local())
    private val tentativa = MutableStateFlow(0)
    private var avisoJob: Job? = null

    private val base = combine(tentativa.flatMapLatest { tarefas.tarefas }, tarefas.passes, conta.sessao, conexao.online) { t, passes, sessao, online ->
        Base(t, passes, (sessao as? Sessao.Conectado)?.uid, online)
    }

    val estado: StateFlow<DeckEstado> = combine(base, convites.membros, local) { b, membros, l ->
        val agora = Instant.now()
        val lista = (b.tarefas as? EstadoDasTarefas.Carregadas)?.tarefas.orEmpty()
        val cartoes = deckSemPassadas(lista, b.passes, agora).filterNot { it.id in l.ocultas }
        val passadas = passadasAbertas(lista, b.passes, agora).filterNot { it.id in l.ocultas }
        val minhas = b.eu?.let { abertasDe(it, lista) } ?: 0
        // Assumidas há pouco que o Firestore ainda não mostra como minhas.
        val pendentes = l.assumidasAgora.count { id -> lista.none { it.id == id && it.donoId == b.eu } }
        DeckEstado(
            situacao = when {
                b.tarefas is EstadoDasTarefas.Carregando -> SituacaoDoDeck.Carregando
                b.tarefas is EstadoDasTarefas.Falhou -> SituacaoDoDeck.Erro
                cartoes.isNotEmpty() -> SituacaoDoDeck.Cartoes
                passadas.isNotEmpty() -> SituacaoDoDeck.Passadas
                else -> SituacaoDoDeck.Vazio
            },
            cartoes = cartoes,
            passadas = passadas,
            offline = !b.online,
            pontosDoMes = membros.firstOrNull { it.uid == b.eu }?.pontosDoMes ?: 0,
            sequencia = l.sequencia,
            abertas = minhas + pendentes,
            agora = agora,
            aviso = l.aviso,
            podeDesfazer = l.ultimo != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAR_APOS_MS), DeckEstado())

    init {
        viewModelScope.launch {
            val uid = (conta.sessao.first() as? Sessao.Conectado)?.uid ?: return@launch
            (conta.perfil(uid) as? ResultadoConta.Ok)?.valor?.let { p -> local.update { it.copy(sequencia = p.sequencia) } }
        }
    }

    /** Swipe à direita ou "Assumir" (R-15, R-20). O cartão sai na hora; o servidor confirma. */
    fun assumir(tarefa: Tarefa) {
        val atual = estado.value
        if (atual.offline || atual.noLimite) return
        local.update { it.copy(ocultas = it.ocultas + tarefa.id, assumidasAgora = it.assumidasAgora + tarefa.id) }
        avisar(AvisoDoDeck.Assumida(tarefa.pontos), tarefa.id to Swipe.ASSUMIR)
        viewModelScope.launch {
            when (val r = tarefas.assumir(tarefa.id)) {
                is ResultadoTarefa.Ok -> Unit
                is ResultadoTarefa.Falha -> {
                    local.update { it.copy(assumidasAgora = it.assumidasAgora - tarefa.id) }
                    when (val erro = r.erro) {
                        // Some do deck de qualquer jeito: já é de outra pessoa.
                        is ErroDeTarefa.JaAssumida -> avisar(AvisoDoDeck.JaPegaram(erro.porQuem), null)
                        else -> {
                            local.update { it.copy(ocultas = it.ocultas - tarefa.id) }
                            avisar(AvisoDoDeck.Falhou, null)
                        }
                    }
                }
            }
        }
    }

    /** Swipe à esquerda ou "Passar" (R-16). */
    fun passar(tarefa: Tarefa) {
        if (estado.value.offline) return
        local.update { it.copy(ocultas = it.ocultas + tarefa.id) }
        avisar(AvisoDoDeck.Passada, tarefa.id to Swipe.PASSAR)
        viewModelScope.launch {
            if (tarefas.passar(tarefa.id) is ResultadoTarefa.Falha) {
                local.update { it.copy(ocultas = it.ocultas - tarefa.id) }
                avisar(AvisoDoDeck.Falhou, null)
            }
        }
    }

    /** "Desfazer" (R-18): o cartão volta para o topo do deck. */
    fun desfazer() {
        val (id, acao) = local.value.ultimo ?: return
        avisoJob?.cancel()
        local.update { it.copy(ocultas = it.ocultas - id, assumidasAgora = it.assumidasAgora - id, aviso = null, ultimo = null) }
        viewModelScope.launch {
            if (tarefas.desfazer(id, acao) is ResultadoTarefa.Falha) avisar(AvisoDoDeck.Falhou, null)
        }
    }

    /** "Rever" (R-17). */
    fun rever() {
        viewModelScope.launch {
            if (tarefas.reverPassadas() is ResultadoTarefa.Falha) avisar(AvisoDoDeck.Falhou, null)
        }
    }

    /** "Tentar de novo" (C1 `DeckErro`): escuta as tarefas outra vez. */
    fun tentarDeNovo() {
        tentativa.value++
    }

    private fun avisar(aviso: AvisoDoDeck, ultimo: Pair<String, Swipe>?) {
        avisoJob?.cancel()
        local.update { it.copy(aviso = aviso, ultimo = ultimo) }
        avisoJob = viewModelScope.launch {
            delay(DURACAO_DO_AVISO_MS)
            local.update { it.copy(aviso = null, ultimo = null) }
        }
    }

    private data class Base(val tarefas: EstadoDasTarefas, val passes: Map<String, Instant>, val eu: String?, val online: Boolean)

    private companion object {
        const val PARAR_APOS_MS = 5_000L

        /** R-18: "Desfazer" por 3 s. */
        const val DURACAO_DO_AVISO_MS = 3_000L
    }
}
