# Firebase

Configuração do único projeto real, `divvup-prod` (ADR 0002), e do Firebase Emulator usado em
desenvolvimento e testes.

- `firestore.rules`: Security Rules (padrão negar tudo). Testes em `backend/test/rules.test.ts`.
- `firebase.json`: Emulator de Auth (9099) e Firestore (8080). Sem Cloud Functions (ADR 0001).

Os testes usam o projeto fictício **`demo-divvup`**: com o prefixo `demo-`, o Emulator nunca
alcança recursos reais. Nada aqui roda contra `divvup-prod`.

```bash
cd backend && npm run test:emulador
```
