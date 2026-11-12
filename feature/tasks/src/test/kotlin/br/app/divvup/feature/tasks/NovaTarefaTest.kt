package br.app.divvup.feature.tasks

import br.app.divvup.core.domain.tarefa.AtalhoDePrazo
import br.app.divvup.core.domain.tarefa.ErroDeTarefa
import br.app.divvup.core.domain.tarefa.ResultadoTarefa
import br.app.divvup.core.model.Categoria
import br.app.divvup.core.model.Peso
import br.app.divvup.core.testing.RegraDispatcherPrincipal
import br.app.divvup.core.testing.capturarTela
import br.app.divvup.core.testing.fakes.FakeRepositorioDeTarefas
import br.app.divvup.feature.tasks.nova.NovaTarefaScreen
import br.app.divvup.feature.tasks.nova.NovaTarefaViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class NovaTarefaTest {
    @get:Rule val principal = RegraDispatcherPrincipal()

    @Test
    fun `cria com titulo, categoria, peso e prazo padrao amanha 20h`() = runTest {
        val repo = FakeRepositorioDeTarefas()
        val vm = NovaTarefaViewModel(repo)
        assertFalse(vm.estado.value.podeCriar)
        vm.aoMudarTitulo("Lavar a louça")
        vm.aoEscolherCategoria(Categoria.LIXO)
        vm.aoEscolherPeso(Peso.PESADA)
        vm.criar()
        assertTrue(vm.estado.value.criada)
        val f = repo.criadas.single()
        assertEquals(Categoria.LIXO, f.categoria)
        assertEquals(Peso.PESADA, f.peso)
        assertEquals(LocalTime.of(20, 0), f.prazo.toLocalTime())
        assertEquals(LocalDate.now(f.prazo.zone).plusDays(1), f.prazo.toLocalDate())
    }

    @Test
    fun `titulo e descricao respeitam os limites`() = runTest {
        val vm = NovaTarefaViewModel(FakeRepositorioDeTarefas())
        vm.aoMudarTitulo("x".repeat(80))
        vm.aoMudarDescricao("y".repeat(300))
        assertEquals(60, vm.estado.value.formulario.titulo.length)
        assertEquals(280, vm.estado.value.formulario.descricao.length)
    }

    @Test
    fun `prazo no passado nao vai para o backend`() = runTest {
        val repo = FakeRepositorioDeTarefas()
        val vm = NovaTarefaViewModel(repo)
        vm.aoMudarTitulo("Lavar")
        vm.aoEscolherData(LocalDate.now().minusDays(1))
        vm.criar()
        assertEquals(ErroDeTarefa.PrazoNoPassado, vm.estado.value.erro)
        assertTrue(repo.criadas.isEmpty())
        assertEquals(null, vm.estado.value.atalho)
    }

    @Test
    fun `fechar com alteracoes pede confirmacao e falha de rede aparece`() = runTest {
        val repo = FakeRepositorioDeTarefas().apply { resultadoCriar = ResultadoTarefa.Falha(ErroDeTarefa.SemConexao) }
        val vm = NovaTarefaViewModel(repo)
        assertTrue(vm.aoFechar())
        vm.aoMudarTitulo("Lavar")
        assertFalse(vm.aoFechar())
        assertTrue(vm.estado.value.confirmandoDescarte)
        vm.continuarEditando()
        vm.criar()
        assertEquals(ErroDeTarefa.SemConexao, vm.estado.value.erro)
    }

    // Quinta, 05/11/2026, 10h em Brasília: "hoje, 22h" · "amanhã, 20h" · "sábado, 12h".
    private val quinta = Instant.parse("2026-11-05T13:00:00Z")

    @Test
    fun screenshotC2() = capturarTela("C2_nova_tarefa") {
        NovaTarefaScreen(NovaTarefaViewModel.estadoInicial(quinta), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test
    fun screenshotC2Preenchida() = capturarTela("C2_nova_tarefa_urgente") {
        val base = NovaTarefaViewModel.estadoInicial(quinta)
        NovaTarefaScreen(
            base.copy(
                formulario = base.formulario.copy(titulo = "Lavar a louça do jantar", categoria = Categoria.COZINHA, peso = Peso.MEDIA),
                atalho = AtalhoDePrazo.Hoje22h,
            ).let { it.copy(formulario = it.formulario.copy(prazo = br.app.divvup.core.domain.tarefa.prazoDoAtalho(AtalhoDePrazo.Hoje22h, quinta))) },
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}
