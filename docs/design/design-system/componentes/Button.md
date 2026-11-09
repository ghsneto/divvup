# Button

Botão de ação com cantos quase retos (`radius-sm`) e texto em Bricolage 600.

- **primary** (ultramarino): a ação principal da tela, uma por vista. “Criar minha casa”.
- **accent** (lima): só para confirmar um swipe ou resgatar pontos; texto em `on-accent`.
- **secondary** (contorno em tinta) e **ghost**: ações de apoio.
- Verbo primeiro, como se fala: “Criar minha casa”, “Passar a vez”, “Aceitar tarefa”. Sem ponto final, sem caixa-alta.
- Altura mínima 44px (`md`); `sm` (32px) só em listas densas.

O consumidor fornece `children` (o rótulo), `onClick` ou `href`.
