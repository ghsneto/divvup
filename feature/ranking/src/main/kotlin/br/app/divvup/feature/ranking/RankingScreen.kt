package br.app.divvup.feature.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import br.app.divvup.core.designsystem.component.DivvupAvatarDaGaleria
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupPointsBadge
import br.app.divvup.core.designsystem.component.DivvupScrim
import br.app.divvup.core.designsystem.component.DivvupSheet
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.TamanhoSelo
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.component.formatarNumero
import br.app.divvup.core.designsystem.component.textoDaHora
import br.app.divvup.core.designsystem.component.textoDoDia
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.icon.icone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.casa.FUSO_DA_TEMPORADA
import br.app.divvup.core.domain.casa.ResumoDaTemporada
import br.app.divvup.core.domain.ranking.Equilibrio
import br.app.divvup.core.domain.ranking.FraseDoEquilibrio
import br.app.divvup.core.domain.ranking.Posicao
import br.app.divvup.core.domain.tarefa.DiaDoPrazo
import br.app.divvup.core.domain.tarefa.prazoFalado
import br.app.divvup.core.model.Evento
import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.TipoDeEvento
import java.time.Duration
import java.time.Instant
import java.time.YearMonth

/** Abas de D1. */
enum class AbaDoRanking { Mes, Geral, Historico }

/** Estado de D1. */
data class RankingEstado(
    val aba: AbaDoRanking = AbaDoRanking.Mes,
    val eu: String? = null,
    val temporada: ResumoDaTemporada = ResumoDaTemporada(1, 0),
    val doMes: List<Posicao> = emptyList(),
    val geral: List<Posicao> = emptyList(),
    val equilibrio: Equilibrio? = null,
    /** "desde mar. 2025" da aba Geral. */
    val casaDesde: Instant? = null,
    val historico: List<Evento> = emptyList(),
    /** Moradores ativos para os filtros do Histórico (você primeiro). */
    val filtros: List<Membro> = emptyList(),
    /** uid filtrado no Histórico; `null` = todos. */
    val filtro: String? = null,
    val ajudaAberta: Boolean = false,
    val agora: Instant = Instant.EPOCH,
)

/** D1 Ranking: Este mês, Geral e Histórico (docs/design/fonte/Ranking.dc.html, R-25, R-27, R-28, R-45). */
@Composable
fun RankingScreen(
    estado: RankingEstado,
    aoTrocarAba: (AbaDoRanking) -> Unit,
    aoAbrirMembro: (String) -> Unit,
    aoTemporadas: () -> Unit,
    aoFiltrar: (String?) -> Unit,
    aoAjuda: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val e = DivvupTheme.spacing
    Box(modifier.fillMaxSize().background(DivvupTheme.colors.background)) {
        Column(Modifier.fillMaxSize()) {
            Abas(estado.aba, aoTrocarAba)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = e.s4, end = e.s4, bottom = e.s5)) {
                when (estado.aba) {
                    AbaDoRanking.Mes -> Mes(estado, aoAbrirMembro, aoTemporadas, aoAjuda)
                    AbaDoRanking.Geral -> Geral(estado, aoAbrirMembro)
                    AbaDoRanking.Historico -> Historico(estado, aoFiltrar)
                }
            }
        }
        if (estado.ajudaAberta) Ajuda { aoAjuda(false) }
    }
}

