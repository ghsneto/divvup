package br.app.divvup.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import br.app.divvup.core.designsystem.R
import br.app.divvup.core.domain.tarefa.DiaDoPrazo
import br.app.divvup.core.domain.tarefa.PrazoFalado

/** "22h" / "18h30". */
@Composable
fun textoDaHora(hora: Int, minuto: Int): String =
    if (minuto == 0) stringResource(R.string.prazo_hora, hora) else stringResource(R.string.prazo_hora_min, hora, minuto)

/** Dia do prazo: "hoje", "amanhã", "sáb." (ou "sábado" com [diaLongo]), "20 nov". */
@Composable
fun textoDoDia(dia: DiaDoPrazo, diaLongo: Boolean = false): String = when (dia) {
    DiaDoPrazo.Hoje -> stringResource(R.string.prazo_hoje)
    DiaDoPrazo.Amanha -> stringResource(R.string.prazo_amanha)
    is DiaDoPrazo.NaSemana ->
        stringArrayResource(if (diaLongo) R.array.prazo_dias_semana else R.array.prazo_dias_curtos)[dia.diaDaSemana - 1]
    is DiaDoPrazo.Data -> stringResource(R.string.prazo_data, dia.dia, stringArrayResource(R.array.prazo_meses)[dia.mes - 1])
}

/** Cartão do deck: "vence hoje às 22h". */
@Composable
fun textoDoVencimento(p: PrazoFalado): String = stringResource(R.string.prazo_vence, textoDoDia(p.dia), textoDaHora(p.hora, p.minuto))

/** Atalho e prévia de C2: "amanhã, 20h", "sábado, 12h". */
@Composable
fun textoDoAtalho(p: PrazoFalado): String = stringResource(R.string.prazo_atalho, textoDoDia(p.dia, diaLongo = true), textoDaHora(p.hora, p.minuto))
