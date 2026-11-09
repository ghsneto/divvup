# CLAUDE.md — Divvup

App Android (Kotlin + Jetpack Compose + Firebase) de divisão gamificada de tarefas domésticas com swipe. Destino: Google Play, pt-BR.

## Leia primeiro
- **[SPEC.md](SPEC.md) é a fonte da verdade.** Regras de negócio têm IDs (`R-01`…`R-46`): cite o ID em commits, testes e comentários quando implementar uma regra.
- Se uma tarefa exigir comportamento que a SPEC não cobre ou contradiz, **pare e pergunte** — não invente regra. Mudanças de regra entram na SPEC antes do código.
- Implemente por marco (SPEC §7). Não adiante funcionalidades de v1.1/Futuro, mas respeite os campos já previstos no modelo de dados (§4.3).

## Invariantes que não podem ser quebrados
- Pacote/applicationId: **`br.app.divvup`**. Nunca alterar.
- O cliente **nunca grava** pontos, status de atribuição, papéis ou vínculo com casa. Isso é sempre Callable Function com transação (SPEC §4.2).
- Pontos: 25 / 50 / 100; meia pontuação após o prazo; nunca negativos.
- Endereço exato (número) nunca exposto publicamente.
- Testes e desenvolvimento usam o **Firebase Emulator** ou o projeto `divvup-dev`. **Nunca** apontar para `divvup-prod` em desenvolvimento.
- Nenhum segredo (chaves, `google-services.json` de prod, keystore) é commitado.

## Arquitetura
- Single-Activity, Navigation Compose, MVVM + UDF (`StateFlow` de estado imutável), Hilt, Coroutines/Flow.
- Módulos: `:app`, `:core:{designsystem,model,domain,data,testing}`, `:feature:{auth,onboarding,house,deck,tasks,ranking,notifications,profile,discover}`.
- `:core:domain` é Kotlin puro (sem Android/Firebase): toda regra de cálculo mora lá e é testada por unidade.
- Cloud Functions em TypeScript em `functions/`, testadas no Emulator.
- minSdk 26. Flavors `dev` / `prod`.

## Design
- **O app tem que ficar igual ao desenho.** A referência está versionada em [`docs/design/`](docs/design/README.md): leia o README dela antes de mexer em qualquer UI. Ela traz o mapa código da TELAS.md → artboard, os PNGs claro/escuro de cada tela, o HTML-fonte com as medidas exatas, tokens, ícones, ilustrações e avatares.
- Ordem de precedência: telas (`docs/design/fonte` + estilo das telas) → PNGs → [TELAS.md](TELAS.md) (comportamento, estados, navegação) → design system → SPEC. Regra de negócio `R-xx` sempre vence o desenho; se o desenho a contradiz, pare e pergunte.
- Originais no Claude Design: telas https://claude.ai/artifact/FCk2g4sxMAj8zyKA7soACw · protótipo https://claude.ai/artifact/JS5bWp4d6nNk2KMTDh4XRk · design system https://claude.ai/artifact/UjULqNSrf5z5nr8z4KdEQq. Se o desenho mudar lá, atualize `docs/design/` no mesmo PR do código.
- Valores do desenho em px = dp; tamanhos de fonte = sp. Textos do desenho são a cópia final (exceto dados de exemplo) e vão para `strings.xml` como estão.
- Toda cor, tipografia, forma e espaçamento vem de `DivvupTheme` em `:core:designsystem`. **Nada de valores literais nas telas** (o lint barra `Color(0x…)`, `.dp` e `.sp` fora de `:core:designsystem`).
- Ícones, ilustrações e avatares vêm de `docs/design/{icones,ilustracoes,avatares}` convertidos para vector drawable; nunca redesenhe nem troque por Material Icons.
- Swipe aceitar = `accent` (lima); passar = `danger`. Lima nunca como texto sobre fundo claro.
- Títulos em minúsculas, sem emoji na UI, números no formato pt-BR, todos os textos em `strings.xml`.
- Toda tela tem estados carregando / vazio / erro / sem permissão. Testar claro e escuro.

## Fluxo de trabalho
- Antes de declarar algo pronto: build + testes unitários + testes do Emulator relevantes passando.
- **Pronto para tela/componente** só quando:
  1. existe teste de screenshot (Roborazzi) de cada estado desenhado, em claro e escuro, a 360×800 dp;
  2. você comparou lado a lado o screenshot gerado com `docs/design/png/<Artboard>.png` (e `__escuro`) e listou no PR qualquer diferença restante, com o motivo;
  3. os screenshots aprovados estão commitados e a CI os verifica (`verifyRoborazzi*`).
- Regras de domínio e Functions: escreva o teste junto (de preferência antes).
- Commits pequenos, mensagem em português, referenciando `R-xx` quando aplicável.

## Agent skills

### Issue tracker

Issues e tickets ficam no GitHub Issues de `ghsneto/divvup`, operados com o `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Vocabulário padrão de cinco rótulos (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: um `GLOSSARY.md` e `docs/adr/` na raiz. See `docs/agents/domain.md`.
