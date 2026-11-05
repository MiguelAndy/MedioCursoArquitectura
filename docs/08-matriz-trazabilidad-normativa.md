# 8. Entregable 4: matriz de trazabilidad normativa (SUNAT ↔ UBL)

## 8.1 Qué cruza la matriz y cómo leerla

La matriz responde a una pregunta: **para cada dato que exige SUNAT, ¿dónde está en el XML UBL 2.1, qué dice el XSD oficial de OASIS sobre ese elemento, en qué campo del contrato entra, qué parte del dominio lo administra y qué regla lo valida?**

- **Fuente única:** `specs/trazabilidad/matriz-trazabilidad.yaml`. Las tablas de este capítulo, el libro `specs/trazabilidad/matriz-trazabilidad.xlsx` (5 hojas, con filtros) y los CSV de `specs/trazabilidad/csv/` se **generan** con `python tools/generar_matrices.py`; ninguno se edita a mano.
- **Columna "Card. XSD":** no se escribió a mano. El generador la **calcula recorriendo los XSD oficiales** (`UBL-Invoice-2.1.xsd`, `UBL-CreditNote-2.1.xsd`, `UBL-DebitNote-2.1.xsd` y sus componentes comunes) por la ruta de cada elemento. Así la matriz es evidencia verificable del estudio del XSD (REQ-07).
- **Marca ▲:** el dato es **obligatorio para SUNAT** (condición M), pero el XSD lo deja **opcional** en algún punto de su ruta. Esas filas muestran el *perfil SUNAT* sobre UBL. El XSD no basta, y por eso existen las reglas VAL-EST-03 y VAL-*.
- **Columnas** (las de la hoja XLSX que no aparecen aquí: dominio y ruta completa de cardinalidades):

| Columna | Significado |
|---|---|
| N.° Anexo 9-A | Número del campo en el Anexo N.° 9-A (estándar UBL 2.1, factura). Si el dato viene de otra norma, se indica la norma |
| Cond. | M = mandatorio; C = condicional (Anexo N.° 9-A) |
| Cat. | Catálogo del Anexo N.° 8 que restringe el valor; entre paréntesis, el código de tributo del Cat. 05 |
| Elemento UBL 2.1 | XPath en el XML. Los predicados `[...]` indican qué ocurrencia; `@` indica atributo |
| Card. XSD | Cardinalidad del elemento final según el XSD OASIS; ▲ = diferencia de perfil |
| Campo DTO | Propiedad en `EmitirComprobanteRequest` o `Comprobante` (JSON Schema) |
| Reglas | Reglas `VAL-*` que lo validan (docs/04) |

## 8.2 Matriz A: factura y boleta (raíz `Invoice`)

**Tabla 8.1.** *Cruce Anexo N.° 9-A ↔ UBL 2.1 ↔ XSD ↔ contrato ↔ reglas*

