"""Construye el informe consolidado (entregable/*.docx y *.pdf) a partir de README + docs/*.md.

Pasos: une los capítulos, cambia los SVG por PNG, convierte con Pandoc (incluido en Quarto),
da formato a las tablas con python-docx y usa MS Word para actualizar el índice y exportar a PDF.
Uso: python tools/construir_informe.py
"""
from __future__ import annotations

import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

RAIZ = Path(__file__).resolve().parent.parent
BUILD = RAIZ / "build"
SALIDA = RAIZ / "entregable"
NOMBRE = "Informe-Medio-Curso-ABC-SpecFirst"
QUARTO = [r"C:\Program Files\RStudio\resources\app\bin\quarto\bin\quarto.exe", "quarto"]

PORTADA = """---
title: "Especificación técnica Spec-First: ABC Software Corp"
subtitle: "Facturación electrónica UBL 2.1, inventario formal y migración del legado · Arquitectura MVC con visión hexagonal"
author:
  - "Universidad Nacional Agraria de la Selva · Facultad de Ingeniería en Informática y Sistemas"
  - "Examen de medio curso de Arquitectura de Software · Grupo A"
  - "Alumno: (completar)"
date: "22 de octubre de 2026"
lang: es-PE
toc-title: "Contenido"
---

"""

SECCION = ('```{{=openxml}}\n<w:p><w:pPr><w:sectPr><w:pgSz w:w="{w}" w:h="{h}"{o}/>'
           '<w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134" w:header="709" w:footer="709" w:gutter="0"/>'
           '</w:sectPr></w:pPr></w:p>\n```\n\n')
VERTICAL = SECCION.format(w=11906, h=16838, o="")
HORIZONTAL = SECCION.format(w=16838, h=11906, o=' w:orient="landscape"')
SALTO = '```{=openxml}\n<w:p><w:r><w:br w:type="page"/></w:r></w:p>\n```\n\n'


def quarto() -> str:
    for c in QUARTO:
        if Path(c).exists() or shutil.which(c):
            return c
    raise SystemExit("Se requiere Quarto (incluye Pandoc).")


def unir_markdown() -> Path:
    partes = [PORTADA]
    readme = (RAIZ / "README.md").read_text(encoding="utf-8")
    readme = readme.split("## Resumen ejecutivo", 1)[1].split("## Estructura", 1)[0]
    partes.append("# Presentación\n\n## Resumen ejecutivo" + readme + SALTO)
    for doc in sorted((RAIZ / "docs").glob("[0-9][0-9]-*.md")):
        texto = doc.read_text(encoding="utf-8")
        texto = re.sub(r"<!-- (inicio|fin):[a-z-]+ -->\n?", "", texto)
        texto = re.sub(r"\]\(\.\./diagramas/svg/([^)]+)\.svg\)",
                       lambda m: f"]({(RAIZ / 'diagramas' / 'png' / (m.group(1) + '.png')).as_posix()})", texto)
        texto = re.sub(r"\]\((?!http)([^)]+\.(md|yaml|json|xlsx|xml))\)", r"](\1)", texto)
        if doc.name.startswith("08-"):
            partes += [VERTICAL, texto, "\n\n", HORIZONTAL]
        else:
            partes += [texto, "\n\n", SALTO]
    BUILD.mkdir(exist_ok=True)
    destino = BUILD / f"{NOMBRE}.md"
    destino.write_text("".join(partes), encoding="utf-8")
    return destino


