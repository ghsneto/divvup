package br.app.divvup.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Cores do Divvup. Valores exatos do estilo das telas (`docs/design/assets/f25551….css`, blocos
 * `.dv` e `.dv.escuro`). Use sempre por `DivvupTheme.colors`; nunca `Color(0x…)` nas telas.
 *
 * Regra da marca: `accent` (lima) é preenchimento com `onAccent` por cima; nunca texto ou ícone
 * sobre fundo claro. Swipe aceitar = `accent`, passar = `danger`.
 */
@Immutable
data class DivvupColors(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val text: Color,
    val textMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val onAccent: Color,
    val link: Color,
    val focus: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val onDanger: Color,
    val successBg: Color,
    val warningBg: Color,
    val dangerBg: Color,
    val infoBg: Color,
    val primaryBg: Color,
    val snack: Color,
    val snackText: Color,
    val scrim: Color,
    /** Cores fixas da marca, iguais nos dois temas. */
    val ultramarino: Color,
    val lima: Color,
    val tinta: Color,
    /** Texto e marca sobre `ultramarino` (branco nos dois temas, ex.: faixa de A2). */
    val onUltramarino: Color,
    /** Papel frio fixo (#F5F5F0), usado em ilustrações sobre ultramarino nos dois temas. */
    val papel: Color,
    /** Superfícies fixas sobre `tinta` nos dois temas (cartão do código em B8 e C1 "conflito"). */
    val sobreTinta: CoresSobreTinta = CoresSobreTinta(),
    val isDark: Boolean,
) {
    /** Swipe à direita (assumir). */
    val aceitar: Color get() = accent
    val onAceitar: Color get() = onAccent

    /** Swipe à esquerda (passar a vez). */
    val passar: Color get() = danger
}

/** Texto, texto secundário, caixas e linhas sobre o fundo `tinta`, iguais nos dois temas. */
@Immutable
data class CoresSobreTinta(
    val texto: Color = Color(0xFFF2F3F5),
    val textoMudo: Color = Color(0xFF9EA1AA),
    val caixa: Color = Color(0xFF272B36),
    val linha: Color = Color(0xFF3D414D),
)

private val Ultramarino = Color(0xFF2F2BD9)
private val Lima = Color(0xFFC9F24D)
private val Tinta = Color(0xFF0C0F18)

internal val LightDivvupColors = DivvupColors(
    background = Color(0xFFF5F5F0),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEBEBE6),
    border = Color(0xFFD4D4CC),
    text = Color(0xFF181B26),
    textMuted = Color(0xFF515561),
    primary = Ultramarino,
    onPrimary = Color(0xFFFFFFFF),
    accent = Lima,
    onAccent = Tinta,
    link = Color(0xFF617007),
    focus = Ultramarino,
    success = Color(0xFF08795E),
    warning = Color(0xFF8F5A06),
    danger = Color(0xFFB4312B),
    info = Color(0xFF196AAF),
    onDanger = Color(0xFFFFFFFF),
    successBg = Color(0xFFD6F8EB),
    warningBg = Color(0xFFFFEBD6),
    dangerBg = Color(0xFFFFE9E6),
    infoBg = Color(0xFFE3F0FF),
    primaryBg = Color(0xFFE9EEFF),
    snack = Color(0xFF181B26),
    snackText = Color(0xFFF2F3F5),
    scrim = Color(0x7A0C0F18),
    ultramarino = Ultramarino,
    lima = Lima,
    tinta = Tinta,
    onUltramarino = Color(0xFFFFFFFF),
    papel = Color(0xFFF5F5F0),
    isDark = false,
)

internal val DarkDivvupColors = DivvupColors(
    background = Color(0xFF0C0F18),
    surface = Color(0xFF181B26),
    surfaceAlt = Color(0xFF272B36),
    border = Color(0xFF3D414D),
    text = Color(0xFFF2F3F5),
    textMuted = Color(0xFF9EA1AA),
    primary = Color(0xFF7590FD),
    onPrimary = Color(0xFF0C0F18),
    accent = Lima,
    onAccent = Tinta,
    link = Color(0xFF7590FD),
    focus = Color(0xFF7590FD),
    success = Color(0xFF3FC6A0),
    warning = Color(0xFFE59B37),
    danger = Color(0xFFFE8477),
    info = Color(0xFF66B2FD),
    onDanger = Color(0xFF0C0F18),
    successBg = Color(0xFF073025),
    warningBg = Color(0xFF3A2305),
    dangerBg = Color(0xFF471713),
    infoBg = Color(0xFF0C2A46),
    primaryBg = Color(0xFF182259),
    snack = Color(0xFF272B36),
    snackText = Color(0xFFF2F3F5),
    scrim = Color(0x99000000),
    ultramarino = Ultramarino,
    lima = Lima,
    tinta = Tinta,
    onUltramarino = Color(0xFFFFFFFF),
    papel = Color(0xFFF5F5F0),
    isDark = true,
)
