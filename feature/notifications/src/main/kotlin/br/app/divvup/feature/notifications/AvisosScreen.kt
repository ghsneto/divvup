package br.app.divvup.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.component.textoDaHora
import br.app.divvup.core.designsystem.component.textoDoDia
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupColors
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.tarefa.DiaDoPrazo
import br.app.divvup.core.domain.tarefa.prazoFalado
import br.app.divvup.core.model.Notificacao
import br.app.divvup.core.model.TipoDeNotificacao
import java.time.Duration
import java.time.Instant

/** Estado de F1. */
data class AvisosEstado(
    val avisos: List<Notificacao> = emptyList(),
    val carregando: Boolean = true,
    val agora: Instant = Instant.EPOCH,
) {
    val naoLidas: Int get() = avisos.count { !it.lida }
}

/** F1 Notificações (docs/design/fonte/Notificacoes.dc.html, R-37). */
@Composable
fun AvisosScreen(
    estado: AvisosEstado,
    aoVoltar: () -> Unit,
    aoMarcarTodas: () -> Unit,
    aoAbrir: (Notificacao) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(modifier.fillMaxSize().background(c.background)) {
        DivvupTopBar(paddingInicio = false) {
            DivvupIconButton(DivvupIcone.ChevL, stringResource(R.string.avisos_voltar), aoVoltar)
            Spacer(Modifier.weight(1f))
            if (estado.naoLidas > 0) {
                DivvupButton(
                    stringResource(R.string.avisos_marcar_todas),
                    aoMarcarTodas,
                    variante = VarianteBotao.Texto,
                    icone = DivvupIcone.Check,
                    modifier = Modifier.heightIn(min = AvisosMedidas.botaoAltura),
                )
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = e.s4, end = e.s4, bottom = e.s5)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(e.s3)) {
                DivvupText(stringResource(R.string.avisos_titulo), t.h1.copy(fontSize = AvisosMedidas.tituloFonte))
                if (estado.naoLidas > 0) {
                    Text(
                        pluralStringResource(R.plurals.avisos_novas, estado.naoLidas, estado.naoLidas),
                        style = t.pts.copy(fontSize = AvisosMedidas.seloNovasFonte),
                        color = c.onAccent,
                        modifier = Modifier
                            .padding(bottom = e.s2)
                            .clip(DivvupTheme.shapes.pill)
                            .background(c.accent)
                            .padding(horizontal = e.s3, vertical = e.s1),
                    )
                }
            }
            Text(stringResource(R.string.avisos_30_dias), style = t.small, color = c.textMuted, modifier = Modifier.padding(top = e.s1))
            if (!estado.carregando && estado.avisos.isEmpty()) {
                Column(Modifier.padding(top = AvisosMedidas.vazioTopo)) {
                    DivvupIcon(DivvupIcone.Sino, tamanho = TamanhoIcone.Grande.dp, tint = c.textMuted)
                    DivvupText(stringResource(R.string.avisos_vazio_titulo), t.h2.copy(fontSize = AvisosMedidas.vazioTitulo), Modifier.padding(top = e.s3))
                    Text(stringResource(R.string.avisos_vazio_texto), style = t.body, color = c.textMuted, modifier = Modifier.padding(top = e.s2))
                }
            }
            estado.avisos.groupBy { grupo(it.quando, estado.agora) }.forEach { (g, doGrupo) ->
                DivvupLabel(stringResource(g.texto), Modifier.padding(top = AvisosMedidas.grupoTopo, bottom = e.s1), cor = c.textMuted)
                doGrupo.forEach { Linha(it, estado.agora) { aoAbrir(it) } }
            }
        }
    }
}

private enum class Grupo(val texto: Int) {
    Hoje(R.string.avisos_hoje),
    Ontem(R.string.avisos_ontem),
    Semana(R.string.avisos_semana),
    Antes(R.string.avisos_antes),
}

private const val SEMANA = 7L

