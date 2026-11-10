# Divvup — Especificação de Telas

> Complementa a seção 6 do [SPEC.md](SPEC.md). Regras citadas por ID (`R-xx`) estão no SPEC.
> Feito para ser usado como briefing no Claude Design (design system **divvup**) e como referência de implementação em Compose.
> **O desenho de cada tela** (PNG claro/escuro, HTML com medidas exatas) está em [docs/design/](docs/design/README.md); o mapa código → artboard fica lá. Este arquivo descreve comportamento, estados e navegação; o visual vem do desenho.

## Convenções gerais

- **Formato:** mobile Android, 360×800 dp como referência; layouts devem funcionar de 320 a 480 dp de largura.
- **Temas:** toda tela em **claro e escuro**.
- **Estados obrigatórios** (quando aplicável): `carregando`, `vazio`, `erro` (com "Tentar de novo"), `sem conexão`, `sem permissão`.
- **Texto:** títulos em minúsculas, "você", imperativo do dia a dia, sem emoji, números pt-BR ("1.240 pts"), datas relativas ("vence amanhã", "há 2 h").
- **Feedback de ação:** snackbar no rodapé (com ação quando houver: "Desfazer", "Tentar de novo").
- **Confirmação destrutiva:** diálogo com o verbo da ação no botão ("Sair da casa", não "OK").
- **Toque mínimo:** 48 dp.

## Navegação

Usuário **sem casa** → fluxo de Onboarding (seção A/B). Nenhuma barra inferior.

Usuário **com casa** → estrutura principal:

```
Barra superior: [avatar da casa / nome da casa]                  [sino com contador]
Conteúdo da aba
Barra inferior: Deck · Minhas tarefas · Ranking · Perfil
FAB "+" (nova tarefa): visível em Deck e Minhas tarefas
```

| Aba | Tela |
|---|---|
| Deck | C1 |
| Minhas tarefas | C5 |
| Ranking | D1 |
| Perfil | E1 |

Mapa de navegação resumido:

```
A1 Splash ─┬─ sem sessão ─→ A2 Boas-vindas ─→ A3 Entrar / A4 Criar conta ─→ A5 Recuperar senha
           ├─ sem casa   ─→ B1 Nome+avatar ─→ B2 Criar ou entrar ─┬→ B3 Criar casa ─→ B6 Tutorial ─→ B7 Permissão ─→ B8 Convide ─→ C1
           │                                                     └→ B4 Entrar com código ─→ B5 Bem-vindo à casa ─→ B6 ─→ B7 ─→ C1
           └─ com casa   ─→ C1 Deck
C1 ─→ C2 Nova tarefa · C3 Detalhe · F1 Notificações
C5 ─→ C3 Detalhe ─→ C4 Concluída (celebração)
D1 ─→ D2 Temporada anterior · D3 Membro
E1 ─→ E2 Editar perfil · E3 Avatar · E4 Notificações (prefs) · E5 Aparência · G1 Gerenciar casa · E6 Sobre · E7 Excluir conta
G1 ─→ G2 Convites · G3 Membro (admin) · G4 Editar casa · (v1.1) H3 Anúncio · H4 Solicitações
```

---

## A. Acesso

### A1 · Splash
- **Objetivo:** carregar sessão e decidir destino.
- **Conteúdo:** logo (símbolo + logotipo) centralizado sobre `background`.
- **Regra:** usa a Splash Screen API do Android; sem animação além da padrão.
- **Estados:** sem conexão na abertura → segue com cache; se não houver cache e não houver sessão, vai para A2.

### A2 · Boas-vindas
- **Objetivo:** apresentar o app em uma tela.
- **Conteúdo:** logo; título "a casa dividida no swipe"; subtítulo curto (ex.: "Arrasta pra direita, a tarefa é sua. Arrasta pra esquerda, passa a vez."); ilustração do cartão partido.
- **Ações:** "Criar conta" (primário) · "Já tenho conta" (secundário) · "Continuar com Google".
- **Rodapé:** "Ao continuar, você aceita os Termos e a Política de Privacidade" (links).

### A3 · Entrar
- **Campos:** e-mail, senha (mostrar/ocultar).
- **Ações:** "Entrar" · "Continuar com Google" · "Esqueci a senha" → A5 · "Criar conta" → A4.
- **Erros inline:** e-mail inválido; senha incorreta; conta não encontrada; muitas tentativas.
- **Estados:** botão em carregamento; sem conexão.

