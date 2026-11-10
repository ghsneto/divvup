import type { Usuario } from "../auth.js";
import { ErroHttp } from "../erros.js";
import type { Documento, Escrita, Firestore } from "../firestore.js";
import { NOME_CASA_MAXIMO, TIPOS_DE_CASA, type TipoDeCasa } from "./casa.js";

/** Lê quem pede, a casa atual e todos os membros, dentro da transação. */
async function contexto(db: Firestore, uid: string, tx: string) {
  const [perfil] = await db.lerVarios([`users/${uid}`], tx);
  const casaId = perfil?.["currentHouseId"];
  if (typeof casaId !== "string") throw new ErroHttp(409, "sem-casa");
  const [casa, eu] = await db.lerVarios([`houses/${casaId}`, `houses/${casaId}/members/${uid}`], tx);
  if (!casa || eu?.["status"] !== "ACTIVE") throw new ErroHttp(409, "sem-casa");
  return { casaId, casa, eu };
}

async function membroAtivo(db: Firestore, casaId: string, uid: string, tx: string): Promise<Documento> {
  const [m] = await db.lerVarios([`houses/${casaId}/members/${uid}`], tx);
  if (m?.["status"] !== "ACTIVE") throw new ErroHttp(404, "membro-inexistente");
  return m;
}

/** Membros ativos da casa, lidos dentro da transação. */
async function ativos(db: Firestore, casaId: string, tx: string): Promise<{ uid: string; dados: Documento }[]> {
  return (await db.listar(`houses/${casaId}/members`, tx)).filter((m) => m.dados["status"] === "ACTIVE");
}

/** Escritas que tiram alguém da casa (R-29: tarefas assumidas voltam ao deck quando existirem). */
function saida(casaId: string, uid: string, casa: Documento, agora: Date): Escrita[] {
  return [
    { tipo: "gravar", caminho: `houses/${casaId}/members/${uid}`, dados: { status: "LEFT", leftAt: agora, role: "MEMBER" }, campos: ["status", "leftAt", "role"], exigirExistente: true },
    { tipo: "gravar", caminho: `houses/${casaId}`, dados: { memberCount: Math.max(((casa["memberCount"] as number) ?? 1) - 1, 0) }, campos: ["memberCount"], exigirExistente: true },
    { tipo: "gravar", caminho: `users/${uid}`, dados: { currentHouseId: null }, campos: ["currentHouseId"], exigirExistente: true },
  ];
}

/** G3: admin torna outro morador admin ou tira o papel (R-03). Sempre sobra pelo menos um admin. */
export async function alterarPapel(db: Firestore, usuario: Usuario, corpo: unknown): Promise<{ ok: true }> {
  const c = objeto(corpo);
  const alvo = texto(c["uid"], "uid-invalido");
  const papel = c["papel"];
  if (papel !== "ADMIN" && papel !== "MEMBER") throw new ErroHttp(400, "papel-invalido");
  return db.transacao(async (tx) => {
    const { casaId, eu } = await contexto(db, usuario.uid, tx);
    if (eu["role"] !== "ADMIN") throw new ErroHttp(403, "nao-admin");
    const membro = await membroAtivo(db, casaId, alvo, tx);
    if (papel === "MEMBER" && membro["role"] === "ADMIN") {
      const admins = (await ativos(db, casaId, tx)).filter((m) => m.dados["role"] === "ADMIN");
      if (admins.length <= 1) throw new ErroHttp(409, "ultimo-admin");
    }
    return {
      escritas: [{ tipo: "gravar", caminho: `houses/${casaId}/members/${alvo}`, dados: { role: papel }, campos: ["role"], exigirExistente: true }],
      resultado: { ok: true as const },
    };
  });
}

