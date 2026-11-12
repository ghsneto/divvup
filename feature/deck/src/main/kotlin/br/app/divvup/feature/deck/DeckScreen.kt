package br.app.divvup.feature.deck

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import br.app.divvup.core.designsystem.component.DivvupAvatarDaGaleria
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupPointsBadge
import br.app.divvup.core.designsystem.component.DivvupProgressBar
import br.app.divvup.core.designsystem.component.DivvupSkeleton
import br.app.divvup.core.designsystem.component.DivvupStatusTag
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.TamanhoSelo
import br.app.divvup.core.designsystem.component.TomSelo
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.component.formatarNumero
import br.app.divvup.core.designsystem.component.textoDoVencimento
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.icon.icone
import br.app.divvup.core.designsystem.icon.ilustracao
import br.app.divvup.core.designsystem.icon.nome
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.tarefa.LIMITE_DE_ABERTAS
import br.app.divvup.core.domain.tarefa.pesoDe
import br.app.divvup.core.domain.tarefa.prazoFalado
import br.app.divvup.core.domain.tarefa.urgente
import br.app.divvup.core.model.Tarefa
import java.time.Instant
import br.app.divvup.core.designsystem.R as RDs

/**
 * C1 Deck (docs/design/fonte/Main.dc.html, R-14): faixa de status, pilha de cartões e botões
 * Passar · Assumir · "+". Estados `DeckCarregando`, `DeckVazio`, `DeckOffline` e `DeckErro`.
 * O swipe e os botões Passar/Assumir ganham ação nas issues #12 e #13.
 */
@Composable
fun DeckScreen(
    estado: DeckEstado,
    aoPassar: () -> Unit,
    aoAssumir: () -> Unit,
    aoNovaTarefa: () -> Unit,
    aoDetalhes: (Tarefa) -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val e = DivvupTheme.spacing
    Column(modifier.fillMaxSize()) {
        FaixaDeStatus(estado)
        Column(Modifier.weight(1f).fillMaxWidth().padding(start = e.s4, end = e.s4, top = DeckMedidas.conteudoTopo)) {
            if (estado.offline && estado.situacao == SituacaoDoDeck.Cartoes) AvisoOffline(Modifier.padding(bottom = e.s3))
            when (estado.situacao) {
                SituacaoDoDeck.Carregando -> Carregando()
                SituacaoDoDeck.Erro -> Erro(aoTentarDeNovo)
                SituacaoDoDeck.Vazio -> Vazio(aoNovaTarefa)
                SituacaoDoDeck.Cartoes -> Pilha(estado, aoDetalhes)
            }
        }
        if (estado.situacao == SituacaoDoDeck.Cartoes) {
            Botoes(swipeLiberado = !estado.offline, aoPassar, aoAssumir, aoNovaTarefa)
        }
    }
}

/** "325 pts no mês" · sequência · segmentos das tarefas assumidas ("3/5"). */
@Composable
private fun FaixaDeStatus(estado: DeckEstado) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Row(
        Modifier.fillMaxWidth().height(DeckMedidas.faixaAltura).padding(start = e.s4, end = e.s4, bottom = e.s2),
        horizontalArrangement = Arrangement.spacedBy(e.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.height(DeckMedidas.faixaLinha), verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.deck_pts_mes, formatarNumero(estado.pontosDoMes)), style = t.pts.copy(lineHeight = UMA_LINHA), color = c.text)
            DivvupLabel(stringResource(R.string.deck_no_mes), cor = c.textMuted)
        }
        val sequencia = stringResource(R.string.deck_sequencia)
        DivvupStatusTag(
            pluralStringResource(R.plurals.deck_dias, estado.sequencia, estado.sequencia),
            TomSelo.Neutro,
            Modifier.semantics { contentDescription = sequencia },
            icone = DivvupIcone.Raio,
        )
        Spacer(Modifier.weight(1f))
        val descricao = stringResource(R.string.deck_abertas_descricao, estado.abertas, LIMITE_DE_ABERTAS.toString())
        Row(
            Modifier.clearAndSetSemantics { contentDescription = descricao },
            horizontalArrangement = Arrangement.spacedBy(e.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(e.s1)) {
                repeat(LIMITE_DE_ABERTAS) { i ->
                    Box(
                        Modifier
                            .size(DeckMedidas.segmentoLargura, DeckMedidas.segmentoAltura)
                            .clip(DivvupTheme.shapes.sm)
                            .background(if (i < estado.abertas) c.text else c.border),
                    )
                }
            }
            Text(
                stringResource(R.string.deck_abertas, estado.abertas, LIMITE_DE_ABERTAS),
                style = t.small.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = c.text,
            )
        }
    }
}

