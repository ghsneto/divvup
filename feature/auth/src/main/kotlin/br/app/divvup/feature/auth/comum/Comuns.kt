package br.app.divvup.feature.auth.comum

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.domain.conta.ErroDeConta
import br.app.divvup.feature.auth.R

/** Barra com "Voltar" (`.topbar` com padding 8 e `.iconbtn` chev-l). */
@Composable
internal fun BarraVoltar(aoVoltar: () -> Unit) {
    DivvupTopBar(paddingInicio = false) {
        DivvupIconButton(DivvupIcone.ChevL, stringResource(R.string.auth_voltar), aoVoltar)
    }
}

/** Olho de mostrar/ocultar senha: ícone 18, opacidade 0,6 quando oculta. */
@Composable
internal fun OlhoDaSenha(visivel: Boolean, aoAlternar: () -> Unit) {
    DivvupIconButton(
        icone = DivvupIcone.Olho,
        descricao = stringResource(if (visivel) R.string.senha_ocultar else R.string.senha_mostrar),
        onClick = aoAlternar,
        tamanhoIcone = TamanhoIcone.Pequeno.dp,
        modifier = Modifier.alpha(if (visivel) 1f else OPACIDADE_OLHO_OCULTO),
    )
}

private const val OPACIDADE_OLHO_OCULTO = 0.6f

internal fun transformacaoSenha(visivel: Boolean): VisualTransformation =
    if (visivel) VisualTransformation.None else PasswordVisualTransformation()

@StringRes
internal fun ErroDeConta.mensagem(): Int = when (this) {
    ErroDeConta.SenhaIncorreta -> R.string.erro_senha_incorreta
    ErroDeConta.ContaNaoEncontrada -> R.string.erro_conta_nao_encontrada
    ErroDeConta.MuitasTentativas -> R.string.erro_muitas_tentativas
    ErroDeConta.EmailInvalido -> R.string.erro_email_invalido
    ErroDeConta.SemConexao -> R.string.erro_sem_conexao
    ErroDeConta.EmailJaCadastrado -> R.string.criar_email_duplicado
    ErroDeConta.SenhaFraca -> R.string.criar_senha_fraca
    ErroDeConta.Desconhecido -> R.string.erro_desconhecido
}
