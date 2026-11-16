package br.app.divvup.feature.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.casa.FUSO_DA_TEMPORADA
import br.app.divvup.core.domain.casa.RepositorioDeCasa
import br.app.divvup.core.domain.casa.resumoDaTemporada
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.domain.ranking.RepositorioDeRanking
import br.app.divvup.core.domain.ranking.equilibrioDoMes
import br.app.divvup.core.domain.ranking.rankingDoMes
import br.app.divvup.core.domain.ranking.rankingGeral
import br.app.divvup.core.model.Temporada
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Instant
import javax.inject.Inject

private data class LocalDoRanking(
    val aba: AbaDoRanking = AbaDoRanking.Mes,
    val filtro: String? = null,
    val ajuda: Boolean = false,
    val agora: Instant? = null,
)

/** D1 (R-25, R-27, R-28, R-29, R-45). */
@HiltViewModel
class RankingViewModel @Inject constructor(
    ranking: RepositorioDeRanking,
    casas: RepositorioDeCasa,
    conta: RepositorioDeConta,
) : ViewModel() {
    private val local = MutableStateFlow(LocalDoRanking())

    private val base = combine(ranking.moradores, ranking.historico, casas.casaAtual, conta.sessao) { moradores, historico, casa, sessao ->
        Base(moradores, historico, casa?.criadaEm, (sessao as? Sessao.Conectado)?.uid)
    }

    val estado: StateFlow<RankingEstado> = combine(base, local) { b, l ->
        val agora = l.agora ?: Instant.now()
        val ativos = b.moradores.filter { it.ativo }
        RankingEstado(
            aba = l.aba,
            eu = b.eu,
            temporada = resumoDaTemporada(agora.atZone(FUSO_DA_TEMPORADA).toLocalDate()),
            doMes = rankingDoMes(b.moradores),
            geral = rankingGeral(b.moradores),
            equilibrio = equilibrioDoMes(b.moradores),
            casaDesde = b.casaDesde,
            historico = b.historico,
            filtros = ativos.sortedBy { if (it.uid == b.eu) 0 else 1 },
            filtro = l.filtro,
            ajudaAberta = l.ajuda,
            agora = agora,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAR_APOS_MS), RankingEstado())

    fun trocarAba(aba: AbaDoRanking) = local.update { it.copy(aba = aba) }

    fun filtrar(uid: String?) = local.update { it.copy(filtro = uid) }

    fun ajuda(aberta: Boolean) = local.update { it.copy(ajuda = aberta) }

    /** A tela avisa a hora a cada minuto ("há 12 min", "faltam 24 dias"). */
    fun relogio(agora: Instant) = local.update { it.copy(agora = agora) }

    private data class Base(
        val moradores: List<br.app.divvup.core.model.Membro>,
        val historico: List<br.app.divvup.core.model.Evento>,
        val casaDesde: Instant?,
        val eu: String?,
    )

    private companion object {
        const val PARAR_APOS_MS = 5_000L
    }
}

/** D2. */
@HiltViewModel
class TemporadasViewModel @Inject constructor(ranking: RepositorioDeRanking, conta: RepositorioDeConta) : ViewModel() {
    private val aberta = MutableStateFlow<String?>(null)

    val estado: StateFlow<TemporadasEstado> = combine(ranking.temporadas, conta.sessao, aberta) { temporadas, sessao, id ->
        TemporadasEstado(temporadas, (sessao as? Sessao.Conectado)?.uid, id ?: temporadas.firstOrNull()?.id, carregando = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAR_APOS_MS), TemporadasEstado())

    /** Abre um mês e fecha o outro; tocar no aberto fecha. */
    fun alternar(t: Temporada) = aberta.update { atual -> if ((atual ?: estado.value.aberta) == t.id) FECHADA else t.id }

    private companion object {
        const val PARAR_APOS_MS = 5_000L

        /** Marca "nenhuma aberta" (o padrão é a mais recente aberta). */
        const val FECHADA = "-"
    }
}