@Composable
private fun Abas(atual: AbaDoRanking, aoTrocar: (AbaDoRanking) -> Unit) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    val nomes = listOf(R.string.ranking_aba_mes, R.string.ranking_aba_geral, R.string.ranking_aba_historico)
    Column {
        Row(Modifier.padding(horizontal = e.s4), horizontalArrangement = Arrangement.spacedBy(e.s2)) {
            AbaDoRanking.entries.forEachIndexed { i, aba ->
                val sel = aba == atual
                val traco = if (sel) c.primary else Color.Transparent
                Box(
                    Modifier
                        .height(RankingMedidas.abaAltura)
                        .drawBehind {
                            val h = RankingMedidas.abaTraco.toPx()
                            drawRect(traco, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
                        }
                        .selectable(selected = sel, role = Role.Tab) { aoTrocar(aba) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(nomes[i]),
                        style = DivvupTheme.typography.botao.copy(fontSize = RankingMedidas.abaFonte),
                        color = if (sel) c.text else c.textMuted,
                        modifier = Modifier.padding(horizontal = RankingMedidas.abaPaddingH),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(e.borda).background(c.border))
    }
}

// ---- Este mês ----

@Composable
private fun Mes(estado: RankingEstado, aoAbrirMembro: (String) -> Unit, aoTemporadas: () -> Unit, aoAjuda: (Boolean) -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val temp = estado.temporada
    Row(Modifier.padding(top = RankingMedidas.cabecalhoTopo, bottom = e.s4), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(e.s3)) {
        Column(Modifier.weight(1f)) {
            DivvupLabel(stringResource(R.string.ranking_temporada), cor = c.textMuted)
            DivvupText(stringArrayResource(R.array.ranking_meses)[temp.mes - 1], t.h2)
        }
        Column(Modifier.width(RankingMedidas.faltamLargura).padding(bottom = e.s2)) {
            Text(
                if (temp.diasRestantes == 0) stringResource(R.string.ranking_ultimo_dia) else pluralStringResource(R.plurals.ranking_faltam, temp.diasRestantes, temp.diasRestantes),
                style = t.small.copy(fontWeight = FontWeight.SemiBold),
                color = c.text,
            )
            val tamanho = YearMonth.of(estado.agora.atZone(FUSO_DA_TEMPORADA).year, temp.mes).lengthOfMonth()
            val passou = (tamanho - temp.diasRestantes).toFloat() / tamanho
            Box(Modifier.padding(top = e.s2).fillMaxWidth().height(RankingMedidas.barraFaltam).clip(DivvupTheme.shapes.sm).background(c.surfaceAlt)) {
                Box(Modifier.fillMaxWidth(passou).height(RankingMedidas.barraFaltam).background(c.text))
            }
        }
    }
    val ninguem = estado.doMes.none { it.pontos > 0 }
    if (ninguem) {
        Text(stringResource(R.string.ranking_vazio), style = t.body, color = c.textMuted, modifier = Modifier.padding(vertical = e.s5))
    } else {
        Podio(estado.doMes.take(PODIO), estado.eu, aoAbrirMembro)
    }
    estado.equilibrio?.let { Equilibrio(it, ninguem, aoAjuda) }
    // Abaixo do pódio: você (se estiver nele) em destaque e depois do 4º em diante.
    val lista = if (ninguem) estado.doMes else estado.doMes.filter { it.membro.uid == estado.eu && it in estado.doMes.take(PODIO) } + estado.doMes.drop(PODIO)
    Column(Modifier.padding(top = RankingMedidas.listaTopo)) {
        lista.forEach { p -> Linha(p, p.membro.uid == estado.eu, ex = false, aoAbrirMembro) }
    }
    Row(
        Modifier.padding(top = e.s3).heightIn(min = e.toqueMinimo).clickable(role = Role.Button, onClick = aoTemporadas),
        horizontalArrangement = Arrangement.spacedBy(e.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.ranking_temporadas_anteriores), style = t.botao, color = c.text)
        DivvupIcon(DivvupIcone.SetaR, tamanho = TamanhoIcone.Pequeno.dp, tint = c.text)
    }
}

private const val PODIO = 3

/** Pódio 2º · 1º · 3º (colunas 1 · 1,25 · 1). */
@Composable
private fun Podio(tres: List<Posicao>, eu: String?, aoAbrirMembro: (String) -> Unit) {
    val e = DivvupTheme.spacing
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(e.s2), verticalAlignment = Alignment.Bottom) {
        val ordem = listOf(1, 0, 2)
        ordem.forEach { i ->
            val p = tres.getOrNull(i)
            val peso = if (i == 0) RankingMedidas.PESO_PRIMEIRO else 1f
            if (p == null) {
                Spacer(Modifier.weight(peso))
            } else {
                Degrau(p, i, p.membro.uid == eu, Modifier.weight(peso).clickable(role = Role.Button) { aoAbrirMembro(p.membro.uid) })
            }
        }
    }
}

@Composable
private fun Degrau(p: Posicao, indice: Int, souEu: Boolean, modifier: Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val primeiro = indice == 0
    val (altura, fundo, texto) = when (indice) {
        0 -> Triple(RankingMedidas.podioAlturaPrimeiro, c.ultramarino, c.onUltramarino)
        1 -> Triple(RankingMedidas.podioAlturaSegundo, c.surface, c.text)
        else -> Triple(RankingMedidas.podioAlturaTerceiro, c.surfaceAlt, c.text)
    }
    val forma = RoundedCornerShape(topStart = DivvupTheme.spacing.s3, topEnd = DivvupTheme.spacing.s3)
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(e.s2),
        horizontalAlignment = when (indice) {
            0 -> Alignment.CenterHorizontally
            1 -> Alignment.Start
            else -> Alignment.End
        },
    ) {
        DivvupAvatarDaGaleria(
            p.membro.avatarId,
            if (primeiro) RankingMedidas.podioAvatarPrimeiro else RankingMedidas.podioAvatar,
            corDaBorda = if (primeiro) c.lima else c.border,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .height(altura)
                .clip(forma)
                .background(fundo)
                .border(e.borda, if (primeiro) c.ultramarino else c.border, forma)
                .padding(e.s3),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                DivvupText(
                    stringResource(R.string.ranking_posicao, p.posicao),
                    t.h1.copy(fontSize = if (primeiro) RankingMedidas.podioPosicaoPrimeiro else RankingMedidas.podioPosicao),
                    cor = texto,
                )
                if (primeiro) DivvupIcon(DivvupIcone.Trofeu, tamanho = TamanhoIcone.Pequeno.dp, tint = c.lima, descricao = stringResource(R.string.ranking_campeao))
            }
            Column {
                Text(if (souEu) stringResource(R.string.ranking_voce) else primeiroNome(p.membro), style = t.botao, color = texto, maxLines = 1)
                Text(
                    stringResource(R.string.ranking_pts, formatarNumero(p.pontos)),
                    style = t.botao.copy(fontFeatureSettings = "tnum"),
                    color = texto,
                    modifier = Modifier.alpha(RankingMedidas.OPACIDADE_PONTOS),
                )
            }
        }
    }
}

