package br.app.divvup.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.app.divvup.R
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.conta.DestinoInicial
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.feature.auth.navegacao.AcessoAcoes
import br.app.divvup.feature.auth.navegacao.RotaBoasVindas
import br.app.divvup.feature.auth.navegacao.RotaSplash
import br.app.divvup.feature.auth.navegacao.grafoDeAcesso
import br.app.divvup.feature.auth.navegacao.limparPilha
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

/** Destinos ainda não construídos: onboarding (#7) e deck (#11). Provisórios, com "Sair". */
@Serializable data object RotaOnboardingProvisoria

@Serializable data object RotaDeckProvisorio

/** Páginas legais (issue #29/#30). */
private const val URL_TERMOS = "https://divvup.app.br/termos"
private const val URL_PRIVACIDADE = "https://divvup.app.br/privacidade"

@Composable
fun DivvupNavHost() {
    val nav = rememberNavController()
    val links = LocalUriHandler.current
    // As telas ocupam a área do artboard (360×800 sem barras do sistema); o teclado empurra o conteúdo.
    NavHost(
        navController = nav,
        startDestination = RotaSplash,
        modifier = Modifier
            .fillMaxSize()
            .background(DivvupTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        grafoDeAcesso(
            nav,
            AcessoAcoes(
                aoDecidir = { destino ->
                    val rota: Any = if (destino == DestinoInicial.Deck) RotaDeckProvisorio else RotaOnboardingProvisoria
                    nav.navigate(rota) { limparPilha(nav) }
                },
                aoGoogle = {},
                aoTermos = { links.openUri(URL_TERMOS) },
                aoPrivacidade = { links.openUri(URL_PRIVACIDADE) },
            ),
        )
        composable<RotaOnboardingProvisoria> {
            EmConstrucao(stringResource(R.string.provisorio_onboarding), aoSair = { nav.navigate(RotaBoasVindas) { limparPilha(nav) } })
        }
        composable<RotaDeckProvisorio> {
            EmConstrucao(stringResource(R.string.provisorio_deck), aoSair = { nav.navigate(RotaBoasVindas) { limparPilha(nav) } })
        }
    }
}

@HiltViewModel
class SessaoViewModel @Inject constructor(private val conta: RepositorioDeConta) : ViewModel() {
    suspend fun sair() = conta.sair()
}

@Composable
private fun EmConstrucao(titulo: String, aoSair: () -> Unit, viewModel: SessaoViewModel = hiltViewModel()) {
    val escopo = rememberCoroutineScope()
    val e = DivvupTheme.spacing
    Column(
        Modifier.fillMaxSize().background(DivvupTheme.colors.background).padding(e.s4),
        verticalArrangement = Arrangement.spacedBy(e.s4),
    ) {
        DivvupLabel(stringResource(R.string.provisorio_rotulo), cor = DivvupTheme.colors.textMuted)
        Text(titulo, style = DivvupTheme.typography.h2, color = DivvupTheme.colors.text)
        DivvupButton(
            stringResource(R.string.provisorio_sair),
            onClick = { escopo.launch { viewModel.sair(); aoSair() } },
            variante = VarianteBotao.Contorno,
        )
    }
}
