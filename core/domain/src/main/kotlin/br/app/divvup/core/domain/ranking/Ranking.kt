package br.app.divvup.core.domain.ranking

import br.app.divvup.core.model.Evento
import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.Temporada
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs
import kotlin.math.roundToInt

/** Dados do D1/D2/D3 da casa atual, em tempo real. */
interface RepositorioDeRanking {
    /** Todos que já moraram na casa, inclusive ex-moradores (`status = LEFT`, R-29). */
    val moradores: Flow<List<Membro>>

    /** Histórico, mais novo primeiro (últimos eventos). */
    val historico: Flow<List<Evento>>

    /** Temporadas encerradas, mais recente primeiro (D2). */
    val temporadas: Flow<List<Temporada>>
}

private const val CEM = 100

/**
 * Índice de Equilíbrio (R-45): 100 menos o coeficiente de Gini normalizado (× n/(n−1)) dos pontos
 * da temporada, em pontos percentuais. 1 morador ou ninguém com pontos: 100. Mesma conta do
 * backend (`backend/src/operacoes/equilibrio.ts`), que grava `houses.balanceIndex`.
 */
fun indiceDeEquilibrio(pontos: List<Int>): Int {
    val n = pontos.size
    val total = pontos.sum()
    if (n < 2 || total <= 0) return CEM
    var diferencas = 0L
    for (a in pontos) for (b in pontos) diferencas += abs(a - b)
    val gini = diferencas.toDouble() / (2.0 * n * total)
    return (CEM * (1 - gini * n / (n - 1))).roundToInt()
}

/** Frase do medidor de D1. */
enum class FraseDoEquilibrio { BemDividido, QuaseEquilibrado, Desequilibrado }

private const val BEM_DIVIDIDO = 75
private const val QUASE = 50

fun fraseDoEquilibrio(indice: Int): FraseDoEquilibrio = when {
    indice >= BEM_DIVIDIDO -> FraseDoEquilibrio.BemDividido
    indice >= QUASE -> FraseDoEquilibrio.QuaseEquilibrado
    else -> FraseDoEquilibrio.Desequilibrado
}

/** Bloco "equilíbrio" de D1: índice, frase e quem fez a maior parte ("Ana fez 35% dos pontos"). */
data class Equilibrio(val indice: Int, val frase: FraseDoEquilibrio, val lider: Membro?, val percentualDoLider: Int)

fun equilibrioDoMes(moradores: List<Membro>): Equilibrio {
    val ativos = moradores.filter { it.ativo }
    val indice = indiceDeEquilibrio(ativos.map { it.pontosDoMes })
    val total = ativos.sumOf { it.pontosDoMes }
    val lider = ativos.maxByOrNull { it.pontosDoMes }?.takeIf { total > 0 }
    val percentual = if (lider == null) 0 else (lider.pontosDoMes * CEM.toDouble() / total).roundToInt()
    return Equilibrio(indice, fraseDoEquilibrio(indice), lider, percentual)
}

/** Linha de ranking com a posição (empate divide a posição: 1º, 1º, 3º). */
data class Posicao(val posicao: Int, val membro: Membro, val pontos: Int, val tarefas: Int)

private fun posicoes(lista: List<Membro>, pontos: (Membro) -> Int, tarefas: (Membro) -> Int): List<Posicao> {
    val ordenada = lista.sortedWith(compareByDescending<Membro> { pontos(it) }.thenBy { it.nome.lowercase() })
    return ordenada.map { m -> Posicao(1 + ordenada.count { pontos(it) > pontos(m) }, m, pontos(m), tarefas(m)) }
}

/** D1 "Este mês": só moradores ativos (R-29), pontos da temporada. */
fun rankingDoMes(moradores: List<Membro>): List<Posicao> =
    posicoes(moradores.filter { it.ativo }, { it.pontosDoMes }, { it.tarefasDoMes })

/** D1 "Geral": acumulado na casa, com ex-moradores (R-28, R-29). */
fun rankingGeral(moradores: List<Membro>): List<Posicao> =
    posicoes(moradores.filter { it.ativo || it.pontosTotais > 0 }, { it.pontosTotais }, { it.tarefasTotais })
