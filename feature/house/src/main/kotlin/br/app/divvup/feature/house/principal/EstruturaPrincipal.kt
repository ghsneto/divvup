package br.app.divvup.feature.house.principal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import br.app.divvup.core.designsystem.component.AbaNavegacao
import br.app.divvup.core.designsystem.component.DivvupAvatarDaCasa
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupSkeleton
import br.app.divvup.core.designsystem.component.DivvupTabBar
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.casa.ResumoDaTemporada
import br.app.divvup.core.model.Casa
import br.app.divvup.feature.house.CasaMedidas
import br.app.divvup.feature.house.R
import java.util.Locale

/** Abas da barra inferior (C1 Main), na ordem do desenho. */
enum class Aba(val icone: DivvupIcone, val rotulo: Int) {
    Deck(DivvupIcone.Deck, R.string.aba_deck),
    MinhasTarefas(DivvupIcone.Tarefas, R.string.aba_minhas_tarefas),
    Ranking(DivvupIcone.Ranking, R.string.aba_ranking),
    Perfil(DivvupIcone.Perfil, R.string.aba_perfil),
}

/**
 * Estrutura principal (C1 `Main`): barra superior com a casa, a temporada e o sino (F1), conteúdo
 * da aba e barra inferior com 4 abas. O conteúdo de cada aba vem das features (deck #11,
 * minhas tarefas #14, ranking #17, perfil #26).
 */
@Composable
fun EstruturaPrincipal(
    casa: Casa?,
    temporada: ResumoDaTemporada,
    naoLidas: Int,
    aba: Aba,
    aoTrocarAba: (Aba) -> Unit,
    aoAbrirNotificacoes: () -> Unit,
    modifier: Modifier = Modifier,
    conteudo: @Composable (Aba) -> Unit,
) {
    val c = DivvupTheme.colors
    Column(modifier.fillMaxSize().background(c.background)) {
        // Só o deck mostra "novembro · faltam 24 dias" (Main); as outras abas, só o nome.
        BarraDaCasa(casa, temporada.takeIf { aba == Aba.Deck }, naoLidas, aoAbrirNotificacoes)
        Box(Modifier.weight(1f).fillMaxWidth()) { conteudo(aba) }
        DivvupTabBar(
            abas = Aba.entries.map { AbaNavegacao(it.icone, stringResource(it.rotulo)) },
            selecionada = aba.ordinal,
            onSelecionar = { aoTrocarAba(Aba.entries[it]) },
        )
    }
}

@Composable
private fun BarraDaCasa(casa: Casa?, temporada: ResumoDaTemporada?, naoLidas: Int, aoAbrirNotificacoes: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    DivvupTopBar {
        DivvupAvatarDaCasa(CasaMedidas.avatarDaCasa, descricao = stringResource(R.string.principal_imagem_casa))
        Column(Modifier.weight(1f)) {
            if (casa == null) {
                DivvupSkeleton(Modifier.width(CasaMedidas.nomeLarguraSkeleton).height(CasaMedidas.nomeAlturaSkeleton))
            } else {
                // Títulos em minúsculas (desenho: "rep. cajueiro").
                Text(
                    casa.nome.lowercase(PtBr),
                    style = t.ttl,
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            temporada?.let {
                DivvupLabel(rotuloDaTemporada(it), cor = c.textMuted, maxLinhas = 1)
            }
        }
        val descricao = if (naoLidas > 0) {
            pluralStringResource(R.plurals.principal_notificacoes_nao_lidas, naoLidas, naoLidas)
        } else {
            stringResource(R.string.principal_notificacoes)
        }
        DivvupIconButton(DivvupIcone.Sino, descricao, aoAbrirNotificacoes, contador = naoLidas.takeIf { it > 0 })
    }
}

/** "novembro · faltam 24 dias". */
@Composable
private fun rotuloDaTemporada(temporada: ResumoDaTemporada): String {
    val mes = stringArrayResource(R.array.meses)[temporada.mes - 1]
    val faltam = if (temporada.diasRestantes == 0) {
        stringResource(R.string.principal_ultimo_dia)
    } else {
        pluralStringResource(R.plurals.principal_faltam_dias, temporada.diasRestantes, temporada.diasRestantes)
    }
    return stringResource(R.string.principal_temporada, mes, faltam)
}

private val PtBr = Locale.forLanguageTag("pt-BR")
