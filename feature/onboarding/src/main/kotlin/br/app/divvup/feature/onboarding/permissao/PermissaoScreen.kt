package br.app.divvup.feature.onboarding.permissao

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupCard
import br.app.divvup.core.designsystem.component.DivvupSimbolo
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.feature.onboarding.OnboardingMedidas
import br.app.divvup.feature.onboarding.PASSOS_ONBOARDING
import br.app.divvup.feature.onboarding.R
import br.app.divvup.feature.onboarding.comum.ProgressoOnboarding

/** B7 Permissão de notificação (docs/design/fonte/Permissao.dc.html, R-36). */
@Composable
fun PermissaoScreen(aoAtivar: () -> Unit, aoAgoraNao: () -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(modifier.fillMaxSize().background(c.background)) {
        ProgressoOnboarding(PASSOS_ONBOARDING)
        // Três avisos de exemplo, só ilustração.
        Column(
            Modifier
                .padding(top = OnboardingMedidas.exemplosTopo)
                .fillMaxWidth()
                .background(c.surfaceAlt)
                .padding(start = e.s4, end = e.s4, top = OnboardingMedidas.exemplosPadding, bottom = e.s5)
                .clearAndSetSemantics { },
            verticalArrangement = Arrangement.spacedBy(e.s2),
        ) {
            Exemplo(R.string.permissao_ex1_titulo, R.string.permissao_ex1_texto, R.string.permissao_ex1_quando, OnboardingMedidas.semRecuo, OnboardingMedidas.exemploRecuo)
            Exemplo(R.string.permissao_ex2_titulo, R.string.permissao_ex2_texto, R.string.permissao_ex2_quando, OnboardingMedidas.exemploRecuo, OnboardingMedidas.semRecuo)
            Exemplo(R.string.permissao_ex3_titulo, R.string.permissao_ex3_texto, R.string.permissao_ex3_quando, OnboardingMedidas.exemploRecuoPequeno, OnboardingMedidas.exemploRecuoMedio)
        }
        Column(Modifier.weight(1f).padding(start = e.s4, end = e.s4, top = OnboardingMedidas.permissaoTopo)) {
            DivvupText(stringResource(R.string.permissao_titulo), t.h1.copy(fontSize = OnboardingMedidas.permissaoTitulo))
            Text(
                stringResource(R.string.permissao_texto),
                style = t.body,
                color = c.textMuted,
                modifier = Modifier.padding(top = e.s3).widthIn(max = OnboardingMedidas.permissaoTextoMaximo),
            )
            Spacer(Modifier.weight(1f))
            Column(Modifier.padding(bottom = e.s5), verticalArrangement = Arrangement.spacedBy(e.s2)) {
                DivvupButton(stringResource(R.string.permissao_ativar), aoAtivar, icone = DivvupIcone.Sino, larguraTotal = true)
                DivvupButton(stringResource(R.string.permissao_agora_nao), aoAgoraNao, variante = VarianteBotao.Texto, larguraTotal = true)
            }
        }
    }
}

@Composable
private fun Exemplo(titulo: Int, texto: Int, quando: Int, inicio: Dp, fim: Dp) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    DivvupCard(Modifier.padding(start = inicio, end = fim).fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = e.s4, vertical = OnboardingMedidas.exemploPaddingV),
            horizontalArrangement = Arrangement.spacedBy(e.s3),
        ) {
            Box(
                Modifier.size(OnboardingMedidas.exemploMarcaFundo).clip(DivvupTheme.shapes.sm).background(c.ultramarino),
                contentAlignment = Alignment.Center,
            ) { DivvupSimbolo(altura = OnboardingMedidas.exemploMarca, cor = c.onUltramarino) }
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(titulo), style = t.small.copy(fontWeight = FontWeight.SemiBold), color = c.text)
                    Text(stringResource(quando), style = t.small.copy(fontSize = OnboardingMedidas.textoQuando), color = c.textMuted)
                }
                Text(stringResource(texto), style = t.small, color = c.textMuted)
            }
        }
    }
}
