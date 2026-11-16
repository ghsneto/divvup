import { beforeEach, describe, expect, it } from "vitest";
import { Firestore } from "../src/firestore.js";
import worker from "../src/index.js";
import { atribuirAutomaticamente, marcarAtrasadas } from "../src/operacoes/agendadas.js";
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
const HORA = 60 * 60 * 1000;

describe("rotina agendada: atrasadas (R-22, R-23)", () => {
  const db = new Firestore(envLocal);
  beforeEach(limparFirestore);

  async function casaComTarefas() {
    const ana = await criarUsuario();
    await chamar("criarPerfil", ana.token, { nome: "Ana" });
    const { casaId } = (await json(await chamar("criarCasa", ana.token, { nome: "Rep", tipo: "REPUBLICA" }))) as { casaId: string };
    const criar = async () => {
      const prazo = new Date(Date.now() + 2 * HORA).toISOString();
      return ((await json(await chamar("criarTarefa", ana.token, { titulo: "Lavar", categoria: "COZINHA", peso: "MEDIA", prazo }))) as { tarefaId: string }).tarefaId;
    };
    const assumida = await criar();
    const aberta = await criar();
    const emDia = await criar();
    await chamar("assumirTarefa", ana.token, { tarefaId: assumida });
    await chamar("assumirTarefa", ana.token, { tarefaId: emDia });
    // O prazo das duas primeiras passa (o backend não aceita criar no passado).
    const passado = new Date(Date.now() - HORA);
    await db.commit(
      [assumida, aberta].map((id) => ({ tipo: "gravar" as const, caminho: `houses/${casaId}/tasks/${id}`, dados: { dueAt: passado }, campos: ["dueAt"] })),
    );
    return { ana, casaId, assumida, aberta, emDia };
  }

  it("marca LATE só as assumidas vencidas, e concluir depois vale metade", async () => {
    const { ana, casaId, assumida, aberta, emDia } = await casaComTarefas();
    expect(await marcarAtrasadas(db)).toEqual({ atrasadas: 1 });
    const status = async (id: string) => (await db.ler(`houses/${casaId}/tasks/${id}`))?.["status"];
    expect(await status(assumida)).toBe("LATE");
    // Aberta vencida não é atraso de ninguém; a atribuição automática cuida dela (R-26).
    expect(await status(aberta)).toBe("OPEN");
    expect(await status(emDia)).toBe("ASSIGNED");

    // Rodar de novo não muda nada.
    expect(await marcarAtrasadas(db)).toEqual({ atrasadas: 0 });

    const r = await json(await chamar("concluirTarefa", ana.token, { tarefaId: assumida }));
    expect(r["pontos"]).toBe(25);
    expect(r["noPrazo"]).toBe(false);
    expect(await status(assumida)).toBe("DONE");
  });

  it("o Cron Trigger chama a rotina", async () => {
    const { casaId, assumida } = await casaComTarefas();
    const espera: Promise<unknown>[] = [];
    const ctx = { waitUntil: (p: Promise<unknown>) => espera.push(p), passThroughOnException: () => {}, props: {} } as unknown as ExecutionContext;
    await worker.scheduled({ scheduledTime: Date.now(), cron: "*/15 * * * *", noRetry: () => {} } as ScheduledController, envLocal, ctx);
    await Promise.all(espera);
    expect((await db.ler(`houses/${casaId}/tasks/${assumida}`))?.["status"]).toBe("LATE");
  });
});

describe("rotina agendada: atribuição automática (R-26)", () => {
  const db = new Firestore(envLocal);
  beforeEach(limparFirestore);

  async function casaDeTres() {
    const pessoas = [];
    for (const nome of ["Ana", "Bruno", "Caio"]) {
      const p = await criarUsuario();
      await chamar("criarPerfil", p.token, { nome });
      pessoas.push(p);
    }
    const [ana, bruno, caio] = pessoas as [Awaited<ReturnType<typeof criarUsuario>>, Awaited<ReturnType<typeof criarUsuario>>, Awaited<ReturnType<typeof criarUsuario>>];
    const { casaId } = (await json(await chamar("criarCasa", ana.token, { nome: "Rep", tipo: "REPUBLICA" }))) as { casaId: string };
    for (const p of [bruno, caio]) {
      const { codigo } = (await json(await chamar("criarConvite", ana.token))) as { codigo: string };
      await chamar("entrarComCodigo", p.token, { codigo });
    }
    const criar = async (emHoras: number) => {
      const prazo = new Date(Date.now() + emHoras * HORA).toISOString();
      return ((await json(await chamar("criarTarefa", ana.token, { titulo: "Lixo", categoria: "LIXO", peso: "LEVE", prazo }))) as { tarefaId: string }).tarefaId;
    };
    const pontos = (uid: string, n: number) => ({ tipo: "gravar" as const, caminho: `houses/${casaId}/members/${uid}`, dados: { seasonPoints: n, openTasks: 5 }, campos: ["seasonPoints", "openTasks"] });
    return { casaId, ana, bruno, caio, criar, pontos };
  }

  it("vai para quem tem menos pontos no mês, mesmo no limite de 5, com via AUTO", async () => {
    const { casaId, ana, bruno, caio, criar, pontos } = await casaDeTres();
    await db.commit([pontos(ana.uid, 300), pontos(bruno.uid, 50), pontos(caio.uid, 200)]);
    const perto = await criar(20);
    const longe = await criar(30);
    expect(await atribuirAutomaticamente(db)).toEqual({ atribuidas: 1 });
    const t = await db.ler(`houses/${casaId}/tasks/${perto}`);
    expect(t?.["status"]).toBe("ASSIGNED");
    expect(t?.["assigneeId"]).toBe(bruno.uid);
    expect(t?.["assignedVia"]).toBe("AUTO");
    expect((await db.ler(`houses/${casaId}/members/${bruno.uid}`))?.["openTasks"]).toBe(6);
    expect((await db.ler(`houses/${casaId}/tasks/${longe}`))?.["status"]).toBe("OPEN");
  });

  it("empate é decidido por sorteio entre os empatados", async () => {
    const { casaId, ana, bruno, caio, criar, pontos } = await casaDeTres();
    await db.commit([pontos(ana.uid, 300), pontos(bruno.uid, 50), pontos(caio.uid, 50)]);
    const tarefa = await criar(10);
    const empatados = [bruno.uid, caio.uid].sort();
    let vistos = 0;
    await atribuirAutomaticamente(db, new Date(), (n) => {
      vistos = n;
      return 1;
    });
    expect(vistos).toBe(2);
    expect((await db.ler(`houses/${casaId}/tasks/${tarefa}`))?.["assigneeId"]).toBe(empatados[1]);
  });
});
