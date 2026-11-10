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
const corpo = async (r: Response) => (await r.json()) as Record<string, unknown>;

type Pessoa = Awaited<ReturnType<typeof criarUsuario>>;

/** Morador pela posição de entrada (falha o teste se não existir). */
function morador(outros: Pessoa[], i: number): Pessoa {
  const p = outros[i];
  if (!p) throw new Error(`morador ${i} inexistente`);
  return p;
}

async function pessoa(nome: string) {
  const u = await criarUsuario();
  await chamar("criarPerfil", u.token, { nome });
  return u;
}

/** Casa com Iane (admin) e os moradores pedidos, entrando nessa ordem. */
async function casaCom(...nomes: string[]) {
  const iane = await pessoa("Iane");
  const { casaId } = (await corpo(await chamar("criarCasa", iane.token, { nome: "Rep. Cajueiro", tipo: "REPUBLICA" }))) as { casaId: string };
  const outros: Pessoa[] = [];
  for (const nome of nomes) {
    const p = await pessoa(nome);
    const convite = await corpo(await chamar("criarConvite", iane.token));
    const codigo = convite["codigo"];
    expect(codigo, JSON.stringify(convite)).toBeTypeOf("string");
    const entrada = await chamar("entrarComCodigo", p.token, { codigo });
    expect(entrada.status, JSON.stringify(await entrada.clone().json())).toBe(200);
    outros.push(p);
  }
  return { casaId, iane, outros };
}

describe("gestão de membros (R-03, R-04)", () => {
  const db = new Firestore(envLocal);
  beforeEach(limparFirestore);

  it("admin promove e rebaixa; sempre sobra um admin", async () => {
    const { casaId, iane, outros } = await casaCom("Bruno");
    const bruno = morador(outros, 0);
    expect((await chamar("alterarPapel", iane.token, { uid: bruno.uid, papel: "ADMIN" })).status).toBe(200);
    expect((await db.ler(`houses/${casaId}/members/${bruno.uid}`))?.["role"]).toBe("ADMIN");
    expect((await chamar("alterarPapel", bruno.token, { uid: iane.uid, papel: "MEMBER" })).status).toBe(200);
    expect(await corpo(await chamar("alterarPapel", bruno.token, { uid: bruno.uid, papel: "MEMBER" }))).toEqual({ erro: "ultimo-admin" });
  });

  it("só admin muda papel, remove ou edita", async () => {
    const { iane, outros } = await casaCom("Bruno");
    const bruno = morador(outros, 0);
    expect((await chamar("alterarPapel", bruno.token, { uid: bruno.uid, papel: "ADMIN" })).status).toBe(403);
    expect((await chamar("removerMembro", bruno.token, { uid: iane.uid })).status).toBe(403);
    expect((await chamar("editarCasa", bruno.token, { nome: "X", tipo: "REPUBLICA" })).status).toBe(403);
  });

  it("admin remove um morador, que fica sem casa e com o histórico guardado", async () => {
    const { casaId, iane, outros } = await casaCom("Bruno");
    const bruno = morador(outros, 0);
    expect((await chamar("removerMembro", iane.token, { uid: bruno.uid })).status).toBe(200);
    expect(await db.ler(`houses/${casaId}/members/${bruno.uid}`)).toMatchObject({ status: "LEFT", role: "MEMBER" });
    expect((await db.ler(`users/${bruno.uid}`))?.["currentHouseId"]).toBeNull();
    expect((await db.ler(`houses/${casaId}`))?.["memberCount"]).toBe(1);
    expect(await corpo(await chamar("removerMembro", iane.token, { uid: iane.uid }))).toEqual({ erro: "use-sair-da-casa" });
  });

  it("último admin saindo promove o morador ativo mais antigo (R-04)", async () => {
    const { casaId, iane, outros } = await casaCom("Bruno", "Carla");
    const bruno = morador(outros, 0);
    const carla = morador(outros, 1);
    const resp = await chamar("sairDaCasa", iane.token);
    expect(await corpo(resp)).toEqual({ novoAdmin: bruno.uid });
    expect((await db.ler(`houses/${casaId}/members/${bruno.uid}`))?.["role"]).toBe("ADMIN");
    expect((await db.ler(`houses/${casaId}/members/${carla.uid}`))?.["role"]).toBe("MEMBER");
    expect((await db.ler(`users/${iane.uid}`))?.["currentHouseId"]).toBeNull();
    expect((await db.ler(`houses/${casaId}`))?.["memberCount"]).toBe(2);
  });

  it("quem sai pode voltar com um código novo e cria ou entra em outra casa", async () => {
    const { iane, outros } = await casaCom("Bruno");
    const bruno = morador(outros, 0);
    await chamar("sairDaCasa", bruno.token);
    const { codigo } = (await corpo(await chamar("criarConvite", iane.token))) as { codigo: string };
    expect((await chamar("entrarComCodigo", bruno.token, { codigo })).status).toBe(200);
  });

  it("morador comum sai sem mudar admins; sozinho, a casa fica vazia", async () => {
    const { casaId, iane, outros } = await casaCom("Bruno");
    const bruno = morador(outros, 0);
    expect(await corpo(await chamar("sairDaCasa", bruno.token))).toEqual({ novoAdmin: null });
    expect(await corpo(await chamar("sairDaCasa", iane.token))).toEqual({ novoAdmin: null });
    expect((await db.ler(`houses/${casaId}`))?.["memberCount"]).toBe(0);
    expect((await chamar("sairDaCasa", iane.token)).status).toBe(409);
  });

  it("admin edita a casa; Casal/Família desliga o anúncio", async () => {
    const { casaId, iane } = await casaCom();
    await db.commit([{ tipo: "gravar", caminho: `houses/${casaId}`, dados: { listing: { enabled: true, vacancies: 2 } }, campos: ["listing"] }]);
    const resp = await chamar("editarCasa", iane.token, { nome: " Rep.  Cajueiro 2 ", tipo: "CASAL_FAMILIA", cidade: "Recife", bairro: "Várzea", rua: "" });
    expect(resp.status).toBe(200);
    expect(await db.ler(`houses/${casaId}`)).toMatchObject({
      name: "Rep. Cajueiro 2",
      type: "CASAL_FAMILIA",
      city: "Recife",
      neighborhood: "Várzea",
      street: null,
      listing: { enabled: false, vacancies: 2 },
    });
    expect((await chamar("editarCasa", iane.token, { nome: "", tipo: "REPUBLICA" })).status).toBe(400);
  });
});
