package br.app.divvup.feature.profile.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import br.app.divvup.core.designsystem.component.DivvupAvatarDaGaleria
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupDivider
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.component.MensagemErro
import br.app.divvup.core.designsystem.component.imagemDoAvatar
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.perfil.GaleriaDeAvatares
import br.app.divvup.feature.profile.ProfileMedidas
import br.app.divvup.feature.profile.R

/** Estado de E3. */
data class GaleriaEstado(
    val selecionado: String = GaleriaDeAvatares.padrao,
    val salvando: Boolean = false,
    val erro: Boolean = false,
)

/**
 * E3 Galeria de avatar (docs/design/fonte/Avatar.dc.html, R-32). "Usar uma foto" e "Remover foto"
 * ficam de fora até o armazenamento da foto ser decidido (custo zero, ADR 0001).
 */
@Composable
fun GaleriaDeAvataresScreen(
    estado: GaleriaEstado,
    aoEscolher: (String) -> Unit,
    aoSalvar: () -> Unit,
    aoVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(modifier.fillMaxSize().background(c.background)) {
        DivvupTopBar(paddingInicio = false) {
            DivvupIconButton(DivvupIcone.ChevL, stringResource(R.string.avatar_voltar), aoVoltar)
            DivvupLabel(stringResource(R.string.avatar_rotulo), cor = c.textMuted)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = e.s4, end = e.s4, bottom = e.s4),
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(e.s4)) {
                DivvupText(stringResource(R.string.avatar_titulo), t.h2, Modifier.weight(1f))
                DivvupAvatarDaGaleria(
                    estado.selecionado,
                    ProfileMedidas.avatarGrande,
                    descricao = stringResource(R.string.avatar_escolhido),
                    corDaBorda = c.lima,
                )
            }
            Text(
                pluralStringResource(R.plurals.avatar_texto, GaleriaDeAvatares.QUANTIDADE, GaleriaDeAvatares.QUANTIDADE),
                style = t.small,
                color = c.textMuted,
                modifier = Modifier.padding(top = e.s2),
            )
            Grade(estado.selecionado, aoEscolher, Modifier.padding(top = ProfileMedidas.galeriaTopo))
        }
        DivvupDivider()
        Column(Modifier.padding(start = e.s4, end = e.s4, top = ProfileMedidas.rodapeTopo, bottom = ProfileMedidas.rodapeFundo)) {
            if (estado.erro) MensagemErro(stringResource(R.string.avatar_erro))
            DivvupButton(stringResource(R.string.avatar_salvar), aoSalvar, larguraTotal = true, carregando = estado.salvando)
        }
    }
}

/** Grade 6×4, espaço 8; o escolhido ganha borda `primary` e escala 1,08. */
@Composable
private fun Grade(selecionado: String, aoEscolher: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    val descricaoGaleria = stringResource(R.string.avatar_galeria)
    Column(
        modifier.selectableGroup().semantics { contentDescription = descricaoGaleria },
        verticalArrangement = Arrangement.spacedBy(e.s2),
    ) {
        GaleriaDeAvatares.ids.chunked(ProfileMedidas.colunasGaleria).forEachIndexed { linha, ids ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(e.s2)) {
                ids.forEachIndexed { coluna, id ->
                    val ativo = id == selecionado
                    val n = linha * ProfileMedidas.colunasGaleria + coluna + 1
                    androidx.compose.foundation.Image(
                        painter = painterResource(imagemDoAvatar(id)),
                        contentDescription = stringResource(R.string.avatar_n, n),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .scale(if (ativo) ProfileMedidas.escalaSelecionado else 1f)
                            .clip(DivvupTheme.shapes.pill)
                            .border(e.borda, if (ativo) c.primary else Color.Transparent, DivvupTheme.shapes.pill)
                            .selectable(selected = ativo, role = Role.RadioButton, onClick = { aoEscolher(id) }),
                    )
                }
            }
        }
    }
}
