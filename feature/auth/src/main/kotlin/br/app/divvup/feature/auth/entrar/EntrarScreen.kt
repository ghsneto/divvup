package br.app.divvup.feature.auth.entrar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.divvup.core.designsystem.component.DivvupAviso
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupDivider
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupTextField
import br.app.divvup.core.designsystem.component.DivvupTextoComLinks
import br.app.divvup.core.designsystem.component.MarcaGoogle
import br.app.divvup.core.designsystem.component.TomSelo
import br.app.divvup.core.designsystem.component.Trecho
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.feature.auth.AuthMedidas
import br.app.divvup.feature.auth.R
import br.app.divvup.feature.auth.comum.BarraVoltar
import br.app.divvup.feature.auth.comum.OlhoDaSenha
import br.app.divvup.feature.auth.comum.mensagem
import br.app.divvup.feature.auth.comum.transformacaoSenha

@Composable
fun EntrarRota(
    aoVoltar: () -> Unit,
    aoEsqueci: () -> Unit,
    aoCriarConta: () -> Unit,
    aoGoogle: () -> Unit,
    aoEntrar: () -> Unit,
    viewModel: EntrarViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.entrou) { if (estado.entrou) aoEntrar() }
    EntrarScreen(
        estado = estado,
        aoMudarEmail = viewModel::aoMudarEmail,
        aoMudarSenha = viewModel::aoMudarSenha,
        aoAlternarSenha = viewModel::aoAlternarSenha,
        aoEntrar = viewModel::entrar,
        aoVoltar = aoVoltar,
        aoEsqueci = aoEsqueci,
        aoCriarConta = aoCriarConta,
        aoGoogle = aoGoogle,
    )
}

/** A3 Entrar (docs/design/fonte/Entrar.dc.html). */
@Composable
fun EntrarScreen(
    estado: EntrarEstado,
    aoMudarEmail: (String) -> Unit,
    aoMudarSenha: (String) -> Unit,
    aoAlternarSenha: () -> Unit,
    aoEntrar: () -> Unit,
    aoVoltar: () -> Unit,
    aoEsqueci: () -> Unit,
    aoCriarConta: () -> Unit,
    aoGoogle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(modifier.fillMaxSize().background(c.background)) {
        BarraVoltar(aoVoltar)
        if (!estado.online) {
            DivvupAviso(
                stringResource(R.string.entrar_sem_conexao),
                TomSelo.Info,
                DivvupIcone.Offline,
                Modifier.padding(horizontal = e.s4),
            )
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = e.s4, end = e.s4, top = AuthMedidas.conteudoTopoFormulario),
        ) {
            DivvupLabel(stringResource(R.string.entrar_rotulo), cor = c.textMuted)
            DivvupText(
                stringResource(R.string.entrar_titulo),
                estilo = t.h1,
                modifier = Modifier.padding(bottom = AuthMedidas.abaixoTituloEntrar),
            )
            DivvupTextField(
                valor = estado.email,
                onValorMudou = aoMudarEmail,
                rotulo = stringResource(R.string.campo_email),
                erro = if (estado.erroNoEmail) stringResource(R.string.erro_email_invalido) else null,
                teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            Column(Modifier.padding(top = e.s4), verticalArrangement = Arrangement.spacedBy(e.s2)) {
                // Como no desenho: rótulo e link alinhados pela linha de base.
                Row {
                    DivvupLabel(stringResource(R.string.campo_senha), Modifier.weight(1f).alignByBaseline())
                    DivvupTextoComLinks(
                        listOf(Trecho.Link(stringResource(R.string.entrar_esqueci), forte = true, aoClicar = aoEsqueci)),
                        estilo = t.small,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                DivvupTextField(
                    valor = estado.senha,
                    onValorMudou = aoMudarSenha,
                    erro = estado.erro?.takeIf { !estado.erroNoEmail }?.let { stringResource(it.mensagem()) },
                    transformacao = transformacaoSenha(estado.senhaVisivel),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    acoesTeclado = KeyboardActions(onDone = { aoEntrar() }),
                    final = { OlhoDaSenha(estado.senhaVisivel, aoAlternarSenha) },
                )
            }
            DivvupButton(
                texto = stringResource(if (estado.carregando) R.string.entrar_carregando else R.string.entrar_botao),
                onClick = aoEntrar,
                larguraTotal = true,
                habilitado = estado.botaoHabilitado,
                carregando = estado.carregando,
                modifier = Modifier.padding(top = AuthMedidas.botaoTopo),
            )
            Row(
                Modifier.padding(vertical = AuthMedidas.ouVertical),
                horizontalArrangement = Arrangement.spacedBy(e.s3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DivvupDivider(Modifier.weight(1f))
                Text(stringResource(R.string.entrar_ou), style = t.small, color = c.textMuted)
                DivvupDivider(Modifier.weight(1f))
            }
            DivvupButton(
                stringResource(R.string.boasvindas_google),
                aoGoogle,
                variante = VarianteBotao.Contorno,
                larguraTotal = true,
                inicio = { MarcaGoogle() },
            )
            Spacer(Modifier.weight(1f))
            DivvupTextoComLinks(
                listOf(
                    Trecho.Comum(stringResource(R.string.entrar_sem_conta)),
                    Trecho.Link(stringResource(R.string.entrar_criar_conta), forte = true, aoClicar = aoCriarConta),
                ),
                estilo = t.small,
                modifier = Modifier.padding(bottom = AuthMedidas.rodapeFormulario),
            )
        }
    }
}
