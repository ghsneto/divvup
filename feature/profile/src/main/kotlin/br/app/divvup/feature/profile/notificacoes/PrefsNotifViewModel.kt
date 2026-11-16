package br.app.divvup.feature.profile.notificacoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.notificacao.Preferencias
import br.app.divvup.core.domain.notificacao.RepositorioDeNotificacoes
import br.app.divvup.core.model.TipoDeNotificacao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/** E4 (R-34, R-35): cada mudança salva na hora; falhou, volta ao que estava e avisa. */
@HiltViewModel
class PrefsNotifViewModel @Inject constructor(private val notificacoes: RepositorioDeNotificacoes) : ViewModel() {
    /** Mudança local enquanto o servidor confirma. */
    private val pendente = MutableStateFlow<Preferencias?>(null)
    private val negadas = MutableStateFlow(false)
    private val erro = MutableStateFlow(false)
    private var avisoJob: Job? = null

    val estado: StateFlow<PrefsNotifEstado> = combine(notificacoes.preferencias, pendente, negadas, erro) { salvas, local, neg, err ->
        PrefsNotifEstado(local ?: salvas, neg, err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAR_APOS_MS), PrefsNotifEstado())

    /** A tela confere a permissão do sistema ao abrir e ao voltar das configurações. */
    fun permissao(concedida: Boolean) {
        negadas.value = !concedida
    }

    fun mudarTipos(tipos: List<TipoDeNotificacao>, ligado: Boolean) = mudar { p -> p.copy(tipos = p.tipos + tipos.associateWith { ligado }) }

    fun mudarSilencio(ligado: Boolean) = mudar { it.copy(silencioLigado = ligado) }

    fun mudarInicio(hora: LocalTime) = mudar { it.copy(inicio = hora) }

    fun mudarFim(hora: LocalTime) = mudar { it.copy(fim = hora) }

    private fun mudar(f: (Preferencias) -> Preferencias) {
        viewModelScope.launch {
            val nova = f(estado.value.preferencias)
            pendente.value = nova
            val ok = notificacoes.salvar(nova)
            if (!ok) avisarErro()
            // Confirmado (ou desfeito): passa a valer o que está no Firestore.
            if (ok) notificacoes.preferencias.first { it == nova }
            pendente.update { if (it == nova) null else it }
        }
    }

    private fun avisarErro() {
        avisoJob?.cancel()
        erro.value = true
        avisoJob = viewModelScope.launch {
            delay(DURACAO_DO_AVISO_MS)
            erro.value = false
        }
    }

    private companion object {
        const val PARAR_APOS_MS = 5_000L
        const val DURACAO_DO_AVISO_MS = 3_000L
    }
}
