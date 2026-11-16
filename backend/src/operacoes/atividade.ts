import type { Documento, Escrita } from "../firestore.js";

/**
 * Feed da casa (`houses/{id}/activity`, Histórico de D1, R-25). Cada evento leva nome, avatar,
 * título e categoria copiados no momento, para o feed não depender de outras leituras.
 */
export type TipoDeEvento = "COMPLETED" | "ASSIGNED" | "AUTO_ASSIGNED" | "JOINED" | "LEFT";

export function evento(casaId: string, id: string, tipo: TipoDeEvento, autor: { uid: string; dados: Documento }, extra: Documento, agora: Date): Extract<Escrita, { tipo: "gravar" }> {
  return {
    tipo: "gravar",
    caminho: `houses/${casaId}/activity/${id}`,
    dados: {
      type: tipo,
      actorId: autor.uid,
      actorName: (autor.dados["displayName"] as string | null | undefined) ?? "",
      actorAvatar: (autor.dados["avatarId"] as string | null | undefined) ?? null,
      createdAt: agora,
      ...extra,
    },
  };
}

/** Dados da tarefa que o feed mostra. */
export function daTarefa(tarefaId: string, tarefa: Documento): Documento {
  return { taskId: tarefaId, taskTitle: (tarefa["title"] as string | undefined) ?? "", category: (tarefa["category"] as string | undefined) ?? "OUTROS" };
}

/** Evento de "assumiu" tem id fixo por tarefa, para o "Desfazer" (R-18) poder apagá-lo. */
export const idDeAssumiu = (tarefaId: string) => `assumiu-${tarefaId}`;
