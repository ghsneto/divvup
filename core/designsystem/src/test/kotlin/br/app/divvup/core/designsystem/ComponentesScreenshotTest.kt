package br.app.divvup.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import br.app.divvup.core.designsystem.component.AbaNavegacao
import br.app.divvup.core.designsystem.component.DivvupAvatar
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupCard
import br.app.divvup.core.designsystem.component.DivvupChip
import br.app.divvup.core.designsystem.component.DivvupDialog
import br.app.divvup.core.designsystem.component.DivvupDivider
import br.app.divvup.core.designsystem.component.DivvupFab
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupLogo
import br.app.divvup.core.designsystem.component.DivvupPointsBadge
import br.app.divvup.core.designsystem.component.DivvupSheet
import br.app.divvup.core.designsystem.component.DivvupSimbolo
import br.app.divvup.core.designsystem.component.DivvupSkeleton
import br.app.divvup.core.designsystem.component.DivvupSnackbar
import br.app.divvup.core.designsystem.component.DivvupStatusTag
import br.app.divvup.core.designsystem.component.DivvupTabBar
import br.app.divvup.core.designsystem.component.DivvupTextField
import br.app.divvup.core.designsystem.component.DivvupToggle
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.component.TomSelo
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.icon.IlustracaoCategoria
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.testing.capturarNosDoisTemas
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Screenshots dos componentes do design system nos dois temas (issue #2). Os textos são os do
 * desenho (docs/design/fonte). Compare com docs/design/png ao revisar.
 */
@OptIn(ExperimentalLayoutApi::class)
@RunWith(RobolectricTestRunner::class)
class ComponentesScreenshotTest {

    @Test
    fun tipografia() = capturarNosDoisTemas("tipografia") {
        val t = DivvupTheme.typography
        val cor = DivvupTheme.colors.text
        Column(Modifier.width(328.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("a casa dividida", style = t.h1, color = cor)
            Text("escolhe sua cara", style = t.h2, color = cor)
            Text("Sair da Rep. Cajueiro?", style = t.h3, color = cor)
            Text("tarefas abertas", style = t.ttl, color = cor)
            Text("Arrasta pra direita, a tarefa é sua.", style = t.body, color = cor)
            Text("esperando alguém no deck agora", style = t.small, color = DivvupTheme.colors.textMuted)
            DivvupLabel("novembro · faltam 24 dias")
            Text("1.240 pts", style = t.pts, color = cor)
        }
    }

    @Test
    fun botoes() = capturarNosDoisTemas("botoes") {
        Column(Modifier.width(328.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DivvupButton("Criar conta", {}, larguraTotal = true)
            DivvupButton("Continuar com Google", {}, variante = VarianteBotao.Contorno, larguraTotal = true)
            DivvupButton("Já tenho conta", {}, variante = VarianteBotao.Texto, larguraTotal = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DivvupButton("Passar", {}, variante = VarianteBotao.Perigo, icone = DivvupIcone.X)
                DivvupButton("Assumir", {}, variante = VarianteBotao.Aceitar, icone = DivvupIcone.Check, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DivvupButton("Ficar", {}, variante = VarianteBotao.Texto)
                DivvupButton("Sair da casa", {}, variante = VarianteBotao.PerigoCheio)
            }
            DivvupButton("Criar tarefa", {}, habilitado = false, larguraTotal = true)
            DivvupButton("Bora", {}, iconeDepois = DivvupIcone.SetaR, larguraTotal = true)
        }
    }

    @Test
    fun chipsESelos() = capturarNosDoisTemas("chips_selos") {
        Column(Modifier.width(328.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DivvupChip("Cozinha", selecionado = true, icone = DivvupIcone.Cozinha, onClick = {})
                DivvupChip("Limpeza geral", icone = DivvupIcone.Limpeza, onClick = {})
                DivvupChip("Banheiro", icone = DivvupIcone.Banheiro, onClick = {})
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DivvupStatusTag("Urgente", TomSelo.Atencao, icone = DivvupIcone.Relogio)
                DivvupStatusTag("Atrasada", TomSelo.Erro, icone = DivvupIcone.Alerta)
                DivvupStatusTag("Atribuída a você", TomSelo.Info)
                DivvupStatusTag("Concluída", TomSelo.Sucesso, icone = DivvupIcone.Check)
                DivvupStatusTag("Admin", TomSelo.Primario)
                DivvupStatusTag("Cozinha", TomSelo.Neutro, icone = DivvupIcone.Cozinha)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DivvupPointsBadge(50, "pts")
                DivvupPointsBadge(1240, "pts")
                DivvupPointsBadge(25, "pts", sinal = true)
            }
        }
    }

    @Test
    fun campos() = capturarNosDoisTemas("campos") {
        Column(Modifier.width(328.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DivvupTextField("iane@email.com", {}, rotulo = "E-mail")
            DivvupTextField(
                "••••••••••",
                {},
                rotulo = "Senha",
                erro = "Senha incorreta. Confere e tenta de novo.",
                final = { DivvupIconButton(DivvupIcone.Olho, "Mostrar senha", {}) },
            )
            DivvupTextField("", {}, rotulo = "Título", placeholder = "Ex.: Lavar a louça do jantar")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DivvupToggle(ligado = true, onMudou = {}, descricao = "Ligado")
                DivvupToggle(ligado = false, onMudou = {}, descricao = "Desligado")
            }
        }
    }

    @Test
    fun superficies() = capturarNosDoisTemas("superficies") {
        Column(Modifier.width(328.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DivvupCard(Modifier.fillMaxWidth().height(72.dp)) {
                Text(
                    "Rep. Cajueiro",
                    style = DivvupTheme.typography.ttl,
                    color = DivvupTheme.colors.text,
                    modifier = Modifier.align(Alignment.CenterStart).width(200.dp).height(24.dp),
                )
            }
            DivvupDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DivvupAvatar(null, 40.dp)
                DivvupAvatar(null, 64.dp)
                DivvupSkeleton(Modifier.width(120.dp).height(16.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                DivvupLogo("divvup")
                DivvupSimbolo(altura = 40.dp)
            }
        }
    }

    @Test
    fun navegacao() = capturarNosDoisTemas("navegacao", margem = 0) {
        Column(Modifier.width(360.dp)) {
            DivvupTopBar {
                DivvupAvatar(null, 40.dp)
                Column(Modifier.weight(1f)) {
                    Text("rep. cajueiro", style = DivvupTheme.typography.ttl, color = DivvupTheme.colors.text)
                    DivvupLabel("novembro · faltam 24 dias", cor = DivvupTheme.colors.textMuted, maxLinhas = 1)
                }
                DivvupIconButton(DivvupIcone.Sino, "Notificações, 3 não lidas", {}, contador = 3)
            }
            Box(Modifier.fillMaxWidth().height(96.dp)) {
                DivvupFab(DivvupIcone.Mais, "Nova tarefa", {}, Modifier.align(Alignment.BottomEnd).width(64.dp))
            }
            DivvupTabBar(
                abas = listOf(
                    AbaNavegacao(DivvupIcone.Deck, "Deck"),
                    AbaNavegacao(DivvupIcone.Tarefas, "Minhas tarefas"),
                    AbaNavegacao(DivvupIcone.Ranking, "Ranking"),
                    AbaNavegacao(DivvupIcone.Perfil, "Perfil"),
                ),
                selecionada = 0,
                onSelecionar = {},
            )
        }
    }

    @Test
    fun sobreposicoes() = capturarNosDoisTemas("sobreposicoes") {
        Column(Modifier.width(328.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DivvupSnackbar("Tarefa assumida", acao = "Desfazer", icone = DivvupIcone.Check)
            DivvupDialog(
                titulo = "Sair da Rep. Cajueiro?",
                texto = "Suas 3 tarefas assumidas voltam para o deck.",
                modifier = Modifier.width(360.dp),
            ) {
                DivvupButton("Ficar", {}, variante = VarianteBotao.Texto)
                DivvupButton("Sair da casa", {}, variante = VarianteBotao.PerigoCheio)
            }
            DivvupSheet {
                DivvupLabel("índice de equilíbrio", cor = DivvupTheme.colors.textMuted)
                Text("quão dividida está a casa", style = DivvupTheme.typography.h2, color = DivvupTheme.colors.text)
            }
        }
    }

    @Test
    fun icones() = capturarNosDoisTemas("icones") {
        FlowRow(Modifier.width(328.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DivvupIcone.entries.forEach { DivvupIcon(it, tint = DivvupTheme.colors.text) }
        }
    }

    @Test
    fun ilustracoes() = capturarNosDoisTemas("ilustracoes") {
        FlowRow(Modifier.width(328.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IlustracaoCategoria.entries.forEach {
                Image(painterResource(it.res), null, Modifier.size(width = 160.dp, height = 100.dp), contentScale = ContentScale.Crop)
            }
        }
    }
}
