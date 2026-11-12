package br.app.divvup.feature.tasks.minhas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupDivider
import br.app.divvup.core.designsystem.component.DivvupFab
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupStatusTag
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.TomSelo
import br.app.divvup.core.designsystem.component.formatarNumero
import br.app.divvup.core.designsystem.component.textoDaHora
import br.app.divvup.core.designsystem.component.textoDoDia
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.icon.ilustracao
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.tarefa.LIMITE_DE_ABERTAS
import br.app.divvup.core.domain.tarefa.SecoesDasMinhas
import br.app.divvup.core.domain.tarefa.pontosDaConclusao
import br.app.divvup.core.domain.tarefa.prazoFalado
import br.app.divvup.core.model.Tarefa
import br.app.divvup.core.model.ViaDeAtribuicao
import br.app.divvup.feature.tasks.R
import br.app.divvup.feature.tasks.TarefasMedidas
import java.time.Instant

/** Estado de C5. */
data class MinhasTarefasEstado(
    val secoes: SecoesDasMinhas = SecoesDasMinhas(emptyList(), emptyList(), emptyList(), emptyList()),
    val agora: Instant = Instant.EPOCH,
    val concluidasAbertas: Boolean = true,
    /** Tarefa sendo concluída agora (botão ✓ carregando). */
    val concluindo: String? = null,
)

/** Seção de C5: rótulo, cor e como o prazo aparece. */
private enum class Secao { Atrasadas, Hoje, Proximas }

/** C5 Minhas tarefas (docs/design/fonte/MinhasTarefas.dc.html, R-20, R-22). */
@Composable
fun MinhasTarefasScreen(
    estado: MinhasTarefasEstado,
    aoAbrir: (Tarefa) -> Unit,
    aoConcluir: (Tarefa) -> Unit,
    aoAlternarConcluidas: () -> Unit,
    aoIrParaDeck: () -> Unit,
    aoNovaTarefa: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val e = DivvupTheme.spacing
    val s = estado.secoes
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = e.s4, end = e.s4, bottom = TarefasMedidas.respiroDaLista)) {
            Cabecalho(s.abertas)
            if (s.abertas == 0 && s.concluidas.isEmpty()) {
                Vazio(aoIrParaDeck)
            }
            listOf(Secao.Atrasadas to s.atrasadas, Secao.Hoje to s.hoje, Secao.Proximas to s.proximas)
                .filter { it.second.isNotEmpty() }
                .forEach { (secao, itens) -> SecaoAberta(secao, itens, estado, aoAbrir, aoConcluir) }
            if (s.concluidas.isNotEmpty()) Concluidas(s, estado, aoAlternarConcluidas, aoAbrir)
        }
        DivvupFab(
            DivvupIcone.Mais,
            stringResource(R.string.minhas_nova),
            aoNovaTarefa,
            Modifier.align(Alignment.BottomEnd).padding(e.s4),
        )
    }
}

/** "minhas tarefas" e o contador grande "3 de 5" com os segmentos. */
@Composable
private fun Cabecalho(abertas: Int) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val descricao = stringResource(R.string.minhas_contador_descricao, abertas, LIMITE_DE_ABERTAS.toString())
    Column {
        Row(
            Modifier.fillMaxWidth().padding(top = e.s2, bottom = TarefasMedidas.previaFundo - e.s2),
            horizontalArrangement = Arrangement.spacedBy(e.s3),
            verticalAlignment = Alignment.Bottom,
        ) {
            DivvupText(stringResource(R.string.minhas_titulo), t.h2.copy(fontSize = TarefasMedidas.tituloFonte), Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.clearAndSetSemantics { contentDescription = descricao }) {
                Row(verticalAlignment = Alignment.Bottom) {
                    DivvupText(abertas.toString(), t.h1.copy(fontSize = TarefasMedidas.contadorFonte, fontFeatureSettings = "tnum"))
                    Text(
                        stringResource(R.string.minhas_de, LIMITE_DE_ABERTAS.toString()),
                        style = t.small.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                        color = c.textMuted,
                    )
                }
                Row(Modifier.padding(top = e.s2), horizontalArrangement = Arrangement.spacedBy(e.s1)) {
                    repeat(LIMITE_DE_ABERTAS) { i ->
                        Box(
                            Modifier
                                .size(TarefasMedidas.segmentoLargura, TarefasMedidas.segmentoAltura)
                                .clip(DivvupTheme.shapes.sm)
                                .background(if (i < abertas) c.text else c.border),
                        )
                    }
                }
            }
        }
        DivvupDivider()
    }
}

