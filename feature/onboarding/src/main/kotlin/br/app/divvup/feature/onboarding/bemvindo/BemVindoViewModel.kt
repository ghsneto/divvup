package br.app.divvup.feature.onboarding.bemvindo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.casa.FUSO_DA_TEMPORADA
import br.app.divvup.core.domain.casa.RepositorioDeCasa
import br.app.divvup.core.domain.casa.RepositorioDeConvites
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.Sessao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** B5. Tarefas abertas e miniaturas entram com o deck (#11); por ora a casa nova não tem tarefas. */
@HiltViewModel
class BemVindoViewModel @Inject constructor(
    casas: RepositorioDeCasa,
    convites: RepositorioDeConvites,
    conta: RepositorioDeConta,
) : ViewModel() {
    val estado: StateFlow<BemVindoEstado> = combine(casas.casaAtual, convites.membros, conta.sessao) { casa, membros, sessao ->
        BemVindoEstado(
            casa = casa,
            membros = membros.sortedBy { it.entrouEm },
            eu = (sessao as? Sessao.Conectado)?.uid,
            ultimoDia = YearMonth.from(LocalDate.now(FUSO_DA_TEMPORADA)).lengthOfMonth(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAR_APOS_MS), BemVindoEstado())

    private companion object {
        const val PARAR_APOS_MS = 5_000L
    }
}
