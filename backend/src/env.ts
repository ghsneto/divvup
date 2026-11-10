/** Variáveis e segredos do Worker (wrangler.toml + `wrangler secret`). */
export interface Env {
  /** ID do projeto Firebase: `divvup-prod` publicado, `demo-divvup` local. */
  FIREBASE_PROJECT_ID: string;
  /** Presente só em execução local: host do Auth Emulator (ex.: `127.0.0.1:9099`). */
  FIREBASE_AUTH_EMULATOR_HOST?: string;
  /** Presente só em execução local: host do Firestore Emulator (ex.: `127.0.0.1:8080`). */
  FIRESTORE_EMULATOR_HOST?: string;
  /** Segredo: JSON da conta de serviço do Firebase (só no ambiente publicado). */
  FIREBASE_SERVICE_ACCOUNT?: string;
}

/** True quando o Worker deve falar com o Emulator, nunca com o projeto real. */
export function usaEmulador(env: Env): boolean {
  return Boolean(env.FIREBASE_AUTH_EMULATOR_HOST || env.FIRESTORE_EMULATOR_HOST);
}
