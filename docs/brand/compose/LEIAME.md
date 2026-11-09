# Tema Jetpack Compose do Divvup

Direção **Meio a Meio**. As cores vêm de `../tokens/tokens.css`.

Estes arquivos são a referência do tema. O lugar deles no app é o módulo `:core:designsystem`
(veja o [CLAUDE.md](../../../CLAUDE.md)).

## Como levar para o app

1. Copie `Color.kt`, `Type.kt` e `Theme.kt` para
   `core/designsystem/src/main/java/br/app/divvup/core/designsystem/theme/`.
   O pacote já é `br.app.divvup.core.designsystem.theme`.
2. Baixe a Bricolage Grotesque em https://fonts.google.com/specimen/Bricolage+Grotesque.
   No zip, pegue o arquivo variável `BricolageGrotesque-VariableFont_opsz,wdth,wght.ttf`,
   renomeie para `bricolage_grotesque.ttf` e coloque em `core/designsystem/src/main/res/font/`.
3. Os eixos variáveis da fonte precisam de `minSdk = 26` (já é o do projeto).
4. Envolva o app no tema:

```kotlin
setContent {
    DivvupTheme {
        // telas do app
    }
}
```

## Cores do swipe

```kotlin
val aceitar = DivvupTheme.colors.aceitar   // swipe à direita (Lima Ácida)
val passar = DivvupTheme.colors.passar     // swipe à esquerda (danger)
```

- A lima é para preenchimentos (fundo do cartão ao arrastar, selo de pontos), com `onAceitar` por cima.
  Nunca use a lima como texto ou ícone sobre fundo claro.
- Para pontos e ranking, use `PontosStyle`: os números têm a mesma largura e o placar não pula.

## Ícone do app

`../logo/app-icon-512.svg` e `../logo/symbol-only.svg` podem virar o ícone adaptativo:
Android Studio → File → New → Image Asset → Launcher Icons (Adaptive) → escolha o SVG do símbolo.
