import type { Usuario } from "../auth.js";
import { ErroHttp } from "../erros.js";
import type { Documento, Escrita, Firestore } from "../firestore.js";

/** R-06: 6 caracteres sem ambíguos (sem O/0/I/1/L). */
export const ALFABETO_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
export const TAMANHO_CODIGO = 6;
/** R-06: válido por 24 h. */
export const VALIDADE_MS = 24 * 60 * 60 * 1000;

/** Papel e situação de quem pede, na casa atual dele. */
async function membroAtual(db: Firestore, uid: string, tx: string) {
  const [perfil] = await db.lerVarios([`users/${uid}`], tx);
  if (!perfil) throw new ErroHttp(409, "perfil-inexistente");
  const casaId = perfil["currentHouseId"];
  if (typeof casaId !== "string") throw new ErroHttp(409, "sem-casa");
  const [membro] = await db.lerVarios([`houses/${casaId}/members/${uid}`], tx);
  if (membro?.["status"] !== "ACTIVE") throw new ErroHttp(409, "sem-casa");
  return { perfil, casaId, membro };
}

/**
 * G2/B8: o admin gera um código (R-06). Grava `houses/{casa}/invites/{codigo}` e o ponteiro
 * `inviteCodes/{codigo}` (só o backend lê), que garante código único entre todas as casas.
 */
export async function criarConvite(
  db: Firestore,
  usuario: Usuario,
  agora: Date = new Date(),
  novoCodigo: () => string = gerarCodigo,
): Promise<{ codigo: string; expiraEm: string }> {
  return db.transacao(async (tx) => {
    const { perfil, casaId, membro } = await membroAtual(db, usuario.uid, tx);
    if (membro["role"] !== "ADMIN") throw new ErroHttp(403, "nao-admin");
    const codigo = novoCodigo();
    const expiraEm = new Date(agora.getTime() + VALIDADE_MS);
    const convite: Documento = {
      createdBy: usuario.uid,
      createdByName: (perfil["displayName"] as string | null) ?? "",
      createdAt: agora,
      expiresAt: expiraEm,
      usedBy: null,
      usedByName: null,
      usedAt: null,
      revoked: false,
      revokedAt: null,
    };
    return {
      escritas: [
        { tipo: "gravar", caminho: `houses/${casaId}/invites/${codigo}`, dados: convite, exigirInexistente: true },
        { tipo: "gravar", caminho: `inviteCodes/${codigo}`, dados: { houseId: casaId, createdAt: agora }, exigirInexistente: true },
      ],
      resultado: { codigo, expiraEm: expiraEm.toISOString() },
    };
  });
}

/** G2: o admin revoga um código ainda não usado (R-06). Idempotente. */
export async function revogarConvite(
  db: Firestore,
  usuario: Usuario,
  corpo: unknown,
  agora: Date = new Date(),
): Promise<{ revogado: boolean }> {
  const codigo = lerCodigo(corpo);
  return db.transacao(async (tx) => {
    const { casaId, membro } = await membroAtual(db, usuario.uid, tx);
    if (membro["role"] !== "ADMIN") throw new ErroHttp(403, "nao-admin");
    const caminho = `houses/${casaId}/invites/${codigo}`;
    const [convite] = await db.lerVarios([caminho], tx);
    if (!convite) throw new ErroHttp(404, "codigo-invalido");
    if (convite["usedBy"]) throw new ErroHttp(409, "codigo-usado");
    if (convite["revoked"] === true) return { escritas: [], resultado: { revogado: true } };
    return {
      escritas: [{ tipo: "gravar", caminho, dados: { revoked: true, revokedAt: agora }, campos: ["revoked", "revokedAt"], exigirExistente: true }],
      resultado: { revogado: true },
    };
  });
}

/**
 * B4: entra na casa com um código válido, sem aprovação (R-06). Erros: `codigo-invalido` (não
 * existe ou foi revogado), `codigo-expirado`, `codigo-usado`, `ja-em-casa` (R-02).
 */
