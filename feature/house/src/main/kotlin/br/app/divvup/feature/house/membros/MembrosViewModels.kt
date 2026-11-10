package br.app.divvup.feature.house.membros

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.casa.DadosDaCasa
import br.app.divvup.core.domain.casa.ErroDeCasa
import br.app.divvup.core.domain.casa.FUSO_DA_TEMPORADA
import br.app.divvup.core.domain.casa.RepositorioDeCasa
import br.app.divvup.core.domain.casa.RepositorioDeConvites
import br.app.divvup.core.domain.casa.ResultadoCasa
import br.app.divvup.core.domain.casa.SituacaoDoConvite
import br.app.divvup.core.domain.casa.nomeDaCasaValido
import br.app.divvup.core.domain.casa.posicoesNoMes
import br.app.divvup.core.domain.casa.resumoDaTemporada
import br.app.divvup.core.domain.casa.situacao
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.model.Papel
import br.app.divvup.feature.house.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

private const val PARAR_APOS_MS = 5_000L
private const val AVISO_MS = 2_200L

/** G1. */
@HiltViewModel
class MinhaCasaViewModel @Inject constructor(
    private val casas: RepositorioDeCasa,
    convites: RepositorioDeConvites,
    conta: RepositorioDeConta,
) : ViewModel() {
    private val local = MutableStateFlow(MinhaCasaEstado())
    private val _saiu = MutableStateFlow(false)
    val saiu: StateFlow<Boolean> = _saiu.asStateFlow()

    val estado: StateFlow<MinhaCasaEstado> =
        combine(casas.casaAtual, convites.membros, convites.convites, conta.sessao, local) { casa, membros, lista, sessao, l ->
            val agora = Instant.now()
            l.copy(
                casa = casa,
                membros = membros.sortedBy { it.entrouEm },
                eu = (sessao as? Sessao.Conectado)?.uid,
                codigosAtivos = lista.count { situacao(it, agora) == SituacaoDoConvite.Ativo },
                temporada = resumoDaTemporada(LocalDate.now(FUSO_DA_TEMPORADA)),
                agora = agora,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAR_APOS_MS), MinhaCasaEstado())

    fun pedirSaida() = local.update { it.copy(confirmandoSaida = true) }

    fun cancelarSaida() = local.update { it.copy(confirmandoSaida = false) }

    fun sair() {
        if (local.value.saindo) return
        local.update { it.copy(saindo = true) }
        viewModelScope.launch {
            val ok = casas.sair() is ResultadoCasa.Ok
            local.update { it.copy(saindo = false, confirmandoSaida = !ok) }
            if (ok) _saiu.value = true
        }
    }
}

/** D3/G3. */
@HiltViewModel
class MembroViewModel @Inject constructor(
    salvo: SavedStateHandle,
    private val casas: RepositorioDeCasa,
    convites: RepositorioDeConvites,
    conta: RepositorioDeConta,
) : ViewModel() {
    private val uid: String = checkNotNull(salvo["uid"])
    private val local = MutableStateFlow(MembroEstado())
    private val _removido = MutableStateFlow(false)
    val removido: StateFlow<Boolean> = _removido.asStateFlow()
    private var aviso: Job? = null

    val estado: StateFlow<MembroEstado> = combine(convites.membros, conta.sessao, local) { membros, sessao, l ->
        val eu = (sessao as? Sessao.Conectado)?.uid
        val souAdmin = membros.any { it.uid == eu && it.papel == Papel.ADMIN }
        l.copy(
            membro = membros.firstOrNull { it.uid == uid },
            posicao = posicoesNoMes(membros)[uid] ?: 1,
            visaoDeAdmin = souAdmin && eu != uid,
            agora = Instant.now(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAR_APOS_MS), MembroEstado())

    fun alternarAdmin() {
        val m = estado.value.membro ?: return
        if (local.value.ocupado) return
        val novo = if (m.papel == Papel.ADMIN) Papel.MEMBER else Papel.ADMIN
        local.update { it.copy(ocupado = true, erroUltimoAdmin = false) }
        viewModelScope.launch {
            val r = casas.alterarPapel(uid, novo)
            local.update { it.copy(ocupado = false, erroUltimoAdmin = (r as? ResultadoCasa.Falha)?.erro == ErroDeCasa.UltimoAdmin) }
            if (r is ResultadoCasa.Ok) avisar(if (novo == Papel.ADMIN) R.string.membro_agora_admin else R.string.membro_agora_morador)
        }
    }

    fun pedirRemocao() = local.update { it.copy(confirmandoRemocao = true) }

    fun cancelarRemocao() = local.update { it.copy(confirmandoRemocao = false) }

    fun remover() {
        if (local.value.ocupado) return
        local.update { it.copy(ocupado = true) }
        viewModelScope.launch {
            val ok = casas.removerMembro(uid) is ResultadoCasa.Ok
            local.update { it.copy(ocupado = false, confirmandoRemocao = !ok) }
            if (ok) _removido.value = true
        }
    }

    private fun avisar(mensagem: Int) {
        aviso?.cancel()
        local.update { it.copy(aviso = mensagem) }
        aviso = viewModelScope.launch {
            delay(AVISO_MS)
            local.update { it.copy(aviso = null) }
        }
    }
}

/** G4: começa com os dados atuais da casa. */
@HiltViewModel
class EditarCasaViewModel @Inject constructor(private val casas: RepositorioDeCasa) : ViewModel() {
    private val _estado = MutableStateFlow(EditarCasaEstado())
    val estado: StateFlow<EditarCasaEstado> = _estado.asStateFlow()
    private val _salvo = MutableStateFlow(false)
    val salvo: StateFlow<Boolean> = _salvo.asStateFlow()

    init {
        viewModelScope.launch {
            val c = casas.casaAtual.filterNotNull().first()
            _estado.update {
                it.copy(
                    dados = DadosDaCasa(c.nome, c.tipo, c.cidade.orEmpty(), c.bairro.orEmpty(), c.rua.orEmpty()),
                    anuncioAtivo = c.anuncioAtivo,
                )
            }
        }
    }

    fun aoMudar(dados: DadosDaCasa) = _estado.update { it.copy(dados = dados, nomeVazio = false, erro = false) }

    fun salvar() {
        val atual = _estado.value
        if (atual.salvando) return
        if (!nomeDaCasaValido(atual.dados.nome)) {
            _estado.update { it.copy(nomeVazio = true) }
            return
        }
        _estado.update { it.copy(salvando = true, erro = false) }
        viewModelScope.launch {
            val ok = casas.editar(atual.dados) is ResultadoCasa.Ok
            _estado.update { it.copy(salvando = false, erro = !ok) }
            if (ok) _salvo.value = true
        }
    }
}
