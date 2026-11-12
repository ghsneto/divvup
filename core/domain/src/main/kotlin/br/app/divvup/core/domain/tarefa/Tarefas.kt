package br.app.divvup.core.domain.tarefa

import br.app.divvup.core.domain.casa.FUSO_DA_TEMPORADA
import br.app.divvup.core.model.Categoria
import br.app.divvup.core.model.Peso
import br.app.divvup.core.model.SituacaoDaTarefa
import br.app.divvup.core.model.Tarefa
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

const val TITULO_MAXIMO = 60
const val DESCRICAO_MAXIMA = 280

/** R-20: no máximo 5 assumidas e não concluídas por morador. */
const val LIMITE_DE_ABERTAS = 5

/** Com menos de 24 h para o prazo: selo "urgente" (C1) e aviso de atribuição automática (C2, R-26). */
val JANELA_URGENTE: Duration = Duration.ofHours(24)

/** R-14: ocorrências `OPEN`, prazo mais próximo primeiro e, no empate, maior peso. */
fun ordenarDeck(tarefas: List<Tarefa>): List<Tarefa> =
    tarefas.filter { it.situacao == SituacaoDaTarefa.OPEN }
        .sortedWith(compareBy<Tarefa> { it.prazo }.thenByDescending { it.pontos })

/** Assumidas por [uid] e ainda não concluídas (indicador "3/5" de C1, R-20). */
fun abertasDe(uid: String, tarefas: List<Tarefa>): Int =
    tarefas.count { it.donoId == uid && (it.situacao == SituacaoDaTarefa.ASSIGNED || it.situacao == SituacaoDaTarefa.LATE) }

fun urgente(prazo: Instant, agora: Instant): Boolean = Duration.between(agora, prazo) < JANELA_URGENTE

/** Peso de uma ocorrência pelos pontos (25/50/100). */
fun pesoDe(pontos: Int): Peso = Peso.entries.firstOrNull { it.pontos == pontos } ?: Peso.MEDIA

/** Dia do prazo como as telas falam dele: "hoje", "amanhã", "sáb.", "20/11". */
sealed interface DiaDoPrazo {
    data object Hoje : DiaDoPrazo
    data object Amanha : DiaDoPrazo
    data object Ontem : DiaDoPrazo

    /** Até 6 dias para a frente ou para trás. 1 = segunda … 7 = domingo. */
    data class NaSemana(val diaDaSemana: Int) : DiaDoPrazo
    data class Data(val dia: Int, val mes: Int) : DiaDoPrazo
}

data class PrazoFalado(val dia: DiaDoPrazo, val hora: Int, val minuto: Int)

fun prazoFalado(prazo: Instant, agora: Instant, fuso: ZoneId = FUSO_DA_TEMPORADA): PrazoFalado {
    val quando = prazo.atZone(fuso)
    val dias = ChronoUnit.DAYS.between(agora.atZone(fuso).toLocalDate(), quando.toLocalDate())
    val dia = when {
        dias == 0L -> DiaDoPrazo.Hoje
        dias == 1L -> DiaDoPrazo.Amanha
        dias == -1L -> DiaDoPrazo.Ontem
        dias in -SEMANA + 1..SEMANA - 1 -> DiaDoPrazo.NaSemana(quando.dayOfWeek.value)
        else -> DiaDoPrazo.Data(quando.dayOfMonth, quando.monthValue)
    }
    return PrazoFalado(dia, quando.hour, quando.minute)
}

/** Atalhos de prazo de C2: hoje 22h, amanhã 20h (padrão) e o próximo sábado ao meio-dia. */
enum class AtalhoDePrazo { Hoje22h, Amanha20h, Sabado12h }

fun prazoDoAtalho(atalho: AtalhoDePrazo, agora: Instant, fuso: ZoneId = FUSO_DA_TEMPORADA): ZonedDateTime {
    val hoje = agora.atZone(fuso).toLocalDate()
    return when (atalho) {
        AtalhoDePrazo.Hoje22h -> hoje.atTime(HORA_HOJE, 0)
        AtalhoDePrazo.Amanha20h -> hoje.plusDays(1).atTime(HORA_AMANHA, 0)
        AtalhoDePrazo.Sabado12h -> hoje.plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).atTime(HORA_SABADO, 0)
    }.atZone(fuso)
}

/** Atalhos ainda no futuro (depois das 22h, "hoje 22h" some). */
fun atalhosValidos(agora: Instant, fuso: ZoneId = FUSO_DA_TEMPORADA): List<AtalhoDePrazo> =
    AtalhoDePrazo.entries.filter { prazoDoAtalho(it, agora, fuso).toInstant().isAfter(agora) }

/** C2: prazo no futuro. */
fun prazoValido(data: LocalDate, hora: LocalTime, agora: Instant, fuso: ZoneId = FUSO_DA_TEMPORADA): Boolean =
    data.atTime(hora).atZone(fuso).toInstant().isAfter(agora)

