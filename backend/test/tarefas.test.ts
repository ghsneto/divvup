import { beforeEach, describe, expect, it } from "vitest";
import { Firestore } from "../src/firestore.js";
import worker from "../src/index.js";
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

async function moradora() {
  const u = await criarUsuario();
  await chamar("criarPerfil", u.token, { nome: "Ana Lima" });
  const resp = await chamar("criarCasa", u.token, { nome: "Rep. Cajueiro", tipo: "REPUBLICA" });
  const { casaId } = (await resp.json()) as { casaId: string };
  return { ...u, casaId };
}

const amanha = () => new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString();
const base = () => ({ titulo: "  Lavar a  louça ", descricao: " Pratos e panelas ", categoria: "COZINHA", peso: "MEDIA", prazo: amanha() });

describe("criarTarefa (R-09 a R-13)", () => {
  const db = new Firestore(envLocal);
  beforeEach(limparFirestore);

  it("cria o modelo sem recorrência e uma ocorrência OPEN com pontos do peso", async () => {
    const ana = await moradora();
    const resp = await chamar("criarTarefa", ana.token, base());
    expect(resp.status).toBe(200);
    const { tarefaId } = (await resp.json()) as { tarefaId: string };
    const tarefa = await db.ler(`houses/${ana.casaId}/tasks/${tarefaId}`);
    expect(tarefa).toMatchObject({
      title: "Lavar a louça",
      description: "Pratos e panelas",
      category: "COZINHA",
      points: 50,
      status: "OPEN",
      assigneeId: null,
      createdBy: ana.uid,
      createdByName: "Ana Lima",
    });
    const modelo = await db.ler(`houses/${ana.casaId}/taskTemplates/${tarefa?.["templateId"] as string}`);
    expect(modelo).toMatchObject({ title: "Lavar a louça", points: 50, recurrence: null });
  });

  it("pontos vêm do peso (25/50/100), nunca do cliente", async () => {
    const ana = await moradora();
    for (const [peso, pontos] of [["LEVE", 25], ["MEDIA", 50], ["PESADA", 100]] as const) {
      const { tarefaId } = (await (await chamar("criarTarefa", ana.token, { ...base(), peso, points: 9999 })).json()) as { tarefaId: string };
      expect((await db.ler(`houses/${ana.casaId}/tasks/${tarefaId}`))?.["points"]).toBe(pontos);
    }
  });

  it("valida título, descrição, categoria, peso e prazo", async () => {
    const ana = await moradora();
    const erro = async (extra: Record<string, unknown>) => (await (await chamar("criarTarefa", ana.token, { ...base(), ...extra })).json()) as { erro: string };
    expect(await erro({ titulo: " " })).toEqual({ erro: "titulo-invalido" });
    expect(await erro({ titulo: "x".repeat(61) })).toEqual({ erro: "titulo-invalido" });
    expect(await erro({ descricao: "x".repeat(281) })).toEqual({ erro: "descricao-invalida" });
    expect(await erro({ categoria: "JARDIM" })).toEqual({ erro: "categoria-invalida" });
    expect(await erro({ peso: "ENORME" })).toEqual({ erro: "peso-invalido" });
    expect(await erro({ prazo: new Date(Date.now() - 60_000).toISOString() })).toEqual({ erro: "prazo-no-passado" });
    expect(await erro({ prazo: "amanhã" })).toEqual({ erro: "prazo-invalido" });
  });

  it("sem casa não cria", async () => {
    const u = await criarUsuario();
    await chamar("criarPerfil", u.token, { nome: "Bruno" });
    expect(await (await chamar("criarTarefa", u.token, base())).json()).toEqual({ erro: "sem-casa" });
  });
});
