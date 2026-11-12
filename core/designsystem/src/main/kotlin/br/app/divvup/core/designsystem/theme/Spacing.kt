package br.app.divvup.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Espaçamentos em passos de 4dp (`space-1`…`space-8`). */
@Immutable
data class DivvupSpacing(
    val s1: Dp = 4.dp,
    val s2: Dp = 8.dp,
    val s3: Dp = 12.dp,
    val s4: Dp = 16.dp,
    val s5: Dp = 24.dp,
    val s6: Dp = 32.dp,
    val s7: Dp = 48.dp,
    val s8: Dp = 80.dp,
    /** Borda padrão (o design usa bordas de 1dp no lugar de sombras). */
    val borda: Dp = 1.dp,
    /** Área mínima de toque (TELAS.md). */
    val toqueMinimo: Dp = 48.dp,
    /** Altura do campo de texto `.in`. */
    val campo: Dp = 52.dp,
)
