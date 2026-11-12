package br.app.divvup.feature.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.SavedStateHandle
import br.app.divvup.core.designsystem.component.AbaNavegacao
import br.app.divvup.core.designsystem.component.DivvupAvatarDaCasa
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupTabBar
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.domain.tarefa.ErroDeTarefa
import br.app.divvup.core.domain.tarefa.ResultadoDaConclusao
import br.app.divvup.core.domain.tarefa.ResultadoTarefa
import br.app.divvup.core.domain.tarefa.SituacaoParaMim
import br.app.divvup.core.domain.tarefa.secoesDasMinhas
import br.app.divvup.core.model.Categoria
import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.Papel
import br.app.divvup.core.model.Peso
import br.app.divvup.core.model.SituacaoDaTarefa
import br.app.divvup.core.model.Tarefa
import br.app.divvup.core.model.ViaDeAtribuicao
import br.app.divvup.core.testing.RegraDispatcherPrincipal
import br.app.divvup.core.testing.capturarTela
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConta
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConvites
import br.app.divvup.core.testing.fakes.FakeRepositorioDeTarefas
import br.app.divvup.feature.tasks.concluida.ConcluidaEstado
import br.app.divvup.feature.tasks.concluida.ConcluidaScreen
import br.app.divvup.feature.tasks.detalhe.DetalheEstado
import br.app.divvup.feature.tasks.detalhe.DetalheScreen
import br.app.divvup.feature.tasks.detalhe.DetalheViewModel
import br.app.divvup.feature.tasks.detalhe.DialogoDoDetalhe
import br.app.divvup.feature.tasks.minhas.MinhasTarefasEstado
import br.app.divvup.feature.tasks.minhas.MinhasTarefasScreen
import br.app.divvup.feature.tasks.minhas.MinhasTarefasViewModel
import br.app.divvup.feature.tasks.navegacao.RotaConcluida
import br.app.divvup.feature.tasks.nova.NovaTarefaScreen
import br.app.divvup.feature.tasks.nova.NovaTarefaViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MinhasEDetalheTest {
    @get:Rule val principal = RegraDispatcherPrincipal()

    // Quinta, 06/11/2026, 17h em Brasília.
    private val agora = Instant.parse("2026-11-06T20:00:00Z")

    private fun t(
        id: String,
        titulo: String,
        cat: Categoria,
        pts: Int,
        prazo: String,
        sit: SituacaoDaTarefa = SituacaoDaTarefa.ASSIGNED,
        dono: String? = "eu",
        desc: String? = null,
        via: ViaDeAtribuicao? = ViaDeAtribuicao.SWIPE,
        concluidaEm: String? = null,
        ganhos: Int? = null,
    ) = Tarefa(
        id, titulo, desc, cat, pts, Instant.parse(prazo), sit, dono, "caio", "Caio", "avataaars:divvup-24",
        criadaEm = Instant.parse("2026-11-05T23:00:00Z"),
        via = via,
        concluidaEm = concluidaEm?.let(Instant::parse),
        pontosGanhos = ganhos,
    )

    /** As linhas do desenho (MinhasTarefas.dc.html). */
    private val doDesenho = listOf(
        t("1", "Trocar a lâmpada da cozinha", Categoria.MANUTENCAO, 50, "2026-11-05T23:00:00Z", SituacaoDaTarefa.LATE),
        t("2", "Tirar o lixo reciclável", Categoria.LIXO, 25, "2026-11-07T02:00:00Z", via = ViaDeAtribuicao.AUTO),
        t("3", "Limpar o banheiro de cima", Categoria.BANHEIRO, 100, "2026-11-08T21:00:00Z"),
        t("4", "Lavar a louça do jantar", Categoria.COZINHA, 50, "2026-11-06T23:00:00Z", SituacaoDaTarefa.DONE, concluidaEm = "2026-11-06T13:00:00Z", ganhos = 50),
        t("5", "Regar as plantas da varanda", Categoria.OUTROS, 25, "2026-11-05T23:00:00Z", SituacaoDaTarefa.DONE, concluidaEm = "2026-11-05T22:00:00Z", ganhos = 25),
        t("6", "Fazer a feira da semana", Categoria.COMPRAS, 100, "2026-11-04T23:00:00Z", SituacaoDaTarefa.DONE, concluidaEm = "2026-11-03T22:00:00Z", ganhos = 50),
    )

    private val eu = Membro("eu", "Iane", "avataaars:divvup-02", Papel.ADMIN, true, 325, null)
    private val bruno = Membro("bruno", "Bruno", "avataaars:divvup-06", Papel.MEMBER, true, 300, null)
    private val conclusao = ResultadoDaConclusao(50, noPrazo = true, posicaoAntes = 3, posicaoDepois = 2, ultrapassou = "Bruno", sequencia = 4)

    // ---- C5 ----

    @Test
    fun `minhas separa atrasadas, hoje, proximas e concluidas e conclui pelo backend (R-21)`() = runTest {
        val repo = FakeRepositorioDeTarefas().apply { listaMinhas.value = doDesenho }
        val vm = MinhasTarefasViewModel(repo)
        vm.relogio(agora)
        val e = vm.estado.first { it.secoes.abertas == 3 }
        assertEquals(listOf("1"), e.secoes.atrasadas.map { it.id })
        assertEquals(listOf("2"), e.secoes.hoje.map { it.id })
        assertEquals(listOf("3"), e.secoes.proximas.map { it.id })
        assertEquals(125, e.secoes.pontosConcluidas)

        vm.concluir(doDesenho[2])
        advanceUntilIdle()
        assertEquals("concluir:3", repo.swipes.single())
        val feita = assertNotNull(vm.conclusao.value)
        assertEquals("Limpar o banheiro de cima", feita.tarefa.titulo)
        assertEquals(RotaConcluida.de(feita).estado, feita.estado)
        vm.conclusaoAberta()
        assertNull(vm.conclusao.value)
    }

    @Test
    fun `falha ao concluir nao abre C4`() = runTest {
        val repo = FakeRepositorioDeTarefas().apply { resultadoConcluir = ResultadoTarefa.Falha(ErroDeTarefa.SemConexao) }
        val vm = MinhasTarefasViewModel(repo)
        vm.concluir(doDesenho[2])
        advanceUntilIdle()
        assertNull(vm.conclusao.value)
        assertNull(vm.estado.value.concluindo)
    }

    private fun minhas(lista: List<Tarefa>) = MinhasTarefasEstado(secoesDasMinhas(lista, agora), agora)

    @Test
    fun screenshotC5() = capturarTela("C5_minhas_tarefas") {
        NaEstrutura { MinhasTarefasScreen(minhas(doDesenho), {}, {}, {}, {}, {}) }
    }

    @Test
    fun screenshotC5Vazio() = capturarTela("C5_minhas_tarefas_vazio") {
        NaEstrutura { MinhasTarefasScreen(minhas(emptyList()), {}, {}, {}, {}, {}) }
    }

    // ---- C3 ----

    private fun detalheVm(tarefa: Tarefa, repo: FakeRepositorioDeTarefas = FakeRepositorioDeTarefas()): Pair<DetalheViewModel, FakeRepositorioDeTarefas> {
        repo.porId.value = mapOf(tarefa.id to tarefa)
        val vm = DetalheViewModel(
            SavedStateHandle(mapOf(DetalheViewModel.ARG_TAREFA to tarefa.id)),
            repo,
            FakeRepositorioDeConvites(membrosIniciais = listOf(eu, bruno)),
            FakeRepositorioDeConta(Sessao.Conectado("eu")),
        )
        vm.relogio(agora)
        return vm to repo
    }

    @Test
    fun `detalhe da sua tarefa devolve com confirmacao (R-19)`() = runTest {
        val (vm, repo) = detalheVm(doDesenho[2])
        val e = vm.estado.first { it.tarefa != null }
        assertEquals(SituacaoParaMim.Sua, e.situacao)
        assertTrue(e.souAdmin)
        vm.pedirDevolucao()
        assertEquals(DialogoDoDetalhe.Devolver, vm.estado.first { it.dialogo != null }.dialogo)
        vm.confirmar()
        advanceUntilIdle()
        assertEquals("devolver:3", repo.swipes.single())
    }

    @Test
    fun `devolver perto do prazo mostra o erro do backend`() = runTest {
        val repo = FakeRepositorioDeTarefas().apply { resultadoSwipe = ResultadoTarefa.Falha(ErroDeTarefa.PrazoCurto) }
        val (vm, _) = detalheVm(doDesenho[2], repo)
        vm.estado.first { it.tarefa != null }
        vm.pedirDevolucao()
        vm.confirmar()
        val e = vm.estado.first { it.aviso != null }
        assertEquals(R.string.detalhe_erro_prazo_curto, e.aviso)
        assertNull(e.dialogo)
    }

    @Test
    fun `concluir no detalhe abre C4 com o resultado`() = runTest {
        val (vm, repo) = detalheVm(doDesenho[0])
        assertEquals(SituacaoParaMim.Atrasada, vm.estado.first { it.tarefa != null }.situacao)
        vm.concluir()
        advanceUntilIdle()
        assertEquals("concluir:1", repo.swipes.single())
        assertEquals(50, vm.conclusao.value?.resultado?.pontos)
    }

    @Test
    fun `aberta sem dono pode ser assumida e editada pelo autor admin (R-12)`() = runTest {
        val aberta = doDesenho[2].copy(situacao = SituacaoDaTarefa.OPEN, donoId = null, via = null)
        val (vm, repo) = detalheVm(aberta)
        val e = vm.estado.first { it.tarefa != null }
        assertEquals(SituacaoParaMim.Aberta, e.situacao)
        assertTrue(e.podeEditar)
        vm.assumir()
        advanceUntilIdle()
        assertEquals("assumir:3", repo.swipes.single())
    }

    private fun detalhe(tarefa: Tarefa, sit: SituacaoParaMim, quando: Instant = agora, dono: Membro? = eu, admin: Boolean = false) =
        DetalheEstado(tarefa, false, sit, dono, null, podeEditar = sit == SituacaoParaMim.Aberta, souAdmin = admin, agora = quando)

    private val banheiro = doDesenho[2].copy(descricao = "Box, pia, vaso e chão. Os produtos ficam no armário do corredor, a luva amarela é a do banheiro.")

    @Composable
    private fun Detalhe(estado: DetalheEstado) = DetalheScreen(estado, {}, {}, {}, {}, {}, {}, {}, {})

    @Test
    fun screenshotC3() = capturarTela("C3_detalhe_sua") { Detalhe(detalhe(banheiro, SituacaoParaMim.Sua)) }

    @Test
    fun screenshotC3Aberta() = capturarTela("C3_detalhe_aberta") {
        Detalhe(detalhe(banheiro.copy(situacao = SituacaoDaTarefa.OPEN, donoId = null, via = null), SituacaoParaMim.Aberta, dono = null))
    }

    @Test
    fun screenshotC3Atrasada() = capturarTela("C3_detalhe_atrasada") {
        val lampada = doDesenho[0].copy(descricao = "Lâmpada de LED E27 já comprada, tá na gaveta da sala. A escada fica na lavanderia.")
        Detalhe(detalhe(lampada, SituacaoParaMim.Atrasada, quando = Instant.parse("2026-11-07T00:00:00Z")))
    }

    @Test
    fun screenshotC3DeOutraAdmin() = capturarTela("C3_detalhe_de_outra") {
        Detalhe(detalhe(banheiro.copy(donoId = "bruno"), SituacaoParaMim.DeOutra, dono = bruno, admin = true))
    }

    @Test
    fun screenshotC3Concluida() = capturarTela("C3_detalhe_concluida") { Detalhe(detalhe(doDesenho[3], SituacaoParaMim.Concluida)) }

    @Test
    fun screenshotC3Devolver() = capturarTela("C3_detalhe_devolver") {
        Detalhe(detalhe(banheiro, SituacaoParaMim.Sua).copy(dialogo = DialogoDoDetalhe.Devolver))
    }

    // ---- C4 ----

    @Test
    fun screenshotC4() = capturarTela("C4_concluida") {
        ConcluidaScreen(ConcluidaEstado("Lavar a louça do jantar", Categoria.COZINHA, conclusao), {}, {}, {})
    }

    @Test
    fun screenshotC4SemSubir() = capturarTela("C4_concluida_mesma_posicao") {
        ConcluidaScreen(
            ConcluidaEstado("Trocar a lâmpada da cozinha", Categoria.MANUTENCAO, conclusao.copy(pontos = 25, noPrazo = false, posicaoAntes = 1, posicaoDepois = 1, ultrapassou = null, sequencia = 1)),
            {},
            {},
            {},
        )
    }

    // ---- C2 em modo edição ----

    @Test
    fun `editar carrega a tarefa, salva e exclui com confirmacao (R-12)`() = runTest {
        val repo = FakeRepositorioDeTarefas()
        // O VM confere o prazo com o relógio de verdade.
        val aberta = banheiro.copy(situacao = SituacaoDaTarefa.OPEN, donoId = null, prazo = Instant.now().plusSeconds(TRES_DIAS))
        repo.porId.value = mapOf(aberta.id to aberta)
        val vm = NovaTarefaViewModel(repo, SavedStateHandle(mapOf(NovaTarefaViewModel.ARG_TAREFA to aberta.id)))
        val e = vm.estado.first { it.formulario.titulo.isNotEmpty() }
        assertTrue(e.editando)
        assertEquals(Peso.PESADA, e.formulario.peso)
        assertEquals(Categoria.BANHEIRO, e.formulario.categoria)
        vm.aoMudarTitulo("Limpar o banheiro")
        vm.criar()
        advanceUntilIdle()
        assertEquals("editar:3:Limpar o banheiro", repo.swipes.single())
        vm.pedirExclusao()
        assertTrue(vm.estado.value.confirmandoExclusao)
        vm.excluir()
        advanceUntilIdle()
        assertTrue(vm.estado.value.excluida)
        assertEquals("excluir:3", repo.swipes.last())
    }

    @Test
    fun screenshotC2Edicao() = capturarTela("C2_editar_tarefa") {
        val base = NovaTarefaViewModel.estadoInicial(agora)
        val estado = base.copy(
            formulario = base.formulario.copy(titulo = banheiro.titulo, categoria = Categoria.BANHEIRO, peso = Peso.PESADA),
            editando = true,
            atalho = null,
        )
        NovaTarefaScreen(estado, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test
    fun screenshotC2Excluir() = capturarTela("C2_editar_tarefa_excluir") {
        val base = NovaTarefaViewModel.estadoInicial(agora)
        val estado = base.copy(formulario = base.formulario.copy(titulo = banheiro.titulo), editando = true, confirmandoExclusao = true)
        NovaTarefaScreen(estado, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }

    /** Barras da estrutura principal em volta de C5, como no artboard `MinhasTarefas`. */
    @Composable
    private fun NaEstrutura(conteudo: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize()) {
            DivvupTopBar {
                DivvupAvatarDaCasa(DivvupTheme.spacing.toqueMinimo - DivvupTheme.spacing.s2)
                Column(Modifier.weight(1f)) {
                    Text("rep. cajueiro", style = DivvupTheme.typography.ttl, color = DivvupTheme.colors.text)
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
                selecionada = 1,
                onSelecionar = {},
            )
        }
    }

    private companion object {
        const val TRES_DIAS = 3 * 86_400L
    }
}
