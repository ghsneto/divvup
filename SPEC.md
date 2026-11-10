# Divvup — Especificação do Produto

> Versão 1.0 da especificação · 2026-11-06
> Fonte da verdade para o desenvolvimento. Toda regra de negócio aqui é decisão tomada; mudanças passam por este arquivo primeiro.

---

## 1. Visão

**Divvup** é um app Android gamificado de divisão de tarefas domésticas para repúblicas, moradias compartilhadas e casais/famílias. As tarefas pendentes da casa aparecem como cartões: **arrasta pra direita, a tarefa é sua; arrasta pra esquerda, passa a vez**. Pontos, temporadas, medalhas e um índice de equilíbrio tornam a divisão visível e justa.

- **Problema:** atrito, desorganização e sobrecarga invisível na divisão de afazeres de quem mora junto.
- **Público:** estudantes em repúblicas (inclusive quem chega de outro estado procurando vaga), casais e pessoas que dividem moradia.
- **Distribuição:** Google Play, pt-BR, Brasil. Gratuito, sem anúncios no lançamento.
- **Identidade:** domínio `divvup.app.br`, pacote Android **`br.app.divvup`** (imutável após publicação), título na loja "Divvup: Tarefas da Casa".

---

## 2. Glossário

| Termo | Significado |
|---|---|
| **Casa** | O grupo (tenant). Tipos: `REPUBLICA`, `MORADIA_COMPARTILHADA`, `CASAL_FAMILIA`. |
| **Membro** | Usuário vinculado a uma casa, com papel `ADMIN` ou `MEMBER`. |
| **Modelo de tarefa** (`TaskTemplate`) | A definição de um afazer (título, categoria, pontos, regra de recorrência). |
| **Ocorrência** (`Task`) | Uma instância concreta de um modelo, com prazo. É o **card** do deck. |
| **Deck** | Pilha de ocorrências sem dono que o usuário vê na tela principal. |
| **Match** | Swipe à direita: o usuário assume a ocorrência. Primeiro a deslizar leva. |
| **Passe** | Swipe à esquerda: oculta a ocorrência só do deck daquele usuário, temporariamente. |
| **Temporada** | Mês civil (`yyyy-MM`). Pontos da temporada zeram a cada mês. |
| **Índice de Equilíbrio** | 0–100: quão uniformemente os pontos da temporada estão distribuídos entre os membros. |
| **Anúncio** | Casa visível publicamente na busca (lista/mapa) com vagas. |

---

## 3. Regras de negócio

### 3.1 Contas e casas
- R-01 · Login por **e-mail/senha** e **Google** (Credential Manager).
- R-02 · Um usuário pertence a **no máximo uma casa por vez**. Para trocar, sai da atual primeiro.
- R-03 · Quem cria a casa vira `ADMIN`. Admins podem promover outros admins.
- R-04 · Se o último admin sai, o membro ativo mais antigo vira admin automaticamente.
- R-05 · Usuário sem casa pode usar o app para criar casa, entrar com código ou (v1.1) buscar casas.

### 3.2 Entrada na casa
- R-06 · **Código de convite**: 6 caracteres de alfabeto sem ambíguos (sem `O/0/I/1/L`), **válido por 24 h e de uso único**. Vários códigos podem estar ativos; o admin vê a lista e revoga. Usar um código válido = entrada direta, sem aprovação.
- R-07 · **Solicitação** (v1.1, via busca): usuário envia pedido com mensagem opcional; admin aprova ou recusa. Aprovação = entrada.
- R-08 · Compartilhamento do código pelo share sheet nativo do Android.

