package br.app.divvup.feature.deck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.divvup.core.model.Tarefa

/** Conteúdo da aba Deck dentro da estrutura principal. */
@Composable
fun AbaDeck(aoNovaTarefa: () -> Unit, aoMinhasTarefas: () -> Unit, aoDetalhes: (Tarefa) -> Unit, vm: DeckViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    DeckScreen(
        estado = estado,
        aoPassar = vm::passar,
        aoAssumir = vm::assumir,
        aoDesfazer = vm::desfazer,
        aoRever = vm::rever,
        aoNovaTarefa = aoNovaTarefa,
        aoMinhasTarefas = aoMinhasTarefas,
        aoDetalhes = aoDetalhes,
        aoTentarDeNovo = vm::tentarDeNovo,
    )
}
