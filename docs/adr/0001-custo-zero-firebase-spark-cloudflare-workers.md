# 0001 · Custo zero: Firebase Spark + backend em Cloudflare Workers

- **Status:** aceita
- **Data:** 2026-11-10

## Contexto

O objetivo do projeto é, pelo menos no início, **não ter nenhum custo de infraestrutura**. A SPEC previa Firebase no plano Blaze com Cloud Functions. Ao ativar o Blaze, o Google pediu um pré-pagamento de R$ 150, o que quebra esse objetivo.

O que o app precisa do servidor:

- mutações protegidas (o cliente nunca grava pontos, atribuição, papéis ou vínculo com casa) em transação;
- disputa pela mesma tarefa no swipe (quem desliza primeiro leva);
- tarefas agendadas: atribuição automática, temporada, lembretes, pushes retidos;
- envio de push.

No plano gratuito do Firebase (Spark) estão Auth, Firestore, FCM, Crashlytics e Analytics, mas **não** a publicação de Cloud Functions.

## Decisão

- **Firebase no plano Spark** para Auth, Firestore (tempo real e cache offline), FCM, Crashlytics e Analytics.
- **Backend em Cloudflare Workers** (plano gratuito, TypeScript, pasta `backend/`) no lugar das Cloud Functions:
  - endpoints HTTPS chamados pelo app com o ID token do Firebase;
  - o Worker valida o token, grava no Firestore com uma conta de serviço (transações pela API REST) e envia push pela API HTTP v1 do FCM;
  - Cron Triggers fazem as tarefas agendadas.
- **Regra:** nada que exija cartão, conta de faturamento ou pré-pagamento entra no projeto sem uma nova decisão.

## Alternativas descartadas

- **Firebase Blaze + Cloud Functions:** a arquitetura mais simples, mas exige o pré-pagamento.
- **Tudo na Cloudflare (D1, Durable Objects, Workers):** também tem custo zero, mas obriga a construir sincronização em tempo real, cache offline e login por e-mail/senha (com envio de e-mail). Ainda precisaria do Firebase para FCM e Crashlytics.
- **Lógica só no app com Security Rules:** sem tarefas agendadas, e proteção frágil de pontos e da disputa do swipe.

## Consequências

- O backend precisa de uma conta de serviço do Firebase guardada como segredo no Cloudflare (uma por ambiente).
- Testes do backend rodam com o Firebase Emulator (Auth + Firestore) e o Worker local (`wrangler dev`); não há emulador de Functions.
- As cotas gratuitas (Firestore, Workers) são o teto de uso. Passar delas derruba o serviço até a renovação diária, sem cobrança. A modelagem deve economizar leituras: ranking e índice pré-calculados.
- Há duas plataformas para configurar (Firebase e Cloudflare) e uma credencial a mais para proteger.
- **Em aberto:** a v1.1 usa Maps SDK e Places API, que exigem conta de faturamento no Google Cloud. Decidir antes da v1.1.
- Custos que não são de infraestrutura e continuam existindo: taxa única do Play Console (US$ 25) e, se desejado, o domínio `divvup.app.br` (há a alternativa gratuita `*.pages.dev`).
