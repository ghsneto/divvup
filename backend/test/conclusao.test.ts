import { beforeEach, describe, expect, it } from "vitest";
import { Firestore } from "../src/firestore.js";
import worker from "../src/index.js";
import { assumirTarefa } from "../src/operacoes/match.js";
import { concluirTarefa, diaEmBrasilia, novaSequencia, pontosDaConclusao } from "../src/operacoes/tarefas.js";
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
const DIA = 24 * 60 * 60 * 1000;

async function casa() {
  const ana = await criarUsuario();
  await chamar("criarPerfil", ana.token, { nome: "Ana" });
  const { casaId } = (await json(await chamar("criarCasa", ana.token, { nome: "Rep", tipo: "REPUBLICA" }))) as { casaId: string };
  const bruno = await criarUsuario();
  await chamar("criarPerfil", bruno.token, { nome: "Bruno" });
  const { codigo } = (await json(await chamar("criarConvite", ana.token))) as { codigo: string };
  await chamar("entrarComCodigo", bruno.token, { codigo });
  return { casaId, ana, bruno };
}

async function tarefa(p: Pessoa, peso = "MEDIA", emMs = 2 * DIA) {
  const prazo = new Date(Date.now() + emMs).toISOString();
  return ((await json(await chamar("criarTarefa", p.token, { titulo: "Lavar", categoria: "COZINHA", peso, prazo }))) as { tarefaId: string }).tarefaId;
}

describe("regras de pontos (R-22, R-31)", () => {
  it("integral no prazo, metade depois", () => {
    const prazo = new Date("2026-11-10T22:00:00Z");
    expect(pontosDaConclusao(50, prazo, new Date("2026-11-10T21:00:00Z"))).toBe(50);
    expect(pontosDaConclusao(50, prazo, new Date("2026-11-10T22:00:01Z"))).toBe(25);
    expect(pontosDaConclusao(25, prazo, new Date("2026-11-11T00:00:00Z"))).toBe(12);
  });

  it("sequência conta dias seguidos no fuso de Brasília", () => {
    expect(diaEmBrasilia(new Date("2026-11-11T02:00:00Z"))).toBe("2026-11-10");
    expect(novaSequencia({ current: 3, best: 5, lastDay: "2026-11-09" }, "2026-11-10")).toEqual({ current: 4, best: 5, lastDay: "2026-11-10" });
    expect(novaSequencia({ current: 4, best: 5, lastDay: "2026-11-10" }, "2026-11-10")).toEqual({ current: 4, best: 5, lastDay: "2026-11-10" });
    expect(novaSequencia({ current: 7, best: 7, lastDay: "2026-11-01" }, "2026-11-10")).toEqual({ current: 1, best: 7, lastDay: "2026-11-10" });
    expect(novaSequencia({ current: 0, best: 0, lastDay: null }, "2026-11-10")).toEqual({ current: 1, best: 1, lastDay: "2026-11-10" });
  });
});