/** Até 3 cartões: o de cima inteiro e os de trás descendo 12dp e encolhendo 5% cada, escurecidos. */
@Composable
private fun Pilha(estado: DeckEstado, aoDetalhes: (Tarefa) -> Unit) {
    val c = DivvupTheme.colors
    val altura = if (estado.offline) DeckMedidas.pilhaComAviso else DeckMedidas.pilhaAltura
    Box(Modifier.fillMaxWidth().heightIn(max = altura).fillMaxHeight()) {
        estado.cartoes.take(POSES.size).withIndex().reversed().forEach { (k, tarefa) ->
            val (y, escala, sombra) = POSES[k]
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = DeckMedidas.respiroDaPilha)
                    .graphicsLayer {
                        translationY = y * density
                        scaleX = escala
                        scaleY = escala
                        transformOrigin = TransformOrigin(CENTRO, BASE)
                    },
            ) {
                CartaoDoDeck(tarefa, estado.agora, aoDetalhes = { aoDetalhes(tarefa) }, Modifier.fillMaxSize())
                if (sombra > 0f) {
                    Box(Modifier.fillMaxSize().clip(DivvupTheme.shapes.md).background(c.background.copy(alpha = sombra)))
                }
            }
        }
    }
}

/** Cartão da tarefa (`TaskCard`): ilustração, categoria, urgente, título, descrição, prazo, autor e pontos. */
@Composable
fun CartaoDoDeck(tarefa: Tarefa, agora: Instant, aoDetalhes: () -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val forma = DivvupTheme.shapes.md
    val vence = textoDoVencimento(prazoFalado(tarefa.prazo, agora))
    val descricao = stringResource(R.string.deck_cartao_descricao, tarefa.titulo, tarefa.pontos.toString(), vence)
    Column(
        modifier
            .clip(forma)
            .background(c.surface)
            .border(e.borda, c.border, forma)
            .semantics(mergeDescendants = false) { contentDescription = descricao },
    ) {
        Box(Modifier.fillMaxWidth().height(DeckMedidas.imagemAltura)) {
            Image(
                painterResource(tarefa.categoria.ilustracao.res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                Modifier
                    .padding(e.s3)
                    .height(DeckMedidas.seloCategoriaAltura)
                    .clip(DivvupTheme.shapes.pill)
                    .background(c.surface)
                    .border(e.borda, c.border, DivvupTheme.shapes.pill)
                    .padding(horizontal = e.s3),
                horizontalArrangement = Arrangement.spacedBy(e.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DivvupIcon(tarefa.categoria.icone, tamanho = TamanhoIcone.MuitoPequeno.dp, tint = c.text)
                DivvupLabel(stringResource(tarefa.categoria.nome))
            }
            if (urgente(tarefa.prazo, agora)) {
                DivvupStatusTag(
                    stringResource(R.string.deck_urgente),
                    TomSelo.Atencao,
                    Modifier.align(Alignment.TopEnd).padding(top = DeckMedidas.seloUrgenteTopo, end = e.s3),
                    icone = DivvupIcone.Relogio,
                )
            }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(e.borda).background(c.border))
        }
        Column(Modifier.weight(1f).padding(start = DeckMedidas.respiroDaPilha - e.s1, end = DeckMedidas.respiroDaPilha - e.s1, top = DeckMedidas.respiroDaPilha - e.s1, bottom = e.s4)) {
            Text(tarefa.titulo, style = t.h3.copy(fontSize = DeckMedidas.tituloFonte), color = c.text)
            tarefa.descricao?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = t.small, color = c.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = e.s2))
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(e.s3)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(e.s2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(e.s2), verticalAlignment = Alignment.CenterVertically) {
                        DivvupIcon(DivvupIcone.Relogio, tamanho = TamanhoIcone.Pequeno.dp, tint = c.text)
                        Text(vence, style = t.body.copy(fontWeight = FontWeight.SemiBold), color = c.text, maxLines = 1)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(e.s2), verticalAlignment = Alignment.CenterVertically) {
                        DivvupAvatarDaGaleria(tarefa.criadaPorAvatar, DeckMedidas.avatarAutor)
                        Text(stringResource(R.string.deck_por, tarefa.criadaPorNome.substringBefore(' ')), style = t.small, color = c.textMuted, maxLines = 1)
                        Text(
                            stringResource(R.string.deck_detalhes),
                            style = t.small.copy(fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline),
                            color = c.link,
                            modifier = Modifier.clickable(role = Role.Button, onClick = aoDetalhes),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(e.s1)) {
                    DivvupLabel(stringResource(pesoDe(tarefa.pontos).nome), cor = c.textMuted)
                    DivvupPointsBadge(tarefa.pontos, stringResource(RDs.string.pts), tamanho = TamanhoSelo.Grande)
                }
            }
        }
    }
}

