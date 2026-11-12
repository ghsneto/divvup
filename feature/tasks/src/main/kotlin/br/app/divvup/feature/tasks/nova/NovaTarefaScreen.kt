package br.app.divvup.feature.tasks.nova

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupCard
import br.app.divvup.core.designsystem.component.DivvupChip
import br.app.divvup.core.designsystem.component.DivvupDialog
import br.app.divvup.core.designsystem.component.DivvupDivider
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupPointsBadge
import br.app.divvup.core.designsystem.component.DivvupScrim
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupTextField
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.component.MensagemErro
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.TamanhoSelo
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.component.textoDoAtalho
import br.app.divvup.core.designsystem.component.textoDoDia
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.icon.icone
import br.app.divvup.core.designsystem.icon.ilustracao
import br.app.divvup.core.designsystem.icon.nome
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.tarefa.AtalhoDePrazo
import br.app.divvup.core.domain.tarefa.DESCRICAO_MAXIMA
import br.app.divvup.core.domain.tarefa.DiaDoPrazo
import br.app.divvup.core.domain.tarefa.ErroDeTarefa
import br.app.divvup.core.domain.tarefa.TITULO_MAXIMO
import br.app.divvup.core.domain.tarefa.prazoDoAtalho
import br.app.divvup.core.domain.tarefa.prazoFalado
import br.app.divvup.core.model.Categoria
import br.app.divvup.core.model.Peso
import br.app.divvup.feature.tasks.R
import br.app.divvup.feature.tasks.TarefasMedidas

