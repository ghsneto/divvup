package br.app.divvup.core.model

/** Perfil do usuário (`users/{uid}`, SPEC §4.3), só os campos que o app usa até aqui. */
data class Usuario(
    val uid: String,
    val nome: String,
    val email: String?,
    val avatarId: String?,
    val casaAtualId: String?,
    /** Sequência diária atual (`streak.current`, R-31). */
    val sequencia: Int = 0,
)
