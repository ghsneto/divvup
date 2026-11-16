import { beforeEach, describe, expect, it } from "vitest";
import { Firestore } from "../src/firestore.js";
import worker from "../src/index.js";
import { CRON_TEMPORADA, mesEmBrasilia, rankingFinal, temporadaQueTermina, virarTemporada } from "../src/operacoes/temporadas.js";
import { criarUsuario, envLocal, limparFirestore } from "./ambiente.js";

const chamar = (rota: string, token: string, corpo: unknown = {}) =>
  worker.fetch(
    new Request(`http://local/v1/${rota}`, {
      method: "POST",
      headers: { authorization: `Bearer ${token}`, "content-type": "application/json" },
      body: JSON.stringify(corpo),
    }),
    envLocal,
  );
const json = async (r: Response) => (await r.json()) as Record<string, unknown>;

describe("temporada (R-27): datas e ranking", () => {
  it("à 00:00 do dia 1º em Brasília fecha o mês anterior", () => {
    const virada = new Date("2026-12-01T03:00:00Z");
    expect(mesEmBrasilia(virada)).toBe("2026-12");
    expect(temporadaQueTermina(virada)).toBe("2026-11");
    expect(temporadaQueTermina(new Date("2027-01-01T03:00:00Z"))).toBe("2026-12");
  });

  it("campeões são todos os empatados no topo; sem pontos, sem campeão", () => {
    const m = (uid: string, nome: string, pts: number, status = "ACTIVE") => ({ uid, dados: { displayName: nome, seasonPoints: pts, status } });
    const { ranking, campeoes } = rankingFinal([m("b", "Bruno", 100), m("a", "Ana", 100), m("c", "Caio", 50), m("d", "Davi", 300, "LEFT")]);
    expect(ranking.map((c) => c.uid)).toEqual(["a", "b", "c"]);
    expect(campeoes.map((c) => c.uid)).toEqual(["a", "b"]);
    expect(rankingFinal([m("a", "Ana", 0)]).campeoes).toEqual([]);
  });
});

describe("virada de temporada no Emulator", () => {
  const db = new Firestore(envLocal);
  beforeEach(limparFirestore);

  it("congela o ranking, aponta o campeão, zera seasonPoints e é idempotente", async () => {
    const ana = await criarUsuario();
    await chamar("criarPerfil", ana.token, { nome: "Ana" });
    const { casaId } = (await json(await chamar("criarCasa", ana.token, { nome: "Rep", tipo: "REPUBLICA" }))) as { casaId: string };
    const bruno = await criarUsuario();
    await chamar("criarPerfil", bruno.token, { nome: "Bruno" });
    const { codigo } = (await json(await chamar("criarConvite", ana.token))) as { codigo: string };
    await chamar("entrarComCodigo", bruno.token, { codigo });
    await db.commit([
      { tipo: "gravar", caminho: `houses/${casaId}/members/${ana.uid}`, dados: { seasonPoints: 150, totalPoints: 400 }, campos: ["seasonPoints", "totalPoints"] },
      { tipo: "gravar", caminho: `houses/${casaId}/members/${bruno.uid}`, dados: { seasonPoints: 275, totalPoints: 275 }, campos: ["seasonPoints", "totalPoints"] },
    ]);

    const agora = new Date("2026-12-01T03:00:00Z");
    expect(await virarTemporada(db, agora)).toEqual({ temporada: "2026-11", casas: 1 });
    const t = await db.ler(`houses/${casaId}/seasons/2026-11`);
    expect(t?.["championId"]).toBe(bruno.uid);
    expect(t?.["championIds"]).toEqual([bruno.uid]);
    expect(t?.["championName"]).toBe("Bruno");
    expect(t?.["championPoints"]).toBe(275);
    expect((t?.["standings"] as { uid: string; points: number }[]).map((c) => [c.uid, c.points])).toEqual([
      [bruno.uid, 275],
      [ana.uid, 150],
    ]);
    const ana2 = await db.ler(`houses/${casaId}/members/${ana.uid}`);
    expect(ana2?.["seasonPoints"]).toBe(0);
    // O ranking geral não zera (R-28).
    expect(ana2?.["totalPoints"]).toBe(400);

    expect(await virarTemporada(db, agora)).toEqual({ temporada: "2026-11", casas: 0 });
  });

  it("o Cron Trigger do dia 1º chama a virada", async () => {
    const ana = await criarUsuario();
    await chamar("criarPerfil", ana.token, { nome: "Ana" });
    const { casaId } = (await json(await chamar("criarCasa", ana.token, { nome: "Rep", tipo: "REPUBLICA" }))) as { casaId: string };
    const espera: Promise<unknown>[] = [];
    const ctx = { waitUntil: (p: Promise<unknown>) => espera.push(p), passThroughOnException: () => {}, props: {} } as unknown as ExecutionContext;
    const scheduledTime = Date.parse("2026-12-01T03:00:00Z");
    await worker.scheduled({ scheduledTime, cron: CRON_TEMPORADA, noRetry: () => {} } as ScheduledController, envLocal, ctx);
    await Promise.all(espera);
    expect(await db.ler(`houses/${casaId}/seasons/2026-11`)).toBeDefined();
  });
});
