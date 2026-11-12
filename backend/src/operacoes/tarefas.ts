import type { Usuario } from "../auth.js";
import { ErroHttp } from "../erros.js";
import type { Documento, Escrita, Firestore } from "../firestore.js";
import { gerarId } from "./casa.js";

/** R-11: categorias fixas. */
export const CATEGORIAS = [
  "COZINHA",
  "LIMPEZA_GERAL",
  "BANHEIRO",
  "LAVANDERIA",
  "LIXO",
  "COMPRAS",
  "PETS",
  "PLANTAS",
  "MANUTENCAO",
  "OUTROS",
] as const;

/** R-10: peso → pontos. */
export const PONTOS_POR_PESO = { LEVE: 25, MEDIA: 50, PESADA: 100 } as const;

export const TITULO_MAXIMO = 60;
export const DESCRICAO_MAXIMA = 280;
/** Prazo máximo aceito (evita datas absurdas digitadas por engano). */
const PRAZO_MAXIMO_MS = 366 * 24 * 60 * 60 * 1000;

interface PedidoTarefa {
  titulo: string;
  descricao: string | null;
  categoria: (typeof CATEGORIAS)[number];
  pontos: number;
  prazo: Date;
}

/**
 * C2: qualquer morador ativo cria uma tarefa (R-09). Grava o modelo `taskTemplates/{id}` com
 * `recurrence = null` e exatamente uma ocorrência `tasks/{id}` em `OPEN` (R-13). Os pontos saem
 * do peso no servidor (R-10); o cliente nunca manda pontos.
 */
export async function criarTarefa(
  db: Firestore,
  usuario: Usuario,
  corpo: unknown,
  agora: Date = new Date(),
  novoId: () => string = gerarId,
): Promise<{ tarefaId: string }> {
  const p = lerPedido(corpo, agora);
  const modeloId = novoId();
  const tarefaId = novoId();
  return db.transacao(async (tx) => {
    const [perfil] = await db.lerVarios([`users/${usuario.uid}`], tx);
    const casaId = perfil?.["currentHouseId"];
    if (typeof casaId !== "string") throw new ErroHttp(409, "sem-casa");
    const [membro] = await db.lerVarios([`houses/${casaId}/members/${usuario.uid}`], tx);
    if (membro?.["status"] !== "ACTIVE") throw new ErroHttp(409, "sem-casa");

    const modelo: Documento = {
      title: p.titulo,
      description: p.descricao,
      category: p.categoria,
      points: p.pontos,
      recurrence: null,
      createdBy: usuario.uid,
      createdAt: agora,
    };
    const tarefa: Documento = {
      templateId: modeloId,
      title: p.titulo,
      description: p.descricao,
      category: p.categoria,
      points: p.pontos,
      dueAt: p.prazo,
      status: "OPEN",
      assigneeId: null,
      assignedVia: null,
      assignedAt: null,
      completedAt: null,
      pointsAwarded: null,
      createdBy: usuario.uid,
      createdByName: (membro["displayName"] as string | null) ?? "",
      createdByAvatar: (membro["avatarId"] as string | null) ?? null,
      createdAt: agora,
    };
    return {
      escritas: [
        { tipo: "gravar", caminho: `houses/${casaId}/taskTemplates/${modeloId}`, dados: modelo, exigirInexistente: true },
        { tipo: "gravar", caminho: `houses/${casaId}/tasks/${tarefaId}`, dados: tarefa, exigirInexistente: true },
      ],
      resultado: { tarefaId },
    };
  });
}

