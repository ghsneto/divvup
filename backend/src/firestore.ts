import { importPKCS8, SignJWT } from "jose";
import { type Env, usaEmulador } from "./env.js";

/**
 * Cliente mínimo da API REST do Firestore. O backend escreve com a conta de serviço, que ignora
 * as Security Rules: é aqui que moram pontos, atribuição, papéis e vínculo com casa (SPEC §4.2).
 */

export type Valor =
  | null
  | boolean
  | number
  | string
  | Date
  | Valor[]
  | { [campo: string]: Valor };

export type Documento = Record<string, Valor>;

type ValorRest = Record<string, unknown>;

export function paraRest(v: Valor): ValorRest {
  if (v === null) return { nullValue: null };
  if (typeof v === "boolean") return { booleanValue: v };
  if (typeof v === "number") return Number.isInteger(v) ? { integerValue: String(v) } : { doubleValue: v };
  if (typeof v === "string") return { stringValue: v };
  if (v instanceof Date) return { timestampValue: v.toISOString() };
  if (Array.isArray(v)) return { arrayValue: { values: v.map(paraRest) } };
  return { mapValue: { fields: camposParaRest(v) } };
}

export function deRest(v: ValorRest): Valor {
  if ("nullValue" in v) return null;
  if ("booleanValue" in v) return v["booleanValue"] as boolean;
  if ("integerValue" in v) return Number(v["integerValue"]);
  if ("doubleValue" in v) return v["doubleValue"] as number;
  if ("stringValue" in v) return v["stringValue"] as string;
  if ("timestampValue" in v) return new Date(v["timestampValue"] as string);
  if ("arrayValue" in v) {
    const valores = (v["arrayValue"] as { values?: ValorRest[] }).values ?? [];
    return valores.map(deRest);
  }
  if ("mapValue" in v) return camposDeRest((v["mapValue"] as { fields?: Record<string, ValorRest> }).fields ?? {});
  throw new Error(`tipo do Firestore não suportado: ${Object.keys(v).join(",")}`);
}

const camposParaRest = (d: Documento) => Object.fromEntries(Object.entries(d).map(([k, v]) => [k, paraRest(v)]));
const camposDeRest = (f: Record<string, ValorRest>): Documento =>
  Object.fromEntries(Object.entries(f).map(([k, v]) => [k, deRest(v)]));

/** Escrita dentro de um commit (atômico; com `transacao`, falha se algo lido mudou). */
export type Escrita =
  | { tipo: "gravar"; caminho: string; dados: Documento; exigirInexistente?: boolean }
  | { tipo: "apagar"; caminho: string };

interface ContaDeServico {
  client_email: string;
  private_key: string;
}

let tokenEmCache: { valor: string; expiraEm: number } | undefined;

export class Firestore {
  private readonly base: string;
  private readonly raiz: string;

  constructor(
    private readonly env: Env,
    private readonly http: typeof fetch = fetch,
  ) {
    const host = env.FIRESTORE_EMULATOR_HOST ? `http://${env.FIRESTORE_EMULATOR_HOST}` : "https://firestore.googleapis.com";
    this.raiz = `projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents`;
    this.base = `${host}/v1/${this.raiz}`;
  }

  /** Lê um documento; `undefined` se não existe. */
  async ler(caminho: string, transacao?: string): Promise<Documento | undefined> {
    const [doc] = await this.lerVarios([caminho], transacao);
    return doc;
  }

  /**
   * Lê vários documentos numa chamada (`batchGet`), na ordem pedida; `undefined` para os que não
   * existem. Dentro de transação, use sempre esta leitura (o GET com `transaction` trava no Emulator).
   */
  async lerVarios(caminhos: string[], transacao?: string): Promise<(Documento | undefined)[]> {
    const nomes = caminhos.map((c) => `${this.raiz}/${c}`);
    const corpo: Record<string, unknown> = { documents: nomes };
    if (transacao) corpo["transaction"] = transacao;
    const resp = await this.chamar(`${this.base}:batchGet`, { method: "POST", body: JSON.stringify(corpo) });
    await garantirOk(resp, "batchGet");
    const itens = (await resp.json()) as { found?: { name: string; fields?: Record<string, ValorRest> } }[];
    const porNome = new Map<string, Documento>();
    for (const item of itens) {
      if (item.found) porNome.set(item.found.name, camposDeRest(item.found.fields ?? {}));
    }
    return nomes.map((n) => porNome.get(n));
  }

