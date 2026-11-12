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
import br.app.divvup.core.domain.tarefa.ErroDeTarefa
import br.app.divvup.core.domain.tarefa.EstadoDasTarefas
import br.app.divvup.core.domain.tarefa.ResultadoTarefa
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
import kotlin.test.assertTrue

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

    private fun vm(tarefas: FakeRepositorioDeTarefas, abertas: Int = 0): DeckViewModel {
        val minhas = (1..abertas).map { t("m$it", "Minha $it", Categoria.OUTROS, 25, "2026-12-08T12:00:00Z", "Iane", "", "", SituacaoDaTarefa.ASSIGNED, "eu") }
        tarefas.estado.value = EstadoDasTarefas.Carregadas(doDesenho.map { it.copy(prazo = Instant.now().plusSeconds(86_400L * it.id.toLong())) } + minhas)
        return DeckViewModel(tarefas, FakeRepositorioDeConvites(), FakeRepositorioDeConta(Sessao.Conectado("eu")), FakeMonitorDeConexao())
    }

    @Test
    fun `assumir tira o cartao, avisa por 3 s e desfazer devolve (R-15, R-18)`() = runTest {
        val repo = FakeRepositorioDeTarefas()
        val v = vm(repo)
        val topo = v.estado.first { it.situacao == SituacaoDoDeck.Cartoes }.cartoes.first()
        v.assumir(topo)
        val depois = v.estado.first { it.aviso != null }
        assertEquals(AvisoDoDeck.Assumida(50), depois.aviso)
        assertEquals(listOf("2", "3"), depois.cartoes.map { it.id })
        assertEquals(1, depois.abertas)
        v.desfazer()
        assertEquals(listOf("1", "2", "3"), v.estado.first { it.aviso == null }.cartoes.map { it.id })
        assertEquals(listOf("assumir:1", "desfazer:1:ASSUMIR"), repo.swipes)
    }

    @Test
    fun `quem perde a corrida ve quem pegou (R-15)`() = runTest {
        val repo = FakeRepositorioDeTarefas().apply { resultadoSwipe = ResultadoTarefa.Falha(ErroDeTarefa.JaAssumida("Davi Lima")) }
        val v = vm(repo)
        v.assumir(v.estado.first { it.situacao == SituacaoDoDeck.Cartoes }.cartoes.first())
        val e = v.estado.first { it.aviso is AvisoDoDeck.JaPegaram }
        assertEquals(AvisoDoDeck.JaPegaram("Davi Lima"), e.aviso)
        assertEquals(false, e.podeDesfazer)
    }

    @Test
    fun `no limite nao assume e passar some com o cartao (R-16, R-20)`() = runTest {
        val repo = FakeRepositorioDeTarefas()
        val v = vm(repo, abertas = 5)
        val e = v.estado.first { it.situacao == SituacaoDoDeck.Cartoes }
        assertTrue(e.noLimite)
        v.assumir(e.cartoes.first())
        assertTrue(repo.swipes.isEmpty())
        v.passar(e.cartoes.first())
        assertEquals(AvisoDoDeck.Passada, v.estado.first { it.aviso != null }.aviso)
        assertEquals(listOf("passar:1"), repo.swipes)
    }

    @Test
    fun `passadas ainda validas vao para o fim do deck e rever chama o backend (R-17)`() = runTest {
        val repo = FakeRepositorioDeTarefas()
        val v = vm(repo)
        repo.listaDePasses.value = listOf("1", "2", "3").associateWith { Instant.now().plusSeconds(3600) }
        val e = v.estado.first { it.situacao == SituacaoDoDeck.Passadas }
        assertEquals(3, e.passadas.size)
        v.rever()
        assertEquals(listOf("rever"), repo.swipes)
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
        DeckEstado(
            situacao = situacao,
            cartoes = if (situacao == SituacaoDoDeck.Cartoes) doDesenho else emptyList(),
            offline = offline,
            pontosDoMes = 325,
            sequencia = 4,
            abertas = 3,
            agora = agora,
        )

    @Composable
    private fun Deck(e: DeckEstado) = DeckScreen(e, {}, {}, {}, {}, {}, {}, {}, {})

    @Test
    fun screenshotC1Limite() = capturarTela("C1_deck_limite") { NaEstrutura { Deck(estado(SituacaoDoDeck.Cartoes).copy(abertas = 5)) } }

    @Test
    fun screenshotC1Passadas() = capturarTela("C1_deck_passadas") {
        val passadas = doDesenho + t("4", "Fazer a feira da semana", Categoria.COMPRAS, 100, "2026-11-07T15:00:00Z", "Davi", "avataaars:divvup-03", "")
        NaEstrutura { Deck(estado(SituacaoDoDeck.Passadas).copy(passadas = passadas)) }
    }

    @Test
    fun screenshotC1Assumida() = capturarTela("C1_deck_assumida") {
        NaEstrutura { Deck(estado(SituacaoDoDeck.Cartoes).copy(cartoes = doDesenho.drop(1), aviso = AvisoDoDeck.Assumida(50), podeDesfazer = true)) }
    }

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
    fun screenshotC1() = capturarTela("C1_deck") { NaEstrutura { Deck(estado(SituacaoDoDeck.Cartoes)) } }

    @Test
    fun screenshotC1Vazio() = capturarTela("C1_deck_vazio") { NaEstrutura { Deck(estado(SituacaoDoDeck.Vazio)) } }

    @Test
    fun screenshotC1Carregando() = capturarTela("C1_deck_carregando") { NaEstrutura { Deck(estado(SituacaoDoDeck.Carregando)) } }

    @Test
    fun screenshotC1Erro() = capturarTela("C1_deck_erro") { NaEstrutura { Deck(estado(SituacaoDoDeck.Erro)) } }

    @Test
    fun screenshotC1Offline() = capturarTela("C1_deck_offline") { NaEstrutura { Deck(estado(SituacaoDoDeck.Cartoes, offline = true)) } }
}
