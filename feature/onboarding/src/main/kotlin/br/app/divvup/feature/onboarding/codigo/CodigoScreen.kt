package br.app.divvup.feature.onboarding.codigo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.MensagemErro
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.casa.ErroDeConvite
import br.app.divvup.core.domain.casa.TAMANHO_CODIGO
import br.app.divvup.feature.onboarding.OnboardingMedidas
import br.app.divvup.feature.onboarding.R
import br.app.divvup.feature.onboarding.comum.ProgressoOnboarding

/** B4 Entrar com código (docs/design/fonte/Codigo.dc.html, R-06). */
@Composable
fun CodigoScreen(
    estado: CodigoEstado,
    aoVoltar: () -> Unit,
    aoDigitar: (String) -> Unit,
    aoColar: () -> Unit,
    aoEntrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(modifier.fillMaxSize().background(c.background)) {
        ProgressoOnboarding(passo = 3, aoVoltar = aoVoltar)
        Column(
            Modifier
                .weight(1f)
                .padding(start = e.s4, end = e.s4, top = OnboardingMedidas.casaConteudoTopo),
        ) {
            DivvupLabel(stringResource(R.string.codigo_rotulo), cor = c.textMuted)
            DivvupText(stringResource(R.string.codigo_titulo), t.h1)
            Text(
                stringResource(R.string.codigo_texto),
                style = t.body,
                color = c.textMuted,
                modifier = Modifier.padding(top = e.s3).widthIn(max = OnboardingMedidas.codigoTextoMaximo),
            )
            CaixasDoCodigo(estado, aoDigitar, Modifier.padding(top = OnboardingMedidas.campoTopo))
            estado.erro?.let { Erro(it, Modifier.padding(top = e.s4)) }
            DivvupButton(
                stringResource(R.string.codigo_colar),
                aoColar,
                variante = VarianteBotao.Contorno,
                icone = DivvupIcone.Copiar,
                modifier = Modifier.padding(top = e.s4),
            )
            Spacer(Modifier.weight(1f))
            DivvupButton(
                stringResource(R.string.codigo_entrar),
                aoEntrar,
                larguraTotal = true,
                habilitado = estado.completo,
                carregando = estado.entrando,
                modifier = Modifier.padding(bottom = OnboardingMedidas.rodape),
            )
        }
    }
}

/** 6 caixas sobre um campo invisível (o teclado digita no campo; as caixas mostram). */
@Composable
private fun CaixasDoCodigo(estado: CodigoEstado, aoDigitar: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    val descricao = stringResource(R.string.codigo_campo)
    val ruim = estado.erro != null && estado.erro != ErroDeConvite.SemConexao && estado.completo
    BasicTextField(
        value = estado.codigo,
        onValueChange = aoDigitar,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        modifier = modifier.fillMaxWidth().semantics { contentDescription = descricao },
        decorationBox = { campo ->
            Box {
                Box(Modifier.alpha(0f)) { campo() }
                Row(horizontalArrangement = Arrangement.spacedBy(e.s2)) {
                    repeat(TAMANHO_CODIGO) { i ->
                        val letra = estado.codigo.getOrNull(i)
                        val ativa = i == minOf(estado.codigo.length, TAMANHO_CODIGO - 1) && estado.codigo.length < TAMANHO_CODIGO
                        val borda = when {
                            ruim -> c.danger
                            ativa -> c.primary
                            else -> c.border
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .height(OnboardingMedidas.caixaCodigo)
                                .clip(DivvupTheme.shapes.sm)
                                .background(if (letra != null) c.surface else c.surfaceAlt)
                                .border(e.borda, borda, DivvupTheme.shapes.sm),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                letra?.toString().orEmpty(),
                                style = DivvupTheme.typography.pts.copy(fontSize = OnboardingMedidas.caixaCodigoFonte, fontWeight = FontWeight.SemiBold),
                                color = c.text,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun Erro(erro: ErroDeConvite, modifier: Modifier = Modifier) {
    val (texto, mais) = when (erro) {
        ErroDeConvite.Invalido -> R.string.codigo_invalido to R.string.codigo_invalido_mais
        ErroDeConvite.Expirado -> R.string.codigo_expirado to R.string.codigo_expirado_mais
        ErroDeConvite.Usado -> R.string.codigo_usado to R.string.codigo_usado_mais
        ErroDeConvite.JaEmCasa -> R.string.codigo_ja_em_casa to R.string.codigo_ja_em_casa_mais
        else -> R.string.codigo_sem_conexao to null
    }
    Column(modifier) {
        MensagemErro(stringResource(texto))
        mais?.let {
            Text(
                stringResource(it),
                style = DivvupTheme.typography.small,
                color = DivvupTheme.colors.textMuted,
                modifier = Modifier.padding(top = DivvupTheme.spacing.s1),
            )
        }
    }
}
