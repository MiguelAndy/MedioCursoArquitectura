# 11. Fase 2 (segundo parcial): implementación de la emisión y del XML UBL 2.1

**Encargo:** implementar la generación de XML a partir de `UBL-Invoice-2.1.xsd`, en el lenguaje del dominio,
con pruebas unitarias de generación, sobre la especificación Spec-First de la fase 1.

**Resultado:** el servidor de [`servidor/`](../servidor/) emite facturas y boletas por `POST /api/v1/comprobantes`.
Para cada una calcula los tributos y genera el XML con clases derivadas del XSD oficial. Luego lo firma con
XMLDSig y lo valida contra el XSD. **El XML generado desde las solicitudes de `specs/ejemplos/json` es el mismo
documento de `specs/ubl`: su `DigestValue` coincide con el de los ejemplos firmados de la fase 1.**

## 11.1 Qué se implementó de la especificación

| Especificación (fase 1) | Implementación (fase 2) |
|---|---|
| ADR-03: Java 21 + Spring Boot 3 | `servidor/` (Maven, Spring Boot 3.5, Java 21) |
| ADR-02 y docs/06 § 6.3-6.6: MVC por fuera, hexágono por dentro | Paquetes `facturacion.{web, aplicacion.puerto.entrada, aplicacion.puerto.salida, aplicacion.servicio, dominio, infraestructura}` y `cpe.{api, aplicacion, infraestructura.ubl, infraestructura.firma}` |
| docs/06 § 6.5: reglas ArchUnit en CI | `ReglasDeArquitecturaTest`: las reglas del capítulo 6, más "cpe no depende de facturacion" y "módulos sin ciclos" |
| `x-puerto-entrada` del OpenAPI | `EmitirComprobanteUseCase`, `CalcularComprobanteUseCase`, `ConsultarComprobantesUseCase`, `DescargarDocumentoElectronicoUseCase` |
| Capa 1 (VAL-EST-01): JSON Schema | El controlador valida el cuerpo con **`specs/schemas/comprobante-emision.request.schema.json` sin copiarlo** (se empaqueta desde `specs/`) → 400 |
| Capa 2: reglas VAL-* | `ValidadorNormativo` (dominio puro) → 422 con `errores[].regla` (`problema.schema.json`) |
| Capa 3 (VAL-EST-02/03): XSD + perfil SUNAT | `ValidadorDocumentoAdapter` con **`specs/ubl/xsd-oasis-ubl-2.1` sin copiarlo** |
| docs/04 § 4.3: algoritmo del motor tributario | `MotorTributario`: función pura, `BigDecimal` HALF_UP, redondeo por ítem (ADR-07) |
| ADR-08: parámetros como datos | `TASA_IGV`, `TARIFA_ICBPER`, `FACTOR_ISC_PVP` y `UMBRAL_IDENTIFICACION_BOLETA` en `application.yml` vía `ParametrosTributariosPort` |
| Generación del XML desde el XSD | `jaxb-maven-plugin` genera ≈ 1 240 clases desde `specs/ubl/xsd-oasis-ubl-2.1/maindoc/UBL-Invoice-2.1.xsd` en cada compilación |
| `FirmadorXmlPort` (ADR-04) | `FirmadorXmlDsigAdapter`: la herramienta `tools/conformidad/FirmarXmlPrueba.java` convertida en adaptador |
| `DocumentoElectronicoPort` → `cpe.api.DocumentoElectronicoApi` (Tabla 6.3) | `DocumentoElectronicoCpeAdapter`: llamada en proceso; cpe recibe un modelo propio (`ComprobanteElectronico`) y no conoce a facturacion |
| VAL-NUM-02 e idempotencia (§ 4.4) | El número se consume **después** de validar. La misma `Idempotency-Key` con el mismo cuerpo devuelve 200 y el mismo comprobante; con otro cuerpo, 409 |

## 11.2 Flujo de emisión

