# Backend (Cloudflare Workers)

Operações protegidas e tarefas agendadas do Divvup (SPEC §4.2, ADR 0001). O app chama
`POST /v1/<operacao>` com o ID token do Firebase Auth; o Worker valida o token e grava no
Firestore com a conta de serviço, em transação.

Requer Node 24 e Java 21 (para o Firestore Emulator).

```bash
npm ci
npm run lint            # ESLint + tipos
npm run test:emulador   # sobe Auth + Firestore Emulator (projeto demo-divvup) e roda os testes
npm run dev             # Worker local, ligado ao Emulator
```

- `src/auth.ts`: validação do ID token (RS256 publicado; só declarações no Emulator).
- `src/firestore.ts`: cliente REST do Firestore (leitura em lote, commit, transação com nova tentativa).
- `wrangler.toml`: execução local (Emulator) e ambiente `prod` (`divvup-prod`).

Deploy: `.github/workflows/deploy-backend.yml`, a cada push na `main`, depois que os segredos da
issue #41 existirem. Nunca rode testes ou scripts contra `divvup-prod` (ADR 0002).
