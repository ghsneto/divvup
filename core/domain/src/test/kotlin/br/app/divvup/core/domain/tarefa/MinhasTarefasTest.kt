package br.app.divvup.core.domain.tarefa

import br.app.divvup.core.model.Categoria
import br.app.divvup.core.model.SituacaoDaTarefa
import br.app.divvup.core.model.Tarefa
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MinhasTarefasTest {
    // Terça, 10/11/2026, 12h em Brasília.
    private val agora = Instant.parse("2026-11-10T15:00:00Z")

    private fun t(id: String, prazo: String, sit: SituacaoDaTarefa, dono: String? = "eu", por: String = "ana", concluida: String? = null, ganhos: Int? = null) =
        Tarefa(id, id, null, Categoria.COZINHA, 50, Instant.parse(prazo), sit, dono, por, "Ana", null, concluidaEm = concluida?.let(Instant::parse), pontosGanhos = ganhos)

    @Test
    fun `pontos da conclusao (R-22)`() {
        val prazo = Instant.parse("2026-11-10T22:00:00Z")
        assertEquals(50, pontosDaConclusao(50, prazo, prazo))
        assertEquals(25, pontosDaConclusao(50, prazo, prazo.plusSeconds(1)))
        assertEquals(12, pontosDaConclusao(25, prazo, prazo.plusSeconds(3600)))
    }

    @Test
    fun `situacao para quem ve (C3)`() {
        assertEquals(SituacaoParaMim.Aberta, situacaoParaMim(t("a", "2026-11-12T12:00:00Z", SituacaoDaTarefa.OPEN, null), "eu", agora))
        assertEquals(SituacaoParaMim.Sua, situacaoParaMim(t("a", "2026-11-12T12:00:00Z", SituacaoDaTarefa.ASSIGNED), "eu", agora))
        assertEquals(SituacaoParaMim.SuaPerto, situacaoParaMim(t("a", "2026-11-11T12:00:00Z", SituacaoDaTarefa.ASSIGNED), "eu", agora))
        assertEquals(SituacaoParaMim.Atrasada, situacaoParaMim(t("a", "2026-11-10T12:00:00Z", SituacaoDaTarefa.ASSIGNED), "eu", agora))
        assertEquals(SituacaoParaMim.DeOutra, situacaoParaMim(t("a", "2026-11-12T12:00:00Z", SituacaoDaTarefa.ASSIGNED, "davi"), "eu", agora))
        assertEquals(SituacaoParaMim.Concluida, situacaoParaMim(t("a", "2026-11-12T12:00:00Z", SituacaoDaTarefa.DONE), "eu", agora))
    }

    @Test
    fun `editar e so do autor ou admin, sem dono (R-12)`() {
        val aberta = t("a", "2026-11-12T12:00:00Z", SituacaoDaTarefa.OPEN, null, por = "eu")
        assertTrue(podeEditar(aberta, "eu", souAdmin = false))
        assertFalse(podeEditar(aberta, "bruno", souAdmin = false))
        assertTrue(podeEditar(aberta, "bruno", souAdmin = true))
        assertFalse(podeEditar(aberta.copy(situacao = SituacaoDaTarefa.ASSIGNED, donoId = "eu"), "eu", souAdmin = true))
    }

    @Test
    fun `secoes de minhas tarefas (C5)`() {
        val s = secoesDasMinhas(
            listOf(
                t("atrasada", "2026-11-09T23:00:00Z", SituacaoDaTarefa.ASSIGNED),
                t("hoje", "2026-11-11T02:00:00Z", SituacaoDaTarefa.ASSIGNED),
                t("proxima", "2026-11-14T21:00:00Z", SituacaoDaTarefa.ASSIGNED),
                t("feita", "2026-11-09T12:00:00Z", SituacaoDaTarefa.DONE, concluida = "2026-11-09T12:00:00Z", ganhos = 50),
                t("antiga", "2026-10-01T12:00:00Z", SituacaoDaTarefa.DONE, concluida = "2026-10-01T12:00:00Z", ganhos = 25),
            ),
            agora,
        )
        assertEquals(listOf("atrasada"), s.atrasadas.map { it.id })
        assertEquals(listOf("hoje"), s.hoje.map { it.id })
        assertEquals(listOf("proxima"), s.proximas.map { it.id })
        assertEquals(listOf("feita"), s.concluidas.map { it.id })
        assertEquals(3, s.abertas)
        assertEquals(50, s.pontosConcluidas)
    }
}
