"""Junta lado a lado o PNG do desenho e o screenshot do app, com a diferença destacada.

Uso (da raiz do repositório):
    python3 docs/design/ferramentas/comparar.py <png do desenho> <screenshot do app> <saída.png>

Exemplo:
    python3 docs/design/ferramentas/comparar.py docs/design/png/Entrar.png \
        feature/auth/src/test/screenshots/A3_entrar_senha_incorreta.png /tmp/entrar.png

A imagem de saída tem três colunas: desenho, app e a diferença (pixels que mudaram em magenta).
Também imprime a porcentagem de pixels diferentes. Requer Pillow.
"""
import sys

from PIL import Image, ImageChops, ImageDraw


def comparar(desenho_path, app_path, saida_path, limiar=48):
    desenho = Image.open(desenho_path).convert("RGB")
    app = Image.open(app_path).convert("RGB")
    if app.size != desenho.size:
        app = app.resize(desenho.size)
    dif = ImageChops.difference(desenho, app).convert("L").point(lambda v: 255 if v > limiar else 0)
    total = dif.size[0] * dif.size[1]
    diferentes = sum(1 for v in dif.getdata() if v)
    marcada = Image.composite(Image.new("RGB", desenho.size, (255, 0, 255)), desenho.point(lambda v: v // 3 + 170), dif)
    w, h = desenho.size
    saida = Image.new("RGB", (w * 3 + 40, h + 40), "white")
    for i, (img, rotulo) in enumerate([(desenho, "desenho"), (app, "app"), (marcada, "diferença")]):
        saida.paste(img, (i * (w + 20), 40))
        ImageDraw.Draw(saida).text((i * (w + 20) + 8, 12), rotulo, fill="black")
    saida.save(saida_path)
    print(f"{diferentes / total:.1%} dos pixels diferentes -> {saida_path}")


if __name__ == "__main__":
    comparar(*sys.argv[1:4])
