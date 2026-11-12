package br.app.divvup.feature.tasks.detalhe

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import br.app.divvup.core.designsystem.component.DivvupAvatarDaGaleria
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupDialog
import br.app.divvup.core.designsystem.component.DivvupDivider
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupPointsBadge
import br.app.divvup.core.designsystem.component.DivvupScrim
import br.app.divvup.core.designsystem.component.DivvupSnackbar
import br.app.divvup.core.designsystem.component.DivvupStatusTag
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.TomSelo
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.component.textoDaHora
import br.app.divvup.core.designsystem.component.textoDoDia
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.icon.icone
import br.app.divvup.core.designsystem.icon.ilustracao
import br.app.divvup.core.designsystem.icon.nome
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.casa.FUSO_DA_TEMPORADA
import br.app.divvup.core.domain.tarefa.DiaDoPrazo
import br.app.divvup.core.domain.tarefa.SituacaoParaMim
import br.app.divvup.core.domain.tarefa.pontosDaConclusao
import br.app.divvup.core.domain.tarefa.prazoFalado
import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.Tarefa
import br.app.divvup.core.model.ViaDeAtribuicao
import br.app.divvup.feature.tasks.R
import br.app.divvup.feature.tasks.TarefasMedidas
import java.time.Duration
import java.time.Instant
import br.app.divvup.core.designsystem.R as RDs

/** Diálogo aberto em C3. */
enum class DialogoDoDetalhe { Devolver, Cancelar }

/** Estado de C3. */
data class DetalheEstado(
    val tarefa: Tarefa? = null,
    val carregando: Boolean = true,
    val situacao: SituacaoParaMim = SituacaoParaMim.Aberta,
    val dono: Membro? = null,
    val autor: Membro? = null,
    val podeEditar: Boolean = false,
    val souAdmin: Boolean = false,
    val agora: Instant = Instant.EPOCH,
    val dialogo: DialogoDoDetalhe? = null,
    val ocupado: Boolean = false,
    /** Snackbar de 3 s (id da string). */
    val aviso: Int? = null,
)

