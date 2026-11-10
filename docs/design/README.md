# Design do Divvup — referência para implementação

Cópia versionada do que foi desenhado no Claude Design. **É daqui que a UI é implementada.**
O app tem que ficar igual a estes desenhos: layout, medidas, cores, texto, ícones, estados e tema escuro.

| Fonte | Link original | Cópia aqui |
|---|---|---|
| Telas do app (canvas, 58 artboards) | https://claude.ai/artifact/FCk2g4sxMAj8zyKA7soACw | [`fonte/`](fonte), [`png/`](png) |
| Protótipo navegável | https://claude.ai/artifact/JS5bWp4d6nNk2KMTDh4XRk (e o artboard `fonte/Prototipo.dc.html`) | — |
| Design system **divvup** | https://claude.ai/artifact/UjULqNSrf5z5nr8z4KdEQq | [`design-system/`](design-system) |

Copiado em 2026-11-09. Se o desenho mudar no Claude Design, atualize esta pasta **no mesmo PR** que muda o código.

## Ordem de precedência

Quando duas fontes discordam, vale a primeira da lista:

1. **As telas** (`fonte/*.dc.html` e a folha de estilo delas, [`assets/f25551cbab6b194e9c1439682cdfeafb.css`](assets/f25551cbab6b194e9c1439682cdfeafb.css), daqui em diante **estilo das telas**). Medidas, cores e textos exatos.
2. Os PNGs em [`png/`](png): a imagem que o app tem que reproduzir.
3. [TELAS.md](../../TELAS.md): comportamento, navegação e regras de cada tela (o que o desenho não mostra).
4. O design system ([`design-system/`](design-system)): tokens e regras de marca.
5. [SPEC.md](../../SPEC.md): regras de negócio. **Regra de negócio sempre vence o desenho**; se um desenho contradiz uma regra `R-xx`, pare e pergunte.

## Como ler uma tela

- **PNG** (`png/<Artboard>.png`, 720×1600 = 360×800 dp em 2x): a referência visual. `png/<Artboard>__escuro.png` é o tema escuro.
- **Fonte** (`fonte/<Artboard>.dc.html`): HTML com estilos inline. Os valores em px são **dp** (o artboard tem 360 px = 360 dp) e os tamanhos de fonte são **sp**. As classes (`btn`, `chip`, `st`, `pb`…) estão no estilo das telas.
- Textos com dados (nomes, números, datas) são exemplos; o resto do texto é a cópia final e vai para `strings.xml` exatamente como está.
- Estados interativos (sheet aberta, snackbar, diálogo) estão no `renderVals()` do script de cada artboard.

Para ver uma tela no navegador ou regenerar os PNGs:

```bash
node docs/design/ferramentas/renderizar.js --servir   # http://localhost:8765/Main.dc.html
node docs/design/ferramentas/renderizar.js Main       # regenera png/Main.png e png/Main__escuro.png
```

## Mapa: código da TELAS.md → artboards