```text
POST /api/v1/comprobantes  (Idempotency-Key)
  │
  ├─ capa 1  ValidadorContrato ── specs/schemas (JSON Schema 2020-12) ──────────── 400
  ├─ MapeadorSolicitud ─ VAL-ALC-01 (alcance de la fase 2) ──────────────────────── 422
  ├─ capa 2  ValidadorNormativo.validarAntesDelCalculo ─────────────────────────── 422
  ├─ MotorTributario.calcular  (ítems, totales y leyendas; función pura)
  ├─ capa 2  ValidadorNormativo.validarDespuesDelCalculo  (VAL-BOL-01, VAL-PAG-*) ─ 422
  ├─ NumeradorPort.siguiente  ← el correlativo se consume recién aquí
  ├─ DocumentoElectronicoPort → cpe.api.DocumentoElectronicoApi
  │     ├─ GeneradorUblAdapter  (InvoiceUblMapper → clases JAXB del XSD → XML)
  │     ├─ FirmadorXmlDsigAdapter  (XMLDSig enveloped en ext:ExtensionContent)
  │     └─ capa 3  ValidadorDocumentoAdapter  (XSD OASIS + perfil SUNAT) ─────────── 500, no se emite
  └─ ComprobanteRepository.guardar ─────────────────────────────────────────────── 201 + Location
```

## 11.3 Aclaraciones a la especificación encontradas al implementar

Spec-First significa que lo que se descubre al programar **vuelve a la especificación**:

| ID | Hallazgo | Decisión |
|---|---|---|
| A-01 | Las plantillas sin firma de `specs/ubl/fuentes-sin-firma` **no validan** contra el XSD: `ext:ExtensionContent` vacío es inválido (el XSD exige su contenido). La Tabla 4.1 decía validar "antes de firmar". | VAL-EST-02 se aplica al XML **firmado**. La prueba `lasPlantillasSinFirmaNoCumplenElXsd` deja constancia de esto. |
| A-02 | TR-A18 (dirección del adquirente) es condicional y no tiene regla. F001-1 la informa y F001-2 no, aunque ambas solicitudes la traen. | Se informa **solo si el comprobante tiene guías de remisión** (traslado de bienes). Minimiza datos personales (Ley 29733, § 6.11) y reproduce ambas referencias. |
| A-03 | El contrato admite funcionalidades que esta fase no implementa. | Regla **VAL-ALC-01**: se rechazan explícitamente (422), nunca se ignoran. Abarca notas 07/08, operaciones ≠ 0101, detracción, cargos y descuentos, afectación 17 (IVAP) e ISC con precio con impuestos. |
| A-04 | El orden de los `cac:TaxSubtotal` de cabecera no estaba escrito. | Se usa el orden de las referencias: 1000, 2000, 9995, 9996, 9997, 9998, 7152 (orden del enum `Tributo`). |
| A-05 | Persistencia: la fase 1 especifica PostgreSQL. | La fase 2 usa adaptadores **en memoria** detrás de `NumeradorPort` y `ComprobanteRepository`. Cambiarlos por JPA no toca el dominio ni la aplicación. |

## 11.4 Reglas implementadas

| Capa | Reglas |
|---|---|
| 1 (contrato) | VAL-EST-01 (todas las que expresa el JSON Schema: serie F/B, factura → RUC, crédito → cuotas, ISC por sistema, moneda ≠ PEN → tipo de cambio, gratuita → valor referencial) |
| 2 (dominio) | VAL-ID-02, VAL-ID-03, VAL-ID-06, VAL-BOL-01, VAL-NUM-01, VAL-NUM-02, VAL-FEC-01, VAL-FEC-03, VAL-MON-02, VAL-ITM-01, VAL-ITM-02, VAL-ITM-04, VAL-ITM-05, VAL-TRB-01 a 09, VAL-TOT-01 a 04, VAL-TOT-06, VAL-PAG-01 a 03, VAL-ALC-01 |
| 3 (documento) | VAL-EST-02 (XSD OASIS), VAL-EST-03 (UBLVersionID, CustomizationID, `InvoiceTypeCode@listID`, DocumentCurrencyCode, firma, AddressTypeCode del emisor, `unitCode`), VAL-ENV-01 (nombre del archivo) |