<!-- inicio:matriz-a -->
| ID | N.° Anexo 9-A | Dato SUNAT | Cond. | Catálogo | Elemento UBL 2.1 (XPath) | Card. XSD | Campo DTO | Reglas |
|---|---|---|---|---|---|---|---|---|
| TR-A01 | 1 | Versión del UBL | M |  | /Invoice/cbc:UBLVersionID = "2.1" | 0..1 ▲ | (constante del generador UBL) | VAL-EST-03 |
| TR-A02 | 2 | Versión de la estructura del documento | M |  | /Invoice/cbc:CustomizationID = "2.0" | 0..1 ▲ | (constante del generador UBL) | VAL-EST-03 |
| TR-A03 | 3 | Numeración: serie y número correlativo | M |  | /Invoice/cbc:ID | 1..1 | serie (solicitud); numero (respuesta) | VAL-NUM-01, VAL-NUM-02 |
| TR-A04 | 4 | Fecha de emisión | M |  | /Invoice/cbc:IssueDate | 1..1 | fechaEmision | VAL-FEC-01, VAL-FEC-02 |
| TR-A05 | 5 | Hora de emisión | C |  | /Invoice/cbc:IssueTime | 0..1 | horaEmision |  |
| TR-A06 | 6 | Tipo de documento | M | 01 | /Invoice/cbc:InvoiceTypeCode | 0..1 ▲ | tipoComprobante | VAL-EST-01 |
| TR-A07 | 58 | Tipo de operación | M | 51 | /Invoice/cbc:InvoiceTypeCode/@listID | 0..1 ▲ | tipoOperacion | VAL-ID-03, VAL-ID-05, VAL-DET-01 |
| TR-A08 | 7 | Tipo de moneda | M | 02 | /Invoice/cbc:DocumentCurrencyCode (y @currencyID de cada importe) | 0..1 ▲ | moneda | VAL-MON-01, VAL-MON-02 |
| TR-A09 | 8 | Fecha de vencimiento | C |  | /Invoice/cbc:DueDate | 0..1 | fechaVencimiento | VAL-FEC-03 |
| TR-A10 | 9 | Firma digital | M |  | /Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/ds:Signature y /Invoice/cac:Signature | any (lax) ▲ | documentoElectronico.hash (respuesta) | VAL-ENV-02, VAL-EST-02 |
| TR-A11 | 10 | Número de RUC del emisor (tipo 6) | M | 06 | /Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyIdentification/cbc:ID/@schemeID | 1..1 ▲ | emisor.ruc (respuesta) | VAL-ID-02 |
| TR-A12 | 11 | Nombre comercial del emisor | C |  | /Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyName/cbc:Name | 1..1 | emisor.nombreComercial |  |
| TR-A13 | 12 | Razón social del emisor | M |  | /Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyLegalEntity/cbc:RegistrationName | 0..1 ▲ | emisor.razonSocial |  |
| TR-A14 | 13 | Domicilio fiscal (ubigeo y dirección) | C | 13 | /Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyLegalEntity/cac:RegistrationAddress/cbc:ID | 0..1 | emisor.ubigeo, emisor.direccion |  |
| TR-A15 | 16 | Código del domicilio fiscal o del establecimiento anexo | M |  | /Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyLegalEntity/cac:RegistrationAddress/cbc:AddressTypeCode | 0..1 ▲ | emisor.codigoEstablecimiento | VAL-EST-03 |
| TR-A16 | 17 | Tipo y número de documento del adquirente | M | 06 | /Invoice/cac:AccountingCustomerParty/cac:Party/cac:PartyIdentification/cbc:ID/@schemeID | 1..1 ▲ | cliente.tipoDocumento, cliente.numeroDocumento | VAL-ID-01, VAL-ID-02, VAL-ID-03, VAL-ID-06, VAL-BOL-01 |
| TR-A17 | 18 | Razón social o nombre del adquirente | M |  | /Invoice/cac:AccountingCustomerParty/cac:Party/cac:PartyLegalEntity/cbc:RegistrationName | 0..1 ▲ | cliente.razonSocial | VAL-ID-04 |
| TR-A18 | 19 | Dirección del adquirente | C | 13 | /Invoice/cac:AccountingCustomerParty/cac:Party/cac:PartyLegalEntity/cac:RegistrationAddress/cac:AddressLine/cbc:Line | 1..1 | cliente.direccion, cliente.ubigeo |  |
| TR-A19 | 22 | Guía de remisión relacionada | C | 01 | /Invoice/cac:DespatchDocumentReference/cbc:ID (+ cbc:DocumentTypeCode) | 1..1 | guiasRemision[].numero, .tipo | VAL-BOL-02 |
| TR-A20 | 59 | Número de la orden de compra | C |  | /Invoice/cac:OrderReference/cbc:ID | 1..1 | ordenCompra |  |
| TR-A21 | 57 | Leyendas (monto en letras, transferencia gratuita, etc.) | C | 52 | /Invoice/cbc:Note/@languageLocaleID | 0..n | leyendas[] (respuesta) | VAL-TOT-06, VAL-ITM-04 |
| TR-A22 | RS 193-2020 | Forma de pago (Contado / Credito) | M |  | /Invoice/cac:PaymentTerms[cbc:ID='FormaPago']/cbc:PaymentMeansID | 0..n ▲ | formaPago.tipo | VAL-PAG-01 |
| TR-A23 | RS 193-2020 | Monto neto pendiente de pago | C |  | /Invoice/cac:PaymentTerms[cbc:PaymentMeansID='Credito']/cbc:Amount | 0..1 | formaPago.montoPendiente | VAL-PAG-02 |
| TR-A24 | RS 193-2020 | Cuotas: monto y fecha de vencimiento | C |  | /Invoice/cac:PaymentTerms[cbc:PaymentMeansID='CuotaNNN']/cbc:Amount y cbc:PaymentDueDate | 0..1 | formaPago.cuotas[] | VAL-PAG-03, VAL-FEC-03 |
| TR-A25 | Detracción | Bien o servicio, porcentaje, monto y cuenta del Banco de la Nación | C | 54, 59 | /Invoice/cac:PaymentMeans/cac:PayeeFinancialAccount/cbc:ID y /Invoice/cac:PaymentTerms[cbc:ID='Detraccion'] | 0..1 | detraccion.* | VAL-DET-01 |
| TR-A26 | 50 | Cargos y descuentos globales | C | 53 | /Invoice/cac:AllowanceCharge (ChargeIndicator, AllowanceChargeReasonCode, MultiplierFactorNumeric, Amount, BaseAmount) | 0..1 | cargosDescuentosGlobales[] | VAL-TOT-01, VAL-TOT-04 |
| TR-A27 | 40 | Monto total de tributos | M |  | /Invoice/cac:TaxTotal/cbc:TaxAmount | 1..1 ▲ | totales.totalTributos | VAL-TOT-02 |
| TR-A28 | 46-47 | Total valor de venta de operaciones gravadas y sumatoria del IGV | C | 05 (1000) | /Invoice/cac:TaxTotal/cac:TaxSubtotal[TaxScheme/ID=1000]/cbc:TaxableAmount y cbc:TaxAmount | 0..1 | totales.operacionesGravadas, totales.igv | VAL-TRB-01, VAL-TRB-08 |
| TR-A29 | 43 | Total valor de venta de operaciones exoneradas | C | 05 (9997) | /Invoice/cac:TaxTotal/cac:TaxSubtotal[TaxScheme/ID=9997]/cbc:TaxableAmount | 0..1 | totales.operacionesExoneradas | VAL-TRB-07 |
| TR-A30 | 42 | Total valor de venta de operaciones inafectas | C | 05 (9998) | /Invoice/cac:TaxTotal/cac:TaxSubtotal[TaxScheme/ID=9998]/cbc:TaxableAmount | 0..1 | totales.operacionesInafectas | VAL-TRB-07 |
| TR-A31 | 41 | Total valor de venta de exportación | C | 05 (9995) | /Invoice/cac:TaxTotal/cac:TaxSubtotal[TaxScheme/ID=9995]/cbc:TaxableAmount | 0..1 | totales.operacionesExportacion | VAL-ITM-05 |
| TR-A32 | 44-45 | Total valor de venta de operaciones gratuitas y sus tributos | C | 05 (9996) | /Invoice/cac:TaxTotal/cac:TaxSubtotal[TaxScheme/ID=9996]/cbc:TaxableAmount y cbc:TaxAmount | 1..1 | totales.operacionesGratuitas, totales.tributosOperacionesGratuitas | VAL-TRB-09, VAL-ITM-04 |
| TR-A33 | 48 | Sumatoria del ISC | C | 05 (2000) | /Invoice/cac:TaxTotal/cac:TaxSubtotal[TaxScheme/ID=2000]/cbc:TaxAmount | 1..1 | totales.isc | VAL-TRB-03 |
| TR-A34 | 49 | Sumatoria de otros tributos (ICBPER 7152, otros 9999) | C | 05 (7152, 9999) | /Invoice/cac:TaxTotal/cac:TaxSubtotal[TaxScheme/ID=7152\|9999]/cbc:TaxAmount | 1..1 | totales.icbper, totales.otrosTributos | VAL-TRB-06 |
| TR-A35 | 54 | Total valor de venta | M |  | /Invoice/cac:LegalMonetaryTotal/cbc:LineExtensionAmount | 0..1 ▲ | totales.totalValorVenta | VAL-TOT-01 |
| TR-A36 | 55 | Total precio de venta (incluye impuestos) | M |  | /Invoice/cac:LegalMonetaryTotal/cbc:TaxInclusiveAmount | 0..1 ▲ | totales.totalPrecioVenta | VAL-TOT-03 |
| TR-A37 | 51 | Sumatoria de otros descuentos (no afectan la base del IGV) | C |  | /Invoice/cac:LegalMonetaryTotal/cbc:AllowanceTotalAmount | 0..1 | totales.descuentosNoAfectos | VAL-TOT-04 |
| TR-A38 | 52 | Sumatoria de otros cargos (no afectan la base del IGV) | C |  | /Invoice/cac:LegalMonetaryTotal/cbc:ChargeTotalAmount | 0..1 | totales.cargosNoAfectos | VAL-TOT-04 |
| TR-A39 | 66 | Total de anticipos | C |  | /Invoice/cac:LegalMonetaryTotal/cbc:PrepaidAmount | 0..1 | totales.anticipos | VAL-TOT-04 |
| TR-A40 | 56 | Monto de redondeo del importe total | C |  | /Invoice/cac:LegalMonetaryTotal/cbc:PayableRoundingAmount | 0..1 | totales.redondeo | VAL-TOT-04 |
| TR-A41 | 53 | Importe total de la venta | M |  | /Invoice/cac:LegalMonetaryTotal/cbc:PayableAmount | 1..1 | totales.importeTotal | VAL-TOT-04, VAL-TOT-06 |
| TR-A42 | 24 | Número de orden del ítem | M |  | /Invoice/cac:InvoiceLine/cbc:ID | 1..1 | items[].numeroOrden (respuesta) | VAL-ITM-01 |
| TR-A43 | 25-26 | Unidad de medida y cantidad del ítem | M | 03 | /Invoice/cac:InvoiceLine/cbc:InvoicedQuantity/@unitCode | 0..1 ▲ | items[].unidadMedida, items[].cantidad | VAL-ITM-02, VAL-ITM-03, VAL-EST-03 |
| TR-A44 | 27 | Código de producto | C |  | /Invoice/cac:InvoiceLine/cac:Item/cac:SellersItemIdentification/cbc:ID | 1..1 | items[].codigoProducto |  |
| TR-A45 | 28 | Código de producto SUNAT (UNSPSC) | C | 25 | /Invoice/cac:InvoiceLine/cac:Item/cac:CommodityClassification/cbc:ItemClassificationCode | 0..1 | items[].codigoProductoSunat |  |
| TR-A46 | 29 | Código de producto GTIN | C |  | /Invoice/cac:InvoiceLine/cac:Item/cac:StandardItemIdentification/cbc:ID | 1..1 | items[].codigoGtin |  |
| TR-A47 | 31 | Descripción del bien o servicio | M |  | /Invoice/cac:InvoiceLine/cac:Item/cbc:Description | 0..n ▲ | items[].descripcion | VAL-ITM-03 |
| TR-A48 | 32 | Valor unitario por ítem (sin impuestos) | M |  | /Invoice/cac:InvoiceLine/cac:Price/cbc:PriceAmount | 1..1 ▲ | items[].valorUnitario | VAL-ITM-02, VAL-TRB-02 |
| TR-A49 | 33 | Precio de venta unitario (tipo 01) | M | 16 | /Invoice/cac:InvoiceLine/cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceAmount (+ cbc:PriceTypeCode) | 1..1 ▲ | items[].precioUnitario | VAL-TRB-02 |
| TR-A50 | 34 | Valor referencial unitario en operaciones no onerosas (tipo 02) | C | 16 | /Invoice/cac:InvoiceLine/cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceTypeCode = 02 | 0..1 | items[].valorReferencialUnitario | VAL-ITM-04 |
| TR-A51 | 35 | Monto total de tributos del ítem | M |  | /Invoice/cac:InvoiceLine/cac:TaxTotal/cbc:TaxAmount | 1..1 ▲ | items[].totalTributos | VAL-TRB-08 |
| TR-A52 | 36 | Afectación al IGV por ítem: base, monto, tasa y código | M | 07, 05 | /Invoice/cac:InvoiceLine/cac:TaxTotal/cac:TaxSubtotal/cac:TaxCategory/cbc:TaxExemptionReasonCode (+ TaxableAmount, TaxAmount, Percent, TaxScheme/ID) | 0..1 ▲ | items[].tipoAfectacionIgv, baseImponibleIgv, montoIgv, porcentajeIgv, codigoTributoIgv | VAL-TRB-01, VAL-TRB-05, VAL-TRB-07 |
| TR-A53 | 37 | Afectación al ISC por ítem: sistema de cálculo (CATÁLOGO N.° 08), base, tasa y monto | C | 08, 05 (2000) | /Invoice/cac:InvoiceLine/cac:TaxTotal/cac:TaxSubtotal/cac:TaxCategory/cbc:TierRange (+ TaxableAmount, TaxAmount, Percent) | 0..1 | items[].isc.sistema, isc.tasa, baseImponibleIsc, montoIsc | VAL-TRB-03, VAL-TRB-04, VAL-TRB-05 |
| TR-A54 | RS 150-2019 | ICBPER por ítem: cantidad de bolsas, tarifa y monto | C | 05 (7152) | /Invoice/cac:InvoiceLine/cac:TaxTotal/cac:TaxSubtotal/cbc:BaseUnitMeasure (+ cac:TaxCategory/cbc:PerUnitAmount) | 0..1 | items[].icbper.cantidadBolsas, tarifaIcbper, montoIcbper | VAL-TRB-06 |
| TR-A55 | 38 | Valor de venta por ítem | M |  | /Invoice/cac:InvoiceLine/cbc:LineExtensionAmount | 1..1 | items[].valorVenta | VAL-TOT-01 |
| TR-A56 | 39 | Cargo o descuento por ítem | C | 53 | /Invoice/cac:InvoiceLine/cac:AllowanceCharge/cbc:AllowanceChargeReasonCode (+ MultiplierFactorNumeric, Amount, BaseAmount) | 0..1 | items[].cargosDescuentos[] | VAL-TOT-01 |
<!-- fin:matriz-a -->