/** Bloco "equilíbrio" colado embaixo do pódio (R-45). */
@Composable
private fun Equilibrio(eq: Equilibrio, semPodio: Boolean, aoAjuda: (Boolean) -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val raio = e.s3
    val forma = if (semPodio) DivvupTheme.shapes.md else RoundedCornerShape(bottomStart = raio, bottomEnd = raio)
    Row(
        Modifier.fillMaxWidth().clip(forma).background(c.surface).border(e.borda, c.border, forma).padding(e.s4),
        horizontalArrangement = Arrangement.spacedBy(e.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            DivvupLabel(stringResource(R.string.ranking_equilibrio), cor = c.textMuted)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(e.s1)) {
                DivvupText(eq.indice.toString(), t.h1.copy(fontSize = RankingMedidas.equilibrioNumero, fontFeatureSettings = "tnum"))
                Text(stringResource(R.string.ranking_de_cem), style = t.small, color = c.textMuted, modifier = Modifier.padding(bottom = e.s1))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(
                    when (eq.frase) {
                        FraseDoEquilibrio.BemDividido -> R.string.ranking_bem_dividido
                        FraseDoEquilibrio.QuaseEquilibrado -> R.string.ranking_quase
                        FraseDoEquilibrio.Desequilibrado -> R.string.ranking_desequilibrado
                    },
                ),
                style = t.small.copy(fontWeight = FontWeight.SemiBold),
                color = c.text,
            )
            Segmentos(eq.indice, Modifier.padding(top = e.s2))
            Text(
                eq.lider?.let { stringResource(R.string.ranking_lider, primeiroNome(it), eq.percentualDoLider) } ?: stringResource(R.string.ranking_sem_pontos_equilibrio),
                style = t.small.copy(fontSize = RankingMedidas.textoPequeno),
                color = c.textMuted,
                modifier = Modifier.padding(top = e.s1),
            )
        }
        Box(Modifier.size(RankingMedidas.botaoAjuda), contentAlignment = Alignment.Center) {
            DivvupIconButton(DivvupIcone.Ajuda, stringResource(R.string.ranking_ajuda), { aoAjuda(true) })
        }
    }
}

