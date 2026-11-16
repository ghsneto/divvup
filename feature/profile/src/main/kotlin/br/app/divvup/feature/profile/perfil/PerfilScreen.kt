package br.app.divvup.feature.profile.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import br.app.divvup.core.designsystem.component.DivvupAvatarDaGaleria
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupDialog
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupScrim
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.component.formatarNumero
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.icon.criterio
import br.app.divvup.core.designsystem.icon.icone
import br.app.divvup.core.designsystem.icon.nomeLongo
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.casa.FUSO_DA_TEMPORADA
import br.app.divvup.core.model.Medalha
import br.app.divvup.core.model.Tema
import br.app.divvup.core.model.Usuario
import br.app.divvup.feature.profile.ProfileMedidas
import br.app.divvup.feature.profile.R
import java.time.LocalDate
import java.util.Locale

/** Estado de E1. */
data class PerfilEstado(
    val usuario: Usuario? = null,
    val pontosDoMes: Int = 0,
    val casa: String? = null,
    val tema: Tema = Tema.SISTEMA,
    val hoje: LocalDate = LocalDate.of(PRIMEIRO_ANO, 1, 1),
    /** Medalha bloqueada tocada: mostra o critério. */
    val dica: Medalha? = null,
    val confirmandoSaida: Boolean = false,
)

/** Ordem da grade de E1: conquistadas primeiro; a primeira conquistada ocupa duas colunas. */
private const val PRIMEIRO_ANO = 1970

private val ORDEM = listOf(Medalha.CAMPEAO, Medalha.SETE_DIAS, Medalha.PRIMEIRA_TAREFA, Medalha.SALVA_VIDAS, Medalha.CEM_TAREFAS)

/** E1 Perfil (docs/design/fonte/Perfil.dc.html, R-30, R-31). */
@Composable
fun PerfilScreen(
    estado: PerfilEstado,
    aoEditar: () -> Unit,
    aoTocarMedalha: (Medalha) -> Unit,
    aoMinhaCasa: () -> Unit,
    aoNotificacoes: () -> Unit,
    aoAparencia: () -> Unit,
    aoSobre: () -> Unit,
    aoPedirSaida: () -> Unit,
    aoCancelarSaida: () -> Unit,
    aoSair: () -> Unit,
    aoExcluir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val u = estado.usuario ?: Usuario("", "", null, null, null)
    Box(modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = e.s4, end = e.s4, bottom = e.s5)) {
            Row(Modifier.fillMaxWidth().padding(top = e.s2)) {
                Spacer(Modifier.weight(1f))
                DivvupButton(
                    stringResource(R.string.perfil_editar),
                    aoEditar,
                    variante = VarianteBotao.Contorno,
                    icone = DivvupIcone.Editar,
                    modifier = Modifier.heightIn(min = ProfileMedidas.editarAltura),
                )
            }
            Row(Modifier.padding(top = e.s1), horizontalArrangement = Arrangement.spacedBy(e.s4), verticalAlignment = Alignment.Bottom) {
                DivvupAvatarDaGaleria(u.avatarId, ProfileMedidas.avatarPerfil, corDaBorda = c.lima)
                Column(Modifier.padding(bottom = e.s2)) {
                    DivvupText(u.nome.substringBefore(' ').lowercase(PtBr), t.h1.copy(fontSize = ProfileMedidas.nomePerfil), maxLinhas = 1)
                    u.email?.let { Text(it, style = t.small, color = c.textMuted, maxLines = 1) }
                }
            }
            Numeros(u, estado.pontosDoMes, estado.hoje)
            Medalhas(u, estado.dica, aoTocarMedalha)
            Links(estado, aoMinhaCasa, aoNotificacoes, aoAparencia, aoSobre, aoPedirSaida, aoExcluir)
        }
        if (estado.confirmandoSaida) {
            DivvupScrim(onFechar = aoCancelarSaida, alinhamento = Alignment.Center) {
                DivvupDialog(
                    titulo = stringResource(R.string.perfil_sair_titulo),
                    texto = estado.casa?.let { stringResource(R.string.perfil_sair_texto, it) } ?: stringResource(R.string.perfil_sair_texto_sem_casa),
                    modifier = Modifier.padding(horizontal = e.s4),
                ) {
                    DivvupButton(stringResource(R.string.perfil_cancelar), aoCancelarSaida, variante = VarianteBotao.Texto)
                    DivvupButton(stringResource(R.string.perfil_sair), aoSair, variante = VarianteBotao.PerigoCheio)
                }
            }
        }
    }
}

