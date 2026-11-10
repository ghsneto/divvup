package br.app.divvup.feature.onboarding.navegacao

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.app.divvup.feature.onboarding.nome.NomeScreen
import br.app.divvup.feature.onboarding.nome.NomeViewModel
import kotlinx.serialization.Serializable

/** B1. */
@Serializable data object RotaNome

/** Chave em que E3 devolve o avatar escolhido (igual a `GaleriaArgs.RESULTADO`). */
const val RESULTADO_AVATAR = "avatarEscolhido"

class OnboardingAcoes(
    /** Abre E3 em modo onboarding com o avatar atual. */
    val aoAbrirGaleria: (avatarAtual: String) -> Unit,
    /** B1 concluído: segue para B2. */
    val aoConcluirNome: () -> Unit,
)

fun NavGraphBuilder.grafoDeOnboarding(acoes: OnboardingAcoes) {
    composable<RotaNome> { entrada -> NomeRota(entrada, acoes) }
}

@Composable
private fun NomeRota(entrada: NavBackStackEntry, acoes: OnboardingAcoes, viewModel: NomeViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val escolhido by entrada.savedStateHandle.getStateFlow<String?>(RESULTADO_AVATAR, null).collectAsStateWithLifecycle()
    LaunchedEffect(escolhido) {
        escolhido?.let {
            viewModel.aoEscolherAvatar(it)
            entrada.savedStateHandle[RESULTADO_AVATAR] = null
        }
    }
    LaunchedEffect(estado.concluido) { if (estado.concluido) acoes.aoConcluirNome() }
    NomeScreen(
        estado = estado,
        aoMudarNome = viewModel::aoMudarNome,
        aoEscolherAvatar = viewModel::aoEscolherAvatar,
        aoVerTodos = { acoes.aoAbrirGaleria(estado.avatarId) },
        aoContinuar = viewModel::continuar,
    )
}
