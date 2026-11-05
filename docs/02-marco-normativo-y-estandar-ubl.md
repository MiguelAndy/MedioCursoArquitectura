# 2. Marco normativo y estudio del estándar UBL 2.1

## 2.1 Normas aplicables

**Tabla 2.1.** *Normas y su impacto en el diseño*

| Norma | Qué regula | Impacto en el diseño |
|---|---|---|
| D. L. N.° 25632 y RS N.° 007-99/SUNAT | Ley Marco y Reglamento de Comprobantes de Pago: tipos de comprobante y requisitos mínimos | Tipos 01, 03, 07 y 08; requisitos de la representación impresa |
| **RS N.° 097-2012/SUNAT** y modificatorias | SEE - Del Contribuyente: el comprobante se emite desde el sistema del contribuyente en XML UBL firmado y se envía a SUNAT u OSE | Es el núcleo del módulo `cpe`. **Anexo N.° 8**: catálogos de códigos. **Anexo N.° 9-A**: estándar UBL 2.1 (incorporado por la RS 114-2019/SUNAT) |
| RS N.° 117-2017/SUNAT | SEE - OSE: un operador valida el comprobante y emite el CDR | Canal OSE intercambiable con SUNAT (ADR-06) |
| RS N.° 155-2017/SUNAT | Designación masiva de emisores electrónicos | ABC está obligada a emitir electrónicamente (SUP-01) |
| RS N.° 150-2019/SUNAT | Adecuación de los comprobantes al ICBPER (tributo 7152) | `BaseUnitMeasure` + `PerUnitAmount` por ítem (VAL-TRB-06) |
| RS N.° 193-2020/SUNAT | Forma de pago contado o crédito, monto neto pendiente y cuotas | `cac:PaymentTerms` FormaPago / CuotaNNN (VAL-PAG-*) |
| RS N.° 000005-2023/SUNAT | Ajusta requisitos de la factura y el Catálogo N.° 51 (p. ej., 0112 solo con DNI o RUC; guion como tipo de documento en supuestos específicos) | VAL-ID-05; catálogo 51 vigente |
| RS N.° 000075-2026/SUNAT (vigente desde el 01-06-2026) | La condición de emisor electrónico y la obligación del SIRE nacen desde la inscripción en el RUC | Registro de Ventas alineado al RVIE del SIRE |
| RS N.° 000048-2026/SUNAT (postergada al 01-01-2027 por la RS N.° 000143-2026/SUNAT) | En el SEE-OSE, cada nota de crédito o débito modifica **un solo** comprobante; 7 días para el resumen diario de baja | Desde ya: `documentoReferencia` es único (VAL-NOT-01) |
| RS N.° 000049-2026/SUNAT (postergada por la RS N.° 000141-2026/SUNAT) | PEI WEB: envío del resumen de comprobantes impresos, incluido el motivo "baja o nula conexión a internet" | Contingencia en la región (SUP-07, RSK-03) |
| **RS N.° 234-2006/SUNAT** | Libros y registros vinculados a asuntos tributarios: Formato 12.1 (Inventario Permanente en Unidades Físicas), Formato 14.1 (Registro de Ventas) y Tablas 2, 5, 6, 10 y 12 | Módulos `registros` e `inventario`; destino de la migración |
| TUO de la Ley del IGV e ISC (D. S. N.° 055-99-EF) | IGV (16 %) + IPM (2 %). El ISC grava al productor, al importador y a sus vinculados; tres sistemas de cálculo (art. 56); Apéndices III y IV | Motor tributario (docs/04); Catálogo N.° 08 |
| Ley N.° 30884 | Crea el ICBPER; desde 2023 la tarifa es S/ 0,50 por bolsa | Parámetro `TARIFA_ICBPER` con vigencia |
| Ley N.° 29733 | Protección de datos personales (DNI, correo y teléfono de clientes) | Mínimo de datos, cifrado en tránsito, auditoría de accesos |
| Código Tributario | Conservar libros y documentos mientras el tributo no prescriba | Conservar XML, CDR y PDF al menos 5 años (RNF-05) |
| OASIS UBL 2.1 (ISO/IEC 19845:2015) y W3C XMLDSig | Sintaxis XML de los documentos comerciales y de la firma | `specs/ubl/xsd-oasis-ubl-2.1`; `tools/conformidad` |

