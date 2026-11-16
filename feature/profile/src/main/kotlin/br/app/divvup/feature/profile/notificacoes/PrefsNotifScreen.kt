package br.app.divvup.feature.profile.notificacoes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupSnackbar
import br.app.divvup.core.designsystem.component.DivvupStatusTag
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupToggle
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.TomSelo
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.component.textoDaHora
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.notificacao.Preferencias
import br.app.divvup.core.model.TipoDeNotificacao
import br.app.divvup.feature.profile.ProfileMedidas
import br.app.divvup.feature.profile.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Estado de E4. */
data class PrefsNotifEstado(
    val preferencias: Preferencias = Preferencias(),
    /** Permissão do sistema negada (Android 13+): banner "Abrir configurações". */
    val negadas: Boolean = false,
    val erro: Boolean = false,
)

/** Linha de E4: um ou mais tipos da R-33 sob o mesmo interruptor; `null` = v1.1. */
private class Item(val tipos: List<TipoDeNotificacao>?, val nome: Int, val quem: Int)

private val GRUPOS = listOf(
    R.string.prefs_suas_tarefas to listOf(
        Item(listOf(TipoDeNotificacao.AUTO_ASSIGNED), R.string.prefs_auto, R.string.prefs_auto_quem),
        Item(listOf(TipoDeNotificacao.DUE_24H, TipoDeNotificacao.DUE_2H), R.string.prefs_prazo, R.string.prefs_prazo_quem),
        Item(listOf(TipoDeNotificacao.LATE), R.string.prefs_atraso, R.string.prefs_atraso_quem),
    ),
    R.string.prefs_sua_casa to listOf(
        Item(listOf(TipoDeNotificacao.NEW_TASK), R.string.prefs_nova, R.string.prefs_nova_quem),
        Item(listOf(TipoDeNotificacao.MEMBER_COMPLETED), R.string.prefs_conclui, R.string.prefs_conclui_quem),
        Item(listOf(TipoDeNotificacao.REMOVED), R.string.prefs_removido, R.string.prefs_removido_quem),
        Item(null, R.string.prefs_pedido, R.string.prefs_pedido_quem),
        Item(null, R.string.prefs_resposta, R.string.prefs_resposta_quem),
    ),
    R.string.prefs_temporada to listOf(
        Item(listOf(TipoDeNotificacao.SEASON_END), R.string.prefs_fim, R.string.prefs_fim_quem),
    ),
)

/** E4 Preferências de notificação (docs/design/fonte/PrefsNotif.dc.html, R-34, R-35). */
@Composable
fun PrefsNotifScreen(
    estado: PrefsNotifEstado,
    aoVoltar: () -> Unit,
    aoMudarTipos: (List<TipoDeNotificacao>, Boolean) -> Unit,
    aoMudarSilencio: (Boolean) -> Unit,
    aoEscolherInicio: () -> Unit,
    aoEscolherFim: () -> Unit,
    aoAbrirConfiguracoes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val p = estado.preferencias
    Box(modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize()) {
            DivvupTopBar(paddingInicio = false) {
                DivvupIconButton(DivvupIcone.ChevL, stringResource(R.string.prefs_voltar), aoVoltar)
                DivvupLabel(stringResource(R.string.prefs_rotulo), cor = c.textMuted)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = e.s4, end = e.s4, bottom = e.s5)) {
                DivvupText(stringResource(R.string.prefs_titulo), t.h2)
                if (estado.negadas) Banner(aoAbrirConfiguracoes)
                GRUPOS.forEach { (grupo, itens) ->
                    DivvupLabel(stringResource(grupo), Modifier.padding(top = ProfileMedidas.secaoTopo, bottom = e.s1))
                    itens.forEach { item ->
                        val tipos = item.tipos
                        Linha(item.nome, item.quem, v11 = tipos == null, ligado = tipos?.all { p.ligado(it) } ?: true) { novo ->
                            if (tipos != null) aoMudarTipos(tipos, novo)
                        }
                    }
                }
                Silencio(p, aoMudarSilencio, aoEscolherInicio, aoEscolherFim)
            }
        }
        if (estado.erro) {
            DivvupSnackbar(
                stringResource(R.string.prefs_erro),
                icone = DivvupIcone.Alerta,
                modifier = Modifier.align(Alignment.BottomCenter).padding(e.s4),
            )
        }
    }
}

