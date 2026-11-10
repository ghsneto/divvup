package br.app.divvup.feature.house

import androidx.lifecycle.SavedStateHandle
import br.app.divvup.core.domain.casa.DadosDaCasa
import br.app.divvup.core.domain.casa.ErroDeCasa
import br.app.divvup.core.domain.casa.ResultadoCasa
import br.app.divvup.core.domain.casa.ResumoDaTemporada
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.model.Casa
import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.Papel
import br.app.divvup.core.model.TipoDeCasa
import br.app.divvup.core.testing.RegraDispatcherPrincipal
import br.app.divvup.core.testing.capturarTela
import br.app.divvup.core.testing.fakes.FakeRepositorioDeCasa
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConta
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConvites
import br.app.divvup.feature.house.membros.EditarCasaEstado
import br.app.divvup.feature.house.membros.EditarCasaScreen
import br.app.divvup.feature.house.membros.EditarCasaViewModel
import br.app.divvup.feature.house.membros.MembroEstado
import br.app.divvup.feature.house.membros.MembroScreen
import br.app.divvup.feature.house.membros.MembroViewModel
import br.app.divvup.feature.house.membros.MinhaCasaEstado
import br.app.divvup.feature.house.membros.MinhaCasaScreen
import br.app.divvup.feature.house.membros.MinhaCasaViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class MembrosTest {
    @get:Rule val principal = RegraDispatcherPrincipal()

    private val agora = Instant.parse("2026-11-10T15:00:00Z")
    private val casa = Casa("h1", "Rep. Cajueiro", TipoDeCasa.REPUBLICA, "2026-11", "Várzea", "Recife", "Rua Amaro Albino Pimentel", anuncioAtivo = true)

    /** Os moradores do desenho de G1. */
    private val moradores = listOf(
        Membro("eu", "Iane", "avataaars:divvup-01", Papel.ADMIN, true, 325, Instant.parse("2025-03-02T12:00:00Z"), 1_900),
        Membro("ana", "Ana", "avataaars:divvup-02", Papel.ADMIN, true, 480, Instant.parse("2025-03-05T12:00:00Z"), 3_240),
        Membro("bruno", "Bruno", "avataaars:divvup-06", Papel.MEMBER, true, 300, Instant.parse("2025-04-10T12:00:00Z"), 1_200),
        Membro("caio", "Caio", "avataaars:divvup-24", Papel.MEMBER, true, 175, Instant.parse("2026-02-10T12:00:00Z"), 400),
        Membro("davi", "Davi", "avataaars:divvup-03", Papel.MEMBER, true, 100, Instant.parse("2026-11-09T12:00:00Z"), 100),
    )

    @Test
    fun `G1 junta casa, moradores e sai da casa`() = runTest {
        val casas = FakeRepositorioDeCasa(casa)
        val vm = MinhaCasaViewModel(casas, FakeRepositorioDeConvites(membrosIniciais = moradores), FakeRepositorioDeConta(Sessao.Conectado("eu")))
        val e = vm.estado.first { it.membros.isNotEmpty() && it.casa != null }
        assertTrue(e.souAdmin)
        vm.pedirSaida()
        vm.sair()
        assertTrue(vm.saiu.value)
        assertEquals(listOf("sair"), casas.chamadas)
    }

    @Test
    fun `G3 admin alterna o papel, ve o erro do ultimo admin e remove`() = runTest {
        val casas = FakeRepositorioDeCasa(casa)
        val vm = MembroViewModel(SavedStateHandle(mapOf("uid" to "ana")), casas, FakeRepositorioDeConvites(membrosIniciais = moradores), FakeRepositorioDeConta(Sessao.Conectado("eu")))
        val e = vm.estado.first { it.membro != null }
        assertTrue(e.visaoDeAdmin)
        assertEquals(1, e.posicao)
        casas.resultadoOperacao = ResultadoCasa.Falha(ErroDeCasa.UltimoAdmin)
        vm.alternarAdmin()
        assertTrue(vm.estado.first { it.membro != null }.erroUltimoAdmin)
        casas.resultadoOperacao = ResultadoCasa.Ok(Unit)
        vm.remover()
        assertTrue(vm.removido.value)
        assertEquals(listOf("papel:ana:MEMBER", "remover:ana"), casas.chamadas)
    }

    @Test
    fun `D3 quem nao e admin nao ve acoes`() = runTest {
        val vm = MembroViewModel(SavedStateHandle(mapOf("uid" to "eu")), FakeRepositorioDeCasa(casa), FakeRepositorioDeConvites(membrosIniciais = moradores), FakeRepositorioDeConta(Sessao.Conectado("bruno")))
        assertFalse(vm.estado.first { it.membro != null }.visaoDeAdmin)
    }

    @Test
    fun `G4 carrega a casa, valida o nome e salva`() = runTest {
        val casas = FakeRepositorioDeCasa(casa)
        val vm = EditarCasaViewModel(casas)
        assertEquals("Recife", vm.estado.value.dados.cidade)
        vm.aoMudar(vm.estado.value.dados.copy(nome = " "))
        vm.salvar()
        assertTrue(vm.estado.value.nomeVazio)
        vm.aoMudar(DadosDaCasa("Rep. Cajueiro", TipoDeCasa.CASAL_FAMILIA, "Recife", "Várzea", ""))
        vm.salvar()
        assertTrue(vm.salvo.value)
        assertEquals(listOf("editar:Rep. Cajueiro:CASAL_FAMILIA:Recife:Várzea:"), casas.chamadas)
    }

    @Test
    fun screenshotG1() = capturarTela("G1_minha_casa") {
        MinhaCasaScreen(
            MinhaCasaEstado(casa, moradores, "eu", codigosAtivos = 2, temporada = ResumoDaTemporada(11, 20), agora = agora),
            {}, {}, {}, {}, {}, {}, {},
        )
    }

    @Test
    fun screenshotG1Sair() = capturarTela("G1_minha_casa_sair") {
        MinhaCasaScreen(
            MinhaCasaEstado(casa, moradores, "bruno", temporada = ResumoDaTemporada(11, 20), agora = agora, confirmandoSaida = true),
            {}, {}, {}, {}, {}, {}, {},
        )
    }

    @Test
    fun screenshotG3() = capturarTela("G3_membro_admin") {
        MembroScreen(MembroEstado(moradores[1], posicao = 1, visaoDeAdmin = true, agora = agora), {}, {}, {}, {}, {})
    }

    @Test
    fun screenshotD3() = capturarTela("D3_membro") {
        MembroScreen(MembroEstado(moradores[2], posicao = 3, agora = agora), {}, {}, {}, {}, {})
    }

    @Test
    fun screenshotG4() = capturarTela("G4_editar_casa") {
        EditarCasaScreen(EditarCasaEstado(DadosDaCasa("Rep. Cajueiro", TipoDeCasa.REPUBLICA, "Recife", "Várzea", "Rua Amaro Albino Pimentel"), anuncioAtivo = true), {}, {}, {})
    }

    @Test
    fun screenshotG4Casal() = capturarTela("G4_editar_casa_casal") {
        EditarCasaScreen(EditarCasaEstado(DadosDaCasa("Rep. Cajueiro", TipoDeCasa.CASAL_FAMILIA, "Recife", "Várzea", "Rua Amaro Albino Pimentel"), anuncioAtivo = true), {}, {}, {})
    }
}
