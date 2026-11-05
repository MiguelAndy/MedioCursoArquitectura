# ABC Software Corp: especificación Spec-First (fase 1) e implementación de la emisión UBL 2.1 (fase 2)

**Universidad Nacional Agraria de la Selva · Facultad de Ingeniería en Informática y Sistemas**
**Arquitectura de Software · Grupo "A"**
**Fase 1:** examen de medio curso, 19 de octubre de 2026 · **Fase 2:** segundo parcial, 5 de noviembre de 2026
**Alumno:** Darikson Anyelo Enrique Debarbiere

Especificaciones técnicas, y su implementación en Java, para modernizar el software de ABC Software Corp, una mediana empresa mayorista y minorista de abarrotes, y llevarlo a la **facturación electrónica SUNAT en UBL 2.1** con **inventario formal**, **migración trazable del legado** y una **arquitectura MVC preparada para hexagonal y microservicios**, con distribución nativa en **Windows (MSI)** y **Android (APK)**.

> Todos los datos de este repositorio (RUC, razones sociales, direcciones) son **ficticios**. Los XML están firmados con un **certificado de prueba autofirmado**, no válido ante SUNAT.

## Fase 2 (segundo parcial): generación del XML a partir de `UBL-Invoice-2.1.xsd`

La fase 2 **implementa** esta especificación en [`servidor/`](servidor/), con Java 21 y Spring Boot 3 (ADR-03).
El detalle completo está en [docs/11-fase-2-implementacion.md](docs/11-fase-2-implementacion.md).

- **El XML se genera a partir del XSD.** En cada compilación, `jaxb-maven-plugin` genera las clases Java desde
  `specs/ubl/xsd-oasis-ubl-2.1/maindoc/UBL-Invoice-2.1.xsd`. El XML se produce con esas clases y nunca concatenando texto.
- **Los contratos se aplican, no se copian.** La capa 1 valida cada solicitud con `specs/schemas` (JSON Schema)
  y la capa 3 valida cada XML con los XSD de `specs/ubl`.
- **MVC por fuera, hexágono por dentro.** Hay controlador, puertos de entrada y salida, un dominio sin frameworks y
  adaptadores (JAXB, XMLDSig, memoria). Las reglas ArchUnit del § 6.5 se ejecutan en cada build.
- **Regresión RNF-03.** Las solicitudes de `specs/ejemplos/json` producen los mismos documentos de `specs/ubl`.
  El **`DigestValue` de la firma es idéntico** al de los ejemplos firmados de la fase 1.
- **124 pruebas unitarias y de API**, con cobertura de líneas del 95,5 % (el build falla si baja del 90 %).

```bash
cd servidor
./mvnw verify            # Windows: mvnw.cmd verify. Solo requiere JDK 21
./mvnw spring-boot:run   # http://localhost:8080/api/v1/comprobantes
```

## Resumen ejecutivo de la fase 1 (especificación)

- **Metodología Spec-First:** primero las reglas (`VAL-*`, `MIG-*`), luego los contratos (JSON Schema 2020-12, OpenAPI 3.1, AsyncAPI 3.0) y al final su validación automática. **En la fase 1 no se escribió lógica de negocio:** solo herramientas que verifican que los contratos sean correctos e implementables. La lógica de negocio se implementa en la fase 2, en [`servidor/`](servidor/), a partir de esos contratos.
- **Estudio del `UBL-Invoice-2.1.xsd`:** se usaron los XSD oficiales de OASIS. 21 de 56 datos exigidos por SUNAT son opcionales en el XSD; esa diferencia ("perfil SUNAT") está marcada en la matriz con ▲.
- **Catálogo 8:** se cubren el Anexo N.° 8 completo (catálogos como datos versionados) y el Catálogo N.° 08 (sistemas de cálculo del ISC, con fórmula, ubicación `cbc:TierRange` y ejemplo firmado).
- **Arquitectura:** "MVC por fuera, hexágono por dentro". Cada endpoint declara su puerto de entrada (`x-puerto-entrada`) y los eventos tienen contrato propio, de modo que extraer microservicios es cambiar adaptadores.
- **Evidencia:** 4 XML UBL 2.1 firmados (factura a crédito con bonificación, factura con ISC, boleta con ICBPER y exonerado, nota de crédito) que **validan contra el XSD oficial** y **cuadran al céntimo**.

## Del enunciado a este repositorio

