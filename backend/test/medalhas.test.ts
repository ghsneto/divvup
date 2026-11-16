import { beforeEach, describe, expect, it } from "vitest";
import { Firestore } from "../src/firestore.js";
import worker from "../src/index.js";
import { medalhasDaConclusao } from "../src/operacoes/medalhas.js";
import { virarTemporada } from "../src/operacoes/temporadas.js";
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
const HORA = 3600_000;

describe("regras das medalhas (R-30)", () => {
  const agora = new Date("2026-11-10T15:00:00Z");
  const base = { tarefasFeitas: 5, sequencia: 3, sequenciaAntes: 2, via: "SWIPE", assumidaEm: new Date(agora.getTime() - 100 * HORA), prazo: new Date(agora.getTime() + HORA) };

  it("primeira tarefa e 100 tarefas", () => {
    expect(medalhasDaConclusao({}, { ...base, tarefasFeitas: 1 }, agora).nova).toBe("FIRST_TASK");
    expect(medalhasDaConclusao({}, { ...base, tarefasFeitas: 100 }, agora).nova).toBe("TASKS_100");
    expect(medalhasDaConclusao({}, base, agora).nova).toBeNull();
  });

  it("7 dias só quando a sequência chega a 7", () => {
    expect(medalhasDaConclusao({}, { ...base, sequencia: 7, sequenciaAntes: 6 }, agora).nova).toBe("STREAK_7");
    expect(medalhasDaConclusao({}, { ...base, sequencia: 7, sequenciaAntes: 7 }, agora).nova).toBeNull();
  });

  it("salva-vidas: assumida pelo swipe a menos de 48 h do prazo; repetida não é nova", () => {
    const perto = { ...base, assumidaEm: new Date(base.prazo.getTime() - 47 * HORA) };
    const r = medalhasDaConclusao({}, perto, agora);
    expect(r.nova).toBe("LIFESAVER");
    const de2 = medalhasDaConclusao(r.medalhas, perto, agora);
    expect(de2.nova).toBeNull();
    expect(de2.medalhas.LIFESAVER?.count).toBe(2);
    expect(medalhasDaConclusao({}, { ...perto, via: "AUTO" }, agora).nova).toBeNull();
  });
});

describe("medalhas no Emulator", () => {
  const db = new Firestore(envLocal);
  beforeEach(limparFirestore);

  it("concluir a primeira tarefa dá a medalha, mostra em C4 e copia no membro; campeão do mês ganha a dele", async () => {
    const ana = await criarUsuario();
    await chamar("criarPerfil", ana.token, { nome: "Ana" });
    const { casaId } = (await json(await chamar("criarCasa", ana.token, { nome: "Rep", tipo: "REPUBLICA" }))) as { casaId: string };
    const prazo = new Date(Date.now() + 72 * HORA).toISOString();
    const { tarefaId } = (await json(await chamar("criarTarefa", ana.token, { titulo: "Lixo", categoria: "LIXO", peso: "LEVE", prazo }))) as { tarefaId: string };
    await chamar("assumirTarefa", ana.token, { tarefaId });
    const r = await json(await chamar("concluirTarefa", ana.token, { tarefaId }));
    expect(r["medalhaNova"]).toBe("FIRST_TASK");
    expect((await db.ler(`users/${ana.uid}`))?.["badges"]).toMatchObject({ FIRST_TASK: { count: 1 } });
    expect((await db.ler(`houses/${casaId}/members/${ana.uid}`))?.["badges"]).toMatchObject({ FIRST_TASK: { count: 1 } });

    await virarTemporada(db, new Date("2026-12-01T03:00:00Z"));
    expect((await db.ler(`users/${ana.uid}`))?.["badges"]).toMatchObject({ FIRST_TASK: { count: 1 }, CHAMPION: { count: 1 } });
    expect((await db.ler(`houses/${casaId}/members/${ana.uid}`))?.["badges"]).toMatchObject({ CHAMPION: { count: 1 } });
  });
});
