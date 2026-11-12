package br.app.divvup.feature.house

import androidx.lifecycle.SavedStateHandle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import br.app.divvup.core.domain.casa.ResumoDaTemporada
import br.app.divvup.core.model.Casa
import br.app.divvup.core.model.TipoDeCasa
import br.app.divvup.core.testing.RegraDispatcherPrincipal
import br.app.divvup.core.testing.capturarTela
import br.app.divvup.core.testing.fakes.FakeRepositorioDeCasa
import br.app.divvup.feature.house.principal.Aba
import br.app.divvup.feature.house.principal.EstruturaPrincipal
import br.app.divvup.feature.house.principal.PrincipalViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class PrincipalTest {
    @get:Rule val principal = RegraDispatcherPrincipal()

    private val casa = Casa("h1", "Rep. Cajueiro", TipoDeCasa.REPUBLICA, "2026-11")

    @Test
    fun `mostra a casa atual, troca de aba e calcula a temporada no fuso de Brasilia`() = runTest {
        val vm = PrincipalViewModel(FakeRepositorioDeCasa(casa), SavedStateHandle())
        assertEquals(casa, vm.casa.first { it != null })
        vm.aoTrocarAba(Aba.Ranking)
        assertEquals(Aba.Ranking, vm.aba.value)
        // 01/12 às 02h UTC ainda é 30/11 em Brasília: último dia de novembro.
        val relogio = Clock.fixed(Instant.parse("2026-12-01T02:00:00Z"), ZoneId.of("America/Sao_Paulo"))
        assertEquals(ResumoDaTemporada(11, 0), vm.temporada(relogio))
    }

    @Test
    fun screenshotEstrutura() = capturarTela("C1_estrutura") {
        EstruturaPrincipal(casa, ResumoDaTemporada(11, 24), naoLidas = 3, aba = Aba.Deck, aoTrocarAba = {}, aoAbrirNotificacoes = {}) {
            Box(Modifier.fillMaxSize())
        }
    }

    @Test
    fun screenshotEstruturaCarregando() = capturarTela("C1_estrutura_carregando") {
        EstruturaPrincipal(null, ResumoDaTemporada(11, 1), naoLidas = 0, aba = Aba.Perfil, aoTrocarAba = {}, aoAbrirNotificacoes = {}) {
            Box(Modifier.fillMaxSize())
        }
    }
}
