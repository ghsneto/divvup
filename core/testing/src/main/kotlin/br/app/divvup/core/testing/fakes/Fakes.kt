package br.app.divvup.core.testing.fakes

import br.app.divvup.core.data.conexao.MonitorDeConexao
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Repositório de conta em memória para testes de ViewModel e de UI. */
class FakeRepositorioDeConta(
    sessaoInicial: Sessao = Sessao.Desconectado,
    var perfilAtual: Usuario? = null,
) : RepositorioDeConta {
    private val _sessao = MutableStateFlow(sessaoInicial)
    override val sessao: StateFlow<Sessao> = _sessao

    var resultadoEntrar: ResultadoConta<Unit> = ResultadoConta.Ok(Unit)
    var resultadoCriar: ResultadoConta<Unit> = ResultadoConta.Ok(Unit)
    var resultadoRecuperar: ResultadoConta<Unit> = ResultadoConta.Ok(Unit)
    val chamadas = mutableListOf<String>()

    override suspend fun entrar(email: String, senha: String): ResultadoConta<Unit> {
        chamadas += "entrar:$email"
        if (resultadoEntrar is ResultadoConta.Ok) _sessao.value = Sessao.Conectado("uid-teste")
        return resultadoEntrar
    }

    override suspend fun criarConta(nome: String, email: String, senha: String): ResultadoConta<Unit> {
        chamadas += "criar:$nome:$email"
        if (resultadoCriar is ResultadoConta.Ok) _sessao.value = Sessao.Conectado("uid-teste")
        return resultadoCriar
    }

    override suspend fun enviarRecuperacao(email: String): ResultadoConta<Unit> {
        chamadas += "recuperar:$email"
        return resultadoRecuperar
    }

    override suspend fun sair() {
        _sessao.value = Sessao.Desconectado
    }

    override suspend fun perfil(uid: String): ResultadoConta<Usuario?> = ResultadoConta.Ok(perfilAtual)
}

/** Conexão controlável nos testes. */
class FakeMonitorDeConexao(online: Boolean = true) : MonitorDeConexao {
    val estado = MutableStateFlow(online)
    override val online: StateFlow<Boolean> = estado
}
