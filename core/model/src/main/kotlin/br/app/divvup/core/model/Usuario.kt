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
    val melhorSequencia: Int = 0,
    /** Último dia com conclusão (`streak.lastDay`, fuso de Brasília). */
    val ultimoDiaDaSequencia: java.time.LocalDate? = null,
    val pontosTotais: Int = 0,
    val tarefasFeitas: Int = 0,
    /** Medalhas da conta (R-30): quantas vezes e quando ganhou a primeira. */
    val medalhas: Map<Medalha, Conquista> = emptyMap(),
    /** Conta com senha (E2 "Alterar senha"); `false` para quem entrou só com o Google. */
    val temSenha: Boolean = true,
)

/** Medalha ganha: vezes e a primeira vez. */
data class Conquista(val vezes: Int, val primeiraVez: java.time.Instant?)
