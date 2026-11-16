package br.app.divvup.feature.house.principal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.casa.FUSO_DA_TEMPORADA
import br.app.divvup.core.domain.casa.RepositorioDeCasa
import br.app.divvup.core.domain.casa.ResumoDaTemporada
import br.app.divvup.core.domain.casa.resumoDaTemporada
import br.app.divvup.core.model.Casa
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import br.app.divvup.core.domain.notificacao.RepositorioDeNotificacoes
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PrincipalViewModel @Inject constructor(
    casas: RepositorioDeCasa,
    private val salvo: SavedStateHandle,
    notificacoes: RepositorioDeNotificacoes,
) : ViewModel() {
    val casa: StateFlow<Casa?> = casas.casaAtual.stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAR_APOS_MS), null)

    /** Aba atual no `SavedStateHandle` da rota, para outras telas (ex.: C4) escolherem a aba ao voltar. */
    val aba: StateFlow<Aba> = salvo.getStateFlow(ARG_ABA, Aba.Deck.name)
        .map { Aba.valueOf(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Aba.valueOf(salvo[ARG_ABA] ?: Aba.Deck.name))

    /** Contador do sino (R-37). */
    val naoLidas: StateFlow<Int> = notificacoes.avisos.map { l -> l.count { !it.lida } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAR_APOS_MS), 0)

    init {
        // R-36: com casa, o aparelho passa a receber push (o token pode ter mudado).
        viewModelScope.launch { notificacoes.registrarAparelho() }
    }

    fun aoTrocarAba(nova: Aba) {
        salvo[ARG_ABA] = nova.name
    }

    /** Temporada de hoje no fuso de Brasília. */
    fun temporada(relogio: Clock = Clock.system(FUSO_DA_TEMPORADA)): ResumoDaTemporada = resumoDaTemporada(LocalDate.now(relogio))

    companion object {
        /** Chave da aba no `SavedStateHandle` de `RotaPrincipal`. */
        const val ARG_ABA = "aba"
        private const val PARAR_APOS_MS = 5_000L
    }
}
