package br.app.divvup.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    secondary = LightAccent,
    onSecondary = LightOnAccent,
    tertiary = LightLink,
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceAlt,
    onSurfaceVariant = LightTextMuted,
    outline = LightBorder,
    error = LightDanger,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    secondary = DarkAccent,
    onSecondary = DarkOnAccent,
    tertiary = DarkLink,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceAlt,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkBorder,
    error = DarkDanger,
    onError = DarkBackground,
)

/**
 * Cores do Divvup que o Material 3 não tem: as do swipe e os estados.
 *
 * aceitar (Lima Ácida) é para preenchimentos com texto onAceitar por cima.
 * Não use a lima como cor de texto ou ícone sobre fundo claro: não tem contraste.
 */
@Immutable
data class DivvupColors(
    val aceitar: Color,
    val onAceitar: Color,
    val passar: Color,
    val sucesso: Color,
    val atencao: Color,
    val info: Color,
    val link: Color,
)

private val LightDivvupColors = DivvupColors(
    aceitar = LightAccent,
    onAceitar = LightOnAccent,
    passar = LightDanger,
    sucesso = LightSuccess,
    atencao = LightWarning,
    info = LightInfo,
    link = LightLink,
)

private val DarkDivvupColors = DivvupColors(
    aceitar = DarkAccent,
    onAceitar = DarkOnAccent,
    passar = DarkDanger,
    sucesso = DarkSuccess,
    atencao = DarkWarning,
    info = DarkInfo,
    link = DarkLink,
)

private val LocalDivvupColors = staticCompositionLocalOf { LightDivvupColors }

@Composable
fun DivvupTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Sem cor dinâmica (Material You): as cores da marca ficam fixas.
    val colors = if (darkTheme) DarkColors else LightColors
    val divvupColors = if (darkTheme) DarkDivvupColors else LightDivvupColors

    CompositionLocalProvider(LocalDivvupColors provides divvupColors) {
        MaterialTheme(
            colorScheme = colors,
            typography = DivvupTypography,
            content = content,
        )
    }
}

/** Acesso às cores extras: DivvupTheme.colors.aceitar */
object DivvupTheme {
    val colors: DivvupColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDivvupColors.current
}
