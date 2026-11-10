package br.app.divvup.feature.house.membros

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import br.app.divvup.core.domain.casa.FUSO_DA_TEMPORADA
import br.app.divvup.core.model.TipoDeCasa
import br.app.divvup.feature.house.R
import java.time.Instant
import java.time.temporal.ChronoUnit

/** "mar. 2025"; "hoje"/"ontem" para quem acabou de entrar (G1 "desde ontem"). */
@Composable
fun textoDesde(entrada: Instant?, agora: Instant): String {
    if (entrada == null) return ""
    val dia = entrada.atZone(FUSO_DA_TEMPORADA).toLocalDate()
    val dias = ChronoUnit.DAYS.between(dia, agora.atZone(FUSO_DA_TEMPORADA).toLocalDate())
    return when {
        dias <= 0L -> stringResource(R.string.data_hoje)
        dias == 1L -> stringResource(R.string.data_ontem)
        else -> stringResource(R.string.data_mes_ano, stringArrayResource(R.array.meses_curtos)[dia.monthValue - 1], dia.year)
    }
}

/** Selo do tipo (`.st-n`): "república". */
@Composable
fun textoDoTipo(tipo: TipoDeCasa): String = stringResource(
    when (tipo) {
        TipoDeCasa.REPUBLICA -> R.string.tipo_republica
        TipoDeCasa.MORADIA_COMPARTILHADA -> R.string.tipo_moradia
        TipoDeCasa.CASAL_FAMILIA -> R.string.tipo_casal
    },
)

/** Pontos no formato pt-BR ("3.240"). */
fun pontos(n: Int): String = "%,d".format(java.util.Locale.forLanguageTag("pt-BR"), n)
