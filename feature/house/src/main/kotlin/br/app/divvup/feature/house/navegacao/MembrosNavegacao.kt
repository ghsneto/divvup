package br.app.divvup.feature.house.navegacao

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.app.divvup.feature.house.membros.EditarCasaScreen
import br.app.divvup.feature.house.membros.EditarCasaViewModel
import br.app.divvup.feature.house.membros.MembroScreen
import br.app.divvup.feature.house.membros.MembroViewModel
import br.app.divvup.feature.house.membros.MinhaCasaScreen
import br.app.divvup.feature.house.membros.MinhaCasaViewModel
import kotlinx.serialization.Serializable

/** G1. */
@Serializable data object RotaMinhaCasa

/** D3/G3. */
@Serializable data class RotaMembro(val uid: String)

/** G4. */
@Serializable data object RotaEditarCasa

/** G1, D3/G3 e G4. [aoSairDaCasa] leva para B2 depois que a pessoa sai. */
fun NavGraphBuilder.telasDeMembros(nav: NavController, aoSairDaCasa: () -> Unit) {
    composable<RotaMinhaCasa> {
        val vm: MinhaCasaViewModel = hiltViewModel()
        val estado by vm.estado.collectAsStateWithLifecycle()
        val saiu by vm.saiu.collectAsStateWithLifecycle()
        LaunchedEffect(saiu) { if (saiu) aoSairDaCasa() }
        MinhaCasaScreen(
            estado = estado,
            aoVoltar = { nav.popBackStack() },
            aoConvidar = { nav.navigate(RotaConvites) },
            aoEditar = { nav.navigate(RotaEditarCasa) },
            aoAbrirMembro = { nav.navigate(RotaMembro(it)) },
            aoPedirSaida = vm::pedirSaida,
            aoCancelarSaida = vm::cancelarSaida,
            aoSair = vm::sair,
        )
    }
    composable<RotaMembro> { MembroRota(nav) }
    composable<RotaEditarCasa> { EditarRota(nav) }
}

@Composable
private fun MembroRota(nav: NavController, vm: MembroViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val removido by vm.removido.collectAsStateWithLifecycle()
    LaunchedEffect(removido) { if (removido) nav.popBackStack() }
    MembroScreen(
        estado = estado,
        aoVoltar = { nav.popBackStack() },
        aoAlternarAdmin = vm::alternarAdmin,
        aoPedirRemocao = vm::pedirRemocao,
        aoCancelarRemocao = vm::cancelarRemocao,
        aoRemover = vm::remover,
    )
}

@Composable
private fun EditarRota(nav: NavController, vm: EditarCasaViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val salvo by vm.salvo.collectAsStateWithLifecycle()
    LaunchedEffect(salvo) { if (salvo) nav.popBackStack() }
    EditarCasaScreen(estado, aoVoltar = { nav.popBackStack() }, aoMudar = vm::aoMudar, aoSalvar = vm::salvar)
}
