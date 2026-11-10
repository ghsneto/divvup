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
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.feature.onboarding.OnboardingMedidas
import br.app.divvup.feature.onboarding.PASSOS_ONBOARDING
import br.app.divvup.feature.onboarding.R

/** Indicador de passo do onboarding: 5 barras (atual em `primary`) e "n/5". */
@Composable
internal fun ProgressoOnboarding(passo: Int, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    val descricao = stringResource(R.string.onb_passo_descricao, passo, PASSOS_ONBOARDING)
    Row(
        modifier.padding(start = e.s4, end = e.s4, top = OnboardingMedidas.progressoTopo),
        horizontalArrangement = Arrangement.spacedBy(e.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f).semantics { contentDescription = descricao }, horizontalArrangement = Arrangement.spacedBy(e.s1)) {
            repeat(PASSOS_ONBOARDING) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(OnboardingMedidas.barraProgresso)
                        .clip(DivvupTheme.shapes.sm)
                        .background(if (i < passo) c.primary else c.border),
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
