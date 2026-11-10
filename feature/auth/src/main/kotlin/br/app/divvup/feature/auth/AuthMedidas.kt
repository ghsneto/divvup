package br.app.divvup.feature.auth

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Medidas de A1–A5 tiradas de docs/design/fonte (px do artboard = dp). */
internal object AuthMedidas {
    // A1 Splash: logo 239,5×60.
    val logoSplash = 60.dp

    // A2 BoasVindas: faixa ultramarino de 332, logo 28 em (16, 20), símbolo 176 a 32 da direita e 84 do topo,
    // traço lima 132×12 no rodapé da faixa, conteúdo com 28 de topo e subtítulo de até 290.
    val faixaBoasVindas = 332.dp
    val logoFaixa = 28.dp
    val logoFaixaX = 16.dp
    val logoFaixaY = 20.dp
    val simboloGrande = 176.dp
    val simboloDireita = 32.dp
    val simboloTopo = 84.dp
    val tracoLimaLargura = 132.dp
    val tracoLimaAltura = 12.dp
    val conteudoTopo = 28.dp
    val subtituloMaximo = 290.dp
    val rodapeBoasVindas = 20.dp
    val fonteRodape = 12.sp

    // A3/A4/A5: conteúdo com 12 de topo; espaço abaixo do título 28 (A3, A5) e 24 (A4).
    val conteudoTopoFormulario = 12.dp
    val abaixoTituloEntrar = 28.dp
    val abaixoTituloCriar = 24.dp
    val campoSenhaFim = 52.dp
    val botaoTopo = 24.dp
    val ouVertical = 20.dp
    val rodapeFormulario = 24.dp
    val shuttleLargura = 48.dp

    // A4: medidor de força (4 barras de 4dp com 4 de espaço; rótulo de 58).
    val barraForca = 4.dp
    val rotuloForcaMinimo = 58.dp
    val termosTopo = 20.dp
    val checkboxTopo = 4.dp

    // A5: texto até 280; ilustração de envelope 200×132 com selo lima de 52 deslocado (-18, -16).
    val recuperarTextoMaximo = 280.dp
    val envelopeLargura = 200.dp
    val envelopeAltura = 132.dp
    val seloEnviado = 52.dp
    val seloEnviadoForaX = 18.dp
    val seloEnviadoForaY = 16.dp
    val envelopeTraco = 4.dp
}
