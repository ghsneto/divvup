import type { Usuario } from "../auth.js";
import { usaEmulador, type Env } from "../env.js";
import { ErroHttp } from "../erros.js";
import type { Escrita, Firestore } from "../firestore.js";
import { saida, sucessor } from "./membros.js";

/** E7: a exclusão exige login recente (reautenticação no app). */
export const JANELA_DE_REAUTENTICACAO_S = 5 * 60;

/**
 * E7 (Play Store): apaga a conta. Se a pessoa está numa casa, sai dela como em "Sair da casa"
 * (R-29, R-04: tarefas voltam ao deck, o mais antigo vira admin) e a participação fica anônima
 * (nome e avatar apagados no membro e no Histórico: aparece como "ex-morador"). Depois apaga
 * `users/{uid}` com os avisos e, por último, a conta no Firebase Auth.
 */
export async function excluirConta(db: Firestore, env: Env, usuario: Usuario, corpo: unknown, agora: Date = new Date(), http: typeof fetch = fetch) {
  const c = typeof corpo === "object" && corpo !== null ? (corpo as Record<string, unknown>) : {};
  if (c["confirmacao"] !== "EXCLUIR") throw new ErroHttp(400, "confirmacao-invalida");
  const loginHa = Math.floor(agora.getTime() / 1000) - (usuario.autenticadoEm ?? 0);
  if (loginHa > JANELA_DE_REAUTENTICACAO_S) throw new ErroHttp(401, "reautenticar");

  const casaId = await db.transacao(async (tx) => {
    const [perfil] = await db.lerVarios([`users/${usuario.uid}`], tx);
    const casa = perfil?.["currentHouseId"];
    if (typeof casa !== "string") return { escritas: [], resultado: null };
    const [casaDoc, membro] = await db.lerVarios([`houses/${casa}`, `houses/${casa}/members/${usuario.uid}`], tx);
    if (!casaDoc || membro?.["status"] !== "ACTIVE") return { escritas: [], resultado: casa };
    const escritas: Escrita[] = await saida(db, tx, casa, usuario.uid, membro, casaDoc, agora);
    const novoAdmin = await sucessor(db, tx, casa, usuario.uid, membro);
    if (novoAdmin) {
      escritas.push({ tipo: "gravar", caminho: `houses/${casa}/members/${novoAdmin}`, dados: { role: "ADMIN" }, campos: ["role"], exigirExistente: true });
    }
    return { escritas, resultado: casa };
  });

  if (casaId) {
    // Anônimo na casa: membro e eventos sem nome nem avatar.
    const escritas: Escrita[] = [
      {
        tipo: "gravar",
        caminho: `houses/${casaId}/members/${usuario.uid}`,
        dados: { displayName: null, avatarId: null, badges: {}, streak: null, deleted: true },
        campos: ["displayName", "avatarId", "badges", "streak", "deleted"],
      },
    ];
    for (const ev of await db.listar(`houses/${casaId}/activity`, undefined, { campo: "actorId", valor: usuario.uid })) {
      escritas.push({ tipo: "gravar", caminho: `houses/${casaId}/activity/${ev.uid}`, dados: { actorName: null, actorAvatar: null }, campos: ["actorName", "actorAvatar"] });
    }
    for (let i = 0; i < escritas.length; i += LOTE) await db.commit(escritas.slice(i, i + LOTE));
  }

  const avisos = await db.listar(`users/${usuario.uid}/notifications`);
  const apagar: Escrita[] = [...avisos.map((a): Escrita => ({ tipo: "apagar", caminho: `users/${usuario.uid}/notifications/${a.uid}` })), { tipo: "apagar", caminho: `users/${usuario.uid}` }];
  for (let i = 0; i < apagar.length; i += LOTE) await db.commit(apagar.slice(i, i + LOTE));

  await apagarNoAuth(db, env, usuario.uid, http);
  return { ok: true as const };
}

const LOTE = 400;

async function apagarNoAuth(db: Firestore, env: Env, uid: string, http: typeof fetch): Promise<void> {
  const base = usaEmulador(env) && env.FIREBASE_AUTH_EMULATOR_HOST ? `http://${env.FIREBASE_AUTH_EMULATOR_HOST}/identitytoolkit.googleapis.com` : "https://identitytoolkit.googleapis.com";
  const resp = await http(`${base}/v1/projects/${env.FIREBASE_PROJECT_ID}/accounts:delete`, {
    method: "POST",
    headers: { authorization: `Bearer ${await db.tokenDeAcesso()}`, "content-type": "application/json" },
    body: JSON.stringify({ localId: uid }),
  });
  if (!resp.ok) throw new Error(`accounts:delete ${resp.status} ${await resp.text()}`);
}
