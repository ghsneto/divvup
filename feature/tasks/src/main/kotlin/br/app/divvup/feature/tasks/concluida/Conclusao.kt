package br.app.divvup.feature.tasks.concluida

import br.app.divvup.core.domain.tarefa.ResultadoDaConclusao
import br.app.divvup.core.model.Tarefa

/** Conclusão confirmada pelo backend, pronta para abrir C4. */
data class ConclusaoFeita(val tarefa: Tarefa, val resultado: ResultadoDaConclusao) {
    val estado: ConcluidaEstado get() = ConcluidaEstado(tarefa.titulo, tarefa.categoria, resultado)
}