## 2.2 Sistema de emisión elegido

SUNAT ofrece tres caminos:

- **SEE - SOL:** emisión manual en el portal.
- **SEE - Del Contribuyente:** el sistema propio genera, firma y envía el XML. El Facturador SUNAT es una versión gratuita de este camino.
- **SEE - OSE:** el sistema propio envía el XML a un operador autorizado.

Como ABC necesita volumen, integración con inventario y operación multipunto, el diseño adopta **sistema propio + canal parametrizable**: envío a un OSE o directo a SUNAT, según la condición del contribuyente (VAL-ENV-06). Cambiar de canal no toca el dominio: es otro adaptador del mismo puerto `CanalEnvioCpePort`.

## 2.3 Estudio del estándar UBL 2.1 y del archivo UBL-Invoice-2.1.xsd

### Arquitectura de los esquemas

- `maindoc/UBL-Invoice-2.1.xsd`: el documento raíz `Invoice`, que SUNAT usa para la **factura (01)** y la **boleta (03)**. Las notas usan `UBL-CreditNote-2.1.xsd` y `UBL-DebitNote-2.1.xsd`.
- `common/UBL-CommonAggregateComponents-2.1.xsd` (`cac:`): agregados como Party, TaxTotal o InvoiceLine. `common/UBL-CommonBasicComponents-2.1.xsd` (`cbc:`): elementos simples como ID, Amount o Percent.
- `common/UBL-CommonExtensionComponents-2.1.xsd` (`ext:`): punto de extensión `UBLExtensions/UBLExtension/ExtensionContent`. Su contenido se valida en modo **lax**, y ahí SUNAT aloja la firma `ds:Signature`.
- Tipos de datos (UDT/QDT, CCTS): `AmountType` exige el atributo `currencyID`; `QuantityType` admite `unitCode`.

### Hallazgos del estudio

1. El XSD **solo** exige seis elementos raíz: `cbc:ID`, `cbc:IssueDate`, `cac:AccountingSupplierParty`, `cac:AccountingCustomerParty`, `cac:LegalMonetaryTotal` (con `PayableAmount`) y al menos un `cac:InvoiceLine`. Todo lo demás es `0..1` o `0..n`.
2. El **perfil SUNAT** (Anexo N.° 9-A) vuelve obligatorios varios elementos que el XSD deja opcionales: `UBLVersionID` ("2.1"), `CustomizationID` ("2.0"), `InvoiceTypeCode` con su `@listID` (tipo de operación), `DocumentCurrencyCode`, la firma, `AddressTypeCode` del emisor y el `unitCode` de cada ítem. Por eso el XSD **no basta**: hace falta una segunda capa de reglas (docs/04).
3. El orden de los elementos es obligatorio (`xsd:sequence`), así que el generador UBL debe respetarlo. Ejemplo: en `TaxCategory`, `Percent` va antes que `TaxExemptionReasonCode`, y este antes que `TierRange` y `TaxScheme`.
4. Los códigos de catálogo no están en el XSD: son texto libre calificado con `listID`, `listURI` o `schemeID`. Los valores válidos los impone SUNAT (Anexo N.° 8), y el sistema los valida como datos.
5. En SUNAT la factura y la boleta comparten el esquema `Invoice` y se distinguen por `InvoiceTypeCode` (01 o 03). En la nota de crédito, el motivo va en `cac:DiscrepancyResponse/cbc:ResponseCode` (Catálogo N.° 09) y el comprobante modificado en `cac:BillingReference`.
6. Los cuatro XML de ejemplo del repositorio validan contra estos XSD oficiales, con la firma incluida (ver docs/05, sección 5.7).

**Tabla 2.2.** *Secuencia raíz de UBL-Invoice-2.1.xsd y su uso en el perfil SUNAT*

