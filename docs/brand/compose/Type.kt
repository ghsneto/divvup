package br.app.divvup.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import br.app.divvup.core.designsystem.R

// Bricolage Grotesque (Google Fonts, OFL-1.1), arquivo variável em res/font/bricolage_grotesque.ttf.
// Títulos: wght 800, opsz 96, wdth 90, espaçamento -30/1000 em.
// Texto: wght 400/600, opsz 14, wdth 100.
// Os eixos variáveis precisam de Android 8.0 (API 26) ou superior.

@OptIn(ExperimentalTextApi::class)
private fun bricolage(weight: Int, opsz: Float, wdth: Float) = Font(
    resId = R.font.bricolage_grotesque,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opsz),
        FontVariation.width(wdth),
    ),
)

val BricolageDisplay = FontFamily(bricolage(800, 96f, 90f))

val BricolageText = FontFamily(
    bricolage(400, 14f, 100f),
    bricolage(600, 14f, 100f),
)

private val displayTracking = (-0.03).em

val DivvupTypography = Typography(
    displayLarge = TextStyle(fontFamily = BricolageDisplay, fontWeight = FontWeight(800), fontSize = 57.sp, lineHeight = 60.sp, letterSpacing = displayTracking),
    displayMedium = TextStyle(fontFamily = BricolageDisplay, fontWeight = FontWeight(800), fontSize = 45.sp, lineHeight = 48.sp, letterSpacing = displayTracking),
    displaySmall = TextStyle(fontFamily = BricolageDisplay, fontWeight = FontWeight(800), fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = displayTracking),
    headlineLarge = TextStyle(fontFamily = BricolageDisplay, fontWeight = FontWeight(800), fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = displayTracking),
    headlineMedium = TextStyle(fontFamily = BricolageDisplay, fontWeight = FontWeight(800), fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = displayTracking),
    headlineSmall = TextStyle(fontFamily = BricolageDisplay, fontWeight = FontWeight(800), fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = displayTracking),
    titleLarge = TextStyle(fontFamily = BricolageText, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = BricolageText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = BricolageText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = BricolageText, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = BricolageText, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = BricolageText, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = BricolageText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = BricolageText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = BricolageText, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp),
)

// Para pontos e ranking: números tabulares (todos com a mesma largura), assim o placar não "pula".
val PontosStyle = TextStyle(
    fontFamily = BricolageText,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 24.sp,
    fontFeatureSettings = "tnum",
)
