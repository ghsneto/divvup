package br.app.divvup.feature.onboarding

import br.app.divvup.core.domain.casa.ErroDeCasa
import br.app.divvup.core.domain.casa.ResultadoCasa
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.model.TipoDeCasa
import br.app.divvup.core.model.Usuario
import br.app.divvup.core.testing.RegraDispatcherPrincipal
import br.app.divvup.core.testing.capturarTela
import br.app.divvup.core.testing.fakes.FakeRepositorioDeCasa
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConta
import br.app.divvup.feature.onboarding.casa.CriarCasaEstado
import br.app.divvup.feature.onboarding.casa.CriarCasaScreen
import br.app.divvup.feature.onboarding.casa.CriarCasaViewModel
import br.app.divvup.feature.onboarding.casa.SuaCasaScreen
import br.app.divvup.feature.onboarding.casa.SuaCasaViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CasaTest {
    @get:Rule val principal = RegraDispatcherPrincipal()

    @Test
    fun `B2 cumprimenta pelo primeiro nome`() = runTest {
        val conta = FakeRepositorioDeConta(Sessao.Conectado("u1"), Usuario("u1", "Iane Souza", null, "avataaars:divvup-01", null))
        assertEquals("Iane", SuaCasaViewModel(conta).primeiroNome.value)
    }

    @Test
    fun `B3 cria a casa com nome, tipo e anuncio`() = runTest {
        val casas = FakeRepositorioDeCasa()
        val vm = CriarCasaViewModel(casas)
        vm.aoMudarNome("Rep. Cajueiro")
        vm.criar()
        assertEquals(listOf("criar:Rep. Cajueiro:REPUBLICA:true"), casas.chamadas)
        assertEquals("Rep. Cajueiro", vm.estado.value.criada)
    }

    @Test
    fun `B3 trocar o tipo reinicia o anuncio e so republica anuncia`() = runTest {
        val casas = FakeRepositorioDeCasa()
        val vm = CriarCasaViewModel(casas)
        vm.aoMudarNome("Apê")
        vm.aoEscolherTipo(TipoDeCasa.CASAL_FAMILIA)
        assertFalse(vm.estado.value.anunciar)
        vm.aoAnunciar(true)
        vm.criar()
        assertEquals(listOf("criar:Apê:CASAL_FAMILIA:false"), casas.chamadas)
    }

    @Test
    fun `B3 nome vazio mostra erro e nao chama o backend`() = runTest {
        val casas = FakeRepositorioDeCasa()
        val vm = CriarCasaViewModel(casas)
        vm.aoMudarNome("   ")
        vm.criar()
        assertTrue(vm.estado.value.nomeVazio)
        assertTrue(casas.chamadas.isEmpty())
        vm.aoMudarNome("x".repeat(40))
        assertEquals(32, vm.estado.value.nome.length)
        assertFalse(vm.estado.value.nomeVazio)
    }

    @Test
    fun `B3 falha de rede mostra erro e quem ja tem casa segue em frente (R-02)`() = runTest {
        val casas = FakeRepositorioDeCasa().apply { resultadoCriar = ResultadoCasa.Falha(ErroDeCasa.SemConexao) }
        val vm = CriarCasaViewModel(casas)
        vm.aoMudarNome("Rep. Cajueiro")
        vm.criar()
        assertTrue(vm.estado.value.erroDeConexao)
        assertNull(vm.estado.value.criada)

        casas.resultadoCriar = ResultadoCasa.Falha(ErroDeCasa.JaEmCasa)
        vm.criar()
        assertTrue(vm.jaTinhaCasa.value)
        assertFalse(vm.estado.value.erroDeConexao)
    }

    @Test
    fun screenshotB2() = capturarTela("B2_sua_casa") {
        SuaCasaScreen("Iane", {}, {}, {}, {})
    }

    @Test
    fun screenshotB3() = capturarTela("B3_criar_casa") {
        CriarCasaScreen(CriarCasaEstado(nome = "Rep. Cajueiro"), {}, {}, {}, {}, {}, {})
    }

    @Test
    fun screenshotB3NomeVazio() = capturarTela("B3_criar_casa_nome_vazio") {
        CriarCasaScreen(CriarCasaEstado(nome = "", nomeVazio = true, tipo = TipoDeCasa.MORADIA_COMPARTILHADA, anunciar = false), {}, {}, {}, {}, {}, {})
    }

    @Test
    fun screenshotB3Criada() = capturarTela("B3_casa_criada") {
        CriarCasaScreen(CriarCasaEstado(nome = "Rep. Cajueiro", criada = "Rep. Cajueiro"), {}, {}, {}, {}, {}, {})
    }
}
