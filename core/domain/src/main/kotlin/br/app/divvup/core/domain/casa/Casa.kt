package br.app.divvup.core.domain.casa

import br.app.divvup.core.model.Casa
import br.app.divvup.core.model.TipoDeCasa
import kotlinx.coroutines.flow.Flow

/** Igual ao `maxlength` do nome da casa em B3 (e ao limite do backend). */
const val NOME_CASA_MAXIMO = 32

/** Nome da casa sem espaços sobrando. */
fun normalizarNomeDaCasa(nome: String): String = nome.trim().replace(Regex("\\s+"), " ")

/** B3: nome obrigatório, até [NOME_CASA_MAXIMO] caracteres. */
fun nomeDaCasaValido(nome: String): Boolean = normalizarNomeDaCasa(nome).length in 1..NOME_CASA_MAXIMO

/** "Anunciar vagas" (v1.1, R-38) só existe para república; vem marcado por padrão (TELAS.md B3). */
fun anunciaPorPadrao(tipo: TipoDeCasa): Boolean = tipo == TipoDeCasa.REPUBLICA

/** Falhas das operações de casa, cada uma com sua mensagem na tela. */
sealed interface ErroDeCasa {
    /** R-02: já está numa casa. */
    data object JaEmCasa : ErroDeCasa
    data object SemConexao : ErroDeCasa
    data object Desconhecido : ErroDeCasa
}

sealed interface ResultadoCasa<out T> {
    data class Ok<T>(val valor: T) : ResultadoCasa<T>
    data class Falha(val erro: ErroDeCasa) : ResultadoCasa<Nothing>
}

/** Casa do usuário. Escritas passam pelo backend (SPEC §4.2); leituras vêm do Firestore. */
interface RepositorioDeCasa {
    /** Casa atual do usuário conectado; `null` sem casa. Atualiza em tempo real. */
    val casaAtual: Flow<Casa?>

    /** B3: cria a casa com o usuário como `ADMIN` (R-02, R-03). Devolve o id. */
    suspend fun criarCasa(nome: String, tipo: TipoDeCasa, anunciar: Boolean): ResultadoCasa<String>
}
