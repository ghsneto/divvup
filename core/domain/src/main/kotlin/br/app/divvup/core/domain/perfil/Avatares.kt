package br.app.divvup.core.domain.perfil

/**
 * Galeria de avatares (R-32): 24 DiceBear avataaars empacotados no app. O `avatarId` guarda
 * estilo + seed (`avataaars:divvup-NN`); as opções de cada um estão em
 * docs/design/avatares-dicebear/catalogo.json, para o editor de avatar futuro.
 */
object GaleriaDeAvatares {
    const val QUANTIDADE = 24

    val ids: List<String> = (1..QUANTIDADE).map { "avataaars:divvup-%02d".format(it) }

    /** Escolha rápida de B1: avatares 1, 2, 3, 4, 5 e 7 da galeria (como no desenho). */
    val escolhaRapida: List<String> = listOf(1, 2, 3, 4, 5, 7).map { ids[it - 1] }

    /** Avatar de quem ainda não escolheu (o primeiro, como no desenho). */
    val padrao: String = ids.first()

    fun valido(avatarId: String?): Boolean = avatarId in ids

    /** Posição na galeria (0..23); o padrão para ids desconhecidos ou nulos. */
    fun indice(avatarId: String?): Int = ids.indexOf(avatarId).takeIf { it >= 0 } ?: 0
}

/** Máximo do nome de exibição (B1: `maxlength="24"`). */
const val NOME_EXIBICAO_MAXIMO = 24

/** Nome de exibição válido: 1 a 24 caracteres sem contar espaços nas pontas. */
fun nomeDeExibicaoValido(nome: String): Boolean = nome.trim().length in 1..NOME_EXIBICAO_MAXIMO
