package br.app.divvup.feature.profile.avatar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.domain.perfil.GaleriaDeAvatares
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Chaves de navegação de E3. */
object GaleriaArgs {
    /** Avatar inicial (vem de B1 ou do perfil). */
    const val INICIAL = "avatarInicial"

    /** true = onboarding: E3 só devolve a escolha para B1, sem gravar. */
    const val SO_ESCOLHER = "soEscolher"

    /** Chave do resultado na entrada anterior da pilha. */
    const val RESULTADO = "avatarEscolhido"
}

@HiltViewModel
class GaleriaViewModel @Inject constructor(
    private val conta: RepositorioDeConta,
    salvo: SavedStateHandle,
) : ViewModel() {
    private val soEscolher: Boolean = salvo[GaleriaArgs.SO_ESCOLHER] ?: false
    private val _estado = MutableStateFlow(
        GaleriaEstado(selecionado = salvo.get<String>(GaleriaArgs.INICIAL)?.takeIf(GaleriaDeAvatares::valido) ?: GaleriaDeAvatares.padrao),
    )
    val estado: StateFlow<GaleriaEstado> = _estado.asStateFlow()

    /** Emite o avatar salvo/escolhido para fechar a tela. */
    private val _concluido = MutableStateFlow<String?>(null)
    val concluido: StateFlow<String?> = _concluido.asStateFlow()

    fun escolher(id: String) = _estado.update { it.copy(selecionado = id, erro = false) }

    fun salvar() {
        val escolhido = _estado.value.selecionado
        if (soEscolher) {
            _concluido.value = escolhido
            return
        }
        _estado.update { it.copy(salvando = true, erro = false) }
        viewModelScope.launch {
            val uid = (conta.sessao.first() as? Sessao.Conectado)?.uid
            val perfil = uid?.let { (conta.perfil(it) as? ResultadoConta.Ok)?.valor }
            val r = if (uid != null && perfil != null) conta.salvarPerfil(uid, perfil.nome, escolhido) else null
            if (r is ResultadoConta.Ok) {
                _concluido.value = escolhido
            } else {
                _estado.update { it.copy(salvando = false, erro = true) }
            }
        }
    }
}
