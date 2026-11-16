package br.app.divvup.feature.ranking

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import br.app.divvup.core.designsystem.component.DivvupAvatarDaGaleria
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.formatarNumero
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.model.Temporada

/** Estado de D2. */
data class TemporadasEstado(
    val temporadas: List<Temporada> = emptyList(),
    val eu: String? = null,
    /** Mês aberto (`yyyy-MM`). */
    val aberta: String? = null,
    val carregando: Boolean = true,
)

/** D2 Temporadas anteriores (docs/design/fonte/Temporadas.dc.html, R-27). */
@Composable
fun TemporadasScreen(estado: TemporadasEstado, aoVoltar: () -> Unit, aoAlternar: (Temporada) -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(modifier.fillMaxSize().background(c.background)) {
        DivvupTopBar(paddingInicio = false) {
            DivvupIconButton(DivvupIcone.ChevL, stringResource(R.string.temporadas_voltar), aoVoltar)
            DivvupLabel(stringResource(R.string.temporadas_rotulo), cor = c.textMuted)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = e.s4, end = e.s4, bottom = e.s5)) {
            DivvupText(stringResource(R.string.temporadas_titulo), t.h2, Modifier.padding(bottom = e.s2))
            Text(stringResource(R.string.temporadas_texto), style = t.small, color = c.textMuted, modifier = Modifier.padding(bottom = e.s4))
            Box(Modifier.fillMaxWidth().height(e.borda).background(c.border))
            if (!estado.carregando && estado.temporadas.isEmpty()) {
                Text(stringResource(R.string.temporadas_vazio), style = t.body, color = c.textMuted, modifier = Modifier.padding(vertical = e.s5))
            }
            estado.temporadas.forEach { temp ->
                Mes(temp, temp.id == estado.aberta, estado.eu) { aoAlternar(temp) }
            }
        }
    }
}

@Composable
private fun Mes(temp: Temporada, aberta: Boolean, eu: String?, aoAlternar: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val (ano, mes) = temp.id.split("-").let { (it.getOrNull(0)?.toIntOrNull() ?: 0) to (it.getOrNull(1)?.toIntOrNull() ?: 1) }
    val nomeDoMes = stringArrayResource(R.array.ranking_meses)[(mes - 1).coerceIn(0, MESES - 1)]
    val campeao = temp.campeao
    val descricao = stringResource(R.string.temporadas_abrir, nomeDoMes)
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = aoAlternar)
                .semantics { contentDescription = descricao }
                .padding(vertical = e.s4),
            horizontalArrangement = Arrangement.spacedBy(e.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.width(RankingMedidas.mesLargura)) {
                DivvupText(nomeDoMes, t.h2.copy(fontSize = RankingMedidas.mesFonte))
                Text(ano.toString(), style = t.small, color = c.textMuted)
            }
            DivvupAvatarDaGaleria(campeao?.avatarId, RankingMedidas.avatarCampeao, corDaBorda = c.lima)
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(e.s1), verticalAlignment = Alignment.CenterVertically) {
                    DivvupIcon(DivvupIcone.Trofeu, tamanho = TamanhoIcone.MuitoPequeno.dp, tint = c.text)
                    Text(
                        campeao?.let { if (it.uid == eu) stringResource(R.string.ranking_voce) else it.nome.substringBefore(' ') }
                            ?: stringResource(R.string.temporadas_sem_campeao),
                        style = t.botao,
                        color = c.text,
                    )
                }
                campeao?.let { Text(stringResource(R.string.ranking_pts, formatarNumero(it.pontos)), style = t.small.copy(fontFeatureSettings = "tnum"), color = c.textMuted) }
            }
            DivvupIcon(if (aberta) DivvupIcone.ChevD else DivvupIcone.ChevR, tamanho = TamanhoIcone.Pequeno.dp, tint = c.textMuted)
        }
        if (aberta) Colocacoes(temp, eu)
        Box(Modifier.fillMaxWidth().height(e.borda).background(c.border))
    }
}

/** Ranking final congelado, com barra proporcional ao campeão. */
@Composable
private fun Colocacoes(temp: Temporada, eu: String?) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val forma = DivvupTheme.shapes.md
    val maior = temp.colocacoes.maxOfOrNull { it.pontos }?.takeIf { it > 0 } ?: 1
    Column(
        Modifier.padding(bottom = e.s4).fillMaxWidth().clip(forma).background(c.surface).border(e.borda, c.border, forma).padding(e.s3),
    ) {
        temp.colocacoes.forEach { col ->
            Row(Modifier.fillMaxWidth().padding(vertical = e.s2), horizontalArrangement = Arrangement.spacedBy(e.s3), verticalAlignment = Alignment.CenterVertically) {
                val posicao = 1 + temp.colocacoes.count { it.pontos > col.pontos }
                Text(
                    stringResource(R.string.ranking_posicao, posicao),
                    style = t.small.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                    color = c.textMuted,
                    modifier = Modifier.width(RankingMedidas.posicaoColocacao),
                )
                DivvupAvatarDaGaleria(col.avatarId, RankingMedidas.avatarColocacao)
                Text(
                    if (col.uid == eu) stringResource(R.string.ranking_voce) else col.nome.substringBefore(' '),
                    style = t.small.copy(fontWeight = FontWeight.SemiBold),
                    color = c.text,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Box(Modifier.width(RankingMedidas.barraColocacaoLargura).height(RankingMedidas.barraColocacaoAltura).clip(DivvupTheme.shapes.sm).background(c.surfaceAlt)) {
                    Box(Modifier.fillMaxWidth(col.pontos.toFloat() / maior).height(RankingMedidas.barraColocacaoAltura).background(c.text))
                }
                Text(
                    formatarNumero(col.pontos),
                    style = t.small.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                    color = c.text,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(RankingMedidas.pontosColocacao),
                )
            }
        }
    }
}

private const val MESES = 12