/** C3 Detalhe da tarefa (docs/design/fonte/DetalheTarefa.dc.html; tabela de ações do TELAS.md C3). */
@Composable
fun DetalheScreen(
    estado: DetalheEstado,
    aoVoltar: () -> Unit,
    aoEditar: () -> Unit,
    aoAssumir: () -> Unit,
    aoConcluir: () -> Unit,
    aoPedirDevolucao: () -> Unit,
    aoPedirCancelamento: () -> Unit,
    aoConfirmar: () -> Unit,
    aoFecharDialogo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val tarefa = estado.tarefa
    Box(modifier.fillMaxSize().background(c.background)) {
        if (tarefa == null) {
            Column(Modifier.padding(e.s4)) {
                DivvupIconButton(DivvupIcone.ChevL, stringResource(R.string.detalhe_voltar), aoVoltar)
                if (!estado.carregando) Text(stringResource(R.string.detalhe_nao_existe), style = t.body, color = c.textMuted)
            }
            return@Box
        }
        val sit = estado.situacao
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Topo(tarefa, estado.podeEditar, aoVoltar, aoEditar)
                Column(Modifier.padding(start = e.s4, end = e.s4, top = TarefasMedidas.conteudoDetalheTopo, bottom = e.s5)) {
                    SeloDaSituacao(sit, estado.dono)
                    Text(tarefa.titulo, style = t.h3, color = c.text, modifier = Modifier.padding(top = e.s3))
                    tarefa.descricao?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = t.body, color = c.textMuted, modifier = Modifier.padding(top = e.s3))
                    }
                    Grade(estado, tarefa, Modifier.padding(top = e.s5))
                    Row(Modifier.padding(top = e.s3), horizontalArrangement = Arrangement.spacedBy(e.s2), verticalAlignment = Alignment.CenterVertically) {
                        DivvupAvatarDaGaleria(tarefa.criadaPorAvatar, TarefasMedidas.bolinhaConcluida)
                        Text(
                            stringResource(R.string.detalhe_criada, tarefa.criadaPorNome.substringBefore(' '), tarefa.criadaEm?.let { quando(it, estado.agora) }.orEmpty()),
                            style = t.small,
                            color = c.textMuted,
                        )
                    }
                    notaDoDetalhe(estado, tarefa)?.let { (fundo, cor, icone, texto) ->
                        Row(
                            Modifier.padding(top = CasaMedida).fillMaxWidth().clip(DivvupTheme.shapes.sm).background(fundo).padding(e.s3),
                            horizontalArrangement = Arrangement.spacedBy(e.s2),
                        ) {
                            DivvupIcon(icone, Modifier.padding(top = e.s1), tamanho = TamanhoIcone.Pequeno.dp, tint = cor)
                            Text(texto, style = t.botao, color = cor)
                        }
                    }
                }
            }
            Acoes(estado, aoAssumir, aoConcluir, aoPedirDevolucao, aoPedirCancelamento)
        }
        estado.aviso?.let {
            DivvupSnackbar(
                stringResource(it),
                icone = if (it in AVISOS_DE_SUCESSO) DivvupIcone.Check else DivvupIcone.Alerta,
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = e.s4, end = e.s4, bottom = TarefasMedidas.respiroDaLista / 2),
            )
        }
        estado.dialogo?.let { d ->
            DivvupScrim(onFechar = aoFecharDialogo, alinhamento = Alignment.Center) {
                val donoNome = estado.dono?.nome?.substringBefore(' ')
                DivvupDialog(
                    titulo = stringResource(if (d == DialogoDoDetalhe.Devolver) R.string.detalhe_devolver_titulo else R.string.detalhe_cancelar_titulo),
                    texto = when {
                        d == DialogoDoDetalhe.Devolver -> stringResource(R.string.detalhe_devolver_texto)
                        donoNome != null -> stringResource(R.string.detalhe_cancelar_texto, donoNome)
                        else -> stringResource(R.string.detalhe_cancelar_texto_sem_dono)
                    },
                    modifier = Modifier.padding(horizontal = e.s4),
                ) {
                    DivvupButton(stringResource(R.string.detalhe_dialogo_voltar), aoFecharDialogo, variante = VarianteBotao.Texto)
                    DivvupButton(
                        stringResource(if (d == DialogoDoDetalhe.Devolver) R.string.detalhe_devolver else R.string.detalhe_cancelar),
                        aoConfirmar,
                        variante = if (d == DialogoDoDetalhe.Devolver) VarianteBotao.Primario else VarianteBotao.PerigoCheio,
                        carregando = estado.ocupado,
                    )
                }
            }
        }
    }
}

/** Ilustração (248) com "voltar", "editar" e o selo da categoria saindo pela borda de baixo. */
@Composable
private fun Topo(tarefa: Tarefa, podeEditar: Boolean, aoVoltar: () -> Unit, aoEditar: () -> Unit) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    Box(Modifier.fillMaxWidth().height(TarefasMedidas.imagemDetalhe)) {
        Image(painterResource(tarefa.categoria.ilustracao.res), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(e.borda).background(c.border))
        BotaoSobreImagem(DivvupIcone.ChevL, stringResource(R.string.detalhe_voltar), aoVoltar, Modifier.align(Alignment.TopStart).padding(e.s2))
        if (podeEditar) BotaoSobreImagem(DivvupIcone.Editar, stringResource(R.string.detalhe_editar), aoEditar, Modifier.align(Alignment.TopEnd).padding(e.s2))
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = e.s4, y = TarefasMedidas.seloCategoriaFora)
                .height(TarefasMedidas.seloCategoriaAltura)
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
    }
}

@Composable
private fun BotaoSobreImagem(icone: DivvupIcone, descricao: String, aoClicar: () -> Unit, modifier: Modifier) {
    val c = DivvupTheme.colors
    Box(modifier.clip(DivvupTheme.shapes.sm).background(c.surface).border(DivvupTheme.spacing.borda, c.border, DivvupTheme.shapes.sm)) {
        DivvupIconButton(icone, descricao, aoClicar)
    }
}

