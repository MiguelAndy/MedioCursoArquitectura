"""Cardinalidades de UBL 2.1 leídas directamente de los XSD oficiales de OASIS.

Uso:
  python tools/xsd_cardinalidad.py Invoice                      # secuencia raíz de UBL-Invoice-2.1.xsd
  python tools/xsd_cardinalidad.py /Invoice/cac:TaxTotal/cac:TaxSubtotal/cac:TaxCategory/cbc:TierRange

Se usa también como módulo desde tools/generar_matrices.py para llenar la columna "Card. XSD" de la matriz.
"""
from __future__ import annotations

import sys
from pathlib import Path

from lxml import etree

XS = "{http://www.w3.org/2001/XMLSchema}"
RAIZ_XSD = Path(__file__).resolve().parent.parent / "specs" / "ubl" / "xsd-oasis-ubl-2.1"


def _tipos(esquema: etree._Element) -> dict[str, etree._Element]:
    return {ct.get("name"): ct for ct in esquema.iter(XS + "complexType") if ct.get("name")}


def _elementos(esquema: etree._Element) -> dict[str, str]:
    return {e.get("name"): e.get("type") for e in esquema.findall(XS + "element") if e.get("name")}


def _formato(minimo: str, maximo: str) -> str:
    return f"{minimo}..{'n' if maximo == 'unbounded' else maximo}"


class ResolutorXsd:
    """Recorre los complexType de UBL siguiendo una ruta tipo XPath (sin predicados ni atributos)."""

    def __init__(self, raiz: Path = RAIZ_XSD) -> None:
        comun = Path(raiz) / "common"
        cac = etree.parse(str(comun / "UBL-CommonAggregateComponents-2.1.xsd")).getroot()
        ext = etree.parse(str(comun / "UBL-CommonExtensionComponents-2.1.xsd")).getroot()
        ext_datos = etree.parse(str(comun / "UBL-ExtensionContentDataType-2.1.xsd")).getroot()
        self._maindoc = Path(raiz) / "maindoc"
        self._tipos = {"cac": _tipos(cac), "ext": {**_tipos(ext), **_tipos(ext_datos)}}
        self._elementos = {"cac": _elementos(cac), "ext": _elementos(ext)}
        self._raices: dict[str, etree._Element] = {}

    def _tipo_raiz(self, documento: str) -> etree._Element:
        if documento not in self._raices:
            esquema = etree.parse(str(self._maindoc / f"UBL-{documento}-2.1.xsd")).getroot()
            self._raices[documento] = _tipos(esquema)[f"{documento}Type"]
        return self._raices[documento]

    @staticmethod
    def _hijos(tipo: etree._Element, prefijo: str) -> tuple[dict[str, tuple[str, str]], bool]:
        """Hijos declarados por referencia; las referencias sin prefijo pertenecen al espacio del propio esquema."""
        hijos = {}
        for e in tipo.iter(XS + "element"):
            ref = e.get("ref")
            if ref:
                hijos[ref if ":" in ref else f"{prefijo}:{ref}"] = (e.get("minOccurs", "1"), e.get("maxOccurs", "1"))
        tiene_any = next(tipo.iter(XS + "any"), None) is not None
        return hijos, tiene_any

    def secuencia_raiz(self, documento: str) -> list[tuple[str, str]]:
        hijos, _ = self._hijos(self._tipo_raiz(documento), "doc")
        return [(nombre, _formato(*card)) for nombre, card in hijos.items()]

    def cadena(self, ruta: str) -> list[tuple[str, str]]:
        """[(paso, 'min..max'), ...] para cada paso; 'any (lax)' si el paso cae en un xsd:any."""
        pasos = [p.split("[")[0].strip() for p in ruta.strip().strip("/").split("/")]
        pasos = [p for p in pasos if p and not p.startswith("@")]
        tipo, prefijo_tipo = self._tipo_raiz(pasos[0]), "doc"
        resultado: list[tuple[str, str]] = []
        for paso in pasos[1:]:
            hijos, tiene_any = self._hijos(tipo, prefijo_tipo)
            if paso in hijos:
                resultado.append((paso, _formato(*hijos[paso])))
            elif tiene_any:
                resultado.append((paso, "any (lax)"))
                break
            else:
                raise ValueError(f"'{paso}' no es un hijo permitido por el XSD en la ruta {ruta}")
            prefijo, local = paso.split(":")
            nombre_tipo = self._elementos.get(prefijo, {}).get(local)
            if not nombre_tipo:
                break  # elemento básico (cbc): fin del recorrido
            tipo, prefijo_tipo = self._tipos[prefijo][nombre_tipo.split(":")[-1]], prefijo
        return resultado

    def resumen(self, ruta: str) -> tuple[str, bool, str]:
        """(cardinalidad del último paso, ¿obligatorio según el XSD en toda la ruta?, cadena legible)."""
        cadena = self.cadena(ruta)
        obligatorio = all(c.split("..")[0] == "1" for _, c in cadena if c != "any (lax)") and \
            all(c != "any (lax)" for _, c in cadena)
        legible = " › ".join(f"{p.split(':')[-1]} {c}" for p, c in cadena)
        return cadena[-1][1], obligatorio, legible


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    argumento = sys.argv[1] if len(sys.argv) > 1 else "Invoice"
    resolutor = ResolutorXsd()
    if argumento.startswith("/"):
        card, obligatorio, legible = resolutor.resumen(argumento)
        print(f"{argumento}\n  {legible}\n  último paso: {card} | obligatorio en XSD: {'sí' if obligatorio else 'no'}")
    else:
        for i, (nombre, card) in enumerate(resolutor.secuencia_raiz(argumento), start=1):
            print(f"{i:>2}. {nombre:<40} {card}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
