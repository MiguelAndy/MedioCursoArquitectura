"""Genera la matriz de trazabilidad (XLSX, CSV y tablas Markdown) y la tabla de endpoints desde los specs.

Fuentes:  specs/trazabilidad/matriz-trazabilidad.yaml, specs/openapi/abc-comercial-api.yaml, XSD OASIS.
Salidas:  specs/trazabilidad/matriz-trazabilidad.xlsx, specs/trazabilidad/csv/*.csv,
          regiones marcadas de docs/08-matriz-trazabilidad-normativa.md y docs/07-contratos-api.md.
Uso:      python tools/generar_matrices.py
"""
from __future__ import annotations

import csv
import re
import sys
from datetime import date
from pathlib import Path

import yaml
from openpyxl import Workbook
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter

sys.path.insert(0, str(Path(__file__).resolve().parent))
from xsd_cardinalidad import ResolutorXsd  # noqa: E402

RAIZ = Path(__file__).resolve().parent.parent
FUENTE = RAIZ / "specs" / "trazabilidad" / "matriz-trazabilidad.yaml"
OPENAPI = RAIZ / "specs" / "openapi" / "abc-comercial-api.yaml"
SALIDA_XLSX = RAIZ / "specs" / "trazabilidad" / "matriz-trazabilidad.xlsx"
SALIDA_CSV = RAIZ / "specs" / "trazabilidad" / "csv"
DOC_MATRIZ = RAIZ / "docs" / "08-matriz-trazabilidad-normativa.md"
DOC_API = RAIZ / "docs" / "07-contratos-api.md"
PERFIL = "▲"  # obligatorio para SUNAT pero opcional en el XSD


def celda(valor) -> str:
    texto = ", ".join(valor) if isinstance(valor, list) else str(valor if valor is not None else "")
    return texto.replace("|", "\\|").replace("\n", " ")


def tabla_markdown(encabezados: list[str], filas: list[list]) -> str:
    lineas = ["| " + " | ".join(encabezados) + " |", "|" + "|".join("---" for _ in encabezados) + "|"]
    lineas += ["| " + " | ".join(celda(v) for v in fila) + " |" for fila in filas]
    return "\n".join(lineas)


def reemplazar_region(texto: str, nombre: str, contenido: str) -> str:
    patron = re.compile(rf"(<!-- inicio:{nombre} -->)(.*?)(<!-- fin:{nombre} -->)", re.S)
    if not patron.search(texto):
        raise SystemExit(f"No se encontró la región '{nombre}' en el documento")
    return patron.sub(lambda m: f"{m.group(1)}\n{contenido}\n{m.group(3)}", texto)


def construir_filas(datos: dict, resolutor: ResolutorXsd) -> dict[str, tuple[list[str], list[list]]]:
    filas_a, filas_n = [], []
    for f in datos["matriz_a"]:
        card, obligatorio, cadena = resolutor.resumen(f["xsd"])
        marca = f" {PERFIL}" if f["cond"].startswith("M") and not obligatorio else ""
        filas_a.append([f["id"], f["campo"], f["dato"], f["cond"], f["cat"], f["xpath"], card + marca,
                        "sí" if obligatorio else "no", cadena, f["dto"], f["dominio"], f["reglas"]])
    for f in datos["matriz_notas"]:
        card, obligatorio, cadena = resolutor.resumen(f["xsd"])
        marca = f" {PERFIL}" if f["cond"].startswith("M") and not obligatorio else ""
        filas_n.append([f["id"], f["dato"], f["cond"], f["cat"], f["xpath"], card + marca,
                        "sí" if obligatorio else "no", cadena, f["dto"], f["reglas"]])
    return {
        "A": (["ID", "N.° Anexo 9-A", "Dato SUNAT", "Cond.", "Catálogo", "Elemento UBL 2.1 (XPath)", "Card. XSD",
               "¿Oblig. en XSD?", "Ruta de cardinalidades (XSD OASIS)", "Campo DTO", "Dominio", "Reglas"], filas_a),
        "N": (["ID", "Dato SUNAT", "Cond.", "Catálogo", "Elemento UBL 2.1 (XPath)", "Card. XSD", "¿Oblig. en XSD?",
               "Ruta de cardinalidades (XSD OASIS)", "Campo DTO", "Reglas"], filas_n),
        "B": (["ID", "Columna del Formato 14.1 / RVIE", "Tabla", "Origen en UBL 2.1", "Campo DTO (LineaRegistroVentas)"],
              [[f["id"], f["columna"], f["tabla"], f["ubl"], f["dto"]] for f in datos["matriz_b"]]),
        "C": (["ID", "Columna del Formato 12.1", "Tabla", "Entidad / atributo", "Campo del legado", "Regla"],
              [[f["id"], f["columna"], f["tabla"], f["dominio"], f["legado"], f["regla"]] for f in datos["matriz_c"]]),
        "D": (["Requisito", "Descripción", "Artefactos", "Verificación"],
              [[f["id"], f["requisito"], f["artefactos"], f["verificacion"]] for f in datos["matriz_d"]]),
    }