/** Chip da situação (TELAS.md C3). */
@Composable
private fun SeloDaSituacao(sit: SituacaoParaMim, dono: Membro?) {
    val (texto, tom, icone) = when (sit) {
        SituacaoParaMim.Aberta -> Triple(stringResource(R.string.detalhe_aberta), TomSelo.Primario, DivvupIcone.Deck)
        SituacaoParaMim.Sua -> Triple(stringResource(R.string.detalhe_sua), TomSelo.Neutro, DivvupIcone.Check)
        SituacaoParaMim.SuaPerto -> Triple(stringResource(R.string.detalhe_sua), TomSelo.Neutro, DivvupIcone.Check)
        SituacaoParaMim.Atrasada -> Triple(stringResource(R.string.detalhe_atrasada), TomSelo.Erro, DivvupIcone.Alerta)
        SituacaoParaMim.DeOutra -> Triple(stringResource(R.string.detalhe_de, dono?.nome?.substringBefore(' ').orEmpty()), TomSelo.Neutro, DivvupIcone.Perfil)
        SituacaoParaMim.Concluida -> Triple(stringResource(R.string.detalhe_concluida), TomSelo.Sucesso, DivvupIcone.Check)
        SituacaoParaMim.Cancelada -> Triple(stringResource(R.string.detalhe_cancelada), TomSelo.Neutro, DivvupIcone.X)
    }
    DivvupStatusTag(texto, tom, icone = icone)
}

/** Pontos · prazo / dono · como, em grade com linhas. */
@Composable
private fun Grade(estado: DetalheEstado, tarefa: Tarefa, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val sit = estado.situacao
    Column(modifier) {
        DivvupDivider()
        Linha(
            {
                DivvupLabel(stringResource(R.string.detalhe_pontos), cor = c.textMuted)
                Row(Modifier.padding(top = e.s2), horizontalArrangement = Arrangement.spacedBy(e.s2), verticalAlignment = Alignment.CenterVertically) {
                    val pontos = when (sit) {
                        SituacaoParaMim.Atrasada -> pontosDaConclusao(tarefa.pontos, tarefa.prazo, estado.agora)
                        SituacaoParaMim.Concluida -> tarefa.pontosGanhos ?: tarefa.pontos
                        else -> tarefa.pontos
                    }
                    DivvupPointsBadge(pontos, stringResource(RDs.string.pts), sinal = sit == SituacaoParaMim.Concluida)
                    if (sit == SituacaoParaMim.Atrasada) {
                        Text(tarefa.pontos.toString(), style = t.small.copy(textDecoration = TextDecoration.LineThrough), color = c.textMuted)
                    }
                }
            },
            {
                DivvupLabel(stringResource(R.string.detalhe_prazo), cor = c.textMuted)
                val (absoluto, relativo, cor) = textosDoPrazo(estado, tarefa)
                Text(absoluto, style = t.body.copy(fontWeight = FontWeight.SemiBold), color = c.text, modifier = Modifier.padding(top = e.s2))
                Text(relativo, style = t.small.copy(fontWeight = FontWeight.SemiBold), color = cor)
            },
        )
        DivvupDivider()
        Linha(
            {
                DivvupLabel(stringResource(R.string.detalhe_dono), cor = c.textMuted)
                Row(Modifier.padding(top = e.s2), horizontalArrangement = Arrangement.spacedBy(e.s2), verticalAlignment = Alignment.CenterVertically) {
                    val dono = estado.dono
                    if (dono == null) {
                        // Sem dono: só o contorno tracejado do avatar.
                        Box(
                            Modifier.size(TarefasMedidas.bolinhaConcluida).drawBehind {
                                val traco = TarefasMedidas.tracoSemDono.toPx()
                                drawCircle(
                                    c.textMuted,
                                    style = Stroke(width = e.borda.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(traco, traco))),
                                )
                            },
                        )
                    } else {
                        DivvupAvatarDaGaleria(dono.avatarId, TarefasMedidas.bolinhaConcluida)
                    }
                    Text(
                        when {
                            dono == null -> stringResource(R.string.detalhe_ninguem)
                            sit == SituacaoParaMim.DeOutra -> dono.nome.substringBefore(' ')
                            else -> stringResource(R.string.detalhe_voce)
                        },
                        style = t.body.copy(fontWeight = FontWeight.SemiBold),
                        color = c.text,
                    )
                }
            },
            {
                DivvupLabel(stringResource(R.string.detalhe_como), cor = c.textMuted)
                Text(
                    stringResource(
                        when (tarefa.via) {
                            ViaDeAtribuicao.SWIPE -> R.string.detalhe_pelo_swipe
                            ViaDeAtribuicao.AUTO -> R.string.detalhe_automatica
                            null -> R.string.detalhe_no_deck
                        },
                    ),
                    style = t.body.copy(fontWeight = FontWeight.SemiBold),
                    color = c.text,
                    modifier = Modifier.padding(top = e.s2),
                )
            },
        )
        DivvupDivider()
    }
}

