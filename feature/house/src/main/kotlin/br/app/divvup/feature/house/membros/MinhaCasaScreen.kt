package br.app.divvup.feature.house.membros

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import br.app.divvup.core.designsystem.component.DivvupAvatarDaGaleria
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupCard
import br.app.divvup.core.designsystem.component.DivvupDialog
import br.app.divvup.core.designsystem.component.DivvupDivider
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupScrim
import br.app.divvup.core.designsystem.component.DivvupStatusTag
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.TomSelo
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.casa.ResumoDaTemporada
import br.app.divvup.core.model.Casa
import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.Papel
import br.app.divvup.feature.house.CasaMedidas
import br.app.divvup.feature.house.R
import java.time.Instant
import java.util.Locale

/** Estado de G1. */
data class MinhaCasaEstado(
    val casa: Casa? = null,
    val membros: List<Membro> = emptyList(),
    val eu: String? = null,
    val codigosAtivos: Int = 0,
    val temporada: ResumoDaTemporada = ResumoDaTemporada(1, 0),
    val agora: Instant = Instant.EPOCH,
    val confirmandoSaida: Boolean = false,
    val saindo: Boolean = false,
) {
    val souAdmin: Boolean get() = membros.any { it.uid == eu && it.papel == Papel.ADMIN }
}

/**
 * G1 Minha casa (docs/design/fonte/MinhaCasa.dc.html, R-03, R-29). Índice de equilíbrio e
 * solicitações são da v1.1 e ficam de fora; "Anúncio" aparece sem ação até lá.
 */
@Composable
fun MinhaCasaScreen(
    estado: MinhaCasaEstado,
    aoVoltar: () -> Unit,
    aoConvidar: () -> Unit,
    aoEditar: () -> Unit,
    aoAbrirMembro: (String) -> Unit,
    aoPedirSaida: () -> Unit,
    aoCancelarSaida: () -> Unit,
    aoSair: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val casa = estado.casa
    Box(modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize()) {
            DivvupTopBar(paddingInicio = false) {
                DivvupIconButton(DivvupIcone.ChevL, stringResource(R.string.convites_voltar), aoVoltar)
                DivvupLabel(stringResource(R.string.minhacasa_voltar_rotulo), cor = c.textMuted)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = e.s4, end = e.s4, bottom = e.s5)) {
                if (casa != null) {
                    DivvupStatusTag(textoDoTipo(casa.tipo), TomSelo.Neutro)
                    DivvupText(casa.nome.lowercase(PtBr), t.h1, Modifier.padding(top = e.s2))
                    Resumo(estado, Modifier.padding(top = e.s3))
                }
                if (estado.souAdmin) {
                    AcoesDeAdmin(estado.codigosAtivos, aoConvidar, aoEditar, Modifier.padding(top = CasaMedidas.cartaoPadding))
                    Solicitacoes(Modifier.padding(top = e.s1))
                }
                DivvupLabel(stringResource(R.string.minhacasa_moradores_rotulo), Modifier.padding(top = e.s5, bottom = e.s1))
                estado.membros.forEach { m ->
                    LinhaDoMorador(m, euMesmo = m.uid == estado.eu, agora = estado.agora, aoAbrir = { aoAbrirMembro(m.uid) })
                    DivvupDivider()
                }
                DivvupButton(
                    stringResource(R.string.minhacasa_sair),
                    aoPedirSaida,
                    variante = VarianteBotao.Perigo,
                    icone = DivvupIcone.Sair,
                    larguraTotal = true,
                    modifier = Modifier.padding(top = CasaMedidas.sairTopo),
                )
            }
        }
        if (estado.confirmandoSaida && casa != null) {
            DivvupScrim(onFechar = aoCancelarSaida, alinhamento = Alignment.Center) {
                DivvupDialog(
                    titulo = stringResource(R.string.minhacasa_sair_titulo, casa.nome),
                    texto = stringResource(R.string.minhacasa_sair_texto),
                    modifier = Modifier.padding(horizontal = e.s4),
                ) {
                    DivvupButton(stringResource(R.string.minhacasa_ficar), aoCancelarSaida, variante = VarianteBotao.Texto)
                    DivvupButton(stringResource(R.string.minhacasa_sair), aoSair, variante = VarianteBotao.PerigoCheio, carregando = estado.saindo)
                }
            }
        }
    }
}

/** "5 moradores   novembro temporada" (`.small`, número em negrito). */
@Composable
private fun Resumo(estado: MinhaCasaEstado, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val n = estado.membros.size
    val mes = stringArrayResource(R.array.meses)[estado.temporada.mes - 1]
    // `display:flex` com os itens encolhidos: número em cima, rótulo embaixo.
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(DivvupTheme.spacing.s4)) {
        listOf(n.toString() to pluralStringResource(R.plurals.minhacasa_moradores, n), mes to stringResource(R.string.minhacasa_temporada))
            .forEach { (forte, fraco) ->
                Column {
                    Text(forte, style = t.small.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), color = c.text)
                    Text(fraco, style = t.small, color = c.textMuted)
                }
            }
    }
}

