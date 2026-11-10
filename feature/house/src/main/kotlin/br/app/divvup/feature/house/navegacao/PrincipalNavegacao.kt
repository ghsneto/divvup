package br.app.divvup.feature.house.navegacao

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.app.divvup.feature.house.principal.Aba
import br.app.divvup.feature.house.principal.EstruturaPrincipal
import br.app.divvup.feature.house.principal.PrincipalViewModel
import kotlinx.serialization.Serializable

/** C1 e as outras abas da estrutura principal. */
@Serializable data object RotaPrincipal

fun NavGraphBuilder.telaPrincipal(aoAbrirNotificacoes: () -> Unit, conteudo: @Composable (Aba) -> Unit) {
    composable<RotaPrincipal> { PrincipalRota(aoAbrirNotificacoes, conteudo) }
}

@Composable
private fun PrincipalRota(aoAbrirNotificacoes: () -> Unit, conteudo: @Composable (Aba) -> Unit, viewModel: PrincipalViewModel = hiltViewModel()) {
    val casa by viewModel.casa.collectAsStateWithLifecycle()
    val aba by viewModel.aba.collectAsStateWithLifecycle()
    EstruturaPrincipal(
        casa = casa,
        temporada = viewModel.temporada(),
        // Notificações (F1) chegam na issue das notificações; até lá, sem contador.
        naoLidas = 0,
        aba = aba,
        aoTrocarAba = viewModel::aoTrocarAba,
        aoAbrirNotificacoes = aoAbrirNotificacoes,
        conteudo = conteudo,
    )
}