*Nota.* Fuentes: RS 097-2012/SUNAT, Anexo N.° 9-A (RS 114-2019/SUNAT), RS 150-2019/SUNAT, RS 193-2020/SUNAT y Anexo N.° 8; XSD OASIS UBL 2.1. Elaboración propia.

**Lectura del Catálogo N.° 08 en la matriz (fila TR-A53):** el sistema de cálculo del ISC viaja en `cac:TaxCategory/cbc:TierRange`, que en el XSD es `0..1` dentro de un `TaxSubtotal` opcional. SUNAT lo exige cuando el ítem está afecto al ISC (condición C). El contrato lo recibe en `items[].isc.sistema` (`Cat08SistemaIsc`) y lo validan VAL-TRB-03, 04 y 05. El ejemplo `20123456786-01-F001-2.xml` lo muestra con el valor `01`.

## 8.3 Matriz A2: notas de crédito y débito

**Tabla 8.2.** *Cruce para `CreditNote` y `DebitNote`*

<!-- inicio:matriz-notas -->
| ID | Dato SUNAT | Cond. | Catálogo | Elemento UBL 2.1 (XPath) | Card. XSD | Campo DTO | Reglas |
|---|---|---|---|---|---|---|---|
| TR-N01 | Serie y número de la nota (letra según el comprobante modificado) | M |  | /CreditNote/cbc:ID | 1..1 | serie; numero | VAL-NUM-01, VAL-NUM-03 |
| TR-N02 | Tipo de nota de crédito (motivo) | M | 09 | /CreditNote/cac:DiscrepancyResponse/cbc:ResponseCode | 0..1 ▲ | documentoReferencia.motivo | VAL-NOT-03 |
| TR-N03 | Sustento del motivo | M |  | /CreditNote/cac:DiscrepancyResponse/cbc:Description | 0..n ▲ | documentoReferencia.sustento | VAL-NOT-03 |
| TR-N04 | Serie y número del comprobante que se modifica | M |  | /CreditNote/cac:BillingReference/cac:InvoiceDocumentReference/cbc:ID (y DiscrepancyResponse/cbc:ReferenceID) | 1..1 ▲ | documentoReferencia.numero | VAL-NOT-01, VAL-NOT-02 |
| TR-N05 | Tipo del comprobante que se modifica | M | 01 | /CreditNote/cac:BillingReference/cac:InvoiceDocumentReference/cbc:DocumentTypeCode | 0..1 ▲ | documentoReferencia.tipoComprobante | VAL-NOT-02, VAL-NOT-04 |
| TR-N06 | Cantidad acreditada por ítem | M | 03 | /CreditNote/cac:CreditNoteLine/cbc:CreditedQuantity/@unitCode | 0..1 ▲ | items[].cantidad, items[].unidadMedida | VAL-NOT-05, VAL-NOT-06 |
| TR-N07 | Importe total de la nota de crédito | M |  | /CreditNote/cac:LegalMonetaryTotal/cbc:PayableAmount | 1..1 | totales.importeTotal | VAL-NOT-05 |
| TR-N08 | Tipo de nota de débito (motivo) | M | 10 | /DebitNote/cac:DiscrepancyResponse/cbc:ResponseCode | 0..1 ▲ | documentoReferencia.motivo | VAL-NOT-03 |
| TR-N09 | Importe total de la nota de débito | M |  | /DebitNote/cac:RequestedMonetaryTotal/cbc:PayableAmount | 1..1 | totales.importeTotal | VAL-TOT-04 |
<!-- fin:matriz-notas -->

