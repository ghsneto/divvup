package br.app.divvup.feature.profile

import androidx.lifecycle.SavedStateHandle
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.domain.perfil.GaleriaDeAvatares
import br.app.divvup.core.model.Usuario
import br.app.divvup.core.testing.RegraDispatcherPrincipal
import br.app.divvup.core.testing.capturarTela
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConta
import br.app.divvup.feature.profile.avatar.GaleriaArgs
import br.app.divvup.feature.profile.avatar.GaleriaDeAvataresScreen
import br.app.divvup.feature.profile.avatar.GaleriaEstado
import br.app.divvup.feature.profile.avatar.GaleriaViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class GaleriaTest {
    @get:Rule val principal = RegraDispatcherPrincipal()

    private fun repo() = FakeRepositorioDeConta(Sessao.Conectado("u1"), Usuario("u1", "Iane", null, null, null))

    @Test
    fun `modo onboarding so devolve a escolha, sem gravar`() = runTest {
        val r = repo()
        val vm = GaleriaViewModel(r, SavedStateHandle(mapOf(GaleriaArgs.INICIAL to "avataaars:divvup-03", GaleriaArgs.SO_ESCOLHER to true)))
        assertEquals("avataaars:divvup-03", vm.estado.value.selecionado)
        vm.escolher("avataaars:divvup-10")
        vm.salvar()
        assertEquals("avataaars:divvup-10", vm.concluido.value)
        assertTrue(r.chamadas.isEmpty())
    }

    @Test
    fun `modo perfil grava o avatar mantendo o nome (R-32)`() = runTest {
        val r = repo()
        val vm = GaleriaViewModel(r, SavedStateHandle())
        assertEquals(GaleriaDeAvatares.padrao, vm.estado.value.selecionado)
        vm.escolher("avataaars:divvup-24")
        vm.salvar()
        assertEquals(listOf("salvar:u1:Iane:avataaars:divvup-24"), r.chamadas)
        assertEquals("avataaars:divvup-24", vm.concluido.value)
    }

    @Test
    fun screenshotE3() = capturarTela("E3_avatar") {
        GaleriaDeAvataresScreen(GaleriaEstado(selecionado = GaleriaDeAvatares.padrao), {}, {}, {})
    }
}
