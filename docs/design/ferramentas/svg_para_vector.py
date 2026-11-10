"""Converte SVGs simples (path, circle, rect, com traço ou preenchimento) em vector drawables Android.

Uso (da raiz do repositório):
    python3 docs/design/ferramentas/svg_para_vector.py docs/design/icones core/designsystem/src/main/res/drawable ic_
    python3 docs/design/ferramentas/svg_para_vector.py docs/design/ilustracoes core/designsystem/src/main/res/drawable ilu_ 0.5

O quarto argumento opcional escala o tamanho intrínseco (dp). As ilustrações (320x200) usam 0.5
porque o lint limita vector drawables a 200dp; a tela sempre as redimensiona.

Cobre exatamente o que os arquivos de docs/design usam. Elementos ou atributos desconhecidos
interrompem a conversão para não gerar um desenho diferente do original.
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

SVG = "{http://www.w3.org/2000/svg}"
HERDADOS = ("fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin", "stroke-opacity", "fill-opacity", "opacity")
NOMES_CSS = {"none": None, "black": "#000000", "white": "#FFFFFF"}


def cor(valor):
    if valor is None:
        return None
    valor = valor.strip()
    if valor in NOMES_CSS:
        return NOMES_CSS[valor]
    if re.fullmatch(r"#[0-9a-fA-F]{3}", valor):
        return "#" + "".join(c * 2 for c in valor[1:]).upper()
    if re.fullmatch(r"#[0-9a-fA-F]{6}", valor):
        return valor.upper()
    raise ValueError(f"cor não suportada: {valor}")


def num(v):
    return float(v) if v is not None else 0.0


def fmt(x):
    s = f"{x:.4f}".rstrip("0").rstrip(".")
    return s if s not in ("-0", "") else "0"


def circulo(cx, cy, r):
    return (f"M{fmt(cx - r)},{fmt(cy)}a{fmt(r)},{fmt(r)} 0 1,0 {fmt(2 * r)},0"
            f"a{fmt(r)},{fmt(r)} 0 1,0 {fmt(-2 * r)},0Z")


def elipse(cx, cy, rx, ry):
    return (f"M{fmt(cx - rx)},{fmt(cy)}a{fmt(rx)},{fmt(ry)} 0 1,0 {fmt(2 * rx)},0"
            f"a{fmt(rx)},{fmt(ry)} 0 1,0 {fmt(-2 * rx)},0Z")


def retangulo(x, y, w, h, rx, ry):
    if rx == 0 and ry == 0:
        return f"M{fmt(x)},{fmt(y)}h{fmt(w)}v{fmt(h)}h{fmt(-w)}Z"
    rx = min(rx or ry, w / 2)
    ry = min(ry or rx, h / 2)
    return (f"M{fmt(x + rx)},{fmt(y)}h{fmt(w - 2 * rx)}a{fmt(rx)},{fmt(ry)} 0 0,1 {fmt(rx)},{fmt(ry)}"
            f"v{fmt(h - 2 * ry)}a{fmt(rx)},{fmt(ry)} 0 0,1 {fmt(-rx)},{fmt(ry)}"
            f"h{fmt(-(w - 2 * rx))}a{fmt(rx)},{fmt(ry)} 0 0,1 {fmt(-rx)},{fmt(-ry)}"
            f"v{fmt(-(h - 2 * ry))}a{fmt(rx)},{fmt(ry)} 0 0,1 {fmt(rx)},{fmt(-ry)}Z")


def geometria(el):
    tag = el.tag.replace(SVG, "")
    a = el.attrib
    if tag == "path":
        return a["d"]
    if tag == "circle":
        return circulo(num(a.get("cx")), num(a.get("cy")), num(a["r"]))
    if tag == "ellipse":
        return elipse(num(a.get("cx")), num(a.get("cy")), num(a["rx"]), num(a["ry"]))
    if tag == "rect":
        return retangulo(num(a.get("x")), num(a.get("y")), num(a["width"]), num(a["height"]),
                         num(a.get("rx")), num(a.get("ry")))
    if tag == "line":
        return f"M{a['x1']},{a['y1']}L{a['x2']},{a['y2']}"
    if tag in ("polyline", "polygon"):
        pts = re.split(r"[\s,]+", a["points"].strip())
        pares = [f"{pts[i]},{pts[i + 1]}" for i in range(0, len(pts), 2)]
        return "M" + "L".join(pares) + ("Z" if tag == "polygon" else "")
    raise ValueError(f"elemento não suportado: {tag}")


def estilo_inline(el):
    estilo = {}
    for parte in el.attrib.get("style", "").split(";"):
        if ":" in parte:
            k, v = parte.split(":", 1)
            estilo[k.strip()] = v.strip()
    return estilo


def rotacao(el):
    t = el.attrib.get("transform")
    if t is None:
        return None
    m = re.fullmatch(r"\s*rotate\(\s*(-?[\d.]+)(?:[\s,]+(-?[\d.]+)[\s,]+(-?[\d.]+))?\s*\)\s*", t)
    if not m:
        raise ValueError(f"transform não suportado: {t}")
    return float(m.group(1)), float(m.group(2) or 0), float(m.group(3) or 0)


def caminhar(el, herdado, saida):
    rot = rotacao(el)
    if rot is not None:
        filhos = []
        copia = ET.Element(el.tag, {k: v for k, v in el.attrib.items() if k != "transform"})
        copia.extend(list(el))
        caminhar(copia, herdado, filhos)
        saida.append(("grupo", rot, filhos))
        return
    attrs = dict(herdado)
    for k in HERDADOS:
        if k in el.attrib:
            attrs[k] = el.attrib[k]
    attrs.update({k: v for k, v in estilo_inline(el).items() if k in HERDADOS})
    tag = el.tag.replace(SVG, "")
    if tag in ("svg", "g"):
        for filho in el:
            caminhar(filho, attrs, saida)
        return
    if tag in ("title", "desc", "defs"):
        return
    saida.append((geometria(el), attrs))


def path_xml(d, attrs):
    linhas = [f'        android:pathData="{d}"']
    fill = cor(attrs.get("fill", "#000"))
    stroke = cor(attrs.get("stroke", "none"))
    opacidade = float(attrs.get("opacity", 1))
    if fill:
        linhas.append(f'        android:fillColor="{fill}"')
        alpha = float(attrs.get("fill-opacity", 1)) * opacidade
        if alpha != 1:
            linhas.append(f'        android:fillAlpha="{fmt(alpha)}"')
    if stroke:
        linhas.append(f'        android:strokeColor="{stroke}"')
        linhas.append(f'        android:strokeWidth="{fmt(float(attrs.get("stroke-width", 1)))}"')
        cap = attrs.get("stroke-linecap", "butt")
        join = attrs.get("stroke-linejoin", "miter")
        linhas.append(f'        android:strokeLineCap="{cap}"')
        linhas.append(f'        android:strokeLineJoin="{join}"')
        alpha = float(attrs.get("stroke-opacity", 1)) * opacidade
        if alpha != 1:
            linhas.append(f'        android:strokeAlpha="{fmt(alpha)}"')
    return "    <path\n" + "\n".join(linhas) + " />"


def renderizar(itens, recuo="    "):
    partes = []
    for item in itens:
        if item[0] == "grupo":
            (ang, px, py), filhos = item[1], item[2]
            partes.append(f'{recuo}<group android:rotation="{fmt(ang)}" android:pivotX="{fmt(px)}" '
                          f'android:pivotY="{fmt(py)}">')
            partes.append(renderizar(filhos, recuo + "    "))
            partes.append(f"{recuo}</group>")
        else:
            partes.append("\n".join(recuo[4:] + linha for linha in path_xml(*item).split("\n")))
    return "\n".join(partes)


def converter(svg_path, origem, escala=1.0):
    raiz = ET.parse(svg_path).getroot()
    vb = [float(v) for v in raiz.attrib["viewBox"].split()]
    if vb[0] != 0 or vb[1] != 0:
        raise ValueError("viewBox com origem diferente de 0 não suportado")
    w, h = vb[2], vb[3]
    largura = float(raiz.attrib.get("width", w)) * escala
    altura = float(raiz.attrib.get("height", h)) * escala
    caminhos = []
    caminhar(raiz, {"fill": "#000"}, caminhos)
    corpo = renderizar(caminhos)
    return (f'<?xml version="1.0" encoding="utf-8"?>\n'
            f"<!-- Gerado de {origem} por docs/design/ferramentas/svg_para_vector.py. Não edite à mão. -->\n"
            f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{fmt(largura)}dp"\n    android:height="{fmt(altura)}dp"\n'
            f'    android:viewportWidth="{fmt(w)}"\n    android:viewportHeight="{fmt(h)}">\n'
            f"{corpo}\n</vector>\n")


def main(entrada, saida, prefixo, escala="1"):
    os.makedirs(saida, exist_ok=True)
    n = 0
    for nome in sorted(os.listdir(entrada)):
        if not nome.endswith(".svg"):
            continue
        base = prefixo + nome[:-4].replace("-", "_")
        origem = os.path.join(entrada, nome)
        with open(os.path.join(saida, base + ".xml"), "w", encoding="utf-8") as f:
            f.write(converter(origem, origem, float(escala)))
        n += 1
    print(f"{n} arquivos convertidos para {saida}")


if __name__ == "__main__":
    main(*sys.argv[1:5])
