package br.app.divvup.feature.auth.recuperar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.conta.ErroDeConta
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.core.domain.conta.emailValido
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado de A5. */
data class RecuperarSenhaEstado(
    val email: String = "",
    val carregando: Boolean = false,
    val erro: ErroDeConta? = null,
    val enviado: Boolean = false,
)

@HiltViewModel
class RecuperarSenhaViewModel @Inject constructor(private val conta: RepositorioDeConta) : ViewModel() {
    private val _estado = MutableStateFlow(RecuperarSenhaEstado())
    val estado: StateFlow<RecuperarSenhaEstado> = _estado.asStateFlow()

    fun aoMudarEmail(v: String) = _estado.update { it.copy(email = v, erro = null) }

    fun enviar() {
        val atual = _estado.value
        if (atual.carregando) return
        if (!emailValido(atual.email)) {
            _estado.update { it.copy(erro = ErroDeConta.EmailInvalido) }
            return
        }
        _estado.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            when (val r = conta.enviarRecuperacao(atual.email)) {
                // A5: a confirmação é a mesma mesmo se a conta não existe.
                is ResultadoConta.Ok -> _estado.update { it.copy(carregando = false, enviado = true) }
                is ResultadoConta.Falha -> _estado.update { it.copy(carregando = false, erro = r.erro) }
            }
        }
    }

    fun usarOutroEmail() = _estado.update { it.copy(enviado = false) }
}
