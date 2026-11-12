package br.app.divvup.feature.deck

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.app.divvup.core.designsystem.component.AbaNavegacao
import br.app.divvup.core.designsystem.component.DivvupAvatarDaCasa
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupTabBar
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.domain.tarefa.EstadoDasTarefas
import br.app.divvup.core.model.Categoria
import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.Papel
import br.app.divvup.core.model.SituacaoDaTarefa
import br.app.divvup.core.model.Tarefa
import br.app.divvup.core.model.Usuario
import br.app.divvup.core.testing.RegraDispatcherPrincipal
import br.app.divvup.core.testing.capturarTela
import br.app.divvup.core.testing.fakes.FakeMonitorDeConexao
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConta
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConvites
import br.app.divvup.core.testing.fakes.FakeRepositorioDeTarefas
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class DeckTest {
    @get:Rule val principal = RegraDispatcherPrincipal()

    // Quinta, 05/11/2026, 17h em Brasília.
    private val agora = Instant.parse("2026-11-05T20:00:00Z")

    private fun t(id: String, titulo: String, cat: Categoria, pts: Int, prazo: String, por: String, avatar: String, desc: String, sit: SituacaoDaTarefa = SituacaoDaTarefa.OPEN, dono: String? = null) =
        Tarefa(id, titulo, desc, cat, pts, Instant.parse(prazo), sit, dono, por.lowercase(), por, avatar)

    /** Os cartões do desenho (Main.dc.html). */
    private val doDesenho = listOf(
        t("1", "Lavar a louça do jantar", Categoria.COZINHA, 50, "2026-11-06T01:00:00Z", "Ana", "avataaars:divvup-02", "Pratos, panelas e a forma do bolo. O detergente novo tá embaixo da pia."),
        t("2", "Tirar o lixo reciclável", Categoria.LIXO, 25, "2026-11-06T10:00:00Z", "Bruno", "avataaars:divvup-06", "O caminhão da coleta seletiva passa cedo. Vidro separado, por favor."),
        t("3", "Limpar o banheiro de cima", Categoria.BANHEIRO, 100, "2026-11-07T21:00:00Z", "Caio", "avataaars:divvup-24", "Box, pia, vaso e chão. Os produtos ficam no armário do corredor."),
    )

    @Test
    fun `deck ordena as abertas e soma pontos, sequencia e minhas tarefas`() = runTest {
        val minhas = (1..3).map { t("m$it", "Minha $it", Categoria.OUTROS, 25, "2026-11-08T12:00:00Z", "Iane", "", "", SituacaoDaTarefa.ASSIGNED, "eu") }
        val tarefas = FakeRepositorioDeTarefas(EstadoDasTarefas.Carregadas(doDesenho.reversed() + minhas))
        val convites = FakeRepositorioDeConvites(membrosIniciais = listOf(Membro("eu", "Iane", null, Papel.ADMIN, true, 325, null)))
        val conta = FakeRepositorioDeConta(Sessao.Conectado("eu"), Usuario("eu", "Iane", null, null, "h1", sequencia = 4))
        val vm = DeckViewModel(tarefas, convites, conta, FakeMonitorDeConexao())
        val e = vm.estado.first { it.situacao == SituacaoDoDeck.Cartoes && it.sequencia == 4 }
        assertEquals(listOf("1", "2", "3"), e.cartoes.map { it.id })
        assertEquals(325, e.pontosDoMes)
        assertEquals(3, e.abertas)
    }

    @Test
    fun `carregando, erro e vazio`() = runTest {
        val tarefas = FakeRepositorioDeTarefas(EstadoDasTarefas.Carregando)
        val vm = DeckViewModel(tarefas, FakeRepositorioDeConvites(), FakeRepositorioDeConta(Sessao.Conectado("eu")), FakeMonitorDeConexao())
        assertEquals(SituacaoDoDeck.Carregando, vm.estado.first().situacao)
        tarefas.estado.value = EstadoDasTarefas.Falhou
        assertEquals(SituacaoDoDeck.Erro, vm.estado.first { it.situacao != SituacaoDoDeck.Carregando }.situacao)
        tarefas.estado.value = EstadoDasTarefas.Carregadas(emptyList())
        assertEquals(SituacaoDoDeck.Vazio, vm.estado.first { it.situacao == SituacaoDoDeck.Vazio }.situacao)
    }

    private fun estado(situacao: SituacaoDoDeck, offline: Boolean = false) =
        DeckEstado(situacao, if (situacao == SituacaoDoDeck.Cartoes) doDesenho else emptyList(), offline, pontosDoMes = 325, sequencia = 4, abertas = 3, agora = agora)

    /** Barras da estrutura principal em volta do deck, como no artboard `Main`. */
    @Composable
    private fun NaEstrutura(conteudo: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize()) {
            DivvupTopBar {
                DivvupAvatarDaCasa(DivvupTheme.spacing.toqueMinimo - DivvupTheme.spacing.s2)
                Column(Modifier.weight(1f)) {
                    Text("rep. cajueiro", style = DivvupTheme.typography.ttl, color = DivvupTheme.colors.text)
                    DivvupLabel("novembro · faltam 24 dias", cor = DivvupTheme.colors.textMuted, maxLinhas = 1)
                }
                DivvupIconButton(DivvupIcone.Sino, "Notificações", {}, contador = 3)
            }
            Box(Modifier.weight(1f)) { conteudo() }
            DivvupTabBar(
                listOf(
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
    fun screenshotC1() = capturarTela("C1_deck") { NaEstrutura { DeckScreen(estado(SituacaoDoDeck.Cartoes), {}, {}, {}, {}, {}) } }

    @Test
    fun screenshotC1Vazio() = capturarTela("C1_deck_vazio") { NaEstrutura { DeckScreen(estado(SituacaoDoDeck.Vazio), {}, {}, {}, {}, {}) } }

    @Test
    fun screenshotC1Carregando() = capturarTela("C1_deck_carregando") { NaEstrutura { DeckScreen(estado(SituacaoDoDeck.Carregando), {}, {}, {}, {}, {}) } }

    @Test
    fun screenshotC1Erro() = capturarTela("C1_deck_erro") { NaEstrutura { DeckScreen(estado(SituacaoDoDeck.Erro), {}, {}, {}, {}, {}) } }

    @Test
    fun screenshotC1Offline() = capturarTela("C1_deck_offline") { NaEstrutura { DeckScreen(estado(SituacaoDoDeck.Cartoes, offline = true), {}, {}, {}, {}, {}) } }
}