| # | Elemento | Card. XSD | Uso SUNAT (Anexo 9-A) |
|---|---|---|---|
| 1 | ext:UBLExtensions | 0..1 | **M**: aloja la firma (campo 9) |
| 2 | cbc:UBLVersionID | 0..1 | **M**: "2.1" (campo 1) |
| 3 | cbc:CustomizationID | 0..1 | **M**: "2.0" (campo 2) |
| 4 | cbc:ProfileID | 0..1 | No se usa (el tipo de operación va en `InvoiceTypeCode/@listID`) |
| 5 | cbc:ProfileExecutionID | 0..1 | No se usa |
| 6 | cbc:ID | 1..1 | **M**: serie-número (campo 3) |
| 7 | cbc:CopyIndicator | 0..1 | No se usa |
| 8 | cbc:UUID | 0..1 | No se usa |
| 9 | cbc:IssueDate | 1..1 | **M**: fecha de emisión (campo 4) |
| 10 | cbc:IssueTime | 0..1 | C: hora de emisión (campo 5); el sistema siempre la envía |
| 11 | cbc:DueDate | 0..1 | C: fecha de vencimiento (campo 8) |
| 12 | cbc:InvoiceTypeCode | 0..1 | **M**: Cat. 01 (campo 6) + `@listID` Cat. 51 (campo 58) |
| 13 | cbc:Note | 0..n | C: leyendas, Cat. 52 (campo 57) |
| 14 | cbc:TaxPointDate | 0..1 | No se usa |
| 15 | cbc:DocumentCurrencyCode | 0..1 | **M**: Cat. 02 (campo 7) |
| 16–21 | TaxCurrencyCode, PricingCurrencyCode, PaymentCurrencyCode, PaymentAlternativeCurrencyCode, AccountingCostCode, AccountingCost | 0..1 | No se usan |
| 22 | cbc:LineCountNumeric | 0..1 | Opcional; el sistema lo envía |
| 23 | cbc:BuyerReference | 0..1 | No se usa |
| 24 | cac:InvoicePeriod | 0..n | C: ciclo de facturación (servicios públicos; ABC no lo usa) |
| 25 | cac:OrderReference | 0..1 | C: orden de compra (campo 59) |
| 26 | cac:BillingReference | 0..n | No se usa en factura (sí en las notas) |
| 27 | cac:DespatchDocumentReference | 0..n | C: guía de remisión relacionada (campo 22) |
| 28–30 | ReceiptDocumentReference, StatementDocumentReference, OriginatorDocumentReference | 0..n | No se usan |
| 31 | cac:ContractDocumentReference | 0..n | C: servicios públicos (no aplica) |
| 32 | cac:AdditionalDocumentReference | 0..n | C: otros documentos (Cat. 12) y anticipos (campos 23 y 65) |
| 33 | cac:ProjectReference | 0..n | No se usa |
| 34 | cac:Signature | 0..n | **M**: referencia a la firma (campo 9) |
| 35 | cac:AccountingSupplierParty | 1..1 | **M**: emisor (campos 10–16) |
| 36 | cac:AccountingCustomerParty | 1..1 | **M**: adquirente (campos 17–20) |
| 37 | cac:PayeeParty | 0..1 | No se usa |
| 38 | cac:BuyerCustomerParty | 0..1 | C: quien opera por cuenta del adquirente (campo 21) |
| 39–40 | SellerSupplierParty, TaxRepresentativeParty | 0..1 | No se usan |
| 41 | cac:Delivery | 0..n | C: venta itinerante y factura guía (campos 14 y 67–83) |
| 42 | cac:DeliveryTerms | 0..1 | C: Incoterm (exportación, campo 62) |
| 43 | cac:PaymentMeans | 0..n | C: medio de pago y cuenta para la detracción |
| 44 | cac:PaymentTerms | 0..n | **M en factura**: forma de pago y cuotas (RS 193-2020); detracción; percepción (campo 64) |
| 45 | cac:PrepaidPayment | 0..n | C: anticipos |
| 46 | cac:AllowanceCharge | 0..n | C: cargos y descuentos globales, Cat. 53 (campos 50, 60 y 63) |
| 47–50 | TaxExchangeRate, PricingExchangeRate, PaymentExchangeRate, PaymentAlternativeExchangeRate | 0..1 | No se usan |
| 51 | cac:TaxTotal | 0..n | **M**: un solo TaxTotal con un subtotal por tributo (campos 40–49) |
| 52 | cac:WithholdingTaxTotal | 0..n | No se usa |
| 53 | cac:LegalMonetaryTotal | 1..1 | **M**: totales (campos 51–56 y 66) |
| 54 | cac:InvoiceLine | 1..n | **M**: ítems (campos 24–39) |

