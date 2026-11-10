package br.app.divvup.core.model

/** Tipos de casa de B3 (`houses.type`, SPEC §4.3). */
enum class TipoDeCasa { REPUBLICA, MORADIA_COMPARTILHADA, CASAL_FAMILIA }

/** Casa atual do usuário (`houses/{id}`), só os campos que o app usa até aqui. */
data class Casa(
    val id: String,
    val nome: String,
    val tipo: TipoDeCasa,
    /** Temporada (mês) corrente no formato `yyyy-MM`. */
    val temporada: String,
)
