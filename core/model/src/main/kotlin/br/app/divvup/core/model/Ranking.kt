package br.app.divvup.core.model

import java.time.Instant

/** Tipos de evento do Histórico (`houses/{id}/activity.type`, R-25). */
enum class TipoDeEvento { COMPLETED, ASSIGNED, AUTO_ASSIGNED, JOINED, LEFT }

/** Linha do Histórico de D1: "Ana concluiu Lavar a louça · +50". */
data class Evento(
    val id: String,
    val tipo: TipoDeEvento,
    val autorId: String,
    val autorNome: String,
    val autorAvatar: String?,
    val quando: Instant,
    val tarefaTitulo: String? = null,
    val categoria: Categoria? = null,
    val pontos: Int? = null,
    /** Concluída depois do prazo (meia pontuação, R-22). */
    val atrasada: Boolean = false,
)

/** Colocação no ranking congelado de uma temporada. */
data class Colocacao(val uid: String, val nome: String, val avatarId: String?, val pontos: Int)

/** Temporada encerrada (`houses/{id}/seasons/{yyyy-MM}`, R-27). */
data class Temporada(
    /** `yyyy-MM`. */
    val id: String,
    val colocacoes: List<Colocacao>,
    val campeaoId: String?,
) {
    val campeao: Colocacao? get() = colocacoes.firstOrNull { it.uid == campeaoId }
}