  /** Grava e apaga atomicamente, sem transação de leitura. */
  async commit(escritas: Escrita[]): Promise<void> {
    await this.enviarCommit(escritas);
  }

  /**
   * Transação: `corpo` lê com o id da transação e devolve as escritas. Se outro cliente mudou o
   * que foi lido, o Firestore rejeita o commit e a transação é repetida (até 5 vezes).
   */
  async transacao<T>(corpo: (tx: string) => Promise<{ escritas: Escrita[]; resultado: T }>): Promise<T> {
    for (let tentativa = 1; ; tentativa++) {
      const resp = await this.chamar(`${this.base}:beginTransaction`, { method: "POST", body: "{}" });
      await garantirOk(resp, "beginTransaction");
      const { transaction } = (await resp.json()) as { transaction: string };
      const { escritas, resultado } = await corpo(transaction);
      try {
        await this.enviarCommit(escritas, transaction);
        return resultado;
      } catch (erro) {
        if (!(erro instanceof ConflitoDeTransacao) || tentativa >= 5) throw erro;
      }
    }
  }

  private async enviarCommit(escritas: Escrita[], transacao?: string): Promise<void> {
    const writes = escritas.map((e) => {
      const nome = `${this.raiz}/${e.caminho}`;
      if (e.tipo === "apagar") return { delete: nome };
      const w: Record<string, unknown> = { update: { name: nome, fields: camposParaRest(e.dados) } };
      if (e.exigirInexistente) w["currentDocument"] = { exists: false };
      return w;
    });
    const resp = await this.chamar(`${this.base}:commit`, {
      method: "POST",
      body: JSON.stringify(transacao ? { writes, transaction: transacao } : { writes }),
    });
    if (resp.status === 409 || resp.status === 412) throw new ConflitoDeTransacao();
    await garantirOk(resp, "commit");
  }

  private async chamar(url: string, init: RequestInit): Promise<Response> {
    const headers = new Headers(init.headers);
    headers.set("authorization", `Bearer ${await this.tokenDeAcesso()}`);
    headers.set("content-type", "application/json");
    return this.http(url, { ...init, headers });
  }

  private async tokenDeAcesso(): Promise<string> {
    // O Emulator aceita "owner" como credencial de administrador.
    if (usaEmulador(this.env)) return "owner";
    const agora = Date.now();
    if (tokenEmCache && tokenEmCache.expiraEm > agora + 60_000) return tokenEmCache.valor;
    if (!this.env.FIREBASE_SERVICE_ACCOUNT) throw new Error("FIREBASE_SERVICE_ACCOUNT ausente");
    const conta = JSON.parse(this.env.FIREBASE_SERVICE_ACCOUNT) as ContaDeServico;
    const chave = await importPKCS8(conta.private_key, "RS256");
    const afirmacao = await new SignJWT({ scope: "https://www.googleapis.com/auth/datastore" })
      .setProtectedHeader({ alg: "RS256", typ: "JWT" })
      .setIssuer(conta.client_email)
      .setAudience("https://oauth2.googleapis.com/token")
      .setIssuedAt()
      .setExpirationTime("1h")
      .sign(chave);
    const resp = await this.http("https://oauth2.googleapis.com/token", {
      method: "POST",
      headers: { "content-type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: afirmacao }),
    });
    await garantirOk(resp, "token");
    const { access_token, expires_in } = (await resp.json()) as { access_token: string; expires_in: number };
    tokenEmCache = { valor: access_token, expiraEm: agora + expires_in * 1000 };
    return access_token;
  }
}

/** O documento lido na transação mudou (ou a precondição falhou): repetir. */
export class ConflitoDeTransacao extends Error {}

async function garantirOk(resp: Response, operacao: string): Promise<void> {
  if (!resp.ok) throw new Error(`Firestore ${operacao}: HTTP ${resp.status} ${await resp.text()}`);
}
