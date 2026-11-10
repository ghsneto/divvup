package br.app.divvup.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme

/**
 * Campo `.field` + `.in`: rótulo em caixa-alta, caixa de 52dp, raio 4, borda `border`
 * (`danger` com erro), texto 16sp, e mensagem `.errmsg` com ícone de alerta.
 */
@Composable
fun DivvupTextField(
    valor: String,
    onValorMudou: (String) -> Unit,
    modifier: Modifier = Modifier,
    rotulo: String? = null,
    placeholder: String? = null,
    erro: String? = null,
    habilitado: Boolean = true,
    linhaUnica: Boolean = true,
    transformacao: VisualTransformation = VisualTransformation.None,
    teclado: KeyboardOptions = KeyboardOptions.Default,
    acoesTeclado: KeyboardActions = KeyboardActions.Default,
    final: (@Composable () -> Unit)? = null,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val shape = DivvupTheme.shapes.sm
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rotulo?.let { DivvupLabel(it) }
        BasicTextField(
            value = valor,
            onValueChange = onValorMudou,
            enabled = habilitado,
            singleLine = linhaUnica,
            textStyle = t.body.copy(color = c.text),
            cursorBrush = SolidColor(c.primary),
            visualTransformation = transformacao,
            keyboardOptions = teclado,
            keyboardActions = acoesTeclado,
            decorationBox = { campo ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(shape)
                        .background(c.surface, shape)
                        .border(DivvupTheme.spacing.borda, if (erro != null) c.danger else c.border, shape)
                        .padding(start = 16.dp, end = if (final != null) 4.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (valor.isEmpty() && placeholder != null) {
                            Text(placeholder, style = t.body, color = c.textMuted, maxLines = 1)
                        }
                        campo()
                    }
                    final?.invoke()
                }
            },
        )
        erro?.let { MensagemErro(it) }
    }
}

/** `.errmsg`: ícone de alerta 16 + texto 14sp/600 em `danger`. */
@Composable
fun MensagemErro(texto: String, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        DivvupIcon(DivvupIcone.Alerta, tamanho = 16.dp, tint = DivvupTheme.colors.danger)
        Text(texto, style = DivvupTheme.typography.botao, color = DivvupTheme.colors.danger)
    }
}

/** Interruptor `.tg`: 52×32 quadrado (raio 4), botão 24; ligado = `primary`. */
@Composable
fun DivvupToggle(
    ligado: Boolean,
    onMudou: (Boolean) -> Unit,
    descricao: String,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val shape = DivvupTheme.shapes.sm
    val deslocamento by animateDpAsState(if (ligado) 20.dp else 0.dp, tween(200), label = "tg")
    Box(
        modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(shape)
            .background(if (ligado) c.primary else c.surfaceAlt, shape)
            .border(DivvupTheme.spacing.borda, if (ligado) c.primary else c.border, shape)
            .toggleable(value = ligado, role = Role.Switch, onValueChange = onMudou)
            .semantics { contentDescription = descricao },
    ) {
        Box(
            Modifier
                .offset { IntOffset((3.dp + deslocamento).roundToPx(), 3.dp.roundToPx()) }
                .size(24.dp)
                .clip(shape)
                .background(if (ligado) c.onPrimary else c.textMuted),
        )
    }
}