def documento_referencia() -> Path:
    ref = BUILD / "referencia.docx"
    subprocess.run([quarto(), "pandoc", "-o", str(ref), "--print-default-data-file", "reference.docx"], check=True)
    doc = Document(str(ref))
    for s in doc.sections:
        s.page_width, s.page_height = Cm(21.0), Cm(29.7)
        s.top_margin = s.bottom_margin = s.left_margin = s.right_margin = Cm(2.0)
    estilos = doc.styles
    for nombre, tam in (("Normal", 10.5), ("Body Text", 10.5), ("First Paragraph", 10.5), ("Compact", 10)):
        if nombre in [s.name for s in estilos]:
            estilos[nombre].font.name = "Calibri"
            estilos[nombre].font.size = Pt(tam)
    for nivel, tam in ((1, 17), (2, 13.5), (3, 11.5)):
        e = estilos[f"Heading {nivel}"]
        e.font.name, e.font.size, e.font.bold = "Calibri", Pt(tam), True
        e.font.color.rgb = RGBColor(0x1A, 0x4D, 0x8F)
    for nombre in ("Title", "Subtitle", "Author", "Date"):
        if nombre in [s.name for s in estilos]:
            estilos[nombre].font.name = "Calibri"
            estilos[nombre].font.color.rgb = RGBColor(0x0B, 0x25, 0x45)
    doc.save(str(ref))
    return ref


def bordes(tabla) -> None:
    tblPr = tabla._tbl.tblPr
    b = OxmlElement("w:tblBorders")
    for lado in ("top", "left", "bottom", "right", "insideH", "insideV"):
        e = OxmlElement(f"w:{lado}")
        e.set(qn("w:val"), "single"), e.set(qn("w:sz"), "4"), e.set(qn("w:color"), "9AA8B8")
        b.append(e)
    tblPr.append(b)


def sombrear(celda, color: str) -> None:
    sh = OxmlElement("w:shd")
    sh.set(qn("w:val"), "clear"), sh.set(qn("w:color"), "auto"), sh.set(qn("w:fill"), color)
    celda._tc.get_or_add_tcPr().append(sh)


def dar_formato(docx: Path) -> None:
    doc = Document(str(docx))
    for tabla in doc.tables:
        columnas = len(tabla.columns)
        tam = Pt(7.5 if columnas >= 7 else 8.5 if columnas >= 5 else 9)
        bordes(tabla)
        for i, fila in enumerate(tabla.rows):
            for celda in fila.cells:
                if i == 0:
                    sombrear(celda, "1A4D8F")
                for p in celda.paragraphs:
                    for r in p.runs:
                        r.font.size = tam
                        if "/" in r.text and len(r.text) > 25:
                            r.text = r.text.replace("/", "/\u200b")
                        if i == 0:
                            r.font.bold = True
                            r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
    for p in doc.paragraphs:
        if p.style.name in ("Captioned Figure", "Figure") or any(r._r.xpath(".//w:drawing") for r in p.runs):
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    doc.save(str(docx))


def word_pdf(docx: Path, pdf: Path) -> bool:
    script = f"""
$w = New-Object -ComObject Word.Application
$w.Visible = $false
$d = $w.Documents.Open('{docx}')
foreach ($t in $d.TablesOfContents) {{ $t.Update() }}
$d.Fields.Update() | Out-Null
$d.Save()
$d.ExportAsFixedFormat('{pdf}', 17)
$d.Close()
$w.Quit()
"""
    r = subprocess.run(["powershell", "-NoProfile", "-Command", script], capture_output=True, text=True)
    if r.returncode != 0:
        print("Aviso: no se pudo usar MS Word para el PDF:", r.stderr.strip()[:300])
    return pdf.exists()


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    md = unir_markdown()
    ref = documento_referencia()
    SALIDA.mkdir(exist_ok=True)
    docx, pdf = SALIDA / f"{NOMBRE}.docx", SALIDA / f"{NOMBRE}.pdf"
    subprocess.run([quarto(), "pandoc", str(md), "-o", str(docx), "--from", "markdown+pipe_tables+raw_attribute",
                    "--toc", "--toc-depth=2", f"--reference-doc={ref}", f"--resource-path={RAIZ}"],
                   check=True, cwd=RAIZ / "docs")
    dar_formato(docx)
    ok = word_pdf(docx, pdf) if os.name == "nt" else False
    print(f"DOCX: {docx.relative_to(RAIZ)}" + (f" | PDF: {pdf.relative_to(RAIZ)}" if ok else " | PDF no generado"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
