package br.app.divvup.core.data

import br.app.divvup.core.data.backend.ErroDoBackend
import br.app.divvup.core.data.tarefa.erroDeTarefa
import br.app.divvup.core.domain.tarefa.ErroDeTarefa
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

class ErrosDeTarefaTest {
    @Test
    fun `traduz as falhas de criar tarefa`() {
        assertEquals(ErroDeTarefa.PrazoNoPassado, erroDeTarefa(ErroDoBackend(400, "prazo-no-passado")))
        assertEquals(ErroDeTarefa.Desconhecido, erroDeTarefa(ErroDoBackend(400, "titulo-invalido")))
        assertEquals(ErroDeTarefa.SemConexao, erroDeTarefa(IOException("x")))
        assertEquals(ErroDeTarefa.JaAssumida("Davi"), erroDeTarefa(ErroDoBackend(409, "ja-assumida", "Davi")))
        assertEquals(ErroDeTarefa.Limite, erroDeTarefa(ErroDoBackend(409, "limite")))
        assertEquals(ErroDeTarefa.NaoDaParaDesfazer, erroDeTarefa(ErroDoBackend(409, "nao-da-para-desfazer")))
    }
}
