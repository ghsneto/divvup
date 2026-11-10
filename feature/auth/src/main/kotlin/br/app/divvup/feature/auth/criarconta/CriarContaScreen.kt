package br.app.divvup.feature.auth.criarconta

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupCheckbox
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupTextField
import br.app.divvup.core.designsystem.component.DivvupTextoComLinks
import br.app.divvup.core.designsystem.component.MarcaGoogle
import br.app.divvup.core.designsystem.component.MensagemErro
import br.app.divvup.core.designsystem.component.Trecho
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.theme.DivvupColors
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.conta.ErroDeConta
import br.app.divvup.core.domain.conta.ForcaSenha
import br.app.divvup.feature.auth.AuthMedidas
import br.app.divvup.feature.auth.R
import br.app.divvup.feature.auth.comum.BarraVoltar
import br.app.divvup.feature.auth.comum.mensagem

@Composable
fun CriarContaRota(
    aoVoltar: () -> Unit,
    aoEntrar: () -> Unit,
    aoGoogle: () -> Unit,
    aoTermos: () -> Unit,
    aoPrivacidade: () -> Unit,
    aoCriar: () -> Unit,
    viewModel: CriarContaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.criou) { if (estado.criou) aoCriar() }
    CriarContaScreen(
        estado = estado,
        eventos = CriarContaEventos(
            aoMudarNome = viewModel::aoMudarNome,
            aoMudarEmail = viewModel::aoMudarEmail,
            aoMudarSenha = viewModel::aoMudarSenha,
            aoMudarConfirmacao = viewModel::aoMudarConfirmacao,
            aoMudarTermos = viewModel::aoMudarTermos,
            aoCriar = viewModel::criar,
            aoVoltar = aoVoltar,
            aoEntrar = aoEntrar,
            aoGoogle = aoGoogle,
            aoTermos = aoTermos,
            aoPrivacidade = aoPrivacidade,
        ),
    )
}

/** Ações de A4. */
data class CriarContaEventos(
    val aoMudarNome: (String) -> Unit = {},
    val aoMudarEmail: (String) -> Unit = {},
    val aoMudarSenha: (String) -> Unit = {},
    val aoMudarConfirmacao: (String) -> Unit = {},
    val aoMudarTermos: (Boolean) -> Unit = {},
    val aoCriar: () -> Unit = {},
    val aoVoltar: () -> Unit = {},
    val aoEntrar: () -> Unit = {},
    val aoGoogle: () -> Unit = {},
    val aoTermos: () -> Unit = {},
    val aoPrivacidade: () -> Unit = {},
)