/** Sequência com os últimos 14 dias, pontos do mês e totais. */
@Composable
private fun Numeros(u: Usuario, doMes: Int, hoje: LocalDate) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val forma = DivvupTheme.shapes.md
    Row(
        Modifier
            .padding(top = ProfileMedidas.cartaoTopo)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(forma)
            .background(c.surface)
            .border(e.borda, c.border, forma),
    ) {
        Column(Modifier.weight(ProfileMedidas.PESO_SEQUENCIA).fillMaxHeight().padding(e.s4)) {
            DivvupLabel(stringResource(R.string.perfil_sequencia), cor = c.textMuted)
            Row(Modifier.padding(top = e.s1), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(e.s1)) {
                DivvupText(u.sequencia.toString(), t.h1.copy(fontSize = ProfileMedidas.numeroSequencia, fontFeatureSettings = "tnum"))
                Text(pluralStringResource(R.plurals.perfil_dias, u.sequencia), style = t.small, color = c.textMuted, modifier = Modifier.padding(bottom = e.s2))
            }
            Spacer(Modifier.weight(1f))
            Quadro(u, hoje)
            Text(
                pluralStringResource(R.plurals.perfil_recorde, u.melhorSequencia, u.melhorSequencia),
                style = t.small.copy(fontSize = ProfileMedidas.textoPequeno),
                color = c.textMuted,
                modifier = Modifier.padding(top = e.s2),
            )
        }
        Box(Modifier.width(e.borda).fillMaxHeight().background(c.border))
        Column(Modifier.weight(1f)) {
            Column(Modifier.padding(horizontal = e.s4, vertical = e.s3)) {
                DivvupLabel(stringResource(R.string.perfil_mes), cor = c.textMuted)
                Text(stringResource(R.string.perfil_pts, formatarNumero(doMes)), style = t.pts, color = c.text, modifier = Modifier.padding(top = e.s1))
            }
            Box(Modifier.fillMaxWidth().height(e.borda).background(c.border))
            Column(Modifier.padding(horizontal = e.s4, vertical = e.s3)) {
                DivvupLabel(stringResource(R.string.perfil_total), cor = c.textMuted)
                Text(stringResource(R.string.perfil_pts, formatarNumero(u.pontosTotais)), style = t.pts, color = c.text, modifier = Modifier.padding(top = e.s1))
                Text(pluralStringResource(R.plurals.perfil_tarefas, u.tarefasFeitas, u.tarefasFeitas), style = t.small, color = c.textMuted)
            }
        }
    }
}

/**
 * Últimos 14 dias (7 por linha): os dias da sequência atual em `accent`. O app só conhece a
 * sequência atual (`streak`), então os dias de antes dela ficam vazios.
 */
@Composable
private fun Quadro(u: Usuario, hoje: LocalDate) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    val ultimo = u.ultimoDiaDaSequencia
    // A sequência só vale se o último dia foi hoje ou ontem.
    val vale = ultimo != null && !ultimo.isBefore(hoje.minusDays(1))
    val descricao = stringResource(R.string.perfil_ultimos_dias)
    Column(Modifier.semantics { contentDescription = descricao }, verticalArrangement = Arrangement.spacedBy(e.s1)) {
        (0 until ProfileMedidas.DIAS_NO_QUADRO / ProfileMedidas.DIAS_POR_LINHA).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(e.s1)) {
                (0 until ProfileMedidas.DIAS_POR_LINHA).forEach { coluna ->
                    val dia = hoje.minusDays((ProfileMedidas.DIAS_NO_QUADRO - 1 - (linha * ProfileMedidas.DIAS_POR_LINHA + coluna)).toLong())
                    val feito = vale && !dia.isAfter(ultimo) && dia.isAfter(ultimo.minusDays(u.sequencia.toLong()))
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(DivvupTheme.shapes.sm)
                            .background(if (feito) c.accent else Color.Transparent)
                            .border(e.borda, if (feito) Color.Transparent else c.border, DivvupTheme.shapes.sm),
                    )
                }
            }
        }
    }
}