/** Formulário de C2. */
data class FormularioTarefa(
    val titulo: String = "",
    val descricao: String = "",
    val categoria: Categoria = Categoria.COZINHA,
    val peso: Peso = Peso.MEDIA,
    val prazo: ZonedDateTime,
) {
    val podeCriar: Boolean
        get() = titulo.trim().isNotEmpty() && titulo.trim().length <= TITULO_MAXIMO && descricao.length <= DESCRICAO_MAXIMA
}

/** Falhas das operações de tarefa. */
sealed interface ErroDeTarefa {
    data object PrazoNoPassado : ErroDeTarefa

    /** R-15: outra pessoa assumiu antes ("alguém já pegou essa"). */
    data class JaAssumida(val porQuem: String) : ErroDeTarefa

    /** R-20: já tem 5 assumidas abertas. */
    data object Limite : ErroDeTarefa

    /** R-18: passou da janela de desfazer. */
    data object NaoDaParaDesfazer : ErroDeTarefa

    /** R-19: faltam 24 h ou menos, não dá mais para devolver. */
    data object PrazoCurto : ErroDeTarefa

    /** R-12: sem permissão, ou a tarefa já tem dono / não é mais sua. */
    data object SemPermissao : ErroDeTarefa
    data object SemConexao : ErroDeTarefa
    data object Desconhecido : ErroDeTarefa
}

sealed interface ResultadoTarefa<out T> {
    data class Ok<T>(val valor: T) : ResultadoTarefa<T>
    data class Falha(val erro: ErroDeTarefa) : ResultadoTarefa<Nothing>
}

/** Carregando, carregado ou falhou (C1 `DeckCarregando` / `DeckErro`). */
sealed interface EstadoDasTarefas {
    data object Carregando : EstadoDasTarefas
    data class Carregadas(val tarefas: List<Tarefa>, val doCache: Boolean = false) : EstadoDasTarefas
    data object Falhou : EstadoDasTarefas
}

/** Tarefas da casa atual. Escritas pelo backend; leituras em tempo real pelo Firestore. */
interface RepositorioDeTarefas {
    /** Ocorrências ainda em jogo (`OPEN`, `ASSIGNED`, `LATE`). */
    val tarefas: Flow<EstadoDasTarefas>

    /** C2 (R-09): devolve o id da ocorrência. */
    suspend fun criar(formulario: FormularioTarefa): ResultadoTarefa<String>

    /** Passes do usuário na casa atual: id da tarefa → quando expira (R-16). */
    val passes: Flow<Map<String, Instant>>

    /** Swipe à direita (R-15, R-20). */
    suspend fun assumir(tarefaId: String): ResultadoTarefa<Unit>

    /** Swipe à esquerda (R-16). */
    suspend fun passar(tarefaId: String): ResultadoTarefa<Unit>

    /** "Desfazer" do último swipe (R-18). */
    suspend fun desfazer(tarefaId: String, acao: Swipe): ResultadoTarefa<Unit>

    /** "Rever" as passadas (R-17). */
    suspend fun reverPassadas(): ResultadoTarefa<Unit>

    /** C5: todas as tarefas que já foram do usuário na casa atual (abertas e concluídas). */
    val minhas: Flow<List<Tarefa>>

    /** C3: uma ocorrência em tempo real; `null` se não existe (ex.: foi excluída). */
    fun tarefa(id: String): Flow<Tarefa?>

    /** C3/C5 "Concluir" (R-21, R-22). */
    suspend fun concluir(id: String): ResultadoTarefa<ResultadoDaConclusao>

    /** C3 "Devolver" (R-19). */
    suspend fun devolver(id: String): ResultadoTarefa<Unit>

    /** C3 "Cancelar tarefa" (admin, R-12). */
    suspend fun cancelar(id: String): ResultadoTarefa<Unit>

    /** C2 em modo edição (R-12). */
    suspend fun editar(id: String, formulario: FormularioTarefa): ResultadoTarefa<Unit>

    /** C2 "Excluir tarefa" (R-12). */
    suspend fun excluir(id: String): ResultadoTarefa<Unit>
}

/** Direção do swipe. */
enum class Swipe { ASSUMIR, PASSAR }

/** R-14: o deck tira as tarefas que a pessoa passou e cujo passe ainda vale. */
fun deckSemPassadas(tarefas: List<Tarefa>, passes: Map<String, Instant>, agora: Instant): List<Tarefa> =
    ordenarDeck(tarefas).filterNot { t -> passes[t.id]?.isAfter(agora) == true }

/** R-17: as que a pessoa passou e ainda estão abertas ("você passou X tarefas"). */
fun passadasAbertas(tarefas: List<Tarefa>, passes: Map<String, Instant>, agora: Instant): List<Tarefa> =
    ordenarDeck(tarefas).filter { t -> passes[t.id]?.isAfter(agora) == true }

private const val SEMANA = 7L
private const val HORA_HOJE = 22
private const val HORA_AMANHA = 20
private const val HORA_SABADO = 12
