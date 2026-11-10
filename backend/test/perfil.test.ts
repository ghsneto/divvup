import { beforeEach, describe, expect, it } from "vitest";
import { Firestore } from "../src/firestore.js";
import worker from "../src/index.js";
import { criarUsuario, envLocal, limparFirestore } from "./ambiente.js";

const criarPerfil = (token: string, corpo: unknown) =>
  worker.fetch(
    new Request("http://local/v1/criarPerfil", {
      method: "POST",
      headers: { authorization: `Bearer ${token}`, "content-type": "application/json" },
      body: JSON.stringify(corpo),
    }),
    envLocal,
  );

describe("criarPerfil", () => {
  const db = new Firestore(envLocal);
  beforeEach(limparFirestore);

  it("cria users/{uid} com valores iniciais zerados", async () => {
    const { uid, token } = await criarUsuario("iane@email.com");
    const resp = await criarPerfil(token, { nome: "  Iane   Souza " });
    expect(resp.status).toBe(200);
    expect(await resp.json()).toEqual({ criado: true });

    const perfil = await db.ler(`users/${uid}`);
    expect(perfil).toMatchObject({
      displayName: "Iane Souza",
      email: "iane@email.com",
      avatarId: null,
      currentHouseId: null,
      totalPoints: 0,
      tasksDone: 0,
      streak: { current: 0, best: 0, lastDay: null },
      badges: [],
      quietHours: { start: "22:00", end: "08:00" },
    });
  });

  it("é idempotente e não sobrescreve o perfil existente", async () => {
    const { uid, token } = await criarUsuario();
    await criarPerfil(token, { nome: "Iane" });
    const segunda = await criarPerfil(token, { nome: "Outro nome" });
    expect(await segunda.json()).toEqual({ criado: false });
    expect((await db.ler(`users/${uid}`))?.["displayName"]).toBe("Iane");
  });

  it("rejeita nome vazio ou longo demais", async () => {
    const { token } = await criarUsuario();
    expect((await criarPerfil(token, { nome: "   " })).status).toBe(400);
    expect((await criarPerfil(token, { nome: "x".repeat(41) })).status).toBe(400);
    expect((await criarPerfil(token, {})).status).toBe(400);
  });

  it("exige login", async () => {
    const resp = await worker.fetch(new Request("http://local/v1/criarPerfil", { method: "POST", body: "{}" }), envLocal);
    expect(resp.status).toBe(401);
  });
});
