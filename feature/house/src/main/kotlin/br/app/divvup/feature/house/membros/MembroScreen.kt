package br.app.divvup.feature.house.membros

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.res.stringResource
import br.app.divvup.core.designsystem.component.DivvupAvatarDaGaleria
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupDialog
import br.app.divvup.core.designsystem.component.DivvupDivider
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupScrim
import br.app.divvup.core.designsystem.component.DivvupSnackbar
import br.app.divvup.core.designsystem.component.DivvupStatusTag
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.component.MensagemErro
import br.app.divvup.core.designsystem.component.TomSelo
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.Papel
import br.app.divvup.feature.house.CasaMedidas
import br.app.divvup.feature.house.R
import java.time.Instant
import java.util.Locale

/** Estado de D3/G3. */
data class MembroEstado(
    val membro: Membro? = null,
    val posicao: Int = 1,
    /** Quem vê é admin e o membro não é a própria pessoa: mostra as ações (G3). */
    val visaoDeAdmin: Boolean = false,
    val agora: Instant = Instant.EPOCH,
    val confirmandoRemocao: Boolean = false,
    val ocupado: Boolean = false,
    val erroUltimoAdmin: Boolean = false,
    /** Snackbar: id da string (recebe o nome). */
    val aviso: Int? = null,
)

/**
 * D3 Morador e G3 visão de admin (docs/design/fonte/Membro.dc.html, R-03). Sequência, tarefas por
 * categoria, medalhas e tarefas abertas entram com as tarefas e a gamificação (M2/M3).
 */
@Composable
fun MembroScreen(
    estado: MembroEstado,
    aoVoltar: () -> Unit,
    aoAlternarAdmin: () -> Unit,
    aoPedirRemocao: () -> Unit,
    aoCancelarRemocao: () -> Unit,
    aoRemover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val m = estado.membro
    val primeiroNome = m?.nome?.substringBefore(' ').orEmpty()
    Box(modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize()) {
            DivvupTopBar(paddingInicio = false) {
                DivvupIconButton(DivvupIcone.ChevL, stringResource(R.string.convites_voltar), aoVoltar)
                DivvupLabel(
                    stringResource(if (estado.visaoDeAdmin) R.string.membro_rotulo_admin else R.string.membro_rotulo),
                    Modifier.weight(1f),
                    cor = c.textMuted,
                )
            }
            if (m == null) return@Column
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = e.s4, end = e.s4, bottom = e.s5)) {
                Row(horizontalArrangement = Arrangement.spacedBy(e.s4), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        val admin = m.papel == Papel.ADMIN
                        DivvupStatusTag(
                            stringResource(if (admin) R.string.minhacasa_admin else R.string.membro_papel_morador),
                            if (admin) TomSelo.Primario else TomSelo.Neutro,
                        )
                        DivvupText(primeiroNome.lowercase(PtBr), t.h1, Modifier.padding(top = e.s2))
                        Text(stringResource(R.string.membro_na_casa_desde, textoDesde(m.entrouEm, estado.agora)), style = t.small, color = c.textMuted)
                    }
                    DivvupAvatarDaGaleria(m.avatarId, CasaMedidas.avatarMembro, corDaBorda = c.lima)
                }
                Numeros(m, estado.posicao, Modifier.padding(top = e.s5))
                if (estado.visaoDeAdmin) {
                    Acoes(m, estado, aoAlternarAdmin, aoPedirRemocao, Modifier.padding(top = CasaMedidas.sairTopo))
                }
            }
        }
        if (estado.confirmandoRemocao && m != null) {
            DivvupScrim(onFechar = aoCancelarRemocao, alinhamento = Alignment.Center) {
                DivvupDialog(
                    titulo = stringResource(R.string.membro_remover_titulo, primeiroNome),
                    texto = stringResource(R.string.membro_remover_texto, primeiroNome),
                    modifier = Modifier.padding(horizontal = e.s4),
                ) {
                    DivvupButton(stringResource(R.string.membro_cancelar), aoCancelarRemocao, variante = VarianteBotao.Texto)
                    DivvupButton(stringResource(R.string.membro_remover), aoRemover, variante = VarianteBotao.PerigoCheio, carregando = estado.ocupado)
                }
            }
        }
        estado.aviso?.let {
            DivvupSnackbar(
                stringResource(it, primeiroNome),
                icone = DivvupIcone.Check,
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = e.s4, end = e.s4, bottom = e.s4),
            )
        }
    }
}

/** "este mês 480 · 1º lugar" e "total 3.240", entre linhas. */
@Composable
private fun Numeros(m: Membro, posicao: Int, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(modifier) {
        DivvupDivider()
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Column(Modifier.weight(PESO_MES).padding(vertical = e.s4)) {
                DivvupLabel(stringResource(R.string.membro_este_mes), cor = c.textMuted)
                DivvupText(pontos(m.pontosDoMes), t.h2.copy(fontSize = CasaMedidas.numeroDoMes, fontFeatureSettings = "tnum"), Modifier.padding(top = e.s1))
                Text(stringResource(R.string.membro_lugar, posicao), style = t.small, color = c.textMuted)
            }
            Box(Modifier.width(e.borda).fillMaxHeight().background(c.border))
            Column(Modifier.weight(1f).padding(start = e.s3, top = e.s4, bottom = e.s4)) {
                DivvupLabel(stringResource(R.string.membro_total), cor = c.textMuted)
                Text(pontos(m.pontosTotais), style = t.pts, color = c.text, modifier = Modifier.padding(top = e.s2))
            }
        }
        DivvupDivider()
    }
}

@Composable
private fun Acoes(m: Membro, estado: MembroEstado, aoAlternarAdmin: () -> Unit, aoPedirRemocao: () -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    Column(modifier, verticalArrangement = Arrangement.spacedBy(e.s2)) {
        Box(Modifier.fillMaxWidth().height(e.borda).background(c.text))
        DivvupLabel(stringResource(R.string.membro_acoes), Modifier.padding(top = e.s2))
        DivvupButton(
            stringResource(if (m.papel == Papel.ADMIN) R.string.membro_remover_admin else R.string.membro_tornar_admin),
            aoAlternarAdmin,
            variante = VarianteBotao.Contorno,
            icone = DivvupIcone.Perfil,
            larguraTotal = true,
            alinharInicio = true,
            carregando = estado.ocupado && !estado.confirmandoRemocao,
        )
        if (estado.erroUltimoAdmin) MensagemErro(stringResource(R.string.membro_ultimo_admin))
        DivvupButton(
            stringResource(R.string.membro_remover),
            aoPedirRemocao,
            variante = VarianteBotao.Perigo,
            icone = DivvupIcone.Sair,
            larguraTotal = true,
            alinharInicio = true,
        )
    }
}

private const val PESO_MES = 1.3f
private val PtBr = Locale.forLanguageTag("pt-BR")
