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

fun NavGraphBuilder.telaNovaTarefa(nav: NavController) {
    composable<RotaNovaTarefa> { NovaTarefaRota(nav) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NovaTarefaRota(nav: NavController, vm: NovaTarefaViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var escolhendoData by remember { mutableStateOf(false) }
    var escolhendoHora by remember { mutableStateOf(false) }
    val fechar = { if (vm.aoFechar()) nav.popBackStack() }
    LaunchedEffect(estado.criada) { if (estado.criada) nav.popBackStack() }
    BackHandler(enabled = !estado.confirmandoDescarte) { fechar() }

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
