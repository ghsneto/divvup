package br.app.divvup.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme

/** Duração padrão do snackbar com contagem (`.snack .bar`, 3 s). */
const val DURACAO_SNACKBAR_MS = 3_000

/**
 * Snackbar `.snack`: 52dp, raio 4, fundo `snack`; ícone e ação em lima; barra de contagem
 * lima de 3dp no rodapé quando `contagem` é true (ex.: "Desfazer" por 3 s, R-18).
 * A posição (12dp das laterais, 84dp do fundo) é da tela.
 */
@Composable
fun DivvupSnackbar(
    mensagem: String,
    modifier: Modifier = Modifier,
    icone: DivvupIcone? = DivvupIcone.Info,
    acao: String? = null,
    onAcao: () -> Unit = {},
    contagem: Boolean = false,
    /** Texto depois do "·" em cinza ("Tarefa assumida · +50 pts ao concluir"). */
    complemento: String? = null,
    /** Ícone da ação ("Desfazer" em C1). */
    iconeAcao: DivvupIcone? = null,
    /** Altura mínima (52; na faixa de C1, 44). */
    alturaMinima: Dp = 52.dp,
) {
    val c = DivvupTheme.colors
    val shape = DivvupTheme.shapes.sm
    var iniciada by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { iniciada = true }
    val restante by animateFloatAsState(
        targetValue = if (iniciada && contagem) 0f else 1f,
        animationSpec = tween(DURACAO_SNACKBAR_MS, easing = LinearEasing),
        label = "contagem",
    )
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = alturaMinima)
            .clip(shape)
            .background(c.snack, shape)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            Modifier.heightIn(min = alturaMinima).padding(start = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icone?.let { DivvupIcon(it, tamanho = TamanhoIcone.Pequeno.dp, tint = c.lima) }
            Text(
                buildAnnotatedString {
                    append(mensagem)
                    complemento?.let {
                        withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = c.sobreTinta.textoMudo)) { append(" · $it") }
                    }
                },
                style = DivvupTheme.typography.botao,
                color = c.snackText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            acao?.let {
                Box(
                    Modifier
                        .heightIn(min = 44.dp)
                        .clip(shape)
                        .clickable(role = Role.Button, onClick = onAcao)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        iconeAcao?.let { i -> DivvupIcon(i, tamanho = TamanhoIcone.Pequeno.dp, tint = c.lima) }
                        Text(it, style = DivvupTheme.typography.botao, color = c.lima)
                    }
                }
            }
        }
        if (contagem) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .graphicsLayer {
                        scaleX = restante
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .background(c.lima),
            )
        }
    }
}

/** Fundo escurecido `.scrim` que fecha ao tocar fora. */
@Composable
fun DivvupScrim(
    onFechar: () -> Unit,
    modifier: Modifier = Modifier,
    alinhamento: Alignment = Alignment.BottomCenter,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(DivvupTheme.colors.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onFechar),
        contentAlignment = alinhamento,
    ) {
        // Toques dentro do conteúdo não fecham.
        Box(Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
            content()
        }
    }
}

/** Folha `.sheet`: `surface`, raio 24 no topo, padding 12/20/24 e alça `.grab` 40×4. */
@Composable
fun DivvupSheet(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = DivvupTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(DivvupTheme.shapes.sheet)
            .background(c.surface)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
                .size(width = 40.dp, height = 4.dp)
                .clip(DivvupTheme.shapes.sm)
                .background(c.border),
        )
        content()
    }
}

/**
 * Diálogo `.dialog`: margem 20, `surface`, raio 12, padding 24, borda. Confirmação destrutiva
 * usa o verbo da ação no botão ("Sair da casa", não "OK").
 */
@Composable
fun DivvupDialog(
    titulo: String,
    texto: String,
    modifier: Modifier = Modifier,
    acoes: @Composable RowScope.() -> Unit,
) {
    val c = DivvupTheme.colors
    val shape = DivvupTheme.shapes.md
    Column(
        modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface, shape)
            .border(DivvupTheme.spacing.borda, c.border, shape)
            .padding(24.dp),
    ) {
        Text(titulo, style = DivvupTheme.typography.h3.copy(fontSize = 28.sp), color = c.text)
        Text(texto, style = DivvupTheme.typography.small, color = c.textMuted, modifier = Modifier.padding(top = 8.dp))
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            content = acoes,
        )
    }
}
