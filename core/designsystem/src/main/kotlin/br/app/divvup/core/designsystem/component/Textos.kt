package br.app.divvup.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import br.app.divvup.core.designsystem.theme.DivvupTheme
import java.util.Locale

private val PtBr: Locale = Locale.forLanguageTag("pt-BR")

/**
 * Rótulo `.label`: 12sp, 600, espaçado, em caixa-alta.
 *
 * `soltoEmBloco`: no desenho, um `<span class="label">` direto dentro de um bloco que não é flex
 * (ex.: `.scroll` de A4) fica numa linha com a altura da fonte do bloco (16px, linha de 19,2) e
 * desce ~2,9dp. Use `true` só nesses casos, para a posição bater com o desenho.
 */
@Composable
fun DivvupLabel(
    texto: String,
    modifier: Modifier = Modifier,
    cor: Color = DivvupTheme.colors.text,
    maxLinhas: Int = Int.MAX_VALUE,
    soltoEmBloco: Boolean = false,
) {
    Text(
        text = texto.uppercase(PtBr),
        modifier = if (soltoEmBloco) modifier.padding(top = 2.94.dp, bottom = 0.3.dp) else modifier,
        style = DivvupTheme.typography.label,
        color = cor,
        maxLines = maxLinhas,
        overflow = TextOverflow.Ellipsis,
    )
}