## 8.4 Matriz B: registro de ventas (RS 234-2006) ↔ UBL

Esta matriz prueba que el **registro formal** que exige el enunciado se obtiene de los mismos datos del comprobante electrónico, sin volver a digitar nada.

**Tabla 8.3.** *Formato 14.1 / RVIE ↔ UBL 2.1 ↔ contrato*

<!-- inicio:matriz-b -->
| ID | Columna del Formato 14.1 / RVIE | Tabla | Origen en UBL 2.1 | Campo DTO (LineaRegistroVentas) |
|---|---|---|---|---|
| TR-B01 | Número correlativo del registro o CUO |  | (lo genera el módulo registros) | cuo |
| TR-B02 | Fecha de emisión del comprobante |  | /Invoice/cbc:IssueDate | fechaEmision |
| TR-B03 | Fecha de vencimiento y/o pago |  | /Invoice/cbc:DueDate (o la PaymentDueDate de la última cuota) | fechaVencimiento |
| TR-B04 | Tipo de comprobante de pago | Tabla 10 | /Invoice/cbc:InvoiceTypeCode (01, 03); raíz CreditNote = 07; DebitNote = 08 | tipoComprobante |
| TR-B05 | Número de serie |  | substring-before(/Invoice/cbc:ID, '-') | serie |
| TR-B06 | Número del comprobante |  | substring-after(/Invoice/cbc:ID, '-') | numero |
| TR-B07 | Tipo de documento de identidad del cliente | Tabla 2 | /Invoice/cac:AccountingCustomerParty/cac:Party/cac:PartyIdentification/cbc:ID/@schemeID (Cat. 06; códigos 0, 1, 4, 6, 7 y A coinciden con la Tabla 2) | tipoDocumentoCliente |
| TR-B08 | Número de documento del cliente |  | /Invoice/cac:AccountingCustomerParty/cac:Party/cac:PartyIdentification/cbc:ID | numeroDocumentoCliente |
| TR-B09 | Apellidos y nombres, denominación o razón social |  | /Invoice/cac:AccountingCustomerParty/cac:Party/cac:PartyLegalEntity/cbc:RegistrationName | razonSocialCliente |
| TR-B10 | Valor facturado de la exportación |  | TaxSubtotal 9995 / cbc:TaxableAmount | valorExportacion |
| TR-B11 | Base imponible de la operación gravada |  | TaxSubtotal 1000 / cbc:TaxableAmount | baseImponibleGravada |
| TR-B12 | Importe total de la operación exonerada |  | TaxSubtotal 9997 / cbc:TaxableAmount | importeExonerado |
| TR-B13 | Importe total de la operación inafecta |  | TaxSubtotal 9998 / cbc:TaxableAmount | importeInafecto |
| TR-B14 | ISC |  | TaxSubtotal 2000 / cbc:TaxAmount | isc |
| TR-B15 | IGV y/o IPM |  | TaxSubtotal 1000 / cbc:TaxAmount | igv |
| TR-B16 | ICBPER (columna incorporada posteriormente en PLE / RVIE) |  | TaxSubtotal 7152 / cbc:TaxAmount | icbper |
| TR-B17 | Otros tributos y cargos que no forman parte de la base imponible |  | TaxSubtotal 9999 / cbc:TaxAmount + LegalMonetaryTotal/cbc:ChargeTotalAmount | otrosTributos |
| TR-B18 | Importe total del comprobante |  | /Invoice/cac:LegalMonetaryTotal/cbc:PayableAmount | importeTotal |
| TR-B19 | Tipo de cambio |  | (no se informa en el XML de venta interna; proviene del comprobante: tipoCambio) | tipoCambio |
| TR-B20 | Referencia del comprobante modificado: fecha, tipo, serie y número | Tabla 10 | /CreditNote/cac:BillingReference/cac:InvoiceDocumentReference/cbc:ID y cbc:DocumentTypeCode | referencia.fecha, .tipo, .serie, .numero |
<!-- fin:matriz-b -->

