package br.app.divvup.core.domain.perfil

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AvataresTest {
    @Test
    fun `galeria tem 24 avatares avataaars com seed`() {
        assertEquals(24, GaleriaDeAvatares.ids.size)
        assertEquals("avataaars:divvup-01", GaleriaDeAvatares.ids.first())
        assertEquals("avataaars:divvup-24", GaleriaDeAvatares.ids.last())
    }

    @Test
    fun `escolha rapida usa os avatares do desenho`() {
        assertEquals(
            listOf("01", "02", "03", "04", "05", "07").map { "avataaars:divvup-$it" },
            GaleriaDeAvatares.escolhaRapida,
        )
    }

    @Test
    fun `valida avatar e indice`() {
        assertTrue(GaleriaDeAvatares.valido("avataaars:divvup-12"))
        assertFalse(GaleriaDeAvatares.valido("avataaars:divvup-25"))
        assertFalse(GaleriaDeAvatares.valido(null))
        assertEquals(11, GaleriaDeAvatares.indice("avataaars:divvup-12"))
        assertEquals(0, GaleriaDeAvatares.indice("outro"))
    }

    @Test
    fun `nome de exibicao tem de 1 a 24 caracteres`() {
        assertTrue(nomeDeExibicaoValido("Iane"))
        assertTrue(nomeDeExibicaoValido("x".repeat(24)))
        assertFalse(nomeDeExibicaoValido("   "))
        assertFalse(nomeDeExibicaoValido("x".repeat(25)))
    }
}