### A4 · Criar conta
- **Campos:** nome, e-mail, senha (mín. 8 caracteres, indicador de força), confirmar senha.
- **Ações:** "Criar conta" · "Continuar com Google" · "Já tenho conta".
- **Checkbox obrigatório:** aceite de Termos e Política (links).
- **Erros inline:** e-mail já cadastrado; senhas diferentes; senha fraca.

### A5 · Recuperar senha
- **Campo:** e-mail. **Ação:** "Enviar link".
- **Sucesso:** estado de confirmação "Mandamos um link para {e-mail}" + "Voltar para entrar". Mesma mensagem mesmo se o e-mail não existir (não revelar contas).

---

## B. Onboarding (primeira vez / sem casa)

Indicador de progresso discreto no topo (passos B1 → B2 → B3/B4 → B6 → B7).

### B1 · Como te chamam
- **Campos:** nome de exibição (pré-preenchido do cadastro/Google).
- **Avatar:** avatar atual grande + "Escolher avatar" → abre E3 em modo onboarding. O link "Usar foto" do desenho fica escondido na v1.0 (ADR 0003).
- **Ação:** "Continuar".

### B2 · Sua casa
- **Objetivo:** escolher como entrar numa casa.
- **Opções em cartões grandes:**
  - "Criar uma casa" → B3
  - "Tenho um código" → B4
  - (v1.1) "Procurar uma vaga" → H1
- **Rodapé:** "Sair da conta".

### B3 · Criar casa
- **Campos:** nome da casa; **tipo** (seleção única em chips com ícone e descrição curta): República · Moradia compartilhada · Casal/Família.
- **v1.1:** se tipo = República, aparece bloco "Anunciar vagas" **pré-marcado** com texto explicando que a casa aparecerá na busca depois de preencher vagas e endereço (R-38, R-39). Pode desmarcar.
- **Ação:** "Criar casa". Em seguida → B6.
- **Erros:** nome vazio; falha de rede.

### B4 · Entrar com código
- **Campo:** 6 caracteres em caixas separadas, maiúsculas automáticas, colar do clipboard.
- **Ação:** "Entrar na casa".
- **Erros:** código inválido; código expirado ("Peça um novo código para quem te convidou"); código já usado; você já está numa casa (oferecer "Sair da casa atual").

### B5 · Bem-vindo à casa
- **Conteúdo:** nome da casa, tipo, avatares dos membros, quantas tarefas abertas existem.
- **Ação:** "Bora" → B6.

### B6 · Tutorial do swipe
- **Objetivo:** ensinar os três gestos com cards fictícios (não gravam nada).
- **Passo 1:** card "Lavar a louça · 50 pts" + dica "Arrasta pra direita: a tarefa é sua" (lima aparece ao arrastar).
- **Passo 2:** card "Tirar o lixo · 25 pts" + "Arrasta pra esquerda: passa a vez" (danger ao arrastar).
- **Passo 3:** após o swipe, destaque no botão "Desfazer" (3 s) + "Errou? Desfaz em até 3 segundos."
- **Ações:** "Pular" (sempre visível) · avança sozinho ao completar cada gesto.
- **Acessibilidade:** botões "Assumir" e "Passar" alternativos ao gesto.

### B7 · Permissão de notificação
- **Conteúdo:** explicação do valor ("Te avisamos quando uma tarefa for sua ou estiver perto do prazo").
- **Ações:** "Ativar notificações" (abre o diálogo do sistema, Android 13+) · "Agora não".
- Só aparece se a permissão ainda não foi concedida/negada definitivamente (R-36).

### B8 · Convide seus moradores
- Aparece só para quem **criou** a casa.
- **Conteúdo:** código grande gerado na hora, validade "vale por 24 h · uso único" (R-06), contagem regressiva.
- **Ações:** "Compartilhar código" (share sheet) · "Copiar" · "Gerar outro código" · "Fazer isso depois" → C1.

---

## C. Tarefas e Match

### C1 · Deck (tela principal)
- **Topo:** nome da casa, sino com contador de não lidas → F1.
- **Faixa de status (opcional, uma linha):** "Seus pontos no mês: 325 pts · 3/5 tarefas".
- **Banner condicional:** se a pessoa está sozinha na casa → "Sua casa está vazia. Convide alguém" → B8/G2.
- **Pilha de cards** (o card do topo interativo, 1–2 cards visíveis atrás):
  - **TaskCard:** ícone + rótulo da categoria (`label`), título (`h3`), descrição curta (2 linhas), prazo relativo ("vence amanhã às 18h"), `PointsBadge` (25/50/100), autor ("criada por Ana").
  - **Ao arrastar à direita:** overlay lima + "É MINHA"; à esquerda: overlay danger + "PASSO".
  - Card com prazo < 24 h: selo "urgente" (warning).
