package br.app.divvup.feature.auth.navegacao

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import br.app.divvup.core.domain.conta.DestinoInicial
import br.app.divvup.feature.auth.boasvindas.BoasVindasScreen
import br.app.divvup.feature.auth.criarconta.CriarContaRota
import br.app.divvup.feature.auth.entrar.EntrarRota
import br.app.divvup.feature.auth.google.ComLoginGoogle
import br.app.divvup.feature.auth.recuperar.RecuperarSenhaRota
import br.app.divvup.feature.auth.splash.SplashRota
import kotlinx.serialization.Serializable

/** Rotas de A1–A5 (mapa de navegação da TELAS.md). */
@Serializable data object RotaSplash

@Serializable data object RotaBoasVindas

@Serializable data object RotaEntrar

@Serializable data object RotaCriarConta

@Serializable data object RotaRecuperarSenha

/** Ações que saem do fluxo de acesso. */
class AcessoAcoes(
    /** A1 decidiu: onboarding (B1) ou deck (C1). */
    val aoDecidir: (DestinoInicial) -> Unit,
    val aoTermos: () -> Unit,
    val aoPrivacidade: () -> Unit,
)

/** Limpa a pilha: depois de entrar ou criar conta, "Voltar" não retorna ao formulário. */
fun NavOptionsBuilder.limparPilha(nav: NavController) {
    popUpTo(nav.graph.id) { inclusive = true }
    launchSingleTop = true
}

fun NavGraphBuilder.grafoDeAcesso(nav: NavController, acoes: AcessoAcoes) {
    composable<RotaSplash> {
        SplashRota(aoDecidir = { destino ->
            if (destino == DestinoInicial.BoasVindas) {
                nav.navigate(RotaBoasVindas) { limparPilha(nav) }
            } else {
                acoes.aoDecidir(destino)
            }
        })
    }
    // Depois do login com Google, a Splash decide entre onboarding e deck (R-01).
    val aposGoogle = { nav.navigate(RotaSplash) { limparPilha(nav) } }
    composable<RotaBoasVindas> {
        ComLoginGoogle(aoEntrar = aposGoogle) { aoGoogle ->
            BoasVindasScreen(
                aoCriarConta = { nav.navigate(RotaCriarConta) },
                aoGoogle = aoGoogle,
                aoJaTenhoConta = { nav.navigate(RotaEntrar) },
                aoTermos = acoes.aoTermos,
                aoPrivacidade = acoes.aoPrivacidade,
            )
        }
    }
    composable<RotaEntrar> {
        ComLoginGoogle(aoEntrar = aposGoogle) { aoGoogle ->
            EntrarRota(
                aoVoltar = { nav.popBackStack() },
                aoEsqueci = { nav.navigate(RotaRecuperarSenha) },
                aoCriarConta = { nav.navigate(RotaCriarConta) { popUpTo<RotaBoasVindas>() } },
                aoGoogle = aoGoogle,
                // Depois de entrar, a Splash decide entre onboarding e deck.
                aoEntrar = { nav.navigate(RotaSplash) { limparPilha(nav) } },
            )
        }
    }
    composable<RotaCriarConta> {
        ComLoginGoogle(aoEntrar = aposGoogle) { aoGoogle ->
            CriarContaRota(
                aoVoltar = { nav.popBackStack() },
                aoEntrar = { nav.navigate(RotaEntrar) { popUpTo<RotaBoasVindas>() } },
                aoGoogle = aoGoogle,
                aoTermos = acoes.aoTermos,
                aoPrivacidade = acoes.aoPrivacidade,
                aoCriar = { acoes.aoDecidir(DestinoInicial.Onboarding) },
            )
        }
    }
    composable<RotaRecuperarSenha> {
        RecuperarSenhaRota(aoVoltar = { nav.popBackStack() })
    }
}
