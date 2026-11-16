package br.app.divvup.feature.profile.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.casa.FUSO_DA_TEMPORADA
import br.app.divvup.core.domain.casa.RepositorioDeCasa
import br.app.divvup.core.domain.casa.RepositorioDeConvites
import br.app.divvup.core.domain.conta.PreferenciasDoAparelho
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.model.Medalha
import br.app.divvup.core.model.Tema
import br.app.divvup.core.model.Usuario
import br.app.divvup.feature.profile.conta.EditarPerfilEstado
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private data class LocalDoPerfil(val dica: Medalha? = null, val confirmandoSaida: Boolean = false)

/** E1 (R-30, R-31). O perfil é relido ao voltar para a aba (pontos e medalhas mudam). */
@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val conta: RepositorioDeConta,
    casas: RepositorioDeCasa,
    convites: RepositorioDeConvites,
    private val aparelho: PreferenciasDoAparelho,
) : ViewModel() {
    private val usuario = MutableStateFlow<Usuario?>(null)
    private val local = MutableStateFlow(LocalDoPerfil())

    val estado: StateFlow<PerfilEstado> = combine(usuario, casas.casaAtual, convites.membros, aparelho.tema, local) { u, casa, membros, tema, l ->
        PerfilEstado(
            usuario = u,
            pontosDoMes = membros.firstOrNull { it.uid == u?.uid }?.pontosDoMes ?: 0,
            casa = casa?.nome,
            tema = tema,
            hoje = LocalDate.now(FUSO_DA_TEMPORADA),
            dica = l.dica,
            confirmandoSaida = l.confirmandoSaida,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAR_APOS_MS), PerfilEstado())

    fun recarregar() {
        viewModelScope.launch {
            val uid = (conta.sessao.first() as? Sessao.Conectado)?.uid ?: return@launch
            (conta.perfil(uid) as? ResultadoConta.Ok)?.valor?.let { usuario.value = it }
        }
    }

    /** Bloqueada: mostra (ou esconde) o critério; conquistada: esconde. */
    fun tocarMedalha(m: Medalha) = local.update {
        val bloqueada = usuario.value?.medalhas?.containsKey(m) != true
        it.copy(dica = if (bloqueada && it.dica != m) m else null)
    }

    fun pedirSaida() = local.update { it.copy(confirmandoSaida = true) }

    fun cancelarSaida() = local.update { it.copy(confirmandoSaida = false) }

    suspend fun sair() = conta.sair()

    private companion object {
        const val PARAR_APOS_MS = 5_000L
    }
}

/** E5. */
@HiltViewModel
class AparenciaViewModel @Inject constructor(private val aparelho: PreferenciasDoAparelho) : ViewModel() {
    val tema: StateFlow<Tema> = aparelho.tema

    fun escolher(tema: Tema) = aparelho.escolherTema(tema)
}

/** E2: nome e avatar são campos que o próprio dono grava (R-32). */
@HiltViewModel
class EditarPerfilViewModel @Inject constructor(private val conta: RepositorioDeConta) : ViewModel() {
    private val _estado = MutableStateFlow(EditarPerfilEstado())
    val estado: StateFlow<EditarPerfilEstado> = _estado.asStateFlow()
    private val _salvo = MutableStateFlow(false)
    val salvo: StateFlow<Boolean> = _salvo.asStateFlow()
    private var uid: String? = null

    init {
        viewModelScope.launch {
            val id = (conta.sessao.first() as? Sessao.Conectado)?.uid ?: return@launch
            uid = id
            (conta.perfil(id) as? ResultadoConta.Ok)?.valor?.let { u ->
                _estado.update { it.copy(nome = u.nome, avatarId = u.avatarId, email = u.email, temSenha = u.temSenha) }
            }
        }
    }

    fun mudarNome(nome: String) = _estado.update { it.copy(nome = nome, erro = false) }

    fun escolherAvatar(avatarId: String) = _estado.update { it.copy(avatarId = avatarId) }

    fun salvar() {
        val id = uid ?: return
        val atual = _estado.value
        if (!atual.podeSalvar) return
        _estado.update { it.copy(salvando = true, erro = false) }
        viewModelScope.launch {
            val ok = conta.salvarPerfil(id, atual.nome.trim(), atual.avatarId.orEmpty()) is ResultadoConta.Ok
            _estado.update { it.copy(salvando = false, erro = !ok) }
            if (ok) _salvo.value = true
        }
    }
}
