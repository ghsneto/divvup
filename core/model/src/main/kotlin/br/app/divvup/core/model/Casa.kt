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
    val bairro: String? = null,
    val cidade: String? = null,
    val rua: String? = null,
    /** "Anunciar vagas" ligado (`listing.enabled`, v1.1). */
    val anuncioAtivo: Boolean = false,
)

/** Papel do morador (`members/{uid}.role`, R-03). */
enum class Papel { ADMIN, MEMBER }

/** Morador da casa (`houses/{id}/members/{uid}`). */
data class Membro(
    val uid: String,
    val nome: String,
    val avatarId: String?,
    val papel: Papel,
    val ativo: Boolean,
    val pontosDoMes: Int,
    val entrouEm: java.time.Instant?,
    val pontosTotais: Int = 0,
)

/** Código de convite (`houses/{id}/invites/{codigo}`, R-06). */
data class Convite(
    val codigo: String,
    val criadoPor: String,
    val criadoPorNome: String,
    val criadoEm: java.time.Instant,
    val expiraEm: java.time.Instant,
    val usadoPorNome: String?,
    val usadoEm: java.time.Instant?,
    val revogado: Boolean,
    val revogadoEm: java.time.Instant?,
)