@Composable
private fun Linha(esquerda: @Composable () -> Unit, direita: @Composable () -> Unit) {
    val e = DivvupTheme.spacing
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.weight(1f).padding(top = e.s4, bottom = e.s4, end = e.s3)) { esquerda() }
        Box(Modifier.width(e.borda).fillMaxHeight().background(DivvupTheme.colors.border))
        Column(Modifier.weight(1f).padding(top = e.s4, bottom = e.s4, start = e.s4)) { direita() }
    }
}

/** "sáb., 8 nov · 18:00" e "em 2 dias" / "vence amanhã" / "venceu há 1 dia" / "no prazo". */
@Composable
private fun textosDoPrazo(estado: DetalheEstado, tarefa: Tarefa): Triple<String, String, Color> {
    val c = DivvupTheme.colors
    val agora = estado.agora
    val prazo = tarefa.prazo.atZone(FUSO_DA_TEMPORADA)
    val p = prazoFalado(tarefa.prazo, agora)
    val diaCurto = stringArrayResource(RDs.array.prazo_dias_curtos)[prazo.dayOfWeek.value - 1]
    val data = stringResource(RDs.string.prazo_data, prazo.dayOfMonth, stringArrayResource(RDs.array.prazo_meses)[prazo.monthValue - 1])
    val relogio = stringResource(RDs.string.prazo_relogio, prazo.hour, prazo.minute)
    if (estado.situacao == SituacaoParaMim.Concluida) {
        val feita = tarefa.concluidaEm ?: agora
        val f = prazoFalado(feita, agora)
        val hora = feita.atZone(FUSO_DA_TEMPORADA)
        return Triple(
            stringResource(R.string.detalhe_concluida_em, textoDoDia(f.dia), stringResource(RDs.string.prazo_relogio, hora.hour, hora.minute)),
            stringResource(if (!feita.isAfter(tarefa.prazo)) R.string.detalhe_no_prazo else R.string.detalhe_depois_do_prazo),
            c.textMuted,
        )
    }
    val absoluto = stringResource(R.string.detalhe_prazo_absoluto, stringResource(RDs.string.prazo_atalho, diaCurto, data), relogio)
    val falta = Duration.between(agora, tarefa.prazo)
    val relativo = when {
        falta.isNegative -> {
            val dias = (-falta.toHours() / HORAS_NO_DIA).toInt()
            if (dias >= 1) pluralStringResource(R.plurals.detalhe_venceu_dias, dias, dias) else stringResource(R.string.detalhe_venceu_horas, maxOf(-falta.toHours().toInt(), 1))
        }
        estado.situacao == SituacaoParaMim.Aberta -> stringResource(R.string.detalhe_vence, textoDoDia(p.dia))
        falta.toHours() >= HORAS_NO_DIA -> (falta.toHours() / HORAS_NO_DIA).toInt().let { pluralStringResource(R.plurals.detalhe_em_dias, it, it) }
        falta.toHours() >= 1 -> stringResource(R.string.detalhe_em_horas, falta.toHours().toInt())
        else -> stringResource(R.string.detalhe_em_minutos, maxOf(falta.toMinutes().toInt(), 1))
    }
    val cor = when (estado.situacao) {
        SituacaoParaMim.Atrasada -> c.danger
        SituacaoParaMim.SuaPerto -> c.warning
        else -> c.textMuted
    }
    return Triple(absoluto, relativo, cor)
}

/** "hoje às 9h", "ontem às 20h", "seg.". */
@Composable
private fun quando(instante: Instant, agora: Instant): String {
    val p = prazoFalado(instante, agora)
    return when (p.dia) {
        is DiaDoPrazo.Hoje, is DiaDoPrazo.Ontem ->
            stringResource(R.string.minhas_prazo, textoDoDia(p.dia), textoDaHora(p.hora, p.minuto))
        else -> textoDoDia(p.dia)
    }
}

