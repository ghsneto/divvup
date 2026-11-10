package br.app.divvup.core.domain.conta

import br.app.divvup.core.model.Usuario
import kotlinx.coroutines.flow.Flow

/** Falhas de conta, cada uma com sua mensagem na tela (TELAS.md A3–A5). */
sealed interface ErroDeConta {
    data object EmailInvalido : ErroDeConta
    data object SenhaIncorreta : ErroDeConta
    data object ContaNaoEncontrada : ErroDeConta
    data object MuitasTentativas : ErroDeConta
    data object EmailJaCadastrado : ErroDeConta
    data object SenhaFraca : ErroDeConta
    data object SemConexao : ErroDeConta
    data object GoogleIndisponivel : ErroDeConta
    data object Desconhecido : ErroDeConta
}

/** Resultado de uma operação de conta. */
sealed interface ResultadoConta<out T> {
    data class Ok<T>(val valor: T) : ResultadoConta<T>
    data class Falha(val erro: ErroDeConta) : ResultadoConta<Nothing>
}

/** Sessão do Firebase Auth. */
sealed interface Sessao {
    data object Desconectado : Sessao
    data class Conectado(val uid: String) : Sessao
}

/** Conta do usuário (login, cadastro, recuperação). Implementação em `:core:data`. */
interface RepositorioDeConta {
    val sessao: Flow<Sessao>

    suspend fun entrar(email: String, senha: String): ResultadoConta<Unit>

    /** Cria a conta no Auth e o perfil `users/{uid}` pelo backend (o cliente não grava pontos). */
    suspend fun criarConta(nome: String, email: String, senha: String): ResultadoConta<Unit>

    /**
     * Entra (ou cria a conta) com o ID token da conta Google (R-01). No primeiro acesso, o perfil é
     * criado pelo backend com nome e foto do Google.
     */
    suspend fun entrarComGoogle(idTokenGoogle: String): ResultadoConta<Unit>

    /** Sempre "sucesso" para e-mail válido: não revela se a conta existe (TELAS.md A5). */
    suspend fun enviarRecuperacao(email: String): ResultadoConta<Unit>

    suspend fun sair()

    /** Perfil do usuário conectado; `null` se ainda não existe. */
    suspend fun perfil(uid: String): ResultadoConta<Usuario?>
}

/** Para onde a Splash (A1) leva. */
enum class DestinoInicial { BoasVindas, Onboarding, Deck }

/** A1: sem sessão → A2; com sessão e sem casa (ou sem perfil) → onboarding (B1); com casa → deck (C1). */
fun destinoInicial(sessao: Sessao, perfil: Usuario?): DestinoInicial = when {
    sessao is Sessao.Desconectado -> DestinoInicial.BoasVindas
    perfil?.casaAtualId == null -> DestinoInicial.Onboarding
    else -> DestinoInicial.Deck
}
