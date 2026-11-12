package br.app.divvup.core.domain.tarefa

import br.app.divvup.core.domain.casa.FUSO_DA_TEMPORADA
import br.app.divvup.core.model.SituacaoDaTarefa
import br.app.divvup.core.model.Tarefa
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** R-19: só devolve com mais de 24 h para o prazo. */
val JANELA_PARA_DEVOLVER: Duration = Duration.ofHours(24)

/** C5: concluídas recentes = últimos 7 dias. */
val JANELA_DE_CONCLUIDAS: Duration = Duration.ofDays(7)

/** R-22: integral no prazo, metade (para baixo) depois; nunca negativo. Igual ao backend. */
fun pontosDaConclusao(pontos: Int, prazo: Instant, quando: Instant): Int =
    maxOf(if (!quando.isAfter(prazo)) pontos else pontos / 2, 0)

/** Situação da tarefa do ponto de vista de quem olha (chip e ações de C3, TELAS.md). */
enum class SituacaoParaMim { Aberta, Sua, SuaPerto, Atrasada, DeOutra, Concluida, Cancelada }

fun situacaoParaMim(tarefa: Tarefa, eu: String?, agora: Instant): SituacaoParaMim = when (tarefa.situacao) {
    SituacaoDaTarefa.OPEN -> SituacaoParaMim.Aberta
    SituacaoDaTarefa.DONE -> SituacaoParaMim.Concluida
    SituacaoDaTarefa.CANCELLED -> SituacaoParaMim.Cancelada
    SituacaoDaTarefa.ASSIGNED, SituacaoDaTarefa.LATE -> when {
        tarefa.donoId != eu -> SituacaoParaMim.DeOutra
        tarefa.situacao == SituacaoDaTarefa.LATE || agora.isAfter(tarefa.prazo) -> SituacaoParaMim.Atrasada
        Duration.between(agora, tarefa.prazo) <= JANELA_PARA_DEVOLVER -> SituacaoParaMim.SuaPerto
        else -> SituacaoParaMim.Sua
    }
}

/** R-12: editar/excluir é do autor ou de admin, e só enquanto a tarefa não tem dono. */
fun podeEditar(tarefa: Tarefa, eu: String?, souAdmin: Boolean): Boolean =
    tarefa.situacao == SituacaoDaTarefa.OPEN && (tarefa.criadaPor == eu || souAdmin)

/** Seções de C5 (TELAS.md): atrasadas, para hoje, próximas e concluídas nos últimos 7 dias. */
data class SecoesDasMinhas(
    val atrasadas: List<Tarefa>,
    val hoje: List<Tarefa>,
    val proximas: List<Tarefa>,
    val concluidas: List<Tarefa>,
) {
    val abertas: Int get() = atrasadas.size + hoje.size + proximas.size
    val pontosConcluidas: Int get() = concluidas.sumOf { it.pontosGanhos ?: 0 }
}

fun secoesDasMinhas(minhas: List<Tarefa>, agora: Instant, fuso: ZoneId = FUSO_DA_TEMPORADA): SecoesDasMinhas {
    val hoje = agora.atZone(fuso).toLocalDate()
    val abertas = minhas.filter { it.situacao == SituacaoDaTarefa.ASSIGNED || it.situacao == SituacaoDaTarefa.LATE }.sortedBy { it.prazo }
    val (atrasadas, emDia) = abertas.partition { it.situacao == SituacaoDaTarefa.LATE || agora.isAfter(it.prazo) }
    val (deHoje, proximas) = emDia.partition { it.prazo.atZone(fuso).toLocalDate() == hoje }
    val concluidas = minhas
        .filter { it.situacao == SituacaoDaTarefa.DONE && it.concluidaEm?.isAfter(agora.minus(JANELA_DE_CONCLUIDAS)) == true }
        .sortedByDescending { it.concluidaEm }
    return SecoesDasMinhas(atrasadas, deHoje, proximas, concluidas)
}

/** O que C4 mostra depois de concluir (vem do backend). */
data class ResultadoDaConclusao(
    val pontos: Int,
    val noPrazo: Boolean,
    val posicaoAntes: Int,
    val posicaoDepois: Int,
    /** Nome de quem a pessoa passou no ranking do mês, se passou alguém. */
    val ultrapassou: String?,
    val sequencia: Int,
)
