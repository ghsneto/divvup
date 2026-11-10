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
import br.app.divvup.feature.onboarding.navegacao.OnboardingAcoes
import br.app.divvup.feature.onboarding.navegacao.RotaNome
import br.app.divvup.feature.onboarding.navegacao.RotaSuaCasa
import br.app.divvup.feature.house.navegacao.RotaPrincipal
import br.app.divvup.feature.house.navegacao.telaPrincipal
import br.app.divvup.feature.house.principal.Aba
import br.app.divvup.feature.onboarding.navegacao.grafoDeOnboarding
import br.app.divvup.feature.profile.navegacao.RotaGaleria
import br.app.divvup.feature.profile.navegacao.telaGaleria
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

/** Destinos ainda não construídos: B4 código (#9) e F1 notificações. Provisórios. */
@Serializable data object RotaCodigoProvisoria

@Serializable data object RotaNotificacoesProvisoria

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
                    val rota: Any = when (destino) {
                        DestinoInicial.Deck -> RotaPrincipal
                        DestinoInicial.SuaCasa -> RotaSuaCasa
                        else -> RotaNome
                    }
                    nav.navigate(rota) { limparPilha(nav) }
                },
                aoTermos = { links.openUri(URL_TERMOS) },
                aoPrivacidade = { links.openUri(URL_PRIVACIDADE) },
            ),
        )
        val aoSair = { nav.navigate(RotaBoasVindas) { limparPilha(nav) } }
        grafoDeOnboarding(
            nav,
            OnboardingAcoes(
                aoAbrirGaleria = { atual -> nav.navigate(RotaGaleria(avatarInicial = atual, soEscolher = true)) },
                aoTenhoCodigo = { nav.navigate(RotaCodigoProvisoria) },
                aoSair = aoSair,
                aoTerCasa = { nav.navigate(RotaPrincipal) { limparPilha(nav) } },
            ),
        )
        telaGaleria(nav)
        telaPrincipal(aoAbrirNotificacoes = { nav.navigate(RotaNotificacoesProvisoria) }) { aba ->
            val titulo = when (aba) {
                Aba.Deck -> R.string.provisorio_deck
                Aba.MinhasTarefas -> R.string.provisorio_minhas_tarefas
                Aba.Ranking -> R.string.provisorio_ranking
                Aba.Perfil -> R.string.provisorio_perfil
            }
            // Só o perfil oferece "Sair" (como E1); as outras abas chegam nas próximas issues.
            EmConstrucao(stringResource(titulo), aoSair = aoSair.takeIf { aba == Aba.Perfil })
        }
        composable<RotaCodigoProvisoria> { EmConstrucao(stringResource(R.string.provisorio_codigo), aoSair = null) }
        composable<RotaNotificacoesProvisoria> { EmConstrucao(stringResource(R.string.provisorio_notificacoes), aoSair = null) }
    }
}

@HiltViewModel
class SessaoViewModel @Inject constructor(private val conta: RepositorioDeConta) : ViewModel() {
    suspend fun sair() = conta.sair()
}

@Composable
private fun EmConstrucao(titulo: String, aoSair: (() -> Unit)?, viewModel: SessaoViewModel = hiltViewModel()) {
    val escopo = rememberCoroutineScope()
    val e = DivvupTheme.spacing
    Column(
        Modifier.fillMaxSize().background(DivvupTheme.colors.background).padding(e.s4),
        verticalArrangement = Arrangement.spacedBy(e.s4),
    ) {
        DivvupLabel(stringResource(R.string.provisorio_rotulo), cor = DivvupTheme.colors.textMuted)
        Text(titulo, style = DivvupTheme.typography.h2, color = DivvupTheme.colors.text)
        if (aoSair != null) {
            DivvupButton(
                stringResource(R.string.provisorio_sair),
                onClick = { escopo.launch { viewModel.sair(); aoSair() } },
                variante = VarianteBotao.Contorno,
            )
        }
    }
}
