package br.app.divvup.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.conta.DestinoInicial
import br.app.divvup.feature.auth.navegacao.AcessoAcoes
import br.app.divvup.feature.auth.navegacao.RotaBoasVindas
import br.app.divvup.feature.auth.navegacao.RotaSplash
import br.app.divvup.feature.auth.navegacao.grafoDeAcesso
import br.app.divvup.feature.auth.navegacao.limparPilha
import br.app.divvup.feature.onboarding.navegacao.OnboardingAcoes
import br.app.divvup.feature.onboarding.navegacao.RotaNome
import br.app.divvup.feature.onboarding.navegacao.RotaSuaCasa
import br.app.divvup.feature.deck.AbaDeck
import br.app.divvup.feature.house.navegacao.RotaConvide
import br.app.divvup.feature.tasks.navegacao.RotaNovaTarefa
import br.app.divvup.feature.tasks.navegacao.telaNovaTarefa
import br.app.divvup.feature.tasks.navegacao.AbaMinhasTarefas
import br.app.divvup.feature.tasks.navegacao.AcoesDaConclusao
import br.app.divvup.feature.tasks.navegacao.RotaDetalhe
import br.app.divvup.feature.tasks.navegacao.telasDeTarefa
import br.app.divvup.feature.house.principal.PrincipalViewModel
import br.app.divvup.feature.house.navegacao.RotaMembro
import br.app.divvup.feature.house.navegacao.RotaMinhaCasa
import br.app.divvup.feature.ranking.navegacao.AbaRanking
import br.app.divvup.feature.ranking.navegacao.RotaTemporadas
import br.app.divvup.feature.ranking.navegacao.telaTemporadas
import br.app.divvup.feature.notifications.navegacao.RotaAvisos
import br.app.divvup.feature.notifications.navegacao.telaAvisos
import br.app.divvup.core.model.TipoDeNotificacao
import br.app.divvup.feature.house.navegacao.RotaPrincipal
import br.app.divvup.feature.house.navegacao.telasDeConvite
import br.app.divvup.feature.house.navegacao.telasDeMembros
import br.app.divvup.feature.house.navegacao.telaPrincipal
import br.app.divvup.feature.house.principal.Aba
import br.app.divvup.feature.onboarding.navegacao.grafoDeOnboarding
import br.app.divvup.feature.profile.navegacao.RotaGaleria
import br.app.divvup.feature.profile.navegacao.telaGaleria
import br.app.divvup.feature.profile.navegacao.AbaPerfil
import br.app.divvup.feature.profile.navegacao.AcoesDoPerfil
import br.app.divvup.feature.profile.navegacao.telasDoPerfil
import br.app.divvup.feature.profile.navegacao.RotaExcluirConta
import br.app.divvup.feature.profile.navegacao.telaExcluirConta
import br.app.divvup.feature.auth.google.pedirTokenGoogle
import br.app.divvup.feature.auth.navegacao.RotaRecuperarSenha
import br.app.divvup.BuildConfig
import br.app.divvup.feature.profile.navegacao.telaPrefsNotif

/** Páginas legais (issue #29/#30). */
private const val URL_TERMOS = "https://divvup.app.br/termos"
private const val URL_PRIVACIDADE = "https://divvup.app.br/privacidade"
private const val EMAIL_CONTATO = "mailto:contato@divvup.app.br"

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
                aoSair = aoSair,
                aoCriarCasa = { nav.navigate(RotaConvide) { limparPilha(nav) } },
                aoTerCasa = { nav.navigate(RotaPrincipal) { limparPilha(nav) } },
            ),
        )
        telasDeConvite(nav, aoConcluirConvide = { nav.navigate(RotaPrincipal) { limparPilha(nav) } })
        telasDeMembros(nav, aoSairDaCasa = { nav.navigate(RotaSuaCasa) { limparPilha(nav) } })
        telaGaleria(nav)
        telaNovaTarefa(nav)
        telaTemporadas(nav)
        telaPrefsNotif(nav)
        val acoesDoPerfil = AcoesDoPerfil(
            aoMinhaCasa = { nav.navigate(RotaMinhaCasa) },
            aoSair = aoSair,
            aoExcluir = { nav.navigate(RotaExcluirConta) },
            aoAlterarSenha = { nav.navigate(RotaRecuperarSenha) },
            aoTermos = { links.openUri(URL_TERMOS) },
            aoPrivacidade = { links.openUri(URL_PRIVACIDADE) },
            aoContato = { links.openUri(EMAIL_CONTATO) },
            versao = BuildConfig.VERSION_NAME,
            build = BuildConfig.VERSION_CODE,
        )
        telasDoPerfil(nav, acoesDoPerfil)
        telaExcluirConta(
            nav,
            pedirTokenGoogle = { contexto -> pedirTokenGoogle(contexto, BuildConfig.GOOGLE_WEB_CLIENT_ID).getOrNull() },
            aoExcluida = { nav.navigate(RotaBoasVindas) { limparPilha(nav) } },
        )
        // C4 volta para a estrutura principal já na aba certa.
        val irParaAba = { aba: Aba ->
            nav.popBackStack<RotaPrincipal>(inclusive = false)
            nav.getBackStackEntry<RotaPrincipal>().savedStateHandle[PrincipalViewModel.ARG_ABA] = aba.name
        }
        telasDeTarefa(nav, AcoesDaConclusao(aoPegarOutra = { irParaAba(Aba.Deck) }, aoContinuar = { irParaAba(Aba.MinhasTarefas) }))
        telaPrincipal(aoAbrirNotificacoes = { nav.navigate(RotaAvisos) }) { aba, trocarAba ->
            when (aba) {
                Aba.Deck -> AbaDeck(
                    aoNovaTarefa = { nav.navigate(RotaNovaTarefa) },
                    aoMinhasTarefas = { trocarAba(Aba.MinhasTarefas) },
                    aoDetalhes = { nav.navigate(RotaDetalhe(it.id)) },
                )
                Aba.MinhasTarefas -> AbaMinhasTarefas(nav, aoIrParaDeck = { trocarAba(Aba.Deck) })
                Aba.Ranking -> AbaRanking(aoAbrirMembro = { nav.navigate(RotaMembro(it)) }, aoTemporadas = { nav.navigate(RotaTemporadas) })
                Aba.Perfil -> AbaPerfil(nav, acoesDoPerfil)
            }
        }
        telaAvisos(nav) { aviso ->
            val tarefa = aviso.tarefaId
            when {
                tarefa != null -> nav.navigate(RotaDetalhe(tarefa))
                aviso.tipo == TipoDeNotificacao.SEASON_END -> nav.navigate(RotaTemporadas)
                else -> Unit
            }
        }
    }
}
