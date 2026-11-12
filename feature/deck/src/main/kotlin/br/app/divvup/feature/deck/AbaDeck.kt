package br.app.divvup.feature.deck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.divvup.core.model.Tarefa

/** Conteúdo da aba Deck dentro da estrutura principal. */
@Composable
fun AbaDeck(aoNovaTarefa: () -> Unit, aoDetalhes: (Tarefa) -> Unit, vm: DeckViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    DeckScreen(
        estado = estado,
        // Passar (#13) e Assumir (#12) ganham ação nas próximas issues.
        aoPassar = {},
        aoAssumir = {},
        aoNovaTarefa = aoNovaTarefa,
        aoDetalhes = aoDetalhes,
        aoTentarDeNovo = vm::tentarDeNovo,
    )
}
