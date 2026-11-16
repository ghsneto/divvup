package br.app.divvup.feature.profile.navegacao

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import br.app.divvup.feature.profile.conta.ExcluirContaScreen
import br.app.divvup.feature.profile.conta.ExcluirContaViewModel
import android.provider.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import br.app.divvup.feature.profile.conta.AparenciaScreen
import br.app.divvup.feature.profile.conta.EditarPerfilScreen
import br.app.divvup.feature.profile.conta.SobreScreen
import br.app.divvup.feature.profile.perfil.AparenciaViewModel
import br.app.divvup.feature.profile.perfil.EditarPerfilViewModel
import br.app.divvup.feature.profile.perfil.PerfilScreen
import br.app.divvup.feature.profile.perfil.PerfilViewModel
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import br.app.divvup.feature.profile.R
import br.app.divvup.feature.profile.notificacoes.PrefsNotifScreen
import br.app.divvup.feature.profile.notificacoes.PrefsNotifViewModel
import java.time.LocalTime
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

/** E4. */
@Serializable data object RotaPrefsNotif

fun NavGraphBuilder.telaPrefsNotif(nav: NavController) {
    composable<RotaPrefsNotif> { PrefsNotifRota(nav) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrefsNotifRota(nav: NavController, vm: PrefsNotifViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var escolhendo by remember { mutableStateOf<Boolean?>(null) }
    // R-36: confere a permissão do sistema ao abrir e ao voltar das configurações.
    LifecycleResumeEffect(Unit) {
        vm.permissao(NotificationManagerCompat.from(contexto).areNotificationsEnabled())
        onPauseOrDispose { }
    }
    PrefsNotifScreen(
        estado = estado,
        aoVoltar = { nav.popBackStack() },
        aoMudarTipos = vm::mudarTipos,
        aoMudarSilencio = vm::mudarSilencio,
        aoEscolherInicio = { escolhendo = true },
        aoEscolherFim = { escolhendo = false },
        aoAbrirConfiguracoes = {
            contexto.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName),
            )
        },
    )
    escolhendo?.let { inicio ->
        val atual = if (inicio) estado.preferencias.inicio else estado.preferencias.fim
        val seletor = rememberTimePickerState(atual.hour, atual.minute, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { escolhendo = null },
            confirmButton = {
                TextButton({
                    val hora = LocalTime.of(seletor.hour, seletor.minute)
                    if (inicio) vm.mudarInicio(hora) else vm.mudarFim(hora)
                    escolhendo = null
                }) { Text(stringResource(R.string.prefs_ok)) }
            },
            dismissButton = { TextButton({ escolhendo = null }) { Text(stringResource(R.string.prefs_cancelar)) } },
            title = { Text(stringResource(if (inicio) R.string.prefs_de else R.string.prefs_ate)) },
        ) { TimePicker(seletor) }
    }
}

/** E2, E5, E6. */
@Serializable data object RotaEditarPerfil

@Serializable data object RotaAparencia

@Serializable data object RotaSobre

/** Para onde E1 e E6 levam (destinos de outros módulos ou links externos). */
class AcoesDoPerfil(
    val aoMinhaCasa: () -> Unit,
    val aoSair: () -> Unit,
    val aoExcluir: () -> Unit,
    val aoAlterarSenha: () -> Unit,
    val aoTermos: () -> Unit,
    val aoPrivacidade: () -> Unit,
    val aoContato: () -> Unit,
    val versao: String,
    val build: Int,
)

fun NavGraphBuilder.telasDoPerfil(nav: NavController, acoes: AcoesDoPerfil) {
    composable<RotaEditarPerfil> { entrada ->
        val vm: EditarPerfilViewModel = hiltViewModel()
        val estado by vm.estado.collectAsStateWithLifecycle()
        val salvo by vm.salvo.collectAsStateWithLifecycle()
        val escolhido by entrada.savedStateHandle.getStateFlow<String?>(GaleriaArgs.RESULTADO, null).collectAsStateWithLifecycle()
        LaunchedEffect(escolhido) {
            escolhido?.let {
                vm.escolherAvatar(it)
                entrada.savedStateHandle[GaleriaArgs.RESULTADO] = null
            }
        }
        LaunchedEffect(salvo) { if (salvo) nav.popBackStack() }
        EditarPerfilScreen(
            estado = estado,
            aoVoltar = { nav.popBackStack() },
            aoTrocarAvatar = { nav.navigate(RotaGaleria(avatarInicial = estado.avatarId, soEscolher = true)) },
            aoMudarNome = vm::mudarNome,
            aoAlterarSenha = acoes.aoAlterarSenha,
            aoSalvar = vm::salvar,
        )
    }
    composable<RotaAparencia> {
        val vm: AparenciaViewModel = hiltViewModel()
        val tema by vm.tema.collectAsStateWithLifecycle()
        AparenciaScreen(tema, aoVoltar = { nav.popBackStack() }, aoEscolher = vm::escolher)
    }
    composable<RotaSobre> {
        SobreScreen(acoes.versao, acoes.build, { nav.popBackStack() }, acoes.aoTermos, acoes.aoPrivacidade, acoes.aoContato)
    }
}

/** Conteúdo da aba Perfil (E1) dentro da estrutura principal. */
@Composable
fun AbaPerfil(nav: NavController, acoes: AcoesDoPerfil, vm: PerfilViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val escopo = rememberCoroutineScope()
    LifecycleResumeEffect(Unit) {
        vm.recarregar()
        onPauseOrDispose { }
    }
    PerfilScreen(
        estado = estado,
        aoEditar = { nav.navigate(RotaEditarPerfil) },
        aoTocarMedalha = vm::tocarMedalha,
        aoMinhaCasa = acoes.aoMinhaCasa,
        aoNotificacoes = { nav.navigate(RotaPrefsNotif) },
        aoAparencia = { nav.navigate(RotaAparencia) },
        aoSobre = { nav.navigate(RotaSobre) },
        aoPedirSaida = vm::pedirSaida,
        aoCancelarSaida = vm::cancelarSaida,
        aoSair = {
            escopo.launch {
                vm.sair()
                acoes.aoSair()
            }
        },
        aoExcluir = acoes.aoExcluir,
    )
}

/** E7. */
@Serializable data object RotaExcluirConta

/**
 * E7: `pedirTokenGoogle` vem do app (o login com Google mora em `feature:auth`); conta só Google
 * confirma por ele. `aoExcluida` leva de volta às boas-vindas.
 */
fun NavGraphBuilder.telaExcluirConta(nav: NavController, pedirTokenGoogle: suspend (Context) -> String?, aoExcluida: () -> Unit) {
    composable<RotaExcluirConta> {
        val vm: ExcluirContaViewModel = hiltViewModel()
        val estado by vm.estado.collectAsStateWithLifecycle()
        val contexto = LocalContext.current
        val escopo = rememberCoroutineScope()
        BackHandler(enabled = estado.passo == 2 && !estado.feita) { vm.voltar() }
        ExcluirContaScreen(
            estado = estado,
            aoVoltar = { if (vm.voltar()) nav.popBackStack() },
            aoContinuar = vm::continuar,
            aoManter = { nav.popBackStack() },
            aoMudarSenha = vm::mudarSenha,
            aoMudarPalavra = vm::mudarPalavra,
            aoExcluir = {
                if (estado.temSenha) {
                    vm.excluir()
                } else {
                    escopo.launch { pedirTokenGoogle(contexto)?.let { vm.excluir(it) } }
                }
            },
            aoInicio = aoExcluida,
        )
    }
}