*Nota.* Cardinalidades extraídas por `tools/xsd_cardinalidad.py` de los XSD OASIS; uso SUNAT según el Anexo N.° 9-A (RS 114-2019/SUNAT). M = mandatorio; C = condicional.

## 2.4 Catálogos del Anexo N.° 8 usados por el sistema

**Tabla 2.3.** *Catálogos SUNAT y su ubicación en UBL*

| Catálogo | Contenido | Elemento UBL | Contrato (`catalogos-sunat.schema.json`) |
|---|---|---|---|
| 01 | Tipo de documento | `cbc:InvoiceTypeCode`, `cbc:DocumentTypeCode` | `Cat01*` |
| 02 | Moneda (ISO 4217) | `cbc:DocumentCurrencyCode`, `@currencyID` | `Cat02Moneda` |
| 03 | Unidad de medida (UN/ECE Rec. 20) | `cbc:InvoicedQuantity/@unitCode` | `Cat03UnidadMedida` |
| 05 | Tributos (IGV 1000, IVAP 1016, ISC 2000, ICBPER 7152, 9995–9999) | `cac:TaxScheme/cbc:ID`, `Name`, `TaxTypeCode` | `Cat05Tributo` |
| 06 | Tipo de documento de identidad | `cbc:ID/@schemeID` | `Cat06TipoDocumentoIdentidad` |
| 07 | Afectación del IGV | `cbc:TaxExemptionReasonCode` | `Cat07TipoAfectacionIgv` |
| **08** | **Sistema de cálculo del ISC** | **`cac:TaxCategory/cbc:TierRange`** | **`Cat08SistemaIsc`** |
| 09 / 10 | Motivo de nota de crédito / débito | `cac:DiscrepancyResponse/cbc:ResponseCode` | `Cat09*`, `Cat10*` |
| 12 | Documentos relacionados | `cac:AdditionalDocumentReference/cbc:DocumentTypeCode` | datos |
| 13 | Ubigeo | `cac:RegistrationAddress/cbc:ID` | `Ubigeo` |
| 16 | Tipo de precio (01 con IGV; 02 valor referencial) | `cbc:PriceTypeCode` | `Cat16TipoPrecio` |
| 19 | Estado del ítem en el resumen diario | `sac:SummaryDocumentsLine` | `ResumenDiario.estadoItem` |
| 25 | Código de producto SUNAT (UNSPSC) | `cbc:ItemClassificationCode` | `codigoProductoSunat` |
| 51 | Tipo de operación | `cbc:InvoiceTypeCode/@listID` | `Cat51TipoOperacion` |
| 52 | Leyendas | `cbc:Note/@languageLocaleID` | `Cat52Leyenda` |
| 53 | Cargos y descuentos | `cbc:AllowanceChargeReasonCode` | `Cat53*` |
| 54 / 59 | Detracción: bien o servicio / medio de pago | `cac:PaymentTerms`, `cac:PaymentMeans` | `Cat54*`, `Cat59*` |

Los catálogos completos se cargan como **datos con vigencia** en el módulo `tributacion` (ADR-08) y se consultan con `GET /catalogos/{numero}?fecha=`. El esquema JSON solo fija el subconjunto mínimo que validan los clientes.

## 2.5 Punto clave: el Catálogo N.° 08 (sistemas de cálculo del ISC)

**Tabla 2.4.** *Catálogo N.° 08 y fórmulas del motor tributario*