function lerPedido(corpo: unknown, agora: Date): PedidoTarefa {
  const c = typeof corpo === "object" && corpo !== null ? (corpo as Record<string, unknown>) : {};
  const titulo = typeof c["titulo"] === "string" ? c["titulo"].trim().replace(/\s+/g, " ") : "";
  if (titulo.length === 0 || titulo.length > TITULO_MAXIMO) throw new ErroHttp(400, "titulo-invalido");
  const d = c["descricao"];
  if (d !== undefined && d !== null && typeof d !== "string") throw new ErroHttp(400, "descricao-invalida");
  const descricao = typeof d === "string" ? d.trim() : "";
  if (descricao.length > DESCRICAO_MAXIMA) throw new ErroHttp(400, "descricao-invalida");
  const categoria = c["categoria"];
  if (typeof categoria !== "string" || !(CATEGORIAS as readonly string[]).includes(categoria)) throw new ErroHttp(400, "categoria-invalida");
  const peso = c["peso"];
  if (typeof peso !== "string" || !(peso in PONTOS_POR_PESO)) throw new ErroHttp(400, "peso-invalido");
  const prazo = typeof c["prazo"] === "string" ? new Date(c["prazo"]) : new Date(NaN);
  if (Number.isNaN(prazo.getTime())) throw new ErroHttp(400, "prazo-invalido");
  // C2: não aceita passado.
  if (prazo.getTime() <= agora.getTime()) throw new ErroHttp(400, "prazo-no-passado");
  if (prazo.getTime() - agora.getTime() > PRAZO_MAXIMO_MS) throw new ErroHttp(400, "prazo-invalido");
  return {
    titulo,
    descricao: descricao || null,
    categoria: categoria as PedidoTarefa["categoria"],
    pontos: PONTOS_POR_PESO[peso as keyof typeof PONTOS_POR_PESO],
    prazo,
  };
}

/** R-19: só devolve com mais de 24 h para o prazo. */
export const JANELA_PARA_DEVOLVER_MS = 24 * 60 * 60 * 1000;
const FUSO = "America/Sao_Paulo";

/** R-22: integral no prazo, metade (arredondada para baixo) depois; nunca negativo. */
export function pontosDaConclusao(pontos: number, prazo: Date, concluidaEm: Date): number {
  return Math.max(concluidaEm.getTime() <= prazo.getTime() ? pontos : Math.floor(pontos / 2), 0);
}

/** Dia (yyyy-mm-dd) no fuso de Brasília. */
export function diaEmBrasilia(d: Date): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone: FUSO, year: "numeric", month: "2-digit", day: "2-digit" }).format(d);
}

/** R-31: dias seguidos com ao menos uma conclusão. */
export function novaSequencia(atual: { current: number; best: number; lastDay: string | null }, hoje: string) {
  if (atual.lastDay === hoje) return atual;
  const ontem = diaEmBrasilia(new Date(new Date(`${hoje}T12:00:00-03:00`).getTime() - 24 * 60 * 60 * 1000));
  const current = atual.lastDay === ontem ? atual.current + 1 : 1;
  return { current, best: Math.max(atual.best, current), lastDay: hoje };
}

async function contextoDaTarefa(db: Firestore, uid: string, tarefaId: string, tx: string) {
  const [perfil] = await db.lerVarios([`users/${uid}`], tx);
  const casaId = perfil?.["currentHouseId"];
  if (!perfil || typeof casaId !== "string") throw new ErroHttp(409, "sem-casa");
  const caminho = `houses/${casaId}/tasks/${tarefaId}`;
  const [membro, tarefa] = await db.lerVarios([`houses/${casaId}/members/${uid}`, caminho], tx);
  if (membro?.["status"] !== "ACTIVE") throw new ErroHttp(409, "sem-casa");
  if (!tarefa) throw new ErroHttp(404, "tarefa-inexistente");
  return { perfil, casaId, membro, tarefa, caminho };
}

function lerId(corpo: unknown): string {
  const id = typeof corpo === "object" && corpo !== null ? (corpo as Record<string, unknown>)["tarefaId"] : undefined;
  if (typeof id !== "string" || !/^[A-Za-z0-9]{1,40}$/.test(id)) throw new ErroHttp(400, "tarefa-invalida");
  return id;
}

/**
 * C3/C5 "Concluir" (R-21, R-22, R-24): só o dono; credita os pontos em `seasonPoints` e
 * `totalPoints`, atualiza a sequência (R-31), grava o evento em `activity` e devolve a posição no
 * mês antes e depois (C4).
 */
