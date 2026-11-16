import type { Firestore } from "../firestore.js";

/** Quantas ocorrências cada passada da rotina trata no máximo (o resto fica para a próxima). */
export const LOTE = 200;

/**
 * Rotina agendada (Cron Trigger): ocorrência assumida que passou do prazo sem conclusão vira
 * `LATE` (R-23). Cada uma muda numa transação própria, que confere de novo o estado: se o dono
 * concluiu ou devolveu no meio do caminho, nada muda. Os pontos pela metade saem na conclusão (R-22).
 */
export async function marcarAtrasadas(db: Firestore, agora: Date = new Date()): Promise<{ atrasadas: number }> {
  const vencidas = await db.consultarGrupo(
    "tasks",
    [
      { campo: "status", op: "EQUAL", valor: "ASSIGNED" },
      { campo: "dueAt", op: "LESS_THAN", valor: agora },
    ],
    LOTE,
  );
  let atrasadas = 0;
  for (const { caminho } of vencidas) {
    const mudou = await db.transacao(async (tx) => {
      const [tarefa] = await db.lerVarios([caminho], tx);
      const vencida = tarefa?.["status"] === "ASSIGNED" && (tarefa["dueAt"] as Date).getTime() < agora.getTime();
      if (!vencida) return { escritas: [], resultado: false };
      return {
        escritas: [{ tipo: "gravar" as const, caminho, dados: { status: "LATE", lateAt: agora }, campos: ["status", "lateAt"], exigirExistente: true }],
        resultado: true,
      };
    });
    if (mudou) atrasadas++;
  }
  return { atrasadas };
}

/** Tudo o que roda a cada disparo do Cron Trigger. */
export async function rotinaAgendada(db: Firestore, agora: Date = new Date()) {
  return { ...(await marcarAtrasadas(db, agora)) };
}
