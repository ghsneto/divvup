import { beforeEach, describe, expect, it } from "vitest";
import { Firestore } from "../src/firestore.js";
import worker from "../src/index.js";
import { ALFABETO_CODIGO, criarConvite, gerarCodigo, VALIDADE_MS } from "../src/operacoes/convites.js";
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

async function pessoa(nome: string) {
  const u = await criarUsuario();
  await chamar("criarPerfil", u.token, { nome });
  return u;
}

async function casaComAdmin() {
  const admin = await pessoa("Iane");
  const { casaId } = (await (await chamar("criarCasa", admin.token, { nome: "Rep. Cajueiro", tipo: "REPUBLICA" })).json()) as { casaId: string };
  return { admin, casaId };
}

async function gerar(token: string) {
  const resp = await chamar("criarConvite", token);
  expect(resp.status).toBe(200);
  return (await resp.json()) as { codigo: string; expiraEm: string };
}

describe("convites (R-06)", () => {
  const db = new Firestore(envLocal);
  beforeEach(limparFirestore);

  it("gera códigos de 6 caracteres sem ambíguos", () => {
    for (let i = 0; i < 200; i++) {
      const c = gerarCodigo();
      expect(c).toHaveLength(6);
      expect([...c].every((ch) => ALFABETO_CODIGO.includes(ch))).toBe(true);
      expect(c).not.toMatch(/[O0I1L]/);
    }
  });

  it("o admin gera um código válido por 24 h e alguém entra com ele", async () => {
    const { admin, casaId } = await casaComAdmin();
    const { codigo, expiraEm } = await gerar(admin.token);
    expect(new Date(expiraEm).getTime() - Date.now()).toBeGreaterThan(VALIDADE_MS - 60_000);
    expect(await db.ler(`houses/${casaId}/invites/${codigo}`)).toMatchObject({ createdBy: admin.uid, createdByName: "Iane", revoked: false, usedBy: null });

    const bruno = await pessoa("Bruno");
    // Aceita minúsculas e separadores.
    const resp = await chamar("entrarComCodigo", bruno.token, { codigo: `${codigo.slice(0, 3).toLowerCase()}-${codigo.slice(3)}` });
    expect(resp.status).toBe(200);
    expect(await resp.json()).toEqual({ casaId });
    expect(await db.ler(`houses/${casaId}/members/${bruno.uid}`)).toMatchObject({ role: "MEMBER", status: "ACTIVE", displayName: "Bruno" });
    expect(await db.ler(`houses/${casaId}`)).toMatchObject({ memberCount: 2 });
    expect((await db.ler(`users/${bruno.uid}`))?.["currentHouseId"]).toBe(casaId);
    expect(await db.ler(`houses/${casaId}/invites/${codigo}`)).toMatchObject({ usedBy: bruno.uid, usedByName: "Bruno" });
  });

  it("o código é de uso único", async () => {
    const { admin } = await casaComAdmin();
    const { codigo } = await gerar(admin.token);
    const [a, b] = [await pessoa("Bruno"), await pessoa("Carla")];
    expect((await chamar("entrarComCodigo", a.token, { codigo })).status).toBe(200);
    const segunda = await chamar("entrarComCodigo", b.token, { codigo });
    expect(segunda.status).toBe(409);
    expect(await segunda.json()).toEqual({ erro: "codigo-usado" });
  });

  it("duas pessoas usando o mesmo código ao mesmo tempo: só uma entra", async () => {
    const { admin, casaId } = await casaComAdmin();
    const { codigo } = await gerar(admin.token);
    const pessoas = await Promise.all(["Bruno", "Carla", "Davi"].map(pessoa));
    const respostas = await Promise.all(pessoas.map((p) => chamar("entrarComCodigo", p.token, { codigo })));
    expect(respostas.map((r) => r.status).sort()).toEqual([200, 409, 409]);
    expect((await db.ler(`houses/${casaId}`))?.["memberCount"]).toBe(2);
  });

  it("o código expira em 24 h", async () => {
    const { admin } = await casaComAdmin();
    const ontem = new Date(Date.now() - VALIDADE_MS - 1000);
    const { codigo } = await criarConvite(db, { uid: admin.uid }, ontem);
    const bruno = await pessoa("Bruno");
    const resp = await chamar("entrarComCodigo", bruno.token, { codigo });
    expect(resp.status).toBe(410);
    expect(await resp.json()).toEqual({ erro: "codigo-expirado" });
  });

  it("código inexistente, malformado ou revogado é inválido", async () => {
    const { admin } = await casaComAdmin();
    const bruno = await pessoa("Bruno");
    expect(await (await chamar("entrarComCodigo", bruno.token, { codigo: "ABCDEF" })).json()).toEqual({ erro: "codigo-invalido" });
    expect((await chamar("entrarComCodigo", bruno.token, { codigo: "AB0" })).status).toBe(404);

    const { codigo } = await gerar(admin.token);
    expect(await (await chamar("revogarConvite", admin.token, { codigo })).json()).toEqual({ revogado: true });
    expect(await (await chamar("entrarComCodigo", bruno.token, { codigo })).json()).toEqual({ erro: "codigo-invalido" });
  });

  it("quem já está numa casa não entra em outra (R-02)", async () => {
    const { admin } = await casaComAdmin();
    const { codigo } = await gerar(admin.token);
    const outro = await pessoa("Bruno");
    await chamar("criarCasa", outro.token, { nome: "Outra", tipo: "CASAL_FAMILIA" });
    const resp = await chamar("entrarComCodigo", outro.token, { codigo });
    expect(await resp.json()).toEqual({ erro: "ja-em-casa" });
  });

  it("só admin gera e revoga; usado não se revoga", async () => {
    const { admin } = await casaComAdmin();
    const { codigo } = await gerar(admin.token);
    const bruno = await pessoa("Bruno");
    await chamar("entrarComCodigo", bruno.token, { codigo });
    expect((await chamar("criarConvite", bruno.token)).status).toBe(403);
    expect((await chamar("revogarConvite", admin.token, { codigo })).status).toBe(409);
    const semCasa = await pessoa("Carla");
    expect(await (await chamar("criarConvite", semCasa.token)).json()).toEqual({ erro: "sem-casa" });
  });
});
