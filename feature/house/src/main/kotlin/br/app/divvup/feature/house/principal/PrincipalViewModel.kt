package br.app.divvup.feature.house.principal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.casa.FUSO_DA_TEMPORADA
import br.app.divvup.core.domain.casa.RepositorioDeCasa
import br.app.divvup.core.domain.casa.ResumoDaTemporada
import br.app.divvup.core.domain.casa.resumoDaTemporada
import br.app.divvup.core.model.Casa
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PrincipalViewModel @Inject constructor(casas: RepositorioDeCasa) : ViewModel() {
    val casa: StateFlow<Casa?> = casas.casaAtual.stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAR_APOS_MS), null)

    private val _aba = MutableStateFlow(Aba.Deck)
    val aba: StateFlow<Aba> = _aba.asStateFlow()

    fun aoTrocarAba(nova: Aba) {
        _aba.value = nova
    }

    /** Temporada de hoje no fuso de Brasília. */
    fun temporada(relogio: Clock = Clock.system(FUSO_DA_TEMPORADA)): ResumoDaTemporada = resumoDaTemporada(LocalDate.now(relogio))

    private companion object {
        const val PARAR_APOS_MS = 5_000L
    }
}
