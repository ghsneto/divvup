package br.app.divvup.feature.onboarding

import br.app.divvup.core.designsystem.icon.IlustracaoCategoria
import br.app.divvup.core.domain.casa.ErroDeConvite
import br.app.divvup.core.domain.casa.ResultadoConvite
import br.app.divvup.core.model.Casa
import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.Papel
import br.app.divvup.core.model.TipoDeCasa
import br.app.divvup.core.testing.RegraDispatcherPrincipal
import br.app.divvup.core.testing.capturarTela
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConvites
import br.app.divvup.feature.onboarding.bemvindo.BemVindoEstado
import br.app.divvup.feature.onboarding.bemvindo.BemVindoScreen
import br.app.divvup.feature.onboarding.codigo.CodigoEstado
import br.app.divvup.feature.onboarding.codigo.CodigoScreen
import br.app.divvup.feature.onboarding.codigo.CodigoViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CodigoTest {
    @get:Rule val principal = RegraDispatcherPrincipal()

    @Test
    fun `B4 normaliza o que foi digitado e entra com 6 caracteres`() = runTest {
        val convites = FakeRepositorioDeConvites()
        val vm = CodigoViewModel(convites)
        vm.aoDigitar("k7p")
        vm.entrar()
        assertTrue(convites.chamadas.isEmpty())
        vm.aoDigitar("k7p-q2x")
        assertEquals("K7PQ2X", vm.estado.value.codigo)
        vm.entrar()
        assertEquals(listOf("entrar:K7PQ2X"), convites.chamadas)
        assertTrue(vm.estado.value.entrou)
    }

    @Test
    fun `B4 mostra o erro do backend e limpa ao digitar`() = runTest {
        val convites = FakeRepositorioDeConvites().apply { resultadoEntrar = ResultadoConvite.Falha(ErroDeConvite.Expirado) }
        val vm = CodigoViewModel(convites)
        vm.aoDigitar("K7PQ2X")
        vm.entrar()
        assertEquals(ErroDeConvite.Expirado, vm.estado.value.erro)
        assertFalse(vm.estado.value.entrou)
        vm.aoDigitar("K7PQ2")
        assertEquals(null, vm.estado.value.erro)
    }

    @Test
    fun screenshotB4() = capturarTela("B4_codigo") {
        CodigoScreen(CodigoEstado(codigo = "K7PQ2X", erro = ErroDeConvite.Expirado), {}, {}, {}, {})
    }

    @Test
    fun screenshotB4Digitando() = capturarTela("B4_codigo_digitando") {
        CodigoScreen(CodigoEstado(codigo = "K7P"), {}, {}, {}, {})
    }

    @Test
    fun screenshotB5() = capturarTela("B5_bem_vindo") {
        val membros = listOf("Ana" to 2, "Bruno" to 6, "Caio" to 11, "Davi" to 18, "Você" to 1).mapIndexed { i, (nome, avatar) ->
            Membro("u$i", nome, "avataaars:divvup-%02d".format(avatar), Papel.MEMBER, true, 0, null)
        }
        BemVindoScreen(
            BemVindoEstado(
                casa = Casa("h1", "Rep. Cajueiro", TipoDeCasa.REPUBLICA, "2026-11", bairro = "Várzea", cidade = "Recife"),
                membros = membros,
                eu = "u4",
                tarefasAbertas = 7,
                miniaturas = listOf(
                    IlustracaoCategoria.Cozinha,
                    IlustracaoCategoria.Lixo,
                    IlustracaoCategoria.Pets,
                    IlustracaoCategoria.Plantas,
                    IlustracaoCategoria.LimpezaGeral,
                ),
            ),
            aoBora = {},
        )
    }
}
