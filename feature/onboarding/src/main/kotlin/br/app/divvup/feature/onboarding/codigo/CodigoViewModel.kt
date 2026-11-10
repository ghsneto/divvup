package br.app.divvup.feature.onboarding.codigo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.casa.ErroDeConvite
import br.app.divvup.core.domain.casa.RepositorioDeConvites
import br.app.divvup.core.domain.casa.ResultadoConvite
import br.app.divvup.core.domain.casa.TAMANHO_CODIGO
import br.app.divvup.core.domain.casa.normalizarCodigo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado de B4. */
data class CodigoEstado(
    val codigo: String = "",
    val entrando: Boolean = false,
    val erro: ErroDeConvite? = null,
    val entrou: Boolean = false,
) {
    val completo: Boolean get() = codigo.length == TAMANHO_CODIGO
}

@HiltViewModel
class CodigoViewModel @Inject constructor(private val convites: RepositorioDeConvites) : ViewModel() {
    private val _estado = MutableStateFlow(CodigoEstado())
    val estado: StateFlow<CodigoEstado> = _estado.asStateFlow()

    /** Digitado ou colado: só o alfabeto do código, em maiúsculas (R-06). */
    fun aoDigitar(v: String) = _estado.update { it.copy(codigo = normalizarCodigo(v), erro = null) }

    fun entrar() {
        val atual = _estado.value
        if (!atual.completo || atual.entrando) return
        _estado.update { it.copy(entrando = true, erro = null) }
        viewModelScope.launch {
            when (val r = convites.entrar(atual.codigo)) {
                is ResultadoConvite.Ok -> _estado.update { it.copy(entrando = false, entrou = true) }
                is ResultadoConvite.Falha -> _estado.update { it.copy(entrando = false, erro = r.erro) }
            }
        }
    }

    fun aoNavegar() = _estado.update { it.copy(entrou = false) }
}
