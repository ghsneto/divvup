package br.app.divvup.feature.auth.entrar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.data.conexao.MonitorDeConexao
import br.app.divvup.core.domain.conta.ErroDeConta
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.core.domain.conta.emailValido
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado de A3. */
data class EntrarEstado(
    val email: String = "",
    val senha: String = "",
    val senhaVisivel: Boolean = false,
    val carregando: Boolean = false,
    val erro: ErroDeConta? = null,
    val online: Boolean = true,
    val entrou: Boolean = false,
) {
    /** Erro de e-mail aparece no campo de e-mail; os demais, abaixo da senha. */
    val erroNoEmail: Boolean get() = erro == ErroDeConta.EmailInvalido
    val botaoHabilitado: Boolean get() = online && erro != ErroDeConta.MuitasTentativas
}

@HiltViewModel
class EntrarViewModel @Inject constructor(
    private val conta: RepositorioDeConta,
    conexao: MonitorDeConexao,
) : ViewModel() {
    private val formulario = MutableStateFlow(EntrarEstado())

    val estado: StateFlow<EntrarEstado> = combine(formulario, conexao.online) { f, online -> f.copy(online = online) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntrarEstado())

    fun aoMudarEmail(v: String) = formulario.update { it.copy(email = v, erro = null) }

    fun aoMudarSenha(v: String) = formulario.update {
        it.copy(senha = v, erro = it.erro.takeIf { e -> e == ErroDeConta.MuitasTentativas })
    }

    fun aoAlternarSenha() = formulario.update { it.copy(senhaVisivel = !it.senhaVisivel) }

    fun entrar() {
        val atual = formulario.value
        if (atual.carregando) return
        if (!emailValido(atual.email)) {
            formulario.update { it.copy(erro = ErroDeConta.EmailInvalido) }
            return
        }
        formulario.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            when (val r = conta.entrar(atual.email, atual.senha)) {
                is ResultadoConta.Ok -> formulario.update { it.copy(carregando = false, entrou = true) }
                is ResultadoConta.Falha -> formulario.update { it.copy(carregando = false, erro = r.erro) }
            }
        }
    }
}
