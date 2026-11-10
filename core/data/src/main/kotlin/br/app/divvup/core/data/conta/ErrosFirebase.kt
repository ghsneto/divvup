package br.app.divvup.core.data.conta

import br.app.divvup.core.domain.conta.ErroDeConta
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import java.io.IOException

/**
 * Traduz exceções do Firebase Auth para os erros mostrados nas telas (TELAS.md A3–A5).
 * Com a proteção contra enumeração de e-mails, senha errada e conta inexistente chegam como
 * `ERROR_INVALID_CREDENTIAL`: a tela mostra "senha incorreta".
 */
fun erroDeConta(erro: Throwable): ErroDeConta = when (erro) {
    is FirebaseTooManyRequestsException -> ErroDeConta.MuitasTentativas
    is FirebaseNetworkException, is IOException -> ErroDeConta.SemConexao
    is FirebaseAuthWeakPasswordException -> ErroDeConta.SenhaFraca
    is FirebaseAuthUserCollisionException -> ErroDeConta.EmailJaCadastrado
    is FirebaseAuthException -> when (erro.errorCode) {
        "ERROR_INVALID_EMAIL" -> ErroDeConta.EmailInvalido
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> ErroDeConta.SenhaIncorreta
        "ERROR_USER_NOT_FOUND", "ERROR_USER_DISABLED" -> ErroDeConta.ContaNaoEncontrada
        "ERROR_EMAIL_ALREADY_IN_USE" -> ErroDeConta.EmailJaCadastrado
        "ERROR_WEAK_PASSWORD" -> ErroDeConta.SenhaFraca
        "ERROR_TOO_MANY_REQUESTS" -> ErroDeConta.MuitasTentativas
        "ERROR_NETWORK_REQUEST_FAILED" -> ErroDeConta.SemConexao
        else -> ErroDeConta.Desconhecido
    }
    else -> ErroDeConta.Desconhecido
}
