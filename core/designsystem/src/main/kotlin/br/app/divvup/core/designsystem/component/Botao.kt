package br.app.divvup.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupColors
import br.app.divvup.core.designsystem.theme.DivvupTheme

/** Variantes do `.btn` no estilo das telas. */
enum class VarianteBotao {
    /** `.btn-p`: ação principal, uma por tela. */
    Primario,

    /** `.btn-a`: lima, só para assumir/confirmar swipe. */
    Aceitar,

    /** `.btn-o`: contorno em `border` sobre `surface`. */
    Contorno,

    /** `.btn-d`: contorno em `danger` (passar a vez). */
    Perigo,

    /** `.btn-dd`: `danger` cheio (ação destrutiva confirmada). */
    PerigoCheio,

    /** `.btn-t`: só texto. */
    Texto,
}

private data class CoresBotao(val fundo: Color, val conteudo: Color, val borda: Color)

private fun cores(v: VarianteBotao, c: DivvupColors) = when (v) {
    VarianteBotao.Primario -> CoresBotao(c.primary, c.onPrimary, Color.Transparent)
    VarianteBotao.Aceitar -> CoresBotao(c.accent, c.onAccent, Color.Transparent)
    VarianteBotao.Contorno -> CoresBotao(c.surface, c.text, c.border)
    VarianteBotao.Perigo -> CoresBotao(Color.Transparent, c.danger, c.danger)
    VarianteBotao.PerigoCheio -> CoresBotao(c.danger, c.onDanger, Color.Transparent)
    VarianteBotao.Texto -> CoresBotao(Color.Transparent, c.text, Color.Transparent)
}

/** Opacidade de `.btn[disabled]`. */
private const val OPACIDADE_DESABILITADO = 0.42f

/** O "G" do botão "Continuar com Google" (16sp/600 em `primary`, como no desenho). */
@Composable
fun MarcaGoogle() {
    Text("G", style = DivvupTheme.typography.ttl, color = DivvupTheme.colors.primary)
}

/**
 * Botão `.btn`: mín. 48dp, raio 4, 14sp/600, ícone 24 com espaço 8.
 * `larguraTotal` = `.btn-w`.
 */
@Composable
fun DivvupButton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variante: VarianteBotao = VarianteBotao.Primario,
    icone: DivvupIcone? = null,
    iconeDepois: DivvupIcone? = null,
    inicio: (@Composable () -> Unit)? = null,
    habilitado: Boolean = true,
    larguraTotal: Boolean = false,
    corConteudo: Color? = null,
    carregando: Boolean = false,
) {
    val c = cores(variante, DivvupTheme.colors)
    val conteudo = corConteudo ?: c.conteudo
    val shape = DivvupTheme.shapes.sm
    val horizontal = if (variante == VarianteBotao.Texto) 8.dp else 20.dp
    Row(
        modifier = modifier
            .then(if (larguraTotal) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = 48.dp)
            .alpha(if (habilitado) 1f else OPACIDADE_DESABILITADO)
            .clip(shape)
            .background(c.fundo, shape)
            .border(DivvupTheme.spacing.borda, c.borda, shape)
            .clickable(enabled = habilitado && !carregando, role = Role.Button, onClick = onClick)
            .semantics { if (carregando) stateDescription = texto }
            .padding(PaddingValues(horizontal = horizontal, vertical = 12.dp)),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides conteudo) {
            if (carregando) {
                // `.shuttle` de 48dp com trilho branco a 30% (estado "carregando" de A3).
                DivvupProgressBar(Modifier.width(48.dp), trilho = conteudo.copy(alpha = 0.3f), barra = c.fundo)
                return@CompositionLocalProvider
            }
            inicio?.invoke()
            icone?.let { DivvupIcon(it) }
            Text(text = texto, style = DivvupTheme.typography.botao, color = conteudo)
            iconeDepois?.let { DivvupIcon(it) }
        }
    }
}
