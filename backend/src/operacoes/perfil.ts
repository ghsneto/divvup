import type { Usuario } from "../auth.js";
import { ErroHttp } from "../erros.js";
import type { Documento, Firestore } from "../firestore.js";

export const NOME_MAXIMO = 40;

/** Horário silencioso padrão (R-35). */
export const SILENCIO_PADRAO = { start: "22:00", end: "08:00" };

/**
 * Cria `users/{uid}` com os valores iniciais (SPEC §4.3). Idempotente: se o perfil já existe,
 * não altera nada. Pontos, sequência e medalhas só o backend grava (SPEC §4.2).
 */
export async function criarPerfil(db: Firestore, usuario: Usuario, corpo: unknown): Promise<{ criado: boolean }> {
  const nome = lerNome(corpo);
  const caminho = `users/${usuario.uid}`;
  if (await db.ler(caminho)) return { criado: false };

  const perfil: Documento = {
    displayName: nome,
    email: usuario.email ?? null,
    avatarId: null,
    photoUrl: null,
    currentHouseId: null,
    totalPoints: 0,
    tasksDone: 0,
    streak: { current: 0, best: 0, lastDay: null },
    badges: [],
    notifPrefs: {},
    quietHours: SILENCIO_PADRAO,
    fcmTokens: [],
    createdAt: new Date(),
  };
  try {
    await db.commit([{ tipo: "gravar", caminho, dados: perfil, exigirInexistente: true }]);
    return { criado: true };
  } catch (erro) {
    // Duas chamadas simultâneas: a segunda encontra o perfil já criado.
    if (await db.ler(caminho)) return { criado: false };
    throw erro;
  }
}

function lerNome(corpo: unknown): string {
  const nome = typeof corpo === "object" && corpo !== null ? (corpo as Record<string, unknown>)["nome"] : undefined;
  if (typeof nome !== "string") throw new ErroHttp(400, "nome-invalido");
  const limpo = nome.trim().replace(/\s+/g, " ");
  if (limpo.length === 0 || limpo.length > NOME_MAXIMO) throw new ErroHttp(400, "nome-invalido");
  return limpo;
}
