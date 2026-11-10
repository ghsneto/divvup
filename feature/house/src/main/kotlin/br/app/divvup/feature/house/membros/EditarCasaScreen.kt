package br.app.divvup.feature.house.membros

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupChip
import br.app.divvup.core.designsystem.component.DivvupDivider
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupTextField
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.component.MensagemErro
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.casa.DadosDaCasa
import br.app.divvup.core.model.TipoDeCasa
import br.app.divvup.feature.house.CasaMedidas
import br.app.divvup.feature.house.R

/** Estado de G4. */
data class EditarCasaEstado(
    val dados: DadosDaCasa = DadosDaCasa("", TipoDeCasa.REPUBLICA),
    /** O anúncio estava ligado: mudar para Casal/Família avisa que ele será desligado. */
    val anuncioAtivo: Boolean = false,
    val nomeVazio: Boolean = false,
    val salvando: Boolean = false,
    val erro: Boolean = false,
)

/** G4 Editar casa (docs/design/fonte/EditarCasa.dc.html). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditarCasaScreen(
    estado: EditarCasaEstado,
    aoVoltar: () -> Unit,
    aoMudar: (DadosDaCasa) -> Unit,
    aoSalvar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val d = estado.dados
    Column(modifier.fillMaxSize().background(c.background)) {
        DivvupTopBar(paddingInicio = false) {
            DivvupIconButton(DivvupIcone.ChevL, stringResource(R.string.convites_voltar), aoVoltar)
            DivvupLabel(stringResource(R.string.convites_rotulo), cor = c.textMuted)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = e.s4, end = e.s4, bottom = e.s4)) {
            DivvupText(stringResource(R.string.editar_titulo), t.h1)
            DivvupTextField(
                d.nome,
                { aoMudar(d.copy(nome = it)) },
                rotulo = stringResource(R.string.editar_nome),
                erro = if (estado.nomeVazio) stringResource(R.string.editar_nome_vazio) else null,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                estiloTexto = t.body.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(top = e.s5),
            )
            DivvupLabel(stringResource(R.string.editar_tipo), Modifier.padding(top = CasaMedidas.cartaoPadding, bottom = e.s2))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(e.s2), verticalArrangement = Arrangement.spacedBy(e.s2)) {
                listOf(
                    Triple(TipoDeCasa.REPUBLICA, DivvupIcone.Usuarios, R.string.editar_tipo_republica),
                    Triple(TipoDeCasa.MORADIA_COMPARTILHADA, DivvupIcone.Casa, R.string.editar_tipo_moradia),
                    Triple(TipoDeCasa.CASAL_FAMILIA, DivvupIcone.Perfil, R.string.editar_tipo_casal),
                ).forEach { (tipo, icone, nome) ->
                    DivvupChip(stringResource(nome), selecionado = d.tipo == tipo, icone = icone, onClick = { aoMudar(d.copy(tipo = tipo)) })
                }
            }
            if (estado.anuncioAtivo && d.tipo == TipoDeCasa.CASAL_FAMILIA) {
                Row(
                    Modifier
                        .padding(top = e.s3)
                        .fillMaxWidth()
                        .clip(DivvupTheme.shapes.sm)
                        .background(c.warningBg)
                        .padding(e.s3),
                    horizontalArrangement = Arrangement.spacedBy(e.s2),
                ) {
                    DivvupIcon(DivvupIcone.Alerta, tamanho = TamanhoIcone.Pequeno.dp, tint = c.warning)
                    Text(stringResource(R.string.editar_aviso_casal), style = t.botao, color = c.warning)
                }
            }
            DivvupLabel(stringResource(R.string.editar_onde), Modifier.padding(top = e.s5, bottom = e.s2))
            Row(horizontalArrangement = Arrangement.spacedBy(e.s2)) {
                DivvupTextField(d.cidade, { aoMudar(d.copy(cidade = it)) }, Modifier.weight(1f), rotulo = stringResource(R.string.editar_cidade), rotuloDiscreto = true)
                DivvupTextField(d.bairro, { aoMudar(d.copy(bairro = it)) }, Modifier.weight(1f), rotulo = stringResource(R.string.editar_bairro), rotuloDiscreto = true)
            }
            DivvupTextField(
                d.rua,
                { aoMudar(d.copy(rua = it)) },
                Modifier.padding(top = e.s2),
                rotulo = stringResource(R.string.editar_rua),
                rotuloDiscreto = true,
            )
            Row(Modifier.padding(top = e.s2), horizontalArrangement = Arrangement.spacedBy(e.s2)) {
                DivvupIcon(DivvupIcone.Cadeado, tamanho = TamanhoIcone.Pequeno.dp, tint = c.textMuted)
                Text(stringResource(R.string.editar_privado), style = t.small, color = c.textMuted)
            }
            if (estado.erro) MensagemErro(stringResource(R.string.convites_erro))
        }
        DivvupDivider()
        DivvupButton(
            stringResource(R.string.editar_salvar),
            aoSalvar,
            larguraTotal = true,
            carregando = estado.salvando,
            modifier = Modifier.padding(start = e.s4, end = e.s4, top = e.s3, bottom = CasaMedidas.cartaoPadding),
        )
    }
}
