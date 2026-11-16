import { beforeEach, describe, expect, it } from "vitest";
import { Firestore } from "../src/firestore.js";
import worker from "../src/index.js";
import { excluirConta } from "../src/operacoes/conta.js";
import { AUTH, criarUsuario, envLocal, limparFirestore, PROJETO } from "./ambiente.js";

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

describe("excluir conta (E7, #28)", () => {
  const db = new Firestore(envLocal);
  beforeEach(limparFirestore);

  it("sai da casa, passa o admin, anonimiza, apaga o perfil e a conta no Auth", async () => {
    const ana = await criarUsuario();
    await chamar("criarPerfil", ana.token, { nome: "Ana" });
    const { casaId } = (await json(await chamar("criarCasa", ana.token, { nome: "Rep", tipo: "REPUBLICA" }))) as { casaId: string };
    const bruno = await criarUsuario();
    await chamar("criarPerfil", bruno.token, { nome: "Bruno" });
    const { codigo } = (await json(await chamar("criarConvite", ana.token))) as { codigo: string };
    await chamar("entrarComCodigo", bruno.token, { codigo });
    const prazo = new Date(Date.now() + 72 * 3600_000).toISOString();
    const { tarefaId } = (await json(await chamar("criarTarefa", ana.token, { titulo: "Lixo", categoria: "LIXO", peso: "LEVE", prazo }))) as { tarefaId: string };
    await chamar("assumirTarefa", ana.token, { tarefaId });

    expect((await chamar("excluirConta", ana.token, { confirmacao: "excluir" })).status).toBe(400);
    expect((await chamar("excluirConta", ana.token, { confirmacao: "EXCLUIR" })).status).toBe(200);

    expect(await db.ler(`users/${ana.uid}`)).toBeUndefined();
    expect((await db.ler(`houses/${casaId}/tasks/${tarefaId}`))?.["status"]).toBe("OPEN");
    expect(await db.ler(`houses/${casaId}/members/${ana.uid}`)).toMatchObject({ status: "LEFT", displayName: null, avatarId: null, deleted: true });
    expect((await db.ler(`houses/${casaId}/members/${bruno.uid}`))?.["role"]).toBe("ADMIN");
    const feed = (await db.listar(`houses/${casaId}/activity`)).map((e) => e.dados).filter((e) => e["actorId"] === ana.uid);
    expect(feed.every((e) => e["actorName"] === null)).toBe(true);

    const busca = await fetch(`http://${AUTH}/identitytoolkit.googleapis.com/v1/projects/${PROJETO}/accounts:lookup`, {
      method: "POST",
      headers: { authorization: "Bearer owner", "content-type": "application/json" },
      body: JSON.stringify({ localId: [ana.uid] }),
    });
    expect(((await busca.json()) as { users?: unknown[] }).users ?? []).toHaveLength(0);
  });

  it("pede login recente", async () => {
    const ana = await criarUsuario();
    await chamar("criarPerfil", ana.token, { nome: "Ana" });
    const depois = new Date(Date.now() + 10 * 60_000);
    await expect(excluirConta(db, envLocal, { uid: ana.uid, autenticadoEm: Math.floor(Date.now() / 1000) }, { confirmacao: "EXCLUIR" }, depois)).rejects.toMatchObject({ status: 401 });
  });
});
