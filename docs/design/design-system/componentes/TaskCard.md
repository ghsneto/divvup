# TaskCard

O cartão de tarefa que se divide no swipe: arrasta pra direita, a tarefa é sua; arrasta pra esquerda, passa a vez. É a ideia central da marca.

- Arrastar além de 96px confirma; menos que isso volta ao lugar. Setas ← e → fazem o mesmo pelo teclado.
- **aceitar**: o cartão se enche de `accent` (lima) com texto `on-accent`, e o selo de pontos inverte.
- **passar**: contorno em `danger` e a palavra “passa a vez”. Nunca só a cor.
- Título em display, minúsculas, curto: “lavar a louça”, “tirar o lixo”. Cômodo em rótulo caixa-alta.
- Um cartão por vez no centro da tela; atrás dele, o trilho mostra “← passar” e “aceitar →”.

O consumidor fornece `title`, `room`, `due`, `points` e os callbacks `onAceitar` / `onPassar`. `state` força o visual em mockups.