| Código | Sistema (TUO IGV e ISC, Apéndice IV) | Fórmula del ISC por ítem | Ejemplo |
|---|---|---|---|
| 01 | Al valor (literal A) | ISC = valor de venta × tasa | 24 latas × S/ 2,00 = 48,00; tasa ilustrativa 25 % → **12,00** (XML F001-2) |
| 02 | Monto fijo (literal B) | ISC = cantidad expresada en la unidad del Apéndice × monto fijo | 10 cajas × 12 botellas × 0,62 L = 74,4 L × S/ 0,50/L (ilustrativo) → **37,20** |
| 03 | Precio de venta al público (literal C) | ISC = cantidad × PVP sugerido × factor (0,847) × tasa | 12 u × S/ 5,00 × 0,847 × 30 % (ilustrativo) → **15,25** |

Consecuencias de diseño:

1. **Quién paga el ISC.** El ISC es monofásico: lo pagan el productor, el importador o sus empresas vinculadas. Un mayorista que solo revende (supuesto SUP-03) recibe el ISC dentro de su costo de compra y **no lo factura**. Por eso el maestro de productos trae el ISC **desactivado por defecto**, y el contador lo activa solo si ABC importa o está vinculada a un productor. La factura F001-2 muestra ese escenario.
2. **Productos de abarrotes potencialmente afectos:** cervezas, licores, cigarrillos y bebidas azucaradas o energizantes. Sus tasas y montos cambian por decreto supremo, así que viven en `TasaIsc` con `vigenteDesde`, `vigenteHasta` y `baseLegal`, nunca en el código (ADR-08).
3. **El ISC entra en la base del IGV:** IGV = (valor de venta + ISC) × 18 % (VAL-TRB-05). En F001-2: (48,00 + 12,00) × 18 % = 10,80.
4. **Representación UBL:** en la línea se agrega un `cac:TaxSubtotal` con `cbc:TaxableAmount` (base del ISC), `cbc:TaxAmount`, `cac:TaxCategory/cbc:Percent`, **`cbc:TierRange` (código del Catálogo N.° 08)** y el `TaxScheme` 2000 / ISC / EXC. En el total va un subtotal 2000 con la suma.
5. **Validación:** VAL-TRB-03 (fórmula según el sistema), VAL-TRB-04 (el ISC declarado coincide con la parametrización vigente), VAL-TRB-05 (base del IGV) y el contraejemplo `i06-isc-al-valor-sin-tasa.json` (falta la tasa del sistema 01).

**Lectura alternativa: el Anexo N.° 8 completo.** El documento oficial que reúne los catálogos se titula "Anexo N.° 8 - Catálogo de códigos" (catálogos 01 a 60). El diseño lo trata como un conjunto de datos versionado con vigencia: un cambio normativo actualiza datos y no obliga a reconstruir el sistema.

## 2.6 Otros tributos y reglas de cálculo relevantes para abarrotes

- **IGV:** 18 % sobre la base imponible. Exonerado (20) e inafecto (30) se informan con los tributos 9997 y 9998 e IGV cero. Algunos productos agrícolas frescos están exonerados según el Apéndice I de la Ley del IGV; el ejemplo B001-1 incluye papa fresca (código 20).
- **Operaciones gratuitas y bonificaciones** (códigos 11 a 16, 21 y 31 a 37): llevan valor referencial (Cat. 16 = 02), valor unitario 0, tributo 9996 y la leyenda 1002. No suman al importe total. El ejemplo F001-1 incluye una bonificación (código 15).
- **ICBPER:** S/ 0,50 por bolsa; no forma parte de la base del IGV. Se informa con `BaseUnitMeasure` (cantidad de bolsas) y `PerUnitAmount` (tarifa). Ejemplo B001-1.
- **Detracción y percepción:** se soportan en el contrato (tipo de operación 1001 o 2001), pero fuera del escenario base (SUP-11).

## 2.7 Operación del SEE: plazos, servicios y respuestas

**Tabla 2.5.** *Operación con SUNAT/OSE*

