import { beforeEach, describe, expect, it } from "vitest";
import { Firestore } from "../src/firestore.js";
import worker from "../src/index.js";
import { assumirTarefa, desfazer } from "../src/operacoes/match.js";
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

type Pessoa = Awaited<ReturnType<typeof criarUsuario>>;

/** Casa com Ana (admin) e Bruno. */
async function casa() {
  const ana = await criarUsuario();
  await chamar("criarPerfil", ana.token, { nome: "Ana" });
  const { casaId } = (await json(await chamar("criarCasa", ana.token, { nome: "Rep. Cajueiro", tipo: "REPUBLICA" }))) as { casaId: string };
  const bruno = await criarUsuario();
  await chamar("criarPerfil", bruno.token, { nome: "Bruno" });
  const { codigo } = (await json(await chamar("criarConvite", ana.token))) as { codigo: string };
  await chamar("entrarComCodigo", bruno.token, { codigo });
  return { casaId, ana, bruno };
}

async function tarefa(p: Pessoa, titulo = "Lavar a louça") {
  const prazo = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString();
  const { tarefaId } = (await json(await chamar("criarTarefa", p.token, { titulo, categoria: "COZINHA", peso: "MEDIA", prazo }))) as { tarefaId: string };
  return tarefaId;
}

describe("match: assumir, passar, desfazer (R-15 a R-20)", () => {
  const db = new Firestore(envLocal);
  beforeEach(limparFirestore);

  it("assume a tarefa e conta no limite do morador", async () => {
    const { casaId, ana } = await casa();
    const id = await tarefa(ana);
    expect((await chamar("assumirTarefa", ana.token, { tarefaId: id })).status).toBe(200);
    expect(await db.ler(`houses/${casaId}/tasks/${id}`)).toMatchObject({ status: "ASSIGNED", assigneeId: ana.uid, assignedVia: "SWIPE" });
    expect((await db.ler(`houses/${casaId}/members/${ana.uid}`))?.["openTasks"]).toBe(1);
  });

  it("dois assumindo ao mesmo tempo: um leva, o outro vê quem pegou (R-15)", async () => {
    const { casaId, ana, bruno } = await casa();
    const id = await tarefa(ana);
    const [a, b] = await Promise.all([chamar("assumirTarefa", ana.token, { tarefaId: id }), chamar("assumirTarefa", bruno.token, { tarefaId: id })]);
    expect([a.status, b.status].sort()).toEqual([200, 409]);
    const perdedor = a.status === 409 ? a : b;
    const vencedor = a.status === 200 ? ana : bruno;
    expect(await json(perdedor)).toEqual({ erro: "ja-assumida", detalhe: vencedor === ana ? "Ana" : "Bruno" });
    expect((await db.ler(`houses/${casaId}/tasks/${id}`))?.["assigneeId"]).toBe(vencedor.uid);
  });

  it("no limite de 5 abertas, não assume mais (R-20)", async () => {
    const { ana } = await casa();
    for (let i = 0; i < 5; i++) expect((await chamar("assumirTarefa", ana.token, { tarefaId: await tarefa(ana, `T${i}`) })).status).toBe(200);
    const sexta = await chamar("assumirTarefa", ana.token, { tarefaId: await tarefa(ana, "T6") });
    expect(await json(sexta)).toEqual({ erro: "limite" });
  });

  it("passar grava um passe de 12 h só para quem passou (R-16)", async () => {
    const { casaId, ana, bruno } = await casa();
    const id = await tarefa(ana);
    const { expiraEm } = (await json(await chamar("passarTarefa", bruno.token, { tarefaId: id }))) as { expiraEm: string };
    expect(new Date(expiraEm).getTime() - Date.now()).toBeGreaterThan(12 * 60 * 60 * 1000 - 60_000);
    expect(await db.ler(`houses/${casaId}/members/${bruno.uid}/passes/${id}`)).toBeDefined();
    expect(await db.ler(`houses/${casaId}/members/${ana.uid}/passes/${id}`)).toBeUndefined();
    expect((await db.ler(`houses/${casaId}/tasks/${id}`))?.["status"]).toBe("OPEN");
  });

  it("desfaz assumir e passar logo depois (R-18)", async () => {
    const { casaId, ana } = await casa();
    const id = await tarefa(ana);
    await chamar("assumirTarefa", ana.token, { tarefaId: id });
    expect((await chamar("desfazer", ana.token, { tarefaId: id, acao: "ASSUMIR" })).status).toBe(200);
    expect(await db.ler(`houses/${casaId}/tasks/${id}`)).toMatchObject({ status: "OPEN", assigneeId: null });
    expect((await db.ler(`houses/${casaId}/members/${ana.uid}`))?.["openTasks"]).toBe(0);

    await chamar("passarTarefa", ana.token, { tarefaId: id });
    expect((await chamar("desfazer", ana.token, { tarefaId: id, acao: "PASSAR" })).status).toBe(200);
    expect(await db.ler(`houses/${casaId}/members/${ana.uid}/passes/${id}`)).toBeUndefined();
  });

  it("não desfaz depois da janela nem a tarefa de outra pessoa", async () => {
    const { ana, bruno } = await casa();
    const id = await tarefa(ana);
    const antes = new Date(Date.now() - 60_000);
    await assumirTarefa(db, { uid: ana.uid }, { tarefaId: id }, antes);
    await expect(desfazer(db, { uid: ana.uid }, { tarefaId: id, acao: "ASSUMIR" })).rejects.toMatchObject({ codigo: "nao-da-para-desfazer" });
    expect(await json(await chamar("desfazer", bruno.token, { tarefaId: id, acao: "ASSUMIR" }))).toEqual({ erro: "nao-da-para-desfazer" });
  });

  it("rever apaga os passes da pessoa (R-17)", async () => {
    const { casaId, bruno, ana } = await casa();
    const ids = [await tarefa(ana, "A"), await tarefa(ana, "B")];
    for (const id of ids) await chamar("passarTarefa", bruno.token, { tarefaId: id });
    expect(await json(await chamar("reverPassadas", bruno.token))).toEqual({ revistas: 2 });
    expect(await db.listar(`houses/${casaId}/members/${bruno.uid}/passes`)).toEqual([]);
  });

  it("assumir apaga o passe que a pessoa tinha", async () => {
    const { casaId, bruno, ana } = await casa();
    const id = await tarefa(ana);
    await chamar("passarTarefa", bruno.token, { tarefaId: id });
    await chamar("assumirTarefa", bruno.token, { tarefaId: id });
    expect(await db.ler(`houses/${casaId}/members/${bruno.uid}/passes/${id}`)).toBeUndefined();
  });
});