/** G3: admin remove outro morador. Para sair, a pessoa usa `sairDaCasa`. */
export async function removerMembro(db: Firestore, usuario: Usuario, corpo: unknown, agora: Date = new Date()): Promise<{ ok: true }> {
  const alvo = texto(objeto(corpo)["uid"], "uid-invalido");
  if (alvo === usuario.uid) throw new ErroHttp(400, "use-sair-da-casa");
  return db.transacao(async (tx) => {
    const { casaId, casa, eu } = await contexto(db, usuario.uid, tx);
    if (eu["role"] !== "ADMIN") throw new ErroHttp(403, "nao-admin");
    await membroAtivo(db, casaId, alvo, tx);
    return { escritas: saida(casaId, alvo, casa, agora), resultado: { ok: true as const } };
  });
}

/** G1: qualquer morador sai. Se era o último admin, o ativo mais antigo vira admin (R-04). */
export async function sairDaCasa(db: Firestore, usuario: Usuario, agora: Date = new Date()): Promise<{ novoAdmin: string | null }> {
  return db.transacao(async (tx) => {
    const { casaId, casa, eu } = await contexto(db, usuario.uid, tx);
    const escritas = saida(casaId, usuario.uid, casa, agora);
    let novoAdmin: string | null = null;
    if (eu["role"] === "ADMIN") {
      const outros = (await ativos(db, casaId, tx)).filter((m) => m.uid !== usuario.uid);
      if (outros.length > 0 && !outros.some((m) => m.dados["role"] === "ADMIN")) {
        const maisAntigo = outros.reduce((a, b) => ((a.dados["joinedAt"] as Date) <= (b.dados["joinedAt"] as Date) ? a : b));
        novoAdmin = maisAntigo.uid;
        escritas.push({ tipo: "gravar", caminho: `houses/${casaId}/members/${novoAdmin}`, dados: { role: "ADMIN" }, campos: ["role"], exigirExistente: true });
      }
    }
    return { escritas, resultado: { novoAdmin } };
  });
}

/** G4: admin edita nome, tipo e endereço sem número. Casal/Família desliga o anúncio (R-38). */
export async function editarCasa(db: Firestore, usuario: Usuario, corpo: unknown): Promise<{ ok: true }> {
  const c = objeto(corpo);
  const nome = typeof c["nome"] === "string" ? c["nome"].trim().replace(/\s+/g, " ") : "";
  if (nome.length === 0 || nome.length > NOME_CASA_MAXIMO) throw new ErroHttp(400, "nome-invalido");
  const tipo = c["tipo"];
  if (typeof tipo !== "string" || !(TIPOS_DE_CASA as readonly string[]).includes(tipo)) throw new ErroHttp(400, "tipo-invalido");
  const local = (campo: string) => {
    const v = c[campo];
    if (v === undefined || v === null) return null;
    if (typeof v !== "string" || v.length > LOCAL_MAXIMO) throw new ErroHttp(400, `${campo}-invalido`);
    return v.trim() || null;
  };
  const dados: Documento = { name: nome, type: tipo as TipoDeCasa, city: local("cidade"), neighborhood: local("bairro"), street: local("rua") };
  return db.transacao(async (tx) => {
    const { casaId, eu } = await contexto(db, usuario.uid, tx);
    if (eu["role"] !== "ADMIN") throw new ErroHttp(403, "nao-admin");
    const campos = ["name", "type", "city", "neighborhood", "street"];
    if (tipo === "CASAL_FAMILIA") {
      dados["listing"] = { enabled: false };
      campos.push("listing.enabled");
    }
    return {
      escritas: [{ tipo: "gravar", caminho: `houses/${casaId}`, dados, campos, exigirExistente: true }],
      resultado: { ok: true as const },
    };
  });
}

const LOCAL_MAXIMO = 80;

function objeto(corpo: unknown): Record<string, unknown> {
  return typeof corpo === "object" && corpo !== null ? (corpo as Record<string, unknown>) : {};
}

function texto(v: unknown, erro: string): string {
  if (typeof v !== "string" || v.length === 0) throw new ErroHttp(400, erro);
  return v;
}
