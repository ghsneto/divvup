package br.app.divvup.core.data

import br.app.divvup.core.data.backend.ErroDoBackend
import br.app.divvup.core.data.casa.erroDeCasa
import br.app.divvup.core.domain.casa.ErroDeCasa
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

class ErrosDeCasaTest {
    @Test
    fun `traduz as respostas do backend e a falta de rede`() {
        assertEquals(ErroDeCasa.JaEmCasa, erroDeCasa(ErroDoBackend(409, "ja-em-casa")))
        assertEquals(ErroDeCasa.Desconhecido, erroDeCasa(ErroDoBackend(400, "nome-invalido")))
        assertEquals(ErroDeCasa.SemConexao, erroDeCasa(IOException("sem rede")))
        assertEquals(ErroDeCasa.Desconhecido, erroDeCasa(IllegalStateException("x")))
    }
}
