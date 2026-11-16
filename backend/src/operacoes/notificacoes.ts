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
      const quer = querPush(perfil, aviso.tipo);
      // R-35: no horário silencioso o push fica retido e sai no fim, exceto "vence em 2 h".
      const reter = quer && aviso.tipo !== "DUE_2H" && emSilencio(perfil, agora);
      escritas.push({
        tipo: "gravar",
        caminho: `users/${uid}/notifications/${gerarId()}`,
        dados: {
          type: aviso.tipo,
          title: aviso.titulo,
          body: aviso.corpo,
          refs: aviso.refs,
          read: false,
          pendingPush: reter,
          createdAt: agora,
          expireAt: new Date(agora.getTime() + VALIDADE_MS),
        },
      });
      const tokens = (perfil["fcmTokens"] as string[] | undefined) ?? [];
      if (tokens.length > 0 && quer && !reter) pushes.push({ uid, tokens });
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

/** Quem recebe e o quê; as rotinas devolvem avisos e quem chama decide entregar. */
export type Avisar = (uids: string[], aviso: Aviso) => Promise<void>;

/** "22h" ou "22h30" no fuso de Brasília. */
export function horaEmBrasilia(d: Date): string {
  const partes = new Intl.DateTimeFormat("pt-BR", { timeZone: "America/Sao_Paulo", hour: "2-digit", minute: "2-digit", hour12: false }).formatToParts(d);
  const hora = Number(partes.find((p) => p.type === "hour")?.value ?? "0");
  const minuto = partes.find((p) => p.type === "minute")?.value ?? "00";
  return minuto === "00" ? `${hora}h` : `${hora}h${minuto}`;
}

/** R-33 "Um morador concluiu" (desligado por padrão): todos menos quem concluiu. */
export async function avisarConclusao(db: Firestore, env: Env, quem: string, tarefaId: string, pontos: number, agora: Date = new Date()): Promise<void> {
  const [perfil] = await db.lerVarios([`users/${quem}`]);
  const casaId = perfil?.["currentHouseId"];
  if (typeof casaId !== "string") return;
  const [tarefa] = await db.lerVarios([`houses/${casaId}/tasks/${tarefaId}`]);
  const outros = (await db.listar(`houses/${casaId}/members`)).filter((m) => m.dados["status"] === "ACTIVE" && m.uid !== quem).map((m) => m.uid);
  const nome = ((perfil?.["displayName"] as string | undefined) ?? "").split(" ")[0] ?? "";
  await notificar(db, env, outros, {
    tipo: "MEMBER_COMPLETED",
    titulo: `${nome} concluiu ${(tarefa?.["title"] as string | undefined) ?? ""}`,
    corpo: `+${pontos} pts`,
    refs: { houseId: casaId, taskId: tarefaId },
  }, agora);
}

/** R-33 "Você foi removido da casa": só quem saiu. */
export async function avisarRemocao(db: Firestore, env: Env, admin: string, removido: string, agora: Date = new Date()): Promise<void> {
  const [perfil] = await db.lerVarios([`users/${admin}`]);
  const casaId = perfil?.["currentHouseId"];
  const [casa] = typeof casaId === "string" ? await db.lerVarios([`houses/${casaId}`]) : [undefined];
  await notificar(db, env, [removido], {
    tipo: "REMOVED",
    titulo: `Você saiu da ${(casa?.["name"] as string | undefined) ?? "casa"}`,
    corpo: "Um admin te removeu. Suas medalhas e seu histórico seguem com você.",
    refs: {},
  }, agora);
}

/** R-35: padrão 22:00–08:00, ligado. */
export const SILENCIO_PADRAO = { enabled: true, start: "22:00", end: "08:00" };

const minutos = (hhmm: string) => {
  const [h, m] = hhmm.split(":").map(Number);
  return (h ?? 0) * 60 + (m ?? 0);
};