### 3.3 Tarefas
- R-09 · Qualquer membro cria tarefas: título, descrição opcional, categoria, peso, prazo.
- R-10 · **Peso → pontos:** Leve = **25**, Média = **50**, Pesada = **100**.
- R-11 · **Categorias fixas** (com ícone): Cozinha, Limpeza geral, Banheiro, Lavanderia, Lixo, Compras, Pets, Plantas, Manutenção, Outros.
- R-12 · Editar/excluir: autor ou admin, **somente enquanto sem dono**. Com dono, só admin pode cancelar.
- R-13 · **Recorrência:** fora do v1.0. Todo modelo nasce com `recurrence = null` e gera exatamente uma ocorrência. A estrutura modelo → ocorrência já existe para habilitar recorrência depois sem migração.

### 3.4 Match (deck de swipe)
- R-14 · O deck mostra ocorrências `OPEN` da casa, ordenadas por **prazo mais próximo**, desempate por **maior peso**, excluindo as que o usuário passou e cujo passe não expirou.
- R-15 · **Swipe à direita**: assume a ocorrência em transação no servidor. Se outro membro levou antes, mostra "alguém já pegou essa" e remove o card.
- R-16 · **Swipe à esquerda**: cria um passe pessoal com expiração de **12 h**. Sem penalidade.
- R-17 · Quando o deck acaba: "Você passou X tarefas — rever?" traz de volta as passadas.
- R-18 · **Desfazer**: botão disponível por **3 s** após cada swipe.
- R-19 · **Devolver**: o dono pode devolver a ocorrência ao deck enquanto faltar **mais de 24 h** para o prazo.
- R-20 · **Limite**: no máximo **5 ocorrências assumidas e não concluídas** por membro. No limite, swipe à direita bloqueado com explicação.

### 3.5 Conclusão e pontos
- R-21 · Conclusão é **autodeclarada** pelo dono, sem verificação.
- R-22 · Pontos são creditados **apenas na conclusão**: valor integral se dentro do prazo, **metade** se após o prazo. Nunca há pontos negativos.
- R-23 · Ocorrência assumida que passa do prazo sem conclusão fica `LATE`, visível no painel da casa.
- R-24 · Ocorrência concluída sai do deck/painel ativo e entra no histórico.

### 3.6 Justiça
- R-25 · **Transparência:** ranking e histórico mostram quem fez o quê.
- R-26 · **Atribuição automática:** ocorrência `OPEN` que chega a **24 h do prazo** é atribuída ao membro ativo com **menos pontos na temporada atual**; empate por sorteio. O escolhido recebe push. O limite de 5 (R-20) não se aplica à atribuição automática.
- R-27 · **Temporadas mensais:** ranking mensal zera todo dia 1º (fuso `America/Sao_Paulo`); o ranking final é congelado e o campeão recebe medalha.
- R-28 · **Ranking geral:** acumulado de todos os tempos **dentro da casa**. Rankings por bairro/cidade ficam para o futuro.
- R-29 · **Saída de membro:** pontos permanecem no histórico da casa (aparece como "ex-morador" no ranking geral, some do mensal); ocorrências assumidas voltam ao deck; histórico pessoal (medalhas, totais) segue com a conta.

### 3.7 Gamificação
- R-30 · **Medalhas v1.0:** primeira tarefa · sequência de 7 dias · campeão do mês · 100 tarefas concluídas · salva-vidas (assumiu ocorrência a menos de 48 h do prazo).
- R-31 · **Sequência diária:** dias consecutivos com ao menos uma conclusão.
- R-32 · **Avatares:** DiceBear estilo **avataaars**. v1.0: galeria de ~24 avatares com seeds pré-escolhidas, **empacotados como SVG no app** (sem rede), livres para todos. Foto pessoal opcional sobrepõe o avatar. `avatarId` guarda `estilo + seed + opções` para um **editor de avatar** futuro (geração no backend com `@dicebear/core`, nunca pela API pública).

### 3.8 Notificações
- R-33 · Eventos e destinatários:

