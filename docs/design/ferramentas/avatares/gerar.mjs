// Gera a galeria de 24 avatares DiceBear avataaars (R-32), na ordem e com as características da
// galeria do desenho (docs/design/png/Avatar.png): cabelo, pele, roupa e fundo nas cores da marca.
//
// Uso (Node 24):
//   cd docs/design/ferramentas/avatares && npm ci && NODE_PATH=/opt/node-tools/node_modules npm run gerar
//   (precisa do pacote playwright com Chromium; nas sessões em nuvem ele fica em /opt/node-tools)
//
// Saídas:
//   docs/design/avatares-dicebear/avatar-NN.svg          SVG de cada avatar
//   docs/design/avatares-dicebear/catalogo.json          avatarId -> estilo, seed e opções
//   core/designsystem/src/main/res/drawable-nodpi/avatar_NN.webp   288 px (96dp em 3x), usado no app
import { createAvatar } from "@dicebear/core";
import * as avataaars from "@dicebear/avataaars";
import { mkdirSync, writeFileSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const aqui = dirname(fileURLToPath(import.meta.url));
const raiz = join(aqui, "../../../..");
const pastaSvg = join(raiz, "docs/design/avatares-dicebear");
const pastaApp = join(raiz, "core/designsystem/src/main/res/drawable-nodpi");

// Cores da marca usadas como fundo e roupa (tokens do estilo das telas).
const COR = {
  ultramarino: "2f2bd9",
  lima: "c9f24d",
  tinta: "0c0f18",
  papel: "ebebe6",
  azulClaro: "e9eeff",
  azulMedio: "d3defe",
  branco: "f5f5f0",
};
// Tons de pele e de cabelo.
const PELE = { clara: "ffdbb4", morena: "edb98a", parda: "d08b5b", marrom: "ae5d29", escura: "614335" };
const CABELO = { preto: "2c1b18", castanho: "724133", lima: "c9f24d", azul: "2f2bd9", branco: "e8e1e1" };

// [top, cabelo, pele, roupa, fundo, extras]
const GALERIA = [
  ["straight01", CABELO.castanho, PELE.morena, COR.tinta, COR.papel],
  ["shortFlat", CABELO.preto, PELE.clara, COR.lima, COR.ultramarino],
  ["fro", CABELO.preto, PELE.marrom, COR.ultramarino, COR.lima],
  ["bun", CABELO.lima, PELE.escura, COR.ultramarino, COR.tinta],
  ["hat", CABELO.preto, PELE.clara, COR.ultramarino, COR.azulClaro, { hatColor: [COR.lima] }],
  ["shortRound", CABELO.preto, PELE.parda, COR.branco, COR.ultramarino, { facialHair: ["beardMedium"], facialHairProbability: 100 }],
  ["straight02", CABELO.azul, PELE.clara, COR.tinta, COR.lima],
  ["curly", CABELO.preto, PELE.escura, COR.lima, COR.papel],
  ["longButNotTooLong", CABELO.castanho, PELE.morena, COR.branco, COR.ultramarino],
  ["bun", CABELO.azul, PELE.clara, COR.branco, COR.lima],
  ["shortCurly", CABELO.preto, PELE.escura, COR.ultramarino, COR.papel],
  ["hijab", CABELO.preto, PELE.parda, COR.branco, COR.tinta, { hatColor: [COR.lima] }],
  ["winterHat1", CABELO.preto, PELE.morena, COR.branco, COR.azulClaro, { hatColor: [COR.branco] }],
  ["frizzle", CABELO.castanho, PELE.clara, COR.lima, COR.azulMedio],
  ["straightAndStrand", CABELO.preto, PELE.escura, COR.branco, COR.ultramarino],
  ["bun", CABELO.preto, PELE.parda, COR.tinta, COR.lima],
  ["shortWaved", CABELO.lima, PELE.morena, COR.branco, COR.papel],
  ["theCaesar", CABELO.branco, PELE.clara, COR.lima, COR.tinta, { facialHair: ["beardMajestic"], facialHairColor: [CABELO.branco], facialHairProbability: 100 }],
  ["winterHat02", CABELO.preto, PELE.escura, COR.ultramarino, COR.azulClaro, { hatColor: [COR.tinta] }],
  ["curvy", CABELO.azul, PELE.parda, COR.tinta, COR.azulMedio],
  ["straight01", CABELO.preto, PELE.morena, COR.branco, COR.ultramarino],
  ["bob", CABELO.preto, PELE.clara, COR.branco, COR.lima],
  ["sides", CABELO.branco, PELE.escura, COR.ultramarino, COR.papel],
  ["shortFlat", CABELO.castanho, PELE.marrom, COR.branco, COR.tinta, { facialHair: ["beardLight"], facialHairColor: [CABELO.castanho], facialHairProbability: 100 }],
];

// Expressão e roupa iguais para todos: rosto simpático e camiseta lisa, como no desenho.
const BASE = {
  clothing: ["shirtCrewNeck"],
  eyes: ["default"],
  eyebrows: ["defaultNatural"],
  mouth: ["smile"],
  accessoriesProbability: 0,
  facialHairProbability: 0,
  topProbability: 100,
  radius: 50,
};

mkdirSync(pastaSvg, { recursive: true });
mkdirSync(pastaApp, { recursive: true });
const catalogo = { estilo: "avataaars", versao: "@dicebear/avataaars 9.4.3", avatares: [] };
const svgs = [];

GALERIA.forEach(([top, cabelo, pele, roupa, fundo, extras = {}], i) => {
  const n = String(i + 1).padStart(2, "0");
  const seed = `divvup-${n}`;
  const opcoes = {
    ...BASE,
    seed,
    top: [top],
    hairColor: [cabelo],
    skinColor: [pele],
    clothesColor: [roupa],
    backgroundColor: [fundo],
    ...extras,
  };
  const svg = createAvatar(avataaars, opcoes).toString();
  writeFileSync(join(pastaSvg, `avatar-${n}.svg`), svg);
  catalogo.avatares.push({ avatarId: `avataaars:${seed}`, arquivo: `avatar_${n}`, opcoes });
  svgs.push([n, svg]);
});
writeFileSync(join(pastaSvg, "catalogo.json"), JSON.stringify(catalogo, null, 2) + "\n");

// Rasteriza para WebP com o Chromium do Playwright (pré-instalado nas sessões em nuvem).
const require = createRequire(import.meta.url);
const { chromium } = require("playwright");
const navegador = await chromium.launch();
const pagina = await navegador.newPage({ viewport: { width: 288, height: 288 } });
for (const [n, svg] of svgs) {
  await pagina.setContent(`<html><body style="margin:0;background:transparent">${svg.replace("<svg ", '<svg width="288" height="288" ')}</body></html>`);
  const png = await pagina.screenshot({ omitBackground: true, clip: { x: 0, y: 0, width: 288, height: 288 } });
  writeFileSync(join(pastaApp, `avatar_${n}.png`), png);
}
await navegador.close();
console.log(`${svgs.length} avatares gerados`);
