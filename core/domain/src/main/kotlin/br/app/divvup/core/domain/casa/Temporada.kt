package br.app.divvup.core.domain.casa

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Fuso das temporadas: o mês vira no horário de Brasília (igual ao backend). */
val FUSO_DA_TEMPORADA: ZoneId = ZoneId.of("America/Sao_Paulo")

/** Resumo da temporada (mês) na barra superior de C1: "novembro · faltam 24 dias". */
data class ResumoDaTemporada(
    /** 1 = janeiro … 12 = dezembro. */
    val mes: Int,
    /** Dias que faltam até o fim do mês, sem contar hoje (0 no último dia). */
    val diasRestantes: Int,
)

fun resumoDaTemporada(hoje: LocalDate): ResumoDaTemporada =
    ResumoDaTemporada(hoje.monthValue, YearMonth.from(hoje).lengthOfMonth() - hoje.dayOfMonth)
