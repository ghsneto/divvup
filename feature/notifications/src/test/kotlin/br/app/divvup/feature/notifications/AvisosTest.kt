package br.app.divvup.feature.notifications

import br.app.divvup.core.model.Notificacao
import br.app.divvup.core.model.TipoDeNotificacao
import br.app.divvup.core.testing.RegraDispatcherPrincipal
import br.app.divvup.core.testing.capturarTela
import br.app.divvup.core.testing.fakes.FakeRepositorioDeNotificacoes
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AvisosTest {
    @get:Rule val principal = RegraDispatcherPrincipal()

    // Sexta, 06/11/2026, 12h em Brasília.
    private val agora = Instant.parse("2026-11-06T15:00:00Z")

    private fun n(id: String, tipo: TipoDeNotificacao, titulo: String, corpo: String, quando: String, lida: Boolean) =
        Notificacao(id, tipo, titulo, corpo, lida, Instant.parse(quando), tarefaId = "t$id".takeIf { tipo != TipoDeNotificacao.SEASON_END })

    /** Os avisos do desenho (Notificacoes). */
    private val doDesenho = listOf(
        n("a", TipoDeNotificacao.AUTO_ASSIGNED, "Tirar o lixo veio pra você", "Ninguém pegou e você tem menos pontos no mês.", "2026-11-06T13:00:00Z", false),
        n("b", TipoDeNotificacao.DUE_2H, "Lavar a louça vence em 2 h", "Às 22h. Bora?", "2026-11-06T12:00:00Z", false),
        n("c", TipoDeNotificacao.NEW_TASK, "Bruno criou Passear com o Thor", "Pets · 50 pts", "2026-11-06T10:00:00Z", false),
        n("d", TipoDeNotificacao.LATE, "Trocar a lâmpada atrasou", "Concluindo agora você ganha 25 pts.", "2026-11-05T23:00:00Z", true),
        n("e", TipoDeNotificacao.REMOVED, "Davi entrou na casa", "Agora são 5 moradores.", "2026-11-05T21:00:00Z", true),
        n("f", TipoDeNotificacao.SEASON_END, "Ana venceu outubro", "Temporada fechada. Você ficou em 2º.", "2026-11-01T03:00:00Z", true),
    )

    @Test
    fun `marcar todas e abrir marcam como lidas`() = runTest {
        val repo = FakeRepositorioDeNotificacoes(doDesenho)
        val vm = AvisosViewModel(repo)
        assertEquals(3, vm.estado.first { !it.carregando }.naoLidas)
        vm.abrir(doDesenho[0])
        advanceUntilIdle()
        assertEquals(2, vm.estado.first { it.naoLidas == 2 }.naoLidas)
        vm.marcarTodas()
        advanceUntilIdle()
        assertEquals(0, vm.estado.first { it.naoLidas == 0 }.naoLidas)
    }

    @Test
    fun screenshotF1() = capturarTela("F1_avisos") { AvisosScreen(AvisosEstado(doDesenho, false, agora), {}, {}, {}) }

    @Test
    fun screenshotF1Vazio() = capturarTela("F1_avisos_vazio") { AvisosScreen(AvisosEstado(emptyList(), false, agora), {}, {}, {}) }
}