| Código | Tela | Artboards (`fonte/` e `png/`) | Escuro |
|---|---|---|---|
| A1 | Splash | `Splash` | `Splash__escuro` |
| A2 | Boas-vindas | `BoasVindas` | `BoasVindasEscuro`, `BoasVindas__escuro` |
| A3 | Entrar (com erro) | `Entrar` | `Entrar__escuro` |
| A4 | Criar conta | `CriarConta` | `CriarConta__escuro` |
| A5 | Recuperar senha | `RecuperarSenha` | `RecuperarSenha__escuro` |
| B1 | Como te chamam | `OnbNome` | `OnbNome__escuro` |
| B2 | Sua casa | `OnbCasa` | `OnbCasa__escuro` |
| B3 | Criar casa | `CriarCasa` | `CriarCasa__escuro` |
| B4 | Entrar com código | `Codigo` | `Codigo__escuro` |
| B5 | Bem-vindo à casa | `BemVindo` | `BemVindo__escuro` |
| B6 | Tutorial do swipe | `Tutorial` | `Tutorial__escuro` |
| B7 | Permissão de notificação | `Permissao` | `Permissao__escuro` |
| B8 | Convide seus moradores | `Convide` | `Convide__escuro` |
| C1 | Deck | `Main` (normal), `DeckCarregando`, `DeckVazio`, `DeckPassadas`, `DeckLimite`, `DeckOffline`, `DeckErro` | `DeckEscuro`, `Main__escuro` |
| C2 | Nova tarefa | `NovaTarefa` | `NovaTarefaEscuro`, `NovaTarefa__escuro` |
| C3 | Detalhe da tarefa | `DetalheTarefa` (sua), `DetalheAberta`, `DetalheAtrasada` | `DetalheTarefa__escuro` |
| C4 | Tarefa concluída | `Concluida` | `ConcluidaEscuro`, `Concluida__escuro` |
| C5 | Minhas tarefas | `MinhasTarefas` | `MinhasTarefas__escuro` |
| D1 | Ranking | `Ranking` (este mês), `RankingGeral`, `Historico` | `RankingEscuro`, `Ranking__escuro` |
| D2 | Temporadas anteriores | `Temporadas` | `Temporadas__escuro` |
| D3 | Perfil de morador | `Membro` | `Membro__escuro` |
| E1 | Perfil | `Perfil` | `PerfilEscuro`, `Perfil__escuro` |
| E2 | Editar perfil | `EditarPerfil` | `EditarPerfil__escuro` |
| E3 | Galeria de avatar | `Avatar` | `Avatar__escuro` |
| E4 | Preferências de notificação | `PrefsNotif` | `PrefsNotif__escuro` |
| E5 | Aparência | `Aparencia` | `Aparencia__escuro` |
| E6 | Sobre o divvup | `Sobre` | `Sobre__escuro` |
| E7 | Excluir conta | `ExcluirConta` | `ExcluirConta__escuro` |
| F1 | Central de notificações | `Notificacoes` | `Notificacoes__escuro` |
| G1 | Minha casa | `MinhaCasa` | `MinhaCasa__escuro` |
| G2 | Convites | `Convites` | `Convites__escuro` |
| G3 | Membro (admin) | `MembroAdmin` | — (usa `Membro`) |
| G4 | Editar casa | `EditarCasa` | `EditarCasa__escuro` |
| H1 | Procurar vaga (v1.1) | `Buscar` (lista), `BuscarMapa` | `Buscar__escuro` |
| H2 | Perfil público da casa (v1.1) | `CasaPublica` | `CasaPublica__escuro` |
| H3 | Anúncio de vagas (v1.1) | `Anuncio` | `Anuncio__escuro` |
| H4 | Solicitações recebidas (v1.1) | `Solicitacoes` | `Solicitacoes__escuro` |
| H5 | Minhas solicitações (v1.1) | `MinhasSolicitacoes` | `MinhasSolicitacoes__escuro` |
| H6 | Denunciar casa (v1.1) | `Denunciar` | `Denunciar__escuro` |

Os `*Escuro` sem sublinhado foram desenhados à mão no canvas; os `*__escuro` são a mesma tela renderizada com `tema="escuro"`. Ambos valem.

## Tokens (vão para `DivvupTheme`)

Cores: o bloco `.dv` (claro) e `.dv.escuro` do estilo das telas. São as 16 cores semânticas do design system **mais** estas, usadas pelas telas e que o tema também precisa ter:

| Token | Claro | Escuro | Uso |
|---|---|---|---|
| `success-bg` | `#d6f8eb` | `#073025` | fundo de selo/aviso de sucesso |
| `warning-bg` | `#ffebd6` | `#3a2305` | fundo de selo/aviso de atenção |
| `danger-bg` | `#ffe9e6` | `#471713` | fundo de selo/aviso de erro |
| `info-bg` | `#e3f0ff` | `#0c2a46` | fundo de selo/aviso informativo |
| `primary-bg` | `#e9eeff` | `#182259` | fundo de selo primário |
| `on-danger` | `#ffffff` | `#0c0f18` | texto sobre `danger` (botão destrutivo) |
| `snack` / `snack-text` | `#181b26` / `#f2f3f5` | `#272b36` / `#f2f3f5` | snackbar |
| `scrim` | `rgba(12,15,24,.48)` | `rgba(0,0,0,.6)` | fundo atrás de sheet e diálogo |
| `ultramarino`, `lima`, `tinta` | `#2f2bd9`, `#c9f24d`, `#0c0f18` | iguais | cores fixas da marca |

Tipografia (escala das telas, igual ao `Type.kt` de `docs/brand/compose`). Fonte única **Bricolage Grotesque** variável:

| Estilo | Tamanho / altura | Peso | Eixos / extras |
|---|---|---|---|
| `h1` | 57 / 1.05 | 800 | `opsz` 96, `wdth` 90, letter-spacing −0,03em |
| `h2` | 45 / 1.07 | 800 | `opsz` 96, `wdth` 90, −0,03em |
| `h3` | 22 / 1.27 | 600 | |
| `ttl` | 16 / 1.5 | 600 | |
| `body` / `txt` | 16 / 1.5 | 400 | |
| `small` | 14 / 1.43 | 400 | |
| `label` | 12 / 1.33 | 600 | caixa-alta, letter-spacing 0,12em |
| `pts` | 20 / 1.2 | 600 | números tabulares |

Forma: controles `4dp`, cartões `12dp`, sheet `24dp` no topo, pílulas `999`. Bordas de 1dp, sem sombras. Espaço em passos de 4dp.

## Componentes que as telas usam

