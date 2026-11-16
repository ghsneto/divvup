package br.app.divvup.feature.ranking

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Medidas de D1/D2 (docs/design/fonte/Ranking.dc.html, Temporadas.dc.html; px = dp). */
internal object RankingMedidas {
    // Abas.
    val abaAltura = 48.dp
    val abaPaddingH = 8.dp
    val abaTraco = 2.dp
    val abaFonte = 14.sp

    // Cabeçalho da temporada.
    val cabecalhoTopo = 20.dp
    val faltamLargura = 92.dp
    val barraFaltam = 4.dp

    // Pódio.
    val podioAvatar = 40.dp
    val podioAvatarPrimeiro = 64.dp
    val podioAlturaPrimeiro = 190.dp
    val podioAlturaSegundo = 150.dp
    val podioAlturaTerceiro = 116.dp
    val podioPosicao = 36.sp
    val podioPosicaoPrimeiro = 57.sp
    const val PESO_PRIMEIRO = 1.25f
    const val OPACIDADE_PONTOS = 0.85f

    // Equilíbrio.
    val equilibrioNumero = 36.sp
    val equilibrioSegmento = 8.dp
    val textoPequeno = 12.sp
    val botaoAjuda = 44.dp
    const val SEGMENTOS = 10

    // Lista.
    val listaTopo = 20.dp
    val posicaoLargura = 22.dp
    val avatarLista = 40.dp
    val destaquePadding = 10.dp
    val destaqueRaio = 8.dp
    val semPadding = 0.dp
    const val OPACIDADE_EX = 0.5f

    // Geral.
    val geralTitulo = 57.sp

    // Histórico.
    val chipAltura = 36.dp
    val chipAvatar = 24.dp
    val chipAvatarFora = (-8).dp
    val avatarEvento = 24.dp
    val eventoTexto = 14.sp
    val vazioPadding = 32.dp

    // Ajuda.
    val ajudaTitulo = 36.sp
    val faixaLargura = 56.dp

    // D2.
    val mesLargura = 112.dp
    val mesFonte = 24.sp
    val avatarCampeao = 40.dp
    val avatarColocacao = 24.dp
    val posicaoColocacao = 20.dp
    val barraColocacaoLargura = 70.dp
    val barraColocacaoAltura = 6.dp
    val pontosColocacao = 36.dp
}
