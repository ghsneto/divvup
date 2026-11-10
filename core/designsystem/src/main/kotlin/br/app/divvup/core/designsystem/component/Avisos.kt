package br.app.divvup.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme

/**
 * Aviso em faixa (ex.: "Sem conexão" em A3): fundo `*-bg`, texto 14sp/600 e ícone 18 na cor do tom,
 * raio 4, padding 12.
 */
@Composable
fun DivvupAviso(
    texto: String,
    tom: TomSelo,
    icone: DivvupIcone,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val (fundo, cor) = when (tom) {
        TomSelo.Sucesso -> c.successBg to c.success
        TomSelo.Atencao -> c.warningBg to c.warning
        TomSelo.Erro -> c.dangerBg to c.danger
        TomSelo.Info -> c.infoBg to c.info
        TomSelo.Primario -> c.primaryBg to c.primary
        TomSelo.Neutro -> c.surfaceAlt to c.text
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(DivvupTheme.shapes.sm)
            .background(fundo)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DivvupIcon(icone, tamanho = TamanhoIcone.Pequeno.dp, tint = cor)
        Text(texto, style = DivvupTheme.typography.botao, color = cor)
    }
}

/** Caixa de seleção 22dp (raio 4); marcada = `primary` com check. */
@Composable
fun DivvupCheckbox(
    marcado: Boolean,
    onMudou: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val shape = DivvupTheme.shapes.sm
    Box(
        modifier
            .size(22.dp)
            .clip(shape)
            .background(if (marcado) c.primary else c.surface, shape)
            .border(DivvupTheme.spacing.borda, if (marcado) c.primary else c.textMuted, shape)
            .toggleable(value = marcado, role = Role.Checkbox, onValueChange = onMudou),
        contentAlignment = Alignment.Center,
    ) {
        if (marcado) DivvupIcon(DivvupIcone.Check, tamanho = 16.dp, tint = c.onPrimary)
    }
}

/** Trecho de texto: comum ou link (`a` do estilo das telas: cor `link`, sublinhado). */
sealed interface Trecho {
    data class Comum(val texto: String) : Trecho
    data class Link(
        val texto: String,
        val forte: Boolean = false,
        val cor: Color? = null,
        val aoClicar: () -> Unit,
    ) : Trecho
}

/** Monta um texto com links inline. Use os trechos vindos de `strings.xml`. */
@Composable
fun textoComLinks(trechos: List<Trecho>): AnnotatedString {
    val c = DivvupTheme.colors
    return buildAnnotatedString {
        trechos.forEach { t ->
            when (t) {
                is Trecho.Comum -> append(t.texto)
                is Trecho.Link -> {
                    val estilo = SpanStyle(
                        color = t.cor ?: c.link,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = if (t.forte) FontWeight.SemiBold else null,
                    )
                    withLink(LinkAnnotation.Clickable(t.texto, TextLinkStyles(estilo)) { t.aoClicar() }) { append(t.texto) }
                }
            }
        }
    }
}

/** Texto com links inline. */
@Composable
fun DivvupTextoComLinks(
    trechos: List<Trecho>,
    estilo: TextStyle,
    modifier: Modifier = Modifier,
    cor: Color = DivvupTheme.colors.text,
) {
    Text(textoComLinks(trechos), modifier = modifier, style = estilo, color = cor)
}