Inventário tirado do estilo das telas. É a lista de componentes de `:core:designsystem` (issue #2); medidas exatas estão na classe indicada.

| Componente | Classe | Variantes / medidas |
|---|---|---|
| Botão | `.btn` | `-p` primário, `-a` lima (aceitar), `-o` contorno, `-d` contorno perigo (passar), `-dd` perigo cheio, `-t` texto; `-w` largura total; mín. 48dp, raio 4, 14sp/600; desabilitado = opacidade 0,42 |
| Chip | `.chip` | 40dp, pílula, borda; `.on` inverte (fundo `text`, texto `background`) |
| Selo de status | `.st` | 24dp, pílula, `label`; `-ok`, `-warn`, `-bad`, `-info`, `-p`, `-n`; ícone 14dp |
| Selo de pontos | `.pb` | lima, 20sp/600 tnum, “pts” 12sp |
| Cartão | `.card` | `surface`, borda 1dp, raio 12 |
| Avatar | `.av` | círculo, fundo `surface-alt` |
| Campo de texto | `.in`, `.field`, `.errmsg` | 52dp, raio 4, 16sp; erro = borda `danger` + mensagem 14sp/600 com ícone 16dp |
| Toggle | `.tg` | 52×32, **quadrado** (raio 4), knob 24; ligado = `primary` |
| Barra superior | `.topbar`, `.iconbtn`, `.count` | 64dp; botão-ícone 48dp; contador lima 20dp |
| Barra inferior | `.tabbar`, `.tab` | 72dp, 4 abas; ativa = texto `text`, ícone `primary`, traço lima 32×4 no topo |
| FAB | `.fab` | 64dp **quadrado** (raio 4), `primary`, ícone 28dp, 16dp da direita, 88dp do fundo |
| Snackbar | `.snack` | 52dp, raio 4, `snack`; ação em lima; barra de tempo lima de 3dp |
| Bottom sheet | `.scrim`, `.sheet`, `.grab` | raio 24 no topo; alça 40×4 |
| Diálogo | `.dialog` | raio 12, padding 24, borda |
| Ícone | `.ic` | 24dp (`-s` 18, `-xs` 14, `-l` 32), cor do texto |
| Esqueleto / progresso | `.skel`, `.shuttle` | carregando |
| Cartão de tarefa (swipe) | em `fonte/Main.dc.html` | ilustração da categoria + corpo; ver C1 |
| Logo | SVG inline em `BoasVindas`/`Splash` | usar os vetores de `docs/brand/logo` |

`SiteHeader` do design system é do site, não do app.

## Movimento

Animações em `@keyframes` no fim do estilo das telas: entrada lateral (`dv-in-r`, `dv-in-l`, `dv-in-up`), `dv-pop`, `dv-shake` (erro), `dv-nudge` (dica de swipe), `dv-grow`, `dv-shuttle` (carregando). Curva padrão `cubic-bezier(.2,.8,.2,1)`. Tudo some com "remover animações" do sistema.

## Imagens

| Pasta | Conteúdo |
|---|---|
| [`icones/`](icones) | 56 ícones de traço (24×24, `stroke-width` 2, ponta reta), com o nome da classe `i-*`. Viram vector drawables `ic_<nome>`. |
| [`ilustracoes/`](ilustracoes) | ilustração de cada categoria de tarefa (topo do cartão): cozinha, banheiro, lavanderia, limpeza geral, lixo, compras, pets, plantas, manutenção, outros. |
| [`avatares/`](avatares) | os 24 avatares do desenho da E3, **só como referência visual**: o app usa DiceBear avataaars (R-32). |
| [`avatares-dicebear/`](avatares-dicebear) | os 24 avatares DiceBear avataaars do app (SVG) e `catalogo.json` com estilo, seed e opções de cada `avatarId`. Gerados por [`ferramentas/avatares`](ferramentas/avatares) com as cores da marca, na ordem e com as características da galeria do desenho. |
| [`fotos/`](fotos) | fotos de exemplo de casa (só para previews; v1.1). |
| [`assets/`](assets) | todos os arquivos com o id original do Claude Design (`/_blob/<id>`), usados por `fonte/` e pelo renderizador. |

## Diferenças encontradas ao copiar (decididas)

- **Escala tipográfica:** o README do design system cita `h1` 64, `h2` 40, `body` 18. As telas usam a escala acima (57/45/16), igual ao `Type.kt`. **Vale a das telas.**
- **Avatares (decidido em 2026-11-10: vale a R-32):** o desenho usa 24 avatares próprios, chapados (`avatares/`), mas a SPEC exige DiceBear estilo avataaars com `avatarId` = estilo + seed. **No app vão 24 avatares DiceBear avataaars**; o layout da E3 (grade, tamanhos, seleção) segue o desenho. Os arquivos de `avatares/` ficam só como referência visual e não entram no app.
- **Foto de perfil (decidido em 2026-11-10, ADR 0003):** "Usar foto" (B1), "Usar uma foto" e "Remover foto" (E2/E3) ficam **escondidos na v1.0**; o perfil usa só os avatares.
- **Componentes:** a issue #2 pedia `TaskCard`, `PointsBadge`, `Chip`, `Button`, `Logo`, `Avatar`; o design system tinha `Ranking` e `SiteHeader`. **Vale o inventário acima**: `SiteHeader` fica de fora (é web); o pódio/lista de ranking é construído na issue do ranking (#17).
- **Falha no desenho:** em `NovaTarefa` o campo descrição mostra `[object Object]`. É erro do mockup: o campo deve aparecer vazio com o placeholder.

## Ferramentas

- [`ferramentas/renderizar.js`](ferramentas/renderizar.js): servidor local + Playwright, gera os PNGs.
- [`ferramentas/support.js`](ferramentas/support.js): runtime dos artboards do Claude Design (inclui React 18, licença MIT). Só serve para renderizar.
