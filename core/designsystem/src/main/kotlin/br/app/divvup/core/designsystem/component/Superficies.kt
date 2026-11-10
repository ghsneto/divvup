package br.app.divvup.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.app.divvup.core.designsystem.theme.DivvupTheme

/** Cartão `.card`: `surface`, borda 1dp `border`, raio 12. */
@Composable
fun DivvupCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = DivvupTheme.shapes.md
    Box(
        modifier = modifier
            .clip(shape)
            .background(DivvupTheme.colors.surface, shape)
            .border(DivvupTheme.spacing.borda, DivvupTheme.colors.border, shape),
        content = content,
    )
}

/** Linha divisória `.rule`: 1dp em `border`. */
@Composable
fun DivvupDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(DivvupTheme.spacing.borda).background(DivvupTheme.colors.border))
}

/** Avatar `.av`: círculo com fundo `surface-alt`; `imagem` nula mostra só o fundo. */
@Composable
fun DivvupAvatar(
    imagem: Painter?,
    tamanho: Dp,
    modifier: Modifier = Modifier,
    descricao: String? = null,
) {
    Box(
        modifier
            .size(tamanho)
            .clip(DivvupTheme.shapes.pill)
            .background(DivvupTheme.colors.surfaceAlt),
    ) {
        if (imagem != null) {
            Image(imagem, descricao, Modifier.size(tamanho), contentScale = ContentScale.Crop)
        }
    }
}

/** Bloco de carregamento `.skel`: `surface-alt`, raio 4. */
@Composable
fun DivvupSkeleton(modifier: Modifier = Modifier) {
    Box(modifier.clip(DivvupTheme.shapes.sm).background(DivvupTheme.colors.surfaceAlt))
}

/** Barra de progresso indeterminada `.shuttle`: trilho 4dp e segmento de 30% em `primary` (1,1 s). */
@Composable
fun DivvupProgressBar(
    modifier: Modifier = Modifier,
    trilho: Color = DivvupTheme.colors.surfaceAlt,
    barra: Color = DivvupTheme.colors.primary,
) {
    // Em previews e screenshots (modo de inspeção) a barra fica parada no início: uma animação
    // infinita impediria a captura.
    val progresso = if (LocalInspectionMode.current) {
        0f
    } else {
        val transicao = rememberInfiniteTransition(label = "shuttle")
        val animado by transicao.animateFloat(
            initialValue = -1.2f,
            targetValue = 3.2f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 1100, easing = LinearEasing), RepeatMode.Restart),
            label = "posicao",
        )
        animado
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(DivvupTheme.shapes.sm)
            .background(trilho),
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.3f)
                .fillMaxHeight()
                .graphicsLayer { translationX = size.width * progresso }
                .clip(DivvupTheme.shapes.sm)
                .background(barra),
        )
    }
}
