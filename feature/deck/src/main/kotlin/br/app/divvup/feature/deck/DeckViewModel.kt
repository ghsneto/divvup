package br.app.divvup.feature.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.data.conexao.MonitorDeConexao
import br.app.divvup.core.domain.casa.RepositorioDeConvites
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.domain.tarefa.EstadoDasTarefas
import br.app.divvup.core.domain.tarefa.RepositorioDeTarefas
import br.app.divvup.core.domain.tarefa.abertasDe
import br.app.divvup.core.domain.tarefa.ordenarDeck
import br.app.divvup.core.model.Tarefa
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** O que o deck mostra no lugar da pilha. */
enum class SituacaoDoDeck { Carregando, Erro, Vazio, Cartoes }

/** Estado de C1. */
data class DeckEstado(
    val situacao: SituacaoDoDeck = SituacaoDoDeck.Carregando,
    val cartoes: List<Tarefa> = emptyList(),
    /** Sem conexão: cartões do cache, sem swipe (C1 `DeckOffline`). */
    val offline: Boolean = false,
    val pontosDoMes: Int = 0,
    val sequencia: Int = 0,
    val abertas: Int = 0,
    val agora: Instant = Instant.EPOCH,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class DeckViewModel @Inject constructor(
    private val tarefas: RepositorioDeTarefas,
    convites: RepositorioDeConvites,
    private val conta: RepositorioDeConta,
    conexao: MonitorDeConexao,
) : ViewModel() {
    private val sequencia = MutableStateFlow(0)
    private val tentativa = MutableStateFlow(0)

    val estado: StateFlow<DeckEstado> =
        combine(tentativa.flatMapLatest { tarefas.tarefas }, convites.membros, conta.sessao, conexao.online, sequencia) { t, membros, sessao, online, seq ->
            val eu = (sessao as? Sessao.Conectado)?.uid
            val lista = (t as? EstadoDasTarefas.Carregadas)?.tarefas.orEmpty()
            val cartoes = ordenarDeck(lista)
            DeckEstado(
                situacao = when {
                    t is EstadoDasTarefas.Carregando -> SituacaoDoDeck.Carregando
                    t is EstadoDasTarefas.Falhou -> SituacaoDoDeck.Erro
                    cartoes.isEmpty() -> SituacaoDoDeck.Vazio
                    else -> SituacaoDoDeck.Cartoes
                },
                cartoes = cartoes,
                offline = !online,
                pontosDoMes = membros.firstOrNull { it.uid == eu }?.pontosDoMes ?: 0,
                sequencia = seq,
                abertas = eu?.let { abertasDe(it, lista) } ?: 0,
                agora = Instant.now(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAR_APOS_MS), DeckEstado())

    init {
        viewModelScope.launch {
            val uid = (conta.sessao.first() as? Sessao.Conectado)?.uid ?: return@launch
            (conta.perfil(uid) as? ResultadoConta.Ok)?.valor?.let { sequencia.value = it.sequencia }
        }
    }

    /** "Tentar de novo" (C1 `DeckErro`): escuta as tarefas outra vez. */
    fun tentarDeNovo() {
        tentativa.value++
    }

    private companion object {
        const val PARAR_APOS_MS = 5_000L
    }
}
