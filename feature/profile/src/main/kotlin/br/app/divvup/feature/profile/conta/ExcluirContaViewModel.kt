package br.app.divvup.feature.profile.conta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.casa.RepositorioDeCasa
import br.app.divvup.core.domain.casa.RepositorioDeConvites
import br.app.divvup.core.domain.conta.ErroDeConta
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.model.Papel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** E7 (Play Store): dois passos, reautenticação e "EXCLUIR" digitado. */
@HiltViewModel
class ExcluirContaViewModel @Inject constructor(
    private val conta: RepositorioDeConta,
    casas: RepositorioDeCasa,
    convites: RepositorioDeConvites,
) : ViewModel() {
    private val _estado = MutableStateFlow(ExcluirContaEstado())
    val estado: StateFlow<ExcluirContaEstado> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = (conta.sessao.first() as? Sessao.Conectado)?.uid ?: return@launch
            val perfil = (conta.perfil(uid) as? ResultadoConta.Ok)?.valor
            val casa = casas.casaAtual.first()
            val membros = convites.membros.first()
            // R-04: único admin → o morador ativo mais antigo herda o papel.
            val souUnicoAdmin = membros.any { it.uid == uid && it.papel == Papel.ADMIN } && membros.count { it.papel == Papel.ADMIN } == 1
            val sucessor = membros.filter { it.uid != uid }.minByOrNull { it.entrouEm ?: java.time.Instant.MAX }
            _estado.update {
                it.copy(
                    temSenha = perfil?.temSenha ?: true,
                    casa = casa?.nome?.takeIf { souUnicoAdmin && sucessor != null },
                    sucessor = sucessor?.nome?.substringBefore(' ')?.takeIf { souUnicoAdmin },
                )
            }
        }
    }

    fun continuar() = _estado.update { it.copy(passo = 2) }

    /** "Voltar": do passo 2 volta ao 1; do 1, a tela fecha (devolve `true`). */
    fun voltar(): Boolean {
        if (_estado.value.passo == 1) return true
        _estado.update { it.copy(passo = 1) }
        return false
    }

    fun mudarSenha(v: String) = _estado.update { it.copy(senha = v, erroSenha = false) }

    fun mudarPalavra(v: String) = _estado.update { it.copy(palavra = v.uppercase().take(PALAVRA.length + 2), erro = false) }

    /** Conta com senha confirma com ela; conta Google traz o ID token. */
    fun excluir(idTokenGoogle: String? = null) {
        val atual = _estado.value
        if (!atual.podeExcluir) return
        _estado.update { it.copy(excluindo = true, erro = false, erroSenha = false) }
        viewModelScope.launch {
            val r = conta.excluirConta(atual.senha.takeIf { atual.temSenha }, idTokenGoogle)
            _estado.update {
                when (r) {
                    is ResultadoConta.Ok -> it.copy(excluindo = false, feita = true)
                    is ResultadoConta.Falha -> it.copy(excluindo = false, erroSenha = r.erro == ErroDeConta.SenhaIncorreta, erro = r.erro != ErroDeConta.SenhaIncorreta)
                }
            }
        }
    }
}
