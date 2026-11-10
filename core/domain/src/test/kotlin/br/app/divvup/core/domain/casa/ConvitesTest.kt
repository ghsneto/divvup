package br.app.divvup.core.domain.casa

import br.app.divvup.core.model.Convite
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ConvitesTest {
    private val agora = Instant.parse("2026-11-10T12:00:00Z")
    private val ativo = Convite("K7PQ2X", "u1", "Iane", agora, agora.plus(VALIDADE_CONVITE), null, null, false, null)

    @Test
    fun `normaliza o codigo digitado ou colado`() {
        assertEquals("K7PQ2X", normalizarCodigo("k7p-q2x"))
        assertEquals("K7PQ2X", normalizarCodigo(" K7P Q2X 99"))
        // O, 0, I, 1 e L não existem no alfabeto (R-06).
        assertEquals("ABC", normalizarCodigo("A0B1CLOI"))
    }

    @Test
    fun `situacao do convite`() {
        assertEquals(SituacaoDoConvite.Ativo, situacao(ativo, agora))
        assertEquals(SituacaoDoConvite.Expirado, situacao(ativo, agora.plus(VALIDADE_CONVITE)))
        assertEquals(SituacaoDoConvite.Revogado, situacao(ativo.copy(revogado = true), agora))
        assertEquals(SituacaoDoConvite.Usado, situacao(ativo.copy(usadoPorNome = "Davi", usadoEm = agora), agora.plus(Duration.ofDays(3))))
    }

    @Test
    fun `tempo restante nunca e negativo`() {
        assertEquals(Duration.ofHours(24), tempoRestante(ativo, agora))
        assertEquals(Duration.ZERO, tempoRestante(ativo, agora.plus(Duration.ofDays(2))))
    }
}

class MomentoTest {
    private val agora = Instant.parse("2026-11-10T15:00:00Z") // terça, 12h em Brasília

    @Test
    fun `classifica o momento no fuso de Brasilia`() {
        assertEquals(Momento.Hoje(9, 30), momento(Instant.parse("2026-11-10T12:30:00Z"), agora))
        assertEquals(Momento.Ontem(18, 0), momento(Instant.parse("2026-11-09T21:00:00Z"), agora))
        // 02h UTC do dia 10 ainda é dia 9 em Brasília.
        assertEquals(Momento.Ontem(23, 0), momento(Instant.parse("2026-11-10T02:00:00Z"), agora))
        assertEquals(Momento.NaSemana(7), momento(Instant.parse("2026-11-08T15:00:00Z"), agora))
        assertEquals(Momento.Data(1, 11), momento(Instant.parse("2026-11-01T15:00:00Z"), agora))
    }
}
