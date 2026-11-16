package br.app.divvup.core.model

/** Medalhas da v1.0 (R-30); `id` é a chave em `badges` no Firestore. */
enum class Medalha(val id: String) {
    PRIMEIRA_TAREFA("FIRST_TASK"),
    SETE_DIAS("STREAK_7"),
    CAMPEAO("CHAMPION"),
    CEM_TAREFAS("TASKS_100"),
    SALVA_VIDAS("LIFESAVER"),
    ;

    companion object {
        fun deId(id: String?): Medalha? = entries.firstOrNull { it.id == id }
    }
}