- **Botões abaixo da pilha:** "Passar" (danger, ícone ✕ em traço) · "Assumir" (accent, ícone ✓). Alternativa acessível ao gesto.
- **Snackbar após swipe:** "Tarefa assumida" / "Você passou" + **"Desfazer"** por 3 s (R-18).
- **FAB "+"** → C2.
- **Estados:**
  - `carregando`: skeleton do card.
  - `vazio — nada pendente`: "Nada pendente por aqui" + "Criar tarefa".
  - `vazio — tudo passado`: "Você passou {X} tarefas" + "Rever" (R-17).
  - `limite atingido` (R-20): pilha desabilitada + "Você já tem 5 tarefas abertas. Conclua uma para pegar outra." + "Ver minhas tarefas".
  - `conflito` (R-15): card some com aviso "Alguém já pegou essa".
  - `sem conexão`: cards em cache visíveis, swipe desabilitado com aviso.
  - `erro`.

### C2 · Nova tarefa / Editar tarefa
- **Campos:**
  - Título (obrigatório, até 60 caracteres).
  - Descrição (opcional, até 280).
  - **Categoria:** grade de 10 chips com ícone (R-11).
  - **Peso:** seletor de 3 opções mostrando pontos — Leve 25 · Média 50 · Pesada 100 (R-10).
  - **Prazo:** data + hora (padrão: amanhã 20h). Não aceita passado. Aviso se < 24 h: "Com menos de 24 h, a tarefa pode ser atribuída automaticamente" (R-26).
  - (Futuro, oculto no v1.0) Repetir.
- **Ação:** "Criar tarefa" / "Salvar".
- **Modo edição:** só para autor/admin e tarefa sem dono (R-12); com botão "Excluir tarefa" (confirmação).
- **Saída com alterações:** diálogo "Descartar alterações?".

### C3 · Detalhe da tarefa
- **Conteúdo:** categoria, título, descrição completa, pontos, prazo absoluto + relativo, status (chip: Aberta / Sua / De {nome} / Atrasada / Concluída / Cancelada), dono com avatar, como foi atribuída ("pelo swipe" / "automaticamente"), criada por e quando.
- **Ações por contexto:**

| Situação | Ações |
|---|---|
| Aberta | Assumir · Editar e Excluir (autor/admin) |
| Sua, > 24 h do prazo | **Concluir** (primário) · Devolver ao deck (R-19) |
| Sua, ≤ 24 h do prazo | **Concluir** · aviso "Não dá mais para devolver" |
| Sua, atrasada | **Concluir** · aviso "Concluindo agora você ganha {metade} pts" (R-22) |
| De outra pessoa | só leitura · Cancelar (admin, com confirmação) |
| Concluída | só leitura, mostra pontos ganhos e data |

### C4 · Tarefa concluída (celebração)
- Tela/folha cheia após "Concluir".
- **Conteúdo:** "+50 pts" grande (`PointsBadge` ampliado), nova posição no ranking do mês ("você subiu para 2º"), sequência diária atualizada ("4 dias seguidos").
- **Se desbloqueou medalha:** card da medalha com nome e descrição (R-30).
- **Ações:** "Continuar" · "Pegar outra tarefa" → C1.
- Respeitar "remover animações".

### C5 · Minhas tarefas
- **Contador no topo:** "3 de 5 tarefas abertas".
- **Seções:** Atrasadas (danger) · Para hoje · Próximas · Concluídas recentemente (últimos 7 dias, recolhível).
- **Item de lista:** ícone da categoria, título, prazo relativo, pontos, ação rápida "Concluir".
- Item atribuído automaticamente mostra selo "atribuída a você".
- **Estados:** `vazio`: "Você não tem tarefas" + "Ir para o deck".

---

## D. Ranking e histórico

### D1 · Ranking
- **Abas:** "Este mês" · "Geral" (R-27, R-28).
- **Este mês:**
  - Cabeçalho: nome da temporada ("novembro"), dias restantes ("faltam 12 dias").
  - **Pódio** com os 3 primeiros (avatar, nome, pontos `tnum`).
  - Lista completa: posição, avatar, nome, pontos, nº de tarefas; linha do usuário destacada.
  - **Índice de Equilíbrio da casa** (0–100) com frase interpretativa ("bem dividido" / "desequilibrado") e explicação em "?" (R-45).
  - Link "Temporadas anteriores" → D2.