| Aspecto | Regla | Fuente |
|---|---|---|
| Envío de factura y notas vinculadas | `sendBill` (síncrono). Plazo máximo parametrizado en 3 días calendario contados desde el día siguiente a la emisión | Portal CPE de SUNAT; SUP-09 |
| Boletas y notas vinculadas | Resumen diario (`sendSummary`, archivo RC) o envío individual, dentro del plazo | Manual del programador; Anexo de boletas |
| Comunicación de baja | `sendSummary` (archivo RA). Hasta 7 días calendario | Orientación SUNAT; RS 000048-2026 |
| Consulta de ticket | `getStatus`: 0 procesado, 98 en proceso, 99 con errores | Manual del programador del SEE |
| Consulta de CDR | `getStatusCdr` (servicio de consulta) | Manual del programador del SEE |
| Nombre de archivos | `RUC-TT-SERIE-NUMERO.zip` con un XML del mismo nombre; RC y RA: `RUC-RC-AAAAMMDD-n` | Manual del programador del SEE |
| Autenticación | WS-Security UsernameToken: RUC + usuario SOL (en beta: `MODDATOS`) | Manual del programador del SEE |
| Ambientes | Beta `https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService`; producción `https://e-factura.sunat.gob.pe/ol-ti-itcpfegem/billService` | Manual del programador del SEE |
| Códigos de respuesta | 0100–0999: excepción SUNAT. 1000–1999: excepción por formato. **2000–3999: rechazo**. **4000 en adelante: observación** (el comprobante es válido) | Manual del programador, §4.1 |
| Rechazo de factura o nota | "La numeración se considera ya utilizada, pero la factura [...] no es válida": se emite un comprobante **nuevo con otro número** | Manual del programador, §4.1 |
| Excepción | El documento se considera no informado: se corrige y se reenvía con el **mismo número** | Manual del programador, §4.1 |
| Resumen o baja rechazados | Se rechaza el documento completo y puede reenviarse con el mismo nombre | Manual del programador, §4.1 |

## 2.8 Registros formales (RS 234-2006/SUNAT)

- **Formato 12.1 (Inventario Permanente en Unidades Físicas).** Columnas: fecha; tipo de documento (Tabla 10); serie; número; tipo de operación (Tabla 12); entradas; salidas; saldo final. Cabecera: periodo, RUC, razón social, establecimiento, código, tipo de existencia (Tabla 5), descripción y unidad de medida (Tabla 6). **Es el destino natural de las fichas del legado** (Matriz C).
- **Formato 14.1 (Registro de Ventas e Ingresos).** Columnas: correlativo/CUO, fechas, comprobante (Tabla 10, serie y número), cliente (Tabla 2, número y nombre), valor de exportación, base gravada, exonerada, inafecta, ISC, IGV, otros tributos, total, tipo de cambio y referencia del comprobante modificado. Hoy se lleva como **RVIE** en el SIRE, que parte de los comprobantes emitidos. El sistema genera la propuesta o el formato desde los mismos datos del CPE (Matriz B).

## 2.9 Fuentes consultadas y nivel de confianza

| Fuente | Uso | Confianza |
|---|---|---|
| Paquete OASIS UBL 2.1 OS (XSD *runtime*) | Estructura, orden y cardinalidad | Fuente primaria, verificada |
| SUNAT, Anexo N.° 8 - Catálogo de códigos (versión UBL 2.1) | Catálogos 01 a 58 | Fuente primaria |
| SUNAT, Anexo N.° 9-A - Estándar UBL 2.1 (RS 114-2019) | Campos, condición y XPath | Fuente primaria |
| SUNAT, Manual del programador del SEE | Servicios SOAP, archivos y códigos de respuesta | Fuente primaria |
| SUNAT, RS 150-2019 (ICBPER), RS 193-2020 (forma de pago), RS 000005-2023 (catálogo 51) | Reglas específicas | Fuente primaria |
| SUNAT, RS 234-2006: formatos 12.1 y 14.1 y tablas | Registros | Fuente primaria |
| Normas 2026 (RS 000048, 000049, 000075, 000141 y 000143) | Cambios próximos | Resúmenes de terceros (Sovos, BYB, El Peruano); verificar el texto oficial |
| Plazos de envío (3 días) y de baja (7 días) | Parámetros | Las fuentes no coinciden; parametrizados (SUP-09) |
