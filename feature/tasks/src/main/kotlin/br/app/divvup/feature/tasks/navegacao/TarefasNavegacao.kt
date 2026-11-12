package br.app.divvup.feature.tasks.navegacao

import androidx.activity.compose.BackHandler
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import br.app.divvup.core.domain.tarefa.ResultadoDaConclusao
import br.app.divvup.core.model.Categoria
import br.app.divvup.feature.tasks.concluida.ConcluidaEstado
import br.app.divvup.feature.tasks.concluida.ConcluidaScreen
import br.app.divvup.feature.tasks.concluida.ConclusaoFeita
import br.app.divvup.feature.tasks.detalhe.DetalheScreen
import br.app.divvup.feature.tasks.detalhe.DetalheViewModel
import br.app.divvup.feature.tasks.minhas.MinhasTarefasScreen
import br.app.divvup.feature.tasks.minhas.MinhasTarefasViewModel
import kotlinx.coroutines.delay
import br.app.divvup.core.domain.casa.FUSO_DA_TEMPORADA
import br.app.divvup.feature.tasks.R
import br.app.divvup.feature.tasks.nova.NovaTarefaScreen
import br.app.divvup.feature.tasks.nova.NovaTarefaViewModel
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** C2. */
@Serializable data object RotaNovaTarefa

/** C2 em modo edição (R-12). O nome do campo é [NovaTarefaViewModel.ARG_TAREFA]. */
@Serializable data class RotaEditarTarefa(val tarefaId: String)

/** C3. O nome do campo é [DetalheViewModel.ARG_TAREFA]. */
@Serializable data class RotaDetalhe(val tarefaId: String)

/** C4: o resultado vem do backend e viaja na rota. */
@Serializable
data class RotaConcluida(
    val titulo: String,
    val categoria: String,
    val pontos: Int,
    val noPrazo: Boolean,
    val posicaoAntes: Int,
    val posicaoDepois: Int,
    val ultrapassou: String? = null,
    val sequencia: Int,
) {
    val estado: ConcluidaEstado
        get() = ConcluidaEstado(
            titulo,
            Categoria.valueOf(categoria),
            ResultadoDaConclusao(pontos, noPrazo, posicaoAntes, posicaoDepois, ultrapassou, sequencia),
        )

    companion object {
        fun de(c: ConclusaoFeita): RotaConcluida = RotaConcluida(
            titulo = c.tarefa.titulo,
            categoria = c.tarefa.categoria.name,
            pontos = c.resultado.pontos,
            noPrazo = c.resultado.noPrazo,
            posicaoAntes = c.resultado.posicaoAntes,
            posicaoDepois = c.resultado.posicaoDepois,
            ultrapassou = c.resultado.ultrapassou,
            sequencia = c.resultado.sequencia,
        )
    }
}

/** Para onde C4 leva: "Pegar outra tarefa" → deck; "Continuar" → minhas tarefas. */
class AcoesDaConclusao(val aoPegarOutra: () -> Unit, val aoContinuar: () -> Unit)

fun NavGraphBuilder.telaNovaTarefa(nav: NavController) {
    composable<RotaNovaTarefa> { NovaTarefaRota(nav) }
    composable<RotaEditarTarefa> { NovaTarefaRota(nav) }
}

/** C3 e C4. */
fun NavGraphBuilder.telasDeTarefa(nav: NavController, conclusao: AcoesDaConclusao) {
    composable<RotaDetalhe> { DetalheRota(nav) }
    composable<RotaConcluida> { entrada ->
        ConcluidaScreen(
            estado = entrada.toRoute<RotaConcluida>().estado,
            aoFechar = { nav.popBackStack() },
            aoPegarOutra = conclusao.aoPegarOutra,
            aoContinuar = conclusao.aoContinuar,
        )
    }
}

/** Conteúdo da aba "Minhas tarefas" (C5) dentro da estrutura principal. */
@Composable
fun AbaMinhasTarefas(nav: NavController, aoIrParaDeck: () -> Unit, vm: MinhasTarefasViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val conclusao by vm.conclusao.collectAsStateWithLifecycle()
    Relogio(vm::relogio)
    LaunchedEffect(conclusao) {
        conclusao?.let {
            vm.conclusaoAberta()
            nav.navigate(RotaConcluida.de(it))
        }
    }
    MinhasTarefasScreen(
        estado = estado,
        aoAbrir = { nav.navigate(RotaDetalhe(it.id)) },
        aoConcluir = vm::concluir,
        aoAlternarConcluidas = vm::alternarConcluidas,
        aoIrParaDeck = aoIrParaDeck,
        aoNovaTarefa = { nav.navigate(RotaNovaTarefa) },
    )
}

