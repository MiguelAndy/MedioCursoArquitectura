# Esquemas XSD oficiales de UBL 2.1 (OASIS)

Copia sin modificaciones de los esquemas *runtime* (`xsdrt`) del paquete oficial
**OASIS Universal Business Language (UBL) 2.1 OS**, publicado el 04-11-2013
(<https://docs.oasis-open.org/ubl/os-UBL-2.1/>). UBL 2.1 también está aprobado como
la norma ISO/IEC 19845:2015.

- `maindoc/UBL-Invoice-2.1.xsd`: el archivo que el enunciado pide estudiar (factura y boleta).
- `maindoc/UBL-CreditNote-2.1.xsd` y `maindoc/UBL-DebitNote-2.1.xsd`: notas de crédito y débito.
- `common/`: componentes comunes (`cac`, `cbc`, `ext`), tipos de datos y el esquema de XMLDSig
  que valida la firma alojada en `ext:ExtensionContent` (procesamiento `lax`).

Copyright (c) OASIS Open 2013. Se redistribuyen bajo la política de propiedad intelectual
de OASIS, conservando los avisos de copyright de cada archivo.

**Importante:** el XSD valida solo la *estructura* UBL. Las reglas del perfil SUNAT
(qué campos opcionales del XSD son obligatorios en Perú, valores de los catálogos,
cuadres de importes) se validan en las capas 2 y 3 descritas en
`docs/04-reglas-de-negocio-y-validaciones.md`.
