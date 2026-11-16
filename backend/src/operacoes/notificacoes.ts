import type { Usuario } from "../auth.js";
import { usaEmulador, type Env } from "../env.js";
import { ErroHttp } from "../erros.js";
import type { Documento, Escrita, Firestore } from "../firestore.js";
import { gerarId } from "./casa.js";

/** Tipos da tabela R-33 (v1.0). */
export type TipoDeNotificacao = "AUTO_ASSIGNED" | "DUE_24H" | "DUE_2H" | "LATE" | "NEW_TASK" | "SEASON_END" | "MEMBER_COMPLETED" | "REMOVED";

/** R-33: ligado por padrão, exceto "um morador concluiu". */
export const PADRAO: Record<TipoDeNotificacao, boolean> = {
  AUTO_ASSIGNED: true,
  DUE_24H: true,
  DUE_2H: true,
  LATE: true,
  NEW_TASK: true,
  SEASON_END: true,
  MEMBER_COMPLETED: false,
  REMOVED: true,
};

/** R-37: a central guarda 30 dias. */
export const VALIDADE_MS = 30 * 24 * 60 * 60 * 1000;
const MAXIMO_DE_TOKENS = 10;

export interface Aviso {
  tipo: TipoDeNotificacao;
  titulo: string;
  corpo: string;
  /** Para onde o toque leva (F1): casa e tarefa. */
  refs: Documento;
}

/** Pushes "enviados" quando roda contra o Emulator (nunca sai push de verdade em teste). */
export const enviadosNoEmulador: { token: string; titulo: string; corpo: string; tipo: TipoDeNotificacao }[] = [];

/** Preferência da pessoa para o tipo (R-34), com o padrão da R-33. */
export function querPush(perfil: Documento | undefined, tipo: TipoDeNotificacao): boolean {
  const prefs = perfil?.["notifPrefs"] as Record<string, unknown> | undefined;
  const v = prefs?.[tipo];
  return typeof v === "boolean" ? v : PADRAO[tipo];
}

/**
 * Entrega um aviso: sempre na central (`users/{uid}/notifications`, R-37) e, se a pessoa quer esse
 * tipo, por push FCM para os aparelhos dela. Roda depois da operação: falha aqui não desfaz nada.
 */
export async function notificar(db: Firestore, env: Env, uids: string[], aviso: Aviso, agora: Date = new Date(), http: typeof fetch = fetch): Promise<void> {
  if (uids.length === 0) return;
  try {
    const perfis = await db.lerVarios(uids.map((u) => `users/${u}`));
    const escritas: Escrita[] = [];
    const pushes: { uid: string; tokens: string[] }[] = [];
    uids.forEach((uid, i) => {
      const perfil = perfis[i];
      if (!perfil) return;
      escritas.push({
        tipo: "gravar",
        caminho: `users/${uid}/notifications/${gerarId()}`,
        dados: {
          type: aviso.tipo,
          title: aviso.titulo,
          body: aviso.corpo,
          refs: aviso.refs,
          read: false,
          createdAt: agora,
          expireAt: new Date(agora.getTime() + VALIDADE_MS),
        },
      });
      const tokens = (perfil["fcmTokens"] as string[] | undefined) ?? [];
      if (tokens.length > 0 && querPush(perfil, aviso.tipo)) pushes.push({ uid, tokens });
    });
    await db.commit(escritas);
    for (const { uid, tokens } of pushes) {
      const invalidos: string[] = [];
      for (const token of tokens) {
        if (!(await enviarPush(db, env, token, aviso, http))) invalidos.push(token);
      }
      if (invalidos.length > 0) {
        const [perfil] = await db.lerVarios([`users/${uid}`]);
        const restantes = ((perfil?.["fcmTokens"] as string[] | undefined) ?? []).filter((t) => !invalidos.includes(t));
        await db.commit([{ tipo: "gravar", caminho: `users/${uid}`, dados: { fcmTokens: restantes }, campos: ["fcmTokens"], exigirExistente: true }]);
      }
    }
  } catch (erro) {
    console.error("notificar", aviso.tipo, erro);
  }
}

