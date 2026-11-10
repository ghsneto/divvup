package br.app.divvup.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme

/**
 * Botão de ícone `.iconbtn` (48dp, raio 4) com contador `.count` opcional
 * (lima, 20dp, 11sp, borda na cor do fundo).
 */
@Composable
fun DivvupIconButton(
    icone: DivvupIcone,
    descricao: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contador: Int? = null,
) {
    Box(
        modifier
            .size(48.dp)
            .clip(DivvupTheme.shapes.sm)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = descricao },
        contentAlignment = Alignment.Center,
    ) {
        DivvupIcon(icone, tint = DivvupTheme.colors.text)
        if (contador != null && contador > 0) {
            Contador(contador, Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp))
        }
    }
}

@Composable
private fun Contador(valor: Int, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val shape = DivvupTheme.shapes.pill
    Box(
        modifier
            .heightIn(min = 20.dp)
            .widthIn(min = 20.dp)
            .clip(shape)
            .background(c.accent, shape)
            .border(DivvupTheme.spacing.borda, c.background, shape)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            formatarNumero(valor),
            style = DivvupTheme.typography.botao.copy(fontSize = 11.sp, lineHeight = 11.sp),
            color = c.onAccent,
        )
    }
}

/** Barra superior `.topbar`: 64dp, padding 16 à esquerda e 8 à direita, espaço 8. */
@Composable
fun DivvupTopBar(
    modifier: Modifier = Modifier,
    paddingInicio: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = if (paddingInicio) 16.dp else 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Item da barra inferior. */
data class AbaNavegacao(val icone: DivvupIcone, val rotulo: String)

/**
 * Barra inferior `.tabbar`: 72dp, borda superior, 4 abas. Aba ativa: texto `text`,
 * ícone `primary` e traço lima 32×4 no topo.
 */
@Composable
fun DivvupTabBar(
    abas: List<AbaNavegacao>,
    selecionada: Int,
    onSelecionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    Column(modifier.fillMaxWidth().background(c.surface)) {
        DivvupDivider()
        Row(Modifier.fillMaxWidth().height(71.dp)) {
            abas.forEachIndexed { i, aba ->
                val ativa = i == selecionada
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(selected = ativa, role = Role.Tab, onClick = { onSelecionar(i) }),
                ) {
                    if (ativa) {
                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-1).dp)
                                .size(width = 32.dp, height = 4.dp)
                                .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(c.accent),
                        )
                    }
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        DivvupIcon(aba.icone, tint = if (ativa) c.primary else c.textMuted)
                        Text(
                            aba.rotulo,
                            style = DivvupTheme.typography.botao.copy(fontSize = 12.sp),
                            color = if (ativa) c.text else c.textMuted,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** FAB `.fab`: 64dp quadrado (raio 4), `primary`, ícone 28. Posição (16 da direita, 88 do fundo) é da tela. */
@Composable
fun DivvupFab(
    icone: DivvupIcone,
    descricao: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    Box(
        modifier
            .size(64.dp)
            .clip(DivvupTheme.shapes.sm)
            .background(c.primary)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = descricao },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides c.onPrimary) {
            DivvupIcon(icone, tamanho = 28.dp)
        }
    }
}
