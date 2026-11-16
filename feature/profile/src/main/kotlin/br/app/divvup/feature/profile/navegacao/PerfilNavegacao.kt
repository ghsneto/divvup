package br.app.divvup.feature.profile.navegacao

import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
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
