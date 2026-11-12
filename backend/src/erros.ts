/** Erro com status HTTP e código estável que o app traduz em mensagem (strings.xml). */
export class ErroHttp extends Error {
  constructor(
    readonly status: number,
    readonly codigo: string,
    mensagem?: string,
    /** Dado público para a mensagem do app (ex.: quem assumiu antes); vai no campo `detalhe`. */
    readonly detalhe?: string,
  ) {
    super(mensagem ?? codigo);
  }
}

export const naoAutenticado = (detalhe?: string) => new ErroHttp(401, "nao-autenticado", detalhe);

export function json(corpo: unknown, status = 200): Response {
  return new Response(JSON.stringify(corpo), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  });
}

export function respostaDeErro(erro: unknown): Response {
  if (erro instanceof ErroHttp) {
    const corpo: Record<string, string> = { erro: erro.codigo };
    if (erro.detalhe !== undefined) corpo["detalhe"] = erro.detalhe;
    return json(corpo, erro.status);
  }
  console.error(erro);
  return json({ erro: "interno" }, 500);
}