- **Geral:** mesma lista com pontos acumulados; ex-moradores com rótulo "ex-morador" (R-29).
- **Histórico (seção abaixo ou terceira aba "Histórico"):** feed de atividade — "Ana concluiu Lavar a louça · +50 pts · há 2 h", "Bruno assumiu Tirar o lixo", "Atribuída automaticamente a Caio", "Davi entrou na casa". Filtro por membro e por categoria.
- **Estados:** `vazio`: "Ninguém pontuou ainda neste mês".

### D2 · Temporadas anteriores
- Lista de meses com campeão (avatar + nome + pontos). Toque → ranking final congelado daquele mês.

### D3 · Perfil de membro (visão de morador)
- Avatar, nome, papel (Admin/Morador), membro desde, pontos do mês e totais, tarefas por categoria (gráfico de barras simples), medalhas, tarefas abertas dele.
- Sem ações administrativas (essas ficam em G3).

---

## E. Perfil e configurações

### E1 · Perfil
- **Cabeçalho:** avatar grande, nome, e-mail, botão "Editar" → E2.
- **Estatísticas:** pontos do mês, pontos totais, tarefas concluídas, sequência atual e melhor.
- **Medalhas:** grade com conquistadas (coloridas) e bloqueadas (cinza, com critério ao tocar).
- **Lista de configurações:**
  - Minha casa → G1
  - Notificações → E4
  - Aparência → E5
  - Sobre o Divvup → E6
  - Sair da conta (confirmação)
  - Excluir conta → E7

### E2 · Editar perfil
- Nome, avatar (→ E3). Foto (adicionar/remover) fica fora da v1.0 (ADR 0003). E-mail somente leitura. "Alterar senha" (só conta com e-mail).

### E3 · Galeria de avatar
- Grade de ~24 avatares avataaars (R-32), item selecionado destacado com borda `primary`.
- "Usar uma foto" e "Remover foto" ficam escondidos na v1.0 (ADR 0003).
- **Ação:** "Salvar".

### E4 · Preferências de notificação
- Interruptor por tipo (tabela R-33), agrupados: "Suas tarefas" · "Sua casa" · "Temporada".
- **Horário silencioso:** interruptor + início/fim (padrão 22:00–08:00) + nota "Lembretes de 2 h ainda chegam" (R-35).
- Se a permissão do sistema está negada: banner "As notificações estão desligadas no Android" + "Abrir configurações".

### E5 · Aparência
- Tema: Seguir o sistema · Claro · Escuro.

### E6 · Sobre o Divvup
- Versão, Termos de uso, Política de privacidade, Licenças de código aberto (DiceBear, avataaars, Bricolage Grotesque), Fale com a gente (e-mail).

### E7 · Excluir conta
- **Passo 1:** o que acontece — dados pessoais apagados; participação no histórico das casas fica anônima; ação irreversível.
- Se é o **único admin** de uma casa com outros membros: aviso de que o membro mais antigo vira admin (R-04).
- **Passo 2:** reautenticação (senha ou Google) + digitar "EXCLUIR".
- **Ação:** "Excluir minha conta" (danger). Sucesso → A2 com mensagem.

---

## F. Notificações

### F1 · Central de notificações
- Lista cronológica agrupada (Hoje · Ontem · Esta semana · Mais antigas), 30 dias (R-37).
- **Item:** ícone do tipo, texto, tempo relativo, ponto de não lida. Toque → destino (ex.: C3 da tarefa).
- **Ação no topo:** "Marcar todas como lidas".
- **Estados:** `vazio`: "Nenhuma notificação por aqui".

---

## G. Gestão da casa

### G1 · Minha casa
- **Cabeçalho:** nome, tipo, nº de membros, temporada atual.
- **Membros:** lista com avatar, nome, papel, pontos do mês. Toque → D3 (morador) ou G3 (se admin).
- **Admin:** "Convidar moradores" → G2 · "Editar casa" → G4 · (v1.1) "Anúncio de vagas" → H3 · (v1.1) "Solicitações" com contador → H4.
- **Todos:** "Sair da casa" (danger, confirmação explicando R-29: tarefas assumidas voltam ao deck).

### G2 · Convites (admin)
- **Ação principal:** "Gerar código" → mostra código + compartilhar/copiar.
- **Lista de códigos:** código, criado por, expira em (contagem), status (ativo / usado por {nome} / expirado / revogado). Ação "Revogar" nos ativos (R-06).

