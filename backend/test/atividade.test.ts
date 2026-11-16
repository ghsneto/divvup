import { beforeEach, describe, expect, it } from "vitest";
import { Firestore } from "../src/firestore.js";
import worker from "../src/index.js";
import { indiceDeEquilibrio } from "../src/operacoes/equilibrio.js";
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

describe("Índice de Equilíbrio (R-45)", () => {
  it("100 com 1 morador, sem pontos ou tudo igual; cai quando um faz tudo", () => {
    expect(indiceDeEquilibrio([300])).toBe(100);
    expect(indiceDeEquilibrio([0, 0, 0])).toBe(100);
    expect(indiceDeEquilibrio([100, 100, 100])).toBe(100);
    expect(indiceDeEquilibrio([300, 0, 0])).toBe(0);
    expect(indiceDeEquilibrio([480, 325, 300, 175])).toBe(76);
  });
});

describe("Histórico e contadores (R-25)", () => {
  const db = new Firestore(envLocal);
  beforeEach(limparFirestore);

  it("entrar, assumir, desfazer e concluir alimentam o feed e os contadores do mês", async () => {
    const ana = await criarUsuario();
    await chamar("criarPerfil", ana.token, { nome: "Ana" });
    const { casaId } = (await json(await chamar("criarCasa", ana.token, { nome: "Rep", tipo: "REPUBLICA" }))) as { casaId: string };
    const bruno = await criarUsuario();
    await chamar("criarPerfil", bruno.token, { nome: "Bruno" });
    const { codigo } = (await json(await chamar("criarConvite", ana.token))) as { codigo: string };
    await chamar("entrarComCodigo", bruno.token, { codigo });

    const prazo = new Date(Date.now() + 48 * 3600_000).toISOString();
    const { tarefaId } = (await json(await chamar("criarTarefa", ana.token, { titulo: "Lavar a louça", categoria: "COZINHA", peso: "MEDIA", prazo }))) as { tarefaId: string };
    await chamar("assumirTarefa", bruno.token, { tarefaId });
    const feed = async () => (await db.listar(`houses/${casaId}/activity`)).map((e) => e.dados);
    expect((await feed()).map((e) => e["type"]).sort()).toEqual(["ASSIGNED", "JOINED"]);

    await chamar("desfazer", bruno.token, { tarefaId, acao: "ASSUMIR" });
    expect((await feed()).map((e) => e["type"])).toEqual(["JOINED"]);

    await chamar("assumirTarefa", bruno.token, { tarefaId });
    await chamar("concluirTarefa", bruno.token, { tarefaId });
    const concluiu = (await feed()).find((e) => e["type"] === "COMPLETED");
    expect(concluiu).toMatchObject({ actorName: "Bruno", taskTitle: "Lavar a louça", category: "COZINHA", points: 50, late: false });

    const m = await db.ler(`houses/${casaId}/members/${bruno.uid}`);
    expect(m).toMatchObject({ seasonTasks: 1, totalTasks: 1, seasonCategories: { COZINHA: 1 } });
    // Bruno 50, Ana 0: tudo com um só.
    expect((await db.ler(`houses/${casaId}`))?.["balanceIndex"]).toBe(0);
  });
});
