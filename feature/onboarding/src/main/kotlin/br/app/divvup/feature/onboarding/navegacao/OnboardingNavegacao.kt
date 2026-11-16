package br.app.divvup.feature.onboarding.navegacao

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.toRoute
import br.app.divvup.feature.onboarding.permissao.PermissaoScreen
import br.app.divvup.feature.onboarding.tutorial.PASSOS_DO_TUTORIAL
import br.app.divvup.feature.onboarding.tutorial.TutorialScreen
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

/** B6: tutorial do swipe, antes de B7. */
@Serializable data class RotaTutorial(val criouCasa: Boolean)

/** B7: depois de criar (segue para B8) ou de entrar numa casa (segue para C1). */
@Serializable data class RotaPermissao(val criouCasa: Boolean)

/** Chave em que E3 devolve o avatar escolhido (igual a `GaleriaArgs.RESULTADO`). */
const val RESULTADO_AVATAR = "avatarEscolhido"

class OnboardingAcoes(
    /** Abre E3 em modo onboarding com o avatar atual. */
    val aoAbrirGaleria: (avatarAtual: String) -> Unit,
    /** B2 "Sair da conta": a sessão já foi encerrada. */
    val aoSair: () -> Unit,
    /** Casa criada (B3 → B7): segue para "chama a galera" (B8). */
    val aoCriarCasa: () -> Unit,
    /** Entrou numa casa (B5 "Bora" → B7) ou já tinha casa: segue para a estrutura principal. */
    val aoTerCasa: () -> Unit,
)

fun NavGraphBuilder.grafoDeOnboarding(nav: NavController, acoes: OnboardingAcoes) {
    composable<RotaNome> { entrada -> NomeRota(entrada, acoes, aoConcluir = { nav.navigate(RotaSuaCasa) }) }
    composable<RotaSuaCasa> { SuaCasaRota(nav, acoes) }
    composable<RotaCriarCasa> { CriarCasaRota(nav, acoes) }
    composable<RotaCodigo> { CodigoRota(nav) }
    composable<RotaBemVindo> { BemVindoRota(acoes, aoBora = { nav.navigate(RotaTutorial(criouCasa = false)) }) }
    composable<RotaTutorial> { entrada ->
        val criou = entrada.toRoute<RotaTutorial>().criouCasa
        var passo by rememberSaveable { mutableIntStateOf(0) }
        val seguir = { nav.navigate(RotaPermissao(criou)) }
        TutorialScreen(
            passo = passo,
            aoAssumir = { if (passo == 0) passo = 1 },
            aoPassar = { if (passo == 1) passo = 2 },
            aoDesfazer = { if (passo == 2) passo = PASSOS_DO_TUTORIAL },
            aoPular = seguir,
            aoContinuar = seguir,
        )
    }
    composable<RotaPermissao> { entrada ->
        val criou = entrada.toRoute<RotaPermissao>().criouCasa
        PermissaoRota(aoSeguir = if (criou) acoes.aoCriarCasa else acoes.aoTerCasa)
    }
}

/**
 * R-36: só pede no Android 13+ e se ainda não foi concedida; senão segue direto. "Agora não" e a
 * resposta do sistema (sim ou não) seguem o fluxo.
 */
@Composable
private fun PermissaoRota(aoSeguir: () -> Unit) {
    val contexto = LocalContext.current
    val precisa = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    LaunchedEffect(precisa) { if (!precisa) aoSeguir() }
    if (!precisa) return
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { aoSeguir() }
    PermissaoScreen(
        aoAtivar = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) pedir.launch(Manifest.permission.POST_NOTIFICATIONS) },
        aoAgoraNao = aoSeguir,
    )
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
        aoSeguir = { nav.navigate(RotaTutorial(criouCasa = true)) },
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
private fun BemVindoRota(acoes: OnboardingAcoes, aoBora: () -> Unit, viewModel: BemVindoViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    BemVindoScreen(estado, aoBora = aoBora)
}
