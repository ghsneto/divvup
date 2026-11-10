package br.app.divvup.feature.onboarding.comum

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.feature.onboarding.OnboardingMedidas
import br.app.divvup.feature.onboarding.PASSOS_ONBOARDING
import br.app.divvup.feature.onboarding.R

/**
 * Indicador de passo do onboarding: 5 barras (feitas em `text`, atual em `primary`) e "n/5".
 * Com [aoVoltar], o cabeçalho de B2/B3: botão "voltar" à esquerda (padding 12 16 0 8).
 */
@Composable
internal fun ProgressoOnboarding(passo: Int, modifier: Modifier = Modifier, aoVoltar: (() -> Unit)? = null) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    val descricao = stringResource(R.string.onb_passo_descricao, passo, PASSOS_ONBOARDING)
    val margens = if (aoVoltar == null) {
        Modifier.padding(start = e.s4, end = e.s4, top = OnboardingMedidas.progressoTopo)
    } else {
        Modifier.padding(start = OnboardingMedidas.cabecalhoInicio, end = e.s4, top = OnboardingMedidas.cabecalhoTopo)
    }
    Row(
        modifier.then(margens),
        horizontalArrangement = Arrangement.spacedBy(e.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        aoVoltar?.let { DivvupIconButton(DivvupIcone.ChevL, stringResource(R.string.onb_voltar), it) }
        Row(Modifier.weight(1f).semantics { contentDescription = descricao }, horizontalArrangement = Arrangement.spacedBy(e.s1)) {
            repeat(PASSOS_ONBOARDING) { i ->
                val cor = when {
                    i + 1 < passo -> c.text
                    i + 1 == passo -> c.primary
                    else -> c.border
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(OnboardingMedidas.barraProgresso)
                        .clip(DivvupTheme.shapes.sm)
                        .background(cor),
                )
            }
        }
        Text(
            stringResource(R.string.onb_passo, passo, PASSOS_ONBOARDING),
            style = DivvupTheme.typography.small.copy(fontFeatureSettings = "tnum"),
            color = c.textMuted,
        )
    }
}
