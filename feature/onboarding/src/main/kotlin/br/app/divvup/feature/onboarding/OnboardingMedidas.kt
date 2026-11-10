package br.app.divvup.feature.onboarding

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Medidas do onboarding (docs/design/fonte/OnbNome, OnbCasa e CriarCasa; px do artboard = dp). */
internal object OnboardingMedidas {
    // Progresso: 20 de topo, barras de 4 com 4 de espaço, 12 até o contador.
    val progressoTopo = 20.dp
    val barraProgresso = 4.dp

    // Cabeçalho com "voltar" (B2, B3): padding 12 16 0 8.
    val cabecalhoTopo = 12.dp
    val cabecalhoInicio = 8.dp

    val conteudoTopo = 28.dp
    val tituloMaximo = 250.dp
    val avatarGrande = 96.dp
    val avatarRapido = 44.dp
    val lapisFora = 4.dp
    val escolhaFundo = 4.dp
    val linksTopo = 12.dp
    val campoTopo = 28.dp
    val rodape = 24.dp

    // B2 OnbCasa.
    val casaConteudoTopo = 20.dp
    val cartaoCriarTopo = 28.dp
    val cartaoCriarPadding = 20.dp
    val cartaoCriarAltura = 176.dp
    val cartaoCriarTitulo = 36.sp
    val cartaoCriarTextoMaximo = 190.dp
    val cartaoCriarSeta = 48.dp
    val codigoFonte = 20.sp
    val codigoPaddingV = 8.dp
    val codigoPaddingH = 12.dp
    val cartaoCodigoPaddingV = 20.dp
    val procurarTopo = 4.dp
    val sairFundo = 20.dp

    // B3 CriarCasa.
    val campoCasaFonte = 20.sp
    val campoCasaAltura = 56.dp
    val tipoAltura = 64.dp
    val radio = 22.dp
    val radioPonto = 10.dp
    val seloPequenoAltura = 18.dp
    val seloPequenoFonte = 12.sp
    val rodapeBotaoTopo = 12.dp
    val rodapeBotaoFundo = 20.dp
}

/** Passos do onboarding (TELAS.md: B1 → B2 → B3/B4 → B6 → B7). */
internal const val PASSOS_ONBOARDING = 5
