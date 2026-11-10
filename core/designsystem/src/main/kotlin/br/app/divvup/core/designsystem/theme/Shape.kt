package br.app.divvup.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Raios do design system: controles quase retos, cartões 12, sheet 24, pílulas. */
@Immutable
data class DivvupShapes(
    /** `radius-sm` 4dp: botões, campos, toggle, FAB, snackbar. */
    val sm: Shape = RoundedCornerShape(4.dp),
    /** `radius-md` 12dp: cartões, diálogos, miniaturas. */
    val md: Shape = RoundedCornerShape(12.dp),
    /** `radius-lg` 24dp no topo: bottom sheet. */
    val sheet: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    /** `radius-pill`: chips, selos, avatares, contadores. */
    val pill: Shape = RoundedCornerShape(percent = 50),
)
