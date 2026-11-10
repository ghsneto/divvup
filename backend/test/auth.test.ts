import { exportJWK, generateKeyPair, createLocalJWKSet, SignJWT } from "jose";
import { describe, expect, it } from "vitest";
import { autenticar } from "../src/auth.js";
import worker from "../src/index.js";
import { criarUsuario, envLocal, PROJETO, tokenSemAssinatura } from "./ambiente.js";

const chamarEu = (token?: string) =>
  worker.fetch(
    new Request("http://local/v1/eu", {
      method: "POST",
      headers: token ? { authorization: `Bearer ${token}` } : {},
    }),
    envLocal,
  );

const agora = () => Math.floor(Date.now() / 1000);

describe("autenticação no Emulator", () => {
  it("aceita o ID token de um usuário do Auth Emulator", async () => {
    const { uid, token } = await criarUsuario();
    const resp = await chamarEu(token);
    expect(resp.status).toBe(200);
    expect(await resp.json()).toEqual({ uid, temPerfil: false });
  });

  it("rejeita chamada sem token", async () => {
    const resp = await chamarEu();
    expect(resp.status).toBe(401);
    expect(await resp.json()).toEqual({ erro: "nao-autenticado" });
  });

  it("rejeita token de outro projeto", async () => {
    const token = tokenSemAssinatura({
      iss: "https://securetoken.google.com/outro",
      aud: "outro",
      sub: "x",
      exp: agora() + 600,
    });
    expect((await chamarEu(token)).status).toBe(401);
  });

  it("rejeita token expirado", async () => {
    const token = tokenSemAssinatura({
      iss: `https://securetoken.google.com/${PROJETO}`,
      aud: PROJETO,
      sub: "x",
      exp: agora() - 1,
    });
    expect((await chamarEu(token)).status).toBe(401);
  });

  it("rejeita lixo no lugar do token", async () => {
    expect((await chamarEu("isso-nao-e-um-jwt")).status).toBe(401);
  });
});

describe("autenticação publicada (assinatura RS256)", () => {
  const envProd = { FIREBASE_PROJECT_ID: "divvup-prod" };

  async function cenario() {
    const { publicKey, privateKey } = await generateKeyPair("RS256");
    const jwk = { ...(await exportJWK(publicKey)), kid: "k1", alg: "RS256" };
    const chaves = createLocalJWKSet({ keys: [jwk] });
    const assinar = (decl: Record<string, unknown>, chave = privateKey) =>
      new SignJWT(decl)
        .setProtectedHeader({ alg: "RS256", kid: "k1" })
        .setIssuer("https://securetoken.google.com/divvup-prod")
        .setAudience("divvup-prod")
        .setIssuedAt()
        .setExpirationTime("10m")
        .sign(chave);
    return { chaves, assinar };
  }

  const req = (token: string) => new Request("http://x", { headers: { authorization: `Bearer ${token}` } });

  it("aceita token assinado pela chave do Firebase", async () => {
    const { chaves, assinar } = await cenario();
    const token = await assinar({ sub: "uid-1", email: "a@b.c" });
    await expect(autenticar(req(token), envProd, chaves)).resolves.toEqual({ uid: "uid-1", email: "a@b.c" });
  });

  it("rejeita token sem assinatura em produção", async () => {
    const { chaves } = await cenario();
    const token = tokenSemAssinatura({
      iss: "https://securetoken.google.com/divvup-prod",
      aud: "divvup-prod",
      sub: "x",
      exp: agora() + 600,
    });
    await expect(autenticar(req(token), envProd, chaves)).rejects.toMatchObject({ status: 401 });
  });

  it("rejeita token assinado por outra chave", async () => {
    const { chaves, assinar } = await cenario();
    const { privateKey: outra } = await generateKeyPair("RS256");
    const token = await assinar({ sub: "uid-1" }, outra);
    await expect(autenticar(req(token), envProd, chaves)).rejects.toMatchObject({ status: 401 });
  });

  it("rejeita token sem sub", async () => {
    const { chaves, assinar } = await cenario();
    const token = await assinar({});
    await expect(autenticar(req(token), envProd, chaves)).rejects.toMatchObject({ status: 401 });
  });
});
