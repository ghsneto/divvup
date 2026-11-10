import { beforeEach, describe, expect, it } from "vitest";
import { Firestore } from "../src/firestore.js";
import worker from "../src/index.js";
import { criarUsuario, envLocal, limparFirestore } from "./ambiente.js";

describe("Firestore pelo backend (Emulator)", () => {
  const db = new Firestore(envLocal);
  beforeEach(limparFirestore);

  it("grava e lê documento com os tipos usados no modelo", async () => {
    const quando = new Date("2026-11-10T12:00:00Z");
    await db.commit([
      {
        tipo: "gravar",
        caminho: "houses/h1",
        dados: { nome: "Rep. Cajueiro", membros: 4, ativo: true, peso: 0.5, criadaEm: quando, tags: ["a"], extra: { x: null } },
      },
    ]);
    expect(await db.ler("houses/h1")).toEqual({
      nome: "Rep. Cajueiro",
      membros: 4,
      ativo: true,
      peso: 0.5,
      criadaEm: quando,
      tags: ["a"],
      extra: { x: null },
    });
  });

  it("devolve undefined para documento inexistente", async () => {
    expect(await db.ler("houses/nao-existe")).toBeUndefined();
  });

  it("transação lê e grava atomicamente", async () => {
    await db.commit([{ tipo: "gravar", caminho: "contadores/c", dados: { valor: 1 } }]);
    const novo = await db.transacao(async (tx) => {
      const atual = (await db.ler("contadores/c", tx))?.["valor"] as number;
      return { escritas: [{ tipo: "gravar", caminho: "contadores/c", dados: { valor: atual + 1 } }], resultado: atual + 1 };
    });
    expect(novo).toBe(2);
    expect(await db.ler("contadores/c")).toEqual({ valor: 2 });
  });

  it("transações concorrentes no mesmo documento não perdem escrita", async () => {
    await db.commit([{ tipo: "gravar", caminho: "contadores/c", dados: { valor: 0 } }]);
    const incrementar = () =>
      db.transacao(async (tx) => {
        const atual = (await db.ler("contadores/c", tx))?.["valor"] as number;
        return { escritas: [{ tipo: "gravar" as const, caminho: "contadores/c", dados: { valor: atual + 1 } }], resultado: null };
      });
    await Promise.all([incrementar(), incrementar(), incrementar()]);
    expect(await db.ler("contadores/c")).toEqual({ valor: 3 });
  });

  it("exigirInexistente impede sobrescrever documento existente", async () => {
    await db.commit([{ tipo: "gravar", caminho: "houses/h1", dados: { nome: "A" } }]);
    await expect(
      db.commit([{ tipo: "gravar", caminho: "houses/h1", dados: { nome: "B" }, exigirInexistente: true }]),
    ).rejects.toThrow();
    expect(await db.ler("houses/h1")).toEqual({ nome: "A" });
  });

  it("/v1/eu indica quando o perfil do usuário existe", async () => {
    const { uid, token } = await criarUsuario();
    await db.commit([{ tipo: "gravar", caminho: `users/${uid}`, dados: { nome: "Iane" } }]);
    const resp = await worker.fetch(
      new Request("http://local/v1/eu", { method: "POST", headers: { authorization: `Bearer ${token}` } }),
      envLocal,
    );
    expect(await resp.json()).toEqual({ uid, temPerfil: true });
  });
});
