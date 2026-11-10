package br.app.divvup.feature.auth.recuperar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupTextField
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.feature.auth.AuthMedidas
import br.app.divvup.feature.auth.R
import br.app.divvup.feature.auth.comum.BarraVoltar
import br.app.divvup.feature.auth.comum.mensagem

@Composable
fun RecuperarSenhaRota(aoVoltar: () -> Unit, viewModel: RecuperarSenhaViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    RecuperarSenhaScreen(
        estado = estado,
        aoMudarEmail = viewModel::aoMudarEmail,
        aoEnviar = viewModel::enviar,
        aoUsarOutroEmail = viewModel::usarOutroEmail,
        aoVoltar = aoVoltar,
    )
}

/** A5 Recuperar senha (docs/design/fonte/RecuperarSenha.dc.html). */
@Composable
fun RecuperarSenhaScreen(
    estado: RecuperarSenhaEstado,
    aoMudarEmail: (String) -> Unit,
    aoEnviar: () -> Unit,
    aoUsarOutroEmail: () -> Unit,
    aoVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    Column(modifier.fillMaxSize().background(c.background)) {
        BarraVoltar(aoVoltar)
        Column(
            Modifier
                .weight(1f)
                .padding(start = DivvupTheme.spacing.s4, end = DivvupTheme.spacing.s4, top = AuthMedidas.conteudoTopoFormulario),
        ) {
            if (estado.enviado) Enviado(estado.email, aoVoltar, aoUsarOutroEmail) else Formulario(estado, aoMudarEmail, aoEnviar)
        }
    }
}

@Composable
private fun ColumnScope.Formulario(estado: RecuperarSenhaEstado, aoMudarEmail: (String) -> Unit, aoEnviar: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    DivvupLabel(stringResource(R.string.recuperar_rotulo), cor = c.textMuted)
    DivvupText(stringResource(R.string.recuperar_titulo), estilo = t.h1)
    Text(
        stringResource(R.string.recuperar_texto),
        style = t.body,
        color = c.textMuted,
        modifier = Modifier
            .padding(top = DivvupTheme.spacing.s4, bottom = AuthMedidas.abaixoTituloEntrar)
            .widthIn(max = AuthMedidas.recuperarTextoMaximo),
    )
    DivvupTextField(
        valor = estado.email,
        onValorMudou = aoMudarEmail,
        rotulo = stringResource(R.string.campo_email),
        erro = estado.erro?.let { stringResource(it.mensagem()) },
        teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
        acoesTeclado = KeyboardActions(onSend = { aoEnviar() }),
    )
    DivvupButton(
        stringResource(R.string.recuperar_botao),
        aoEnviar,
        icone = DivvupIcone.Email,
        larguraTotal = true,
        carregando = estado.carregando,
        modifier = Modifier.padding(top = AuthMedidas.termosTopo),
    )
}

@Composable
private fun ColumnScope.Enviado(email: String, aoVoltar: () -> Unit, aoUsarOutroEmail: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    Envelope(Modifier.padding(top = DivvupTheme.spacing.s2, bottom = AuthMedidas.abaixoTituloEntrar))
    DivvupLabel(stringResource(R.string.recuperar_enviado_rotulo), cor = c.textMuted)
    DivvupText(stringResource(R.string.recuperar_enviado_titulo), estilo = t.h1)
    val modelo = stringResource(R.string.recuperar_enviado_texto, email)
    val inicio = modelo.indexOf(email)
    Text(
        buildAnnotatedString {
            append(modelo.substring(0, inicio))
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(email) }
            append(modelo.substring(inicio + email.length))
        },
        style = t.body,
        color = c.text,
        modifier = Modifier.padding(top = DivvupTheme.spacing.s4),
    )
    Text(
        stringResource(R.string.recuperar_enviado_spam),
        style = t.small,
        color = c.textMuted,
        modifier = Modifier.padding(top = DivvupTheme.spacing.s2),
    )
    Spacer(Modifier.weight(1f))
    Column(
        Modifier.padding(bottom = AuthMedidas.rodapeFormulario),
        verticalArrangement = Arrangement.spacedBy(DivvupTheme.spacing.s2),
    ) {
        DivvupButton(stringResource(R.string.recuperar_voltar), aoVoltar, larguraTotal = true)
        DivvupButton(stringResource(R.string.recuperar_outro_email), aoUsarOutroEmail, variante = VarianteBotao.Texto, larguraTotal = true)
    }
}

/** Envelope ultramarino 200×132 (raio 12) com o "V" em papel e o selo lima de 52 com check. */
@Composable
private fun Envelope(modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    Box(modifier.size(AuthMedidas.envelopeLargura, AuthMedidas.envelopeAltura)) {
        Canvas(
            Modifier
                .size(AuthMedidas.envelopeLargura, AuthMedidas.envelopeAltura)
                .clip(DivvupTheme.shapes.md)
                .background(c.ultramarino),
        ) {
            // Do desenho: "M0 12 L100 76 L200 12" num quadro de 200×132.
            val escala = size.width / LARGURA_ENVELOPE
            val v = Path().apply {
                moveTo(0f, V_TOPO * escala)
                lineTo(size.width / 2, V_FUNDO * escala)
                lineTo(size.width, V_TOPO * escala)
            }
            drawPath(v, c.papel, style = Stroke(width = AuthMedidas.envelopeTraco.toPx()))
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = AuthMedidas.seloEnviadoForaX, y = -AuthMedidas.seloEnviadoForaY)
                .size(AuthMedidas.seloEnviado)
                .clip(DivvupTheme.shapes.pill)
                .background(c.lima),
            contentAlignment = Alignment.Center,
        ) {
            DivvupIcon(DivvupIcone.Check, tint = c.tinta)
        }
    }
}

private const val LARGURA_ENVELOPE = 200f
private const val V_TOPO = 12f
private const val V_FUNDO = 76f