export async function concluirTarefa(db: Firestore, usuario: Usuario, corpo: unknown, agora: Date = new Date(), novoId: () => string = gerarId) {
  const tarefaId = lerId(corpo);
  const eventoId = novoId();
  return db.transacao(async (tx) => {
    const { perfil, casaId, membro, tarefa, caminho } = await contextoDaTarefa(db, usuario.uid, tarefaId, tx);
    if (tarefa["assigneeId"] !== usuario.uid || (tarefa["status"] !== "ASSIGNED" && tarefa["status"] !== "LATE")) {
      throw new ErroHttp(409, "nao-e-sua");
    }
    const prazo = tarefa["dueAt"] as Date;
    const pontos = pontosDaConclusao((tarefa["points"] as number) ?? 0, prazo, agora);
    const moradores = (await db.listar(`houses/${casaId}/members`, tx)).filter((m) => m.dados["status"] === "ACTIVE");
    const doMes = (m: { dados: Documento }) => (m.dados["seasonPoints"] as number | undefined) ?? 0;
    const meus = (membro["seasonPoints"] as number | undefined) ?? 0;
    const posicao = (pts: number) => 1 + moradores.filter((m) => m.uid !== usuario.uid && doMes(m) > pts).length;
    const ultrapassados = moradores.filter((m) => m.uid !== usuario.uid && doMes(m) >= meus && doMes(m) < meus + pontos);
    const ultrapassado = ultrapassados.sort((a, b) => doMes(b) - doMes(a))[0];
    const sequenciaAtual = (perfil["streak"] as { current: number; best: number; lastDay: string | null } | undefined) ?? { current: 0, best: 0, lastDay: null };
    const sequencia = novaSequencia(sequenciaAtual, diaEmBrasilia(agora));
    const escritas: Escrita[] = [
      {
        tipo: "gravar",
        caminho,
        dados: { status: "DONE", completedAt: agora, pointsAwarded: pontos },
        campos: ["status", "completedAt", "pointsAwarded"],
        exigirExistente: true,
      },
      {
        tipo: "gravar",
        caminho: `houses/${casaId}/members/${usuario.uid}`,
        dados: {
          seasonPoints: meus + pontos,
          totalPoints: ((membro["totalPoints"] as number | undefined) ?? 0) + pontos,
          openTasks: Math.max(((membro["openTasks"] as number | undefined) ?? 1) - 1, 0),
        },
        campos: ["seasonPoints", "totalPoints", "openTasks"],
        exigirExistente: true,
      },
      {
        tipo: "gravar",
        caminho: `users/${usuario.uid}`,
        dados: {
          totalPoints: ((perfil["totalPoints"] as number | undefined) ?? 0) + pontos,
          tasksDone: ((perfil["tasksDone"] as number | undefined) ?? 0) + 1,
          streak: sequencia,
        },
        campos: ["totalPoints", "tasksDone", "streak"],
        exigirExistente: true,
      },
      {
        tipo: "gravar",
        caminho: `houses/${casaId}/activity/${eventoId}`,
        dados: { type: "COMPLETED", actorId: usuario.uid, taskId: tarefaId, taskTitle: tarefa["title"] ?? "", points: pontos, createdAt: agora },
        exigirInexistente: true,
      },
    ];
    return {
      escritas,
      resultado: {
        pontos,
        noPrazo: agora.getTime() <= prazo.getTime(),
        posicaoAntes: posicao(meus),
        posicaoDepois: posicao(meus + pontos),
        ultrapassou: (ultrapassado?.dados["displayName"] as string | undefined) ?? null,
        sequencia: sequencia.current,
      },
    };
  });
}

/** C3 "Devolver" (R-19): o dono devolve ao deck enquanto faltar mais de 24 h. */
export async function devolverTarefa(db: Firestore, usuario: Usuario, corpo: unknown, agora: Date = new Date()) {
  const tarefaId = lerId(corpo);
  return db.transacao(async (tx) => {
    const { casaId, membro, tarefa, caminho } = await contextoDaTarefa(db, usuario.uid, tarefaId, tx);
    if (tarefa["assigneeId"] !== usuario.uid || tarefa["status"] !== "ASSIGNED") throw new ErroHttp(409, "nao-e-sua");
    if ((tarefa["dueAt"] as Date).getTime() - agora.getTime() <= JANELA_PARA_DEVOLVER_MS) throw new ErroHttp(409, "prazo-curto");
    return {
      escritas: [
        {
          tipo: "gravar",
          caminho,
          dados: { status: "OPEN", assigneeId: null, assignedVia: null, assignedAt: null },
          campos: ["status", "assigneeId", "assignedVia", "assignedAt"],
          exigirExistente: true,
        },
        {
          tipo: "gravar",
          caminho: `houses/${casaId}/members/${usuario.uid}`,
          dados: { openTasks: Math.max(((membro["openTasks"] as number | undefined) ?? 1) - 1, 0) },
          campos: ["openTasks"],
          exigirExistente: true,
        },
      ] as Escrita[],
      resultado: { ok: true as const },
    };
  });
}

