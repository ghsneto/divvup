package br.app.divvup.feature.house.convites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.casa.RepositorioDeCasa
import br.app.divvup.core.domain.casa.RepositorioDeConvites
import br.app.divvup.core.domain.casa.ResultadoConvite
import br.app.divvup.core.domain.casa.tempoRestante
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.model.Convite
import br.app.divvup.feature.house.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** Duração do aviso (snackbar) de B8/G2, como no desenho. */
private const val AVISO_MS = 2_200L

/** B8: gera um código ao abrir e conta o tempo que falta. */
@HiltViewModel
class ConvideViewModel @Inject constructor(
    private val convites: RepositorioDeConvites,
    casas: RepositorioDeCasa,
) : ViewModel() {
    private val relogio: Clock = Clock.systemUTC()
    private val _estado = MutableStateFlow(ConvideEstado())
    val estado: StateFlow<ConvideEstado> = _estado.asStateFlow()
    private var atual: Convite? = null
    private var aviso: Job? = null

    init {
        viewModelScope.launch { casas.casaAtual.collect { c -> _estado.update { it.copy(nomeDaCasa = c?.nome.orEmpty()) } } }
        gerar(primeiro = true)
    }

    /** Chamado a cada segundo pela tela, enquanto ela está visível. */
    fun tique() {
        atual?.let { c -> _estado.update { it.copy(restante = tempoRestante(c, Instant.now(relogio))) } }
    }

    fun gerar(primeiro: Boolean = false) {
        if (_estado.value.gerando) return
        _estado.update { it.copy(gerando = true, erro = false) }
        viewModelScope.launch {
            when (val r = convites.criar()) {
                is ResultadoConvite.Ok -> {
                    atual = r.valor
                    _estado.update { it.copy(gerando = false, codigo = r.valor.codigo, restante = tempoRestante(r.valor, Instant.now(relogio))) }
                    if (!primeiro) avisar(R.string.convide_novo)
                }
                is ResultadoConvite.Falha -> _estado.update { it.copy(gerando = false, erro = true) }
            }
        }
    }

    fun avisar(mensagem: Int) {
        aviso?.cancel()
        _estado.update { it.copy(aviso = mensagem) }
        aviso = viewModelScope.launch {
            delay(AVISO_MS)
            _estado.update { it.copy(aviso = null) }
        }
    }
}

/** G2: lista ao vivo dos códigos da casa; gerar e revogar. */
@HiltViewModel
class ConvitesViewModel @Inject constructor(
    private val convites: RepositorioDeConvites,
    casas: RepositorioDeCasa,
    conta: RepositorioDeConta,
) : ViewModel() {
    private val relogio: Clock = Clock.systemUTC()
    private val agora = MutableStateFlow(Instant.now(relogio))
    private val local = MutableStateFlow(ConvitesEstado())
    private var aviso: Job? = null

    val estado: StateFlow<ConvitesEstado> = combine(convites.convites, casas.casaAtual, conta.sessao, agora, local) { lista, casa, sessao, instante, l ->
        l.copy(nomeDaCasa = casa?.nome.orEmpty(), convites = lista, eu = (sessao as? Sessao.Conectado)?.uid, agora = instante)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAR_APOS_MS), ConvitesEstado())

    /** Chamado a cada minuto pela tela: atualiza "expira em…". */
    fun tique() {
        agora.value = Instant.now(relogio)
    }

    fun gerar() {
        if (local.value.gerando) return
        local.update { it.copy(gerando = true, erro = false) }
        viewModelScope.launch {
            when (val r = convites.criar()) {
                is ResultadoConvite.Ok -> {
                    local.update { it.copy(gerando = false) }
                    avisar(R.string.convites_gerado, r.valor.codigo)
                }
                is ResultadoConvite.Falha -> local.update { it.copy(gerando = false, erro = true) }
            }
        }
    }

    fun revogar(codigo: String) {
        viewModelScope.launch {
            when (convites.revogar(codigo)) {
                is ResultadoConvite.Ok -> avisar(R.string.convites_revogado_msg, codigo)
                is ResultadoConvite.Falha -> local.update { it.copy(erro = true) }
            }
        }
    }

    private fun avisar(mensagem: Int, codigo: String) {
        aviso?.cancel()
        local.update { it.copy(aviso = mensagem to codigo) }
        aviso = viewModelScope.launch {
            delay(AVISO_MS)
            local.update { it.copy(aviso = null) }
        }
    }

    private companion object {
        const val PARAR_APOS_MS = 5_000L
    }
}
