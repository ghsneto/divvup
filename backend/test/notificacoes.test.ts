import { beforeEach, describe, expect, it } from "vitest";
import { Firestore } from "../src/firestore.js";
import worker from "../src/index.js";
import { enviadosNoEmulador, notificar, querPush } from "../src/operacoes/notificacoes.js";
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
    await notificar(db, envLocal, [ana.uid], { tipo: "LATE", titulo: "x", corpo: "y", refs: {} });
    expect((await db.ler(`users/${ana.uid}`))?.["fcmTokens"]).toEqual(["valido-1234567"]);

    enviadosNoEmulador.length = 0;
    await notificar(db, envLocal, [ana.uid], { tipo: "MEMBER_COMPLETED", titulo: "x", corpo: "y", refs: {} });
    expect(enviadosNoEmulador).toHaveLength(0);
    expect(await db.listar(`users/${ana.uid}/notifications`)).toHaveLength(2);
  });
});
