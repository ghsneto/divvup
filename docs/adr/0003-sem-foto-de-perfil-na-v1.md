# 0003 · Sem foto de perfil própria na v1.0

- **Status:** aceita
- **Data:** 2026-11-10

## Contexto

As telas B1, E2 e E3 têm "Usar foto" e "Remover foto". Guardar a imagem exige um armazenamento de arquivos, e as opções óbvias quebram o custo zero (ADR 0001): o Firebase Storage exige o plano Blaze em projetos novos e o Cloudflare R2 exige cartão cadastrado. Guardar uma foto pequena no Cloudflare KV seria gratuito, mas traz mais código no backend e consome a cota diária de leituras.

## Decisão

- Na v1.0 o perfil usa **só os 24 avatares DiceBear** (R-32).
- Os links "Usar foto", "Usar uma foto" e "Remover foto" do desenho ficam **escondidos**.
- O campo `photoUrl` continua no modelo; o backend grava nele a foto do Google no primeiro acesso, mas o app não a exibe.

## Consequências

- Diferença conhecida em relação ao desenho, registrada em `docs/design/README.md`.
- Quando houver orçamento, a foto entra sem mudar o modelo de dados: falta escolher o armazenamento, a operação de upload no Worker e as rules.
