package br.app.divvup.feature.house.convites

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupDivider
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupSnackbar
import br.app.divvup.core.designsystem.component.DivvupStatusTag
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.component.MensagemErro
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.TomSelo
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.casa.SituacaoDoConvite
import br.app.divvup.core.domain.casa.momento
import br.app.divvup.core.domain.casa.situacao
import br.app.divvup.core.domain.casa.tempoRestante
import br.app.divvup.core.model.Convite
import br.app.divvup.feature.house.CasaMedidas
import br.app.divvup.feature.house.R
import java.time.Instant

/** Estado de G2. */
data class ConvitesEstado(
    val nomeDaCasa: String = "",
    val convites: List<Convite> = emptyList(),
    val eu: String? = null,
    val agora: Instant = Instant.EPOCH,
    val gerando: Boolean = false,
    val erro: Boolean = false,
    /** Snackbar: id da string e o código. */
    val aviso: Pair<Int, String>? = null,
)

/** G2 Convites, visão de admin (docs/design/fonte/Convites.dc.html, R-06). */
@Composable
fun ConvitesScreen(
    estado: ConvitesEstado,
    aoVoltar: () -> Unit,
    aoGerar: () -> Unit,
    aoCompartilhar: (String) -> Unit,
    aoRevogar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Box(modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize()) {
            DivvupTopBar(paddingInicio = false) {
                DivvupIconButton(DivvupIcone.ChevL, stringResource(R.string.convites_voltar), aoVoltar)
                DivvupLabel(stringResource(R.string.convites_rotulo), cor = c.textMuted)
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = e.s4, end = e.s4, bottom = e.s5),
            ) {
                DivvupText(stringResource(R.string.convites_titulo), t.h1)
                Text(stringResource(R.string.convites_texto), style = t.small, color = c.textMuted, modifier = Modifier.padding(top = e.s2))
                DivvupButton(
                    stringResource(R.string.convites_gerar),
                    aoGerar,
                    icone = DivvupIcone.Mais,
                    larguraTotal = true,
                    carregando = estado.gerando,
                    modifier = Modifier.padding(top = CasaMedidas.cartaoPadding),
                )
                if (estado.erro) MensagemErro(stringResource(R.string.convites_erro))
                Column(Modifier.padding(top = CasaMedidas.cartaoPadding)) {
                    DivvupDivider()
                    estado.convites.forEach { convite ->
                        LinhaDoConvite(convite, estado, aoCompartilhar, aoRevogar)
                        DivvupDivider()
                    }
                }
            }
        }
        estado.aviso?.let { (mensagem, codigo) ->
            DivvupSnackbar(
                stringResource(mensagem, codigo),
                icone = DivvupIcone.Check,
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = e.s4, end = e.s4, bottom = e.s4),
            )
        }
    }
}

@Composable
private fun LinhaDoConvite(convite: Convite, estado: ConvitesEstado, aoCompartilhar: (String) -> Unit, aoRevogar: (String) -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val sit = situacao(convite, estado.agora)
    val (rotulo, tom) = when (sit) {
        SituacaoDoConvite.Ativo -> R.string.convites_ativo to TomSelo.Sucesso
        SituacaoDoConvite.Usado -> R.string.convites_usado to TomSelo.Primario
        SituacaoDoConvite.Expirado -> R.string.convites_expirado to TomSelo.Neutro
        SituacaoDoConvite.Revogado -> R.string.convites_revogado to TomSelo.Erro
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = e.s4),
        horizontalArrangement = Arrangement.spacedBy(e.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val riscado = sit == SituacaoDoConvite.Revogado || sit == SituacaoDoConvite.Expirado
        Text(
            convite.codigo,
            style = t.codigo.copy(textDecoration = if (riscado) TextDecoration.LineThrough else null),
            color = c.text,
            modifier = Modifier.width(CasaMedidas.codigoLargura),
        )
        Column(Modifier.weight(1f)) {
            DivvupStatusTag(stringResource(rotulo), tom, Modifier.height(CasaMedidas.seloAltura))
            Text(meta(convite, sit, estado), style = t.small, color = c.textMuted, modifier = Modifier.padding(top = e.s1))
        }
        if (sit == SituacaoDoConvite.Ativo) {
            DivvupIconButton(
                DivvupIcone.Compartilhar,
                stringResource(R.string.convites_compartilhar, convite.codigo),
                { aoCompartilhar(convite.codigo) },
                tamanhoIcone = TamanhoIcone.Pequeno.dp,
            )
            // `.btn-t` com padding 8 4 (mais estreito que o botão padrão), em `danger`.
            Box(
                Modifier
                    .heightIn(min = CasaMedidas.botaoDepois)
                    .clip(DivvupTheme.shapes.sm)
                    .clickable(role = Role.Button) { aoRevogar(convite.codigo) }
                    .padding(horizontal = CasaMedidas.revogarPaddingH, vertical = CasaMedidas.revogarPaddingV),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.convites_revogar), style = t.botao, color = c.danger)
            }
        }
    }
}

/** "por você · expira em 23 h 41 min", "usado por Davi · ontem às 18h"… */
@Composable
private fun meta(convite: Convite, sit: SituacaoDoConvite, estado: ConvitesEstado): String {
    val autor = if (convite.criadoPor == estado.eu) stringResource(R.string.convites_voce) else convite.criadoPorNome.substringBefore(' ')
    return when (sit) {
        SituacaoDoConvite.Ativo ->
            stringResource(R.string.convites_meta_ativo, autor, textoDaDuracao(tempoRestante(convite, estado.agora)))
        SituacaoDoConvite.Usado ->
            stringResource(
                R.string.convites_meta_usado,
                convite.usadoPorNome.orEmpty().substringBefore(' '),
                textoDoMomento(momento(convite.usadoEm ?: convite.criadoEm, estado.agora)),
            )
        SituacaoDoConvite.Expirado ->
            stringResource(R.string.convites_meta_expirado, autor, textoDoMomento(momento(convite.expiraEm, estado.agora)))
        SituacaoDoConvite.Revogado ->
            stringResource(R.string.convites_meta_revogado, autor, textoDoMomento(momento(convite.revogadoEm ?: convite.criadoEm, estado.agora)))
    }
}