private fun grupo(quando: Instant, agora: Instant): Grupo = when (prazoFalado(quando, agora).dia) {
    DiaDoPrazo.Hoje -> Grupo.Hoje
    DiaDoPrazo.Ontem -> Grupo.Ontem
    else -> if (Duration.between(quando, agora).toDays() < SEMANA) Grupo.Semana else Grupo.Antes
}

@Composable
private fun Linha(n: Notificacao, agora: Instant, aoAbrir: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val (icone, fundo, frente) = aparencia(n.tipo, c)
    val naoLida = stringResource(R.string.avisos_nao_lida)
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = aoAbrir).padding(vertical = e.s3),
            horizontalArrangement = Arrangement.spacedBy(e.s3),
        ) {
            Box(
                Modifier.size(AvisosMedidas.iconeCaixa).clip(RoundedCornerShape(AvisosMedidas.iconeRaio)).background(fundo),
                contentAlignment = Alignment.Center,
            ) { DivvupIcon(icone, tamanho = TamanhoIcone.Pequeno.dp, tint = frente) }
            Column(Modifier.weight(1f)) {
                Text(
                    n.titulo,
                    style = t.botao.copy(fontSize = AvisosMedidas.textoAviso, fontWeight = if (n.lida) FontWeight.Normal else FontWeight.SemiBold),
                    color = c.text,
                )
                Text(n.corpo, style = t.small, color = c.textMuted)
                Text(quandoFoi(n.quando, agora), style = t.small.copy(fontSize = AvisosMedidas.textoQuando), color = c.textMuted)
            }
            Box(
                Modifier
                    .padding(top = AvisosMedidas.pontoTopo)
                    .size(AvisosMedidas.ponto)
                    .clip(DivvupTheme.shapes.pill)
                    .background(if (n.lida) Color.Transparent else c.primary)
                    .then(if (n.lida) Modifier else Modifier.semantics { contentDescription = naoLida }),
            )
        }
        Box(Modifier.fillMaxWidth().height(e.borda).background(c.border))
    }
}

/** Ícone e cores da caixinha de cada tipo (F1). */
private fun aparencia(tipo: TipoDeNotificacao, c: DivvupColors): Triple<DivvupIcone, Color, Color> = when (tipo) {
    TipoDeNotificacao.AUTO_ASSIGNED -> Triple(DivvupIcone.Raio, c.accent, c.onAccent)
    TipoDeNotificacao.DUE_24H, TipoDeNotificacao.DUE_2H -> Triple(DivvupIcone.Relogio, c.warningBg, c.warning)
    TipoDeNotificacao.NEW_TASK -> Triple(DivvupIcone.Mais, c.surfaceAlt, c.text)
    TipoDeNotificacao.LATE -> Triple(DivvupIcone.Alerta, c.dangerBg, c.danger)
    TipoDeNotificacao.SEASON_END -> Triple(DivvupIcone.Trofeu, c.ultramarino, c.lima)
    TipoDeNotificacao.MEMBER_COMPLETED -> Triple(DivvupIcone.Check, c.successBg, c.success)
    TipoDeNotificacao.REMOVED -> Triple(DivvupIcone.Usuarios, c.primaryBg, c.primary)
}

@Composable
private fun quandoFoi(quando: Instant, agora: Instant): String {
    val passou = Duration.between(quando, agora)
    val f = prazoFalado(quando, agora)
    return when {
        f.dia == DiaDoPrazo.Hoje && passou.toMinutes() < 1 -> stringResource(R.string.avisos_agora)
        f.dia == DiaDoPrazo.Hoje && passou.toHours() < 1 -> stringResource(R.string.avisos_ha_min, passou.toMinutes().toInt())
        f.dia == DiaDoPrazo.Hoje -> stringResource(R.string.avisos_ha_h, passou.toHours().toInt())
        f.dia == DiaDoPrazo.Ontem -> stringResource(R.string.avisos_dia_hora, textoDoDia(f.dia), textoDaHora(f.hora, f.minuto))
        else -> textoDoDia(f.dia)
    }
}
