package br.app.divvup.feature.house.navegacao

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.app.divvup.feature.house.R
import br.app.divvup.feature.house.convites.ConvideScreen
import br.app.divvup.feature.house.convites.ConvideViewModel
import br.app.divvup.feature.house.convites.ConvitesScreen
import br.app.divvup.feature.house.convites.ConvitesViewModel
import br.app.divvup.feature.house.convites.compartilharTexto
import br.app.divvup.feature.house.convites.copiarTexto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** B8. */
@Serializable data object RotaConvide

/** G2. */
@Serializable data object RotaConvites

fun NavGraphBuilder.telasDeConvite(nav: NavController, aoConcluirConvide: () -> Unit) {
    composable<RotaConvide> { ConvideRota(aoConcluirConvide) }
    composable<RotaConvites> { ConvitesRota(nav) }
}

@Composable
private fun ConvideRota(aoDepois: () -> Unit, viewModel: ConvideViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val escopo = rememberCoroutineScope()
    Tique(TIQUE_SEGUNDO_MS, viewModel::tique)
    val codigo = estado.codigo.orEmpty()
    val mensagem = stringResource(R.string.convide_mensagem, estado.nomeDaCasa, codigo)
    ConvideScreen(
        estado = estado,
        aoDepois = aoDepois,
        aoCompartilhar = { compartilharTexto(context, mensagem) },
        aoCopiar = {
            escopo.launch {
                copiarTexto(clipboard, codigo)
                viewModel.avisar(R.string.convide_copiado)
            }
        },
        aoGerarOutro = { viewModel.gerar() },
    )
}

@Composable
private fun ConvitesRota(nav: NavController, viewModel: ConvitesViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val recursos = LocalResources.current
    Tique(TIQUE_MINUTO_MS, viewModel::tique)
    ConvitesScreen(
        estado = estado,
        aoVoltar = { nav.popBackStack() },
        aoGerar = viewModel::gerar,
        aoCompartilhar = { codigo -> compartilharTexto(context, recursos.getString(R.string.convide_mensagem, estado.nomeDaCasa, codigo)) },
        aoRevogar = viewModel::revogar,
    )
}

/** Repete [acao] a cada [intervaloMs] enquanto a tela está na composição. */
@Composable
private fun Tique(intervaloMs: Long, acao: () -> Unit) {
    LaunchedEffect(intervaloMs) {
        while (true) {
            delay(intervaloMs)
            acao()
        }
    }
}

private const val TIQUE_SEGUNDO_MS = 1_000L
private const val TIQUE_MINUTO_MS = 60_000L
