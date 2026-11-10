import { beforeEach, describe, expect, it } from "vitest";
import { Firestore } from "../src/firestore.js";
import worker from "../src/index.js";
import { temporada } from "../src/operacoes/casa.js";
import { criarUsuario, envLocal, limparFirestore } from "./ambiente.js";

const chamar = (rota: string, token: string, corpo: unknown) =>
  worker.fetch(
    new Request(`http://local/v1/${rota}`, {
      method: "POST",
      headers: { authorization: `Bearer ${token}`, "content-type": "application/json" },
      body: JSON.stringify(corpo),
    }),
    envLocal,
  );

async function usuarioComPerfil(nome = "Iane") {
  const u = await criarUsuario();
  await chamar("criarPerfil", u.token, { nome });
  return u;
}

describe("criarCasa (R-02, R-03)", () => {
  const db = new Firestore(envLocal);
  beforeEach(limparFirestore);

  it("cria a casa, o membro ADMIN e o vínculo do usuário numa transação", async () => {
    const { uid, token } = await usuarioComPerfil();
    const resp = await chamar("criarCasa", token, { nome: "  Rep.   Cajueiro ", tipo: "REPUBLICA", anunciar: true });
    expect(resp.status).toBe(200);
    const { casaId } = (await resp.json()) as { casaId: string };
    expect(casaId).toMatch(/^[A-Za-z0-9]{20}$/);

    expect(await db.ler(`houses/${casaId}`)).toMatchObject({
      name: "Rep. Cajueiro",
      type: "REPUBLICA",
      createdBy: uid,
      memberCount: 1,
      currentSeason: temporada(new Date()),
      listing: { enabled: true },
    });
    expect(await db.ler(`houses/${casaId}/members/${uid}`)).toMatchObject({
      role: "ADMIN",
      status: "ACTIVE",
      displayName: "Iane",
      seasonPoints: 0,
      totalPoints: 0,
      openTasks: 0,
    });
    // Só currentHouseId muda no perfil; o resto continua igual.
    expect(await db.ler(`users/${uid}`)).toMatchObject({ currentHouseId: casaId, displayName: "Iane", totalPoints: 0 });
  });

  it("quem já está numa casa não cria outra (R-02)", async () => {
    const { uid, token } = await usuarioComPerfil();
    const primeira = (await (await chamar("criarCasa", token, { nome: "Casa 1", tipo: "CASAL_FAMILIA" })).json()) as { casaId: string };
    const segunda = await chamar("criarCasa", token, { nome: "Casa 2", tipo: "REPUBLICA" });
    expect(segunda.status).toBe(409);
    expect(await segunda.json()).toEqual({ erro: "ja-em-casa" });
    expect((await db.ler(`users/${uid}`))?.["currentHouseId"]).toBe(primeira.casaId);
  });

  it("dois pedidos ao mesmo tempo criam só uma casa", async () => {
    const { uid, token } = await usuarioComPerfil();
    const respostas = await Promise.all([1, 2, 3].map((n) => chamar("criarCasa", token, { nome: `Casa ${n}`, tipo: "REPUBLICA" })));
    expect(respostas.map((r) => r.status).sort()).toEqual([200, 409, 409]);
    const vencedora = respostas.find((r) => r.status === 200);
    const ok = (await vencedora?.json()) as { casaId: string };
    expect((await db.ler(`users/${uid}`))?.["currentHouseId"]).toBe(ok.casaId);
  });

  it("anunciar vagas só vale para república", async () => {
    const { token } = await usuarioComPerfil();
    const { casaId } = (await (await chamar("criarCasa", token, { nome: "Apê", tipo: "MORADIA_COMPARTILHADA", anunciar: true })).json()) as {
      casaId: string;
    };
    expect((await db.ler(`houses/${casaId}`))?.["listing"]).toEqual({ enabled: false });
  });

  it("valida nome (1–32), tipo e anunciar", async () => {
    const { token } = await usuarioComPerfil();
    expect((await chamar("criarCasa", token, { nome: "  ", tipo: "REPUBLICA" })).status).toBe(400);
    expect((await chamar("criarCasa", token, { nome: "x".repeat(33), tipo: "REPUBLICA" })).status).toBe(400);
    expect((await chamar("criarCasa", token, { nome: "Casa", tipo: "CASTELO" })).status).toBe(400);
    expect((await chamar("criarCasa", token, { nome: "Casa", tipo: "REPUBLICA", anunciar: "sim" })).status).toBe(400);
    expect((await chamar("criarCasa", token, { nome: "x".repeat(32), tipo: "REPUBLICA" })).status).toBe(200);
  });

  it("exige perfil e login", async () => {
    const { token } = await criarUsuario();
    expect((await chamar("criarCasa", token, { nome: "Casa", tipo: "REPUBLICA" })).status).toBe(409);
    const semLogin = await worker.fetch(new Request("http://local/v1/criarCasa", { method: "POST", body: "{}" }), envLocal);
    expect(semLogin.status).toBe(401);
  });

  it("a temporada vira no horário de Brasília", () => {
    expect(temporada(new Date("2026-12-01T02:59:00Z"))).toBe("2026-11");
    expect(temporada(new Date("2026-12-01T03:00:00Z"))).toBe("2026-12");
  });
});
