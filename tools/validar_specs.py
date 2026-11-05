"""Valida los artefactos Spec-First del repositorio (contratos antes que lógica de negocio).

Comprobaciones:
  1. Cada JSON Schema es válido contra el metaesquema Draft 2020-12.
  2. Los ejemplos JSON válidos cumplen su contrato y los inválidos fallan por la regla esperada.
  3. Los XML UBL 2.1 de ejemplo validan contra los XSD oficiales de OASIS (incluida la firma).
  4. Los importes de cada XML cuadran (líneas vs. totales), según docs/04 (VAL-TOT-*).
  5. La especificación OpenAPI 3.1 es válida y resuelve sus $ref externos.
  6. El documento AsyncAPI es YAML legible y declara los canales esperados.

Uso:
  python tools/validar_specs.py            # valida
  python tools/validar_specs.py --firmar   # además re-firma las plantillas UBL con un certificado de prueba

Requisitos: pip install -r tools/requirements.txt ; para --firmar: JDK 11+ (java y keytool en el PATH o JAVA_HOME).
"""
from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
import urllib.parse
import urllib.request
from decimal import Decimal
from pathlib import Path

import yaml
from jsonschema import Draft202012Validator
from lxml import etree
from referencing import Registry, Resource
from referencing.jsonschema import DRAFT202012

RAIZ = Path(__file__).resolve().parent.parent
SCHEMAS = RAIZ / "specs" / "schemas"
EJEMPLOS_JSON = RAIZ / "specs" / "ejemplos" / "json"
UBL = RAIZ / "specs" / "ubl"
XSD = UBL / "xsd-oasis-ubl-2.1" / "maindoc"
OPENAPI = RAIZ / "specs" / "openapi" / "abc-comercial-api.yaml"
ASYNCAPI = RAIZ / "specs" / "asyncapi" / "abc-comercial-eventos.yaml"

NS = {
    "cac": "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2",
    "cbc": "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2",
    "ds": "http://www.w3.org/2000/09/xmldsig#",
}

# Ejemplo válido -> (archivo de esquema, puntero al $def o "" para la raíz, ¿es un arreglo?)
EJEMPLOS_VALIDOS = {
    "01-emitir-factura-credito.request.json": ("comprobante-emision.request.schema.json", "", False),
    "02-emitir-boleta-icbper.request.json": ("comprobante-emision.request.schema.json", "", False),
    "03-emitir-factura-isc.request.json": ("comprobante-emision.request.schema.json", "", False),
    "04-emitir-nota-credito.request.json": ("comprobante-emision.request.schema.json", "", False),
    "05-comprobante-emitido.response.json": ("comprobante.response.schema.json", "", False),
    "06-ficha-legado.json": ("migracion.schema.json", "#/$defs/FichaLegado", True),
}

# Ejemplo inválido -> fragmento que debe aparecer en la ruta del error (prueba que falla por la razón correcta)
EJEMPLOS_INVALIDOS = {
    "i01-boleta-con-serie-de-factura.json": "serie",
    "i02-factura-a-cliente-con-dni.json": "tipoDocumento",
    "i03-gratuita-con-valor-unitario.json": "items",
    "i04-nota-credito-sin-referencia.json": "documentoReferencia",
    "i05-credito-sin-cuotas.json": "formaPago",
    "i06-isc-al-valor-sin-tasa.json": "isc",
    "i07-moneda-extranjera-sin-tipo-cambio.json": "tipoCambio",
}

resultados: list[tuple[str, bool, str]] = []


def registrar(nombre: str, ok: bool, detalle: str = "") -> None:
    resultados.append((nombre, ok, detalle))
    print(f"[{'OK ' if ok else 'ERR'}] {nombre}{' - ' + detalle if detalle else ''}")


def recuperar(uri: str) -> Resource:
    ruta = Path(urllib.request.url2pathname(urllib.parse.urlparse(uri).path))
    return Resource.from_contents(json.loads(ruta.read_text(encoding="utf-8")), default_specification=DRAFT202012)


REGISTRO = Registry(retrieve=recuperar)


def validador(esquema_archivo: str, puntero: str) -> Draft202012Validator:
    uri = (SCHEMAS / esquema_archivo).as_uri()
    esquema = {"$ref": uri + puntero} if puntero else {"$ref": uri}
    return Draft202012Validator(esquema, registry=REGISTRO, format_checker=Draft202012Validator.FORMAT_CHECKER)


def validar_esquemas() -> None:
    for archivo in sorted(SCHEMAS.glob("*.schema.json")):
        try:
            Draft202012Validator.check_schema(json.loads(archivo.read_text(encoding="utf-8")))
            registrar(f"metaesquema {archivo.name}", True)
        except Exception as exc:  # noqa: BLE001 - se informa cualquier fallo de esquema
            registrar(f"metaesquema {archivo.name}", False, str(exc)[:200])


