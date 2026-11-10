package br.app.divvup.feature.onboarding.nome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import br.app.divvup.core.designsystem.component.DivvupAvatarDaGaleria
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupTextField
import br.app.divvup.core.designsystem.component.DivvupTextoComLinks
import br.app.divvup.core.designsystem.component.MensagemErro
import br.app.divvup.core.designsystem.component.Trecho
import br.app.divvup.core.designsystem.component.imagemDoAvatar
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.perfil.GaleriaDeAvatares
import br.app.divvup.feature.onboarding.OnboardingMedidas
import br.app.divvup.feature.onboarding.R
import br.app.divvup.feature.onboarding.comum.ProgressoOnboarding

/**
 * B1 Como te chamam (docs/design/fonte/OnbNome.dc.html). O link "Usar foto" fica de fora até o
 * armazenamento da foto ser decidido (custo zero, ADR 0001).
 */
@Composable
fun NomeScreen(
    estado: NomeEstado,
    aoMudarNome: (String) -> Unit,
    aoEscolherAvatar: (String) -> Unit,
    aoVerTodos: () -> Unit,
    aoContinuar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(modifier.fillMaxSize().background(c.background)) {
        ProgressoOnboarding(passo = 1)
        Column(
            Modifier
                .weight(1f)
                .padding(start = e.s4, end = e.s4, top = OnboardingMedidas.conteudoTopo),
        ) {
            DivvupText(stringResource(R.string.nome_titulo), t.h1, Modifier.widthIn(max = OnboardingMedidas.tituloMaximo))
            Row(
                Modifier.padding(top = e.s5),
                horizontalArrangement = Arrangement.spacedBy(e.s4),
                verticalAlignment = Alignment.Bottom,
            ) {
                AvatarComLapis(estado.avatarId, aoVerTodos)
                Column(Modifier.padding(bottom = OnboardingMedidas.escolhaFundo), verticalArrangement = Arrangement.spacedBy(e.s2)) {
                    DivvupLabel(stringResource(R.string.nome_escolha_rapida), cor = c.textMuted)
                    GaleriaDeAvatares.escolhaRapida.chunked(COLUNAS_RAPIDAS).forEachIndexed { linha, ids ->
                        Row(horizontalArrangement = Arrangement.spacedBy(e.s2)) {
                            ids.forEachIndexed { coluna, id ->
                                val ativo = id == estado.avatarId
                                Image(
                                    painter = painterResource(imagemDoAvatar(id)),
                                    contentDescription = stringResource(R.string.nome_avatar_n, linha * COLUNAS_RAPIDAS + coluna + 1),
                                    modifier = Modifier
                                        .size(OnboardingMedidas.avatarRapido)
                                        .clip(DivvupTheme.shapes.pill)
                                        .border(e.borda, if (ativo) c.primary else Color.Transparent, DivvupTheme.shapes.pill)
                                        .selectable(selected = ativo, role = Role.RadioButton) { aoEscolherAvatar(id) },
                                )
                            }
                        }
                    }
                }
            }
            DivvupTextoComLinks(
                listOf(Trecho.Link(stringResource(R.string.nome_ver_todos), forte = true, aoClicar = aoVerTodos)),
                estilo = t.small,
                modifier = Modifier.padding(top = OnboardingMedidas.linksTopo),
            )
            DivvupTextField(
                valor = estado.nome,
                onValorMudou = aoMudarNome,
                rotulo = stringResource(R.string.nome_campo),
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                abaixo = { Text(stringResource(R.string.nome_dica), style = t.small, color = c.textMuted) },
                modifier = Modifier.padding(top = OnboardingMedidas.campoTopo),
            )
            if (estado.erro) MensagemErro(stringResource(R.string.nome_erro))
            Spacer(Modifier.weight(1f))
            DivvupButton(
                stringResource(R.string.nome_continuar),
                aoContinuar,
                iconeDepois = DivvupIcone.SetaR,
                larguraTotal = true,
                habilitado = estado.podeContinuar,
                carregando = estado.salvando,
                modifier = Modifier.padding(bottom = OnboardingMedidas.rodape),
            )
        }
    }
}

private const val COLUNAS_RAPIDAS = 3

/** Avatar grande (96, borda lima) com o botão de lápis `primary` sobreposto no canto. */
@Composable
private fun AvatarComLapis(avatarId: String, aoEditar: () -> Unit) {
    val c = DivvupTheme.colors
    Box {
        DivvupAvatarDaGaleria(avatarId, OnboardingMedidas.avatarGrande, descricao = stringResource(R.string.nome_seu_avatar), corDaBorda = c.accent)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = OnboardingMedidas.lapisFora, y = OnboardingMedidas.lapisFora)
                .size(DivvupTheme.spacing.toqueMinimo)
                .clip(DivvupTheme.shapes.pill)
                .background(c.primary)
                .selectable(selected = false, role = Role.Button, onClick = aoEditar),
            contentAlignment = Alignment.Center,
        ) {
            DivvupIcon(DivvupIcone.Editar, descricao = stringResource(R.string.nome_escolher_avatar), tint = c.onPrimary)
        }
    }
}
