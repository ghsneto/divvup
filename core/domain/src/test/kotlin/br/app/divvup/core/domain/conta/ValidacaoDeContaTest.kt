package br.app.divvup.core.domain.conta

import br.app.divvup.core.model.Usuario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidacaoDeContaTest {

    @Test
    fun `e-mail valido e invalido`() {
        assertTrue(emailValido("iane@email.com"))
        assertTrue(emailValido("  iane@email.com.br "))
        assertFalse(emailValido("iane@email"))
        assertFalse(emailValido("iane email.com"))
        assertFalse(emailValido(""))
    }

    @Test
    fun `forca da senha segue a pontuacao do desenho`() {
        assertEquals(ForcaSenha.Vazia, forcaDaSenha(""))
        assertEquals(ForcaSenha.Fraca, forcaDaSenha("caju"))
        assertEquals(ForcaSenha.Fraca, forcaDaSenha("Caju1!")) // menos de 8: no máximo fraca
        assertEquals(ForcaSenha.Fraca, forcaDaSenha("cajueiro"))
        assertEquals(ForcaSenha.Razoavel, forcaDaSenha("cajueiro25")) // 8+ e dígito
        assertEquals(ForcaSenha.Boa, forcaDaSenha("cajueiro2025")) // 12+ conta como o 4º critério
        assertEquals(ForcaSenha.Boa, forcaDaSenha("Cajueiro25"))
        assertEquals(ForcaSenha.Forte, forcaDaSenha("Cajueiro-25"))
    }

    @Test
    fun `senhas diferentes so aparece depois de digitar a confirmacao`() {
        assertFalse(FormularioCriarConta(senha = "cajueiro2025", confirmacao = "").senhasDiferentes)
        assertTrue(FormularioCriarConta(senha = "cajueiro2025", confirmacao = "caju").senhasDiferentes)
        assertFalse(FormularioCriarConta(senha = "cajueiro2025", confirmacao = "cajueiro2025").senhasDiferentes)
    }

    @Test
    fun `criar conta exige nome, e-mail, senha razoavel, confirmacao e termos`() {
        val ok = FormularioCriarConta("Iane", "iane@email.com", "cajueiro2025", "cajueiro2025", aceitouTermos = true)
        assertTrue(ok.podeEnviar)
        assertFalse(ok.copy(nome = " ").podeEnviar)
        assertFalse(ok.copy(email = "iane@").podeEnviar)
        assertFalse(ok.copy(senha = "cajueiro", confirmacao = "cajueiro").podeEnviar) // fraca
        assertFalse(ok.copy(confirmacao = "cajueiro2026").podeEnviar)
        assertFalse(ok.copy(aceitouTermos = false).podeEnviar)
    }

    @Test
    fun `destino inicial da splash`() {
        val semCasa = Usuario("u", "Iane", "i@e.com", null, casaAtualId = null)
        assertEquals(DestinoInicial.BoasVindas, destinoInicial(Sessao.Desconectado, null))
        assertEquals(DestinoInicial.Onboarding, destinoInicial(Sessao.Conectado("u"), null))
        assertEquals(DestinoInicial.Onboarding, destinoInicial(Sessao.Conectado("u"), semCasa))
        assertEquals(DestinoInicial.Deck, destinoInicial(Sessao.Conectado("u"), semCasa.copy(casaAtualId = "h1")))
    }
}