def validar_ejemplos_json() -> None:
    for archivo, (esquema, puntero, es_arreglo) in EJEMPLOS_VALIDOS.items():
        datos = json.loads((EJEMPLOS_JSON / archivo).read_text(encoding="utf-8"))
        v = validador(esquema, puntero)
        instancias = datos if es_arreglo else [datos]
        errores = [e for inst in instancias for e in v.iter_errors(inst)]
        registrar(f"ejemplo válido {archivo}", not errores,
                  "; ".join(f"{list(e.absolute_path)}: {e.message[:120]}" for e in errores[:3]))
    v = validador("comprobante-emision.request.schema.json", "")
    for archivo, esperado in EJEMPLOS_INVALIDOS.items():
        datos = json.loads((EJEMPLOS_JSON / "invalidos" / archivo).read_text(encoding="utf-8"))
        errores = list(v.iter_errors(datos))
        rutas = [".".join(str(p) for p in e.absolute_path) for e in errores]
        rutas += [".".join(str(p) for p in c.absolute_path) for e in errores for c in (e.context or [])]
        mensajes = " ".join(e.message for e in errores)
        motivo_correcto = any(esperado in r for r in rutas) or esperado in mensajes
        registrar(f"ejemplo inválido {archivo}", bool(errores) and motivo_correcto,
                  f"rechazado en: {sorted(set(r for r in rutas if r))[:4]}")


def buscar_jdk(herramienta: str) -> str:
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidato = Path(java_home) / "bin" / (herramienta + (".exe" if os.name == "nt" else ""))
        if candidato.exists():
            return str(candidato)
    ruta = shutil.which(herramienta)
    if not ruta:
        raise SystemExit(f"No se encontró '{herramienta}'. Instale un JDK 11+ o defina JAVA_HOME.")
    return ruta


def firmar_plantillas() -> None:
    keytool, java = buscar_jdk("keytool"), buscar_jdk("java")
    firmador = RAIZ / "tools" / "conformidad" / "FirmarXmlPrueba.java"
    (UBL / "ejemplos").mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        almacen, clave = Path(tmp) / "certificado-prueba.p12", "prueba-no-valida-sunat"
        subprocess.run([keytool, "-genkeypair", "-alias", "abc-prueba", "-keyalg", "RSA", "-keysize", "2048",
                        "-validity", "365", "-storetype", "PKCS12", "-keystore", str(almacen),
                        "-storepass", clave, "-keypass", clave,
                        "-dname", "CN=ABC SOFTWARE CORP SAC - CERTIFICADO DE PRUEBA, OU=20123456786, O=ABC SOFTWARE CORP SAC, C=PE"],
                       check=True, capture_output=True)
        carpeta_fuente = (UBL / "fuentes-sin-firma") if (UBL / "fuentes-sin-firma").exists() else (UBL / "plantillas")
        for plantilla in sorted(carpeta_fuente.glob("*.xml")):
            salida = UBL / "ejemplos" / plantilla.name
            r = subprocess.run([java, str(firmador), str(plantilla), str(salida), str(almacen), clave, "abc-prueba"],
                               capture_output=True, text=True)
            registrar(f"firma {plantilla.name}", r.returncode == 0, (r.stdout or r.stderr).strip()[:220])


def xsd_para(raiz: etree._Element) -> etree.XMLSchema:
    nombre = etree.QName(raiz).localname
    return etree.XMLSchema(etree.parse(str(XSD / f"UBL-{nombre}-2.1.xsd")))


def d(nodo: etree._Element | None) -> Decimal:
    return Decimal(nodo.text) if nodo is not None else Decimal("0")


def cuadrar_importes(doc: etree._ElementTree) -> list[str]:
    """Cuadres mínimos VAL-TOT-01..05 sobre el XML (no reemplaza al motor tributario)."""
    problemas = []
    raiz = doc.getroot()
    linea_tag = "cac:InvoiceLine" if etree.QName(raiz).localname == "Invoice" else "cac:CreditNoteLine"
    total_onerosas = Decimal("0")
    for linea in raiz.findall(linea_tag, NS):
        num = linea.findtext("cbc:ID", namespaces=NS)
        valor_venta = d(linea.find("cbc:LineExtensionAmount", NS))
        subtotales = linea.findall("cac:TaxTotal/cac:TaxSubtotal", NS)
        suma = sum((d(s.find("cbc:TaxAmount", NS)) for s in subtotales), Decimal("0"))
        if suma != d(linea.find("cac:TaxTotal/cbc:TaxAmount", NS)):
            problemas.append(f"línea {num}: TaxTotal de la línea no es la suma de sus TaxSubtotal")
        es_gratuita = any(s.findtext("cac:TaxCategory/cac:TaxScheme/cbc:ID", namespaces=NS) == "9996" for s in subtotales)
        if not es_gratuita:
            total_onerosas += valor_venta
        for s in subtotales:
            pct = s.findtext("cac:TaxCategory/cbc:Percent", namespaces=NS)
            base = s.find("cbc:TaxableAmount", NS)
            if pct is not None and base is not None:
                esperado = (d(base) * Decimal(pct) / 100).quantize(Decimal("0.01"))
                if abs(esperado - d(s.find("cbc:TaxAmount", NS))) > Decimal("0.01"):
                    problemas.append(f"línea {num}: tributo {esperado} != {s.findtext('cbc:TaxAmount', namespaces=NS)}")
    lmt = raiz.find("cac:LegalMonetaryTotal", NS)
    if total_onerosas != d(lmt.find("cbc:LineExtensionAmount", NS)):
        problemas.append(f"LineExtensionAmount {lmt.findtext('cbc:LineExtensionAmount', namespaces=NS)} != suma onerosas {total_onerosas}")
    tax_total = raiz.find("cac:TaxTotal", NS)
    cobrados = sum((d(s.find("cbc:TaxAmount", NS)) for s in tax_total.findall("cac:TaxSubtotal", NS)
                    if s.findtext("cac:TaxCategory/cac:TaxScheme/cbc:ID", namespaces=NS) != "9996"), Decimal("0"))
    if cobrados != d(tax_total.find("cbc:TaxAmount", NS)):
        problemas.append("TaxTotal global no es la suma de los tributos cobrados (excluye 9996)")
    if d(lmt.find("cbc:LineExtensionAmount", NS)) + d(tax_total.find("cbc:TaxAmount", NS)) != d(lmt.find("cbc:TaxInclusiveAmount", NS)):
        problemas.append("TaxInclusiveAmount != LineExtensionAmount + TaxTotal")
    if d(lmt.find("cbc:TaxInclusiveAmount", NS)) != d(lmt.find("cbc:PayableAmount", NS)):
        problemas.append("PayableAmount != TaxInclusiveAmount (no hay descuentos, cargos ni anticipos en los ejemplos)")
    return problemas