Las reglas restantes (VAL-ID-04 padrón, VAL-NOT-*, VAL-BAJ-*, VAL-ENV-02 a 06, VAL-DET-01, VAL-INV-*) corresponden
a módulos o integraciones de fases posteriores (envío a OSE/SUNAT, notas, inventario).

## 11.5 Evidencia: pruebas

`cd servidor && ./mvnw verify` → **124 pruebas, 0 fallas; cobertura de líneas 95,5 %** (mínimo exigido en el build: 90 %).

| Prueba | N.° | Qué demuestra |
|---|---:|---|
| `RegresionXmlReferenciaTest` | 10 | **RNF-03.** Las solicitudes 01, 03 y 02 generan XML equivalentes a las plantillas F001-1, F001-2 y B001-1. El **`DigestValue` es idéntico** al de `specs/ubl/ejemplos`. Además, el XML firmado valida contra el XSD y la firma se verifica. |
| `GeneracionXmlUblTest` | 20 | **Pruebas unitarias de generación**, por XPath. Cubren cabecera, leyendas 1000/1002, guía, orden de compra, firma y `cac:Signature`, emisor y adquirente (A-02), crédito y cuotas, el orden de los subtotales, ISC (`TierRange`), ICBPER (`BaseUnitMeasure`, `PerUnitAmount`), gratuitas (tipo de precio 02), valor unitario a 10 decimales y un comprobante en USD. |
| `ValidacionCapa3Test` | 6 | VAL-EST-02 y VAL-EST-03. Un XML sin firma no se acepta, se bloquea XXE y **nunca se entrega un XML inválido**. |
| `MotorTributarioTest` | 19 | Tabla 4.5, TC-07 y TC-09, ISC de los sistemas 01, 02 y 03, precio con IGV, gratuitas, redondeo HALF_UP por ítem y tasa como parámetro. |
| `ValidadorNormativoTest` | 24 | TC-02, 03, 04, 05, 06, 08 y 10, y las reglas de la capa 2. Todas las violaciones se informan juntas. |
| `MontoEnLetrasTest` | 23 | Leyenda 1000, incluidas las tres de los ejemplos. |
| `ComprobanteApiTest` | 17 | TC-01 y TC-12 (idempotencia); los 7 contraejemplos `invalidos/i01..i07` → 400 en el campo esperado; TC-03 y TC-04 → 422; VAL-ALC-01; **la respuesta cumple `comprobante.response.schema.json`**; consulta y descarga del XML firmado. |
| `ReglasDeArquitecturaTest` | 5 | Reglas de dependencia del § 6.5 y módulos sin ciclos. |

**Mutaciones manuales.** Si el IGV se calcula sin el ISC en la base (se viola VAL-TRB-05), fallan 6 pruebas de tres
niveles distintos: el motor, la generación y la regresión contra los XML de referencia.

## 11.6 Cómo ejecutarlo

```bash
cd servidor
./mvnw verify                                   # Windows: mvnw.cmd verify (genera clases del XSD, pruebas, cobertura)
./mvnw spring-boot:run                          # API en http://localhost:8080/api/v1

curl -X POST http://localhost:8080/api/v1/comprobantes \
     -H "Content-Type: application/json" -H "Idempotency-Key: demo-1" \
     -d @../specs/ejemplos/json/01-emitir-factura-credito.request.json
curl http://localhost:8080/api/v1/comprobantes/{id}/xml -o factura.xml
```

Sin configuración, la firma usa un **certificado de prueba autofirmado** que se genera al iniciar. No es válido
ante SUNAT. Para un certificado real: `ABC_CERTIFICADO_PFX`, `ABC_CERTIFICADO_CLAVE` y `ABC_CERTIFICADO_ALIAS`.

## 11.7 Pendiente para las fases siguientes

Envío a OSE/SUNAT con outbox y CDR (ADR-05/06), notas de crédito y débito (con `UBL-CreditNote/DebitNote-2.1.xsd`),
persistencia en PostgreSQL, numeración con `UPDATE … RETURNING`, seguridad JWT, el resto de endpoints del OpenAPI
y los clientes Compose Multiplatform (MSI/APK).
