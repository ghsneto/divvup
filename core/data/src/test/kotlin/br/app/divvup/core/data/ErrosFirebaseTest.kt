package br.app.divvup.core.data

import br.app.divvup.core.data.conta.erroDeConta
import br.app.divvup.core.domain.conta.ErroDeConta
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import org.junit.Test
import kotlin.test.assertEquals

class ErrosFirebaseTest {

    @Test
    fun `traduz os erros do Firebase Auth para as mensagens das telas`() {
        assertEquals(ErroDeConta.EmailInvalido, erroDeConta(FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "x")))
        assertEquals(ErroDeConta.SenhaIncorreta, erroDeConta(FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "x")))
        assertEquals(ErroDeConta.SenhaIncorreta, erroDeConta(FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "x")))
        assertEquals(ErroDeConta.ContaNaoEncontrada, erroDeConta(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "x")))
        assertEquals(ErroDeConta.EmailJaCadastrado, erroDeConta(FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "x")))
        assertEquals(ErroDeConta.MuitasTentativas, erroDeConta(FirebaseTooManyRequestsException("x")))
        assertEquals(ErroDeConta.SemConexao, erroDeConta(FirebaseNetworkException("x")))
        assertEquals(ErroDeConta.Desconhecido, erroDeConta(IllegalStateException("x")))
    }
}
