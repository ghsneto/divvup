package br.app.divvup.feature.profile

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Medidas de E3 (docs/design/fonte/Avatar.dc.html; px do artboard = dp). */
internal object ProfileMedidas {
    val avatarGrande = 96.dp
    val colunasGaleria = 6
    val escalaSelecionado = 1.08f
    val galeriaTopo = 20.dp
    val rodapeTopo = 12.dp
    val rodapeFundo = 20.dp

    // E4 PrefsNotif.
    val secaoTopo = 28.dp
    val linhaAltura = 60.dp
    val textoLinha = 14.sp
    val textoPequeno = 12.sp
    /** `.small` com 12px: line-height 1,43. */
    val textoPequenoLinha = 17.sp
    val seloV11 = 18.dp
    val caixaHorario = 64.dp
    val textoHorario = 20.sp
    val textoSilencio = 16.sp
    val botaoBanner = 40.dp

    // E1 Perfil.
    val editarAltura = 40.dp
    val avatarPerfil = 96.dp
    val nomePerfil = 57.sp
    val cartaoTopo = 20.dp
    const val PESO_SEQUENCIA = 1.2f
    val numeroSequencia = 57.sp
    val medalhaAltura = 96.dp
    val discoGrande = 56.dp
    val discoPequeno = 36.dp
    val nomeMedalhaGrande = 16.sp
    val nomeMedalhaPequeno = 14.sp
    val tracoBloqueada = 4.dp
    val linkAltura = 56.dp
    const val DIAS_NO_QUADRO = 14
    const val DIAS_POR_LINHA = 7

    // E5 Aparencia / E6 Sobre / E2 EditarPerfil.
    val tituloGrande = 57.sp
    val miniaturaLargura = 64.dp
    val miniaturaAltura = 84.dp
    val miniaturaBarra = 6.dp
    val miniaturaBloco = 24.dp
    val radio = 24.dp
    val radioPonto = 12.dp
    val bordaEscolhida = 2.dp
    const val FRACAO_LINHA = 0.7f
    const val FRACAO_CURTA = 0.4f
    val sobreCartaoPadding = 20.dp
    val sobreLogo = 40.dp
    val avatarEditar = 96.dp
    val semEspaco = 0.dp

    // E7 ExcluirConta.
    val numeroItem = 28.sp
    val colunaNumero = 36.dp
    val itensTopo = 20.dp
    const val ESPACAMENTO_PALAVRA = 0.12f
}