| Evento | Quem recebe | Padrão |
|---|---|---|
| Tarefa atribuída automaticamente a você | o escolhido | ligado |
| Sua tarefa vence em 24 h / em 2 h | o dono | ligado |
| Sua tarefa atrasou | o dono | ligado |
| Nova tarefa criada na casa | todos menos o autor | ligado |
| Solicitação de entrada recebida (v1.1) | admins | ligado |
| Sua solicitação foi aprovada/recusada (v1.1) | o solicitante | ligado |
| Fim de temporada + campeão do mês | todos | ligado |
| Um morador concluiu uma tarefa | todos | **desligado** |
| Você foi removido da casa | o removido | ligado |

- R-34 · Usuário liga/desliga cada tipo no Perfil.
- R-35 · **Horário silencioso** configurável (padrão 22h–8h): pushes retidos e entregues no fim do período, exceto "vence em 2 h".
- R-36 · Permissão de notificação (Android 13+) pedida **após entrar numa casa**, nunca na primeira tela.
- R-37 · **Central de notificações** (sino na tela principal): histórico de 30 dias, lida/não lida, independente da permissão de push.

### 3.9 Busca de casas e anúncio (v1.1)
- R-38 · **Visibilidade por tipo de casa:** `REPUBLICA` nasce com "Anunciar vagas" **ligado** (pré-marcado e visível na criação, o admin pode desmarcar); `MORADIA_COMPARTILHADA` e `CASAL_FAMILIA` nascem **desligados**. Admin liga/desliga a qualquer momento; desligar remove a casa da busca imediatamente.
- R-39 · O anúncio só fica **efetivamente visível** depois de o admin preencher vagas e endereço e aceitar o termo de exposição.
- R-40 · **Privacidade do endereço:** a listagem mostra **bairro + rua, sem número**; o pin no mapa cai em localização aproximada (quadra). O **número** só é revelado ao solicitante após aprovação.
- R-41 · **Busca por local digitado** (cidade, bairro, ponto de referência como "UFPE"), via Places Autocomplete; não depende de GPS.
- R-42 · Alternância **Lista ↔ Mapa** (satélite/híbrido). Card: nome, tipo, bairro + rua, distância do ponto buscado, vagas, valor mensal (opcional), Índice de Equilíbrio, selo de atividade, foto de capa, botão "Solicitar vaga".
- R-43 · Filtros: vagas > 0, faixa de valor, tipo. Ordenação por distância.
- R-44 · Busca acessível a usuários **sem casa**.
- R-45 · **Pontuação pública** = Índice de Equilíbrio + selo de atividade (ex.: "40 tarefas no último mês"). Pontos brutos nunca são públicos.
- R-46 · Botão **Denunciar** no perfil público da casa.

---

## 4. Arquitetura técnica

### 4.1 Android
- Kotlin, **Jetpack Compose**, **Material 3** com o design system Divvup (cores dinâmicas **desligadas**, modo escuro suportado).
- **minSdk 26** (exigido pelos eixos variáveis da fonte), targetSdk = o mais recente exigido pela Play Store.
- Single-Activity + Navigation Compose. MVVM + UDF (`StateFlow` de UI state imutável). **Hilt**. Coroutines/Flow.
- Módulos:
  - `:app`
  - `:core:designsystem` (tema, tokens, componentes base)
  - `:core:model` (modelos de domínio puros)
  - `:core:domain` (regras puras: pontos, índice, temporadas — testáveis sem Android)
  - `:core:data` (repositórios, Firebase)
  - `:core:testing`
  - `:feature:auth`, `:feature:onboarding`, `:feature:house`, `:feature:deck`, `:feature:tasks`, `:feature:ranking`, `:feature:notifications`, `:feature:profile`, `:feature:discover` (v1.1)
- Build flavors: `dev` aponta para o **Firebase Emulator** (local); `prod` aponta para o único projeto real, `divvup-prod` (ADR 0002).
- Textos sempre em `strings.xml` (pt-BR), prontos para i18n.

