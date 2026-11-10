package br.app.divvup.feature.onboarding.casa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.casa.ErroDeCasa
import br.app.divvup.core.domain.casa.NOME_CASA_MAXIMO
import br.app.divvup.core.domain.casa.RepositorioDeCasa
import br.app.divvup.core.domain.casa.ResultadoCasa
import br.app.divvup.core.domain.casa.anunciaPorPadrao
import br.app.divvup.core.domain.casa.nomeDaCasaValido
import br.app.divvup.core.domain.casa.normalizarNomeDaCasa
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.model.TipoDeCasa
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** B2: só precisa do primeiro nome para o "oi, iane" e da saída da conta. */
@HiltViewModel
class SuaCasaViewModel @Inject constructor(private val conta: RepositorioDeConta) : ViewModel() {
    private val _primeiroNome = MutableStateFlow("")
    val primeiroNome: StateFlow<String> = _primeiroNome.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = (conta.sessao.first() as? Sessao.Conectado)?.uid ?: return@launch
            val perfil = (conta.perfil(uid) as? ResultadoConta.Ok)?.valor ?: return@launch
            _primeiroNome.value = perfil.nome.trim().substringBefore(' ')
        }
    }

    suspend fun sair() = conta.sair()
}

/** Estado de B3. */
data class CriarCasaEstado(
    val nome: String = "",
    val tipo: TipoDeCasa = TipoDeCasa.REPUBLICA,
    val anunciar: Boolean = anunciaPorPadrao(TipoDeCasa.REPUBLICA),
    /** Tentou criar com o nome vazio. */
    val nomeVazio: Boolean = false,
    val criando: Boolean = false,
    val erroDeConexao: Boolean = false,
    /** Nome da casa criada: mostra a confirmação. */
    val criada: String? = null,
)

@HiltViewModel
class CriarCasaViewModel @Inject constructor(private val casas: RepositorioDeCasa) : ViewModel() {
    private val _estado = MutableStateFlow(CriarCasaEstado())
    val estado: StateFlow<CriarCasaEstado> = _estado.asStateFlow()

    /** R-02: a pessoa já estava numa casa; segue direto para a estrutura principal. */
    private val _jaTinhaCasa = MutableStateFlow(false)
    val jaTinhaCasa: StateFlow<Boolean> = _jaTinhaCasa.asStateFlow()

    fun aoMudarNome(v: String) = _estado.update { it.copy(nome = v.take(NOME_CASA_MAXIMO), nomeVazio = false, erroDeConexao = false) }

    /** Trocar o tipo reinicia "Anunciar vagas" no padrão do tipo (TELAS.md B3). */
    fun aoEscolherTipo(tipo: TipoDeCasa) = _estado.update { it.copy(tipo = tipo, anunciar = anunciaPorPadrao(tipo)) }

    fun aoAnunciar(ligado: Boolean) = _estado.update { it.copy(anunciar = ligado) }

    fun criar() {
        val atual = _estado.value
        if (atual.criando) return
        if (!nomeDaCasaValido(atual.nome)) {
            _estado.update { it.copy(nomeVazio = true) }
            return
        }
        _estado.update { it.copy(criando = true, erroDeConexao = false) }
        viewModelScope.launch {
            val anunciar = atual.tipo == TipoDeCasa.REPUBLICA && atual.anunciar
            when (val r = casas.criarCasa(atual.nome, atual.tipo, anunciar)) {
                is ResultadoCasa.Ok -> _estado.update { it.copy(criando = false, criada = normalizarNomeDaCasa(atual.nome)) }
                is ResultadoCasa.Falha -> {
                    if (r.erro == ErroDeCasa.JaEmCasa) _jaTinhaCasa.value = true
                    _estado.update { it.copy(criando = false, erroDeConexao = r.erro != ErroDeCasa.JaEmCasa) }
                }
            }
        }
    }
}
