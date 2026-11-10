package br.app.divvup.feature.onboarding.nome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.domain.perfil.GaleriaDeAvatares
import br.app.divvup.core.domain.perfil.NOME_EXIBICAO_MAXIMO
import br.app.divvup.core.domain.perfil.nomeDeExibicaoValido
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado de B1. */
data class NomeEstado(
    val nome: String = "",
    val avatarId: String = GaleriaDeAvatares.padrao,
    val salvando: Boolean = false,
    val erro: Boolean = false,
    val concluido: Boolean = false,
) {
    val podeContinuar: Boolean get() = nomeDeExibicaoValido(nome) && !salvando
}

@HiltViewModel
class NomeViewModel @Inject constructor(private val conta: RepositorioDeConta) : ViewModel() {
    private val _estado = MutableStateFlow(NomeEstado())
    val estado: StateFlow<NomeEstado> = _estado.asStateFlow()
    private var uid: String? = null

    init {
        // Nome pré-preenchido do cadastro ou do Google (TELAS.md B1).
        viewModelScope.launch {
            uid = (conta.sessao.first() as? Sessao.Conectado)?.uid
            val perfil = uid?.let { (conta.perfil(it) as? ResultadoConta.Ok)?.valor }
            if (perfil != null) {
                _estado.update {
                    it.copy(
                        nome = perfil.nome.take(NOME_EXIBICAO_MAXIMO),
                        avatarId = perfil.avatarId?.takeIf(GaleriaDeAvatares::valido) ?: it.avatarId,
                    )
                }
            }
        }
    }

    fun aoMudarNome(v: String) = _estado.update { it.copy(nome = v.take(NOME_EXIBICAO_MAXIMO), erro = false) }

    fun aoEscolherAvatar(id: String) = _estado.update { it.copy(avatarId = id) }

    /** A navegação para B2 já aconteceu: voltar para B1 não deve avançar de novo. */
    fun aoNavegar() = _estado.update { it.copy(concluido = false) }

    fun continuar() {
        val atual = _estado.value
        val id = uid
        if (!atual.podeContinuar || id == null) return
        _estado.update { it.copy(salvando = true, erro = false) }
        viewModelScope.launch {
            when (conta.salvarPerfil(id, atual.nome, atual.avatarId)) {
                is ResultadoConta.Ok -> _estado.update { it.copy(salvando = false, concluido = true) }
                is ResultadoConta.Falha -> _estado.update { it.copy(salvando = false, erro = true) }
            }
        }
    }
}