@Composable
private fun Medalhas(u: Usuario, dica: Medalha?, aoTocar: (Medalha) -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val ordem = ORDEM.sortedBy { if (u.medalhas.containsKey(it)) 0 else 1 }
    Row(Modifier.fillMaxWidth().padding(top = ProfileMedidas.secaoTopo, bottom = e.s3)) {
        DivvupLabel(stringResource(R.string.perfil_medalhas), Modifier.weight(1f))
        DivvupLabel(stringResource(R.string.perfil_medalhas_de, u.medalhas.size, Medalha.entries.size), cor = c.textMuted)
    }
    val grande = ordem.first().takeIf { u.medalhas.containsKey(it) }
    // Grade de 3 colunas: a grande ocupa 2.
    val celulas = ordem.flatMap { if (it == grande) listOf(it, it) else listOf(it) }
    Column(verticalArrangement = Arrangement.spacedBy(e.s2)) {
        celulas.chunked(TRES).forEach { linha ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(e.s2)) {
                linha.distinct().forEach { m ->
                    val peso = linha.count { it == m }.toFloat()
                    Medalha(m, u, m == grande, Modifier.weight(peso).fillMaxHeight()) { aoTocar(m) }
                }
                repeat(TRES - linha.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    dica?.let {
        Row(
            Modifier.padding(top = e.s2).fillMaxWidth().clip(DivvupTheme.shapes.sm).background(c.surfaceAlt).padding(e.s3),
            horizontalArrangement = Arrangement.spacedBy(e.s2),
        ) {
            DivvupIcon(DivvupIcone.Cadeado, tamanho = TamanhoIcone.Pequeno.dp, tint = c.text)
            val nome = stringResource(it.nomeLongo)
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(nome) }
                    append(": ")
                    append(stringResource(it.criterio))
                },
                style = t.small,
                color = c.text,
            )
        }
    }
}

private const val TRES = 3

@Composable
private fun Medalha(m: Medalha, u: Usuario, grande: Boolean, modifier: Modifier, aoTocar: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val conquista = u.medalhas[m]
    val ganhou = conquista != null
    val forma = DivvupTheme.shapes.md
    val nome = stringResource(m.nomeLongo)
    val estadoTexto = stringResource(if (ganhou) R.string.perfil_conquistada else R.string.perfil_bloqueada)
    val descricao = stringResource(R.string.perfil_medalha_estado, nome, estadoTexto)
    val sub = when {
        conquista?.primeiraVez != null -> {
            val d = conquista.primeiraVez!!.atZone(FUSO_DA_TEMPORADA)
            if (m == Medalha.CAMPEAO) {
                stringArrayResource(R.array.perfil_meses)[d.minusMonths(1).monthValue - 1]
            } else {
                stringArrayResource(R.array.perfil_meses_curtos)[d.monthValue - 1]
            }
        }
        ganhou -> ""
        m == Medalha.CEM_TAREFAS -> stringResource(R.string.perfil_cem_progresso, u.tarefasFeitas.coerceAtMost(CEM))
        else -> stringResource(R.string.perfil_bloqueada)
    }
    val fundo = when {
        grande -> c.ultramarino
        ganhou -> c.surface
        else -> Color.Transparent
    }
    val texto = when {
        grande -> c.onUltramarino
        ganhou -> c.text
        else -> c.textMuted
    }
    val borda = if (grande) c.ultramarino else c.border
    val traco = ProfileMedidas.tracoBloqueada
    val conteudo: @Composable () -> Unit = {
        Box(
            Modifier.size(if (grande) ProfileMedidas.discoGrande else ProfileMedidas.discoPequeno).clip(DivvupTheme.shapes.pill).background(if (ganhou) c.lima else c.surfaceAlt),
            contentAlignment = Alignment.Center,
        ) { DivvupIcon(m.icone, tamanho = if (grande) TamanhoIcone.Padrao.dp else TamanhoIcone.Pequeno.dp, tint = if (ganhou) c.tinta else c.textMuted) }
        Column {
            Text(
                nome,
                style = t.ttl.copy(fontSize = if (grande) ProfileMedidas.nomeMedalhaGrande else ProfileMedidas.nomeMedalhaPequeno, lineHeight = t.ttl.fontSize),
                color = texto,
            )
            if (sub.isNotEmpty()) Text(sub, style = t.small.copy(fontSize = ProfileMedidas.textoPequeno), color = texto)
        }
    }
    val base = modifier
        .heightIn(min = ProfileMedidas.medalhaAltura)
        .clip(forma)
        .background(fundo)
        .then(
            if (ganhou) {
                Modifier.border(e.borda, borda, forma)
            } else {
                Modifier.drawBehind {
                    val raio = e.s3.toPx()
                    drawRoundRect(
                        borda,
                        cornerRadius = CornerRadius(raio, raio),
                        style = Stroke(width = e.borda.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(traco.toPx(), traco.toPx()))),
                    )
                }
            },
        )
        .clickable(role = Role.Button, onClick = aoTocar)
        .semantics(mergeDescendants = true) { contentDescription = descricao }
        .padding(e.s3)
    if (grande) {
        Row(base, horizontalArrangement = Arrangement.spacedBy(e.s3), verticalAlignment = Alignment.CenterVertically) { conteudo() }
    } else {
        Column(base, verticalArrangement = Arrangement.spacedBy(e.s3)) { conteudo() }
    }
}

