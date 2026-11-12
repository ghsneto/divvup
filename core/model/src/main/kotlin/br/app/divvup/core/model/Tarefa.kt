package br.app.divvup.core.model

import java.time.Instant

/** R-11: categorias fixas, na ordem do desenho de C2. */
enum class Categoria { COZINHA, LIMPEZA_GERAL, BANHEIRO, LAVANDERIA, LIXO, COMPRAS, PETS, PLANTAS, MANUTENCAO, OUTROS }

/** R-10: peso → pontos. */
enum class Peso(val pontos: Int) { LEVE(25), MEDIA(50), PESADA(100) }

/** Como a tarefa chegou ao dono (`assignedVia`). */
enum class ViaDeAtribuicao { SWIPE, AUTO }

/** Situação da ocorrência (`tasks/{id}.status`). */
enum class SituacaoDaTarefa { OPEN, ASSIGNED, DONE, LATE, CANCELLED }

/** Ocorrência de tarefa (`houses/{casa}/tasks/{id}`). */
data class Tarefa(
    val id: String,
    val titulo: String,
    val descricao: String?,
    val categoria: Categoria,
    val pontos: Int,
    val prazo: Instant,
    val situacao: SituacaoDaTarefa,
    val donoId: String?,
    val criadaPor: String,
    val criadaPorNome: String,
    val criadaPorAvatar: String?,
    val criadaEm: Instant? = null,
    val via: ViaDeAtribuicao? = null,
    val concluidaEm: Instant? = null,
    val pontosGanhos: Int? = null,
)
