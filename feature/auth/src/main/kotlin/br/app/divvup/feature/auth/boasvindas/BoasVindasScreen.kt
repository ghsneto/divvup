package br.app.divvup.feature.auth.boasvindas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupLogo
import br.app.divvup.core.designsystem.component.DivvupSimbolo
import br.app.divvup.core.designsystem.component.DivvupTextoComLinks
import br.app.divvup.core.designsystem.component.MarcaGoogle
import br.app.divvup.core.designsystem.component.Trecho
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.feature.auth.AuthMedidas
import br.app.divvup.feature.auth.R

/** A2 Boas-vindas (docs/design/fonte/BoasVindas.dc.html). */
@Composable
fun BoasVindasScreen(
    aoCriarConta: () -> Unit,
    aoGoogle: () -> Unit,
    aoJaTenhoConta: () -> Unit,
    aoTermos: () -> Unit,
    aoPrivacidade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(modifier.fillMaxSize().background(c.background)) {
        Box(Modifier.fillMaxWidth().height(AuthMedidas.faixaBoasVindas).background(c.ultramarino)) {
            DivvupLogo(
                descricao = stringResource(R.string.auth_logo),
                altura = AuthMedidas.logoFaixa,
                corSimbolo = c.onUltramarino,
                corNome = c.onUltramarino,
                modifier = Modifier.offset(AuthMedidas.logoFaixaX, AuthMedidas.logoFaixaY),
            )
            DivvupSimbolo(
                altura = AuthMedidas.simboloGrande,
                cor = c.onUltramarino,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = AuthMedidas.simboloTopo, end = AuthMedidas.simboloDireita),
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .size(AuthMedidas.tracoLimaLargura, AuthMedidas.tracoLimaAltura)
                    .background(c.lima),
            )
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = e.s4, end = e.s4, top = AuthMedidas.conteudoTopo),
        ) {
            DivvupText(stringResource(R.string.boasvindas_titulo), estilo = t.h1)
            Text(
                stringResource(R.string.boasvindas_subtitulo),
                style = t.body,
                color = c.textMuted,
                modifier = Modifier.padding(top = e.s4).widthIn(max = AuthMedidas.subtituloMaximo),
            )
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(e.s2)) {
                DivvupButton(stringResource(R.string.boasvindas_criar_conta), aoCriarConta, larguraTotal = true)
                DivvupButton(
                    stringResource(R.string.boasvindas_google),
                    aoGoogle,
                    variante = VarianteBotao.Contorno,
                    larguraTotal = true,
                    inicio = { MarcaGoogle() },
                )
                DivvupButton(stringResource(R.string.boasvindas_ja_tenho), aoJaTenhoConta, variante = VarianteBotao.Texto, larguraTotal = true)
            }
            DivvupTextoComLinks(
                trechos = listOf(
                    Trecho.Comum(stringResource(R.string.boasvindas_termos_inicio)),
                    Trecho.Link(stringResource(R.string.boasvindas_termos_link), aoClicar = aoTermos),
                    Trecho.Comum(stringResource(R.string.boasvindas_termos_meio)),
                    Trecho.Link(stringResource(R.string.boasvindas_privacidade_link), aoClicar = aoPrivacidade),
                    Trecho.Comum(stringResource(R.string.boasvindas_termos_fim)),
                ),
                estilo = t.small.copy(fontSize = AuthMedidas.fonteRodape),
                cor = c.textMuted,
                modifier = Modifier.padding(top = e.s4, bottom = AuthMedidas.rodapeBoasVindas),
            )
        }
    }
}
