package br.app.divvup.core.domain.casa

import br.app.divvup.core.model.Convite
import br.app.divvup.core.model.Membro
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant

/** R-06: alfabeto sem ambíguos (sem O/0/I/1/L) e 6 caracteres. */
const val ALFABETO_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
const val TAMANHO_CODIGO = 6

/** R-06: validade de 24 h. */
val VALIDADE_CONVITE: Duration = Duration.ofHours(24)

/** B4: maiúsculas, só caracteres do alfabeto, até 6 (colar "k7p-q2x" vira "K7PQ2X"). */
fun normalizarCodigo(digitado: String): String =
    digitado.uppercase().filter { it in ALFABETO_CODIGO }.take(TAMANHO_CODIGO)

/** Situação do código na lista de G2. */
enum class SituacaoDoConvite { Ativo, Usado, Expirado, Revogado }

fun situacao(convite: Convite, agora: Instant): SituacaoDoConvite = when {
    convite.usadoPorNome != null || convite.usadoEm != null -> SituacaoDoConvite.Usado
    convite.revogado -> SituacaoDoConvite.Revogado
    !agora.isBefore(convite.expiraEm) -> SituacaoDoConvite.Expirado
    else -> SituacaoDoConvite.Ativo
}

/** Tempo que falta para expirar (zero se já expirou). */
fun tempoRestante(convite: Convite, agora: Instant): Duration =
    Duration.between(agora, convite.expiraEm).takeIf { !it.isNegative } ?: Duration.ZERO

/** Falhas de B4 (TELAS.md: inválido, expirado, usado, já está numa casa). */
sealed interface ErroDeConvite {
    data object Invalido : ErroDeConvite
    data object Expirado : ErroDeConvite
    data object Usado : ErroDeConvite
    data object JaEmCasa : ErroDeConvite
    data object NaoAdmin : ErroDeConvite
    data object SemConexao : ErroDeConvite
    data object Desconhecido : ErroDeConvite
}

sealed interface ResultadoConvite<out T> {
    data class Ok<T>(val valor: T) : ResultadoConvite<T>
    data class Falha(val erro: ErroDeConvite) : ResultadoConvite<Nothing>
}

/** Convites e moradores da casa atual. Escritas pelo backend; leituras pelo Firestore. */
interface RepositorioDeConvites {
    /** Códigos da casa atual, do mais novo para o mais antigo (G2). */
    val convites: Flow<List<Convite>>

    /** Moradores ativos da casa atual, por ordem de entrada (B5, G1). */
    val membros: Flow<List<Membro>>

    /** B8/G2: gera um código (só admin). */
    suspend fun criar(): ResultadoConvite<Convite>

    suspend fun revogar(codigo: String): ResultadoConvite<Unit>

    /** B4: entra na casa do código. Devolve o id da casa. */
    suspend fun entrar(codigo: String): ResultadoConvite<String>
}
