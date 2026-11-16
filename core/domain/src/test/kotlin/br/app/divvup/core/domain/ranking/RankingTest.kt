package br.app.divvup.core.domain.ranking

import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.Papel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RankingTest {
    private fun m(uid: String, mes: Int, total: Int = mes, ativo: Boolean = true) =
        Membro(uid, uid.replaceFirstChar { it.uppercase() }, null, Papel.MEMBER, ativo, mes, null, total)

    @Test
    fun `equilibrio (R-45) casa de 1, sem pontos, empate e desigualdade`() {
        assertEquals(100, indiceDeEquilibrio(listOf(300)))
        assertEquals(100, indiceDeEquilibrio(listOf(0, 0, 0)))
        assertEquals(100, indiceDeEquilibrio(listOf(100, 100, 100)))
        assertEquals(0, indiceDeEquilibrio(listOf(300, 0, 0)))
        // Mesma conta do backend (backend/test/atividade.test.ts).
        assertEquals(76, indiceDeEquilibrio(listOf(480, 325, 300, 175)))
    }

    @Test
    fun `frase e lider do equilibrio ignoram ex-moradores`() {
        val e = equilibrioDoMes(listOf(m("ana", 480), m("voce", 325), m("bruno", 300), m("caio", 175), m("lia", 900, ativo = false)))
        assertEquals(76, e.indice)
        assertEquals(FraseDoEquilibrio.BemDividido, e.frase)
        assertEquals(FraseDoEquilibrio.QuaseEquilibrado, fraseDoEquilibrio(74))
        assertEquals("ana", e.lider?.uid)
        assertEquals(38, e.percentualDoLider)
        assertNull(equilibrioDoMes(listOf(m("ana", 0))).lider)
        assertEquals(FraseDoEquilibrio.BemDividido, fraseDoEquilibrio(75))
        assertEquals(FraseDoEquilibrio.Desequilibrado, fraseDoEquilibrio(49))
    }


    @Test
    fun `ranking do mes so com ativos, geral com ex-moradores, empate divide a posicao`() {
        val lista = listOf(m("bruno", 300, 2980), m("ana", 480, 3240), m("caio", 300, 1870), m("lia", 0, 1420, ativo = false))
        assertEquals(listOf("ana" to 1, "bruno" to 2, "caio" to 2), rankingDoMes(lista).map { it.membro.uid to it.posicao })
        assertEquals(listOf("ana", "bruno", "caio", "lia"), rankingGeral(lista).map { it.membro.uid })
    }
}
