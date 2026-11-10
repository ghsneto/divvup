package br.app.divvup.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import br.app.divvup.core.designsystem.R

// Bricolage Grotesque variável (Google Fonts, OFL-1.1), res/font/bricolage_grotesque.ttf.
// Eixos: wght, opsz, wdth. Exige API 26 (minSdk do projeto).
private fun bricolage(weight: Int, opsz: Float, wdth: Float) = Font(
    resId = R.font.bricolage_grotesque,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opsz),
        FontVariation.width(wdth),
    ),
)

/** Títulos (`h1`, `h2` e números grandes): wght 800, opsz 96, wdth 90. */
val BricolageDisplay = FontFamily(bricolage(weight = 800, opsz = 96f, wdth = 90f))

/** Texto (`.dv`): opsz 14, wdth 100, pesos 400 e 600. */
val BricolageText = FontFamily(
    bricolage(weight = 400, opsz = 14f, wdth = 100f),
    bricolage(weight = 600, opsz = 14f, wdth = 100f),
    bricolage(weight = 800, opsz = 14f, wdth = 100f),
)

// Como no CSS: entrelinha dividida igualmente acima e abaixo, sem cortar.
private val CssLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun estilo(
    family: FontFamily,
    weight: Int,
    size: Int,
    lineHeight: Float,
    letterSpacing: Float = 0f,
    tnum: Boolean = false,
) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = lineHeight.em,
    letterSpacing = letterSpacing.em,
    lineHeightStyle = CssLineHeight,
    fontFeatureSettings = if (tnum) "tnum" else null,
)

/**
 * Escala tipográfica das telas (classes do estilo das telas, iguais ao `Type.kt` do manual).
 * `label` é exibido em caixa-alta: use `DivvupText`/componentes, que aplicam o uppercase.
 */
@Immutable
data class DivvupTypography(
    /** `.h1` 57/1.05, 800, −0,03em. Títulos de tela, em minúsculas. */
    val h1: TextStyle = estilo(BricolageDisplay, 800, 57, 1.05f, -0.03f),
    /** `.h2` 45/1.07, 800, −0,03em. */
    val h2: TextStyle = estilo(BricolageDisplay, 800, 45, 1.07f, -0.03f),
    /** `.h3` 22/1.27, 600. */
    val h3: TextStyle = estilo(BricolageText, 600, 22, 1.27f),
    /** `.ttl` 16/1.5, 600. */
    val ttl: TextStyle = estilo(BricolageText, 600, 16, 1.5f),
    /** `.body` / `.txt` 16/1.5, 400. */
    val body: TextStyle = estilo(BricolageText, 400, 16, 1.5f),
    /** `.small` 14/1.43, 400. */
    val small: TextStyle = estilo(BricolageText, 400, 14, 1.43f),
    /** Texto de botão e chip (`.btn`, `.chip`): 14/1.43, 600. */
    val botao: TextStyle = estilo(BricolageText, 600, 14, 1.43f),
    /** `.label` 12/1.33, 600, +0,12em, caixa-alta. */
    val label: TextStyle = estilo(BricolageText, 600, 12, 1.33f, 0.12f),
    /** `.pts` 20/1.2, 600, números tabulares. */
    val pts: TextStyle = estilo(BricolageText, 600, 20, 1.2f, tnum = true),
)
