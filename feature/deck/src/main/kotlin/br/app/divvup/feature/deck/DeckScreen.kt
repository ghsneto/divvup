package br.app.divvup.feature.deck

import androidx.compose.foundation.Image
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import br.app.divvup.core.designsystem.component.DivvupSnackbar
import kotlinx.coroutines.launch
import kotlin.math.abs
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
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
 * Swipe e botões: assumir (R-15, R-20), passar (R-16), desfazer por 3 s (R-18) e rever (R-17).
 */
@Composable
fun DeckScreen(
    estado: DeckEstado,
    aoPassar: (Tarefa) -> Unit,
    aoAssumir: (Tarefa) -> Unit,
    aoDesfazer: () -> Unit,
    aoRever: () -> Unit,
    aoNovaTarefa: () -> Unit,
    aoMinhasTarefas: () -> Unit,
    aoDetalhes: (Tarefa) -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val e = DivvupTheme.spacing
    val comCartoes = estado.situacao == SituacaoDoDeck.Cartoes
    Column(modifier.fillMaxSize()) {
        val aviso = estado.aviso
        if (aviso == null) FaixaDeStatus(estado) else AvisoNaFaixa(aviso, estado.podeDesfazer, aoDesfazer)
        Column(Modifier.weight(1f).fillMaxWidth().padding(start = e.s4, end = e.s4, top = DeckMedidas.conteudoTopo)) {
            if (comCartoes && estado.noLimite) AvisoDeLimite(aoMinhasTarefas, Modifier.padding(bottom = e.s3))
            if (comCartoes && estado.offline) AvisoOffline(Modifier.padding(bottom = e.s3))
            when (estado.situacao) {
                SituacaoDoDeck.Carregando -> Carregando()
                SituacaoDoDeck.Erro -> Erro(aoTentarDeNovo)
                SituacaoDoDeck.Vazio -> Vazio(aoNovaTarefa)
                SituacaoDoDeck.Passadas -> Passadas(estado.passadas, aoRever, aoNovaTarefa)
                SituacaoDoDeck.Cartoes -> Pilha(estado, aoPassar, aoAssumir, aoDetalhes)
            }
        }
        if (comCartoes) {
            val topo = estado.cartoes.first()
            Botoes(
                podePassar = !estado.offline,
                podeAssumir = !estado.offline && !estado.noLimite,
                noLimite = estado.noLimite,
                aoPassar = { aoPassar(topo) },
                aoAssumir = { aoAssumir(topo) },
                aoNovaTarefa = aoNovaTarefa,
            )
        }
    }
}

/** O snackbar de C1 fica no lugar da faixa de status por 3 s, com "Desfazer" (R-18). */
@Composable
private fun AvisoNaFaixa(aviso: AvisoDoDeck, podeDesfazer: Boolean, aoDesfazer: () -> Unit) {
    val e = DivvupTheme.spacing
    val (texto, complemento, icone) = when (aviso) {
        is AvisoDoDeck.Assumida -> Triple(
            stringResource(R.string.deck_aviso_assumida),
            stringResource(R.string.deck_aviso_assumida_pts, aviso.pontos.toString()),
            DivvupIcone.Check,
        )
        AvisoDoDeck.Passada -> Triple(stringResource(R.string.deck_aviso_passada), stringResource(R.string.deck_aviso_passada_volta), DivvupIcone.X)
        is AvisoDoDeck.JaPegaram -> Triple(
            stringResource(R.string.deck_aviso_ja_pegaram),
            if (aviso.porQuem.isBlank()) stringResource(R.string.deck_aviso_saiu) else stringResource(R.string.deck_aviso_quem, aviso.porQuem.substringBefore(' ')),
            DivvupIcone.Alerta,
        )
        AvisoDoDeck.Falhou -> Triple(stringResource(R.string.deck_aviso_falhou), null, DivvupIcone.Alerta)
    }
    Box(Modifier.fillMaxWidth().height(DeckMedidas.faixaAltura).padding(start = e.s4, end = e.s4, bottom = e.s2)) {
        DivvupSnackbar(
            texto,
            icone = icone,
            complemento = complemento,
            acao = if (podeDesfazer) stringResource(R.string.deck_desfazer) else null,
            iconeAcao = DivvupIcone.Desfazer,
            onAcao = aoDesfazer,
            contagem = true,
            alturaMinima = DeckMedidas.faixaLinha,
        )
    }
}

