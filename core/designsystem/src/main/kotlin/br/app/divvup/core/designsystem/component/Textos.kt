package br.app.divvup.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import br.app.divvup.core.designsystem.theme.DivvupTheme
import java.util.Locale

private val PtBr: Locale = Locale.forLanguageTag("pt-BR")

/** Rótulo `.label`: 12sp, 600, espaçado, em caixa-alta. */
@Composable
fun DivvupLabel(
    texto: String,
    modifier: Modifier = Modifier,
    cor: Color = DivvupTheme.colors.text,
    maxLinhas: Int = Int.MAX_VALUE,
) {
    Text(
        text = texto.uppercase(PtBr),
        modifier = modifier,
        style = DivvupTheme.typography.label,
        color = cor,
        maxLines = maxLinhas,
        overflow = TextOverflow.Ellipsis,
    )
}