describe("concluir, devolver, cancelar, editar e excluir", () => {
  const db = new Firestore(envLocal);
  beforeEach(limparFirestore);

  it("concluir credita pontos, conta a sequência, grava o evento e mostra a posição (R-21, R-22, R-24)", async () => {
    const { casaId, ana, bruno } = await casa();
    // Bruno tem 25 no mês; Ana conclui uma de 50 e passa ele.
    const daBruno = await tarefa(ana, "LEVE");
    await chamar("assumirTarefa", bruno.token, { tarefaId: daBruno });
    await chamar("concluirTarefa", bruno.token, { tarefaId: daBruno });
    const id = await tarefa(ana);
    await chamar("assumirTarefa", ana.token, { tarefaId: id });
    const r = await json(await chamar("concluirTarefa", ana.token, { tarefaId: id }));
    expect(r).toEqual({ pontos: 50, noPrazo: true, posicaoAntes: 2, posicaoDepois: 1, ultrapassou: "Bruno", sequencia: 1 });
    expect(await db.ler(`houses/${casaId}/tasks/${id}`)).toMatchObject({ status: "DONE", pointsAwarded: 50 });
    expect(await db.ler(`houses/${casaId}/members/${ana.uid}`)).toMatchObject({ seasonPoints: 50, totalPoints: 50, openTasks: 0 });
    expect(await db.ler(`users/${ana.uid}`)).toMatchObject({ totalPoints: 50, tasksDone: 1, streak: { current: 1, best: 1 } });
    const eventos = await db.listar(`houses/${casaId}/activity`);
    expect(eventos.map((e) => e.dados)).toContainEqual(expect.objectContaining({ type: "COMPLETED", actorId: ana.uid, points: 50 }));
  });

  it("depois do prazo vale metade e só o dono conclui", async () => {
    const { ana, bruno } = await casa();
    const id = await tarefa(ana, "PESADA", DIA);
    await assumirTarefa(db, { uid: ana.uid }, { tarefaId: id });
    expect(await json(await chamar("concluirTarefa", bruno.token, { tarefaId: id }))).toEqual({ erro: "nao-e-sua" });
    const r = await concluirTarefa(db, { uid: ana.uid }, { tarefaId: id }, new Date(Date.now() + 2 * DIA));
    expect(r).toMatchObject({ pontos: 50, noPrazo: false });
    expect(await json(await chamar("concluirTarefa", ana.token, { tarefaId: id }))).toEqual({ erro: "nao-e-sua" });
  });

  it("devolve só com mais de 24 h para o prazo (R-19)", async () => {
    const { casaId, ana } = await casa();
    const longe = await tarefa(ana, "MEDIA", 3 * DIA);
    const perto = await tarefa(ana, "MEDIA", 10 * 60 * 60 * 1000);
    for (const id of [longe, perto]) await chamar("assumirTarefa", ana.token, { tarefaId: id });
    expect((await chamar("devolverTarefa", ana.token, { tarefaId: longe })).status).toBe(200);
    expect(await db.ler(`houses/${casaId}/tasks/${longe}`)).toMatchObject({ status: "OPEN", assigneeId: null });
    expect(await json(await chamar("devolverTarefa", ana.token, { tarefaId: perto }))).toEqual({ erro: "prazo-curto" });
    expect((await db.ler(`houses/${casaId}/members/${ana.uid}`))?.["openTasks"]).toBe(1);
  });

  it("só admin cancela; o dono perde a tarefa (R-12)", async () => {
    const { casaId, ana, bruno } = await casa();
    const id = await tarefa(ana);
    await chamar("assumirTarefa", bruno.token, { tarefaId: id });
    expect(await json(await chamar("cancelarTarefa", bruno.token, { tarefaId: id }))).toEqual({ erro: "nao-admin" });
    expect((await chamar("cancelarTarefa", ana.token, { tarefaId: id })).status).toBe(200);
    expect((await db.ler(`houses/${casaId}/tasks/${id}`))?.["status"]).toBe("CANCELLED");
    expect((await db.ler(`houses/${casaId}/members/${bruno.uid}`))?.["openTasks"]).toBe(0);
  });

  it("editar e excluir: autor ou admin, só sem dono (R-12)", async () => {
    const { casaId, ana, bruno } = await casa();
    const daBruno = await tarefa(bruno);
    const prazo = new Date(Date.now() + 3 * DIA).toISOString();
    const edicao = { tarefaId: daBruno, titulo: "Varrer a sala", categoria: "LIMPEZA_GERAL", peso: "PESADA", prazo };
    // Admin edita a de outra pessoa; o autor também.
    expect((await chamar("editarTarefa", ana.token, edicao)).status).toBe(200);
    expect(await db.ler(`houses/${casaId}/tasks/${daBruno}`)).toMatchObject({ title: "Varrer a sala", category: "LIMPEZA_GERAL", points: 100 });
    const daAna = await tarefa(ana);
    expect(await json(await chamar("excluirTarefa", bruno.token, { tarefaId: daAna }))).toEqual({ erro: "sem-permissao" });
    await chamar("assumirTarefa", bruno.token, { tarefaId: daBruno });
    expect(await json(await chamar("excluirTarefa", bruno.token, { tarefaId: daBruno }))).toEqual({ erro: "tem-dono" });
    expect((await chamar("excluirTarefa", ana.token, { tarefaId: daAna })).status).toBe(200);
    expect(await db.ler(`houses/${casaId}/tasks/${daAna}`)).toBeUndefined();
  });
});