/** A4 Criar conta (docs/design/fonte/CriarConta.dc.html). */
@Composable
fun CriarContaScreen(estado: CriarContaEstado, eventos: CriarContaEventos, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val f = estado.formulario
    Column(modifier.fillMaxSize().background(c.background)) {
        BarraVoltar(eventos.aoVoltar)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = e.s4, end = e.s4, bottom = e.s4),
        ) {
            DivvupLabel(stringResource(R.string.criar_rotulo), cor = c.textMuted, soltoEmBloco = true)
            DivvupText(
                stringResource(R.string.criar_titulo),
                estilo = t.h1,
                modifier = Modifier.padding(bottom = AuthMedidas.abaixoTituloCriar),
            )
            DivvupTextField(
                valor = f.nome,
                onValorMudou = eventos.aoMudarNome,
                rotulo = stringResource(R.string.campo_nome),
                placeholder = stringResource(R.string.criar_nome_placeholder),
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            DivvupTextField(
                valor = f.email,
                onValorMudou = eventos.aoMudarEmail,
                rotulo = stringResource(R.string.campo_email),
                teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                erroComLinks = if (estado.emailDuplicado) {
                    listOf(
                        Trecho.Comum(stringResource(R.string.criar_email_duplicado)),
                        Trecho.Link(stringResource(R.string.criar_email_duplicado_entrar), aoClicar = eventos.aoEntrar),
                    )
                } else {
                    null
                },
                modifier = Modifier.padding(top = e.s4),
            )
            DivvupTextField(
                valor = f.senha,
                onValorMudou = eventos.aoMudarSenha,
                rotulo = stringResource(R.string.campo_senha),
                placeholder = stringResource(R.string.criar_senha_placeholder),
                transformacao = PasswordVisualTransformation(),
                teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                abaixo = { MedidorDeForca(f.forca) },
                modifier = Modifier.padding(top = e.s4),
            )
            DivvupTextField(
                valor = f.confirmacao,
                onValorMudou = eventos.aoMudarConfirmacao,
                rotulo = stringResource(R.string.campo_confirmar_senha),
                transformacao = PasswordVisualTransformation(),
                teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                erro = if (f.senhasDiferentes) stringResource(R.string.criar_senhas_diferentes) else null,
                modifier = Modifier.padding(top = e.s4),
            )
            Row(Modifier.padding(top = AuthMedidas.termosTopo), horizontalArrangement = Arrangement.spacedBy(e.s3)) {
                DivvupCheckbox(f.aceitouTermos, eventos.aoMudarTermos, Modifier.padding(top = AuthMedidas.checkboxTopo))
                DivvupTextoComLinks(
                    listOf(
                        Trecho.Comum(stringResource(R.string.criar_termos_inicio)),
                        Trecho.Link(stringResource(R.string.criar_termos_link), aoClicar = eventos.aoTermos),
                        Trecho.Comum(stringResource(R.string.criar_termos_meio)),
                        Trecho.Link(stringResource(R.string.criar_privacidade_link), aoClicar = eventos.aoPrivacidade),
                        Trecho.Comum(stringResource(R.string.criar_termos_fim)),
                    ),
                    estilo = t.small,
                )
            }
            estado.erro?.takeIf { it != ErroDeConta.EmailJaCadastrado }?.let {
                MensagemErro(stringResource(it.mensagem()))
            }
            DivvupButton(
                stringResource(R.string.criar_botao),
                eventos.aoCriar,
                larguraTotal = true,
                habilitado = estado.botaoHabilitado,
                carregando = estado.carregando,
                modifier = Modifier.padding(top = AuthMedidas.termosTopo),
            )
            Row(Modifier.padding(top = e.s2), horizontalArrangement = Arrangement.spacedBy(e.s2)) {
                DivvupButton(
                    stringResource(R.string.criar_google),
                    eventos.aoGoogle,
                    variante = VarianteBotao.Contorno,
                    inicio = { MarcaGoogle() },
                    modifier = Modifier.weight(PESO_GOOGLE),
                )
                DivvupButton(
                    stringResource(R.string.criar_ja_tenho),
                    eventos.aoEntrar,
                    variante = VarianteBotao.Texto,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Grade 1,4fr / 1fr dos botões de rodapé de A4. */
private const val PESO_GOOGLE = 1.4f

/** Medidor de força: 4 barras de 4dp e rótulo à direita (cores do desenho). */
@Composable
private fun MedidorDeForca(forca: ForcaSenha) {
    val c = DivvupTheme.colors
    val corNivel = forca.cor(c)
    val preenchidas = maxOf(forca.nivel, if (forca == ForcaSenha.Vazia) 0 else 1)
    Row(
        Modifier.padding(top = DivvupTheme.spacing.s1),
        horizontalArrangement = Arrangement.spacedBy(DivvupTheme.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(DivvupTheme.spacing.s1)) {
            repeat(QUANTIDADE_BARRAS) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(AuthMedidas.barraForca)
                        .clip(DivvupTheme.shapes.sm)
                        .background(if (i < preenchidas) corNivel else c.surfaceAlt),
                )
            }
        }
        Text(
            text = forca.rotulo()?.let { stringResource(it) }.orEmpty(),
            style = DivvupTheme.typography.botao,
            color = corNivel,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = AuthMedidas.rotuloForcaMinimo),
        )
    }
}

private const val QUANTIDADE_BARRAS = 4

private fun ForcaSenha.cor(c: DivvupColors): Color = when (this) {
    ForcaSenha.Vazia, ForcaSenha.Fraca -> c.danger
    ForcaSenha.Razoavel -> c.warning
    ForcaSenha.Boa, ForcaSenha.Forte -> c.success
}

private fun ForcaSenha.rotulo(): Int? = when (this) {
    ForcaSenha.Vazia -> null
    ForcaSenha.Fraca -> R.string.forca_fraca
    ForcaSenha.Razoavel -> R.string.forca_razoavel
    ForcaSenha.Boa -> R.string.forca_boa
    ForcaSenha.Forte -> R.string.forca_forte
}
