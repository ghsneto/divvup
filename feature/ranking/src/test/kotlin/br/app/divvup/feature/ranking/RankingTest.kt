package br.app.divvup.feature.ranking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.app.divvup.core.designsystem.component.AbaNavegacao
import br.app.divvup.core.designsystem.component.DivvupAvatarDaCasa
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupTabBar
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.casa.ResumoDaTemporada
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.domain.ranking.equilibrioDoMes
import br.app.divvup.core.domain.ranking.rankingDoMes
import br.app.divvup.core.domain.ranking.rankingGeral
import br.app.divvup.core.model.Casa
import br.app.divvup.core.model.Categoria
import br.app.divvup.core.model.Colocacao
import br.app.divvup.core.model.Evento
import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.Papel
import br.app.divvup.core.model.Temporada
import br.app.divvup.core.model.TipoDeCasa
import br.app.divvup.core.model.TipoDeEvento
import br.app.divvup.core.testing.RegraDispatcherPrincipal
import br.app.divvup.core.testing.capturarTela
import br.app.divvup.core.testing.fakes.FakeRepositorioDeCasa
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConta
import br.app.divvup.core.testing.fakes.FakeRepositorioDeRanking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class RankingTest {
    @get:Rule val principal = RegraDispatcherPrincipal()

    // Sexta, 06/11/2026, 12h em Brasília: "faltam 24 dias".
    private val agora = Instant.parse("2026-11-06T15:00:00Z")

    private fun m(uid: String, nome: String, avatar: String, mes: Int, tarefasMes: Int, total: Int, tarefas: Int, ativo: Boolean = true) =
        Membro(uid, nome, "avataaars:$avatar", Papel.MEMBER, ativo, mes, null, total, tarefasDoMes = tarefasMes, tarefasTotais = tarefas)

    /** Os moradores do desenho (Ranking, RankingGeral). */
    private val moradores = listOf(
        m("ana", "Ana", "divvup-02", 480, 11, 3_240, 81),
        m("eu", "Iane", "divvup-01", 325, 8, 2_115, 52),
        m("bruno", "Bruno", "divvup-06", 300, 7, 2_980, 74),
        m("caio", "Caio", "divvup-24", 175, 5, 1_870, 45),
        m("lia", "Lia", "divvup-12", 0, 0, 1_420, 30, ativo = false),
        m("davi", "Davi", "divvup-03", 60, 2, 940, 23),
    )

    private fun ev(id: String, tipo: TipoDeEvento, quem: String, quando: String, titulo: String? = null, cat: Categoria? = null, pts: Int? = null, atrasada: Boolean = false): Evento {
        val membro = moradores.first { it.uid == quem }
        return Evento(id, tipo, quem, membro.nome, membro.avatarId, Instant.parse(quando), titulo, cat, pts, atrasada)
    }

    /** O feed do desenho (Historico). */
    private val historico = listOf(
        ev("1", TipoDeEvento.COMPLETED, "eu", "2026-11-06T14:48:00Z", "Lavar a louça do jantar", Categoria.COZINHA, 50),
        ev("2", TipoDeEvento.ASSIGNED, "bruno", "2026-11-06T14:00:00Z", "Passear com o Thor", Categoria.PETS),
        ev("3", TipoDeEvento.AUTO_ASSIGNED, "caio", "2026-11-06T12:00:00Z", "Tirar o lixo", Categoria.LIXO),
        ev("4", TipoDeEvento.COMPLETED, "ana", "2026-11-06T00:00:00Z", "Limpar a geladeira", Categoria.COZINHA, 100),
        ev("5", TipoDeEvento.JOINED, "davi", "2026-11-05T21:00:00Z"),
        ev("6", TipoDeEvento.COMPLETED, "ana", "2026-11-05T14:00:00Z", "Regar as plantas", Categoria.PLANTAS, 12, atrasada = true),
    )

    private val casa = Casa("h1", "Rep. Cajueiro", TipoDeCasa.REPUBLICA, "2026-11", criadaEm = Instant.parse("2025-03-02T12:00:00Z"))

    private fun estado(aba: AbaDoRanking, lista: List<Membro> = moradores) = RankingEstado(
        aba = aba,
        eu = "eu",
        temporada = ResumoDaTemporada(11, 24),
        doMes = rankingDoMes(lista),
        geral = rankingGeral(lista),
        equilibrio = equilibrioDoMes(lista),
        casaDesde = casa.criadaEm,
        historico = historico,
        filtros = lista.filter { it.ativo }.sortedBy { if (it.uid == "eu") 0 else 1 },
        agora = agora,
    )

    @Test
    fun `ranking junta moradores, historico e casa e filtra o historico`() = runTest {
        val vm = RankingViewModel(FakeRepositorioDeRanking(moradores, historico), FakeRepositorioDeCasa(casa), FakeRepositorioDeConta(Sessao.Conectado("eu")))
        vm.relogio(agora)
        val e = vm.estado.first { it.doMes.isNotEmpty() }
        assertEquals(listOf("ana", "eu", "bruno", "caio", "davi"), e.doMes.map { it.membro.uid })
        // Ex-moradora fica no geral (R-29).
        assertEquals("lia", e.geral[4].membro.uid)
        assertEquals("eu", e.filtros.first().uid)
        assertEquals(casa.criadaEm, e.casaDesde)
        vm.filtrar("ana")
        assertEquals("ana", vm.estado.first { it.filtro != null }.filtro)
        vm.trocarAba(AbaDoRanking.Geral)
        assertEquals(AbaDoRanking.Geral, vm.estado.first { it.aba == AbaDoRanking.Geral }.aba)
    }

    private val temporadas = listOf(
        Temporada(
            "2026-10",
            listOf(
                Colocacao("ana", "Ana", "avataaars:divvup-02", 520),
                Colocacao("eu", "Iane", "avataaars:divvup-01", 410),
                Colocacao("bruno", "Bruno", "avataaars:divvup-06", 385),
                Colocacao("caio", "Caio", "avataaars:divvup-24", 260),
                Colocacao("lia", "Lia", "avataaars:divvup-12", 90),
            ),
            "ana",
        ),
        Temporada("2026-09", listOf(Colocacao("bruno", "Bruno", "avataaars:divvup-06", 455)), "bruno"),
        Temporada("2026-08", listOf(Colocacao("ana", "Ana", "avataaars:divvup-02", 610)), "ana"),
        Temporada("2026-07", listOf(Colocacao("caio", "Caio", "avataaars:divvup-24", 380)), "caio"),
    )

    @Test
    fun `temporadas abre a mais recente e alterna`() = runTest {
        val vm = TemporadasViewModel(FakeRepositorioDeRanking(temporadasIniciais = temporadas), FakeRepositorioDeConta(Sessao.Conectado("eu")))
        assertEquals("2026-10", vm.estado.first { !it.carregando }.aberta)
        vm.alternar(temporadas[1])
        assertEquals("2026-09", vm.estado.first { it.aberta == "2026-09" }.aberta)
        vm.alternar(temporadas[1])
        assertNull(vm.estado.first { it.aberta != "2026-09" }.temporadas.firstOrNull { it.id == vm.estado.value.aberta })
    }

    @Composable
    private fun D1(estado: RankingEstado) = NaEstrutura { RankingScreen(estado, {}, {}, {}, {}, {}) }

    @Test
    fun screenshotD1() = capturarTela("D1_ranking") { D1(estado(AbaDoRanking.Mes)) }

    @Test
    fun screenshotD1Geral() = capturarTela("D1_ranking_geral") { D1(estado(AbaDoRanking.Geral)) }

    @Test
    fun screenshotD1Historico() = capturarTela("D1_ranking_historico") { D1(estado(AbaDoRanking.Historico)) }

    @Test
    fun screenshotD1Vazio() = capturarTela("D1_ranking_vazio") {
        D1(estado(AbaDoRanking.Mes, moradores.map { it.copy(pontosDoMes = 0, tarefasDoMes = 0) }))
    }

    @Test
    fun screenshotD1Ajuda() = capturarTela("D1_ranking_ajuda") { D1(estado(AbaDoRanking.Mes).copy(ajudaAberta = true)) }

    @Test
    fun screenshotD2() = capturarTela("D2_temporadas") {
        TemporadasScreen(TemporadasEstado(temporadas, "eu", "2026-10", carregando = false), {}, {})
    }

    @Test
    fun screenshotD2Vazio() = capturarTela("D2_temporadas_vazio") {
        TemporadasScreen(TemporadasEstado(emptyList(), "eu", null, carregando = false), {}, {})
    }

    /** Barras da estrutura principal em volta de D1, como no artboard `Ranking`. */
    @Composable
    private fun NaEstrutura(conteudo: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize()) {
            DivvupTopBar {
                DivvupAvatarDaCasa(DivvupTheme.spacing.toqueMinimo - DivvupTheme.spacing.s2)
                Column(Modifier.weight(1f)) {
                    Text("rep. cajueiro", style = DivvupTheme.typography.ttl, color = DivvupTheme.colors.text)
                }
                DivvupIconButton(DivvupIcone.Sino, "Notificações", {}, contador = 3)
            }
            Box(Modifier.weight(1f)) { conteudo() }
            DivvupTabBar(
                listOf(
                    AbaNavegacao(DivvupIcone.Deck, "Deck"),
                    AbaNavegacao(DivvupIcone.Tarefas, "Minhas tarefas"),
                    AbaNavegacao(DivvupIcone.Ranking, "Ranking"),
                    AbaNavegacao(DivvupIcone.Perfil, "Perfil"),
                ),
                selecionada = 2,
                onSelecionar = {},
            )
        }
    }
}
