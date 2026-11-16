package br.app.divvup.feature.profile

import br.app.divvup.core.domain.notificacao.Preferencias
import br.app.divvup.core.model.TipoDeNotificacao
import br.app.divvup.core.testing.RegraDispatcherPrincipal
import br.app.divvup.core.testing.capturarTela
import br.app.divvup.core.testing.fakes.FakeRepositorioDeNotificacoes
import br.app.divvup.feature.profile.notificacoes.PrefsNotifEstado
import br.app.divvup.feature.profile.notificacoes.PrefsNotifScreen
import br.app.divvup.feature.profile.notificacoes.PrefsNotifViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PrefsNotifTest {
    @get:Rule val principal = RegraDispatcherPrincipal()

    @Test
    fun `padroes da R-33 e mudancas salvas na hora`() = runTest {
        val repo = FakeRepositorioDeNotificacoes()
        val vm = PrefsNotifViewModel(repo)
        val inicial = vm.estado.first().preferencias
        assertTrue(inicial.ligado(TipoDeNotificacao.NEW_TASK))
        assertFalse(inicial.ligado(TipoDeNotificacao.MEMBER_COMPLETED))
        assertEquals(LocalTime.of(22, 0), inicial.inicio)

        vm.mudarTipos(listOf(TipoDeNotificacao.DUE_24H, TipoDeNotificacao.DUE_2H), false)
        advanceUntilIdle()
        assertFalse(repo.prefs.value.ligado(TipoDeNotificacao.DUE_2H))
        vm.mudarFim(LocalTime.of(7, 30))
        advanceUntilIdle()
        assertEquals(LocalTime.of(7, 30), repo.prefs.value.fim)
    }

    @Test
    fun `falha ao salvar volta ao que estava e avisa`() = runTest {
        val repo = FakeRepositorioDeNotificacoes().apply { salvarDaCerto = false }
        val vm = PrefsNotifViewModel(repo)
        vm.estado.first()
        vm.mudarSilencio(false)
        assertTrue(vm.estado.first { it.erro }.erro)
        assertTrue(vm.estado.first { it.preferencias.silencioLigado }.preferencias.silencioLigado)
    }

    @Test
    fun screenshotE4() = capturarTela("E4_prefs_notif") { PrefsNotifScreen(PrefsNotifEstado(Preferencias()), {}, { _, _ -> }, {}, {}, {}, {}) }

    @Test
    fun screenshotE4Negadas() = capturarTela("E4_prefs_notif_negadas") {
        PrefsNotifScreen(PrefsNotifEstado(Preferencias(silencioLigado = false), negadas = true), {}, { _, _ -> }, {}, {}, {}, {})
    }
}