/** FCM HTTP v1. Devolve `false` se o token não vale mais (o app foi desinstalado, por exemplo). */
async function enviarPush(db: Firestore, env: Env, token: string, aviso: Aviso, http: typeof fetch): Promise<boolean> {
  if (usaEmulador(env)) {
    enviadosNoEmulador.push({ token, titulo: aviso.titulo, corpo: aviso.corpo, tipo: aviso.tipo });
    return !token.startsWith("invalido");
  }
  const dados: Record<string, string> = { type: aviso.tipo };
  for (const [k, v] of Object.entries(aviso.refs)) if (typeof v === "string") dados[k] = v;
  const resp = await http(`https://fcm.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/messages:send`, {
    method: "POST",
    headers: { authorization: `Bearer ${await db.tokenDeAcesso()}`, "content-type": "application/json" },
    body: JSON.stringify({
      message: {
        token,
        notification: { title: aviso.titulo, body: aviso.corpo },
        data: dados,
        android: { priority: "HIGH", notification: { channel_id: "avisos" } },
      },
    }),
  });
  if (resp.status === 404 || resp.status === 400) return false;
  if (!resp.ok) console.error("fcm", resp.status, await resp.text());
  return true;
}

/** Registra o token FCM do aparelho (R-36): no máximo 10 por pessoa, o mais novo fica. */
export async function registrarToken(db: Firestore, usuario: Usuario, corpo: unknown): Promise<{ ok: true }> {
  const token = typeof corpo === "object" && corpo !== null ? (corpo as Record<string, unknown>)["token"] : undefined;
  if (typeof token !== "string" || token.length < 10 || token.length > 4096) throw new ErroHttp(400, "token-invalido");
  return db.transacao(async (tx) => {
    const [perfil] = await db.lerVarios([`users/${usuario.uid}`], tx);
    if (!perfil) throw new ErroHttp(409, "perfil-inexistente");
    const atuais = ((perfil["fcmTokens"] as string[] | undefined) ?? []).filter((t) => t !== token);
    const tokens = [...atuais, token].slice(-MAXIMO_DE_TOKENS);
    return {
      escritas: [{ tipo: "gravar", caminho: `users/${usuario.uid}`, dados: { fcmTokens: tokens }, campos: ["fcmTokens"], exigirExistente: true }],
      resultado: { ok: true as const },
    };
  });
}

/** Tira o token do aparelho ao sair da conta. */
export async function removerToken(db: Firestore, usuario: Usuario, corpo: unknown): Promise<{ ok: true }> {
  const token = typeof corpo === "object" && corpo !== null ? (corpo as Record<string, unknown>)["token"] : undefined;
  if (typeof token !== "string") throw new ErroHttp(400, "token-invalido");
  return db.transacao(async (tx) => {
    const [perfil] = await db.lerVarios([`users/${usuario.uid}`], tx);
    if (!perfil) return { escritas: [], resultado: { ok: true as const } };
    const tokens = ((perfil["fcmTokens"] as string[] | undefined) ?? []).filter((t) => t !== token);
    return {
      escritas: [{ tipo: "gravar", caminho: `users/${usuario.uid}`, dados: { fcmTokens: tokens }, campos: ["fcmTokens"], exigirExistente: true }],
      resultado: { ok: true as const },
    };
  });
}

/** Nomes das categorias como o app mostra (R-11), para os textos dos avisos. */
export const NOME_DA_CATEGORIA: Record<string, string> = {
  COZINHA: "Cozinha",
  LIMPEZA_GERAL: "Limpeza geral",
  BANHEIRO: "Banheiro",
  LAVANDERIA: "Lavanderia",
  LIXO: "Lixo",
  COMPRAS: "Compras",
  PETS: "Pets",
  PLANTAS: "Plantas",
  MANUTENCAO: "Manutenção",
  OUTROS: "Outros",
};

/** R-33 "Nova tarefa criada na casa": todos os moradores ativos menos o autor. */
export async function avisarNovaTarefa(db: Firestore, env: Env, autor: string, tarefaId: string, agora: Date = new Date()): Promise<void> {
  const [perfil] = await db.lerVarios([`users/${autor}`]);
  const casaId = perfil?.["currentHouseId"];
  if (typeof casaId !== "string") return;
  const [tarefa, membro] = await db.lerVarios([`houses/${casaId}/tasks/${tarefaId}`, `houses/${casaId}/members/${autor}`]);
  if (!tarefa) return;
  const outros = (await db.listar(`houses/${casaId}/members`)).filter((m) => m.dados["status"] === "ACTIVE" && m.uid !== autor).map((m) => m.uid);
  const nome = ((membro?.["displayName"] as string | undefined) ?? "").split(" ")[0] ?? "";
  await notificar(db, env, outros, {
    tipo: "NEW_TASK",
    titulo: `${nome} criou ${tarefa["title"] as string}`,
    corpo: `${NOME_DA_CATEGORIA[tarefa["category"] as string] ?? ""} · ${tarefa["points"] as number} pts`,
    refs: { houseId: casaId, taskId: tarefaId },
  }, agora);
}
