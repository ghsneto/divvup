package br.app.divvup.feature.profile.navegacao

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.app.divvup.feature.profile.avatar.GaleriaArgs
import br.app.divvup.feature.profile.avatar.GaleriaDeAvataresScreen
import br.app.divvup.feature.profile.avatar.GaleriaViewModel
import kotlinx.serialization.Serializable

/** E3. `soEscolher` = modo onboarding (devolve a escolha sem gravar). */
@Serializable
data class RotaGaleria(val avatarInicial: String? = null, val soEscolher: Boolean = false)

fun NavGraphBuilder.telaGaleria(nav: NavController) {
    composable<RotaGaleria> { GaleriaRota(nav) }
}

@Composable
private fun GaleriaRota(nav: NavController, viewModel: GaleriaViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val concluido by viewModel.concluido.collectAsStateWithLifecycle()
    LaunchedEffect(concluido) {
        concluido?.let {
            nav.previousBackStackEntry?.savedStateHandle?.set(GaleriaArgs.RESULTADO, it)
            nav.popBackStack()
        }
    }
    GaleriaDeAvataresScreen(estado, viewModel::escolher, viewModel::salvar, aoVoltar = { nav.popBackStack() })
}
