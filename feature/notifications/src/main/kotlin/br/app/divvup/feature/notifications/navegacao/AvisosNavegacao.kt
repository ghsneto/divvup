package br.app.divvup.feature.notifications.navegacao

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.app.divvup.core.model.Notificacao
import br.app.divvup.feature.notifications.AvisosScreen
import br.app.divvup.feature.notifications.AvisosViewModel
import kotlinx.serialization.Serializable

/** F1. */
@Serializable data object RotaAvisos

/** F1; `aoAbrir` leva ao destino do aviso (tarefa, temporadas…). */
fun NavGraphBuilder.telaAvisos(nav: NavController, aoAbrir: (Notificacao) -> Unit) {
    composable<RotaAvisos> {
        val vm: AvisosViewModel = hiltViewModel()
        val estado by vm.estado.collectAsStateWithLifecycle()
        AvisosScreen(
            estado = estado,
            aoVoltar = { nav.popBackStack() },
            aoMarcarTodas = vm::marcarTodas,
            aoAbrir = {
                vm.abrir(it)
                aoAbrir(it)
            },
        )
    }
}
