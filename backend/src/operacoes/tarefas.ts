import type { Usuario } from "../auth.js";
import { ErroHttp } from "../erros.js";
import type { Documento, Firestore } from "../firestore.js";
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