### 4.2 Backend (custo zero: Firebase Spark + Cloudflare Workers)
- **Restrição:** nesta fase o projeto **não tem custo de infraestrutura**. Só se usa plano gratuito; nada que exija cartão, conta de faturamento ou pré-pagamento (decisão em [ADR 0001](docs/adr/0001-custo-zero-firebase-spark-cloudflare-workers.md)).
- **Firebase, plano Spark (gratuito):** **Auth** (e-mail/senha e Google), **Firestore** (tempo real e cache offline no app), **FCM**, **Crashlytics**, **Analytics**. Um único projeto, `divvup-prod` (ADR 0002), com Firestore em `southamerica-east1` (São Paulo, imutável); desenvolvimento e testes automatizados usam o Firebase Emulator. **Cloud Functions não são usadas** (exigem o plano Blaze).
- **Cloudflare Workers, plano gratuito:** o backend, em TypeScript, na pasta `backend/`. Valida o ID token do Firebase Auth em cada chamada, grava no Firestore com uma conta de serviço (transações pela API do Firestore) e envia push pela API HTTP v1 do FCM. Ambiente local (`wrangler dev`, ligado ao Emulator) e um ambiente publicado ligado a `divvup-prod`.
- **Regra central de segurança:** o cliente **nunca grava** pontos, status de atribuição, papéis ou vínculo com casa. Essas mutações são **operações do backend** (endpoints HTTPS do Worker, chamados com o ID token) e rodam em transação:
  `createHouse`, `createInvite`, `revokeInvite`, `joinWithCode`, `leaveHouse`, `removeMember`, `setRole`, `claimTask`, `passTask`, `undoLastAction`, `returnTask`, `completeTask`, `cancelTask`, `deleteAccount`; v1.1: `requestToJoin`, `reviewJoinRequest`, `setListing`, `reportHouse`.
- **Tarefas agendadas** (Cron Triggers do Worker): atribuição automática e lembretes de prazo (a cada 15 min), marcação de atraso, virada de temporada (dia 1º, 00:00 BRT), entrega de pushes retidos pelo horário silencioso, limpeza de convites e notificações expiradas.
- **Cotas gratuitas são o teto de uso.** Modelar para poucas leituras (ex.: ranking pré-calculado num documento por casa) e acompanhar o consumo; ultrapassar a cota derruba o serviço até o dia seguinte, não gera cobrança.
- **Em aberto (v1.1):** Maps SDK e Places API exigem conta de faturamento no Google Cloud; antes da v1.1 decidir alternativa sem custo ou rever a restrição.
- Security Rules isolam cada casa: leitura só para membros ativos; escrita direta só em campos não sensíveis (ex.: preferências do próprio usuário, criação de tarefa).

### 4.3 Modelo de dados (Firestore)

```
users/{uid}
  displayName, email, avatarId, photoUrl?, currentHouseId?
  totalPoints, tasksDone, streak{current, best, lastDay}, badges[]
  notifPrefs{<tipo>: bool}, quietHours{start, end}, fcmTokens[]
  └ notifications/{id}      title, body, type, refs, read, createdAt, expireAt (TTL 30d)

houses/{houseId}
  name, type, createdBy, createdAt, memberCount, city, neighborhood, street
  currentSeason ("2026-11")
  listing{enabled, vacancies, monthlyPrice?, coverUrl?, description?, termsAcceptedAt?}   ← v1.1
  approxLocation{lat, lng}, geohash, balanceIndex, activityLast30d                         ← v1.1
  └ private/address          number, complement, exactLocation (só membros + aprovados)
  └ members/{uid}            role, status(ACTIVE|LEFT), joinedAt, leftAt?,
                             displayName, avatarId, seasonPoints, totalPoints, openTasks
      └ passes/{taskId}      expiresAt (+12h)
  └ invites/{code}           createdBy, createdAt, expiresAt (+24h), usedBy?, usedAt?, revoked
  └ joinRequests/{uid}       status(PENDING|APPROVED|REJECTED), message, createdAt   ← v1.1
  └ taskTemplates/{id}       title, description, category, points(25|50|100),
                             recurrence: null | {rule}, createdBy, createdAt
  └ tasks/{id}               templateId, title, description, category, points, dueAt,
                             status(OPEN|ASSIGNED|DONE|LATE|CANCELLED),
                             assigneeId?, assignedVia(SWIPE|AUTO), assignedAt?,
                             completedAt?, pointsAwarded?, createdBy
  └ seasons/{yyyy-MM}        standings[], championId, closedAt
  └ activity/{id}            type, actorId, taskId?, points?, createdAt   (alimenta o Histórico)

reports/{id}                 houseId, reporterId, reason, createdAt   ← v1.1
```