private const val CEM = 100

@Composable
private fun Links(
    estado: PerfilEstado,
    aoMinhaCasa: () -> Unit,
    aoNotificacoes: () -> Unit,
    aoAparencia: () -> Unit,
    aoSobre: () -> Unit,
    aoPedirSaida: () -> Unit,
    aoExcluir: () -> Unit,
) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    Column(Modifier.padding(top = ProfileMedidas.secaoTopo)) {
        Box(Modifier.fillMaxWidth().height(e.borda).background(c.border))
        Link(DivvupIcone.Casa, stringResource(R.string.perfil_minha_casa), estado.casa, aoMinhaCasa)
        Link(DivvupIcone.Sino, stringResource(R.string.perfil_notificacoes), null, aoNotificacoes)
        val tema = stringResource(
            when (estado.tema) {
                Tema.SISTEMA -> R.string.tema_sistema
                Tema.CLARO -> R.string.tema_claro
                Tema.ESCURO -> R.string.tema_escuro
            },
        )
        Link(DivvupIcone.Sol, stringResource(R.string.perfil_aparencia), tema, aoAparencia)
        Link(DivvupIcone.Info, stringResource(R.string.perfil_sobre), null, aoSobre)
        Link(DivvupIcone.Sair, stringResource(R.string.perfil_sair), null, aoPedirSaida, seta = false)
        Link(DivvupIcone.Lixo, stringResource(R.string.perfil_excluir), null, aoExcluir, cor = c.danger)
    }
}

@Composable
private fun Link(icone: DivvupIcone, nome: String, meta: String?, aoClicar: () -> Unit, seta: Boolean = true, cor: Color = DivvupTheme.colors.text) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = ProfileMedidas.linkAltura).clickable(role = Role.Button, onClick = aoClicar),
            horizontalArrangement = Arrangement.spacedBy(e.s4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DivvupIcon(icone, tint = cor)
            Text(nome, style = t.ttl, color = cor, modifier = Modifier.weight(1f))
            meta?.let { Text(it, style = t.small, color = c.textMuted, maxLines = 1) }
            if (seta) DivvupIcon(DivvupIcone.ChevR, tamanho = TamanhoIcone.Pequeno.dp, tint = if (cor == c.danger) cor else c.textMuted)
        }
        Box(Modifier.fillMaxWidth().height(e.borda).background(c.border))
    }
}

private val PtBr = Locale.forLanguageTag("pt-BR")
