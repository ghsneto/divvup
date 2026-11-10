package br.app.divvup.feature.onboarding.casa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.em
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupDivider
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupStatusTag
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupTextField
import br.app.divvup.core.designsystem.component.DivvupToggle
import br.app.divvup.core.designsystem.component.MensagemErro
import br.app.divvup.core.designsystem.component.TomSelo
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.model.TipoDeCasa
import br.app.divvup.feature.onboarding.OnboardingMedidas
import br.app.divvup.feature.onboarding.R
import br.app.divvup.feature.onboarding.comum.ProgressoOnboarding
import java.util.Locale

/** B3 Criar casa (docs/design/fonte/CriarCasa.dc.html, R-02, R-03). */
@Composable
fun CriarCasaScreen(
    estado: CriarCasaEstado,
    aoVoltar: () -> Unit,
    aoMudarNome: (String) -> Unit,
    aoEscolherTipo: (TipoDeCasa) -> Unit,
    aoAnunciar: (Boolean) -> Unit,
    aoCriar: () -> Unit,
    aoSeguir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val criada = estado.criada
    if (criada != null) {
        CasaCriada(criada, aoSeguir, modifier)
        return
    }
    Column(modifier.fillMaxSize().background(c.background)) {
        ProgressoOnboarding(passo = 3, aoVoltar = aoVoltar)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = e.s4, end = e.s4, top = OnboardingMedidas.casaConteudoTopo, bottom = e.s4),
        ) {
            DivvupText(stringResource(R.string.criar_titulo), t.h1)
            DivvupTextField(
                valor = estado.nome,
                onValorMudou = aoMudarNome,
                rotulo = stringResource(R.string.criar_nome),
                placeholder = stringResource(R.string.criar_nome_exemplo),
                erro = if (estado.nomeVazio) stringResource(R.string.criar_nome_vazio) else null,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                estiloTexto = t.body.copy(fontSize = OnboardingMedidas.campoCasaFonte, fontWeight = FontWeight.SemiBold),
                altura = OnboardingMedidas.campoCasaAltura,
                modifier = Modifier.padding(top = e.s5),
            )
            DivvupLabel(
                stringResource(R.string.criar_tipo),
                modifier = Modifier.padding(top = e.s5, bottom = e.s2),
            )
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(e.s2)) {
                TipoDeCasa.entries.forEach { tipo ->
                    OpcaoDeTipo(tipo, selecionado = tipo == estado.tipo, aoEscolher = { aoEscolherTipo(tipo) })
                }
            }
            if (estado.tipo == TipoDeCasa.REPUBLICA) {
                Anunciar(estado.anunciar, aoAnunciar, Modifier.padding(top = e.s4))
            }
            if (estado.erroDeConexao) MensagemErro(stringResource(R.string.criar_erro_conexao))
        }
        DivvupDivider()
        DivvupButton(
            stringResource(R.string.criar_acao),
            aoCriar,
            larguraTotal = true,
            carregando = estado.criando,
            modifier = Modifier.padding(
                start = e.s4,
                end = e.s4,
                top = OnboardingMedidas.rodapeBotaoTopo,
                bottom = OnboardingMedidas.rodapeBotaoFundo,
            ),
        )
    }
}

/** Opção de tipo: ícone, nome, descrição e o "radio" redondo à direita. */
@Composable
private fun OpcaoDeTipo(tipo: TipoDeCasa, selecionado: Boolean, aoEscolher: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val forma = DivvupTheme.shapes.md
    val corDaBorda = if (selecionado) c.primary else c.border
    val (icone, nome, texto) = when (tipo) {
        TipoDeCasa.REPUBLICA -> Triple(DivvupIcone.Usuarios, R.string.criar_tipo_republica, R.string.criar_tipo_republica_texto)
        TipoDeCasa.MORADIA_COMPARTILHADA -> Triple(DivvupIcone.Casa, R.string.criar_tipo_moradia, R.string.criar_tipo_moradia_texto)
        TipoDeCasa.CASAL_FAMILIA -> Triple(DivvupIcone.Perfil, R.string.criar_tipo_casal, R.string.criar_tipo_casal_texto)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = OnboardingMedidas.tipoAltura)
            .clip(forma)
            .background(if (selecionado) c.primaryBg else c.surface)
            .border(e.borda, corDaBorda, forma)
            .selectable(selected = selecionado, role = Role.RadioButton, onClick = aoEscolher)
            .padding(e.s4),
        horizontalArrangement = Arrangement.spacedBy(e.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DivvupIcon(icone, tint = if (selecionado) c.primary else c.text)
        Column(Modifier.weight(1f)) {
            // Dentro de <button> a altura de linha é "normal" (1,2em), não a do corpo.
            Text(stringResource(nome), style = t.ttl.copy(lineHeight = ALTURA_NORMAL.em), color = c.text)
            Text(stringResource(texto), style = t.small, color = c.textMuted)
        }
        Box(
            Modifier
                .size(OnboardingMedidas.radio)
                .border(e.borda, corDaBorda, DivvupTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(OnboardingMedidas.radioPonto)
                    .clip(DivvupTheme.shapes.pill)
                    .background(if (selecionado) c.primary else Color.Transparent),
            )
        }
    }
}

/** Bloco "Anunciar vagas" (v1.1, R-38): só para república, marcado por padrão. */
@Composable
private fun Anunciar(ligado: Boolean, aoMudar: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Row(
        modifier
            .fillMaxWidth()
            .clip(DivvupTheme.shapes.md)
            .background(c.surfaceAlt)
            .padding(e.s4),
        horizontalArrangement = Arrangement.spacedBy(e.s3),
        verticalAlignment = Alignment.Top,
    ) {
        DivvupToggle(ligado, aoMudar, descricao = stringResource(R.string.criar_anunciar))
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(e.s2), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.criar_anunciar), style = t.botao, color = c.text)
                DivvupStatusTag(
                    stringResource(R.string.casa_versao_11),
                    TomSelo.Primario,
                    modifier = Modifier.height(OnboardingMedidas.seloPequenoAltura),
                )
            }
            Text(
                stringResource(R.string.criar_anunciar_texto),
                style = t.small,
                color = c.textMuted,
                modifier = Modifier.padding(top = e.s1),
            )
        }
    }
}

/** Confirmação depois de criar: "pronto · {nome} existe" e "Bora aprender o swipe" (→ B6). */
@Composable
private fun CasaCriada(nome: String, aoSeguir: () -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    Column(
        modifier.fillMaxSize().background(c.background).padding(horizontal = DivvupTheme.spacing.s5),
        verticalArrangement = Arrangement.Center,
    ) {
        DivvupLabel(stringResource(R.string.criar_pronto), cor = c.textMuted, soltoEmBloco = true)
        DivvupText(stringResource(R.string.criar_existe, nome.lowercase(Locale.forLanguageTag("pt-BR"))), DivvupTheme.typography.h1)
        DivvupButton(
            stringResource(R.string.criar_bora),
            aoSeguir,
            iconeDepois = DivvupIcone.SetaR,
            modifier = Modifier.padding(top = OnboardingMedidas.rodapeBotaoFundo),
        )
    }
}

/** `line-height: normal` da Bricolage. */
private const val ALTURA_NORMAL = 1.2f