| Enunciado | Dónde se responde |
|---|---|
| 1. Descripción del caso: brechas (RUC, impuestos, XML firmado, OSE/SUNAT) | [docs/01-analisis-del-caso.md](docs/01-analisis-del-caso.md) |
| Normas RS 097-2012, RS 234-2006, masificación, UBL/OASIS, `UBL-Invoice-2.1.xsd` | [docs/02-marco-normativo-y-estandar-ubl.md](docs/02-marco-normativo-y-estandar-ubl.md) |
| 2. Legado y migración de datos | [docs/09-migracion-del-legado.md](docs/09-migracion-del-legado.md) |
| 2. Normativa tributaria y **catálogo 8** | [docs/02](docs/02-marco-normativo-y-estandar-ubl.md) (sección 2.5) y [docs/04-reglas-de-negocio-y-validaciones.md](docs/04-reglas-de-negocio-y-validaciones.md) |
| 2. Arquitectura base y futura (MVC → hexagonal → microservicios) | [docs/06-arquitectura-mvc-hexagonal.md](docs/06-arquitectura-mvc-hexagonal.md) |
| 2. Distribución multiplataforma (MSI y APK) | [docs/06](docs/06-arquitectura-mvc-hexagonal.md) (sección 6.8) |
| 3. Descubrimiento de los casos de uso (opiniones de usuarios, Nubefact, DEMO) | [docs/03-casos-de-uso.md](docs/03-casos-de-uso.md) |
| **4. Entregable: modelo de dominio y contratos (DTOs/Schemas)** | [docs/05-modelo-de-dominio-y-contratos.md](docs/05-modelo-de-dominio-y-contratos.md) + [specs/schemas/](specs/schemas/) |
| **4. Entregable: diagramas MVC con visión hexagonal** | [docs/06-arquitectura-mvc-hexagonal.md](docs/06-arquitectura-mvc-hexagonal.md) + [diagramas/](diagramas/) |
| **4. Entregable: endpoints y servicios (contratos API)** | [docs/07-contratos-api.md](docs/07-contratos-api.md) + [specs/openapi/abc-comercial-api.yaml](specs/openapi/abc-comercial-api.yaml) + [specs/asyncapi/](specs/asyncapi/) |
| **4. Entregable: matriz de trazabilidad SUNAT ↔ UBL** | [docs/08-matriz-trazabilidad-normativa.md](docs/08-matriz-trazabilidad-normativa.md) + [specs/trazabilidad/matriz-trazabilidad.xlsx](specs/trazabilidad/matriz-trazabilidad.xlsx) |
| Informe consolidado de la fase 1 | [entregable/](entregable/) (Word y PDF) |
| **Segundo parcial: generación del XML a partir del XSD Invoice 2.1, en el lenguaje del dominio** | [servidor/](servidor/) + [docs/11-fase-2-implementacion.md](docs/11-fase-2-implementacion.md) |
| **Segundo parcial: pruebas unitarias de generación** | [GeneracionXmlUblTest](servidor/src/test/java/pe/abc/comercial/cpe/GeneracionXmlUblTest.java) + [RegresionXmlReferenciaTest](servidor/src/test/java/pe/abc/comercial/RegresionXmlReferenciaTest.java) |

## Estructura

```text
docs/                         10 capítulos de la especificación + 11 (implementación de la fase 2)
servidor/                     FASE 2: servidor Java 21 + Spring Boot 3 (emisión, motor tributario, UBL, firma)
  src/main/java/pe/abc/comercial/facturacion/   web · aplicacion (puertos) · dominio · infraestructura
  src/main/java/pe/abc/comercial/cpe/           api · aplicacion · infraestructura/{ubl, firma}
  src/test/java/                                124 pruebas (regresión specs/ubl, generación, reglas, API, ArchUnit)
specs/
  schemas/                    contratos de datos: JSON Schema 2020-12 (11 archivos)
  openapi/                    contrato REST: OpenAPI 3.1 (43 rutas, 50 operaciones)
  asyncapi/                   contrato de eventos: AsyncAPI 3.0
  ejemplos/json/              solicitudes y respuestas válidas + invalidos/ (contraejemplos)
  ubl/fuentes-sin-firma/             XML UBL 2.1 sin firma (fuente)
  ubl/ejemplos/               XML UBL 2.1 firmados (XMLDSig) y validados contra el XSD
  ubl/xsd-oasis-ubl-2.1/      XSD oficiales de OASIS (sin modificar)
  trazabilidad/               matriz (YAML fuente → XLSX, CSV y tablas de docs/08)
  migracion/                  reglas declarativas de mapeo del legado
diagramas/fuentes/            15 diagramas Mermaid (.mmd) → svg/ (Markdown) y png/ (informe)
tools/                        validación y generación (no es lógica de negocio)
entregable/                   informe consolidado .docx y .pdf
```

## Cómo verificar

Se necesita Python 3.11 o superior; para firmar, JDK 11 o superior; para los diagramas, Node.js y Chrome o Edge.

```bash
pip install -r tools/requirements.txt
python tools/validar_specs.py            # esquemas, ejemplos, XSD OASIS, firma, cuadres, OpenAPI y AsyncAPI
python tools/validar_specs.py --firmar   # además re-firma las plantillas UBL con un certificado de prueba
python tools/xsd_cardinalidad.py Invoice # secuencia raíz de UBL-Invoice-2.1.xsd con cardinalidades
python tools/generar_matrices.py         # regenera la matriz (XLSX, CSV, docs/08) y la tabla de endpoints (docs/07)
python tools/renderizar_diagramas.py     # regenera diagramas/svg y diagramas/png
python tools/construir_informe.py        # regenera entregable/*.docx y *.pdf (requiere Quarto/Pandoc y MS Word)
```

Resultado esperado de la validación: `Resumen: 40 OK, 0 con error` (44 con `--firmar`).
