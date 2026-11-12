package br.app.divvup.core.testing.fakes

import br.app.divvup.core.data.conexao.MonitorDeConexao
import br.app.divvup.core.domain.casa.DadosDaCasa
import br.app.divvup.core.domain.casa.ErroDeConvite
import br.app.divvup.core.domain.casa.RepositorioDeCasa
import br.app.divvup.core.domain.casa.RepositorioDeConvites
import br.app.divvup.core.domain.casa.ResultadoConvite
import br.app.divvup.core.domain.casa.VALIDADE_CONVITE
import br.app.divvup.core.domain.casa.ResultadoCasa
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.tarefa.EstadoDasTarefas
import br.app.divvup.core.domain.tarefa.FormularioTarefa
import br.app.divvup.core.domain.tarefa.RepositorioDeTarefas
import br.app.divvup.core.domain.tarefa.ResultadoTarefa
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.model.Casa
import br.app.divvup.core.model.Convite
import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.Papel
import br.app.divvup.core.model.TipoDeCasa
import br.app.divvup.core.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

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

    var resultadoGoogle: ResultadoConta<Unit> = ResultadoConta.Ok(Unit)

    override suspend fun entrarComGoogle(idTokenGoogle: String): ResultadoConta<Unit> {
        chamadas += "google:$idTokenGoogle"
        if (resultadoGoogle is ResultadoConta.Ok) _sessao.value = Sessao.Conectado("uid-google")
        return resultadoGoogle
    }

    override suspend fun enviarRecuperacao(email: String): ResultadoConta<Unit> {
        chamadas += "recuperar:$email"
        return resultadoRecuperar
    }

    override suspend fun sair() {
        _sessao.value = Sessao.Desconectado
    }

    var resultadoSalvar: ResultadoConta<Unit> = ResultadoConta.Ok(Unit)

    override suspend fun salvarPerfil(uid: String, nome: String, avatarId: String): ResultadoConta<Unit> {
        chamadas += "salvar:$uid:$nome:$avatarId"
        if (resultadoSalvar is ResultadoConta.Ok) {
            perfilAtual = (perfilAtual ?: Usuario(uid, nome, null, null, null)).copy(nome = nome, avatarId = avatarId)
        }
        return resultadoSalvar
    }

    override suspend fun perfil(uid: String): ResultadoConta<Usuario?> = ResultadoConta.Ok(perfilAtual)
}

/** Conexão controlável nos testes. */
class FakeMonitorDeConexao(online: Boolean = true) : MonitorDeConexao {
    val estado = MutableStateFlow(online)
    override val online: StateFlow<Boolean> = estado
}

/** Repositório de casa em memória. */
class FakeRepositorioDeCasa(casaInicial: Casa? = null) : RepositorioDeCasa {
    val casa = MutableStateFlow(casaInicial)
    override val casaAtual: StateFlow<Casa?> = casa

    var resultadoCriar: ResultadoCasa<String> = ResultadoCasa.Ok("casa-teste")
    val chamadas = mutableListOf<String>()

    override suspend fun criarCasa(nome: String, tipo: TipoDeCasa, anunciar: Boolean): ResultadoCasa<String> {
        chamadas += "criar:$nome:$tipo:$anunciar"
        val r = resultadoCriar
        if (r is ResultadoCasa.Ok) casa.value = Casa(r.valor, nome, tipo, "2026-11")
        return r
    }

    var resultadoOperacao: ResultadoCasa<Unit> = ResultadoCasa.Ok(Unit)

    override suspend fun alterarPapel(uid: String, papel: Papel): ResultadoCasa<Unit> {
        chamadas += "papel:$uid:$papel"
        return resultadoOperacao
    }

    override suspend fun removerMembro(uid: String): ResultadoCasa<Unit> {
        chamadas += "remover:$uid"
        return resultadoOperacao
    }

    override suspend fun sair(): ResultadoCasa<Unit> {
        chamadas += "sair"
        if (resultadoOperacao is ResultadoCasa.Ok) casa.value = null
        return resultadoOperacao
    }

    override suspend fun editar(dados: DadosDaCasa): ResultadoCasa<Unit> {
        chamadas += "editar:${dados.nome}:${dados.tipo}:${dados.cidade}:${dados.bairro}:${dados.rua}"
        return resultadoOperacao
    }
}

/** Convites e moradores em memória. */
class FakeRepositorioDeConvites(
    convitesIniciais: List<Convite> = emptyList(),
    membrosIniciais: List<Membro> = emptyList(),
) : RepositorioDeConvites {
    val listaDeConvites = MutableStateFlow(convitesIniciais)
    val listaDeMembros = MutableStateFlow(membrosIniciais)
    override val convites: StateFlow<List<Convite>> = listaDeConvites
    override val membros: StateFlow<List<Membro>> = listaDeMembros

    var proximoCodigo = "K7PQ2X"
    var resultadoEntrar: ResultadoConvite<String> = ResultadoConvite.Ok("casa-teste")
    var falhaAoCriar: ErroDeConvite? = null
    val chamadas = mutableListOf<String>()

    override suspend fun criar(): ResultadoConvite<Convite> {
        chamadas += "criar"
        falhaAoCriar?.let { return ResultadoConvite.Falha(it) }
        val agora = Instant.parse("2026-11-10T12:00:00Z")
        val novo = Convite(proximoCodigo, "uid-teste", "Iane", agora, agora.plus(VALIDADE_CONVITE), null, null, false, null)
        listaDeConvites.value = listOf(novo) + listaDeConvites.value
        return ResultadoConvite.Ok(novo)
    }

    override suspend fun revogar(codigo: String): ResultadoConvite<Unit> {
        chamadas += "revogar:$codigo"
        listaDeConvites.value = listaDeConvites.value.map { if (it.codigo == codigo) it.copy(revogado = true) else it }
        return ResultadoConvite.Ok(Unit)
    }

    override suspend fun entrar(codigo: String): ResultadoConvite<String> {
        chamadas += "entrar:$codigo"
        return resultadoEntrar
    }
}

/** Tarefas em memória. */
class FakeRepositorioDeTarefas(inicial: EstadoDasTarefas = EstadoDasTarefas.Carregadas(emptyList())) : RepositorioDeTarefas {
    val estado = MutableStateFlow(inicial)
    override val tarefas: StateFlow<EstadoDasTarefas> = estado

    var resultadoCriar: ResultadoTarefa<String> = ResultadoTarefa.Ok("tarefa-teste")
    val criadas = mutableListOf<FormularioTarefa>()

    override suspend fun criar(formulario: FormularioTarefa): ResultadoTarefa<String> {
        criadas += formulario
        return resultadoCriar
    }
}
