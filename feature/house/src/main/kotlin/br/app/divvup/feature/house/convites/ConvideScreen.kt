package br.app.divvup.feature.house.convites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupSkeleton
import br.app.divvup.core.designsystem.component.DivvupSnackbar
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.component.MensagemErro
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.casa.TAMANHO_CODIGO
import br.app.divvup.core.domain.casa.VALIDADE_CONVITE
import br.app.divvup.feature.house.CasaMedidas
import br.app.divvup.feature.house.R
import java.time.Duration

/** Estado de B8. */
data class ConvideEstado(
    val nomeDaCasa: String = "",
    val codigo: String? = null,
    val restante: Duration = VALIDADE_CONVITE,
    val gerando: Boolean = false,
    val erro: Boolean = false,
    /** Mensagem do snackbar (id de string). */
    val aviso: Int? = null,
)

/** B8 Convide seus moradores (docs/design/fonte/Convide.dc.html, R-06, R-08). */
@Composable
fun ConvideScreen(
    estado: ConvideEstado,
    aoDepois: () -> Unit,
    aoCompartilhar: () -> Unit,
    aoCopiar: () -> Unit,
    aoGerarOutro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Box(modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize()) {
            DivvupTopBar {
                DivvupLabel(stringResource(R.string.convide_rotulo), Modifier.weight(1f), cor = c.textMuted)
                DivvupButton(
                    stringResource(R.string.convide_depois),
                    aoDepois,
                    variante = VarianteBotao.Texto,
                    modifier = Modifier.heightIn(min = CasaMedidas.botaoDepois),
                )
            }
            Column(Modifier.weight(1f).padding(start = e.s4, end = e.s4, top = e.s2)) {
                DivvupText(stringResource(R.string.convide_titulo), t.h1)
                Text(
                    stringResource(R.string.convide_texto, estado.nomeDaCasa),
                    style = t.body,
                    color = c.textMuted,
                    modifier = Modifier.padding(top = e.s3).widthIn(max = CasaMedidas.textoMaximo),
                )
                CartaoDoCodigo(estado, Modifier.padding(top = CasaMedidas.cartaoTopo))
                if (estado.erro) MensagemErro(stringResource(R.string.convide_erro))
                Row(Modifier.padding(top = e.s3), horizontalArrangement = Arrangement.spacedBy(e.s2)) {
                    DivvupButton(
                        stringResource(R.string.convide_compartilhar),
                        aoCompartilhar,
                        icone = DivvupIcone.Compartilhar,
                        habilitado = estado.codigo != null,
                        larguraTotal = true,
                        modifier = Modifier.weight(PESO_COMPARTILHAR),
                    )
                    DivvupButton(
                        stringResource(R.string.convide_copiar),
                        aoCopiar,
                        variante = VarianteBotao.Contorno,
                        icone = DivvupIcone.Copiar,
                        habilitado = estado.codigo != null,
                        larguraTotal = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                BotaoDeTextoSemRecuo(
                    stringResource(R.string.convide_outro),
                    DivvupIcone.Refresh,
                    aoGerarOutro,
                    Modifier.padding(top = e.s1),
                )
                Spacer(Modifier.weight(1f))
                Row(Modifier.padding(bottom = e.s5), horizontalArrangement = Arrangement.spacedBy(e.s2)) {
                    DivvupIcon(DivvupIcone.Info, tamanho = TamanhoIcone.Pequeno.dp, tint = c.textMuted)
                    Text(stringResource(R.string.convide_depois_dica), style = t.small, color = c.textMuted)
                }
            }
        }
        estado.aviso?.let {
            DivvupSnackbar(
                stringResource(it),
                icone = DivvupIcone.Check,
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = e.s4, end = e.s4, bottom = CasaMedidas.snackFundo),
            )
        }
    }
}

/** Cartão `tinta` com o código grande, a contagem e a validade. */
@Composable
private fun CartaoDoCodigo(estado: ConvideEstado, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val st = c.sobreTinta
    Column(modifier.fillMaxWidth().clip(DivvupTheme.shapes.md).background(c.tinta)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = CasaMedidas.cartaoPadding, end = CasaMedidas.cartaoPadding, top = CasaMedidas.cartaoPadding, bottom = e.s4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DivvupLabel(stringResource(R.string.convide_codigo), cor = c.lima)
            Row(horizontalArrangement = Arrangement.spacedBy(e.s1), verticalAlignment = Alignment.CenterVertically) {
                DivvupIcon(DivvupIcone.Relogio, tamanho = TamanhoIcone.MuitoPequeno.dp, tint = st.textoMudo)
                Text(relogio(estado.restante), style = t.small.copy(fontFeatureSettings = "tnum"), color = st.textoMudo)
            }
        }
        val codigo = estado.codigo
        val descricao = codigo?.let { stringResource(R.string.convide_codigo_descricao, it.toList().joinToString(" ")) }.orEmpty()
        Row(
            Modifier.padding(horizontal = e.s4).semantics(mergeDescendants = true) { contentDescription = descricao },
            horizontalArrangement = Arrangement.spacedBy(e.s1),
        ) {
            repeat(TAMANHO_CODIGO) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(CasaMedidas.caixaGrande)
                        .clip(DivvupTheme.shapes.sm)
                        .background(st.caixa),
                    contentAlignment = Alignment.Center,
                ) {
                    val letra = codigo?.getOrNull(i)
                    if (letra != null) {
                        Text(
                            letra.toString(),
                            style = t.codigoGrande,
                            color = if (i % 2 == 1) c.lima else st.texto,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        DivvupSkeleton(Modifier.fillMaxWidth(METADE).height(CasaMedidas.cartaoPadding))
                    }
                }
            }
        }
        Row(
            Modifier
                .padding(top = e.s4)
                .fillMaxWidth()
                .drawBehind {
                    val traco = CasaMedidas.tracejado.toPx()
                    drawLine(
                        st.linha,
                        Offset(0f, 0f),
                        Offset(size.width, 0f),
                        strokeWidth = e.borda.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(traco, traco)),
                    )
                }
                .padding(horizontal = CasaMedidas.cartaoPadding, vertical = e.s4),
            horizontalArrangement = Arrangement.spacedBy(e.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.convide_validade), style = t.small, color = st.textoMudo, modifier = Modifier.weight(1f))
            val fracao = (estado.restante.seconds.toFloat() / VALIDADE_CONVITE.seconds).coerceIn(0f, 1f)
            Box(
                Modifier
                    .width(CasaMedidas.barraValidade)
                    .height(CasaMedidas.barraValidadeAltura)
                    .clip(DivvupTheme.shapes.sm)
                    .background(st.caixa),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(fracao).background(c.lima))
            }
        }
    }
}

/** `.btn-t` com ícone pequeno e sem recuo à esquerda. */
@Composable
internal fun BotaoDeTextoSemRecuo(texto: String, icone: DivvupIcone, aoClicar: () -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    Row(
        modifier
            .heightIn(min = DivvupTheme.spacing.toqueMinimo)
            .clip(DivvupTheme.shapes.sm)
            .clickable(role = Role.Button, onClick = aoClicar),
        horizontalArrangement = Arrangement.spacedBy(DivvupTheme.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DivvupIcon(icone, tamanho = TamanhoIcone.Pequeno.dp, tint = c.text)
        Text(texto, style = DivvupTheme.typography.botao, color = c.text)
    }
}

private const val PESO_COMPARTILHAR = 1.6f
private const val METADE = 0.5f