/**
 * C2 Nova tarefa (docs/design/fonte/NovaTarefa.dc.html, R-09, R-10, R-11, R-13). O campo
 * descrição do desenho mostra "[object Object]" por erro do mockup: aqui ele começa vazio com o
 * placeholder (docs/design/README.md).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NovaTarefaScreen(
    estado: NovaTarefaEstado,
    aoFechar: () -> Unit,
    aoMudarTitulo: (String) -> Unit,
    aoMudarDescricao: (String) -> Unit,
    aoEscolherCategoria: (Categoria) -> Unit,
    aoEscolherPeso: (Peso) -> Unit,
    aoEscolherAtalho: (AtalhoDePrazo) -> Unit,
    aoAbrirData: () -> Unit,
    aoAbrirHora: () -> Unit,
    aoCriar: () -> Unit,
    aoContinuarEditando: () -> Unit,
    aoDescartar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val f = estado.formulario
    Box(modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize()) {
            DivvupTopBar {
                DivvupIconButton(DivvupIcone.X, stringResource(R.string.nova_fechar), aoFechar)
                Spacer(Modifier.weight(1f))
                DivvupLabel(stringResource(R.string.nova_contexto), Modifier.padding(end = e.s2), cor = c.textMuted)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = e.s4, end = e.s4, bottom = e.s5)) {
                DivvupText(
                    stringResource(R.string.nova_titulo),
                    t.h2.copy(fontSize = TarefasMedidas.tituloFonte),
                    Modifier.padding(top = TarefasMedidas.tituloTopo, bottom = TarefasMedidas.tituloFundo),
                )
                DivvupLabel(stringResource(R.string.nova_previa), cor = c.textMuted, soltoEmBloco = true)
                Previa(estado, Modifier.padding(top = e.s2, bottom = TarefasMedidas.previaFundo))
                Campo(stringResource(R.string.nova_campo_titulo), null, f.titulo.length, TITULO_MAXIMO) {
                    DivvupTextField(
                        f.titulo,
                        aoMudarTitulo,
                        placeholder = stringResource(R.string.nova_campo_titulo_exemplo),
                        teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                }
                Campo(
                    stringResource(R.string.nova_campo_descricao),
                    stringResource(R.string.nova_opcional),
                    f.descricao.length,
                    DESCRICAO_MAXIMA,
                    Modifier.padding(top = e.s4),
                ) {
                    DivvupTextField(
                        f.descricao,
                        aoMudarDescricao,
                        placeholder = stringResource(R.string.nova_campo_descricao_exemplo),
                        linhaUnica = false,
                        altura = TarefasMedidas.descricaoAltura,
                        alinharTopo = true,
                        teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                }
                DivvupLabel(stringResource(R.string.nova_categoria), Modifier.padding(top = TarefasMedidas.secaoTopo, bottom = e.s3))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(e.s2), verticalArrangement = Arrangement.spacedBy(e.s2)) {
                    Categoria.entries.forEach { cat ->
                        DivvupChip(stringResource(cat.nome), selecionado = cat == f.categoria, icone = cat.icone, onClick = { aoEscolherCategoria(cat) })
                    }
                }
                DivvupLabel(stringResource(R.string.nova_peso), Modifier.padding(top = TarefasMedidas.secaoTopo, bottom = e.s3))
                SeletorDePeso(f.peso, aoEscolherPeso)
                DivvupLabel(stringResource(R.string.nova_prazo), Modifier.padding(top = TarefasMedidas.secaoTopo, bottom = e.s3))
                FlowRow(Modifier.padding(bottom = e.s3), horizontalArrangement = Arrangement.spacedBy(e.s2), verticalArrangement = Arrangement.spacedBy(e.s2)) {
                    estado.atalhos.forEach { a ->
                        DivvupChip(
                            textoDoAtalho(prazoFalado(prazoDoAtalho(a, estado.agora).toInstant(), estado.agora)),
                            selecionado = a == estado.atalho,
                            onClick = { aoEscolherAtalho(a) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(e.s2)) {
                    val p = prazoFalado(f.prazo.toInstant(), estado.agora)
                    val data = stringResource(
                        br.app.divvup.core.designsystem.R.string.prazo_data,
                        f.prazo.dayOfMonth,
                        androidx.compose.ui.res.stringArrayResource(br.app.divvup.core.designsystem.R.array.prazo_meses)[f.prazo.monthValue - 1],
                    )
                    val textoData = if (p.dia is DiaDoPrazo.Data) data else stringResource(br.app.divvup.core.designsystem.R.string.prazo_atalho, textoDoDia(p.dia), data)
                    CaixaDePrazo(stringResource(R.string.nova_data), DivvupIcone.Calendario, textoData, aoAbrirData, Modifier.weight(PESO_DATA))
                    CaixaDePrazo(
                        stringResource(R.string.nova_hora),
                        DivvupIcone.Relogio,
                        stringResource(br.app.divvup.core.designsystem.R.string.prazo_relogio, f.prazo.hour, f.prazo.minute),
                        aoAbrirHora,
                        Modifier.weight(1f),
                    )
                }
                if (estado.prazoCurto && !estado.prazoNoPassado) {
                    Row(
                        Modifier.padding(top = e.s3).fillMaxWidth().clip(DivvupTheme.shapes.sm).background(c.warningBg).padding(e.s3),
                        horizontalArrangement = Arrangement.spacedBy(e.s2),
                    ) {
                        DivvupIcon(DivvupIcone.Alerta, Modifier.padding(top = e.s1), tamanho = TamanhoIcone.Pequeno.dp, tint = c.warning)
                        Text(stringResource(R.string.nova_aviso_24h), style = t.botao, color = c.warning)
                    }
                }
                when (estado.erro) {
                    ErroDeTarefa.PrazoNoPassado -> MensagemErro(stringResource(R.string.nova_erro_passado))
                    null -> Unit
                    else -> MensagemErro(stringResource(R.string.nova_erro_conexao))
                }
            }
            DivvupDivider()
            DivvupButton(
                stringResource(R.string.nova_criar),
                aoCriar,
                larguraTotal = true,
                habilitado = estado.formulario.podeCriar,
                carregando = estado.criando,
                modifier = Modifier.background(c.background).padding(start = e.s4, end = e.s4, top = e.s3, bottom = e.s4),
            )
        }
        if (estado.confirmandoDescarte) {
            DivvupScrim(onFechar = aoContinuarEditando, alinhamento = Alignment.Center) {
                DivvupDialog(
                    titulo = stringResource(R.string.nova_descartar_titulo),
                    texto = stringResource(R.string.nova_descartar_texto),
                    modifier = Modifier.padding(horizontal = e.s4),
                ) {
                    DivvupButton(stringResource(R.string.nova_continuar), aoContinuarEditando, variante = VarianteBotao.Texto)
                    DivvupButton(stringResource(R.string.nova_descartar), aoDescartar, variante = VarianteBotao.PerigoCheio)
                }
            }
        }
    }
}

/** Rótulo à esquerda (com o "· opcional") e o contador à direita, sobre o campo. */
@Composable
private fun Campo(rotulo: String, extra: String?, usado: Int, maximo: Int, modifier: Modifier = Modifier, campo: @Composable () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    Column(modifier, verticalArrangement = Arrangement.spacedBy(DivvupTheme.spacing.s2)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            DivvupLabel(rotulo)
            extra?.let { Text(" $it", style = t.small.copy(fontSize = TarefasMedidas.rotuloPequeno), color = c.textMuted) }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.nova_contador, usado, maximo), style = t.small.copy(fontFeatureSettings = "tnum"), color = c.textMuted)
        }
        campo()
    }
}

