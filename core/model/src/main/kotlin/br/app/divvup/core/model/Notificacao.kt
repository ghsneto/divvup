package br.app.divvup.core.model

import java.time.Instant

/** Tipos de aviso da tabela R-33 (`users/{uid}/notifications.type`). */
enum class TipoDeNotificacao { AUTO_ASSIGNED, DUE_24H, DUE_2H, LATE, NEW_TASK, SEASON_END, MEMBER_COMPLETED, REMOVED }

/** Aviso da central F1 (R-37). */
data class Notificacao(
    val id: String,
    val tipo: TipoDeNotificacao,
    val titulo: String,
    val corpo: String,
    val lida: Boolean,
    val quando: Instant,
    val tarefaId: String? = null,
)
