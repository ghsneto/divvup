package br.app.divvup.feature.onboarding

import androidx.compose.ui.unit.dp

/** Medidas do onboarding (docs/design/fonte/OnbNome.dc.html; px do artboard = dp). */
internal object OnboardingMedidas {
    // Progresso: 20 de topo, barras de 4 com 4 de espaço, 12 até o contador.
    val progressoTopo = 20.dp
    val barraProgresso = 4.dp

    val conteudoTopo = 28.dp
    val tituloMaximo = 250.dp
    val avatarGrande = 96.dp
    val avatarRapido = 44.dp
    val lapisFora = 4.dp
    val escolhaFundo = 4.dp
    val linksTopo = 12.dp
    val campoTopo = 28.dp
    val rodape = 24.dp
}

/** Passos do onboarding (TELAS.md: B1 → B2 → B3/B4 → B6 → B7). */
internal const val PASSOS_ONBOARDING = 5
