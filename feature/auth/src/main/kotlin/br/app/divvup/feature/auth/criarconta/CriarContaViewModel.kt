package br.app.divvup.feature.auth.criarconta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.conta.ErroDeConta
import br.app.divvup.core.domain.conta.FormularioCriarConta
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.ResultadoConta
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado de A4. */
data class CriarContaEstado(
    val formulario: FormularioCriarConta = FormularioCriarConta(),
    val carregando: Boolean = false,
    val erro: ErroDeConta? = null,
    val criou: Boolean = false,
) {
    val emailDuplicado: Boolean get() = erro == ErroDeConta.EmailJaCadastrado
    val botaoHabilitado: Boolean get() = formulario.podeEnviar && !emailDuplicado
}

@HiltViewModel
class CriarContaViewModel @Inject constructor(private val conta: RepositorioDeConta) : ViewModel() {
    private val _estado = MutableStateFlow(CriarContaEstado())
    val estado: StateFlow<CriarContaEstado> = _estado.asStateFlow()

    private fun editar(bloco: FormularioCriarConta.() -> FormularioCriarConta, limparErroEmail: Boolean = false) =
        _estado.update {
            it.copy(formulario = it.formulario.bloco(), erro = if (limparErroEmail || it.erro != ErroDeConta.EmailJaCadastrado) null else it.erro)
        }

    fun aoMudarNome(v: String) = editar({ copy(nome = v) })
    fun aoMudarEmail(v: String) = editar({ copy(email = v) }, limparErroEmail = true)
    fun aoMudarSenha(v: String) = editar({ copy(senha = v) })
    fun aoMudarConfirmacao(v: String) = editar({ copy(confirmacao = v) })
    fun aoMudarTermos(v: Boolean) = editar({ copy(aceitouTermos = v) })

    fun criar() {
        val atual = _estado.value
        if (atual.carregando || !atual.botaoHabilitado) return
        _estado.update { it.copy(carregando = true, erro = null) }
        val f = atual.formulario
        viewModelScope.launch {
            when (val r = conta.criarConta(f.nome.trim(), f.email, f.senha)) {
                is ResultadoConta.Ok -> _estado.update { it.copy(carregando = false, criou = true) }
                is ResultadoConta.Falha -> _estado.update { it.copy(carregando = false, erro = r.erro) }
            }
        }
    }
}
