package br.app.divvup.feature.tasks

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Medidas de C2 (docs/design/fonte/NovaTarefa.dc.html; px do artboard = dp). */
internal object TarefasMedidas {
    val tituloFonte = 36.sp
    val tituloTopo = 4.dp
    val tituloFundo = 20.dp
    val previaAltura = 116.dp
    val previaImagem = 112.dp
    val previaFundo = 28.dp
    val secaoTopo = 28.dp
    val descricaoAltura = 82.dp
    val pesoAltura = 76.dp
    val pesoFonte = 36.sp
    val rotuloPequeno = 12.sp

    // C5 MinhasTarefas.
    val contadorFonte = 57.sp
    val segmentoLargura = 12.dp
    val segmentoAltura = 6.dp
    val miniatura = 56.dp
    val tituloItem = 16.sp
    val pontosItem = 16.sp
    val botaoConcluir = 44.dp
    val seloPequenoAltura = 20.dp
    val bolinhaConcluida = 24.dp
    val textoConcluida = 14.sp
    val vazioTopo = 56.dp
    val textoMaximoVazio = 250.dp
    val respiroDaLista = 160.dp

    // C3 DetalheTarefa.
    val imagemDetalhe = 248.dp
    val seloCategoriaAltura = 30.dp
    val seloCategoriaFora = 15.dp
    val conteudoDetalheTopo = 32.dp
    /** Contorno tracejado do avatar "ninguém ainda" (stroke-dasharray 2 2). */
    val tracoSemDono = 2.dp

    // C4 Concluida.
    val numeroGrande = 57.sp
    val sublinhadoLargura = 148.dp
    val sublinhadoAltura = 8.dp
    val sublinhadoFora = 12.dp
    val tituloConcluida = 36.sp
    val numeroConcluida = 36.sp
    val diaDaSequencia = 12.dp
    val medalha = 56.dp
    val tituloMedalha = 20.sp
    /** `.sheet` de C4: padding 20 nos quatro lados. */
    val folhaPadding = 20.dp
}
