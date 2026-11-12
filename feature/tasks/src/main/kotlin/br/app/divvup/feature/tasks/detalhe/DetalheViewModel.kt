package br.app.divvup.feature.tasks.detalhe

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.casa.RepositorioDeConvites
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.domain.tarefa.ErroDeTarefa
import br.app.divvup.core.domain.tarefa.RepositorioDeTarefas
import br.app.divvup.core.domain.tarefa.ResultadoTarefa
import br.app.divvup.core.domain.tarefa.podeEditar
import br.app.divvup.core.domain.tarefa.situacaoParaMim
import br.app.divvup.core.model.Papel
import br.app.divvup.feature.tasks.R
import br.app.divvup.feature.tasks.concluida.ConclusaoFeita
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

private data class LocalDoDetalhe(
    val dialogo: DialogoDoDetalhe? = null,
    val ocupado: Boolean = false,
    @param:StringRes val aviso: Int? = null,
    val agora: Instant? = null,
)

/** C3 (R-12, R-15, R-19, R-21, R-22). */
@HiltViewModel
class DetalheViewModel @Inject constructor(
    salvo: SavedStateHandle,
    private val tarefas: RepositorioDeTarefas,
    convites: RepositorioDeConvites,
    conta: RepositorioDeConta,
) : ViewModel() {
    private val id: String = checkNotNull(salvo[ARG_TAREFA]) { "RotaDetalhe sem tarefaId" }
    private val local = MutableStateFlow(LocalDoDetalhe())
    private var avisoJob: Job? = null
    private val _conclusao = MutableStateFlow<ConclusaoFeita?>(null)

    /** Conclusão que a rota deve abrir em C4. */
    val conclusao: StateFlow<ConclusaoFeita?> = _conclusao.asStateFlow()

    val estado: StateFlow<DetalheEstado> = combine(tarefas.tarefa(id), convites.membros, conta.sessao, local) { tarefa, membros, sessao, l ->
        val eu = (sessao as? Sessao.Conectado)?.uid
        val agora = l.agora ?: Instant.now()
        val souAdmin = membros.any { it.uid == eu && it.papel == Papel.ADMIN }
        DetalheEstado(
            tarefa = tarefa,
            carregando = false,
            situacao = tarefa?.let { situacaoParaMim(it, eu, agora) } ?: DetalheEstado().situacao,
            dono = tarefa?.donoId?.let { d -> membros.firstOrNull { it.uid == d } },
            autor = tarefa?.let { t -> membros.firstOrNull { it.uid == t.criadaPor } },
            podeEditar = tarefa?.let { podeEditar(it, eu, souAdmin) } ?: false,
            souAdmin = souAdmin,
            agora = agora,
            dialogo = l.dialogo,
            ocupado = l.ocupado,
            aviso = l.aviso,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAR_APOS_MS), DetalheEstado())

    /** A tela avisa a cada minuto ("em 3 h", "venceu há 1 h"). */
    fun relogio(agora: Instant) = local.update { it.copy(agora = agora) }

    fun assumir() = executar(R.string.detalhe_assumida) { tarefas.assumir(id) }

    fun concluir() {
        val tarefa = estado.value.tarefa ?: return
        if (local.value.ocupado) return
        local.update { it.copy(ocupado = true) }
        viewModelScope.launch {
            val r = tarefas.concluir(id)
            local.update { it.copy(ocupado = false) }
            when (r) {
                is ResultadoTarefa.Ok -> _conclusao.value = ConclusaoFeita(tarefa, r.valor)
                is ResultadoTarefa.Falha -> avisar(R.string.detalhe_erro)
            }
        }
    }

    fun conclusaoAberta() {
        _conclusao.value = null
    }

    fun pedirDevolucao() = local.update { it.copy(dialogo = DialogoDoDetalhe.Devolver) }

    fun pedirCancelamento() = local.update { it.copy(dialogo = DialogoDoDetalhe.Cancelar) }

    fun fecharDialogo() = local.update { it.copy(dialogo = null) }

    /** Confirma o diálogo aberto: devolver (R-19) ou cancelar (admin, R-12). */
    fun confirmar() = when (local.value.dialogo) {
        DialogoDoDetalhe.Devolver -> executar(R.string.detalhe_devolvida) { tarefas.devolver(id) }
        DialogoDoDetalhe.Cancelar -> executar(R.string.detalhe_cancelada_aviso) { tarefas.cancelar(id) }
        null -> Unit
    }

    private fun executar(@StringRes sucesso: Int, operacao: suspend () -> ResultadoTarefa<Unit>) {
        if (local.value.ocupado) return
        local.update { it.copy(ocupado = true) }
        viewModelScope.launch {
            val r = operacao()
            local.update { it.copy(ocupado = false, dialogo = null) }
            avisar(
                when (r) {
                    is ResultadoTarefa.Ok -> sucesso
                    is ResultadoTarefa.Falha -> mensagemDe(r.erro)
                },
            )
        }
    }

    private fun avisar(@StringRes aviso: Int) {
        avisoJob?.cancel()
        local.update { it.copy(aviso = aviso) }
        avisoJob = viewModelScope.launch {
            delay(DURACAO_DO_AVISO_MS)
            local.update { it.copy(aviso = null) }
        }
    }

    companion object {
        /** Nome do argumento de `RotaDetalhe`. */
        const val ARG_TAREFA = "tarefaId"
        private const val PARAR_APOS_MS = 5_000L
        private const val DURACAO_DO_AVISO_MS = 3_000L

        @StringRes
        fun mensagemDe(erro: ErroDeTarefa): Int = when (erro) {
            ErroDeTarefa.PrazoCurto -> R.string.detalhe_erro_prazo_curto
            ErroDeTarefa.Limite -> R.string.detalhe_erro_limite
            is ErroDeTarefa.JaAssumida -> R.string.detalhe_erro_ja_assumida
            ErroDeTarefa.SemPermissao -> R.string.detalhe_erro_sem_permissao
            else -> R.string.detalhe_erro
        }
    }
}
