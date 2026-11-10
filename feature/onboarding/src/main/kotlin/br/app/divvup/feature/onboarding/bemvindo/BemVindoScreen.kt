package br.app.divvup.feature.onboarding.bemvindo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import br.app.divvup.core.designsystem.component.DivvupAvatarDaGaleria
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupDivider
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.icon.IlustracaoCategoria
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.model.Casa
import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.TipoDeCasa
import br.app.divvup.feature.onboarding.OnboardingMedidas
import br.app.divvup.feature.onboarding.R
import java.util.Locale

/** Dados de B5. */
data class BemVindoEstado(
    val casa: Casa? = null,
    /** Moradores ativos por ordem de entrada; o último é quem acabou de entrar. */
    val membros: List<Membro> = emptyList(),
    val eu: String? = null,
    val tarefasAbertas: Int = 0,
    /** Ilustrações das tarefas abertas (até 5). */
    val miniaturas: List<IlustracaoCategoria> = emptyList(),
    /** Último dia do mês ("até dia 30"). */
    val ultimoDia: Int = 30,
)

/** B5 Bem-vindo à casa (docs/design/fonte/BemVindo.dc.html). */
@Composable
fun BemVindoScreen(estado: BemVindoEstado, aoBora: () -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(modifier.fillMaxSize().background(c.background)) {
        Faixa(estado)
        Column(Modifier.weight(1f).padding(start = e.s4, end = e.s4, top = e.s5)) {
            Row(horizontalArrangement = Arrangement.spacedBy(e.s4), verticalAlignment = Alignment.CenterVertically) {
                DivvupText(estado.tarefasAbertas.toString(), t.h1.copy(fontFeatureSettings = "tnum"))
                Column {
                    Text(pluralStringResource(R.plurals.bemvindo_tarefas, estado.tarefasAbertas), style = t.ttl, color = c.text)
                    Text(stringResource(R.string.bemvindo_esperando), style = t.small, color = c.textMuted)
                }
            }
            Spacer(Modifier.height(OnboardingMedidas.bemVindoPadding))
            DivvupDivider()
            if (estado.miniaturas.isNotEmpty()) {
                Row(Modifier.padding(top = e.s4), horizontalArrangement = Arrangement.spacedBy(e.s2)) {
                    estado.miniaturas.take(MINIATURAS).forEach {
                        Image(
                            painterResource(it.res),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(OnboardingMedidas.miniatura)
                                .clip(DivvupTheme.shapes.md)
                                .border(e.borda, c.border, DivvupTheme.shapes.md),
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.bemvindo_temporada, estado.ultimoDia.toString()),
                style = t.small,
                color = c.textMuted,
                modifier = Modifier.padding(top = e.s3),
            )
            Spacer(Modifier.weight(1f))
            DivvupButton(
                stringResource(R.string.bemvindo_bora),
                aoBora,
                iconeDepois = DivvupIcone.SetaR,
                larguraTotal = true,
                modifier = Modifier.padding(bottom = OnboardingMedidas.rodape).heightIn(min = OnboardingMedidas.botaoBora),
            )
        }
    }
}

/** Faixa ultramarino: "você entrou na", nome da casa, tipo e local, moradores. */
@Composable
private fun Faixa(estado: BemVindoEstado) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val casa = estado.casa
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.ultramarino)
            .padding(
                start = OnboardingMedidas.bemVindoPadding,
                end = OnboardingMedidas.bemVindoPadding,
                top = OnboardingMedidas.bemVindoTopo,
                bottom = OnboardingMedidas.bemVindoFundo,
            ),
    ) {
        DivvupLabel(stringResource(R.string.bemvindo_rotulo), cor = c.lima, soltoEmBloco = true)
        DivvupText(
            casa?.nome?.lowercase(PtBr).orEmpty(),
            t.h1,
            Modifier.padding(top = e.s2),
            cor = c.onUltramarino,
        )
        if (casa != null) {
            Row(
                Modifier.padding(top = e.s2),
                horizontalArrangement = Arrangement.spacedBy(e.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DivvupIcon(DivvupIcone.Usuarios, tamanho = TamanhoIcone.Pequeno.dp, tint = c.onUltramarino)
                Text(localDaCasa(casa), style = t.small, color = c.onUltramarino)
            }
        }
        val outros = estado.membros.filter { it.uid != estado.eu }
        Row(Modifier.padding(top = e.s5), verticalAlignment = Alignment.CenterVertically) {
            // Avatares sobrepostos (margin-left −14) e, ao lado, quem já estava e "e agora você".
            Row(horizontalArrangement = Arrangement.spacedBy(OnboardingMedidas.avatarSobreposto)) {
                estado.membros.take(AVATARES).forEach { m ->
                    DivvupAvatarDaGaleria(m.avatarId, OnboardingMedidas.avatarMembro, descricao = m.nome, corDaBorda = c.ultramarino)
                }
            }
            Text(
                listOfNotNull(
                    outros.joinToString(", ") { it.nome.substringBefore(' ') }.takeIf { it.isNotEmpty() },
                    stringResource(R.string.bemvindo_e_voce),
                ).joinToString("\n"),
                style = t.small,
                color = c.onUltramarino,
                modifier = Modifier.padding(start = e.s3),
            )
        }
    }
}

/** "República · Várzea, Recife" (sem local, só o tipo). */
@Composable
private fun localDaCasa(casa: Casa): String {
    val tipo = stringResource(
        when (casa.tipo) {
            TipoDeCasa.REPUBLICA -> R.string.criar_tipo_republica
            TipoDeCasa.MORADIA_COMPARTILHADA -> R.string.criar_tipo_moradia
            TipoDeCasa.CASAL_FAMILIA -> R.string.criar_tipo_casal
        },
    )
    val local = listOfNotNull(casa.bairro, casa.cidade).filter { it.isNotBlank() }.joinToString(", ")
    return if (local.isEmpty()) tipo else stringResource(R.string.bemvindo_local, tipo, local)
}

private const val AVATARES = 5
private const val MINIATURAS = 5
private val PtBr = Locale.forLanguageTag("pt-BR")