/** 10 segmentos: os do valor preenchidos, os até 50 em `text` e o resto em `textMuted`. */
@Composable
private fun Segmentos(indice: Int, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val cheios = (indice + RankingMedidas.SEGMENTOS / 2) / RankingMedidas.SEGMENTOS
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DivvupTheme.spacing.s1)) {
        repeat(RankingMedidas.SEGMENTOS) { i ->
            val cor = when {
                i >= cheios -> c.surfaceAlt
                i < RankingMedidas.SEGMENTOS / 2 -> c.text
                else -> c.textMuted
            }
            Box(Modifier.weight(1f).height(RankingMedidas.equilibrioSegmento).background(cor))
        }
    }
}

/** Linha da lista: posição, avatar, nome, tarefas e pontos; você em destaque. */
@Composable
private fun Linha(p: Posicao, souEu: Boolean, ex: Boolean, aoAbrirMembro: (String) -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val forma = RoundedCornerShape(RankingMedidas.destaqueRaio)
    val tarefas = pluralStringResource(R.plurals.ranking_tarefas, p.tarefas, p.tarefas)
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (souEu) Modifier.clip(forma).background(c.primaryBg) else Modifier)
                .clickable(role = Role.Button) { aoAbrirMembro(p.membro.uid) }
                .padding(vertical = e.s3, horizontal = if (souEu) RankingMedidas.destaquePadding else RankingMedidas.semPadding),
            horizontalArrangement = Arrangement.spacedBy(e.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.ranking_posicao, p.posicao),
                style = t.botao.copy(fontFeatureSettings = "tnum"),
                color = c.textMuted,
                modifier = Modifier.width(RankingMedidas.posicaoLargura),
            )
            DivvupAvatarDaGaleria(p.membro.avatarId, RankingMedidas.avatarLista, Modifier.alpha(if (ex) RankingMedidas.OPACIDADE_EX else 1f))
            Column(Modifier.weight(1f)) {
                Text(if (souEu) stringResource(R.string.ranking_voce) else primeiroNome(p.membro), style = t.ttl, color = c.text, maxLines = 1)
                Text(
                    when {
                        ex -> stringResource(R.string.ranking_ex_morador)
                        souEu -> stringResource(R.string.ranking_voce_aqui, tarefas)
                        else -> tarefas
                    },
                    style = t.small,
                    color = c.textMuted,
                )
            }
            Text(formatarNumero(p.pontos), style = t.pts.copy(fontSize = t.ttl.fontSize), color = c.text)
        }
        Box(Modifier.fillMaxWidth().height(e.borda).background(c.border))
    }
}

// ---- Geral ----

@Composable
private fun Geral(estado: RankingEstado, aoAbrirMembro: (String) -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(Modifier.padding(top = RankingMedidas.cabecalhoTopo, bottom = e.s3)) {
        estado.casaDesde?.let {
            val d = it.atZone(FUSO_DA_TEMPORADA)
            val mes = stringArrayResource(R.array.ranking_meses_curtos)[d.monthValue - 1]
            DivvupLabel(stringResource(R.string.ranking_desde, stringResource(R.string.ranking_mes_ano, mes, d.year)), cor = c.textMuted)
        }
        DivvupText(stringResource(R.string.ranking_todos_os_tempos), t.h1.copy(fontSize = RankingMedidas.geralTitulo))
    }
    Column(Modifier.padding(top = RankingMedidas.listaTopo)) {
        estado.geral.forEach { p -> Linha(p, p.membro.uid == estado.eu, ex = !p.membro.ativo, aoAbrirMembro) }
    }
}