def validar_ubl() -> None:
    ejemplos = sorted((UBL / "ejemplos").glob("*.xml"))
    if not ejemplos:
        registrar("UBL firmados", False, "no hay XML en specs/ubl/ejemplos; ejecute con --firmar")
        return
    for archivo in ejemplos:
        doc = etree.parse(str(archivo))
        esquema = xsd_para(doc.getroot())
        ok = esquema.validate(doc)
        registrar(f"XSD OASIS {archivo.name}", ok, "" if ok else str(esquema.error_log.last_error)[:220])
        firmado = doc.find(".//ds:Signature/ds:SignatureValue", NS) is not None
        registrar(f"firma presente {archivo.name}", firmado)
        problemas = cuadrar_importes(doc)
        registrar(f"cuadre de importes {archivo.name}", not problemas, "; ".join(problemas))


def validar_openapi() -> None:
    if not OPENAPI.exists():
        registrar("OpenAPI", False, f"no existe {OPENAPI}")
        return
    from openapi_spec_validator import validate
    from openapi_spec_validator.readers import read_from_filename

    spec, base_uri = read_from_filename(str(OPENAPI))
    try:
        validate(spec, base_uri=base_uri)
        operaciones = sum(1 for p in spec["paths"].values() for m in p if m in {"get", "post", "put", "patch", "delete"})
        registrar("OpenAPI 3.1 abc-comercial-api.yaml", True, f"{len(spec['paths'])} rutas, {operaciones} operaciones")
    except Exception as exc:  # noqa: BLE001
        registrar("OpenAPI 3.1 abc-comercial-api.yaml", False, str(exc)[:300])


def validar_asyncapi() -> None:
    if not ASYNCAPI.exists():
        registrar("AsyncAPI", False, f"no existe {ASYNCAPI}")
        return
    doc = yaml.safe_load(ASYNCAPI.read_text(encoding="utf-8"))
    ok = str(doc.get("asyncapi", "")).startswith("3.") and len(doc.get("channels", {})) >= 5
    registrar("AsyncAPI abc-comercial-eventos.yaml", ok, f"{len(doc.get('channels', {}))} canales")


def validar_yaml_de_reglas() -> None:
    reglas = yaml.safe_load((RAIZ / "specs" / "migracion" / "reglas-mapeo-legado.yaml").read_text(encoding="utf-8"))
    esperadas = {f"MIG-{n:02d}" for n in range(1, 13)}
    faltan = esperadas - set(reglas.get("reglas", {}))
    registrar("reglas de migración MIG-01..12", not faltan, f"faltan: {sorted(faltan)}" if faltan else "")
    matriz = yaml.safe_load((RAIZ / "specs" / "trazabilidad" / "matriz-trazabilidad.yaml").read_text(encoding="utf-8"))
    ids = [f["id"] for clave in ("matriz_a", "matriz_notas", "matriz_b", "matriz_c", "matriz_d") for f in matriz[clave]]
    registrar("matriz de trazabilidad (ids únicos)", len(ids) == len(set(ids)), f"{len(ids)} filas")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--firmar", action="store_true", help="re-firma las plantillas UBL con un certificado de prueba")
    args = parser.parse_args()
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    validar_esquemas()
    validar_ejemplos_json()
    if args.firmar:
        firmar_plantillas()
    validar_ubl()
    validar_openapi()
    validar_asyncapi()
    validar_yaml_de_reglas()
    fallos = [r for r in resultados if not r[1]]
    print(f"\nResumen: {len(resultados) - len(fallos)} OK, {len(fallos)} con error")
    return 1 if fallos else 0


if __name__ == "__main__":
    raise SystemExit(main())
