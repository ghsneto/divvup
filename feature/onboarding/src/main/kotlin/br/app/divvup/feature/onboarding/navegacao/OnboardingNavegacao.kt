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

/** Chave em que E3 devolve o avatar escolhido (igual a `GaleriaArgs.RESULTADO`). */
const val RESULTADO_AVATAR = "avatarEscolhido"

class OnboardingAcoes(
    /** Abre E3 em modo onboarding com o avatar atual. */
    val aoAbrirGaleria: (avatarAtual: String) -> Unit,
    /** B2 "tenho um código" → B4. */
    val aoTenhoCodigo: () -> Unit,
    /** B2 "Sair da conta": a sessão já foi encerrada. */
    val aoSair: () -> Unit,
    /** Casa criada (ou a pessoa já tinha casa): segue para o tutorial/estrutura principal. */
    val aoTerCasa: () -> Unit,
)

fun NavGraphBuilder.grafoDeOnboarding(nav: NavController, acoes: OnboardingAcoes) {
    composable<RotaNome> { entrada -> NomeRota(entrada, acoes, aoConcluir = { nav.navigate(RotaSuaCasa) }) }
    composable<RotaSuaCasa> { SuaCasaRota(nav, acoes) }
    composable<RotaCriarCasa> { CriarCasaRota(nav, acoes) }
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
        aoTenhoCodigo = acoes.aoTenhoCodigo,
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
        aoSeguir = acoes.aoTerCasa,
    )
}
