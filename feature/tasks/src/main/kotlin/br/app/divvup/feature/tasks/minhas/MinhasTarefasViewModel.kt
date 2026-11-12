package br.app.divvup.feature.tasks.minhas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.tarefa.RepositorioDeTarefas
import br.app.divvup.core.domain.tarefa.ResultadoTarefa
import br.app.divvup.core.domain.tarefa.secoesDasMinhas
import br.app.divvup.core.model.Tarefa
import br.app.divvup.feature.tasks.concluida.ConclusaoFeita
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

private data class LocalDasMinhas(val concluidasAbertas: Boolean = true, val concluindo: String? = null, val agora: Instant? = null)

/** C5 (R-20, R-21, R-22). */
@HiltViewModel
class MinhasTarefasViewModel @Inject constructor(private val tarefas: RepositorioDeTarefas) : ViewModel() {
    private val local = MutableStateFlow(LocalDasMinhas())
    private val _conclusao = MutableStateFlow<ConclusaoFeita?>(null)

    /** Conclusão que a rota deve abrir em C4 (consumida com [conclusaoAberta]). */
    val conclusao: StateFlow<ConclusaoFeita?> = _conclusao.asStateFlow()

    val estado: StateFlow<MinhasTarefasEstado> = combine(tarefas.minhas, local) { minhas, l ->
        val agora = l.agora ?: Instant.now()
        MinhasTarefasEstado(secoesDasMinhas(minhas, agora), agora, l.concluidasAbertas, l.concluindo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAR_APOS_MS), MinhasTarefasEstado())

    /** A tela avisa a cada minuto, para "vence em" e as seções andarem com o relógio. */
    fun relogio(agora: Instant) = local.update { it.copy(agora = agora) }

    fun alternarConcluidas() = local.update { it.copy(concluidasAbertas = !it.concluidasAbertas) }

    /** "✓" da linha: conclui pelo backend e abre C4 (R-21). */
    fun concluir(tarefa: Tarefa) {
        if (local.value.concluindo != null) return
        local.update { it.copy(concluindo = tarefa.id) }
        viewModelScope.launch {
            val r = tarefas.concluir(tarefa.id)
            local.update { it.copy(concluindo = null) }
            if (r is ResultadoTarefa.Ok) _conclusao.value = ConclusaoFeita(tarefa, r.valor)
        }
    }

    fun conclusaoAberta() {
        _conclusao.value = null
    }

    private companion object {
        const val PARAR_APOS_MS = 5_000L
    }
}