### G3 · Membro (visão de admin)
- Tudo de D3 + ações: "Tornar admin" / "Remover admin" · "Remover da casa" (danger, confirmação: "As tarefas dele voltam para o deck").
- Não é possível remover a si mesmo por aqui (usar "Sair da casa").

### G4 · Editar casa (admin)
- Nome, tipo, cidade, bairro, rua. Ação "Salvar".
- Mudar tipo para Casal/Família com anúncio ativo → aviso de que o anúncio será desligado.

---

## H. Busca de casas e anúncio (v1.1)

### H1 · Procurar uma vaga
- **Barra de busca:** "Cidade, bairro ou faculdade" com autocomplete (R-41).
- **Alternância:** Lista ↔ Mapa (R-42).
- **Filtros (folha inferior):** só com vagas · faixa de valor · tipo. **Ordenar:** distância.
- **Lista:** cards com foto de capa, nome, tipo, "Rua X · Bairro", distância ("1,2 km do ponto buscado"), vagas ("2 vagas"), valor ("R$ 750/mês", se houver), Índice de Equilíbrio, selo de atividade.
- **Mapa:** satélite/híbrido, pins aproximados (R-40); toque no pin → mini-card → H2.
- **Estados:** `antes de buscar` (sugestão de digitar um local) · `vazio` ("Nenhuma casa com vagas por aqui") · `sem conexão`.
- Acessível sem casa (R-44).

### H2 · Perfil público da casa
- Galeria de fotos, nome, tipo, rua + bairro (sem número), mapa pequeno com área aproximada, vagas, valor, descrição, Índice de Equilíbrio, atividade, nº de moradores.
- **Ações:** "Solicitar vaga" → folha com mensagem opcional → enviar · "Denunciar" → H6.
- Se já solicitou: estado "Solicitação enviada · aguardando".

### H3 · Anúncio de vagas (admin)
- Interruptor "Anunciar vagas" (R-38).
- **Campos:** nº de vagas, valor mensal (opcional), descrição, fotos (até 6), endereço completo (CEP, rua, número, complemento, bairro, cidade) com nota "O número só aparece para quem você aprovar" (R-40).
- **Termo de exposição** (checkbox obrigatório para ativar) (R-39).
- **Prévia:** "Como sua casa aparece na busca" (card H1).

### H4 · Solicitações recebidas (admin)
- Lista: avatar, nome, mensagem, data. Ações "Aprovar" / "Recusar" (confirmação).
- Aprovar com casa sem vagas → aviso.

### H5 · Minhas solicitações
- Lista das casas solicitadas com status (aguardando / aprovada / recusada). Aprovada → mostra endereço completo e "Entrar na casa".

### H6 · Denunciar casa
- Motivos (seleção única): informação falsa · conteúdo impróprio · golpe · outro (com texto). Ação "Enviar denúncia".

---

## I. Componentes e elementos transversais

Para o Claude Design montar junto com as telas:

| Elemento | Onde aparece |
|---|---|
| `TaskCard` (deck) + variante de lista | C1, C5, D1 |
| `PointsBadge` (25/50/100, tamanho normal e ampliado) | C1, C3, C4 |
| Chip de categoria (10 ícones de traço) | C1, C2, C3 |
| Chip de status da tarefa (Aberta, Sua, Atrasada, Concluída, Cancelada, Atribuída a você) | C3, C5 |
| Avatar (tamanhos 24/40/64/96, com e sem foto) | todas |
| Linha de ranking + pódio | D1 |
| Medalha (conquistada / bloqueada) — 5 medalhas (R-30) | C4, E1 |
| Indicador de sequência diária | C4, E1 |
| Medidor do Índice de Equilíbrio | D1, H1, H2 |
| Snackbar com "Desfazer" | C1 |
| Diálogo de confirmação destrutiva | vários |
| Barra inferior (4 abas) + FAB | estrutura principal |
| Campo de código de 6 dígitos | B4 |
| Estados vazio / erro / sem conexão (ilustração + texto + ação) | todas |

## J. Ícones necessários

Traço simples, ponta reta, cor do texto (sem emoji):
- **Categorias:** cozinha, limpeza geral, banheiro, lavanderia, lixo, compras, pets, plantas, manutenção, outros.
- **Ações/navegação:** deck (cartões), minhas tarefas (checklist), ranking (pódio), perfil, sino, mais, assumir (✓), passar (✕), desfazer, compartilhar, copiar, editar, excluir, sair, configurações, filtro, mapa, lista, localização, câmera, cadeado.