@Composable
private fun Vazio(aoIrParaDeck: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(Modifier.padding(top = TarefasMedidas.vazioTopo), verticalArrangement = Arrangement.spacedBy(e.s4)) {
        DivvupLabel(stringResource(R.string.minhas_vazio_rotulo), cor = c.textMuted)
        DivvupText(stringResource(R.string.minhas_vazio_titulo), t.h2.copy(fontSize = TarefasMedidas.tituloFonte))
        Text(stringResource(R.string.minhas_vazio_texto), style = t.body, color = c.textMuted, modifier = Modifier.widthIn(max = TarefasMedidas.textoMaximoVazio))
        DivvupButton(stringResource(R.string.minhas_ir_deck), aoIrParaDeck, icone = DivvupIcone.Deck)
    }
}

@Composable
private fun SecaoAberta(secao: Secao, itens: List<Tarefa>, estado: MinhasTarefasEstado, aoAbrir: (Tarefa) -> Unit, aoConcluir: (Tarefa) -> Unit) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    val (rotulo, cor) = when (secao) {
        Secao.Atrasadas -> R.string.minhas_atrasadas to c.danger
        Secao.Hoje -> R.string.minhas_hoje to c.warning
        Secao.Proximas -> R.string.minhas_proximas to c.text
    }
    Column(Modifier.padding(top = e.s5)) {
        Row(Modifier.padding(bottom = e.s2), horizontalArrangement = Arrangement.spacedBy(e.s2), verticalAlignment = Alignment.CenterVertically) {
            DivvupLabel(stringResource(rotulo), cor = cor)
            DivvupLabel(stringResource(R.string.minhas_contagem, itens.size), cor = c.textMuted)
        }
        itens.forEach { t ->
            ItemAberto(secao, t, estado, aoAbrir = { aoAbrir(t) }, aoConcluir = { aoConcluir(t) })
            DivvupDivider()
        }
    }
}

@Composable
private fun ItemAberto(secao: Secao, tarefa: Tarefa, estado: MinhasTarefasEstado, aoAbrir: () -> Unit, aoConcluir: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val forma = DivvupTheme.shapes.md
    val p = prazoFalado(tarefa.prazo, estado.agora)
    val (texto, corPrazo, icone) = when (secao) {
        Secao.Atrasadas -> Triple(stringResource(R.string.minhas_venceu, textoDoDia(p.dia)), c.danger, DivvupIcone.Alerta)
        Secao.Hoje -> Triple(stringResource(R.string.minhas_prazo, textoDoDia(p.dia), textoDaHora(p.hora, p.minuto)), c.warning, DivvupIcone.Relogio)
        Secao.Proximas -> Triple(stringResource(R.string.minhas_prazo, textoDoDia(p.dia), textoDaHora(p.hora, p.minuto)), c.textMuted, DivvupIcone.Relogio)
    }
    val pontos = if (secao == Secao.Atrasadas) pontosDaConclusao(tarefa.pontos, tarefa.prazo, estado.agora) else tarefa.pontos
    Row(Modifier.fillMaxWidth().padding(vertical = e.s3), horizontalArrangement = Arrangement.spacedBy(e.s3), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).clickable(role = Role.Button, onClick = aoAbrir),
            horizontalArrangement = Arrangement.spacedBy(e.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painterResource(tarefa.categoria.ilustracao.res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(TarefasMedidas.miniatura).clip(forma).border(e.borda, c.border, forma),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(e.s1)) {
                Text(
                    tarefa.titulo,
                    style = t.ttl.copy(fontSize = TarefasMedidas.tituloItem, lineHeight = ALTURA_NORMAL),
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(e.s1)) {
                    DivvupIcon(icone, Modifier.padding(top = TOPO_ICONE), tamanho = TamanhoIcone.MuitoPequeno.dp, tint = corPrazo)
                    Text(
                        texto,
                        style = t.small.copy(fontWeight = if (secao == Secao.Proximas) FontWeight.Normal else FontWeight.SemiBold),
                        color = corPrazo,
                    )
                }
                if (tarefa.via == ViaDeAtribuicao.AUTO) {
                    DivvupStatusTag(stringResource(R.string.minhas_atribuida), TomSelo.Info, Modifier.heightIn(max = TarefasMedidas.seloPequenoAltura))
                }
            }
        }
        Text(
            pontos.toString(),
            style = t.pts.copy(fontSize = TarefasMedidas.pontosItem),
            color = if (secao == Secao.Atrasadas) c.danger else c.text,
        )
        val descricao = stringResource(R.string.minhas_concluir, tarefa.titulo)
        Box(
            Modifier
                .size(TarefasMedidas.botaoConcluir)
                .clip(DivvupTheme.shapes.sm)
                .border(e.borda, c.border, DivvupTheme.shapes.sm)
                .background(c.surface)
                .clickable(enabled = estado.concluindo == null, role = Role.Button, onClick = aoConcluir)
                .semantics { contentDescription = descricao },
            contentAlignment = Alignment.Center,
        ) { DivvupIcon(DivvupIcone.Check, tint = c.text) }
    }
}