// ---- Histórico ----

@Composable
private fun Historico(estado: RankingEstado, aoFiltrar: (String?) -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Row(
        Modifier.padding(top = e.s4).offset(x = -e.s4).horizontalScroll(rememberScrollState()).padding(horizontal = e.s4),
        horizontalArrangement = Arrangement.spacedBy(e.s2),
    ) {
        Filtro(stringResource(R.string.ranking_todos), null, estado.filtro == null) { aoFiltrar(null) }
        estado.filtros.forEach { m ->
            val nome = if (m.uid == estado.eu) stringResource(R.string.ranking_voce) else primeiroNome(m)
            Filtro(nome, m.avatarId, estado.filtro == m.uid) { aoFiltrar(m.uid) }
        }
    }
    val eventos = estado.historico.filter { estado.filtro == null || it.autorId == estado.filtro }
    if (eventos.isEmpty()) {
        val nome = estado.filtros.firstOrNull { it.uid == estado.filtro }?.let { if (it.uid == estado.eu) stringResource(R.string.ranking_voce) else primeiroNome(it) }
        Text(
            if (nome != null) stringResource(R.string.ranking_nada_de, nome) else stringResource(R.string.ranking_historico_vazio),
            style = t.body,
            color = c.textMuted,
            modifier = Modifier.padding(vertical = RankingMedidas.vazioPadding),
        )
        return
    }
    eventos.groupBy { prazoFalado(it.quando, estado.agora).dia }.forEach { (dia, doDia) ->
        DivvupLabel(
            when (dia) {
                DiaDoPrazo.Hoje -> stringResource(R.string.ranking_hoje)
                DiaDoPrazo.Ontem -> stringResource(R.string.ranking_ontem)
                else -> textoDoDia(dia)
            },
            Modifier.padding(top = RankingMedidas.listaTopo, bottom = e.s1),
            cor = c.textMuted,
        )
        doDia.forEach { ev -> LinhaDoEvento(ev, estado.eu, estado.agora) }
    }
}

@Composable
private fun Filtro(nome: String, avatar: String?, selecionado: Boolean, aoClicar: () -> Unit) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    val forma = DivvupTheme.shapes.pill
    val conteudo = if (selecionado) c.background else c.text
    Row(
        Modifier
            .heightIn(min = RankingMedidas.chipAltura)
            .clip(forma)
            .background(if (selecionado) c.text else c.surface)
            .border(e.borda, if (selecionado) c.text else c.border, forma)
            .selectable(selected = selecionado, role = Role.Checkbox, onClick = aoClicar)
            .padding(horizontal = e.s4),
        horizontalArrangement = Arrangement.spacedBy(e.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (avatar != null) {
            DivvupAvatarDaGaleria(avatar, RankingMedidas.chipAvatar, Modifier.offset(x = RankingMedidas.chipAvatarFora))
        }
        Text(nome, style = DivvupTheme.typography.botao, color = conteudo, maxLines = 1)
    }
}

