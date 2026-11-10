# 0002 · Um único projeto Firebase (`divvup-prod`)

- **Status:** aceita
- **Data:** 2026-11-10

## Contexto

A SPEC previa dois projetos Firebase, `divvup-dev` e `divvup-prod`, para separar dados de teste dos dados reais. O dono do projeto criou só o `divvup-prod` e decidiu trabalhar sempre em cima dele, mantendo a configuração simples.

## Decisão

- Existe **um só projeto Firebase real: `divvup-prod`**, no plano Spark (ADR 0001).
- O flavor `dev` do app e o backend local apontam para o **Firebase Emulator**. Testes automatizados e CI usam **só** o Emulator.
- O flavor `prod` aponta para `divvup-prod`. Testes manuais em aparelho real, como "dois aparelhos na mesma casa", usam esse projeto.

## Consequências

- Dados criados em testes manuais ficam no mesmo banco que os usuários reais vão usar. **Antes da publicação (M5), apagar as contas e os dados de teste** do Auth e do Firestore.
- Nunca rodar testes automatizados, scripts, seeds ou limpezas contra `divvup-prod`: um erro afeta usuários reais.
- As cotas gratuitas do Spark são do projeto: o uso de teste consome a mesma cota dos usuários.
- Só há um `google-services.json` (`GOOGLE_SERVICES_JSON_PROD`) e uma conta de serviço (`FIREBASE_SERVICE_ACCOUNT_PROD`).
- O login com Google no aparelho de desenvolvimento exige o **SHA-1 de debug** cadastrado no app Android do `divvup-prod`, além do SHA-1 da assinatura do Play quando o app for publicado.
- Se for preciso separar ambientes depois, criar um projeto de dev é gratuito e não muda o código, só a configuração do flavor.