/** Prévia: ilustração da categoria, nome, título, prazo curto e pontos. */
@Composable
private fun Previa(estado: NovaTarefaEstado, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val f = estado.formulario
    DivvupCard(modifier.fillMaxWidth().height(TarefasMedidas.previaAltura)) {
        Row(Modifier.fillMaxSize()) {
            Image(
                painterResource(f.categoria.ilustracao.res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(TarefasMedidas.previaImagem).fillMaxHeight(),
            )
            Box(Modifier.width(e.borda).fillMaxHeight().background(c.border))
            Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = e.s4, vertical = e.s3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(e.s1), verticalAlignment = Alignment.CenterVertically) {
                    DivvupIcon(f.categoria.icone, tamanho = TamanhoIcone.MuitoPequeno.dp, tint = c.textMuted)
                    DivvupLabel(stringResource(f.categoria.nome), cor = c.textMuted, maxLinhas = 1)
                }
                Text(
                    f.titulo.ifBlank { stringResource(R.string.nova_previa_vazia) },
                    style = t.ttl,
                    color = if (f.titulo.isBlank()) c.textMuted else c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = e.s1),
                )
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(textoDoAtalho(prazoFalado(f.prazo.toInstant(), estado.agora)), style = t.small, color = c.textMuted)
                    DivvupPointsBadge(f.peso.pontos, stringResource(br.app.divvup.core.designsystem.R.string.pts), tamanho = TamanhoSelo.Pequeno)
                }
            }
        }
    }
}

/** Três colunas (1 · 1 · 1,3) num contorno: a escolhida em `accent`. */
@Composable
private fun SeletorDePeso(atual: Peso, aoEscolher: (Peso) -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val forma = DivvupTheme.shapes.md
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(forma)
            .background(c.surface)
            .border(e.borda, c.border, forma),
    ) {
        Peso.entries.forEachIndexed { i, p ->
            val escolhido = p == atual
            if (i > 0) Box(Modifier.width(e.borda).fillMaxHeight().background(c.border))
            Column(
                Modifier
                    .weight(if (p == Peso.PESADA) PESO_PESADA else 1f)
                    .heightIn(min = TarefasMedidas.pesoAltura)
                    .background(if (escolhido) c.accent else Color.Transparent)
                    .selectable(selected = escolhido, role = Role.RadioButton) { aoEscolher(p) }
                    .padding(e.s3),
                verticalArrangement = Arrangement.spacedBy(e.s1),
            ) {
                val cor = if (escolhido) c.onAccent else c.text
                DivvupLabel(stringResource(p.nome), cor = cor)
                DivvupText(
                    p.pontos.toString(),
                    t.h2.copy(fontSize = TarefasMedidas.pesoFonte, fontFeatureSettings = "tnum"),
                    cor = cor,
                )
            }
        }
    }
}

/** Caixa (`.in`) com ícone que abre o seletor de data ou hora. */
@Composable
private fun CaixaDePrazo(rotulo: String, icone: DivvupIcone, texto: String, aoAbrir: () -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val forma = DivvupTheme.shapes.sm
    Column(modifier, verticalArrangement = Arrangement.spacedBy(e.s2)) {
        Text(rotulo, style = t.small, color = c.textMuted)
        Row(
            Modifier
                .fillMaxWidth()
                .height(e.campo)
                .clip(forma)
                .background(c.surface)
                .border(e.borda, c.border, forma)
                .clickable(role = Role.Button, onClick = aoAbrir)
                .padding(horizontal = e.s4),
            horizontalArrangement = Arrangement.spacedBy(e.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DivvupIcon(icone, tamanho = TamanhoIcone.Pequeno.dp, tint = c.textMuted)
            Text(texto, style = t.body.copy(fontWeight = FontWeight.Normal, fontFeatureSettings = "tnum"), color = c.text, maxLines = 1)
        }
    }
}

private const val PESO_PESADA = 1.3f
private const val PESO_DATA = 1.5f