def escribir_csv(matrices: dict) -> None:
    SALIDA_CSV.mkdir(parents=True, exist_ok=True)
    nombres = {"A": "A-factura-boleta", "N": "A2-notas", "B": "B-registro-ventas-14-1", "C": "C-inventario-12-1",
               "D": "D-requisitos"}
    for clave, (encabezados, filas) in matrices.items():
        with (SALIDA_CSV / f"matriz-{nombres[clave]}.csv").open("w", newline="", encoding="utf-8-sig") as archivo:
            escritor = csv.writer(archivo)
            escritor.writerow(encabezados)
            escritor.writerows([[celda(v) for v in fila] for fila in filas])


def escribir_xlsx(matrices: dict, datos: dict) -> None:
    libro = Workbook()
    portada = libro.active
    portada.title = "Léeme"
    lineas = [
        ("Matriz de trazabilidad normativa SUNAT ↔ UBL 2.1 · ABC Software Corp (fase 1, Spec-First)", True),
        (f"Versión {datos['version']} · generada el {date.today().isoformat()} por tools/generar_matrices.py", False),
        ("", False),
        ("Hojas:", True),
        ("A  Factura y boleta (Invoice): dato SUNAT ↔ XPath UBL ↔ cardinalidad XSD ↔ DTO ↔ dominio ↔ reglas", False),
        ("A2 Notas de crédito y débito (CreditNote / DebitNote)", False),
        ("B  Registro de Ventas e Ingresos (Formato 14.1 / RVIE) ↔ UBL ↔ DTO", False),
        ("C  Registro del Inventario Permanente en Unidades Físicas (Formato 12.1) ↔ dominio ↔ legado", False),
        ("D  Requisitos del enunciado ↔ artefactos ↔ verificación", False),
        ("", False),
        ("Leyenda:", True),
        ("Cond.: M = mandatorio, C = condicional (Anexo N.° 9-A).", False),
        ("Card. XSD: cardinalidad del último elemento en UBL-*-2.1.xsd (OASIS), calculada automáticamente.", False),
        (f"{PERFIL}: obligatorio para SUNAT pero opcional en el XSD (diferencia del perfil SUNAT).", False),
        ("", False),
        ("Fuentes:", True),
    ] + [(f"{clave}: {texto}", False) for clave, texto in datos["fuentes"].items()]
    for i, (texto, negrita) in enumerate(lineas, start=1):
        portada.cell(row=i, column=1, value=texto).font = Font(bold=negrita, size=12 if i == 1 else 11)
    portada.column_dimensions["A"].width = 120

    titulos = {"A": "A Factura-Boleta", "N": "A2 Notas", "B": "B Registro Ventas 14.1", "C": "C Inventario 12.1",
               "D": "D Requisitos"}
    anchos = {"ID": 9, "N.° Anexo 9-A": 11, "Cond.": 7, "Catálogo": 11, "Tabla": 10, "Card. XSD": 11,
              "¿Oblig. en XSD?": 10, "Requisito": 10, "Regla": 10}
    borde = Border(*(Side(style="thin", color="B0B7C3"),) * 4)
    for clave, (encabezados, filas) in matrices.items():
        hoja = libro.create_sheet(titulos[clave])
        hoja.append(encabezados)
        for fila in filas:
            hoja.append([celda(v) for v in fila])
        for col, encabezado in enumerate(encabezados, start=1):
            c = hoja.cell(row=1, column=col)
            c.font = Font(bold=True, color="FFFFFF")
            c.fill = PatternFill("solid", fgColor="1A4D8F")
            c.alignment = Alignment(wrap_text=True, vertical="center")
            hoja.column_dimensions[get_column_letter(col)].width = anchos.get(encabezado, 38)
        for fila in hoja.iter_rows(min_row=1, max_row=hoja.max_row, max_col=len(encabezados)):
            for c in fila:
                c.border = borde
                if c.row > 1:
                    c.alignment = Alignment(wrap_text=True, vertical="top")
                    if c.row % 2 == 0:
                        c.fill = PatternFill("solid", fgColor="F3F6FA")
        hoja.freeze_panes = "B2"
        hoja.auto_filter.ref = hoja.dimensions
    libro.save(SALIDA_XLSX)


