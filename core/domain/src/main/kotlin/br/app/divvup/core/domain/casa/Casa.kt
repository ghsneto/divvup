package br.app.divvup.core.domain.casa

import br.app.divvup.core.model.Casa
import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.Papel
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

    /** Só admin faz isso (R-03). */
    data object NaoAdmin : ErroDeCasa

    /** Tirar o papel do único admin deixaria a casa sem admin. */
    data object UltimoAdmin : ErroDeCasa
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

    /** G3: torna admin ou tira o papel (R-03). */
    suspend fun alterarPapel(uid: String, papel: Papel): ResultadoCasa<Unit>

    /** G3: remove outro morador da casa. */
    suspend fun removerMembro(uid: String): ResultadoCasa<Unit>

    /** G1: sai da casa; se era o último admin, o ativo mais antigo assume (R-04). */
    suspend fun sair(): ResultadoCasa<Unit>

    /** G4: nome, tipo e endereço sem número. */
    suspend fun editar(dados: DadosDaCasa): ResultadoCasa<Unit>
}

/** Campos editáveis em G4. */
data class DadosDaCasa(
    val nome: String,
    val tipo: TipoDeCasa,
    val cidade: String = "",
    val bairro: String = "",
    val rua: String = "",
)

/** Posição de cada morador no mês (1 = mais pontos); empates dividem a posição. */
fun posicoesNoMes(membros: List<Membro>): Map<String, Int> =
    membros.associate { m -> m.uid to 1 + membros.count { it.pontosDoMes > m.pontosDoMes } }
