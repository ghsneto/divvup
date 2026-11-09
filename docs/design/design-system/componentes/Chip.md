# Chip

Pílula com contorno em tinta, como as palavras de “sensação” no manual. Serve para filtros (cômodo, morador) e etiquetas.

- Sem `selected`/`onToggle` é uma etiqueta estática.
- Com eles, vira botão alternável: selecionado inverte para fundo `text` e texto `background`.
- Texto curto, minúsculo quando for palavra solta: “cozinha”, “banheiro”.

O consumidor fornece `children` e, se alternável, `selected` e `onToggle`.