@Composable
private fun Banner(aoAbrirConfiguracoes: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Row(
        Modifier.padding(top = e.s4).fillMaxWidth().clip(DivvupTheme.shapes.md).background(c.warningBg).padding(e.s4),
        horizontalArrangement = Arrangement.spacedBy(e.s3),
    ) {
        DivvupIcon(DivvupIcone.Alerta, tint = c.warning)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.prefs_negadas), style = t.small.copy(fontWeight = FontWeight.SemiBold), color = c.text)
            Text(stringResource(R.string.prefs_negadas_texto), style = t.small, color = c.textMuted)
            DivvupButton(
                stringResource(R.string.prefs_abrir_config),
                aoAbrirConfiguracoes,
                variante = VarianteBotao.Contorno,
                modifier = Modifier.padding(top = e.s3).heightIn(min = ProfileMedidas.botaoBanner),
            )
        }
    }
}

@Composable
private fun Linha(nome: Int, quem: Int, v11: Boolean, ligado: Boolean, aoMudar: (Boolean) -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val titulo = stringResource(nome)
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = ProfileMedidas.linhaAltura).padding(vertical = e.s2),
            horizontalArrangement = Arrangement.spacedBy(e.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(e.s2), verticalAlignment = Alignment.CenterVertically) {
                    Text(titulo, style = t.botao.copy(fontSize = ProfileMedidas.textoLinha), color = c.text)
                    if (v11) DivvupStatusTag(stringResource(R.string.prefs_v11), TomSelo.Primario, Modifier.height(ProfileMedidas.seloV11))
                }
                Text(stringResource(quem), style = t.small.copy(fontSize = ProfileMedidas.textoPequeno, lineHeight = ProfileMedidas.textoPequenoLinha), color = c.textMuted)
            }
            // Itens da v1.1 aparecem como no desenho, ligados, mas ainda não fazem nada.
            DivvupToggle(ligado, { if (!v11) aoMudar(it) }, descricao = titulo)
        }
        Box(Modifier.fillMaxWidth().height(e.borda).background(c.border))
    }
}

private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Cartão "Horário silencioso" com de/até (R-35). */
@Composable
private fun Silencio(p: Preferencias, aoMudar: (Boolean) -> Unit, aoInicio: () -> Unit, aoFim: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val forma = DivvupTheme.shapes.md
    val titulo = stringResource(R.string.prefs_silencio)
    Column(
        Modifier.padding(top = ProfileMedidas.secaoTopo).fillMaxWidth().clip(forma).background(c.surface).border(e.borda, c.border, forma).padding(e.s4),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(e.s3), verticalAlignment = Alignment.CenterVertically) {
            DivvupIcon(DivvupIcone.Lua, tint = c.text)
            Text(titulo, style = t.ttl.copy(fontSize = ProfileMedidas.textoSilencio), color = c.text, modifier = Modifier.weight(1f))
            DivvupToggle(p.silencioLigado, aoMudar, descricao = titulo)
        }
        if (p.silencioLigado) {
            Row(Modifier.padding(top = e.s4), horizontalArrangement = Arrangement.spacedBy(e.s2), verticalAlignment = Alignment.CenterVertically) {
                Horario(stringResource(R.string.prefs_de), p.inicio, aoInicio, Modifier.weight(1f))
                DivvupIcon(DivvupIcone.SetaR, tamanho = TamanhoIcone.Pequeno.dp, tint = c.textMuted)
                Horario(stringResource(R.string.prefs_ate), p.fim, aoFim, Modifier.weight(1f))
            }
            Text(
                stringResource(R.string.prefs_silencio_nota, textoDaHora(p.fim.hour, p.fim.minute)),
                style = t.small,
                color = c.textMuted,
                modifier = Modifier.padding(top = e.s3),
            )
        }
    }
}

@Composable
private fun Horario(rotulo: String, hora: LocalTime, aoClicar: () -> Unit, modifier: Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val forma = DivvupTheme.shapes.sm
    Column(
        modifier
            .height(ProfileMedidas.caixaHorario)
            .clip(forma)
            .background(c.surface)
            .border(e.borda, c.border, forma)
            .clickable(role = Role.Button, onClick = aoClicar)
            .padding(horizontal = e.s4),
        verticalArrangement = Arrangement.Center,
    ) {
        DivvupLabel(rotulo, cor = c.textMuted)
        Text(hora.format(HHMM), style = t.ttl.copy(fontSize = ProfileMedidas.textoHorario, fontFeatureSettings = "tnum"), color = c.text)
    }
}