@Composable
private fun DetalheRota(nav: NavController, vm: DetalheViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val conclusao by vm.conclusao.collectAsStateWithLifecycle()
    Relogio(vm::relogio)
    LaunchedEffect(conclusao) {
        conclusao?.let {
            vm.conclusaoAberta()
            nav.navigate(RotaConcluida.de(it)) { popUpTo<RotaDetalhe> { inclusive = true } }
        }
    }
    BackHandler(enabled = estado.dialogo != null) { vm.fecharDialogo() }
    DetalheScreen(
        estado = estado,
        aoVoltar = { nav.popBackStack() },
        aoEditar = { estado.tarefa?.let { nav.navigate(RotaEditarTarefa(it.id)) } },
        aoAssumir = vm::assumir,
        aoConcluir = vm::concluir,
        aoPedirDevolucao = vm::pedirDevolucao,
        aoPedirCancelamento = vm::pedirCancelamento,
        aoConfirmar = vm::confirmar,
        aoFecharDialogo = vm::fecharDialogo,
    )
}

/** Avisa a hora a cada minuto, para prazos relativos ("em 3 h") andarem com o relógio. */
@Composable
private fun Relogio(aoTicar: (Instant) -> Unit) {
    LaunchedEffect(Unit) {
        while (true) {
            aoTicar(Instant.now())
            delay(UM_MINUTO_MS)
        }
    }
}

private const val UM_MINUTO_MS = 60_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NovaTarefaRota(nav: NavController, vm: NovaTarefaViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var escolhendoData by remember { mutableStateOf(false) }
    var escolhendoHora by remember { mutableStateOf(false) }
    val fechar = { if (vm.aoFechar()) nav.popBackStack() }
    LaunchedEffect(estado.criada) { if (estado.criada) nav.popBackStack() }
    // Excluída: o detalhe dela também sai da pilha.
    LaunchedEffect(estado.excluida) { if (estado.excluida && !nav.popBackStack<RotaDetalhe>(inclusive = true)) nav.popBackStack() }
    BackHandler(enabled = !estado.confirmandoDescarte && !estado.confirmandoExclusao) { fechar() }

    NovaTarefaScreen(
        estado = estado,
        aoFechar = { fechar() },
        aoMudarTitulo = vm::aoMudarTitulo,
        aoMudarDescricao = vm::aoMudarDescricao,
        aoEscolherCategoria = vm::aoEscolherCategoria,
        aoEscolherPeso = vm::aoEscolherPeso,
        aoEscolherAtalho = vm::aoEscolherAtalho,
        aoAbrirData = { escolhendoData = true },
        aoAbrirHora = { escolhendoHora = true },
        aoCriar = vm::criar,
        aoContinuarEditando = vm::continuarEditando,
        aoDescartar = { nav.popBackStack() },
        aoPedirExclusao = vm::pedirExclusao,
        aoConfirmarExclusao = vm::excluir,
        aoCancelarExclusao = vm::cancelarExclusao,
    )

    if (escolhendoData) {
        // C2: não aceita passado; o seletor só oferece hoje em diante.
        val hoje = LocalDate.now(FUSO_DA_TEMPORADA)
        val seletor = rememberDatePickerState(
            initialSelectedDateMillis = estado.formulario.prazo.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(hoje)
            },
        )
        DatePickerDialog(
            onDismissRequest = { escolhendoData = false },
            confirmButton = {
                TextButton({
                    seletor.selectedDateMillis?.let { vm.aoEscolherData(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    escolhendoData = false
                }) { Text(stringResource(R.string.nova_ok)) }
            },
            dismissButton = { TextButton({ escolhendoData = false }) { Text(stringResource(R.string.nova_cancelar)) } },
        ) { DatePicker(seletor) }
    }
    if (escolhendoHora) {
        val seletor = rememberTimePickerState(estado.formulario.prazo.hour, estado.formulario.prazo.minute, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { escolhendoHora = false },
            confirmButton = {
                TextButton({
                    vm.aoEscolherHora(LocalTime.of(seletor.hour, seletor.minute))
                    escolhendoHora = false
                }) { Text(stringResource(R.string.nova_ok)) }
            },
            dismissButton = { TextButton({ escolhendoHora = false }) { Text(stringResource(R.string.nova_cancelar)) } },
            title = { Text(stringResource(R.string.nova_hora)) },
        ) { TimePicker(seletor) }
    }
}
