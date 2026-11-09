// Renderiza as telas do Claude Design (docs/design/fonte) em PNG (docs/design/png).
//
// Uso (da raiz do repositório):
//   node docs/design/ferramentas/renderizar.js            # todas as telas, claro e escuro
//   node docs/design/ferramentas/renderizar.js Main Ranking
//   node docs/design/ferramentas/renderizar.js --servir   # só sobe http://localhost:8765/<Tela>.dc.html
//
// Precisa de Playwright com Chromium (já instalado nas sessões em nuvem).
// A fonte Bricolage Grotesque vem do Google Fonts; sem rede a renderização cai em system-ui.
const http = require('http');
const fs = require('fs');
const path = require('path');

const RAIZ = path.resolve(__dirname, '..');
const FONTE = path.join(RAIZ, 'fonte');
const ASSETS = path.join(RAIZ, 'assets');
const SAIDA = path.join(RAIZ, 'png');
const PORTA = 8765;
const TIPOS = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.svg': 'image/svg+xml', '.json': 'application/json' };

// Tela base = artboard com conteúdo próprio (as variantes só importam outra tela).
const ehVariante = (txt) => /<dc-import /.test(txt) && txt.length < 1500;

function wrapperEscuro(nome) {
  return `<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>${nome} escuro</title>
<script src="./support.js"></script><link rel="stylesheet" href="/_blob/f25551cbab6b194e9c1439682cdfeafb"></head>
<body><x-dc><helmet><link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wdth,wght@12..96,75..100,200..800&amp;display=swap"><style>body{margin:0}</style></helmet>
<div style="width:360px;height:800px;overflow:hidden"><dc-import name="${nome}" tema="escuro" hint-size="360px,800px"></dc-import></div></x-dc>
<script type="text/x-dc" data-dc-script data-props='{"$preview":{"width":360,"height":800}}'>
class Component extends DCLogic { renderVals() { return {}; } }
</script></body></html>`;
}

function servidor() {
  return http.createServer((req, res) => {
    const url = decodeURIComponent(req.url.split('?')[0]);
    const m = url.match(/^\/([A-Za-z0-9]+)__escuro\.dc\.html$/);
    if (m) { res.writeHead(200, { 'content-type': 'text/html' }); return res.end(wrapperEscuro(m[1])); }
    let arq;
    if (url.startsWith('/_blob/')) {
      const id = url.slice(7);
      const achado = fs.readdirSync(ASSETS).find((x) => x.startsWith(id + '.'));
      arq = achado && path.join(ASSETS, achado);
    } else if (url.endsWith('/support.js')) {
      arq = path.join(__dirname, 'support.js');
    } else {
      arq = path.join(FONTE, path.normalize(url).replace(/^(\.\.[/\\])+/, ''));
    }
    if (!arq || !fs.existsSync(arq) || fs.statSync(arq).isDirectory()) { res.writeHead(404); return res.end(); }
    res.writeHead(200, { 'content-type': TIPOS[path.extname(arq)] || 'application/octet-stream' });
    fs.createReadStream(arq).pipe(res);
  }).listen(PORTA);
}

(async () => {
  const args = process.argv.slice(2);
  const srv = servidor();
  if (args.includes('--servir')) { console.log(`http://localhost:${PORTA}/Main.dc.html`); return; }

  const todas = fs.readdirSync(FONTE).filter((f) => f.endsWith('.dc.html')).map((f) => f.replace('.dc.html', ''))
    .filter((n) => n !== 'Prototipo');
  const alvo = args.length ? args : todas;
  const nomes = [];
  for (const n of alvo) {
    nomes.push(n);
    if (!ehVariante(fs.readFileSync(path.join(FONTE, n + '.dc.html'), 'utf8'))) nomes.push(n + '__escuro');
  }

  const { chromium } = require('playwright');
  const nav = await chromium.launch();
  const pag = await nav.newPage({ viewport: { width: 360, height: 800 }, deviceScaleFactor: 2 });
  pag.on('pageerror', (e) => console.error('erro na tela:', e.message));
  for (const n of nomes) {
    await pag.goto(`http://localhost:${PORTA}/${n}.dc.html`, { waitUntil: 'networkidle' });
    await pag.evaluate(() => document.fonts.ready);
    await pag.waitForTimeout(1500); // animações de entrada
    await pag.screenshot({ path: path.join(SAIDA, n + '.png'), clip: { x: 0, y: 0, width: 360, height: 800 } });
    console.log('ok', n);
  }
  await nav.close();
  srv.close();
})();