## 8.5 Matriz C: inventario permanente (Formato 12.1) ↔ dominio ↔ legado

Las cinco columnas del sistema legado (fecha, ingreso, salida, producto y nro_documento) son, casi una a una, las del Formato 12.1. La migración es, en esencia, **formalizar el kárdex informal** (docs/09).

**Tabla 8.4.** *Formato 12.1 ↔ entidades ↔ ficha del legado*

<!-- inicio:matriz-c -->
| ID | Columna del Formato 12.1 | Tabla | Entidad / atributo | Campo del legado | Regla |
|---|---|---|---|---|---|
| TR-C01 | Periodo |  | MovimientoInventario.fecha (AAAA-MM) | fecha | MIG-01 |
| TR-C02 | RUC |  | Empresa.ruc | — |  |
| TR-C03 | Apellidos y nombres, denominación o razón social |  | Empresa.razonSocial | — |  |
| TR-C04 | Establecimiento |  | Almacen.codigoEstablecimiento | (parámetro del lote: almacenId) |  |
| TR-C05 | Código de la existencia |  | Producto.codigo | producto | MIG-03 |
| TR-C06 | Tipo de existencia | Tabla 5 | Producto.tipoExistencia | — (por defecto 01 Mercadería) | MIG-07 |
| TR-C07 | Descripción |  | Producto.descripcion | producto | MIG-03 |
| TR-C08 | Código de la unidad de medida | Tabla 6 | Producto.unidadBase (Cat. 03 → Tabla 6) | — (por defecto 07 Unidades) | MIG-07 |
| TR-C09 | Fecha del documento |  | MovimientoInventario.fecha | fecha | MIG-01 |
| TR-C10 | Tipo de documento de traslado, comprobante o documento interno | Tabla 10 | DocumentoSoporte.tipo | nro_documento | MIG-04 |
| TR-C11 | Serie |  | DocumentoSoporte.serie | nro_documento | MIG-04 |
| TR-C12 | Número |  | DocumentoSoporte.numero (+ textoOriginal) | nro_documento | MIG-04 |
| TR-C13 | Tipo de operación | Tabla 12 | MovimientoInventario.tipoOperacion | (inferido de ingreso/salida y del documento) | MIG-05 |
| TR-C14 | Entradas |  | MovimientoInventario.entrada | ingreso | MIG-02 |
| TR-C15 | Salidas |  | MovimientoInventario.salida | salida | MIG-02 |
| TR-C16 | Saldo final |  | MovimientoInventario.saldoFinal | (calculado y conciliado) | MIG-10 |
| TR-C17 | (trazabilidad, no forma parte del formato) |  | Linaje (idLegado, hashRegistro, reglas, versión) | idLegado (clave u archivo#hoja#fila) | MIG-11 |
<!-- fin:matriz-c -->

## 8.6 Matriz D: requisitos del enunciado ↔ artefactos

**Tabla 8.5.** *Cumplimiento del enunciado*

<!-- inicio:matriz-d -->
| Requisito | Descripción | Artefactos | Verificación |
|---|---|---|---|
| REQ-01 | Emitir CPE en UBL 2.1 (factura, boleta y notas) | docs/05; specs/schemas/comprobante-*.json; specs/ubl/ejemplos (4 XML) | XSD OASIS + cuadres (validar_specs.py) |
| REQ-02 | Registros formales (14.1 / RVIE y 12.1) | Matrices B y C; GET /registros/*; inventario.schema.json (KardexFormato121) | Revisión de columnas contra los formatos oficiales |
| REQ-03 | Validar RUC | VAL-ID-01 a 04; GET /consultas/ruc/{ruc}; ConsultaContribuyentePort | TC-03; contraejemplo i02 |
| REQ-04 | Calcular IGV, ISC (Cat. 08) e ICBPER | docs/04, sección 4.3; VAL-TRB-*; ejemplos F001-2 y B001-1 | TC-07 a TC-09; cuadres de los XML |
| REQ-05 | XML firmado digitalmente | FirmadorXmlPort; tools/conformidad/FirmarXmlPrueba.java; ejemplos firmados | Firma verificada + XSD (ds:Signature, lax) |
| REQ-06 | Comunicación con OSE o SUNAT | docs/07, sección 7.6; CanalEnvioCpePort; estados (Figura 4.1) | Pruebas en el ambiente beta (fase de implementación) |
| REQ-07 | Spec-First y estudio del UBL-Invoice-2.1.xsd | specs/ (contratos primero); docs/02, Tabla 2.2; esta matriz con cardinalidades del XSD | validar_specs.py en verde antes de programar |
| REQ-08 | Migración del legado con trazabilidad | docs/09; migracion.schema.json; /migraciones/*; Matriz C | Controles CTL-01 a CTL-05; linaje |
| REQ-09 | Catálogos, series, correlativos y validaciones (Catálogo 8) | catalogos-sunat.schema.json; VAL-NUM-*; docs/02, sección 2.5 | Contraejemplos i01, i06; TC-12 |
| REQ-10 | MVC con visión hexagonal y microservicios | docs/06; x-puerto-entrada en el OpenAPI; AsyncAPI; reglas ArchUnit | ArchUnit / Spring Modulith en CI |
| REQ-11 | Nativo en Windows y Android; MSI y APK | docs/06, sección 6.8 (Tabla 6.4); ADR-03 | Pipeline de empaquetado (packageMsi / assembleRelease) |
| REQ-12 | Casos de uso con opiniones de usuarios y benchmark | docs/03 (benchmark, PU-01 a PU-12, guion DEMO, CU-01 a CU-32) | Ejecución del guion DEMO por el equipo |
| REQ-13 | Modelo de dominio y contratos | docs/05; specs/schemas (11 contratos) | Metaesquema + ejemplos |
| REQ-14 | Diagramas MVC con visión hexagonal | docs/06, Figuras 6.1 a 6.8; diagramas/fuentes | Revisión de arquitectura |
| REQ-15 | Endpoints y servicios (contratos API) | specs/openapi (50 operaciones); docs/07 | openapi-spec-validator + Redocly |
| REQ-16 | Matriz de trazabilidad normativa SUNAT ↔ UBL | docs/08; matriz-trazabilidad.xlsx; csv/ | Cardinalidades calculadas del XSD oficial |
| REQ-17 | Venta mayorista y minorista | producto.schema.json (Presentacion, Precio); FormaPago; bonificaciones | Ejemplos F001-1 y B001-1 |
| REQ-18 | Inventario integrado a la facturación | Evento ComprobanteEmitido; VAL-INV-03; inventario.schema.json | TC-16; AsyncAPI |
| REQ-19 | Continuidad ante caídas de internet, OSE o SUNAT | ADR-05; outbox; reintentos; contingencia (RS 000049-2026) | Prueba de desconexión controlada |
<!-- fin:matriz-d -->
