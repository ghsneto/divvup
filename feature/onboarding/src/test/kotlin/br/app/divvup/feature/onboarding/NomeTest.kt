package br.app.divvup.feature.onboarding

import br.app.divvup.core.domain.conta.ErroDeConta
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.domain.perfil.GaleriaDeAvatares
import br.app.divvup.core.model.Usuario
import br.app.divvup.core.testing.RegraDispatcherPrincipal
import br.app.divvup.core.testing.capturarTela
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConta
import br.app.divvup.feature.onboarding.nome.NomeEstado
import br.app.divvup.feature.onboarding.nome.NomeScreen
import br.app.divvup.feature.onboarding.nome.NomeViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class NomeTest {
    @get:Rule val principal = RegraDispatcherPrincipal()

    private fun repo() = FakeRepositorioDeConta(Sessao.Conectado("u1"), Usuario("u1", "Iane Souza", "i@e.com", null, null))

    @Test
    fun `pre-preenche o nome do perfil e salva nome e avatar`() = runTest {
        val r = repo()
        val vm = NomeViewModel(r)
        assertEquals("Iane Souza", vm.estado.value.nome)
        assertEquals(GaleriaDeAvatares.padrao, vm.estado.value.avatarId)
        vm.aoEscolherAvatar("avataaars:divvup-07")
        vm.continuar()
        assertTrue(vm.estado.value.concluido)
        assertEquals(listOf("salvar:u1:Iane Souza:avataaars:divvup-07"), r.chamadas)
    }

    @Test
    fun `nome vazio nao continua e nome longo e cortado em 24`() = runTest {
        val vm = NomeViewModel(repo())
        vm.aoMudarNome("   ")
        assertFalse(vm.estado.value.podeContinuar)
        vm.aoMudarNome("x".repeat(30))
        assertEquals(24, vm.estado.value.nome.length)
    }

    @Test
    fun `falha ao salvar mostra erro`() = runTest {
        val r = repo().apply { resultadoSalvar = ResultadoConta.Falha(ErroDeConta.SemConexao) }
        val vm = NomeViewModel(r)
        vm.continuar()
        assertTrue(vm.estado.value.erro)
        assertFalse(vm.estado.value.concluido)
    }

    @Test
    fun screenshotB1() = capturarTela("B1_nome") {
        NomeScreen(NomeEstado(nome = "Iane", avatarId = GaleriaDeAvatares.padrao), {}, {}, {}, {})
    }
}
