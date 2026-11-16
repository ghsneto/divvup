package br.app.divvup.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.notificacao.RepositorioDeNotificacoes
import br.app.divvup.core.model.Notificacao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** F1 (R-37). */
@HiltViewModel
class AvisosViewModel @Inject constructor(private val notificacoes: RepositorioDeNotificacoes) : ViewModel() {
    val estado: StateFlow<AvisosEstado> = notificacoes.avisos
        .map { AvisosEstado(it, carregando = false, agora = Instant.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAR_APOS_MS), AvisosEstado())

    fun marcarTodas() {
        viewModelScope.launch { notificacoes.marcarTodas() }
    }

    fun abrir(n: Notificacao) {
        if (!n.lida) viewModelScope.launch { notificacoes.marcarLida(n.id) }
    }

    private companion object {
        const val PARAR_APOS_MS = 5_000L
    }
}