/** C3 "Cancelar tarefa" (R-12): só admin; com dono, o dono perde a tarefa e ninguém pontua. */
export async function cancelarTarefa(db: Firestore, usuario: Usuario, corpo: unknown, agora: Date = new Date()) {
  const tarefaId = lerId(corpo);
  return db.transacao(async (tx) => {
    const { casaId, membro, tarefa, caminho } = await contextoDaTarefa(db, usuario.uid, tarefaId, tx);
    if (membro["role"] !== "ADMIN") throw new ErroHttp(403, "nao-admin");
    if (!["OPEN", "ASSIGNED", "LATE"].includes(tarefa["status"] as string)) throw new ErroHttp(409, "ja-encerrada");
    const escritas: Escrita[] = [
      { tipo: "gravar", caminho, dados: { status: "CANCELLED", cancelledBy: usuario.uid, cancelledAt: agora }, campos: ["status", "cancelledBy", "cancelledAt"], exigirExistente: true },
    ];
    const dono = tarefa["assigneeId"];
    if (typeof dono === "string") {
      const [doDono] = await db.lerVarios([`houses/${casaId}/members/${dono}`], tx);
      if (doDono) {
        escritas.push({
          tipo: "gravar",
          caminho: `houses/${casaId}/members/${dono}`,
          dados: { openTasks: Math.max(((doDono["openTasks"] as number | undefined) ?? 1) - 1, 0) },
          campos: ["openTasks"],
          exigirExistente: true,
        });
      }
    }
    return { escritas, resultado: { ok: true as const } };
  });
}

/** Autor ou admin, e só sem dono (R-12). */
function podeMexer(usuario: Usuario, membro: Documento, tarefa: Documento) {
  if (tarefa["createdBy"] !== usuario.uid && membro["role"] !== "ADMIN") throw new ErroHttp(403, "sem-permissao");
  if (tarefa["status"] !== "OPEN") throw new ErroHttp(409, "tem-dono");
}

/** C2 em modo edição (R-12). */
export async function editarTarefa(db: Firestore, usuario: Usuario, corpo: unknown, agora: Date = new Date()) {
  const tarefaId = lerId(corpo);
  const p = lerPedido(corpo, agora);
  return db.transacao(async (tx) => {
    const { casaId, membro, tarefa, caminho } = await contextoDaTarefa(db, usuario.uid, tarefaId, tx);
    podeMexer(usuario, membro, tarefa);
    const campos = { title: p.titulo, description: p.descricao, category: p.categoria, points: p.pontos };
    return {
      escritas: [
        { tipo: "gravar", caminho, dados: { ...campos, dueAt: p.prazo }, campos: [...Object.keys(campos), "dueAt"], exigirExistente: true },
        { tipo: "gravar", caminho: `houses/${casaId}/taskTemplates/${tarefa["templateId"] as string}`, dados: campos, campos: Object.keys(campos) },
      ] as Escrita[],
      resultado: { ok: true as const },
    };
  });
}

/** C2 "Excluir tarefa" (R-12): some do deck de todo mundo. */
export async function excluirTarefa(db: Firestore, usuario: Usuario, corpo: unknown) {
  const tarefaId = lerId(corpo);
  return db.transacao(async (tx) => {
    const { casaId, membro, tarefa, caminho } = await contextoDaTarefa(db, usuario.uid, tarefaId, tx);
    podeMexer(usuario, membro, tarefa);
    return {
      escritas: [
        { tipo: "apagar", caminho },
        { tipo: "apagar", caminho: `houses/${casaId}/taskTemplates/${tarefa["templateId"] as string}` },
      ] as Escrita[],
      resultado: { ok: true as const },
    };
  });
}
