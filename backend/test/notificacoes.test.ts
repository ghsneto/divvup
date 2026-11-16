import { beforeEach, describe, expect, it } from "vitest";
import { Firestore } from "../src/firestore.js";
import worker from "../src/index.js";
import { atribuirAutomaticamente, lembrarPrazos, marcarAtrasadas } from "../src/operacoes/agendadas.js";
import { emSilencio, enviadosNoEmulador, entregarRetidos, notificar, querPush } from "../src/operacoes/notificacoes.js";
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

describe("notificações (R-33, R-36, R-37)", () => {
  const db = new Firestore(envLocal);
  beforeEach(async () => {
    await limparFirestore();
    enviadosNoEmulador.length = 0;
  });

  it("preferência por tipo com o padrão da tabela", () => {
    expect(querPush(undefined, "NEW_TASK")).toBe(true);
    expect(querPush(undefined, "MEMBER_COMPLETED")).toBe(false);
    expect(querPush({ notifPrefs: { NEW_TASK: false } }, "NEW_TASK")).toBe(false);
  });

  it("nova tarefa avisa todos menos o autor, na central e por push", async () => {
    const ana = await criarUsuario();
    await chamar("criarPerfil", ana.token, { nome: "Ana Souza" });
    await chamar("criarCasa", ana.token, { nome: "Rep", tipo: "REPUBLICA" });
    const bruno = await criarUsuario();
    await chamar("criarPerfil", bruno.token, { nome: "Bruno" });
    const { codigo } = (await json(await chamar("criarConvite", ana.token))) as { codigo: string };
    await chamar("entrarComCodigo", bruno.token, { codigo });
    expect((await chamar("registrarToken", bruno.token, { token: "token-do-bruno-1" })).status).toBe(200);
    // O teste roda a qualquer hora: sem horário silencioso, o push sai na hora.
    await chamar("salvarPreferencias", bruno.token, { silencio: { ligado: false } });
    await chamar("registrarToken", ana.token, { token: "token-da-ana-1" });

    const prazo = new Date(Date.now() + 48 * 3600_000).toISOString();
    await chamar("criarTarefa", ana.token, { titulo: "Passear com o Thor", categoria: "PETS", peso: "MEDIA", prazo });

    const caixa = await db.listar(`users/${bruno.uid}/notifications`);
    expect(caixa).toHaveLength(1);
    expect(caixa[0]?.dados).toMatchObject({ type: "NEW_TASK", title: "Ana criou Passear com o Thor", body: "Pets · 50 pts", read: false });
    expect(await db.listar(`users/${ana.uid}/notifications`)).toHaveLength(0);
    expect(enviadosNoEmulador.map((p) => p.token)).toEqual(["token-do-bruno-1"]);
  });

  it("token inválido sai da lista; sem querer o tipo, só a central", async () => {
    const ana = await criarUsuario();
    await chamar("criarPerfil", ana.token, { nome: "Ana" });
    await chamar("registrarToken", ana.token, { token: "invalido-123456" });
    await chamar("registrarToken", ana.token, { token: "valido-1234567" });
    await chamar("salvarPreferencias", ana.token, { silencio: { ligado: false } });
    await notificar(db, envLocal, [ana.uid], { tipo: "LATE", titulo: "x", corpo: "y", refs: {} });
    expect((await db.ler(`users/${ana.uid}`))?.["fcmTokens"]).toEqual(["valido-1234567"]);

    enviadosNoEmulador.length = 0;
    await notificar(db, envLocal, [ana.uid], { tipo: "MEMBER_COMPLETED", titulo: "x", corpo: "y", refs: {} });
    expect(enviadosNoEmulador).toHaveLength(0);
    expect(await db.listar(`users/${ana.uid}/notifications`)).toHaveLength(2);
  });
});

