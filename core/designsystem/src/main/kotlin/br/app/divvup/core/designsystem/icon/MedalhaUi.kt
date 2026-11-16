package br.app.divvup.core.designsystem.icon

import androidx.annotation.StringRes
import br.app.divvup.core.designsystem.R
import br.app.divvup.core.model.Medalha

/** Ícone, nome e critério de cada medalha (R-30; textos de E1 `Perfil`). */
val Medalha.icone: DivvupIcone
    get() = when (this) {
        Medalha.PRIMEIRA_TAREFA -> DivvupIcone.Check
        Medalha.SETE_DIAS -> DivvupIcone.Raio
        Medalha.CAMPEAO -> DivvupIcone.Trofeu
        Medalha.CEM_TAREFAS -> DivvupIcone.Medalha
        Medalha.SALVA_VIDAS -> DivvupIcone.Relogio
    }

@get:StringRes
val Medalha.nome: Int
    get() = when (this) {
        Medalha.PRIMEIRA_TAREFA -> R.string.medalha_primeira_tarefa
        Medalha.SETE_DIAS -> R.string.medalha_sete_dias
        Medalha.CAMPEAO -> R.string.medalha_campeao
        Medalha.CEM_TAREFAS -> R.string.medalha_cem_tarefas
        Medalha.SALVA_VIDAS -> R.string.medalha_salva_vidas
    }

/** O que a pessoa fez para ganhar (C4 "medalha nova"). */
@get:StringRes
val Medalha.conquista: Int
    get() = when (this) {
        Medalha.PRIMEIRA_TAREFA -> R.string.medalha_primeira_tarefa_conquista
        Medalha.SETE_DIAS -> R.string.medalha_sete_dias_conquista
        Medalha.CAMPEAO -> R.string.medalha_campeao_conquista
        Medalha.CEM_TAREFAS -> R.string.medalha_cem_tarefas_conquista
        Medalha.SALVA_VIDAS -> R.string.medalha_salva_vidas_conquista
    }

/** Critério para ganhar (E1, ao tocar numa bloqueada). */
@get:StringRes
val Medalha.criterio: Int
    get() = when (this) {
        Medalha.PRIMEIRA_TAREFA -> R.string.medalha_primeira_tarefa_criterio
        Medalha.SETE_DIAS -> R.string.medalha_sete_dias_criterio
        Medalha.CAMPEAO -> R.string.medalha_campeao_criterio
        Medalha.CEM_TAREFAS -> R.string.medalha_cem_tarefas_criterio
        Medalha.SALVA_VIDAS -> R.string.medalha_salva_vidas_criterio
    }

/** Nome na grade de E1 ("sequência de 7 dias"); nos chips de D3 fica o curto. */
@get:StringRes
val Medalha.nomeLongo: Int
    get() = if (this == Medalha.SETE_DIAS) R.string.medalha_sete_dias_longo else nome
