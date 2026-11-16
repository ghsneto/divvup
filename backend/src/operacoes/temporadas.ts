import type { Documento, Escrita, Firestore } from "../firestore.js";

const FUSO = "America/Sao_Paulo";

/** Mesmo texto do `wrangler.toml`: 00:00 de Brasília (UTC-3, sem horário de verão) no dia 1º. */
export const CRON_TEMPORADA = "0 3 1 * *";

/** Mês (yyyy-MM) de um instante no fuso de Brasília. */
export function mesEmBrasilia(d: Date): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone: FUSO, year: "numeric", month: "2-digit" }).format(d).slice(0, 7);
}

/** A temporada que termina quando a rotina roda à 00:00 do dia 1º: o mês anterior (R-27). */
export function temporadaQueTermina(agora: Date): string {
  const [ano, mes] = mesEmBrasilia(agora).split("-").map(Number) as [number, number];
  return mes === 1 ? `${ano - 1}-12` : `${ano}-${String(mes - 1).padStart(2, "0")}`;
}

export interface Colocacao {
  uid: string;
  name: string;
  avatarId: string | null;
  points: number;
}

/** Ranking final: mais pontos primeiro; empate por nome. Campeões: todos com o maior placar (> 0). */
export function rankingFinal(membros: { uid: string; dados: Documento }[]): { ranking: Colocacao[]; campeoes: Colocacao[] } {
  const ranking = membros
    .filter((m) => m.dados["status"] === "ACTIVE")
    .map((m) => ({
      uid: m.uid,
      name: (m.dados["displayName"] as string | undefined) ?? "",
      avatarId: (m.dados["avatarId"] as string | null | undefined) ?? null,
      points: (m.dados["seasonPoints"] as number | undefined) ?? 0,
    }))
    .sort((a, b) => b.points - a.points || a.name.localeCompare(b.name, "pt-BR"));
  const topo = ranking[0]?.points ?? 0;
  return { ranking, campeoes: topo > 0 ? ranking.filter((c) => c.points === topo) : [] };
}

/**
 * Virada de temporada (R-27), à 00:00 do dia 1º em Brasília: para cada casa, congela o ranking em
 * `houses/{id}/seasons/{yyyy-MM}` com o campeão e zera `seasonPoints` dos moradores. Idempotente:
 * casa que já tem a temporada gravada é pulada. A medalha do campeão entra com as medalhas (#25).
 */
export async function virarTemporada(db: Firestore, agora: Date = new Date()): Promise<{ temporada: string; casas: number }> {
  const temporada = temporadaQueTermina(agora);
  const casas = await db.listar("houses");
  let viradas = 0;
  for (const casa of casas) {
    const virou = await db.transacao(async (tx) => {
      const [jaTem] = await db.lerVarios([`houses/${casa.uid}/seasons/${temporada}`], tx);
      if (jaTem) return { escritas: [], resultado: false };
      const membros = await db.listar(`houses/${casa.uid}/members`, tx);
      const { ranking, campeoes } = rankingFinal(membros);
      const campeao = campeoes[0];
      const escritas: Escrita[] = [
        {
          tipo: "gravar",
          caminho: `houses/${casa.uid}/seasons/${temporada}`,
          dados: {
            standings: ranking.map((c) => ({ ...c })),
            championId: campeao?.uid ?? null,
            championIds: campeoes.map((c) => c.uid),
            championName: campeao?.name ?? null,
            championAvatar: campeao?.avatarId ?? null,
            championPoints: campeao?.points ?? 0,
            closedAt: agora,
          },
          exigirInexistente: true,
        },
        ...membros
          .map((m): Escrita => ({
            tipo: "gravar",
            caminho: `houses/${casa.uid}/members/${m.uid}`,
            dados: { seasonPoints: 0, seasonTasks: 0, seasonCategories: {} },
            campos: ["seasonPoints", "seasonTasks", "seasonCategories"],
            exigirExistente: true,
          })),
        // Mês novo, placar zerado: equilíbrio volta a 100 e a temporada atual avança.
        { tipo: "gravar", caminho: `houses/${casa.uid}`, dados: { balanceIndex: 100, currentSeason: mesEmBrasilia(agora) }, campos: ["balanceIndex", "currentSeason"], exigirExistente: true },
      ];
      return { escritas, resultado: true };
    });
    if (virou) viradas++;
  }
  return { temporada, casas: viradas };
}