/** "concluídas · 7 dias · 3   +125 pts ⌄", recolhível. */
@Composable
private fun Concluidas(s: SecoesDasMinhas, estado: MinhasTarefasEstado, aoAlternar: () -> Unit, aoAbrir: (Tarefa) -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val acao = stringResource(if (estado.concluidasAbertas) R.string.minhas_esconder else R.string.minhas_mostrar)
    Column(Modifier.padding(top = TarefasMedidas.secaoTopo)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = DivvupTheme.spacing.toqueMinimo)
                .clickable(role = Role.Button, onClickLabel = acao, onClick = aoAlternar)
                .padding(vertical = e.s2),
            horizontalArrangement = Arrangement.spacedBy(e.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DivvupLabel(stringResource(R.string.minhas_concluidas), cor = c.success)
            DivvupLabel(stringResource(R.string.minhas_contagem, s.concluidas.size), cor = c.textMuted)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.minhas_soma, formatarNumero(s.pontosConcluidas)),
                style = t.small.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = c.textMuted,
            )
            DivvupIcon(if (estado.concluidasAbertas) DivvupIcone.ChevD else DivvupIcone.ChevR, tamanho = TamanhoIcone.Pequeno.dp, tint = c.text)
        }
        if (estado.concluidasAbertas) {
            s.concluidas.forEach { tarefa ->
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { aoAbrir(tarefa) }.padding(vertical = e.s3),
                    horizontalArrangement = Arrangement.spacedBy(e.s3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(TarefasMedidas.bolinhaConcluida).clip(DivvupTheme.shapes.pill).background(c.successBg),
                        contentAlignment = Alignment.Center,
                    ) { DivvupIcon(DivvupIcone.Check, tamanho = TamanhoIcone.MuitoPequeno.dp, tint = c.success) }
                    Text(
                        tarefa.titulo,
                        style = t.body.copy(fontSize = TarefasMedidas.textoConcluida, textDecoration = TextDecoration.LineThrough),
                        color = c.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    tarefa.concluidaEm?.let { Text(textoDoDia(prazoFalado(it, estado.agora).dia), style = t.small, color = c.textMuted) }
                    Text(
                        stringResource(R.string.minhas_mais, (tarefa.pontosGanhos ?: 0).toString()),
                        style = t.botao.copy(fontFeatureSettings = "tnum"),
                        color = c.onAccent,
                        modifier = Modifier.clip(DivvupTheme.shapes.pill).background(c.accent).padding(horizontal = e.s2, vertical = e.s1),
                    )
                }
                DivvupDivider()
            }
        }
    }
}

private val ALTURA_NORMAL = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Em)
private val TOPO_ICONE = TarefasMedidas.tituloTopo - TarefasMedidas.tituloTopo / 2
