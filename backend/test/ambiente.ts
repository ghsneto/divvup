import type { Env } from "../src/env.js";

/** Projeto fictício: o prefixo `demo-` impede o Emulator de alcançar recursos reais. */
export const PROJETO = "demo-divvup";
export const AUTH = process.env["FIREBASE_AUTH_EMULATOR_HOST"] ?? "127.0.0.1:9099";
export const FIRESTORE = process.env["FIRESTORE_EMULATOR_HOST"] ?? "127.0.0.1:8080";

export const envLocal: Env = {
  FIREBASE_PROJECT_ID: PROJETO,
  FIREBASE_AUTH_EMULATOR_HOST: AUTH,
  FIRESTORE_EMULATOR_HOST: FIRESTORE,
};

/** Cria um usuário no Auth Emulator e devolve uid e ID token. */
export async function criarUsuario(email = `u${Date.now()}${Math.random()}@teste.dev`) {
  const resp = await fetch(`http://${AUTH}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=chave-falsa`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ email, password: "senha-de-teste", returnSecureToken: true }),
  });
  if (!resp.ok) throw new Error(`signUp: ${resp.status} ${await resp.text()}`);
  const corpo = (await resp.json()) as { localId: string; idToken: string };
  return { uid: corpo.localId, token: corpo.idToken };
}

/** Apaga todos os documentos do Firestore Emulator. */
export async function limparFirestore() {
  await fetch(`http://${FIRESTORE}/emulator/v1/projects/${PROJETO}/databases/(default)/documents`, { method: "DELETE" });
}

/** JWT sem assinatura, como os do Auth Emulator, com as declarações dadas. */
export function tokenSemAssinatura(declaracoes: Record<string, unknown>): string {
  const b64 = (o: unknown) => Buffer.from(JSON.stringify(o)).toString("base64url");
  return `${b64({ alg: "none", typ: "JWT" })}.${b64(declaracoes)}.`;
}
