# Firebase

Configuração do único projeto real, `divvup-prod` (ADR 0002), e do Firebase Emulator usado em
desenvolvimento e testes.

- `firestore.rules`: Security Rules (padrão negar tudo). Testes em `backend/test/rules.test.ts`.
- `firestore.indexes.json`: índices compostos (ex.: grupo `tasks` por `status` + `dueAt`, usado pela
  rotina agendada do backend).
- `firebase.json`: Emulator de Auth (9099) e Firestore (8080). Sem Cloud Functions (ADR 0001).

Os testes usam o projeto fictício **`demo-divvup`**: com o prefixo `demo-`, o Emulator nunca
alcança recursos reais. Nada aqui roda contra `divvup-prod`.

```bash
cd backend && npm run test:emulador
```

## Publicação

Regras e índices vão para o `divvup-prod` pelo workflow `deploy-firebase.yml`, a cada push na
`main` que mexe neles (ou manualmente em Actions › Run workflow). Ele usa o segredo
`FIREBASE_SERVICE_ACCOUNT_PROD`, o mesmo do backend.
