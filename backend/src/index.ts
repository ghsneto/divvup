import { autenticar } from "./auth.js";
import { type Env } from "./env.js";
import { ErroHttp, json, respostaDeErro } from "./erros.js";
import { Firestore } from "./firestore.js";
import { criarCasa } from "./operacoes/casa.js";
import { criarConvite, entrarComCodigo, revogarConvite } from "./operacoes/convites.js";
import { alterarPapel, editarCasa, removerMembro, sairDaCasa } from "./operacoes/membros.js";
import { assumirTarefa, desfazer, passarTarefa, reverPassadas } from "./operacoes/match.js";
import { cancelarTarefa, concluirTarefa, criarTarefa, devolverTarefa, editarTarefa, excluirTarefa } from "./operacoes/tarefas.js";
import { criarPerfil } from "./operacoes/perfil.js";

/**
 * Backend do Divvup (Cloudflare Worker, ADR 0001). Rotas:
 * - `GET /saude`: verificação de disponibilidade, sem autenticação.
 * - `POST /v1/eu`: devolve o usuário autenticado e se o perfil `users/{uid}` existe.
 * - `POST /v1/criarPerfil` `{nome}`: cria `users/{uid}` com os valores iniciais (idempotente).
 * - `POST /v1/criarCasa` `{nome, tipo, anunciar}`: cria a casa com quem pediu como admin (R-02, R-03).
 * - `POST /v1/criarConvite`, `/v1/revogarConvite` `{codigo}`, `/v1/entrarComCodigo` `{codigo}` (R-06).
 * - `POST /v1/alterarPapel` `{uid, papel}`, `/v1/removerMembro` `{uid}`, `/v1/sairDaCasa`,
 *   `/v1/editarCasa` `{nome, tipo, cidade?, bairro?, rua?}` (R-03, R-04).
 * - `POST /v1/criarTarefa` `{titulo, descricao?, categoria, peso, prazo}` (R-09 a R-13).
 * - `POST /v1/assumirTarefa` / `/v1/passarTarefa` `{tarefaId}`, `/v1/desfazer` `{tarefaId, acao}`,
 *   `/v1/reverPassadas` (R-15 a R-20).
 * - `POST /v1/concluirTarefa`, `/v1/devolverTarefa`, `/v1/cancelarTarefa`, `/v1/excluirTarefa`
 *   `{tarefaId}` e `/v1/editarTarefa` `{tarefaId, titulo, …}` (R-12, R-19, R-21, R-22).
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

      if (req.method === "POST" && url.pathname === "/v1/criarCasa") {
        const usuario = await autenticar(req, env);
        return json(await criarCasa(new Firestore(env), usuario, await lerJson(req)));
      }

      if (req.method === "POST" && url.pathname === "/v1/criarConvite") {
        const usuario = await autenticar(req, env);
        return json(await criarConvite(new Firestore(env), usuario));
      }

      if (req.method === "POST" && url.pathname === "/v1/revogarConvite") {
        const usuario = await autenticar(req, env);
        return json(await revogarConvite(new Firestore(env), usuario, await lerJson(req)));
      }

      if (req.method === "POST" && url.pathname === "/v1/entrarComCodigo") {
        const usuario = await autenticar(req, env);
        return json(await entrarComCodigo(new Firestore(env), usuario, await lerJson(req)));
      }

      const operacoes: Record<string, (db: Firestore, u: Awaited<ReturnType<typeof autenticar>>, corpo: unknown) => Promise<unknown>> = {
        "/v1/alterarPapel": alterarPapel,
        "/v1/removerMembro": (db, u, corpo) => removerMembro(db, u, corpo),
        "/v1/sairDaCasa": (db, u) => sairDaCasa(db, u),
        "/v1/editarCasa": editarCasa,
        "/v1/criarTarefa": (db, u, corpo) => criarTarefa(db, u, corpo),
        "/v1/assumirTarefa": (db, u, corpo) => assumirTarefa(db, u, corpo),
        "/v1/passarTarefa": (db, u, corpo) => passarTarefa(db, u, corpo),
        "/v1/desfazer": (db, u, corpo) => desfazer(db, u, corpo),
        "/v1/reverPassadas": (db, u) => reverPassadas(db, u),
        "/v1/concluirTarefa": (db, u, corpo) => concluirTarefa(db, u, corpo),
        "/v1/devolverTarefa": (db, u, corpo) => devolverTarefa(db, u, corpo),
        "/v1/cancelarTarefa": (db, u, corpo) => cancelarTarefa(db, u, corpo),
        "/v1/editarTarefa": (db, u, corpo) => editarTarefa(db, u, corpo),
        "/v1/excluirTarefa": excluirTarefa,
      };
      const operacao = operacoes[url.pathname];
      if (req.method === "POST" && operacao) {
        const usuario = await autenticar(req, env);
        return json(await operacao(new Firestore(env), usuario, await lerJson(req)));
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
