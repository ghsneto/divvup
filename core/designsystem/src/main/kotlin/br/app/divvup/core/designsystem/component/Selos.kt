package br.app.divvup.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupColors
import br.app.divvup.core.designsystem.theme.DivvupTheme
import java.text.NumberFormat
import java.util.Locale

/**
 * Chip `.chip`: 40dp, pílula, borda; selecionado (`.on`) inverte para fundo `text`.
 * Sem `onClick` é etiqueta estática.
 */
@Composable
fun DivvupChip(
    texto: String,
    modifier: Modifier = Modifier,
    selecionado: Boolean = false,
    icone: DivvupIcone? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = DivvupTheme.colors
    val shape = DivvupTheme.shapes.pill
    val fundo = if (selecionado) c.text else c.surface
    val conteudo = if (selecionado) c.background else c.text
    val borda = if (selecionado) c.text else c.border
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(fundo, shape)
            .border(DivvupTheme.spacing.borda, borda, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Checkbox, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icone?.let { DivvupIcon(it, tamanho = TamanhoIcone.Pequeno.dp, tint = conteudo) }
        Text(texto, style = DivvupTheme.typography.botao, color = conteudo, maxLines = 1)
    }
}

/** Tons do selo de status `.st-*`. */
enum class TomSelo { Sucesso, Atencao, Erro, Info, Primario, Neutro }

private fun TomSelo.cores(c: DivvupColors): Pair<Color, Color> = when (this) {
    TomSelo.Sucesso -> c.successBg to c.success
    TomSelo.Atencao -> c.warningBg to c.warning
    TomSelo.Erro -> c.dangerBg to c.danger
    TomSelo.Info -> c.infoBg to c.info
    TomSelo.Primario -> c.primaryBg to c.primary
    TomSelo.Neutro -> c.surfaceAlt to c.text
}

/** Selo de status `.st`: 24dp, pílula, rótulo em caixa-alta, ícone 14. Sempre com palavra, nunca só cor. */
@Composable
fun DivvupStatusTag(
    texto: String,
    tom: TomSelo,
    modifier: Modifier = Modifier,
    icone: DivvupIcone? = null,
) {
    val (fundo, conteudo) = tom.cores(DivvupTheme.colors)
    Row(
        modifier = modifier
            .height(24.dp)
            .clip(DivvupTheme.shapes.pill)
            .background(fundo)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icone?.let { DivvupIcon(it, tamanho = TamanhoIcone.MuitoPequeno.dp, tint = conteudo) }
        DivvupLabel(texto, cor = conteudo, maxLinhas = 1)
    }
}

private val FormatoPtBr: NumberFormat = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"))

/** Número no formato brasileiro ("1.240"). */
fun formatarNumero(valor: Int): String = FormatoPtBr.format(valor)

/** Tamanhos do `.pb` usados nas telas: padrão, no cartão do deck (28) e na prévia de C2 (16). */
enum class TamanhoSelo(val fonte: TextUnit, val horizontal: Dp, val vertical: Dp) {
    Padrao(20.sp, 12.dp, 4.dp),
    Grande(28.sp, 16.dp, 8.dp),
    Pequeno(16.sp, 12.dp, 4.dp),
}

/** Selo de pontos `.pb`: lima, 20sp/600 com números tabulares e o sufixo ("pts") em 12sp. */
@Composable
fun DivvupPointsBadge(
    pontos: Int,
    sufixo: String,
    modifier: Modifier = Modifier,
    sinal: Boolean = false,
    tamanho: TamanhoSelo = TamanhoSelo.Padrao,
) {
    val c = DivvupTheme.colors
    val texto = (if (sinal && pontos > 0) "+" else "") + formatarNumero(pontos)
    Row(
        modifier = modifier
            .clip(DivvupTheme.shapes.pill)
            .background(c.accent)
            .padding(horizontal = tamanho.horizontal, vertical = tamanho.vertical),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            texto,
            style = DivvupTheme.typography.pts.copy(fontSize = tamanho.fonte, lineHeight = 1.1.em),
            color = c.onAccent,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            sufixo,
            style = DivvupTheme.typography.botao.copy(fontSize = 12.sp),
            color = c.onAccent,
            modifier = Modifier.alignByBaseline(),
        )
    }
}
