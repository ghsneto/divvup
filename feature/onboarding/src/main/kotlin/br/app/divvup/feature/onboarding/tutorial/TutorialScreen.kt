package br.app.divvup.feature.onboarding.tutorial

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupCard
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupPointsBadge
import br.app.divvup.core.designsystem.component.DivvupSnackbar
import br.app.divvup.core.designsystem.component.DivvupStatusTag
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.TamanhoSelo
import br.app.divvup.core.designsystem.component.TomSelo
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.icon.icone
import br.app.divvup.core.designsystem.icon.ilustracao
import br.app.divvup.core.designsystem.icon.nome
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.model.Categoria
import br.app.divvup.feature.onboarding.OnboardingMedidas
import br.app.divvup.feature.onboarding.R
import br.app.divvup.feature.onboarding.comum.ProgressoOnboarding
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import br.app.divvup.core.designsystem.R as RDs

/** Passos de B6: 0 assumir, 1 passar, 2 desfazer, 3 pronto. */
const val PASSOS_DO_TUTORIAL = 3

private const val PASSO_ONBOARDING = 4
private const val PONTOS_LAVAR = 50
private const val PONTOS_LIXO = 25

/**
 * B6 Tutorial (docs/design/fonte/Tutorial.dc.html): três passos com cartões de mentira, nada é
 * gravado. "Pular" sempre visível; sem animações quando o sistema pede para removê-las.
 */
@Composable
fun TutorialScreen(
    passo: Int,
    aoAssumir: () -> Unit,
    aoPassar: () -> Unit,
    aoDesfazer: () -> Unit,
    aoPular: () -> Unit,
    aoContinuar: () -> Unit,
    modifier: Modifier = Modifier,
    semAnimacao: Boolean = semAnimacoesDoSistema(),
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val titulos = listOf(R.string.tutorial_titulo_1, R.string.tutorial_titulo_2, R.string.tutorial_titulo_3, R.string.tutorial_titulo_4)
    Box(modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize()) {
            ProgressoOnboarding(PASSO_ONBOARDING, final = {
                DivvupButton(stringResource(R.string.tutorial_pular), aoPular, variante = VarianteBotao.Texto)
            })
            Column(Modifier.heightIn(min = OnboardingMedidas.tutorialCabecalho).padding(start = e.s4, end = e.s4, top = OnboardingMedidas.tutorialTopo)) {
                DivvupLabel(
                    if (passo < PASSOS_DO_TUTORIAL) stringResource(R.string.tutorial_passo, passo + 1) else stringResource(R.string.tutorial_completo),
                    cor = c.textMuted,
                )
                DivvupText(stringResource(titulos[passo]), t.h2.copy(fontSize = OnboardingMedidas.tutorialTitulo), Modifier.padding(top = e.s2))
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(start = e.s4, end = e.s4, top = e.s2)) {
                when {
                    passo < 2 -> Pilha(passo, semAnimacao, aoAssumir, aoPassar)
                    passo == PASSOS_DO_TUTORIAL -> Fim()
                }
            }
            if (passo < 2) {
                Row(Modifier.padding(start = e.s4, end = e.s4, top = e.s3, bottom = e.s5), horizontalArrangement = Arrangement.spacedBy(e.s2)) {
                    DivvupButton(
                        stringResource(R.string.tutorial_passar),
                        aoPassar,
                        variante = VarianteBotao.Perigo,
                        icone = DivvupIcone.X,
                        habilitado = passo == 1,
                        larguraTotal = true,
                        modifier = Modifier.weight(1f),
                    )
                    DivvupButton(
                        stringResource(R.string.tutorial_assumir),
                        aoAssumir,
                        variante = VarianteBotao.Aceitar,
                        icone = DivvupIcone.Check,
                        habilitado = passo == 0,
                        larguraTotal = true,
                        modifier = Modifier.weight(OnboardingMedidas.PESO_ASSUMIR),
                    )
                }
            } else if (passo == PASSOS_DO_TUTORIAL) {
                DivvupButton(
                    stringResource(R.string.tutorial_continuar),
                    aoContinuar,
                    iconeDepois = DivvupIcone.SetaR,
                    larguraTotal = true,
                    modifier = Modifier.padding(start = e.s4, end = e.s4, top = e.s3, bottom = e.s5),
                )
            }
        }
        if (passo == 2) {
            DivvupSnackbar(
                stringResource(R.string.tutorial_voce_passou),
                icone = null,
                acao = stringResource(R.string.tutorial_desfazer),
                onAcao = aoDesfazer,
                iconeAcao = DivvupIcone.Desfazer,
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = e.s4, end = e.s4, bottom = OnboardingMedidas.snackFundo),
            )
        }
    }
}

