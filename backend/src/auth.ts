import { createRemoteJWKSet, decodeJwt, jwtVerify, type JWTPayload, type JWTVerifyGetKey } from "jose";
import { type Env, usaEmulador } from "./env.js";
import { naoAutenticado } from "./erros.js";

/** Chaves públicas do Firebase Auth (tokens de ID assinados com RS256). */
const CHAVES_FIREBASE = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"),
);

export interface Usuario {
  uid: string;
  email?: string;
}

/**
 * Valida o ID token do Firebase Auth enviado como `Authorization: Bearer <token>`.
 * Publicado: verifica assinatura RS256, `iss`, `aud`, `exp` e `sub`.
 * No Emulator os tokens não são assinados: valida só as declarações (nunca em produção).
 */
export async function autenticar(req: Request, env: Env, chaves: JWTVerifyGetKey = CHAVES_FIREBASE): Promise<Usuario> {
  const cabecalho = req.headers.get("authorization") ?? "";
  const [tipo, token] = cabecalho.split(" ");
  if (tipo !== "Bearer" || !token) throw naoAutenticado("sem token");

  const emissor = `https://securetoken.google.com/${env.FIREBASE_PROJECT_ID}`;
  let payload: JWTPayload;
  try {
    if (usaEmulador(env)) {
      payload = decodeJwt(token);
      validarDeclaracoes(payload, emissor, env.FIREBASE_PROJECT_ID);
    } else {
      ({ payload } = await jwtVerify(token, chaves, {
        issuer: emissor,
        audience: env.FIREBASE_PROJECT_ID,
        algorithms: ["RS256"],
      }));
    }
  } catch {
    throw naoAutenticado("token inválido");
  }

  if (typeof payload.sub !== "string" || payload.sub.length === 0) throw naoAutenticado("sem sub");
  const usuario: Usuario = { uid: payload.sub };
  if (typeof payload["email"] === "string") usuario.email = payload["email"];
  return usuario;
}

function validarDeclaracoes(p: JWTPayload, emissor: string, projeto: string): void {
  const agora = Math.floor(Date.now() / 1000);
  if (p.iss !== emissor) throw new Error("iss");
  if (p.aud !== projeto) throw new Error("aud");
  if (typeof p.exp !== "number" || p.exp <= agora) throw new Error("exp");
}
