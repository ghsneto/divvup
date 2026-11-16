package br.app.divvup.feature.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.app.divvup.core.designsystem.component.AbaNavegacao
import br.app.divvup.core.designsystem.component.DivvupTabBar
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.model.Casa
import br.app.divvup.core.model.Conquista
import br.app.divvup.core.model.Medalha
import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.Papel
import br.app.divvup.core.model.Tema
import br.app.divvup.core.model.TipoDeCasa
import br.app.divvup.core.model.Usuario
import br.app.divvup.core.testing.RegraDispatcherPrincipal
import br.app.divvup.core.testing.capturarTela
import br.app.divvup.core.testing.fakes.FakePreferenciasDoAparelho
import br.app.divvup.core.testing.fakes.FakeRepositorioDeCasa
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConta
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConvites
import br.app.divvup.feature.profile.conta.AparenciaScreen
import br.app.divvup.feature.profile.conta.EditarPerfilEstado
import br.app.divvup.feature.profile.conta.EditarPerfilScreen
import br.app.divvup.feature.profile.conta.ExcluirContaEstado
import br.app.divvup.feature.profile.conta.ExcluirContaScreen
import br.app.divvup.feature.profile.conta.ExcluirContaViewModel
import br.app.divvup.feature.profile.conta.SobreScreen
import br.app.divvup.feature.profile.perfil.EditarPerfilViewModel
import br.app.divvup.feature.profile.perfil.PerfilEstado
import br.app.divvup.feature.profile.perfil.PerfilScreen
import br.app.divvup.feature.profile.perfil.PerfilViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PerfilTest {
    @get:Rule val principal = RegraDispatcherPrincipal()

    private val hoje = LocalDate.of(2026, 11, 6)

    /** A Iane do desenho (Perfil). */
    private val iane = Usuario(
        "eu", "Iane", "iane@email.com", "avataaars:divvup-01", "h1",
        sequencia = 4, melhorSequencia = 12, ultimoDiaDaSequencia = hoje,
        pontosTotais = 2_115, tarefasFeitas = 52,
        medalhas = mapOf(
            Medalha.CAMPEAO to Conquista(1, Instant.parse("2026-11-01T03:00:00Z")),
            Medalha.SETE_DIAS to Conquista(1, Instant.parse("2026-09-12T15:00:00Z")),
            Medalha.PRIMEIRA_TAREFA to Conquista(1, Instant.parse("2025-03-05T15:00:00Z")),
        ),
    )
    private val casa = Casa("h1", "Rep. Cajueiro", TipoDeCasa.REPUBLICA, "2026-11")
    private val eu = Membro("eu", "Iane", "avataaars:divvup-01", Papel.ADMIN, true, 325, Instant.parse("2025-03-02T12:00:00Z"))
    private val ana = Membro("ana", "Ana", "avataaars:divvup-02", Papel.MEMBER, true, 480, Instant.parse("2025-03-05T12:00:00Z"))

    @Test
    fun `perfil junta conta, casa, pontos do mes e tema e a dica so nas bloqueadas`() = runTest {
        val vm = PerfilViewModel(
            FakeRepositorioDeConta(Sessao.Conectado("eu"), iane),
            FakeRepositorioDeCasa(casa),
            FakeRepositorioDeConvites(membrosIniciais = listOf(eu)),
            FakePreferenciasDoAparelho(Tema.ESCURO),
        )
        vm.recarregar()
        val e = vm.estado.first { it.usuario != null }
        assertEquals(325, e.pontosDoMes)
        assertEquals("Rep. Cajueiro", e.casa)
        assertEquals(Tema.ESCURO, e.tema)
        vm.tocarMedalha(Medalha.CAMPEAO)
        assertNull(vm.estado.value.dica)
        vm.tocarMedalha(Medalha.SALVA_VIDAS)
        assertEquals(Medalha.SALVA_VIDAS, vm.estado.first { it.dica != null }.dica)
    }

    @Test
    fun `editar salva nome e avatar`() = runTest {
        val conta = FakeRepositorioDeConta(Sessao.Conectado("eu"), iane)
        val vm = EditarPerfilViewModel(conta)
        assertEquals("Iane", vm.estado.first { it.nome.isNotEmpty() }.nome)
        vm.mudarNome("Iane S.")
        vm.escolherAvatar("avataaars:divvup-05")
        vm.salvar()
        advanceUntilIdle()
        assertTrue(vm.salvo.value)
        assertEquals("avataaars:divvup-05", conta.perfilAtual?.avatarId)
    }

    @Test
    fun `excluir avisa do unico admin e pede EXCLUIR e senha`() = runTest {
        val conta = FakeRepositorioDeConta(Sessao.Conectado("eu"), iane)
        val vm = ExcluirContaViewModel(conta, FakeRepositorioDeCasa(casa), FakeRepositorioDeConvites(membrosIniciais = listOf(eu, ana)))
        val e = vm.estado.first { it.sucessor != null }
        assertEquals("Ana", e.sucessor)
        vm.continuar()
        vm.mudarPalavra("excluir")
        assertTrue(!vm.estado.value.podeExcluir)
        vm.mudarSenha("segredo123")
        assertTrue(vm.estado.value.podeExcluir)
        vm.excluir()
        advanceUntilIdle()
        assertTrue(vm.estado.value.feita)
        assertEquals("excluir:segredo123", conta.chamadas.last())
        conta.resultadoExcluir = ResultadoConta.Ok(Unit)
    }

    private val estadoDoDesenho = PerfilEstado(iane, pontosDoMes = 325, casa = "Rep. Cajueiro", hoje = hoje)

    @Composable
    private fun E1(estado: PerfilEstado) = NaEstrutura {
        PerfilScreen(estado, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test
    fun screenshotE1() = capturarTela("E1_perfil") { E1(estadoDoDesenho) }

    @Test
    fun screenshotE1Dica() = capturarTela("E1_perfil_dica") { E1(estadoDoDesenho.copy(dica = Medalha.SALVA_VIDAS)) }

    @Test
    fun screenshotE1Sair() = capturarTela("E1_perfil_sair") { E1(estadoDoDesenho.copy(confirmandoSaida = true)) }

    @Test
    fun screenshotE2() = capturarTela("E2_editar_perfil") {
        EditarPerfilScreen(EditarPerfilEstado("Iane", "avataaars:divvup-01", "iane@email.com"), {}, {}, {}, {}, {})
    }

    @Test
    fun screenshotE5() = capturarTela("E5_aparencia") { AparenciaScreen(Tema.SISTEMA, {}, {}) }

    @Test
    fun screenshotE6() = capturarTela("E6_sobre") { SobreScreen("1.0.0", 100, {}, {}, {}, {}) }

    @Test
    fun screenshotE7() = capturarTela("E7_excluir_conta") {
        ExcluirContaScreen(ExcluirContaEstado(casa = "Rep. Cajueiro", sucessor = "Ana"), {}, {}, {}, {}, {}, {}, {})
    }

    @Test
    fun screenshotE7Passo2() = capturarTela("E7_excluir_conta_confirma") {
        ExcluirContaScreen(ExcluirContaEstado(passo = 2, senha = "segredo123", palavra = "EXC"), {}, {}, {}, {}, {}, {}, {})
    }

    @Test
    fun screenshotE7Feita() = capturarTela("E7_excluir_conta_feita") { ExcluirContaScreen(ExcluirContaEstado(feita = true), {}, {}, {}, {}, {}, {}, {}) }

    @Composable
    private fun NaEstrutura(conteudo: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { conteudo() }
            DivvupTabBar(
                listOf(
                    AbaNavegacao(DivvupIcone.Deck, "Deck"),
                    AbaNavegacao(DivvupIcone.Tarefas, "Minhas tarefas"),
                    AbaNavegacao(DivvupIcone.Ranking, "Ranking"),
                    AbaNavegacao(DivvupIcone.Perfil, "Perfil"),
                ),
                selecionada = 3,
                onSelecionar = {},
            )
        }
    }
}