export async function entrarComCodigo(
  db: Firestore,
  usuario: Usuario,
  corpo: unknown,
  agora: Date = new Date(),
): Promise<{ casaId: string }> {
  const codigo = lerCodigo(corpo);
  return db.transacao(async (tx) => {
    const [ponteiro, perfil] = await db.lerVarios([`inviteCodes/${codigo}`, `users/${usuario.uid}`], tx);
    if (!perfil) throw new ErroHttp(409, "perfil-inexistente");
    const casaId = ponteiro?.["houseId"];
    if (typeof casaId !== "string") throw new ErroHttp(404, "codigo-invalido");

    const caminhoConvite = `houses/${casaId}/invites/${codigo}`;
    const caminhoMembro = `houses/${casaId}/members/${usuario.uid}`;
    const [convite, casa, membroAntigo] = await db.lerVarios([caminhoConvite, `houses/${casaId}`, caminhoMembro], tx);
    if (!convite || !casa || convite["revoked"] === true) throw new ErroHttp(404, "codigo-invalido");
    if (convite["usedBy"]) throw new ErroHttp(409, "codigo-usado");
    if ((convite["expiresAt"] as Date).getTime() <= agora.getTime()) throw new ErroHttp(410, "codigo-expirado");
    if (perfil["currentHouseId"]) throw new ErroHttp(409, "ja-em-casa");

    const nome = (perfil["displayName"] as string | null) ?? "";
    // Quem volta para a mesma casa mantém os pontos que já tinha nela.
    const membro: Documento = {
      role: "MEMBER",
      status: "ACTIVE",
      joinedAt: agora,
      leftAt: null,
      displayName: nome,
      avatarId: (perfil["avatarId"] as string | null) ?? null,
      seasonPoints: (membroAntigo?.["seasonPoints"] as number | undefined) ?? 0,
      totalPoints: (membroAntigo?.["totalPoints"] as number | undefined) ?? 0,
      openTasks: 0,
    };
    const escritas: Escrita[] = [
      { tipo: "gravar", caminho: caminhoConvite, dados: { usedBy: usuario.uid, usedByName: nome, usedAt: agora }, campos: ["usedBy", "usedByName", "usedAt"], exigirExistente: true },
      { tipo: "gravar", caminho: caminhoMembro, dados: membro },
      { tipo: "gravar", caminho: `houses/${casaId}`, dados: { memberCount: ((casa["memberCount"] as number) ?? 0) + 1 }, campos: ["memberCount"], exigirExistente: true },
      { tipo: "gravar", caminho: `users/${usuario.uid}`, dados: { currentHouseId: casaId }, campos: ["currentHouseId"], exigirExistente: true },
    ];
    return { escritas, resultado: { casaId } };
  });
}

/** Aceita minúsculas, espaços e separadores ("k7p-x2m"); devolve os 6 caracteres. */
function lerCodigo(corpo: unknown): string {
  const c = typeof corpo === "object" && corpo !== null ? (corpo as Record<string, unknown>)["codigo"] : undefined;
  const codigo = typeof c === "string" ? c.toUpperCase().replace(/[^A-Z0-9]/g, "") : "";
  if (codigo.length !== TAMANHO_CODIGO || [...codigo].some((ch) => !ALFABETO_CODIGO.includes(ch))) {
    throw new ErroHttp(404, "codigo-invalido");
  }
  return codigo;
}

/** Sorteio uniforme no alfabeto (descarta bytes que causariam viés). */
export function gerarCodigo(): string {
  const limite = 256 - (256 % ALFABETO_CODIGO.length);
  let codigo = "";
  while (codigo.length < TAMANHO_CODIGO) {
    for (const b of crypto.getRandomValues(new Uint8Array(TAMANHO_CODIGO * 2))) {
      if (b < limite && codigo.length < TAMANHO_CODIGO) codigo += ALFABETO_CODIGO[b % ALFABETO_CODIGO.length];
    }
  }
  return codigo;
}