/** "Remover animações" ligado no Android: escala de animação 0. */
@Composable
private fun semAnimacoesDoSistema(): Boolean {
    val contexto = LocalContext.current
    return remember { Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

@Composable
private fun Pilha(passo: Int, semAnimacao: Boolean, aoAssumir: () -> Unit, aoPassar: () -> Unit) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    val densidade = LocalDensity.current
    val limite = with(densidade) { OnboardingMedidas.limiteDoSwipe.toPx() }
    val x = remember(passo) { Animatable(0f) }
    val escopo = rememberCoroutineScope()
    val categoria = if (passo == 0) Categoria.COZINHA else Categoria.LIXO
    Box(Modifier.fillMaxWidth().height(OnboardingMedidas.cartaoAltura)) {
        // Cartão de trás, mais baixo e menor.
        Box(
            Modifier
                .fillMaxSize()
                .offset(y = OnboardingMedidas.cartaoAtrasDescida)
                .graphicsLayer { scaleX = OnboardingMedidas.CARTAO_ATRAS_ESCALA; scaleY = OnboardingMedidas.CARTAO_ATRAS_ESCALA }
                .clip(DivvupTheme.shapes.md)
                .background(c.surfaceAlt)
                .border(e.borda, c.border, DivvupTheme.shapes.md),
        )
        Box(
            Modifier
                .fillMaxSize()
                .offset { IntOffset(x.value.roundToInt(), 0) }
                .rotate(with(densidade) { x.value.toDp().value } * OnboardingMedidas.GIRO_POR_DP)
                .pointerInput(passo) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            escopo.launch {
                                val certo = (passo == 0 && x.value > limite) || (passo == 1 && x.value < -limite)
                                if (certo) {
                                    if (passo == 0) aoAssumir() else aoPassar()
                                } else if (semAnimacao) {
                                    x.snapTo(0f)
                                } else {
                                    x.animateTo(0f, tween(DURACAO_VOLTA_MS))
                                }
                            }
                        },
                    ) { _, arrasto -> escopo.launch { x.snapTo(x.value + arrasto) } }
                },
        ) {
            Cartao(categoria, if (passo == 0) R.string.tutorial_lavar else R.string.tutorial_lixo, if (passo == 0) PONTOS_LAVAR else PONTOS_LIXO, x.value / limite)
        }
        Seta(passo, semAnimacao)
    }
}

private const val DURACAO_VOLTA_MS = 200

@Composable
private fun Cartao(categoria: Categoria, titulo: Int, pontos: Int, progresso: Float) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val nome = stringResource(titulo)
    val descricao = stringResource(R.string.tutorial_cartao, nome)
    DivvupCard(Modifier.fillMaxSize().semantics { contentDescription = descricao }) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(OnboardingMedidas.cartaoImagem)) {
                    Image(painterResource(categoria.ilustracao.res), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    DivvupStatusTag(
                        stringResource(R.string.tutorial_de_mentira),
                        TomSelo.Neutro,
                        Modifier.align(Alignment.TopEnd).padding(OnboardingMedidas.seloPadding),
                    )
                }
                Box(Modifier.fillMaxWidth().height(e.borda).background(c.border))
                Column(Modifier.weight(1f).padding(horizontal = OnboardingMedidas.exemplosPadding, vertical = e.s4)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(e.s1), verticalAlignment = Alignment.CenterVertically) {
                        DivvupIcon(categoria.icone, tamanho = TamanhoIcone.MuitoPequeno.dp, tint = c.textMuted)
                        DivvupLabel(stringResource(categoria.nome), cor = c.textMuted)
                    }
                    Text(nome, style = t.h3.copy(fontSize = OnboardingMedidas.cartaoTitulo), color = c.text, modifier = Modifier.padding(top = e.s1))
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.tutorial_vence), style = t.small, color = c.textMuted)
                        DivvupPointsBadge(pontos, stringResource(RDs.string.pts), tamanho = TamanhoSelo.Grande)
                    }
                }
            }
            // Arrastando: lima "É MINHA" à direita, vermelho "PASSO" à esquerda.
            if (progresso > 0f) Carimbo(stringResource(R.string.tutorial_e_minha), c.aceitar, c.onAceitar, progresso, Alignment.TopStart, -GIRO_CARIMBO)
            if (progresso < 0f) Carimbo(stringResource(R.string.tutorial_passo_carimbo), c.passar, c.onDanger, -progresso, Alignment.TopEnd, GIRO_CARIMBO)
        }
    }
}

