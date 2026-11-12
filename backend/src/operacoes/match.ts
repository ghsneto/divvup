import type { Usuario } from "../auth.js";
import { ErroHttp } from "../erros.js";
import type { Documento, Escrita, Firestore } from "../firestore.js";

/** R-20: no máximo 5 assumidas e não concluídas. */
export const LIMITE_DE_ABERTAS = 5;
/** R-16: o passe vale 12 h. */
export const VALIDADE_DO_PASSE_MS = 12 * 60 * 60 * 1000;
/** R-18: o app oferece "Desfazer" por 3 s; o servidor aceita até 15 s por causa da rede. */
export const JANELA_DE_DESFAZER_MS = 15_000;

/** Morador ativo e a casa atual dele, lidos na transação. */
async function moradorAtivo(db: Firestore, uid: string, tx: string) {
  const [perfil] = await db.lerVarios([`users/${uid}`], tx);
  const casaId = perfil?.["currentHouseId"];
  if (typeof casaId !== "string") throw new ErroHttp(409, "sem-casa");
  const [membro] = await db.lerVarios([`houses/${casaId}/members/${uid}`], tx);
  if (membro?.["status"] !== "ACTIVE") throw new ErroHttp(409, "sem-casa");
  return { casaId, membro };
}

function lerTarefaId(corpo: unknown): string {
  const id = typeof corpo === "object" && corpo !== null ? (corpo as Record<string, unknown>)["tarefaId"] : undefined;
  if (typeof id !== "string" || !/^[A-Za-z0-9]{1,40}$/.test(id)) throw new ErroHttp(400, "tarefa-invalida");
  return id;
}

/**
 * Swipe à direita (R-15): assume a ocorrência em transação. Se outra pessoa levou antes,
 * `ja-assumida` com o nome dela ("alguém já pegou essa"); no limite de 5, `limite` (R-20).
 */
export async function assumirTarefa(db: Firestore, usuario: Usuario, corpo: unknown, agora: Date = new Date()) {
  const tarefaId = lerTarefaId(corpo);
  return db.transacao(async (tx) => {
    const { casaId, membro } = await moradorAtivo(db, usuario.uid, tx);
    const caminho = `houses/${casaId}/tasks/${tarefaId}`;
    const [tarefa] = await db.lerVarios([caminho], tx);
    if (!tarefa) throw new ErroHttp(404, "tarefa-inexistente");
    if (tarefa["status"] !== "OPEN") {
      const [dono] = typeof tarefa["assigneeId"] === "string" ? await db.lerVarios([`houses/${casaId}/members/${tarefa["assigneeId"]}`], tx) : [undefined];
      throw new ErroHttp(409, "ja-assumida", undefined, (dono?.["displayName"] as string | undefined) ?? "");
    }
    const abertas = (membro["openTasks"] as number | undefined) ?? 0;
    if (abertas >= LIMITE_DE_ABERTAS) throw new ErroHttp(409, "limite");
    const escritas: Escrita[] = [
      {
        tipo: "gravar",
        caminho,
        dados: { status: "ASSIGNED", assigneeId: usuario.uid, assignedVia: "SWIPE", assignedAt: agora },
        campos: ["status", "assigneeId", "assignedVia", "assignedAt"],
        exigirExistente: true,
      },
      { tipo: "gravar", caminho: `houses/${casaId}/members/${usuario.uid}`, dados: { openTasks: abertas + 1 }, campos: ["openTasks"], exigirExistente: true },
      // Quem assume uma tarefa que tinha passado não precisa mais do passe.
      { tipo: "apagar", caminho: `houses/${casaId}/members/${usuario.uid}/passes/${tarefaId}` },
    ];
    return { escritas, resultado: { ok: true as const } };
  });
}

/** Swipe à esquerda (R-16): passe pessoal por 12 h, sem penalidade. */
export async function passarTarefa(db: Firestore, usuario: Usuario, corpo: unknown, agora: Date = new Date()) {
  const tarefaId = lerTarefaId(corpo);
  return db.transacao(async (tx) => {
    const { casaId } = await moradorAtivo(db, usuario.uid, tx);
    const [tarefa] = await db.lerVarios([`houses/${casaId}/tasks/${tarefaId}`], tx);
    if (!tarefa) throw new ErroHttp(404, "tarefa-inexistente");
    const passe: Documento = { createdAt: agora, expiresAt: new Date(agora.getTime() + VALIDADE_DO_PASSE_MS) };
    return {
      escritas: [{ tipo: "gravar", caminho: `houses/${casaId}/members/${usuario.uid}/passes/${tarefaId}`, dados: passe }],
      resultado: { expiraEm: (passe["expiresAt"] as Date).toISOString() },
    };
  });
}

/** "Desfazer" (R-18): devolve a tarefa assumida há instantes ou apaga o passe. */
export async function desfazer(db: Firestore, usuario: Usuario, corpo: unknown, agora: Date = new Date()) {
  const tarefaId = lerTarefaId(corpo);
  const acao = typeof corpo === "object" && corpo !== null ? (corpo as Record<string, unknown>)["acao"] : undefined;
  if (acao !== "ASSUMIR" && acao !== "PASSAR") throw new ErroHttp(400, "acao-invalida");
  return db.transacao(async (tx) => {
    const { casaId, membro } = await moradorAtivo(db, usuario.uid, tx);
    if (acao === "PASSAR") {
      return { escritas: [{ tipo: "apagar", caminho: `houses/${casaId}/members/${usuario.uid}/passes/${tarefaId}` }] as Escrita[], resultado: { ok: true as const } };
    }
    const caminho = `houses/${casaId}/tasks/${tarefaId}`;
    const [tarefa] = await db.lerVarios([caminho], tx);
    const assumidaEm = tarefa?.["assignedAt"];
    if (!tarefa || tarefa["status"] !== "ASSIGNED" || tarefa["assigneeId"] !== usuario.uid || !(assumidaEm instanceof Date)) {
      throw new ErroHttp(409, "nao-da-para-desfazer");
    }
    if (agora.getTime() - assumidaEm.getTime() > JANELA_DE_DESFAZER_MS) throw new ErroHttp(409, "nao-da-para-desfazer");
    const abertas = (membro["openTasks"] as number | undefined) ?? 1;
    const escritas: Escrita[] = [
      {
        tipo: "gravar",
        caminho,
        dados: { status: "OPEN", assigneeId: null, assignedVia: null, assignedAt: null },
        campos: ["status", "assigneeId", "assignedVia", "assignedAt"],
        exigirExistente: true,
      },
      { tipo: "gravar", caminho: `houses/${casaId}/members/${usuario.uid}`, dados: { openTasks: Math.max(abertas - 1, 0) }, campos: ["openTasks"], exigirExistente: true },
    ];
    return { escritas, resultado: { ok: true as const } };
  });
}

/** "Rever" (R-17): apaga os passes da pessoa para as tarefas voltarem ao deck dela. */
export async function reverPassadas(db: Firestore, usuario: Usuario) {
  return db.transacao(async (tx) => {
    const { casaId } = await moradorAtivo(db, usuario.uid, tx);
    const passes = await db.listar(`houses/${casaId}/members/${usuario.uid}/passes`, tx);
    return {
      escritas: passes.map((p) => ({ tipo: "apagar", caminho: `houses/${casaId}/members/${usuario.uid}/passes/${p.uid}` })) as Escrita[],
      resultado: { revistas: passes.length },
    };
  });
}
