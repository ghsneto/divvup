package br.app.divvup.feature.profile.conta

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupTextField
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.component.MensagemErro
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.feature.profile.ProfileMedidas
import br.app.divvup.feature.profile.R

/** Estado de E7. */
data class ExcluirContaEstado(
    val passo: Int = 1,
    val temSenha: Boolean = true,
    val senha: String = "",
    val palavra: String = "",
    /** Único admin: nome da casa e de quem herda o papel (R-04). */
    val casa: String? = null,
    val sucessor: String? = null,
    val excluindo: Boolean = false,
    val erroSenha: Boolean = false,
    val erro: Boolean = false,
    val feita: Boolean = false,
) {
    val podeExcluir: Boolean get() = palavra.trim() == PALAVRA && (!temSenha || senha.isNotEmpty()) && !excluindo
}

const val PALAVRA = "EXCLUIR"

/** E7 Excluir conta (docs/design/fonte/ExcluirConta.dc.html; exigência da Play Store). */
@Composable
fun ExcluirContaScreen(
    estado: ExcluirContaEstado,
    aoVoltar: () -> Unit,
    aoContinuar: () -> Unit,
    aoManter: () -> Unit,
    aoMudarSenha: (String) -> Unit,
    aoMudarPalavra: (String) -> Unit,
    aoExcluir: () -> Unit,
    aoInicio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    if (estado.feita) {
        Column(modifier.fillMaxSize().background(c.background).padding(horizontal = e.s5), verticalArrangement = Arrangement.Center) {
            DivvupLabel(stringResource(R.string.excluir_feita), cor = c.textMuted)
            DivvupText(stringResource(R.string.excluir_valeu), t.h1.copy(fontSize = ProfileMedidas.tituloGrande), Modifier.padding(top = e.s2))
            DivvupButton(stringResource(R.string.excluir_inicio), aoInicio, modifier = Modifier.padding(top = e.s5))
        }
        return
    }
    Column(modifier.fillMaxSize().background(c.background)) {
        DivvupTopBar(paddingInicio = false) {
            DivvupIconButton(DivvupIcone.ChevL, stringResource(R.string.prefs_voltar), aoVoltar)
            DivvupLabel(stringResource(R.string.excluir_passo, estado.passo), Modifier.weight(1f), cor = c.textMuted)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = e.s4)) {
            if (estado.passo == 1) Passo1(estado) else Passo2(estado, aoMudarSenha, aoMudarPalavra)
        }
        Column(Modifier.padding(start = e.s4, end = e.s4, bottom = e.s5, top = e.s3), verticalArrangement = Arrangement.spacedBy(e.s2)) {
            if (estado.passo == 1) {
                DivvupButton(stringResource(R.string.excluir_continuar), aoContinuar, variante = VarianteBotao.Perigo, larguraTotal = true)
                DivvupButton(stringResource(R.string.excluir_manter), aoManter, variante = VarianteBotao.Texto, larguraTotal = true)
            } else {
                DivvupButton(
                    stringResource(R.string.excluir_botao),
                    aoExcluir,
                    variante = VarianteBotao.PerigoCheio,
                    icone = DivvupIcone.Lixo,
                    larguraTotal = true,
                    habilitado = estado.podeExcluir,
                    carregando = estado.excluindo,
                )
            }
        }
    }
}

@Composable
private fun Passo1(estado: ExcluirContaEstado) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    DivvupText(stringResource(R.string.excluir_titulo), t.h1.copy(fontSize = ProfileMedidas.tituloGrande), cor = c.danger)
    Text(stringResource(R.string.excluir_antes), style = t.body, color = c.textMuted, modifier = Modifier.padding(top = e.s3))
    Column(Modifier.padding(top = ProfileMedidas.itensTopo)) {
        listOf(
            R.string.excluir_1_titulo to R.string.excluir_1_texto,
            R.string.excluir_2_titulo to R.string.excluir_2_texto,
            R.string.excluir_3_titulo to R.string.excluir_3_texto,
        ).forEachIndexed { i, (titulo, texto) ->
            Box(Modifier.fillMaxWidth().height(e.borda).background(c.border))
            Row(Modifier.padding(vertical = e.s3), horizontalArrangement = Arrangement.spacedBy(e.s2)) {
                Text((i + 1).toString(), style = t.h3.copy(fontSize = ProfileMedidas.numeroItem, fontFeatureSettings = "tnum"), color = c.textMuted, modifier = Modifier.width(ProfileMedidas.colunaNumero))
                val tituloTexto = stringResource(titulo)
                val corpo = stringResource(texto)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(tituloTexto) }
                        append(" ")
                        append(corpo)
                    },
                    style = t.body,
                    color = c.text,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(e.borda).background(c.border))
    }
    if (estado.casa != null && estado.sucessor != null) {
        Row(
            Modifier.padding(top = e.s4).fillMaxWidth().clip(DivvupTheme.shapes.sm).background(c.warningBg).padding(e.s3),
            horizontalArrangement = Arrangement.spacedBy(e.s2),
        ) {
            DivvupIcon(DivvupIcone.Alerta, tamanho = TamanhoIcone.Pequeno.dp, tint = c.warning)
            Text(
                stringResource(R.string.excluir_unico_admin, estado.casa, estado.sucessor),
                style = t.small.copy(fontWeight = FontWeight.SemiBold),
                color = c.warning,
            )
        }
    }
    Spacer(Modifier.height(e.s4))
}

@Composable
private fun Passo2(estado: ExcluirContaEstado, aoMudarSenha: (String) -> Unit, aoMudarPalavra: (String) -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    DivvupText(stringResource(R.string.excluir_confirma), t.h1.copy(fontSize = ProfileMedidas.tituloGrande))
    if (estado.temSenha) {
        Column(Modifier.padding(top = e.s5), verticalArrangement = Arrangement.spacedBy(e.s2)) {
            DivvupLabel(stringResource(R.string.excluir_senha))
            DivvupTextField(
                estado.senha,
                aoMudarSenha,
                transformacao = PasswordVisualTransformation(),
                teclado = KeyboardOptions(keyboardType = KeyboardType.Password),
                erro = if (estado.erroSenha) stringResource(R.string.excluir_erro_senha) else null,
            )
        }
    }
    Text(stringResource(R.string.excluir_google), style = t.small, color = c.textMuted, modifier = Modifier.padding(top = e.s2))
    Column(Modifier.padding(top = ProfileMedidas.itensTopo), verticalArrangement = Arrangement.spacedBy(e.s2)) {
        DivvupLabel(stringResource(R.string.excluir_digite))
        DivvupTextField(
            estado.palavra,
            { aoMudarPalavra(it.uppercase()) },
            placeholder = stringResource(R.string.excluir_palavra),
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            estiloTexto = t.body.copy(fontWeight = FontWeight.SemiBold, letterSpacing = ProfileMedidas.ESPACAMENTO_PALAVRA.em),
        )
    }
    if (estado.erro) MensagemErro(stringResource(R.string.excluir_erro))
}
