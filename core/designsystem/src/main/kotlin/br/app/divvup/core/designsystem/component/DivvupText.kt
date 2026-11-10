package br.app.divvup.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnitType
import br.app.divvup.core.designsystem.theme.DivvupTheme
import kotlin.math.roundToInt

/**
 * Altura natural da Bricolage Grotesque: ascendente 930 + descendente 270 por 1000 unidades
 * (tabelas hhea/typo da fonte).
 */
private const val ALTURA_NATURAL_EM = 1.2f

/**
 * Texto do Divvup. Igual ao `Text`, mas reproduz a altura de linha do CSS quando ela é menor que a
 * altura natural da fonte (`h1` 1,05 e `h2` 1,07): o Android aplica a altura pedida entre as linhas,
 * mas mantém a altura natural nas bordas de cima e de baixo. Aqui a sobra (1,2em − altura) é
 * descontada, metade em cima e metade embaixo, e a caixa fica igual à do desenho.
 * Use para títulos; para os demais estilos o resultado é o mesmo do `Text`.
 */
@Composable
fun DivvupText(
    texto: String,
    estilo: TextStyle,
    modifier: Modifier = Modifier,
    cor: Color = DivvupTheme.colors.text,
    alinhamento: TextAlign? = null,
    maxLinhas: Int = Int.MAX_VALUE,
) {
    val altura = estilo.lineHeight
    val sobraEm = if (altura.type == TextUnitType.Em) ALTURA_NATURAL_EM - altura.value else 0f
    val ajuste = if (sobraEm > 0f) {
        Modifier.layout { medida, restricoes ->
            val p = medida.measure(restricoes)
            val sobra = (sobraEm * estilo.fontSize.toPx()).roundToInt()
            layout(p.width, (p.height - sobra).coerceAtLeast(0)) { p.place(0, -sobra / 2) }
        }
    } else {
        Modifier
    }
    Text(
        text = texto,
        modifier = modifier.then(ajuste),
        style = estilo,
        color = cor,
        textAlign = alinhamento,
        maxLines = maxLinhas,
        overflow = TextOverflow.Ellipsis,
    )
}