/** Passar (danger, 96) · Assumir (accent) · "+" (primary, 56), com 56 de altura. */
@Composable
private fun Botoes(swipeLiberado: Boolean, aoPassar: () -> Unit, aoAssumir: () -> Unit, aoNovaTarefa: () -> Unit) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    Row(Modifier.fillMaxWidth().padding(horizontal = e.s4, vertical = e.s3), horizontalArrangement = Arrangement.spacedBy(e.s2)) {
        Column(
            Modifier
                .width(DeckMedidas.passarLargura)
                .height(DeckMedidas.botaoAltura)
                .alpha(if (swipeLiberado) 1f else OPACIDADE_DESLIGADO)
                .clip(DivvupTheme.shapes.sm)
                .border(e.borda, c.danger, DivvupTheme.shapes.sm)
                .clickable(enabled = swipeLiberado, role = Role.Button, onClick = aoPassar),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            DivvupIcon(DivvupIcone.X, tint = c.danger)
            Text(stringResource(R.string.deck_passar), style = DivvupTheme.typography.botao, color = c.danger)
        }
        DivvupButton(
            stringResource(R.string.deck_assumir),
            aoAssumir,
            variante = VarianteBotao.Aceitar,
            icone = DivvupIcone.Check,
            habilitado = swipeLiberado,
            modifier = Modifier.weight(1f).height(DeckMedidas.botaoAltura),
            estiloTexto = DivvupTheme.typography.botao.copy(fontSize = DeckMedidas.assumirFonte),
        )
        Box(
            Modifier
                .size(DeckMedidas.maisLargura, DeckMedidas.botaoAltura)
                .clip(DivvupTheme.shapes.sm)
                .background(c.primary)
                .clickable(role = Role.Button, onClick = aoNovaTarefa),
            contentAlignment = Alignment.Center,
        ) {
            DivvupIcon(DivvupIcone.Mais, descricao = stringResource(R.string.deck_nova), tamanho = TamanhoIcone.Grande.dp, tint = c.onPrimary)
        }
    }
}

