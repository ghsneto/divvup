package br.app.divvup.feature.onboarding.navegacao

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.compose.ui.platform.LocalClipboard
import br.app.divvup.feature.onboarding.bemvindo.BemVindoScreen
import br.app.divvup.feature.onboarding.bemvindo.BemVindoViewModel
import br.app.divvup.feature.onboarding.codigo.CodigoScreen
import br.app.divvup.feature.onboarding.codigo.CodigoViewModel
import br.app.divvup.feature.onboarding.casa.CriarCasaScreen
import br.app.divvup.feature.onboarding.casa.CriarCasaViewModel
import br.app.divvup.feature.onboarding.casa.SuaCasaScreen
import br.app.divvup.feature.onboarding.casa.SuaCasaViewModel
import br.app.divvup.feature.onboarding.nome.NomeScreen
import br.app.divvup.feature.onboarding.nome.NomeViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** B1. */
@Serializable data object RotaNome

/** B2. */
@Serializable data object RotaSuaCasa

/** B3. */
@Serializable data object RotaCriarCasa

/** B4. */
@Serializable data object RotaCodigo

/** B5. */
@Serializable data object RotaBemVindo

/** Chave em que E3 devolve o avatar escolhido (igual a `GaleriaArgs.RESULTADO`). */
const val RESULTADO_AVATAR = "avatarEscolhido"

class OnboardingAcoes(
    /** Abre E3 em modo onboarding com o avatar atual. */
    val aoAbrirGaleria: (avatarAtual: String) -> Unit,
    /** B2 "Sair da conta": a sessão já foi encerrada. */
    val aoSair: () -> Unit,
    /** Casa criada (B3): segue para "chama a galera" (B8). */
    val aoCriarCasa: () -> Unit,
    /** Entrou numa casa (B5 "Bora") ou já tinha casa: segue para a estrutura principal. */
    val aoTerCasa: () -> Unit,
)

fun NavGraphBuilder.grafoDeOnboarding(nav: NavController, acoes: OnboardingAcoes) {
    composable<RotaNome> { entrada -> NomeRota(entrada, acoes, aoConcluir = { nav.navigate(RotaSuaCasa) }) }
    composable<RotaSuaCasa> { SuaCasaRota(nav, acoes) }
    composable<RotaCriarCasa> { CriarCasaRota(nav, acoes) }
    composable<RotaCodigo> { CodigoRota(nav) }
    composable<RotaBemVindo> { BemVindoRota(acoes) }
}

@Composable
private fun NomeRota(entrada: NavBackStackEntry, acoes: OnboardingAcoes, aoConcluir: () -> Unit, viewModel: NomeViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val escolhido by entrada.savedStateHandle.getStateFlow<String?>(RESULTADO_AVATAR, null).collectAsStateWithLifecycle()
    LaunchedEffect(escolhido) {
        escolhido?.let {
            viewModel.aoEscolherAvatar(it)
            entrada.savedStateHandle[RESULTADO_AVATAR] = null
        }
    }
    LaunchedEffect(estado.concluido) {
        if (estado.concluido) {
            viewModel.aoNavegar()
            aoConcluir()
        }
    }
    NomeScreen(
        estado = estado,
        aoMudarNome = viewModel::aoMudarNome,
        aoEscolherAvatar = viewModel::aoEscolherAvatar,
        aoVerTodos = { acoes.aoAbrirGaleria(estado.avatarId) },
        aoContinuar = viewModel::continuar,
    )
}

@Composable
private fun SuaCasaRota(nav: NavController, acoes: OnboardingAcoes, viewModel: SuaCasaViewModel = hiltViewModel()) {
    val nome by viewModel.primeiroNome.collectAsStateWithLifecycle()
    val escopo = rememberCoroutineScope()
    SuaCasaScreen(
        primeiroNome = nome,
        aoVoltar = { nav.popBackStack() },
        aoCriar = { nav.navigate(RotaCriarCasa) },
        aoTenhoCodigo = { nav.navigate(RotaCodigo) },
        aoSair = { escopo.launch { viewModel.sair(); acoes.aoSair() } },
    )
}

@Composable
private fun CriarCasaRota(nav: NavController, acoes: OnboardingAcoes, viewModel: CriarCasaViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val jaTinhaCasa by viewModel.jaTinhaCasa.collectAsStateWithLifecycle()
    LaunchedEffect(jaTinhaCasa) { if (jaTinhaCasa) acoes.aoTerCasa() }
    CriarCasaScreen(
        estado = estado,
        aoVoltar = { nav.popBackStack() },
        aoMudarNome = viewModel::aoMudarNome,
        aoEscolherTipo = viewModel::aoEscolherTipo,
        aoAnunciar = viewModel::aoAnunciar,
        aoCriar = viewModel::criar,
        aoSeguir = acoes.aoCriarCasa,
    )
}

@Composable
private fun CodigoRota(nav: NavController, viewModel: CodigoViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current
    val escopo = rememberCoroutineScope()
    LaunchedEffect(estado.entrou) {
        if (estado.entrou) {
            viewModel.aoNavegar()
            nav.navigate(RotaBemVindo) { popUpTo(nav.graph.id) { inclusive = true } }
        }
    }
    CodigoScreen(
        estado = estado,
        aoVoltar = { nav.popBackStack() },
        aoDigitar = viewModel::aoDigitar,
        aoColar = {
            escopo.launch {
                clipboard.getClipEntry()?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.let { viewModel.aoDigitar(it.toString()) }
            }
        },
        aoEntrar = viewModel::entrar,
    )
}

@Composable
private fun BemVindoRota(acoes: OnboardingAcoes, viewModel: BemVindoViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    BemVindoScreen(estado, aoBora = acoes.aoTerCasa)
}
