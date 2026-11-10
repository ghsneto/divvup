package br.app.divvup.feature.onboarding.casa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import br.app.divvup.core.designsystem.component.DivvupCard
import br.app.divvup.core.designsystem.component.DivvupDivider
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupStatusTag
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.TomSelo
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.feature.onboarding.OnboardingMedidas
import br.app.divvup.feature.onboarding.R
import br.app.divvup.feature.onboarding.comum.ProgressoOnboarding

/**
 * B2 Sua casa (docs/design/fonte/OnbCasa.dc.html, R-05). "Procurar uma vaga" aparece com o selo
 * v1.1, como no desenho, mas não abre nada até a v1.1.
 */
@Composable
fun SuaCasaScreen(
    primeiroNome: String,
    aoVoltar: () -> Unit,
    aoCriar: () -> Unit,
    aoTenhoCodigo: () -> Unit,
    aoSair: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(modifier.fillMaxSize().background(c.background)) {
        ProgressoOnboarding(passo = 2, aoVoltar = aoVoltar)
        Column(
            Modifier
                .weight(1f)
                .padding(start = e.s4, end = e.s4, top = OnboardingMedidas.casaConteudoTopo),
        ) {
            if (primeiroNome.isNotBlank()) {
                DivvupLabel(stringResource(R.string.casa_oi, primeiroNome), cor = c.textMuted)
            }
            DivvupText(stringResource(R.string.casa_titulo), t.h1)
            CartaoCriar(aoCriar, Modifier.padding(top = OnboardingMedidas.cartaoCriarTopo))
            CartaoCodigo(aoTenhoCodigo, Modifier.padding(top = e.s3))
            Procurar(Modifier.padding(top = OnboardingMedidas.procurarTopo))
            Spacer(Modifier.weight(1f))
            Sair(aoSair, Modifier.padding(bottom = OnboardingMedidas.sairFundo))
        }
    }
}

/** Cartão ultramarino "criar uma casa" com a seta lima no canto. */
@Composable
private fun CartaoCriar(aoCriar: () -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = OnboardingMedidas.cartaoCriarAltura)
            .clip(DivvupTheme.shapes.md)
            .background(c.ultramarino)
            .clickable(role = Role.Button, onClick = aoCriar),
    ) {
        Column(Modifier.padding(OnboardingMedidas.cartaoCriarPadding)) {
            DivvupLabel(stringResource(R.string.casa_criar_rotulo), cor = c.lima, soltoEmBloco = true)
            DivvupText(
                stringResource(R.string.casa_criar_titulo),
                t.h2.copy(fontSize = OnboardingMedidas.cartaoCriarTitulo, lineHeight = ALTURA_TITULO_CARTAO.em),
                Modifier.padding(top = e.s2),
                cor = c.onUltramarino,
            )
            Text(
                stringResource(R.string.casa_criar_texto),
                style = t.small,
                color = c.onUltramarino,
                modifier = Modifier.padding(top = e.s3).widthIn(max = OnboardingMedidas.cartaoCriarTextoMaximo),
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(e.s4)
                .size(OnboardingMedidas.cartaoCriarSeta)
                .clip(DivvupTheme.shapes.sm)
                .background(c.lima),
            contentAlignment = Alignment.Center,
        ) {
            DivvupIcon(DivvupIcone.SetaR, tint = c.tinta)
        }
    }
}

/** Cartão "tenho um código" (→ B4). */
@Composable
private fun CartaoCodigo(aoAbrir: () -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    DivvupCard(modifier.fillMaxWidth().clickable(role = Role.Button, onClick = aoAbrir)) {
        Row(
            Modifier.padding(horizontal = e.s4, vertical = OnboardingMedidas.cartaoCodigoPaddingV),
            horizontalArrangement = Arrangement.spacedBy(e.s4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.casa_codigo_exemplo),
                style = t.pts.copy(fontSize = OnboardingMedidas.codigoFonte, letterSpacing = ESPACO_CODIGO.em),
                color = c.text,
                modifier = Modifier
                    .clip(DivvupTheme.shapes.sm)
                    .background(c.surfaceAlt)
                    .padding(horizontal = OnboardingMedidas.codigoPaddingH, vertical = OnboardingMedidas.codigoPaddingV)
                    .clearAndSetSemantics { },
            )
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.casa_codigo_titulo), style = t.ttl, color = c.text)
                Text(stringResource(R.string.casa_codigo_texto), style = t.small, color = c.textMuted)
            }
            DivvupIcon(DivvupIcone.ChevR, tint = c.textMuted)
        }
    }
}

/** "Procurar uma vaga" (v1.1): visível com o selo, sem ação na v1.0. */
@Composable
private fun Procurar(modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    val descricao = stringResource(R.string.casa_procurar_descricao)
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = descricao }) {
        Row(
            Modifier.padding(horizontal = e.s1, vertical = e.s4),
            horizontalArrangement = Arrangement.spacedBy(e.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DivvupIcon(DivvupIcone.Local, tint = c.text)
            Text(
                stringResource(R.string.casa_procurar),
                style = DivvupTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = c.text,
                modifier = Modifier.weight(1f),
            )
            DivvupStatusTag(stringResource(R.string.casa_versao_11), TomSelo.Primario)
        }
        DivvupDivider()
    }
}

/** `.btn-t` "Sair da conta" sem recuo à esquerda, em `text-muted`. */
@Composable
private fun Sair(aoSair: () -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    Row(
        modifier
            .heightIn(min = DivvupTheme.spacing.toqueMinimo)
            .clickable(role = Role.Button, onClick = aoSair),
        horizontalArrangement = Arrangement.spacedBy(DivvupTheme.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DivvupIcon(DivvupIcone.Sair, tamanho = TamanhoIcone.Pequeno.dp, tint = c.textMuted)
        Text(stringResource(R.string.casa_sair), style = DivvupTheme.typography.botao, color = c.textMuted)
    }
}

/** `line-height: 1.05` do título do cartão. */
private const val ALTURA_TITULO_CARTAO = 1.05f

/** `letter-spacing: .12em` do código de exemplo. */
private const val ESPACO_CODIGO = 0.12f
