package br.app.divvup.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalDivvupColors = staticCompositionLocalOf { LightDivvupColors }
private val LocalDivvupTypography = staticCompositionLocalOf { DivvupTypography() }
private val LocalDivvupShapes = staticCompositionLocalOf { DivvupShapes() }
private val LocalDivvupSpacing = staticCompositionLocalOf { DivvupSpacing() }

/**
 * Tema do app. Cores dinâmicas (Material You) desligadas: as cores da marca são fixas.
 * Os tokens vêm do estilo das telas (`docs/design/README.md` › Tokens).
 */
@Composable
fun DivvupTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkDivvupColors else LightDivvupColors
    val typography = DivvupTypography()
    CompositionLocalProvider(
        LocalDivvupColors provides colors,
        LocalDivvupTypography provides typography,
        LocalDivvupShapes provides DivvupShapes(),
        LocalDivvupSpacing provides DivvupSpacing(),
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = MaterialTheme.typography.copy(
                bodyLarge = typography.body,
                bodyMedium = typography.small,
                labelLarge = typography.ttl,
            ),
            content = content,
        )
    }
}

/** Acesso aos tokens: `DivvupTheme.colors.primary`, `DivvupTheme.typography.h1`… */
object DivvupTheme {
    val colors: DivvupColors
        @Composable @ReadOnlyComposable get() = LocalDivvupColors.current
    val typography: DivvupTypography
        @Composable @ReadOnlyComposable get() = LocalDivvupTypography.current
    val shapes: DivvupShapes
        @Composable @ReadOnlyComposable get() = LocalDivvupShapes.current
    val spacing: DivvupSpacing
        @Composable @ReadOnlyComposable get() = LocalDivvupSpacing.current
}

// Para componentes Material que ainda forem usados por dentro (ex.: campos de texto).
private fun DivvupColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryBg,
        onPrimaryContainer = primary,
        secondary = accent,
        onSecondary = onAccent,
        tertiary = link,
        background = background,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = surfaceAlt,
        onSurfaceVariant = textMuted,
        surfaceContainer = surface,
        surfaceContainerLow = surface,
        surfaceContainerHigh = surface,
        outline = border,
        outlineVariant = border,
        error = danger,
        onError = onDanger,
        errorContainer = dangerBg,
        onErrorContainer = danger,
        inverseSurface = snack,
        inverseOnSurface = snackText,
        scrim = scrim,
    )
}
