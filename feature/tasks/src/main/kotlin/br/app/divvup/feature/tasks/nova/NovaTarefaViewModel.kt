package br.app.divvup.feature.tasks.nova

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.domain.casa.FUSO_DA_TEMPORADA
import br.app.divvup.core.domain.tarefa.AtalhoDePrazo
import br.app.divvup.core.domain.tarefa.DESCRICAO_MAXIMA
import br.app.divvup.core.domain.tarefa.ErroDeTarefa
import br.app.divvup.core.domain.tarefa.FormularioTarefa
import br.app.divvup.core.domain.tarefa.RepositorioDeTarefas
import br.app.divvup.core.domain.tarefa.ResultadoTarefa
import br.app.divvup.core.domain.tarefa.TITULO_MAXIMO
import br.app.divvup.core.domain.tarefa.atalhosValidos
import br.app.divvup.core.domain.tarefa.pesoDe
import br.app.divvup.core.domain.tarefa.prazoDoAtalho
import br.app.divvup.core.domain.tarefa.urgente
import br.app.divvup.core.model.Categoria
import br.app.divvup.core.model.Peso
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** Estado de C2. */
data class NovaTarefaEstado(
    val formulario: FormularioTarefa,
    val agora: Instant,
    /** Atalho escolhido; `null` quando a data ou a hora foi escolhida à mão. */
    val atalho: AtalhoDePrazo? = AtalhoDePrazo.Amanha20h,
    val atalhos: List<AtalhoDePrazo> = AtalhoDePrazo.entries,
    val alterado: Boolean = false,
    val criando: Boolean = false,
    val erro: ErroDeTarefa? = null,
    val confirmandoDescarte: Boolean = false,
    val criada: Boolean = false,
    /** C2 em modo edição (R-12): "Salvar" e "Excluir tarefa". */
    val editando: Boolean = false,
    val confirmandoExclusao: Boolean = false,
    val excluindo: Boolean = false,
    val excluida: Boolean = false,
) {
    /** Aviso de atribuição automática (R-26). */
    val prazoCurto: Boolean get() = urgente(formulario.prazo.toInstant(), agora)
    val prazoNoPassado: Boolean get() = !formulario.prazo.toInstant().isAfter(agora)
    val podeCriar: Boolean get() = formulario.podeCriar && !criando
}

@HiltViewModel
class NovaTarefaViewModel @Inject constructor(
    private val tarefas: RepositorioDeTarefas,
    salvo: SavedStateHandle,
) : ViewModel() {
    private val relogio: Clock = Clock.system(FUSO_DA_TEMPORADA)

    /** Id da ocorrência em edição (`RotaEditarTarefa`); `null` em "nova tarefa". */
    private val tarefaId: String? = salvo[ARG_TAREFA]
    private val _estado = MutableStateFlow(estadoInicial(Instant.now(relogio)).copy(editando = tarefaId != null))
    val estado: StateFlow<NovaTarefaEstado> = _estado.asStateFlow()

    init {
        tarefaId?.let { id ->
            viewModelScope.launch {
                val t = tarefas.tarefa(id).first() ?: return@launch
                _estado.update {
                    it.copy(
                        formulario = FormularioTarefa(
                            titulo = t.titulo,
                            descricao = t.descricao.orEmpty(),
                            categoria = t.categoria,
                            peso = pesoDe(t.pontos),
                            prazo = t.prazo.atZone(FUSO_DA_TEMPORADA),
                        ),
                        atalho = null,
                    )
                }
            }
        }
    }

    fun aoMudarTitulo(v: String) = mudar { it.copy(titulo = v.take(TITULO_MAXIMO)) }

    fun aoMudarDescricao(v: String) = mudar { it.copy(descricao = v.take(DESCRICAO_MAXIMA)) }

    fun aoEscolherCategoria(c: Categoria) = mudar { it.copy(categoria = c) }

    fun aoEscolherPeso(p: Peso) = mudar { it.copy(peso = p) }

    fun aoEscolherAtalho(a: AtalhoDePrazo) {
        val agora = Instant.now(relogio)
        _estado.update { it.copy(formulario = it.formulario.copy(prazo = prazoDoAtalho(a, agora)), atalho = a, agora = agora, alterado = true, erro = null) }
    }

    fun aoEscolherData(data: LocalDate) = mudarPrazo { it.with(data) }

    fun aoEscolherHora(hora: LocalTime) = mudarPrazo { it.with(hora) }

    /** "X": com alterações, pergunta antes de sair (TELAS.md C2). Devolve `true` se pode fechar já. */
    fun aoFechar(): Boolean {
        if (!_estado.value.alterado) return true
        _estado.update { it.copy(confirmandoDescarte = true) }
        return false
    }

    fun continuarEditando() = _estado.update { it.copy(confirmandoDescarte = false) }

    fun criar() {
        val atual = _estado.value.copy(agora = Instant.now(relogio))
        if (!atual.podeCriar) return
        if (atual.prazoNoPassado) {
            _estado.update { it.copy(erro = ErroDeTarefa.PrazoNoPassado, agora = atual.agora) }
            return
        }
        _estado.update { it.copy(criando = true, erro = null) }
        viewModelScope.launch {
            val r = tarefaId?.let { tarefas.editar(it, atual.formulario) } ?: tarefas.criar(atual.formulario)
            when (r) {
                is ResultadoTarefa.Ok -> _estado.update { it.copy(criando = false, criada = true) }
                is ResultadoTarefa.Falha -> _estado.update { it.copy(criando = false, erro = r.erro) }
            }
        }
    }

    fun pedirExclusao() = _estado.update { it.copy(confirmandoExclusao = true) }

    fun cancelarExclusao() = _estado.update { it.copy(confirmandoExclusao = false) }

    /** "Excluir tarefa" (R-12): só autor/admin e sem dono; o backend confere. */
    fun excluir() {
        val id = tarefaId ?: return
        if (_estado.value.excluindo) return
        _estado.update { it.copy(excluindo = true) }
        viewModelScope.launch {
            when (val r = tarefas.excluir(id)) {
                is ResultadoTarefa.Ok -> _estado.update { it.copy(excluindo = false, confirmandoExclusao = false, excluida = true) }
                is ResultadoTarefa.Falha -> _estado.update { it.copy(excluindo = false, confirmandoExclusao = false, erro = r.erro) }
            }
        }
    }

    private fun mudar(f: (FormularioTarefa) -> FormularioTarefa) =
        _estado.update { it.copy(formulario = f(it.formulario), alterado = true, erro = null) }

    private fun mudarPrazo(f: (java.time.ZonedDateTime) -> java.time.ZonedDateTime) =
        _estado.update { it.copy(formulario = it.formulario.copy(prazo = f(it.formulario.prazo)), atalho = null, alterado = true, erro = null) }

    companion object {
        /** Nome do argumento de `RotaEditarTarefa`. */
        const val ARG_TAREFA = "tarefaId"

        /** Padrão: amanhã às 20h (TELAS.md C2). */
        fun estadoInicial(agora: Instant): NovaTarefaEstado = NovaTarefaEstado(
            formulario = FormularioTarefa(prazo = prazoDoAtalho(AtalhoDePrazo.Amanha20h, agora)),
            agora = agora,
            atalhos = atalhosValidos(agora),
        )
    }
}
