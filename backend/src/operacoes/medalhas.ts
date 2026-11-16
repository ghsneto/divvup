import type { Documento } from "../firestore.js";

/** R-30: medalhas da v1.0. */
export const MEDALHAS = ["FIRST_TASK", "STREAK_7", "CHAMPION", "TASKS_100", "LIFESAVER"] as const;
export type Medalha = (typeof MEDALHAS)[number];

/** `users/{uid}.badges` (copiado em `members/{uid}.badges` para D3): vezes que ganhou e a primeira vez. */
export type Medalhas = Partial<Record<Medalha, { count: number; firstAt: Date }>>;

/** Salva-vidas: assumiu pelo swipe a menos de 48 h do prazo. */
export const JANELA_SALVA_VIDAS_MS = 48 * 60 * 60 * 1000;
const SETE_DIAS = 7;
const CEM_TAREFAS = 100;

export function lerMedalhas(doc: Documento | undefined): Medalhas {
  const m = doc?.["badges"];
  // Perfis antigos guardavam `badges: []`.
  return m && typeof m === "object" && !Array.isArray(m) ? (m as Medalhas) : {};
}

/** Soma uma conquista; devolve as medalhas novas e se foi a primeira vez. */
export function ganhar(atuais: Medalhas, id: Medalha, agora: Date): { medalhas: Medalhas; primeira: boolean } {
  const antes = atuais[id];
  return { medalhas: { ...atuais, [id]: { count: (antes?.count ?? 0) + 1, firstAt: antes?.firstAt ?? agora } }, primeira: !antes };
}

/**
 * Medalhas de uma conclusão (R-30): primeira tarefa, 100 tarefas, 7 dias seguidos (R-31) e
 * salva-vidas. Devolve o mapa atualizado e a primeira medalha inédita (C4 mostra "medalha nova").
 */
export function medalhasDaConclusao(
  atuais: Medalhas,
  c: { tarefasFeitas: number; sequencia: number; sequenciaAntes: number; via: unknown; assumidaEm: Date | null; prazo: Date },
  agora: Date,
): { medalhas: Medalhas; nova: Medalha | null } {
  let medalhas = atuais;
  let nova: Medalha | null = null;
  const somar = (id: Medalha) => {
    const r = ganhar(medalhas, id, agora);
    medalhas = r.medalhas;
    if (r.primeira && !nova) nova = id;
  };
  // Ordem de destaque em C4 quando sai mais de uma de uma vez.
  if (c.tarefasFeitas === 1) somar("FIRST_TASK");
  if (c.tarefasFeitas === CEM_TAREFAS) somar("TASKS_100");
  if (c.sequencia >= SETE_DIAS && c.sequenciaAntes < SETE_DIAS) somar("STREAK_7");
  if (c.via === "SWIPE" && c.assumidaEm && c.prazo.getTime() - c.assumidaEm.getTime() < JANELA_SALVA_VIDAS_MS) somar("LIFESAVER");
  return { medalhas, nova };
}