@Composable
private fun LinhaDoEvento(ev: Evento, eu: String?, agora: Instant) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val quem = if (ev.autorId == eu) stringResource(R.string.ranking_voce) else ev.autorNome.substringBefore(' ')
    val verbo = stringResource(
        when (ev.tipo) {
            TipoDeEvento.COMPLETED -> R.string.ranking_concluiu
            TipoDeEvento.ASSIGNED -> R.string.ranking_assumiu
            TipoDeEvento.AUTO_ASSIGNED -> R.string.ranking_recebeu
            TipoDeEvento.JOINED -> R.string.ranking_entrou
            TipoDeEvento.LEFT -> R.string.ranking_saiu
        },
    )
    val frase = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(quem) }
        append(" ")
        append(verbo)
        ev.tarefaTitulo?.takeIf { ev.tipo != TipoDeEvento.JOINED && ev.tipo != TipoDeEvento.LEFT }?.let {
            append(" ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(it) }
        }
    }
    val quando = quandoFoi(ev.quando, agora)
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = e.s3), horizontalArrangement = Arrangement.spacedBy(e.s3)) {
            DivvupAvatarDaGaleria(ev.autorAvatar, RankingMedidas.avatarEvento)
            Column(Modifier.weight(1f)) {
                Text(frase, style = t.body.copy(fontSize = RankingMedidas.eventoTexto), color = c.text)
                Row(horizontalArrangement = Arrangement.spacedBy(e.s1), verticalAlignment = Alignment.CenterVertically) {
                    val icone = when (ev.tipo) {
                        TipoDeEvento.JOINED, TipoDeEvento.LEFT -> DivvupIcone.Casa
                        else -> ev.categoria?.icone ?: DivvupIcone.Casa
                    }
                    DivvupIcon(icone, tamanho = TamanhoIcone.MuitoPequeno.dp, tint = c.textMuted)
                    Text(
                        if (ev.atrasada) stringResource(R.string.ranking_atrasada, quando) else quando,
                        style = t.small.copy(fontSize = RankingMedidas.textoPequeno),
                        color = c.textMuted,
                    )
                }
            }
            ev.pontos?.takeIf { ev.tipo == TipoDeEvento.COMPLETED }?.let {
                DivvupPointsBadge(it, "", sinal = true, tamanho = TamanhoSelo.Pequeno)
            }
        }
        Box(Modifier.fillMaxWidth().height(e.borda).background(c.border))
    }
}

/** "há 12 min", "há 3 h", "ontem às 21h", "seg. às 9h". */
@Composable
private fun quandoFoi(quando: Instant, agora: Instant): String {
    val passou = Duration.between(quando, agora)
    val falado = prazoFalado(quando, agora)
    return when {
        falado.dia == DiaDoPrazo.Hoje && passou.toMinutes() < 1 -> stringResource(R.string.ranking_agora)
        falado.dia == DiaDoPrazo.Hoje && passou.toHours() < 1 -> stringResource(R.string.ranking_ha_min, passou.toMinutes().toInt())
        falado.dia == DiaDoPrazo.Hoje -> stringResource(R.string.ranking_ha_h, passou.toHours().toInt())
        else -> stringResource(R.string.ranking_dia_hora, textoDoDia(falado.dia), textoDaHora(falado.hora, falado.minuto))
    }
}

/** Folha "índice de equilíbrio" (botão "?"). */
@Composable
private fun Ajuda(aoFechar: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    DivvupScrim(onFechar = aoFechar) {
        DivvupSheet {
            DivvupLabel(stringResource(R.string.ranking_ajuda_rotulo), cor = c.textMuted)
            DivvupText(stringResource(R.string.ranking_ajuda_titulo), t.h2.copy(fontSize = RankingMedidas.ajudaTitulo), Modifier.padding(top = e.s1))
            Text(stringResource(R.string.ranking_ajuda_texto), style = t.body, color = c.textMuted, modifier = Modifier.padding(top = e.s3))
            Column(Modifier.padding(top = e.s4), verticalArrangement = Arrangement.spacedBy(e.s2)) {
                listOf(
                    R.string.ranking_faixa_alta to R.string.ranking_bem_dividido,
                    R.string.ranking_faixa_media to R.string.ranking_quase,
                    R.string.ranking_faixa_baixa to R.string.ranking_desequilibrado,
                ).forEach { (faixa, nome) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(e.s3)) {
                        Text(stringResource(faixa), style = t.small.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"), color = c.text, modifier = Modifier.width(RankingMedidas.faixaLargura))
                        Text(stringResource(nome), style = t.small, color = c.text)
                    }
                }
            }
            DivvupButton(stringResource(R.string.ranking_entendi), aoFechar, larguraTotal = true, variante = VarianteBotao.Primario, modifier = Modifier.padding(top = RankingMedidas.cabecalhoTopo))
        }
    }
}

internal fun primeiroNome(m: Membro): String = m.nome.substringBefore(' ')
