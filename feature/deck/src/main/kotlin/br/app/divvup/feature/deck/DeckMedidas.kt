package br.app.divvup.feature.deck

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Medidas de C1 (docs/design/fonte/Main.dc.html; px do artboard = dp). */
internal object DeckMedidas {
    // Faixa de status.
    val faixaAltura = 52.dp
    val faixaLinha = 44.dp
    val segmentoLargura = 10.dp
    val segmentoAltura = 14.dp

    // Pilha e cartão.
    val conteudoTopo = 4.dp
    val pilhaAltura = 500.dp
    val pilhaComAviso = 444.dp
    val respiroDaPilha = 24.dp
    val imagemAltura = 200.dp
    val seloCategoriaAltura = 30.dp
    val seloUrgenteTopo = 15.dp
    val tituloFonte = 28.sp
    val avatarAutor = 24.dp
    val rotuloPeso = 12.sp

    // Botões.
    val botaoAltura = 56.dp
    val passarLargura = 96.dp
    val maisLargura = 56.dp
    val assumirFonte = 16.sp

    // Estados.
    val vazioTopo = 24.dp
    val desenhoVazioAltura = 150.dp
    val cartaoTracejadoLargura = 150.dp
    val cartaoTracejadoAltura = 100.dp
    val cartaoTracejado1X = 0.dp
    val cartaoTracejado1Y = 30.dp
    val cartaoTracejado2X = 46.dp
    val cartaoTracejado2Y = 14.dp
    val selinhoX = 180.dp
    val selinhoY = 46.dp
    val selinho = 44.dp
    val tracejado = 4.dp
    val tituloVazio = 57.sp
    val textoMaximoVazio = 260.dp
    val erroTopo = 40.dp
    val tituloErro = 36.sp
    val textoMaximoErro = 280.dp
    val carregandoAltura = 470.dp
    val carregandoTitulo = 28.dp
    val carregandoLinha = 14.dp
    val carregandoRodapeTopo = 92.dp
    val carregandoPrazo = 18.dp
    val carregandoSeloAltura = 44.dp
    val carregandoSeloLargura = 96.dp
    val barraCarregandoTopo = 20.dp
    val barraCarregandoLargura = 48.dp

    // Swipe (Main.dc.html: 110 de arraste, overlay cheio a 120, peteleco a partir de 32).
    val limiteDoSwipe = 110.dp
    val inicioDoOverlay = 120.dp
    val petelecoMinimo = 32.dp
    val carimboTopo = 28.dp
    val carimboFonte = 36.sp

    // Passadas.
    val miniaturaLargura = 64.dp
    val miniaturaAltura = 84.dp
    val textoMaximoPassadas = 270.dp
}
