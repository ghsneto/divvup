package br.app.divvup.core.domain.conta

/** Força da senha, como no indicador de A4 (4 barras + rótulo). */
enum class ForcaSenha(val nivel: Int) {
    Vazia(0),
    Fraca(1),
    Razoavel(2),
    Boa(3),
    Forte(4),
}

/** Mínimo de caracteres da senha (TELAS.md A4). */
const val TAMANHO_MINIMO_SENHA = 8

private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")

/** E-mail com formato plausível (o Firebase Auth faz a validação definitiva). */
fun emailValido(email: String): Boolean = EMAIL.matches(email.trim())

/**
 * Força da senha, mesma pontuação do desenho (CriarConta): 1 ponto por ter 8+ caracteres,
 * maiúscula e minúscula, dígito, e símbolo ou 12+ caracteres. Abaixo de 8 caracteres, no máximo fraca.
 */
fun forcaDaSenha(senha: String): ForcaSenha {
    if (senha.isEmpty()) return ForcaSenha.Vazia
    var pontos = 0
    if (senha.length >= TAMANHO_MINIMO_SENHA) pontos++
    if (senha.any { it.isUpperCase() } && senha.any { it.isLowerCase() }) pontos++
    if (senha.any { it.isDigit() }) pontos++
    if (senha.any { !it.isLetterOrDigit() } || senha.length >= 12) pontos++
    if (senha.length < TAMANHO_MINIMO_SENHA) pontos = minOf(pontos, 1)
    return when (pontos) {
        0, 1 -> ForcaSenha.Fraca
        2 -> ForcaSenha.Razoavel
        3 -> ForcaSenha.Boa
        else -> ForcaSenha.Forte
    }
}

/** Formulário de A4. */
data class FormularioCriarConta(
    val nome: String = "",
    val email: String = "",
    val senha: String = "",
    val confirmacao: String = "",
    val aceitouTermos: Boolean = false,
) {
    val forca: ForcaSenha get() = forcaDaSenha(senha)

    /** "As senhas não batem": só depois que a confirmação começou a ser digitada. */
    val senhasDiferentes: Boolean get() = confirmacao.isNotEmpty() && confirmacao != senha

    /** Habilita "Criar conta" (regras do desenho + mínimo de 8 caracteres e aceite dos termos). */
    val podeEnviar: Boolean
        get() = nome.isNotBlank() &&
            emailValido(email) &&
            senha.length >= TAMANHO_MINIMO_SENHA &&
            forca.nivel >= ForcaSenha.Razoavel.nivel &&
            confirmacao == senha &&
            aceitouTermos
}