describe("eventos da R-33 (#23)", () => {
  const db = new Firestore(envLocal);
  const HORA = 3600_000;
  beforeEach(async () => {
    await limparFirestore();
    enviadosNoEmulador.length = 0;
  });

  async function casa() {
    const ana = await criarUsuario();
    await chamar("criarPerfil", ana.token, { nome: "Ana" });
    const { casaId } = (await json(await chamar("criarCasa", ana.token, { nome: "Rep. Cajueiro", tipo: "REPUBLICA" }))) as { casaId: string };
    const bruno = await criarUsuario();
    await chamar("criarPerfil", bruno.token, { nome: "Bruno" });
    const { codigo } = (await json(await chamar("criarConvite", ana.token))) as { codigo: string };
    await chamar("entrarComCodigo", bruno.token, { codigo });
    const tarefa = async (emHoras: number) => {
      const prazo = new Date(Date.now() + emHoras * HORA).toISOString();
      return ((await json(await chamar("criarTarefa", ana.token, { titulo: "Lavar a louça", categoria: "COZINHA", peso: "MEDIA", prazo }))) as { tarefaId: string }).tarefaId;
    };
    return { ana, bruno, casaId, tarefa };
  }
  const caixa = async (uid: string) => (await db.listar(`users/${uid}/notifications`)).map((n) => n.dados);
  const avisar = (uids: string[], aviso: Parameters<typeof notificar>[3]) => notificar(db, envLocal, uids, aviso);

  it("atribuída automaticamente, vence em 24 h e em 2 h (uma vez cada) e atrasada", async () => {
    const { ana, bruno, casaId, tarefa } = await casa();
    // Bruno tem menos pontos: recebe a automática.
    await db.commit([{ tipo: "gravar", caminho: `houses/${casaId}/members/${ana.uid}`, dados: { seasonPoints: 100 }, campos: ["seasonPoints"] }]);
    const auto = await tarefa(10);
    await atribuirAutomaticamente(db, new Date(), undefined, avisar);
    expect((await caixa(bruno.uid)).find((n) => n["type"] === "AUTO_ASSIGNED")?.["title"]).toBe("Lavar a louça veio pra você");

    const minha = await tarefa(23);
    await chamar("assumirTarefa", ana.token, { tarefaId: minha });
    expect(await lembrarPrazos(db, new Date(), avisar)).toEqual({ lembretes: 1 });
    expect(await lembrarPrazos(db, new Date(), avisar)).toEqual({ lembretes: 0 });
    expect((await caixa(ana.uid)).filter((n) => n["type"] === "DUE_24H")).toHaveLength(1);

    // A automática pula o de 24 h; a 2 h de cada prazo o dono é lembrado.
    expect(await lembrarPrazos(db, new Date(Date.now() + 8.5 * HORA), avisar)).toEqual({ lembretes: 1 });
    expect((await caixa(bruno.uid)).filter((n) => n["type"] === "DUE_2H")).toHaveLength(1);
    expect(await lembrarPrazos(db, new Date(Date.now() + 21.5 * HORA), avisar)).toEqual({ lembretes: 1 });
    expect((await caixa(ana.uid)).filter((n) => n["type"] === "DUE_2H")).toHaveLength(1);

    await marcarAtrasadas(db, new Date(Date.now() + 30 * HORA), avisar);
    const atrasou = (await caixa(ana.uid)).find((n) => n["type"] === "LATE");
    expect(atrasou).toMatchObject({ title: "Lavar a louça atrasou", body: "Concluindo agora você ganha 25 pts." });
    expect(auto).toBeTruthy();
  });

  it("fim de temporada, morador concluiu (só a central, padrão desligado) e removido", async () => {
    const { ana, bruno, casaId, tarefa } = await casa();
    const t = await tarefa(48);
    await chamar("assumirTarefa", ana.token, { tarefaId: t });
    await chamar("concluirTarefa", ana.token, { tarefaId: t });
    expect((await caixa(bruno.uid)).find((n) => n["type"] === "MEMBER_COMPLETED")?.["title"]).toBe("Ana concluiu Lavar a louça");

    await virarTemporada(db, new Date("2026-12-01T03:00:00Z"), avisar);
    expect((await caixa(ana.uid)).find((n) => n["type"] === "SEASON_END")).toMatchObject({ title: "Você venceu novembro", body: "Temporada fechada. Você ficou em 1º com 50 pts." });
    expect((await caixa(bruno.uid)).find((n) => n["type"] === "SEASON_END")?.["title"]).toBe("Ana venceu novembro");

    await chamar("removerMembro", ana.token, { uid: bruno.uid });
    expect((await caixa(bruno.uid)).find((n) => n["type"] === "REMOVED")?.["title"]).toBe("Você saiu da Rep. Cajueiro");
    expect(casaId).toBeTruthy();
  });
});

describe("preferências e horário silencioso (R-34, R-35, #24)", () => {
  const db = new Firestore(envLocal);
  beforeEach(async () => {
    await limparFirestore();
    enviadosNoEmulador.length = 0;
  });

  it("silêncio padrão 22h–8h em Brasília, atravessando a meia-noite", () => {
    expect(emSilencio(undefined, new Date("2026-11-10T02:00:00Z"))).toBe(true); // 23h
    expect(emSilencio(undefined, new Date("2026-11-10T10:30:00Z"))).toBe(true); // 7h30
    expect(emSilencio(undefined, new Date("2026-11-10T11:00:00Z"))).toBe(false); // 8h
    expect(emSilencio({ quietHours: { enabled: false } }, new Date("2026-11-10T02:00:00Z"))).toBe(false);
    expect(emSilencio({ quietHours: { enabled: true, start: "13:00", end: "14:00" } }, new Date("2026-11-10T16:30:00Z"))).toBe(true);
  });

  it("salva tipos e horário; no silêncio retém o push e entrega no fim, menos o de 2 h", async () => {
    const ana = await criarUsuario();
    await chamar("criarPerfil", ana.token, { nome: "Ana" });
    await chamar("registrarToken", ana.token, { token: "token-da-ana-123" });
    expect((await chamar("salvarPreferencias", ana.token, { tipos: { NEW_TASK: false, MEMBER_COMPLETED: true }, silencio: { ligado: true, inicio: "22:00", fim: "08:00" } })).status).toBe(200);
    expect((await chamar("salvarPreferencias", ana.token, { silencio: { inicio: "25:00" } })).status).toBe(400);
    expect((await db.ler(`users/${ana.uid}`))?.["notifPrefs"]).toEqual({ NEW_TASK: false, MEMBER_COMPLETED: true });

    const noite = new Date("2026-11-10T02:00:00Z");
    await notificar(db, envLocal, [ana.uid], { tipo: "LATE", titulo: "Atrasou", corpo: "x", refs: {} }, noite);
    await notificar(db, envLocal, [ana.uid], { tipo: "DUE_2H", titulo: "Faltam 2 h", corpo: "x", refs: {} }, noite);
    expect(enviadosNoEmulador.map((p) => p.titulo)).toEqual(["Faltam 2 h"]);

    expect(await entregarRetidos(db, envLocal, new Date("2026-11-10T05:00:00Z"))).toEqual({ entregues: 0 });
    expect(await entregarRetidos(db, envLocal, new Date("2026-11-10T11:05:00Z"))).toEqual({ entregues: 1 });
    expect(enviadosNoEmulador.map((p) => p.titulo)).toEqual(["Faltam 2 h", "Atrasou"]);
    expect(await entregarRetidos(db, envLocal, new Date("2026-11-10T11:20:00Z"))).toEqual({ entregues: 0 });
  });
});