/** Está no horário silencioso da pessoa agora (fuso de Brasília)? Atravessa a meia-noite. */
export function emSilencio(perfil: Documento | undefined, agora: Date): boolean {
  const q = { ...SILENCIO_PADRAO, ...((perfil?.["quietHours"] as Documento | undefined) ?? {}) } as typeof SILENCIO_PADRAO;
  if (!q.enabled) return false;
  const [h, m] = horaEmBrasilia(agora).replace("h", ":").split(":");
  const agoraMin = Number(h) * 60 + Number(m || "0");
  const inicio = minutos(q.start);
  const fim = minutos(q.end);
  return inicio <= fim ? agoraMin >= inicio && agoraMin < fim : agoraMin >= inicio || agoraMin < fim;
}

const TIPOS = Object.keys(PADRAO) as TipoDeNotificacao[];
const HORARIO = /^([01]\d|2[0-3]):[0-5]\d$/;

/** E4 (R-34, R-35): liga/desliga cada tipo e o horário silencioso. */
export async function salvarPreferencias(db: Firestore, usuario: Usuario, corpo: unknown): Promise<{ ok: true }> {
  const c = typeof corpo === "object" && corpo !== null ? (corpo as Record<string, unknown>) : {};
  const tipos = (c["tipos"] ?? {}) as Record<string, unknown>;
  const prefs: Documento = {};
  for (const t of TIPOS) {
    const v = tipos[t];
    if (v !== undefined && typeof v !== "boolean") throw new ErroHttp(400, "preferencia-invalida");
    if (typeof v === "boolean") prefs[t] = v;
  }
  const s = (c["silencio"] ?? {}) as Record<string, unknown>;
  const silencio = {
    enabled: typeof s["ligado"] === "boolean" ? s["ligado"] : SILENCIO_PADRAO.enabled,
    start: typeof s["inicio"] === "string" ? s["inicio"] : SILENCIO_PADRAO.start,
    end: typeof s["fim"] === "string" ? s["fim"] : SILENCIO_PADRAO.end,
  };
  if (!HORARIO.test(silencio.start) || !HORARIO.test(silencio.end)) throw new ErroHttp(400, "horario-invalido");
  return db.transacao(async (tx) => {
    const [perfil] = await db.lerVarios([`users/${usuario.uid}`], tx);
    if (!perfil) throw new ErroHttp(409, "perfil-inexistente");
    const atuais = (perfil["notifPrefs"] as Documento | undefined) ?? {};
    return {
      escritas: [
        { tipo: "gravar", caminho: `users/${usuario.uid}`, dados: { notifPrefs: { ...atuais, ...prefs }, quietHours: silencio }, campos: ["notifPrefs", "quietHours"], exigirExistente: true },
      ],
      resultado: { ok: true as const },
    };
  });
}

/** Rotina: entrega os pushes retidos de quem já saiu do horário silencioso (R-35). */
export async function entregarRetidos(db: Firestore, env: Env, agora: Date = new Date(), http: typeof fetch = fetch): Promise<{ entregues: number }> {
  const retidos = await db.consultarGrupo("notifications", [{ campo: "pendingPush", op: "EQUAL", valor: true }], LOTE_RETIDOS);
  let entregues = 0;
  for (const { caminho, dados } of retidos) {
    const uid = caminho.split("/")[1] ?? "";
    const [perfil] = await db.lerVarios([`users/${uid}`]);
    if (emSilencio(perfil, agora)) continue;
    await db.commit([{ tipo: "gravar", caminho, dados: { pendingPush: false }, campos: ["pendingPush"], exigirExistente: true }]);
    const tokens = (perfil?.["fcmTokens"] as string[] | undefined) ?? [];
    const aviso: Aviso = { tipo: dados["type"] as TipoDeNotificacao, titulo: dados["title"] as string, corpo: dados["body"] as string, refs: (dados["refs"] as Documento | undefined) ?? {} };
    for (const token of tokens) await enviarPush(db, env, token, aviso, http);
    entregues++;
  }
  return { entregues };
}

const LOTE_RETIDOS = 200;