/** Nota da situação: fundo, cor, ícone e texto (TELAS.md C3). */
@Composable
private fun notaDoDetalhe(estado: DetalheEstado, tarefa: Tarefa): NotaDoDetalhe? {
    val c = DivvupTheme.colors
    return when (estado.situacao) {
        SituacaoParaMim.SuaPerto -> NotaDoDetalhe(
            c.infoBg,
            c.info,
            DivvupIcone.Info,
            stringResource(if (tarefa.via == ViaDeAtribuicao.AUTO) R.string.detalhe_nota_automatica else R.string.detalhe_nota_perto),
        )
        SituacaoParaMim.Atrasada -> NotaDoDetalhe(
            c.dangerBg,
            c.danger,
            DivvupIcone.Alerta,
            stringResource(R.string.detalhe_nota_atrasada, pontosDaConclusao(tarefa.pontos, tarefa.prazo, estado.agora).toString()),
        )
        SituacaoParaMim.DeOutra -> if (estado.souAdmin) NotaDoDetalhe(c.surfaceAlt, c.text, DivvupIcone.Cadeado, stringResource(R.string.detalhe_nota_outra)) else null
        SituacaoParaMim.Concluida -> NotaDoDetalhe(
            c.successBg,
            c.success,
            DivvupIcone.Check,
            stringResource(R.string.detalhe_nota_concluida, (tarefa.pontosGanhos ?: tarefa.pontos).toString()),
        )
        SituacaoParaMim.Cancelada -> NotaDoDetalhe(c.surfaceAlt, c.text, DivvupIcone.X, stringResource(R.string.detalhe_nota_cancelada))
        else -> null
    }
}

private data class NotaDoDetalhe(val fundo: Color, val cor: Color, val icone: DivvupIcone, val texto: String)

/** Barra de ações por situação (TELAS.md C3). */
@Composable
private fun Acoes(estado: DetalheEstado, aoAssumir: () -> Unit, aoConcluir: () -> Unit, aoPedirDevolucao: () -> Unit, aoPedirCancelamento: () -> Unit) {
    val e = DivvupTheme.spacing
    val sit = estado.situacao
    val tarefa = estado.tarefa ?: return
    val minha = sit == SituacaoParaMim.Sua || sit == SituacaoParaMim.SuaPerto || sit == SituacaoParaMim.Atrasada
    val temAcoes = sit == SituacaoParaMim.Aberta || minha || (sit == SituacaoParaMim.DeOutra && estado.souAdmin)
    if (!temAcoes) return
    DivvupDivider()
    Row(
        Modifier.fillMaxWidth().background(DivvupTheme.colors.background).padding(start = e.s4, end = e.s4, top = e.s3, bottom = e.s4),
        horizontalArrangement = Arrangement.spacedBy(e.s2),
    ) {
        if (sit == SituacaoParaMim.Sua) {
            DivvupButton(stringResource(R.string.detalhe_devolver), aoPedirDevolucao, variante = VarianteBotao.Contorno, icone = DivvupIcone.Devolver)
        }
        when {
            minha -> DivvupButton(
                stringResource(R.string.detalhe_concluir),
                aoConcluir,
                icone = DivvupIcone.Check,
                carregando = estado.ocupado && estado.dialogo == null,
                modifier = Modifier.weight(1f),
            )
            sit == SituacaoParaMim.Aberta -> DivvupButton(
                stringResource(R.string.detalhe_assumir, tarefa.pontos.toString()),
                aoAssumir,
                variante = VarianteBotao.Aceitar,
                icone = DivvupIcone.Check,
                carregando = estado.ocupado,
                modifier = Modifier.weight(1f),
            )
            else -> DivvupButton(
                stringResource(R.string.detalhe_cancelar),
                aoPedirCancelamento,
                variante = VarianteBotao.Perigo,
                icone = DivvupIcone.X,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private const val HORAS_NO_DIA = 24L
private val AVISOS_DE_SUCESSO = setOf(R.string.detalhe_assumida, R.string.detalhe_devolvida, R.string.detalhe_cancelada_aviso)
private val CasaMedida = TarefasMedidas.tituloFundo
