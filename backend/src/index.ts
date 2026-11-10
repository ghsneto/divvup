import { autenticar } from "./auth.js";
import { type Env } from "./env.js";
import { ErroHttp, json, respostaDeErro } from "./erros.js";
import { Firestore } from "./firestore.js";
import { criarPerfil } from "./operacoes/perfil.js";

/**
 * Backend do Divvup (Cloudflare Worker, ADR 0001). Rotas:
 * - `GET /saude`: verificação de disponibilidade, sem autenticação.
 * - `POST /v1/eu`: devolve o usuário autenticado e se o perfil `users/{uid}` existe.
 * - `POST /v1/criarPerfil` `{nome}`: cria `users/{uid}` com os valores iniciais (idempotente).
 * As operações protegidas da SPEC §4.2 entram aqui, uma rota por operação.
 */
export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    try {
      const url = new URL(req.url);
      if (req.method === "GET" && url.pathname === "/saude") return json({ ok: true });

      if (req.method === "POST" && url.pathname === "/v1/eu") {
        const usuario = await autenticar(req, env);
        const perfil = await new Firestore(env).ler(`users/${usuario.uid}`);
        return json({ uid: usuario.uid, temPerfil: perfil !== undefined });
      }

      if (req.method === "POST" && url.pathname === "/v1/criarPerfil") {
        const usuario = await autenticar(req, env);
        return json(await criarPerfil(new Firestore(env), usuario, await lerJson(req)));
      }

      throw new ErroHttp(404, "rota-inexistente");
    } catch (erro) {
      return respostaDeErro(erro);
    }
  },
} satisfies ExportedHandler<Env>;

async function lerJson(req: Request): Promise<unknown> {
  try {
    return await req.json();
  } catch {
    throw new ErroHttp(400, "json-invalido");
  }
}
