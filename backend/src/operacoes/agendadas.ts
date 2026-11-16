import type { Escrita, Firestore } from "../firestore.js";

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

/** R-26: a 24 h do prazo, ocorrência sem dono vai para alguém. */
export const JANELA_AUTOMATICA_MS = 24 * 60 * 60 * 1000;

/**
 * Rotina agendada (R-26): ocorrência `OPEN` a 24 h ou menos do prazo vai para o morador ativo com
 * menos pontos na temporada; empate por sorteio. Ignora o limite de 5 (R-20). Grava
 * `assignedVia = AUTO`, que o app mostra como "atribuída a você" (C5).
 */
export async function atribuirAutomaticamente(
  db: Firestore,
  agora: Date = new Date(),
  sortear: (n: number) => number = (n) => Math.floor(Math.random() * n),
): Promise<{ atribuidas: number }> {
  const limite = new Date(agora.getTime() + JANELA_AUTOMATICA_MS);
  const perto = await db.consultarGrupo(
    "tasks",
    [
      { campo: "status", op: "EQUAL", valor: "OPEN" },
      { campo: "dueAt", op: "LESS_THAN_OR_EQUAL", valor: limite },
    ],
    LOTE,
  );
  let atribuidas = 0;
  for (const { caminho } of perto) {
    const casaId = caminho.split("/")[1];
    const mudou = await db.transacao(async (tx) => {
      const [tarefa] = await db.lerVarios([caminho], tx);
      if (tarefa?.["status"] !== "OPEN" || (tarefa["dueAt"] as Date).getTime() > limite.getTime()) return { escritas: [], resultado: false };
      const ativos = (await db.listar(`houses/${casaId}/members`, tx)).filter((m) => m.dados["status"] === "ACTIVE");
      if (ativos.length === 0) return { escritas: [], resultado: false };
      const pontos = (m: (typeof ativos)[number]) => (m.dados["seasonPoints"] as number | undefined) ?? 0;
      const menor = Math.min(...ativos.map(pontos));
      const empatados = ativos.filter((m) => pontos(m) === menor).sort((a, b) => (a.uid < b.uid ? -1 : a.uid > b.uid ? 1 : 0));
      const escolhido = empatados[sortear(empatados.length)] ?? empatados[0];
      if (!escolhido) return { escritas: [], resultado: false };
      const escritas: Escrita[] = [
        {
          tipo: "gravar",
          caminho,
          dados: { status: "ASSIGNED", assigneeId: escolhido.uid, assignedVia: "AUTO", assignedAt: agora },
          campos: ["status", "assigneeId", "assignedVia", "assignedAt"],
          exigirExistente: true,
        },
        {
          tipo: "gravar",
          caminho: `houses/${casaId}/members/${escolhido.uid}`,
          dados: { openTasks: ((escolhido.dados["openTasks"] as number | undefined) ?? 0) + 1 },
          campos: ["openTasks"],
          exigirExistente: true,
        },
      ];
      return { escritas, resultado: true };
    });
    if (mudou) atribuidas++;
  }
  return { atribuidas };
}

/**
 * Tudo o que roda a cada disparo do Cron Trigger. A atribuição vem antes: uma aberta já vencida
 * ganha dono e, na passada seguinte, vira atrasada.
 */
export async function rotinaAgendada(db: Firestore, agora: Date = new Date()) {
  return { ...(await atribuirAutomaticamente(db, agora)), ...(await marcarAtrasadas(db, agora)) };
}
