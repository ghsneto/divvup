package br.app.divvup.feature.house.convites

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import br.app.divvup.core.domain.casa.Momento
import br.app.divvup.feature.house.R
import java.time.Duration

/** R-08: share sheet nativo do Android. */
fun compartilharTexto(context: Context, texto: String) {
    val envio = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
    context.startActivity(Intent.createChooser(envio, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

suspend fun copiarTexto(clipboard: Clipboard, texto: String) {
    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("código", texto)))
}

/** "18h", "18h30". */
@Composable
private fun hora(h: Int, m: Int): String =
    if (m == 0) stringResource(R.string.momento_hora, h) else stringResource(R.string.momento_hora_min, h, m)

/** "hoje às 18h", "ontem às 18h", "seg.", "10/11". */
@Composable
fun textoDoMomento(momento: Momento): String = when (momento) {
    is Momento.Hoje -> stringResource(R.string.momento_hoje, hora(momento.hora, momento.minuto))
    is Momento.Ontem -> stringResource(R.string.momento_ontem, hora(momento.hora, momento.minuto))
    is Momento.NaSemana -> stringArrayResource(R.array.dias_semana_curtos)[momento.diaDaSemana - 1]
    is Momento.Data -> stringResource(R.string.momento_data, momento.dia, momento.mes)
}

/** "23 h 41 min" / "41 min". */
@Composable
fun textoDaDuracao(d: Duration): String {
    val horas = d.toHours().toInt()
    val minutos = (d.toMinutes() % MINUTOS_POR_HORA).toInt()
    return if (horas > 0) stringResource(R.string.convites_horas_min, horas, minutos) else stringResource(R.string.convites_min, minutos)
}

/** "23:59:41". */
fun relogio(d: Duration): String =
    "%02d:%02d:%02d".format(d.toHours(), d.toMinutes() % MINUTOS_POR_HORA, d.seconds % SEGUNDOS_POR_MINUTO)

private const val MINUTOS_POR_HORA = 60L
private const val SEGUNDOS_POR_MINUTO = 60L
