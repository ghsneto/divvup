package br.app.divvup.feature.house

import br.app.divvup.core.domain.casa.ErroDeConvite
import br.app.divvup.core.domain.casa.VALIDADE_CONVITE
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.model.Convite
import br.app.divvup.core.testing.RegraDispatcherPrincipal
import br.app.divvup.core.testing.capturarTela
import br.app.divvup.core.testing.fakes.FakeRepositorioDeCasa
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConta
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConvites
import br.app.divvup.feature.house.convites.ConvideEstado
import br.app.divvup.feature.house.convites.ConvideScreen
import br.app.divvup.feature.house.convites.ConvideViewModel
import br.app.divvup.feature.house.convites.ConvitesEstado
import br.app.divvup.feature.house.convites.ConvitesScreen
import br.app.divvup.feature.house.convites.ConvitesViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ConvitesTest {
    @get:Rule val principal = RegraDispatcherPrincipal()

    private val agora = Instant.parse("2026-11-10T15:00:00Z")

    private fun convite(codigo: String, por: String, nome: String, criado: Instant) =
        Convite(codigo, por, nome, criado, criado.plus(VALIDADE_CONVITE), null, null, false, null)

    /** Os 5 códigos do desenho de G2. */
    private val doDesenho = listOf(
        convite("K7PQ2X", "eu", "Iane", agora.minus(Duration.ofMinutes(19))),
        convite("H3MW9R", "ana", "Ana", agora.minus(Duration.ofHours(17).plusMinutes(48))),
        convite("T8ZN4B", "eu", "Iane", Instant.parse("2026-11-09T12:00:00Z"))
            .copy(usadoPorNome = "Davi Lima", usadoEm = Instant.parse("2026-11-09T21:00:00Z")),
        convite("F2CJ7D", "eu", "Iane", Instant.parse("2026-11-08T21:00:00Z")),
        convite("R5YB8K", "ana", "Ana", Instant.parse("2026-11-07T12:00:00Z")).copy(revogado = true, revogadoEm = Instant.parse("2026-11-08T13:00:00Z")),
    )

    @Test
    fun `B8 gera um codigo ao abrir e outro sob demanda`() = runTest {
        val convites = FakeRepositorioDeConvites()
        val vm = ConvideViewModel(convites, FakeRepositorioDeCasa())
        assertEquals("K7PQ2X", vm.estado.value.codigo)
        convites.proximoCodigo = "H3MW9R"
        vm.gerar()
        assertEquals("H3MW9R", vm.estado.value.codigo)
        assertEquals(R.string.convide_novo, vm.estado.value.aviso)
        assertEquals(listOf("criar", "criar"), convites.chamadas)
    }

    @Test
    fun `B8 mostra erro se nao conseguir gerar`() = runTest {
        val convites = FakeRepositorioDeConvites().apply { falhaAoCriar = ErroDeConvite.SemConexao }
        val vm = ConvideViewModel(convites, FakeRepositorioDeCasa())
        assertTrue(vm.estado.value.erro)
    }

    @Test
    fun `G2 lista os codigos, gera e revoga`() = runTest {
        val convites = FakeRepositorioDeConvites(doDesenho)
        val vm = ConvitesViewModel(convites, FakeRepositorioDeCasa(), FakeRepositorioDeConta(Sessao.Conectado("eu")))
        assertEquals(5, vm.estado.first { it.convites.isNotEmpty() }.convites.size)
        vm.revogar("K7PQ2X")
        vm.gerar()
        assertEquals(listOf("revogar:K7PQ2X", "criar"), convites.chamadas)
    }

    @Test
    fun screenshotB8() = capturarTela("B8_convide") {
        ConvideScreen(
            ConvideEstado(nomeDaCasa = "Rep. Cajueiro", codigo = "K7PQ2X", restante = Duration.ofHours(23).plusMinutes(59).plusSeconds(41)),
            {}, {}, {}, {},
        )
    }

    @Test
    fun screenshotG2() = capturarTela("G2_convites") {
        ConvitesScreen(ConvitesEstado(nomeDaCasa = "Rep. Cajueiro", convites = doDesenho, eu = "eu", agora = agora), {}, {}, {}, {})
    }
}