def tabla_endpoints() -> str:
    spec = yaml.safe_load(OPENAPI.read_text(encoding="utf-8"))
    filas = []
    for ruta, item in spec["paths"].items():
        for metodo in ("get", "post", "put", "patch", "delete"):
            op = item.get(metodo)
            if not op:
                continue
            filas.append([metodo.upper(), f"`{ruta}`", op["operationId"], op.get("x-casos-uso", []),
                          op.get("x-modulo", ""), op.get("x-puerto-entrada", ""),
                          op.get("x-roles", ["autenticado"] if op.get("security", [1]) != [] else ["público"])])
    return tabla_markdown(["Método", "Ruta", "operationId", "CU", "Módulo", "Puerto de entrada", "Roles"], filas)


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    datos = yaml.safe_load(FUENTE.read_text(encoding="utf-8"))
    matrices = construir_filas(datos, ResolutorXsd())
    escribir_csv(matrices)
    escribir_xlsx(matrices, datos)

    vista = {  # columnas visibles en el Markdown (el XLSX/CSV conservan todas)
        "A": [0, 1, 2, 3, 4, 5, 6, 9, 11],
        "N": [0, 1, 2, 3, 4, 5, 8, 9],
        "B": [0, 1, 2, 3, 4],
        "C": [0, 1, 2, 3, 4, 5],
        "D": [0, 1, 2, 3],
    }
    regiones = {"A": "matriz-a", "N": "matriz-notas", "B": "matriz-b", "C": "matriz-c", "D": "matriz-d"}
    texto = DOC_MATRIZ.read_text(encoding="utf-8")
    for clave, (encabezados, filas) in matrices.items():
        columnas = vista[clave]
        md = tabla_markdown([encabezados[i] for i in columnas], [[fila[i] for i in columnas] for fila in filas])
        texto = reemplazar_region(texto, regiones[clave], md)
    DOC_MATRIZ.write_text(texto, encoding="utf-8")

    DOC_API.write_text(reemplazar_region(DOC_API.read_text(encoding="utf-8"), "tabla-endpoints", tabla_endpoints()),
                       encoding="utf-8")
    perfil = sum(1 for fila in matrices["A"][1] if PERFIL in fila[6])
    print(f"Matriz A: {len(matrices['A'][1])} filas ({perfil} con diferencia de perfil {PERFIL}); "
          f"A2: {len(matrices['N'][1])}; B: {len(matrices['B'][1])}; C: {len(matrices['C'][1])}; "
          f"D: {len(matrices['D'][1])}")
    print(f"Escrito: {SALIDA_XLSX.relative_to(RAIZ)}, {SALIDA_CSV.relative_to(RAIZ)}/*.csv, "
          f"{DOC_MATRIZ.relative_to(RAIZ)}, {DOC_API.relative_to(RAIZ)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
