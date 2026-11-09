divvup é um app de tarefas domésticas: **a casa dividida no swipe**. Arrasta pra direita, a louça é sua; arrasta pra esquerda, passa a vez. A marca é um cartão de tarefa cortado na diagonal, com uma metade já deslizando para a direita. Dividir é um gesto.

Sensação: **ousado, jovem, nítido**. Mais brincalhão que sério, mais discreto que barulhento, no meio entre quente e frio. Ultramarino escuro com lima ácida e uma grotesca de caráter, sem cantos fofos. É o oposto dos concorrentes de azul-céu e fonte arredondada.

## Voz e texto

- Português do Brasil, falado e direto. Trate a pessoa por “você”; a casa é “sua casa”.
- Frases curtas no imperativo do dia a dia: “Arrasta pra direita”, “Passa a vez”, “Criar minha casa”.
- Títulos em **minúsculas** (“a casa dividida no swipe”, “em uso”). Texto corrido e botões como escritos, sem caixa-alta.
- Rótulos acima de blocos em caixa-alta espaçada (`label`): “ONDE SE POSICIONA”, “COZINHA”.
- Nada de emoji na interface. Números em formato brasileiro: “1.240 pts”.

## Cor

“Ultramarino profundo com um corte de lima ácida, sobre papel frio.”

- Áreas grandes ficam com `background` e `primary`; o destaque (`accent`) fica pequeno.
- Texto em `text` sobre `background`, `surface` e `surface-alt`; secundário em `text-muted`.
- `primary` é o ultramarino: botão principal, link de navegação ativo, o cartão do logo. No escuro ele clareia (#7590FD) e o texto por cima vira tinta (`on-primary`).
- `ultramarino` e `lima-acida` são as cores exatas da marca, iguais nos dois temas: use em capas, cartão de visita, avatar e ícone do app.
- **Nunca use lima como texto ou ícone sobre fundo claro.** Ela é preenchimento: swipe “aceitar”, selo de pontos, um traço sob uma imagem, sempre com `on-accent` por cima.
- Tons 80/50/20% das escalas só em gráficos e fundos, nunca em texto.
- Estados (`success`, `warning`, `danger`, `info`) sempre vêm com palavra ou ícone. `danger` também é o “passar” do swipe.
- Links em texto corrido: `link` (oliva no claro, ultramarino claro no escuro).
- Para impressão, peça à gráfica equivalências CMYK/Pantone; os valores de tela são exatos.

## Tipografia

Uma família só: **Bricolage Grotesque** (Google Fonts, OFL-1.1), com eixos variáveis.

- Títulos (`h1` 64px, `h2` 40px): peso 800, `opsz` 96, `wdth` 90, espaçamento −0,03em, minúsculas. A grotesca de ombros largos dá o tom de pôster.
- Texto (`h3` 28px/600, `body` 18px/400, `small` 14px/400): `opsz` 14, `wdth` 100.
- Pontos e ranking em `pontos` (20px/600) com `tnum`: os números têm a mesma largura e o placar não pula.
- Carregue com `https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wdth,wght@12..96,75..100,200..800&display=swap`. Reserva: `system-ui, sans-serif`.
- Para largura e tamanho óptico, use `font-variation-settings: "opsz" 96, "wdth" 90` nos títulos.

## Logo

- O símbolo + logotipo é o logo principal; todas as outras versões derivam dele. Escolha a versão pelo espaço, não pelo gosto.
- Abaixo de 20px de altura na tela (7mm impresso), use só o símbolo. Favicon a 48, 32 e 16px.
- Área de proteção: a altura das maiúsculas do logotipo livre em todos os lados.
- Cada parte mantém ao menos 3:1 contra o fundo; onde não dá, o logo vira uma cor. Combinações aprovadas: papel (colorido, 8,5:1), ultramarino (branco, 8,5:1), tinta (branco, 17,2:1), lima (tinta, 14,8:1).
- Não distorça, não troque cores, não aplique efeitos, não reorganize símbolo e nome, não feche o corte, não arraste a metade para a esquerda.
- Na interface use o componente `Logo` (vetores originais, cores do tema). Arquivos no grupo **Logos**.

## Forma e espaço

- Cantos quase retos nos controles (`radius-sm`, 4px); cartões com `radius-md` (12px), ecoando o cartão do símbolo; pílulas (`radius-pill`) só em chips, selos e avatares.
- Bordas de 1px em `border` em vez de sombras. Sem gradientes.
- Espaçamento em passos de 4px (`space-1` a `space-8`); margem de página no desktop `space-8` (80px), gutters `space-6` (32px).
- Layout editorial: rótulo pequeno em caixa-alta, título enorme em minúsculas, texto curto ao lado.
- Foco: anel de 2px sólido em `focus`, deslocado 2px.
- Movimento: o único gesto é o deslizar lateral do cartão. Respeite `prefers-reduced-motion`.

## Escuro

O tema escuro troca o papel por tinta (`background` #0C0F18), clareia o ultramarino e mantém a lima igual. Teste toda tela nos dois temas.

## Iconografia

O manual não traz um conjunto de ícones. Use ícones de traço simples com ponta reta, na cor do texto; nunca emoji. O único pictograma da marca é o cartão partido.

## Android (Jetpack Compose)

O manual inclui `Color.kt`, `Type.kt` e `Theme.kt` com `DivvupTheme`: as mesmas cores e estilos, `DivvupTheme.colors.aceitar` (lima) e `passar` (danger), e `PontosStyle` com tnum. Precisa de `minSdk 26` para os eixos variáveis.
