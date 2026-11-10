package br.app.divvup.core.domain.casa

import br.app.divvup.core.model.TipoDeCasa
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CasaTest {
    @Test
    fun `nome da casa vai de 1 a 32 caracteres sem contar espacos sobrando`() {
        assertFalse(nomeDaCasaValido("   "))
        assertTrue(nomeDaCasaValido("Rep. Cajueiro"))
        assertTrue(nomeDaCasaValido("  " + "x".repeat(32) + "  "))
        assertFalse(nomeDaCasaValido("x".repeat(33)))
        assertEquals("Rep. Cajueiro", normalizarNomeDaCasa("  Rep.   Cajueiro "))
    }

    @Test
    fun `anunciar vagas vem marcado so para republica`() {
        assertTrue(anunciaPorPadrao(TipoDeCasa.REPUBLICA))
        assertFalse(anunciaPorPadrao(TipoDeCasa.MORADIA_COMPARTILHADA))
        assertFalse(anunciaPorPadrao(TipoDeCasa.CASAL_FAMILIA))
    }

    @Test
    fun `dias que faltam na temporada`() {
        assertEquals(ResumoDaTemporada(11, 24), resumoDaTemporada(LocalDate.of(2026, 11, 6)))
        assertEquals(ResumoDaTemporada(11, 0), resumoDaTemporada(LocalDate.of(2026, 11, 30)))
        assertEquals(ResumoDaTemporada(2, 28), resumoDaTemporada(LocalDate.of(2028, 2, 1)))
    }

    @Test
    fun `posicao no mes divide empates`() {
        fun m(uid: String, pts: Int) = br.app.divvup.core.model.Membro(uid, uid, null, br.app.divvup.core.model.Papel.MEMBER, true, pts, null)
        assertEquals(mapOf("a" to 1, "b" to 2, "c" to 2, "d" to 4), posicoesNoMes(listOf(m("a", 480), m("b", 300), m("c", 300), m("d", 100))))
    }
}