private const val GIRO_CARIMBO = 8f

@Composable
private fun Carimbo(texto: String, fundo: androidx.compose.ui.graphics.Color, cor: androidx.compose.ui.graphics.Color, progresso: Float, onde: Alignment, giro: Float) {
    val e = DivvupTheme.spacing
    Box(Modifier.fillMaxSize().alpha(progresso.coerceIn(0f, 1f)).background(fundo).padding(e.s5), contentAlignment = onde) {
        Text(
            texto,
            style = DivvupTheme.typography.h2.copy(fontSize = OnboardingMedidas.carimbo, fontWeight = FontWeight.ExtraBold),
            color = cor,
            modifier = Modifier.rotate(giro).border(e.borda, cor, DivvupTheme.shapes.sm).padding(horizontal = e.s3, vertical = e.s1),
        )
    }
}

/** Seta que mostra para onde arrastar, balançando (parada com "remover animações"). */
@Composable
private fun Seta(passo: Int, semAnimacao: Boolean) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    val direita = passo == 0
    val empurrao = if (semAnimacao) {
        0f
    } else {
        val transicao = rememberInfiniteTransition(label = "seta")
        val v by transicao.animateFloat(0f, 1f, infiniteRepeatable(tween(DURACAO_EMPURRAO_MS), RepeatMode.Reverse), label = "empurrao")
        v
    }
    val deslocamento = with(LocalDensity.current) { (OnboardingMedidas.empurrao.toPx() * empurrao * if (direita) 1 else -1).roundToInt() }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(if (direita) Alignment.TopEnd else Alignment.TopStart)
                .padding(top = OnboardingMedidas.setaTopo)
                .offset(x = if (direita) e.s3 else -e.s3)
                .offset { IntOffset(deslocamento, 0) }
                .size(OnboardingMedidas.seta)
                .clip(DivvupTheme.shapes.pill)
                .background(if (direita) c.aceitar else c.passar),
            contentAlignment = Alignment.Center,
        ) {
            DivvupIcon(if (direita) DivvupIcone.SetaR else DivvupIcone.ChevL, tamanho = TamanhoIcone.Pequeno.dp, tint = if (direita) c.onAceitar else c.onDanger)
        }
    }
}

private const val DURACAO_EMPURRAO_MS = 800

/** Passo final: os três gestos e o aviso de que nada foi salvo. */
@Composable
private fun Fim() {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(Modifier.padding(top = e.s3), verticalArrangement = Arrangement.spacedBy(e.s3)) {
        Row(horizontalArrangement = Arrangement.spacedBy(e.s2)) {
            listOf(
                Triple(R.string.tutorial_assumir_curto, DivvupIcone.Check, c.aceitar to c.onAceitar),
                Triple(R.string.tutorial_passar_curto, DivvupIcone.X, c.passar to c.onDanger),
                Triple(R.string.tutorial_desfazer_curto, DivvupIcone.Desfazer, c.text to c.background),
            ).forEach { (texto, icone, cores) ->
                Row(
                    Modifier.weight(1f).height(OnboardingMedidas.chipFinal).clip(DivvupTheme.shapes.md).background(cores.first),
                    horizontalArrangement = Arrangement.spacedBy(e.s2, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DivvupIcon(icone, tint = cores.second)
                    Text(stringResource(texto), style = t.botao, color = cores.second)
                }
            }
        }
        Text(stringResource(R.string.tutorial_fim), style = t.body, color = c.textMuted)
    }
}