/** Cartão "convidar moradores" (ultramarino) e "Editar casa" / "Anúncio" ao lado. */
@Composable
private fun AcoesDeAdmin(codigosAtivos: Int, aoConvidar: () -> Unit, aoEditar: () -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Row(modifier.fillMaxWidth().height(CasaMedidas.cartaoConvidarAltura), horizontalArrangement = Arrangement.spacedBy(e.s2)) {
        Column(
            Modifier
                .weight(PESO_CONVIDAR)
                .fillMaxSize()
                .clip(DivvupTheme.shapes.md)
                .background(c.ultramarino)
                .clickable(role = Role.Button, onClick = aoConvidar)
                .padding(e.s4),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            DivvupIcon(DivvupIcone.Usuarios, tamanho = TamanhoIcone.Grande.dp, tint = c.lima)
            Column {
                Text(
                    stringResource(R.string.minhacasa_convidar),
                    style = t.ttl.copy(fontSize = CasaMedidas.cartaoConvidarTitulo, letterSpacing = (-0.03).em, lineHeight = 1.05.em),
                    color = c.onUltramarino,
                )
                Text(pluralStringResource(R.plurals.minhacasa_codigos_ativos, codigosAtivos, codigosAtivos), style = t.small, color = c.onUltramarino)
            }
        }
        Column(Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(e.s2)) {
            CartaoPequeno(DivvupIcone.Editar, stringResource(R.string.minhacasa_editar), aoEditar, Modifier.weight(1f))
            // Anúncio de vagas (H3) é da v1.1: aparece como no desenho, sem ação.
            val descricao = stringResource(R.string.minhacasa_anuncio_descricao)
            CartaoPequeno(DivvupIcone.Local, stringResource(R.string.minhacasa_anuncio), null, Modifier.weight(1f).semantics { contentDescription = descricao })
        }
    }
}

/** "Solicitações de entrada" (H4, v1.1): aparece com o selo, sem ação até lá. */
@Composable
private fun Solicitacoes(modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    val descricao = stringResource(R.string.minhacasa_solicitacoes_descricao)
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = descricao }) {
        Row(Modifier.padding(vertical = e.s4), horizontalArrangement = Arrangement.spacedBy(e.s3), verticalAlignment = Alignment.CenterVertically) {
            DivvupIcon(DivvupIcone.Chat, tint = c.text)
            Text(
                stringResource(R.string.minhacasa_solicitacoes),
                style = DivvupTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = c.text,
                modifier = Modifier.weight(1f),
            )
            DivvupStatusTag(stringResource(R.string.versao_11), TomSelo.Primario)
        }
        DivvupDivider()
    }
}

@Composable
private fun CartaoPequeno(icone: DivvupIcone, texto: String, aoClicar: (() -> Unit)?, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    DivvupCard(modifier.fillMaxWidth().then(if (aoClicar != null) Modifier.clickable(role = Role.Button, onClick = aoClicar) else Modifier)) {
        Row(
            Modifier.align(Alignment.CenterStart).padding(DivvupTheme.spacing.s3),
            horizontalArrangement = Arrangement.spacedBy(DivvupTheme.spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DivvupIcon(icone, tamanho = TamanhoIcone.Pequeno.dp, tint = c.text)
            Text(texto, style = DivvupTheme.typography.botao, color = c.text)
        }
    }
}

@Composable
private fun LinhaDoMorador(m: Membro, euMesmo: Boolean, agora: Instant, aoAbrir: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = aoAbrir).padding(vertical = e.s3),
        horizontalArrangement = Arrangement.spacedBy(e.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DivvupAvatarDaGaleria(m.avatarId, CasaMedidas.avatarDaCasa)
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(e.s2), verticalAlignment = Alignment.CenterVertically) {
                // Dentro do flex do `<a>`, a altura de linha é "normal" (1,2em).
                Text(
                    if (euMesmo) stringResource(R.string.minhacasa_voce) else m.nome.substringBefore(' '),
                    style = t.ttl.copy(lineHeight = ALTURA_NORMAL.em),
                    color = c.text,
                )
                if (m.papel == Papel.ADMIN) {
                    DivvupStatusTag(stringResource(R.string.minhacasa_admin), TomSelo.Primario, Modifier.heightIn(max = CasaMedidas.seloPequenoAltura))
                }
            }
            Text(stringResource(R.string.minhacasa_desde, textoDesde(m.entrouEm, agora)), style = t.small, color = c.textMuted)
        }
        Text(
            stringResource(R.string.minhacasa_pts, pontos(m.pontosDoMes)),
            style = t.small.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
            color = c.text,
        )
    }
}

private const val PESO_CONVIDAR = 1.4f
private const val ALTURA_NORMAL = 1.2f
private val PtBr = Locale.forLanguageTag("pt-BR")