@Composable
private fun AvisoOffline(modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    Row(
        modifier.fillMaxWidth().clip(DivvupTheme.shapes.md).background(c.infoBg).padding(horizontal = e.s4, vertical = e.s3),
        horizontalArrangement = Arrangement.spacedBy(e.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DivvupIcon(DivvupIcone.Offline, tint = c.info)
        Text(stringResource(R.string.deck_offline), style = DivvupTheme.typography.botao, color = c.text)
    }
}

/** Skeleton do cartão e a barra de carregamento (`DeckCarregando`). */
@Composable
private fun Carregando() {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    val forma = DivvupTheme.shapes.md
    val descricao = stringResource(R.string.deck_carregando)
    Column(Modifier.semantics { contentDescription = descricao }) {
        Column(Modifier.fillMaxWidth().height(DeckMedidas.carregandoAltura).clip(forma).background(c.surface).border(e.borda, c.border, forma)) {
            Box(Modifier.fillMaxWidth().height(DeckMedidas.imagemAltura).background(c.surfaceAlt))
            Column(Modifier.padding(DeckMedidas.respiroDaPilha - e.s1), verticalArrangement = Arrangement.spacedBy(e.s3)) {
                DivvupSkeleton(Modifier.fillMaxWidth(LARGURA_TITULO).height(DeckMedidas.carregandoTitulo))
                DivvupSkeleton(Modifier.fillMaxWidth(LARGURA_LINHA1).height(DeckMedidas.carregandoLinha))
                DivvupSkeleton(Modifier.fillMaxWidth(LARGURA_LINHA2).height(DeckMedidas.carregandoLinha))
                Row(
                    Modifier.fillMaxWidth().padding(top = DeckMedidas.carregandoRodapeTopo - e.s3),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    DivvupSkeleton(Modifier.fillMaxWidth(LARGURA_PRAZO).height(DeckMedidas.carregandoPrazo))
                    Box(
                        Modifier
                            .size(DeckMedidas.carregandoSeloLargura, DeckMedidas.carregandoSeloAltura)
                            .clip(DivvupTheme.shapes.pill)
                            .background(c.surfaceAlt),
                    )
                }
            }
        }
        DivvupProgressBar(Modifier.padding(top = DeckMedidas.barraCarregandoTopo).fillMaxWidth())
    }
}

/** `DeckErro`: "o deck não carregou" + "Tentar de novo". */
@Composable
private fun Erro(aoTentarDeNovo: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(Modifier.padding(start = e.s1, end = e.s1, top = DeckMedidas.erroTopo), verticalArrangement = Arrangement.spacedBy(e.s4)) {
        Row(horizontalArrangement = Arrangement.spacedBy(e.s2), verticalAlignment = Alignment.CenterVertically) {
            DivvupIcon(DivvupIcone.Alerta, tamanho = TamanhoIcone.Pequeno.dp, tint = c.danger)
            DivvupLabel(stringResource(R.string.deck_erro_rotulo), cor = c.danger)
        }
        DivvupText(stringResource(R.string.deck_erro_titulo), t.h2.copy(fontSize = DeckMedidas.tituloErro))
        Text(stringResource(R.string.deck_erro_texto), style = t.body, color = c.textMuted, modifier = Modifier.widthIn(max = DeckMedidas.textoMaximoErro))
        DivvupButton(stringResource(R.string.deck_tentar), aoTentarDeNovo, icone = DivvupIcone.Refresh)
    }
}

/** `DeckVazio`: dois cartões tracejados, o selo lima e "nada pendente por aqui". */
@Composable
private fun Vazio(aoNovaTarefa: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(Modifier.padding(start = e.s1, end = e.s1, top = DeckMedidas.vazioTopo), verticalArrangement = Arrangement.spacedBy(e.s4)) {
        Box(Modifier.padding(bottom = e.s2).fillMaxWidth().height(DeckMedidas.desenhoVazioAltura).clearAndSetSemantics { }) {
            CartaoTracejado(Modifier.offset(DeckMedidas.cartaoTracejado1X, DeckMedidas.cartaoTracejado1Y).rotate(GIRO_1))
            CartaoTracejado(Modifier.offset(DeckMedidas.cartaoTracejado2X, DeckMedidas.cartaoTracejado2Y).rotate(GIRO_2))
            Box(
                Modifier
                    .offset(DeckMedidas.selinhoX, DeckMedidas.selinhoY)
                    .size(DeckMedidas.selinho)
                    .clip(DivvupTheme.shapes.pill)
                    .background(c.accent),
                contentAlignment = Alignment.Center,
            ) { DivvupIcon(DivvupIcone.Check, tint = c.onAccent) }
        }
        DivvupLabel(stringResource(R.string.deck_vazio_rotulo), cor = c.textMuted)
        DivvupText(stringResource(R.string.deck_vazio_titulo), t.h2.copy(fontSize = DeckMedidas.tituloVazio))
        Text(stringResource(R.string.deck_vazio_texto), style = t.body, color = c.textMuted, modifier = Modifier.widthIn(max = DeckMedidas.textoMaximoVazio))
        DivvupButton(stringResource(R.string.deck_criar), aoNovaTarefa, icone = DivvupIcone.Mais)
    }
}

@Composable
private fun CartaoTracejado(modifier: Modifier) {
    val c = DivvupTheme.colors
    val raio = DivvupTheme.spacing.s3
    val borda = DivvupTheme.spacing.borda
    Box(
        modifier
            .size(DeckMedidas.cartaoTracejadoLargura, DeckMedidas.cartaoTracejadoAltura)
            .drawBehind {
                val traco = DeckMedidas.tracejado.toPx()
                drawRoundRect(
                    color = c.border,
                    cornerRadius = CornerRadius(raio.toPx()),
                    style = Stroke(width = borda.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(traco, traco))),
                )
            },
    )
}

/** Posições dos cartões de trás: deslocamento (dp), escala e escurecimento (`POSE` do desenho). */
private val POSES = listOf(Triple(0f, 1f, 0f), Triple(12f, 0.95f, 0.12f), Triple(24f, 0.9f, 0.24f))
/** Mesma opacidade de `.btn[disabled]`. */
private const val OPACIDADE_DESLIGADO = 0.42f
private const val CENTRO = 0.5f
private const val BASE = 1f
private const val GIRO_1 = -6f
private const val GIRO_2 = 3f
private const val LARGURA_TITULO = 0.78f
private const val LARGURA_LINHA1 = 0.92f
private const val LARGURA_LINHA2 = 0.6f
private const val LARGURA_PRAZO = 0.46f
private val UMA_LINHA = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Em)
