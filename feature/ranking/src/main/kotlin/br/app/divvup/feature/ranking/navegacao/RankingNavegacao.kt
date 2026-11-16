package br.app.divvup.feature.ranking.navegacao

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.app.divvup.feature.ranking.RankingScreen
import br.app.divvup.feature.ranking.RankingViewModel
import br.app.divvup.feature.ranking.TemporadasScreen
import br.app.divvup.feature.ranking.TemporadasViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.time.Instant

/** D2. */
@Serializable data object RotaTemporadas

fun NavGraphBuilder.telaTemporadas(nav: NavController) {
    composable<RotaTemporadas> {
        val vm: TemporadasViewModel = hiltViewModel()
        val estado by vm.estado.collectAsStateWithLifecycle()
        TemporadasScreen(estado, aoVoltar = { nav.popBackStack() }, aoAlternar = vm::alternar)
    }
}

/** Conteúdo da aba Ranking (D1) dentro da estrutura principal. */
@Composable
fun AbaRanking(aoAbrirMembro: (String) -> Unit, aoTemporadas: () -> Unit, vm: RankingViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        while (true) {
            vm.relogio(Instant.now())
            delay(UM_MINUTO_MS)
        }
    }
    RankingScreen(
        estado = estado,
        aoTrocarAba = vm::trocarAba,
        aoAbrirMembro = aoAbrirMembro,
        aoTemporadas = aoTemporadas,
        aoFiltrar = vm::filtrar,
        aoAjuda = vm::ajuda,
    )
}

private const val UM_MINUTO_MS = 60_000L
