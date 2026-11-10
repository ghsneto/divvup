package br.app.divvup.core.data

import br.app.divvup.core.data.backend.ErroDoBackend
import br.app.divvup.core.data.casa.erroDeCasa
import br.app.divvup.core.data.casa.erroDeConvite
import br.app.divvup.core.domain.casa.ErroDeConvite
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

class ErrosDeConviteTest {
    @Test
    fun `traduz os erros de B4`() {
        assertEquals(ErroDeConvite.Invalido, erroDeConvite(ErroDoBackend(404, "codigo-invalido")))
        assertEquals(ErroDeConvite.Expirado, erroDeConvite(ErroDoBackend(410, "codigo-expirado")))
        assertEquals(ErroDeConvite.Usado, erroDeConvite(ErroDoBackend(409, "codigo-usado")))
        assertEquals(ErroDeConvite.JaEmCasa, erroDeConvite(ErroDoBackend(409, "ja-em-casa")))
        assertEquals(ErroDeConvite.NaoAdmin, erroDeConvite(ErroDoBackend(403, "nao-admin")))
        assertEquals(ErroDeConvite.SemConexao, erroDeConvite(IOException("x")))
    }
}