### 4.4 Qualidade
- Testes unitários: ViewModels e `:core:domain` (pontos, meia pontuação, índice, temporada, desempates).
- **Firebase Emulator** (Auth + Firestore) com o backend rodando localmente (`wrangler dev`): Security Rules e operações do backend (corridas no `claimTask`, atribuição automática, expiração de convite).
- Testes de UI Compose no fluxo de swipe e desfazer.
- **GitHub Actions**: build + testes + lint a cada push/PR.

---

## 5. Design

- **Fonte da verdade visual:** as telas desenhadas no Claude Design (https://claude.ai/artifact/FCk2g4sxMAj8zyKA7soACw), feitas com o design system **divvup** (https://claude.ai/artifact/UjULqNSrf5z5nr8z4KdEQq). A cópia versionada, com PNGs, medidas, tokens, ícones e o mapa de telas, está em [docs/design/](docs/design/README.md); o app é implementado fielmente a partir dela.
- **Isto substitui a proposta de cores original (menta/coral).** Marca: ultramarino `#2F2BD9` + lima ácida `#C9F24D` sobre papel frio `#F5F5F0`; escuro em `#0C0F18`.
  - Swipe **aceitar** = `accent` (lima) com `on-accent`; swipe **passar** = `danger`.
  - Lima nunca como texto/ícone sobre fundo claro.
- Tipografia única: **Bricolage Grotesque** (eixos `opsz`, `wdth`, `wght`). Pontos com números tabulares (`tnum`).
- Cantos: `radius-sm` 4px (controles), `radius-md` 12px (cartões), `radius-pill` (chips, selos, avatares). Bordas de 1px no lugar de sombras. Sem gradientes. Espaçamento em passos de 4px.
- Títulos em minúsculas; rótulos em caixa-alta espaçada; voz "você", imperativo do dia a dia; **sem emoji na interface**; números no formato brasileiro ("1.240 pts").
- Único movimento de marca: deslizar lateral do cartão. Respeitar "remover animações" do sistema.
- `:core:designsystem` traduz os tokens em `DivvupTheme` (cores claro/escuro, tipografia, formas, espaçamentos) e os componentes base que as telas usam (inventário em [docs/design/README.md](docs/design/README.md#componentes-que-as-telas-usam)). Nenhuma tela usa cor ou medida literal.
- Contraste mínimo 4,5:1 para texto nos dois temas.

---

## 6. Inventário de telas

> **Especificação detalhada de cada tela** (conteúdo, ações, estados, navegação, componentes e ícones): **[TELAS.md](TELAS.md)**. A tabela abaixo é o resumo; em caso de diferença, vale o TELAS.md.

Cada tela deve ter os estados **carregando, vazio, erro e sem permissão** (quando aplicável).

### v1.0
| # | Tela | Notas |
|---|---|---|
| 1 | Login / Cadastro | e-mail/senha, Google, recuperar senha |
| 2 | Onboarding: nome + avatar | galeria DiceBear |
| 3 | Onboarding: criar casa ou "tenho um código" | criar pede nome e tipo |
| 4 | Onboarding: tutorial do swipe | 3 cards fictícios: direita, esquerda, desfazer |
| 5 | Convide seus moradores | após criar casa; código gerado + share |
| 6 | **Deck (principal)** | swipe, desfazer, sino, banner de convite se sozinho, "rever passadas" |
| 7 | Nova tarefa | título, descrição, categoria, peso, prazo |
| 8 | Detalhe da tarefa | ações: concluir, devolver, editar, cancelar (conforme regra) |
| 9 | Minhas tarefas | assumidas, atrasadas, contador x/5 |
| 10 | Ranking + Histórico | abas mensal/geral; feed de atividade |
| 11 | Central de notificações | |
| 12 | Perfil | conta, estatísticas, medalhas, sequência |
| 13 | Preferências de notificação | tipos + horário silencioso |
| 14 | Galeria de avatar | |
| 15 | Gerenciar casa | membros, papéis, convites, sair da casa |
| 16 | Excluir conta | exigência da Play Store |

### v1.1
| # | Tela | Notas |
|---|---|---|
| 17 | Buscar casas (Lista ↔ Mapa) | busca por local, filtros |
| 18 | Perfil público da casa | fotos, vagas, índice, solicitar, denunciar |
| 19 | Configurar anúncio | vagas, endereço, valor, fotos, termo |
| 20 | Solicitações recebidas (admin) | aprovar/recusar |
| 21 | Minhas solicitações | status |
| 22 | Denunciar | motivo |

### Onboarding (fluxo)
Login → nome + avatar → criar casa / tenho um código (v1.1: procurar casas) → tutorial do swipe → permissão de notificação → (se criou) convide seus moradores → Deck.

---

## 7. Roadmap

| Marco | Conteúdo | Pronto quando |
|---|---|---|
| **M0 · Fundação** | projeto multi-módulo, `:core:designsystem`, Firebase dev/prod (Spark), backend em Workers, emuladores, CI | app vazio com tema roda nos dois temas; CI verde |
| **M1 · Conta e casa** | auth, perfil + avatar, criar casa, convites, entrar com código, papéis, sair | 2 aparelhos na mesma casa |
| **M2 · Tarefas e Match** | criar tarefa, deck, passe, desfazer, devolver, limite 5, concluir + pontos | swipe em um aparelho some no outro |
| **M3 · Justiça** | temporadas, ranking mensal/geral, histórico, atribuição automática, atraso e meia pontuação | regras cobertas por testes no Emulator |
| **M4 · Engajamento** | FCM, central, preferências, horário silencioso, medalhas, sequência | pushes de todos os eventos chegando |
| **M5 · Lançamento** | tutorial, exclusão de conta, política de privacidade e página de exclusão em `divvup.app.br`, Data Safety, teste fechado (12 testadores × 14 dias), publicação | app em produção |
| **v1.1 · Busca e mapa** | anúncio, busca lista/mapa, índice de equilíbrio, solicitações, denúncia | estudante encontra e entra numa república |
| **Futuro** | recorrência, rankings por bairro/cidade, editor de avatar, avatares desbloqueáveis, monetização | — |

> Recrutar os 12 testadores do teste fechado já durante o M3.

---

## 8. Conformidade (Play Store / LGPD)

- Política de privacidade e termos publicados em `divvup.app.br` (enquanto o domínio não for registrado, no endereço gratuito `*.pages.dev` do Cloudflare Pages).
- Exclusão de conta **no app** e por **página web**; apaga dados pessoais e anonimiza a participação no histórico das casas.
- Formulário de Segurança de Dados (Data Safety) preenchido conforme Firebase + localização (v1.1).
- Localização exata de casas nunca é pública (R-40); anúncio exige termo aceito (R-39).
- Conta pessoal de desenvolvedor nova: teste fechado obrigatório antes da produção.
- Atribuição de licenças: DiceBear (MIT), estilo avataaars (Pablo Stanley, uso comercial livre), Bricolage Grotesque (OFL-1.1).

---

## 9. Fora de escopo (v1.0)
Recorrência · verificação de conclusão por foto · múltiplas casas por usuário · categorias personalizadas · rankings entre casas · monetização · iOS/web · outros idiomas.
