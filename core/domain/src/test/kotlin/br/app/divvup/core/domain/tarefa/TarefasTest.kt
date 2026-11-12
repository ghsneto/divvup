package br.app.divvup.core.domain.tarefa

import br.app.divvup.core.model.Categoria
import br.app.divvup.core.model.SituacaoDaTarefa
import br.app.divvup.core.model.Tarefa
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TarefasTest {
    // Terça, 10/11/2026, 12h em Brasília.
    private val agora = Instant.parse("2026-11-10T15:00:00Z")

    private fun t(id: String, prazo: String, pontos: Int, situacao: SituacaoDaTarefa = SituacaoDaTarefa.OPEN, dono: String? = null) =
        Tarefa(id, id, null, Categoria.COZINHA, pontos, Instant.parse(prazo), situacao, dono, "u", "Ana", null)

    @Test
    fun `deck mostra so OPEN por prazo e depois maior peso (R-14)`() {
        val deck = ordenarDeck(
            listOf(
                t("c", "2026-11-12T12:00:00Z", 25),
                t("a", "2026-11-11T12:00:00Z", 50),
                t("b", "2026-11-11T12:00:00Z", 100),
                t("x", "2026-11-10T20:00:00Z", 100, SituacaoDaTarefa.ASSIGNED, "u1"),
            ),
        )
        assertEquals(listOf("b", "a", "c"), deck.map { it.id })
    }

    @Test
    fun `conta as assumidas abertas de cada um (R-20)`() {
        val lista = listOf(
            t("a", "2026-11-11T12:00:00Z", 50, SituacaoDaTarefa.ASSIGNED, "u1"),
            t("b", "2026-11-11T12:00:00Z", 50, SituacaoDaTarefa.LATE, "u1"),
            t("c", "2026-11-11T12:00:00Z", 50, SituacaoDaTarefa.DONE, "u1"),
            t("d", "2026-11-11T12:00:00Z", 50, SituacaoDaTarefa.ASSIGNED, "u2"),
        )
        assertEquals(2, abertasDe("u1", lista))
    }

    @Test
    fun `prazo falado no fuso de Brasilia`() {
        assertEquals(PrazoFalado(DiaDoPrazo.Hoje, 22, 0), prazoFalado(Instant.parse("2026-11-11T01:00:00Z"), agora))
        assertEquals(PrazoFalado(DiaDoPrazo.Amanha, 7, 0), prazoFalado(Instant.parse("2026-11-11T10:00:00Z"), agora))
        assertEquals(PrazoFalado(DiaDoPrazo.NaSemana(6), 18, 30), prazoFalado(Instant.parse("2026-11-14T21:30:00Z"), agora))
        assertEquals(PrazoFalado(DiaDoPrazo.Data(20, 11), 9, 0), prazoFalado(Instant.parse("2026-11-20T12:00:00Z"), agora))
        assertEquals(PrazoFalado(DiaDoPrazo.Ontem, 20, 0), prazoFalado(Instant.parse("2026-11-09T23:00:00Z"), agora))
        assertEquals(PrazoFalado(DiaDoPrazo.NaSemana(7), 12, 0), prazoFalado(Instant.parse("2026-11-08T15:00:00Z"), agora))
    }

    @Test
    fun `atalhos de prazo e urgencia`() {
        assertEquals("2026-11-11T20:00-03:00[America/Sao_Paulo]", prazoDoAtalho(AtalhoDePrazo.Amanha20h, agora).toString())
        assertEquals(LocalDate.of(2026, 11, 14), prazoDoAtalho(AtalhoDePrazo.Sabado12h, agora).toLocalDate())
        // Às 23h, "hoje 22h" já passou.
        assertEquals(listOf(AtalhoDePrazo.Amanha20h, AtalhoDePrazo.Sabado12h), atalhosValidos(Instant.parse("2026-11-11T02:00:00Z")))
        assertTrue(urgente(prazoDoAtalho(AtalhoDePrazo.Hoje22h, agora).toInstant(), agora))
        assertFalse(urgente(prazoDoAtalho(AtalhoDePrazo.Sabado12h, agora).toInstant(), agora))
        assertFalse(prazoValido(LocalDate.of(2026, 11, 10), LocalTime.of(11, 0), agora))
        assertTrue(prazoValido(LocalDate.of(2026, 11, 10), LocalTime.of(13, 0), agora))
    }

    @Test
    fun `deck tira as passadas ainda validas (R-14, R-17)`() {
        val lista = listOf(t("a", "2026-11-11T12:00:00Z", 50), t("b", "2026-11-12T12:00:00Z", 50), t("c", "2026-11-13T12:00:00Z", 50))
        val passes = mapOf("a" to agora.plusSeconds(3600), "b" to agora.minusSeconds(1))
        assertEquals(listOf("b", "c"), deckSemPassadas(lista, passes, agora).map { it.id })
        assertEquals(listOf("a"), passadasAbertas(lista, passes, agora).map { it.id })
    }
}
