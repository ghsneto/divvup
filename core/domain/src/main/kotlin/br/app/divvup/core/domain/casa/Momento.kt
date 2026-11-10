package br.app.divvup.core.domain.casa

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Quando algo aconteceu, do jeito que as listas mostram ("ontem às 18h", "seg.", "10/11"). */
sealed interface Momento {
    data class Hoje(val hora: Int, val minuto: Int) : Momento
    data class Ontem(val hora: Int, val minuto: Int) : Momento

    /** Nos últimos 7 dias. 1 = segunda … 7 = domingo. */
    data class NaSemana(val diaDaSemana: Int) : Momento
    data class Data(val dia: Int, val mes: Int) : Momento
}

fun momento(instante: Instant, agora: Instant, fuso: ZoneId = FUSO_DA_TEMPORADA): Momento {
    val quando = instante.atZone(fuso)
    val dias = ChronoUnit.DAYS.between(quando.toLocalDate(), agora.atZone(fuso).toLocalDate())
    return when {
        dias <= 0L -> Momento.Hoje(quando.hour, quando.minute)
        dias == 1L -> Momento.Ontem(quando.hour, quando.minute)
        dias < SEMANA -> Momento.NaSemana(quando.dayOfWeek.value)
        else -> Momento.Data(quando.dayOfMonth, quando.monthValue)
    }
}

private const val SEMANA = 7L
