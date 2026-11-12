package br.app.divvup.feature.tasks.concluida

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.icon.icone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.tarefa.ResultadoDaConclusao
import br.app.divvup.core.model.Categoria
import br.app.divvup.feature.tasks.R
import br.app.divvup.feature.tasks.TarefasMedidas

/** Estado de C4: o que o backend devolveu ao concluir (R-21, R-22, R-31). */
data class ConcluidaEstado(
    val titulo: String,
    val categoria: Categoria,
    val resultado: ResultadoDaConclusao,
)

/** C4 Tarefa concluída (docs/design/fonte/Concluida.dc.html). Medalha nova chega com as conquistas (#25). */
@Composable
fun ConcluidaScreen(
    estado: ConcluidaEstado,
    aoFechar: () -> Unit,
    aoPegarOutra: () -> Unit,
    aoContinuar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val r = estado.resultado
    Column(modifier.fillMaxSize().background(c.ultramarino)) {
        DivvupTopBar {
            DivvupIconButton(DivvupIcone.X, stringResource(R.string.concluida_fechar), aoFechar, tint = c.onUltramarino)
            Box(Modifier.weight(1f))
            DivvupLabel(stringResource(R.string.concluida_rotulo), cor = c.lima, modifier = Modifier.padding(end = e.s2))
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(start = e.s5, end = e.s5, top = e.s2),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(e.s2), verticalAlignment = Alignment.CenterVertically) {
                DivvupIcon(estado.categoria.icone, tamanho = TamanhoIcone.Pequeno.dp, tint = c.onUltramarino)
                Text(
                    stringResource(
                        R.string.concluida_tarefa,
                        estado.titulo,
                        stringResource(if (r.noPrazo) R.string.detalhe_no_prazo else R.string.detalhe_depois_do_prazo),
                    ),
                    style = t.small,
                    color = c.onUltramarino,
                )
            }
            Box(Modifier.padding(top = e.s1)) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = -e.s5, y = TarefasMedidas.sublinhadoFora)
                        .size(TarefasMedidas.sublinhadoLargura, TarefasMedidas.sublinhadoAltura)
                        .background(c.lima),
                )
                Text(
                    stringResource(R.string.concluida_pontos, r.pontos),
                    style = t.h1.copy(fontSize = TarefasMedidas.numeroGrande),
                    color = c.onUltramarino,
                )
            }
            Text(
                stringResource(R.string.concluida_na_conta),
                style = t.h2.copy(fontSize = TarefasMedidas.tituloConcluida),
                color = c.onUltramarino,
                modifier = Modifier.padding(top = e.s5),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(DivvupTheme.shapes.sheet)
                .background(c.surface)
                .padding(TarefasMedidas.folhaPadding),
            verticalArrangement = Arrangement.spacedBy(e.s4),
        ) {
            Quadro(r)
            Row(horizontalArrangement = Arrangement.spacedBy(e.s2)) {
                DivvupButton(
                    stringResource(R.string.concluida_pegar),
                    aoPegarOutra,
                    icone = DivvupIcone.Deck,
                    modifier = Modifier.weight(1f),
                )
                DivvupButton(stringResource(R.string.concluida_continuar), aoContinuar, variante = VarianteBotao.Contorno)
            }
        }
    }
}

/** Ranking do mês e sequência lado a lado (1,25fr / 1fr). */
@Composable
private fun Quadro(r: ResultadoDaConclusao) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val numero = t.h2.copy(fontSize = TarefasMedidas.numeroConcluida)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(DivvupTheme.shapes.md)
            .border(e.borda, c.border, DivvupTheme.shapes.md),
    ) {
        Column(Modifier.weight(PESO_RANKING).padding(e.s4)) {
            DivvupLabel(stringResource(R.string.concluida_ranking), cor = c.textMuted)
            Row(
                Modifier.padding(top = e.s1),
                horizontalArrangement = Arrangement.spacedBy(e.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val subiu = r.posicaoDepois < r.posicaoAntes
                if (subiu) {
                    Text(
                        stringResource(R.string.concluida_posicao, r.posicaoAntes),
                        style = t.small.copy(textDecoration = TextDecoration.LineThrough),
                        color = c.textMuted,
                    )
                    DivvupIcon(DivvupIcone.SetaR, tamanho = TamanhoIcone.MuitoPequeno.dp, tint = c.textMuted)
                }
                Text(stringResource(R.string.concluida_posicao, r.posicaoDepois), style = numero, color = c.text)
            }
            Text(
                r.ultrapassou?.let { stringResource(R.string.concluida_passou, it.substringBefore(' ')) }
                    ?: stringResource(R.string.concluida_manteve, r.posicaoDepois),
                style = t.small,
                color = c.textMuted,
            )
        }
        Box(Modifier.width(e.borda).fillMaxHeight().background(c.border))
        val descricao = pluralStringResource(R.plurals.concluida_dias_descricao, r.sequencia, r.sequencia)
        Column(Modifier.weight(1f).padding(e.s4).semantics(mergeDescendants = true) { contentDescription = descricao }) {
            DivvupLabel(stringResource(R.string.concluida_sequencia), cor = c.textMuted)
            Row(Modifier.padding(top = e.s1), horizontalArrangement = Arrangement.spacedBy(e.s1), verticalAlignment = Alignment.Bottom) {
                Text(r.sequencia.toString(), style = numero, color = c.text)
                Text(pluralStringResource(R.plurals.concluida_dias, r.sequencia), style = t.small, color = c.textMuted)
            }
            Row(Modifier.padding(top = e.s2), horizontalArrangement = Arrangement.spacedBy(e.s1)) {
                repeat(DIAS_NA_SEMANA) { i ->
                    val feito = i < r.sequencia
                    Box(
                        Modifier
                            .size(TarefasMedidas.diaDaSequencia)
                            .clip(DivvupTheme.shapes.sm)
                            .background(if (feito) c.accent else c.surface)
                            .border(e.borda, if (feito) c.accent else c.border, DivvupTheme.shapes.sm),
                    )
                }
            }
        }
    }
}

private const val PESO_RANKING = 1.25f
private const val DIAS_NA_SEMANA = 7
