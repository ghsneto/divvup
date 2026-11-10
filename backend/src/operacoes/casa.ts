import type { Usuario } from "../auth.js";
import { ErroHttp } from "../erros.js";
import type { Documento, Firestore } from "../firestore.js";

/** Igual ao `maxlength` do nome da casa em B3. */
export const NOME_CASA_MAXIMO = 32;

/** Tipos de casa de B3 (R-02 e SPEC §4.3 `houses.type`). */
export const TIPOS_DE_CASA = ["REPUBLICA", "MORADIA_COMPARTILHADA", "CASAL_FAMILIA"] as const;
export type TipoDeCasa = (typeof TIPOS_DE_CASA)[number];

/** Fuso das temporadas (o mês da casa vira no horário de Brasília). */
const FUSO = "America/Sao_Paulo";

interface PedidoCriarCasa {
  nome: string;
  tipo: TipoDeCasa;
  anunciar: boolean;
}

/**
 * Cria a casa (B3). Numa transação: `houses/{id}`, o membro `ADMIN` de quem criou (R-03) e
 * `users/{uid}.currentHouseId`. Quem já está numa casa não cria outra (R-02).
 * `anunciar` só guarda a intenção do bloco "Anunciar vagas" (v1.1, R-38); a busca ainda não existe.
 */
export async function criarCasa(
  db: Firestore,
  usuario: Usuario,
  corpo: unknown,
  agora: Date = new Date(),
  novoId: () => string = gerarId,
): Promise<{ casaId: string }> {
  const pedido = lerPedido(corpo);
  const casaId = novoId();
  return db.transacao(async (tx) => {
    const [perfil] = await db.lerVarios([`users/${usuario.uid}`], tx);
    if (!perfil) throw new ErroHttp(409, "perfil-inexistente");
    if (perfil["currentHouseId"]) throw new ErroHttp(409, "ja-em-casa");

    const casa: Documento = {
      name: pedido.nome,
      type: pedido.tipo,
      createdBy: usuario.uid,
      createdAt: agora,
      memberCount: 1,
      city: null,
      neighborhood: null,
      street: null,
      currentSeason: temporada(agora),
      listing: { enabled: pedido.tipo === "REPUBLICA" && pedido.anunciar },
    };
    const membro: Documento = {
      role: "ADMIN",
      status: "ACTIVE",
      joinedAt: agora,
      leftAt: null,
      displayName: (perfil["displayName"] as string | null) ?? "",
      avatarId: (perfil["avatarId"] as string | null) ?? null,
      seasonPoints: 0,
      totalPoints: 0,
      openTasks: 0,
    };
    return {
      escritas: [
        { tipo: "gravar", caminho: `houses/${casaId}`, dados: casa, exigirInexistente: true },
        { tipo: "gravar", caminho: `houses/${casaId}/members/${usuario.uid}`, dados: membro, exigirInexistente: true },
        { tipo: "gravar", caminho: `users/${usuario.uid}`, dados: { currentHouseId: casaId }, campos: ["currentHouseId"], exigirExistente: true },
      ],
      resultado: { casaId },
    };
  });
}

/** Temporada no formato `yyyy-MM`, no fuso de Brasília. */
export function temporada(data: Date): string {
  const partes = new Intl.DateTimeFormat("en-CA", { timeZone: FUSO, year: "numeric", month: "2-digit" }).formatToParts(data);
  const ano = partes.find((p) => p.type === "year")?.value;
  const mes = partes.find((p) => p.type === "month")?.value;
  return `${ano}-${mes}`;
}

function lerPedido(corpo: unknown): PedidoCriarCasa {
  const c = typeof corpo === "object" && corpo !== null ? (corpo as Record<string, unknown>) : {};
  const nome = typeof c["nome"] === "string" ? c["nome"].trim().replace(/\s+/g, " ") : "";
  if (nome.length === 0 || nome.length > NOME_CASA_MAXIMO) throw new ErroHttp(400, "nome-invalido");
  const tipo = c["tipo"];
  if (typeof tipo !== "string" || !(TIPOS_DE_CASA as readonly string[]).includes(tipo)) throw new ErroHttp(400, "tipo-invalido");
  const anunciar = c["anunciar"] ?? false;
  if (typeof anunciar !== "boolean") throw new ErroHttp(400, "anunciar-invalido");
  return { nome, tipo: tipo as TipoDeCasa, anunciar };
}

/** Id de documento como o do Firestore: 20 caracteres alfanuméricos. */
function gerarId(): string {
  const alfabeto = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  const bytes = crypto.getRandomValues(new Uint8Array(20));
  return Array.from(bytes, (b) => alfabeto[b % alfabeto.length]).join("");
}