/** R-20: "Você já tem 5 tarefas abertas…" (C1 `DeckLimite`). */
@Composable
private fun AvisoDeLimite(aoMinhasTarefas: () -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Row(
        modifier.fillMaxWidth().clip(DivvupTheme.shapes.md).background(c.warningBg).padding(horizontal = e.s4, vertical = e.s3),
        horizontalArrangement = Arrangement.spacedBy(e.s3),
    ) {
        DivvupIcon(DivvupIcone.Cadeado, Modifier.padding(top = e.s1), tint = c.warning)
        Column {
            Text(stringResource(R.string.deck_limite, LIMITE_DE_ABERTAS.toString()), style = t.botao, color = c.text)
            Text(
                stringResource(R.string.deck_ver_minhas),
                style = t.botao.copy(textDecoration = TextDecoration.Underline),
                color = c.warning,
                modifier = Modifier.clickable(role = Role.Button, onClick = aoMinhasTarefas),
            )
        }
    }
}

/** `DeckPassadas`: miniaturas das passadas, "você passou X tarefas", Rever e Criar tarefa (R-17). */
@Composable
private fun Passadas(passadas: List<Tarefa>, aoRever: () -> Unit, aoNovaTarefa: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(Modifier.padding(start = e.s1, end = e.s1, top = DeckMedidas.vazioTopo), verticalArrangement = Arrangement.spacedBy(e.s4)) {
        Row(Modifier.padding(bottom = e.s3).clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(e.s2)) {
            passadas.take(GIROS_MINIATURAS.size).forEachIndexed { i, tarefa ->
                Image(
                    painterResource(tarefa.categoria.ilustracao.res),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .rotate(GIROS_MINIATURAS[i])
                        .size(DeckMedidas.miniaturaLargura, DeckMedidas.miniaturaAltura)
                        .clip(DivvupTheme.shapes.md)
                        .border(e.borda, c.border, DivvupTheme.shapes.md),
                )
            }
        }
        DivvupLabel(stringResource(R.string.deck_passadas_rotulo), cor = c.textMuted)
        DivvupText(
            pluralStringResource(R.plurals.deck_passadas_titulo, passadas.size, passadas.size),
            t.h2.copy(fontSize = DeckMedidas.tituloVazio),
        )
        Text(stringResource(R.string.deck_passadas_texto), style = t.body, color = c.textMuted, modifier = Modifier.widthIn(max = DeckMedidas.textoMaximoPassadas))
        Row(horizontalArrangement = Arrangement.spacedBy(e.s2)) {
            DivvupButton(stringResource(R.string.deck_rever), aoRever, icone = DivvupIcone.Desfazer)
            DivvupButton(stringResource(R.string.deck_criar), aoNovaTarefa, variante = VarianteBotao.Contorno)
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

/** Até 3 cartões: o de cima arrastável e os de trás descendo 12dp e encolhendo 5% cada, escurecidos. */
@Composable
private fun Pilha(estado: DeckEstado, aoPassar: (Tarefa) -> Unit, aoAssumir: (Tarefa) -> Unit, aoDetalhes: (Tarefa) -> Unit) {
    val c = DivvupTheme.colors
    val altura = if (estado.offline || estado.noLimite) DeckMedidas.pilhaComAviso else DeckMedidas.pilhaAltura
    Box(Modifier.fillMaxWidth().heightIn(max = altura).fillMaxHeight()) {
        estado.cartoes.take(POSES.size).withIndex().reversed().forEach { (k, tarefa) ->
            key(tarefa.id) {
                val (y, escala, sombra) = POSES[k]
                val camada = Modifier
                    .fillMaxSize()
                    .padding(bottom = DeckMedidas.respiroDaPilha)
                    .graphicsLayer {
                        translationY = y * density
                        scaleX = escala
                        scaleY = escala
                        transformOrigin = TransformOrigin(CENTRO, BASE)
                    }
                if (k == 0) {
                    CartaoArrastavel(
                        tarefa = tarefa,
                        agora = estado.agora,
                        podePassar = !estado.offline,
                        podeAssumir = !estado.offline && !estado.noLimite,
                        aoPassar = { aoPassar(tarefa) },
                        aoAssumir = { aoAssumir(tarefa) },
                        aoDetalhes = { aoDetalhes(tarefa) },
                        modifier = camada,
                    )
                } else {
                    // Os de trás só aparecem pela borda de baixo: basta a casca do cartão, escurecida.
                    Box(
                        camada
                            .clearAndSetSemantics { }
                            .clip(DivvupTheme.shapes.md)
                            .background(c.surface)
                            .border(DivvupTheme.spacing.borda, c.border, DivvupTheme.shapes.md),
                    ) {
                        Box(Modifier.fillMaxSize().background(c.background.copy(alpha = sombra)))
                    }
                }
            }
        }
    }
}

/**
 * Cartão do topo: arrasta e gira (dx/18 graus). Passou de 110dp, ou um peteleco rápido, conta como
 * swipe: à direita assume (overlay lima "É MINHA"), à esquerda passa (overlay danger "PASSO").
 * Antes disso, volta com mola.
 */
@Composable
private fun CartaoArrastavel(
    tarefa: Tarefa,
    agora: Instant,
    podePassar: Boolean,
    podeAssumir: Boolean,
    aoPassar: () -> Unit,
    aoAssumir: () -> Unit,
    aoDetalhes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val escopo = rememberCoroutineScope()
    val dx = remember { Animatable(0f) }
    val dy = remember { Animatable(0f) }
    val densidade = LocalDensity.current.density
    val limite = DeckMedidas.limiteDoSwipe.value * densidade
    val progresso = (abs(dx.value) / (DeckMedidas.inicioDoOverlay.value * densidade)).coerceIn(0f, 1f)
    Box(
        modifier
            .graphicsLayer {
                translationX = dx.value
                translationY = dy.value * ARRASTO_VERTICAL
                rotationZ = dx.value / densidade / GIRO_POR_DP
            }
            .pointerInput(podePassar, podeAssumir) {
                val velocidade = VelocityTracker()
                detectDragGestures(
                    onDragStart = { velocidade.resetTracking() },
                    onDragEnd = {
                        val vx = velocidade.calculateVelocity().x / densidade
                        val peteleco = abs(vx) > PETELECO_DP_POR_S && abs(dx.value) > DeckMedidas.petelecoMinimo.value * densidade &&
                            (vx > 0) == (dx.value > 0)
                        val direita = dx.value > limite || (peteleco && dx.value > 0)
                        val esquerda = dx.value < -limite || (peteleco && dx.value < 0)
                        escopo.launch {
                            when {
                                direita && podeAssumir -> {
                                    dx.animateTo(size.width * SAIDA, tween(DURACAO_SAIDA_MS))
                                    aoAssumir()
                                }
                                esquerda && podePassar -> {
                                    dx.animateTo(-size.width * SAIDA, tween(DURACAO_SAIDA_MS))
                                    aoPassar()
                                }
                                else -> {
                                    launch { dy.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                    dx.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                            }
                        }
                    },
                ) { mudanca, arrasto ->
                    mudanca.consume()
                    velocidade.addPosition(mudanca.uptimeMillis, mudanca.position)
                    escopo.launch {
                        dx.snapTo(dx.value + arrasto.x)
                        dy.snapTo(dy.value + arrasto.y)
                    }
                }
            },
    ) {
        CartaoDoDeck(tarefa, agora, aoDetalhes, Modifier.fillMaxSize())
        if (dx.value > 0f) Carimbo(stringResource(R.string.deck_e_minha), c.aceitar, c.onAceitar, progresso, direita = true)
        if (dx.value < 0f) Carimbo(stringResource(R.string.deck_passo), c.passar, c.onDanger, progresso, direita = false)
    }
}

/** Overlay do swipe: fundo na cor da ação e o carimbo girado ("É MINHA" / "PASSO"). */
@Composable
private fun Carimbo(texto: String, fundo: Color, conteudo: Color, opacidade: Float, direita: Boolean) {
    val e = DivvupTheme.spacing
    Box(
        Modifier
            .fillMaxSize()
            .clip(DivvupTheme.shapes.md)
            .graphicsLayer { alpha = opacidade }
            .background(fundo)
            .padding(horizontal = e.s5, vertical = DeckMedidas.carimboTopo),
        contentAlignment = if (direita) Alignment.TopStart else Alignment.TopEnd,
    ) {
        val escala = CARIMBO_MIN + (1f - CARIMBO_MIN) * (opacidade * CARIMBO_RITMO).coerceAtMost(1f)
        Text(
            texto,
            style = DivvupTheme.typography.h2.copy(fontSize = DeckMedidas.carimboFonte, letterSpacing = CARIMBO_ESPACO),
            color = conteudo,
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = if (direita) -CARIMBO_GIRO else CARIMBO_GIRO
                    scaleX = escala
                    scaleY = escala
                }
                .border(e.borda, conteudo, DivvupTheme.shapes.sm)
                .padding(horizontal = e.s3, vertical = e.s1),
        )
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
        // Como no desenho: com pouca altura, o conteúdo passa do cartão (cortado) em vez de espremer.
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .alturaNaturalOuMaior()
                .padding(start = DeckMedidas.respiroDaPilha - e.s1, end = DeckMedidas.respiroDaPilha - e.s1, top = DeckMedidas.respiroDaPilha - e.s1, bottom = e.s4),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(tarefa.titulo, style = t.h3.copy(fontSize = DeckMedidas.tituloFonte), color = c.text)
                tarefa.descricao?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = t.small, color = c.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = e.s2))
                }
            }
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

/** Ocupa pelo menos a altura dada e, se precisar, mais (o excedente fica fora e é cortado). */
private fun Modifier.alturaNaturalOuMaior(): Modifier = layout { medida, limites ->
    val p = medida.measure(limites.copy(minHeight = limites.maxHeight, maxHeight = Constraints.Infinity))
    layout(p.width, limites.maxHeight) { p.place(0, 0) }
}

/** Passar (danger, 96) · Assumir (accent; "Limite 5/5" com cadeado no limite) · "+" (primary, 56). */
@Composable
private fun Botoes(
    podePassar: Boolean,
    podeAssumir: Boolean,
    noLimite: Boolean,
    aoPassar: () -> Unit,
    aoAssumir: () -> Unit,
    aoNovaTarefa: () -> Unit,
) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    Row(Modifier.fillMaxWidth().padding(horizontal = e.s4, vertical = e.s3), horizontalArrangement = Arrangement.spacedBy(e.s2)) {
        Column(
            Modifier
                .width(DeckMedidas.passarLargura)
                .height(DeckMedidas.botaoAltura)
                .alpha(if (podePassar) 1f else OPACIDADE_DESLIGADO)
                .clip(DivvupTheme.shapes.sm)
                .border(e.borda, c.danger, DivvupTheme.shapes.sm)
                .clickable(enabled = podePassar, role = Role.Button, onClick = aoPassar),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            DivvupIcon(DivvupIcone.X, tint = c.danger)
            Text(stringResource(R.string.deck_passar), style = DivvupTheme.typography.botao, color = c.danger)
        }
        DivvupButton(
            if (noLimite) stringResource(R.string.deck_limite_botao, LIMITE_DE_ABERTAS, LIMITE_DE_ABERTAS) else stringResource(R.string.deck_assumir),
            aoAssumir,
            variante = VarianteBotao.Aceitar,
            icone = if (noLimite) DivvupIcone.Cadeado else DivvupIcone.Check,
            habilitado = podeAssumir,
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
private const val ARRASTO_VERTICAL = 0.35f
private const val GIRO_POR_DP = 18f
private const val PETELECO_DP_POR_S = 550f
private const val SAIDA = 1.6f
private const val DURACAO_SAIDA_MS = 300
private const val CARIMBO_MIN = 0.8f
private const val CARIMBO_RITMO = 1.4f
private const val CARIMBO_GIRO = 8f
private val CARIMBO_ESPACO = androidx.compose.ui.unit.TextUnit(0.12f, androidx.compose.ui.unit.TextUnitType.Em)
private val GIROS_MINIATURAS = listOf(-6f, 3f, -2f, 5f)
private const val BASE = 1f
private const val GIRO_1 = -6f
private const val GIRO_2 = 3f
private const val LARGURA_TITULO = 0.78f
private const val LARGURA_LINHA1 = 0.92f
private const val LARGURA_LINHA2 = 0.6f
private const val LARGURA_PRAZO = 0.46f
private val UMA_LINHA = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Em)
