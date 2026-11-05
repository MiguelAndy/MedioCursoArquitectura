"""Renderiza los diagramas Mermaid de diagramas/fuentes/*.mmd a SVG (para Markdown/GitHub) y PNG (para el informe).

Usa @mermaid-js/mermaid-cli mediante npx y un Chrome o Edge ya instalado (no descarga Chromium).
Uso: python tools/renderizar_diagramas.py [nombre-sin-extension ...]
Variables opcionales: MERMAID_CHROME=ruta del navegador.
"""
from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
FUENTES = RAIZ / "diagramas" / "fuentes"
SALIDA_SVG = RAIZ / "diagramas" / "svg"
SALIDA_PNG = RAIZ / "diagramas" / "png"
MERMAID_CLI = "@mermaid-js/mermaid-cli@11.4.2"

CONFIG_MERMAID = {
    "theme": "base",
    "themeVariables": {
        "fontFamily": "Arial, Helvetica, sans-serif",
        "fontSize": "15px",
        "primaryColor": "#E8F0FE",
        "primaryBorderColor": "#1A4D8F",
        "primaryTextColor": "#0B2545",
        "secondaryColor": "#FFF4E0",
        "tertiaryColor": "#F7F9FC",
        "lineColor": "#4A5A6A",
        "clusterBkg": "#F7F9FC",
        "clusterBorder": "#9AA8B8",
        "noteBkgColor": "#FFF8DC",
        "actorBkg": "#E8F0FE",
        "actorBorder": "#1A4D8F",
    },
    "flowchart": {"htmlLabels": True, "curve": "basis", "nodeSpacing": 40, "rankSpacing": 55},
    "sequence": {"mirrorActors": False},
}

CANDIDATOS_NAVEGADOR = [
    os.environ.get("MERMAID_CHROME", ""),
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    "/usr/bin/google-chrome",
    "/usr/bin/chromium",
]


def navegador() -> str:
    for ruta in CANDIDATOS_NAVEGADOR:
        if ruta and Path(ruta).exists():
            return ruta
    raise SystemExit("No se encontró Chrome/Edge. Defina MERMAID_CHROME con la ruta del navegador.")


def main() -> int:
    npx = shutil.which("npx") or shutil.which("npx.cmd")
    if not npx:
        raise SystemExit("Se requiere Node.js (npx) para renderizar los diagramas.")
    filtro = set(sys.argv[1:])
    SALIDA_SVG.mkdir(parents=True, exist_ok=True)
    SALIDA_PNG.mkdir(parents=True, exist_ok=True)
    entorno = dict(os.environ, PUPPETEER_SKIP_DOWNLOAD="true")
    with tempfile.TemporaryDirectory() as tmp:
        config = Path(tmp) / "mermaid.json"
        config.write_text(json.dumps(CONFIG_MERMAID), encoding="utf-8")
        pptr = Path(tmp) / "puppeteer.json"
        pptr.write_text(json.dumps({"executablePath": navegador(), "args": ["--no-sandbox"]}), encoding="utf-8")
        errores = 0
        for fuente in sorted(FUENTES.glob("*.mmd")):
            if filtro and fuente.stem not in filtro:
                continue
            for destino, extra in ((SALIDA_SVG / f"{fuente.stem}.svg", []),
                                   (SALIDA_PNG / f"{fuente.stem}.png", ["-s", "2", "-w", "1400"])):
                cmd = [npx, "-y", MERMAID_CLI, "-q", "-p", str(pptr), "-c", str(config),
                       "-b", "white", "-i", str(fuente), "-o", str(destino), *extra]
                r = subprocess.run(cmd, capture_output=True, text=True, env=entorno)
                ok = r.returncode == 0 and destino.exists()
                errores += 0 if ok else 1
                print(f"[{'OK ' if ok else 'ERR'}] {destino.relative_to(RAIZ)}"
                      + ("" if ok else f" - {(r.stderr or r.stdout).strip()[:300]}"))
    return 1 if errores else 0


if __name__ == "__main__":
    raise SystemExit(main())
