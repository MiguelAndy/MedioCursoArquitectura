# 10. Conclusiones, siguientes pasos y referencias

## 10.1 Conclusiones

1. **El problema de ABC no es "hacer facturas", sino formalizar una operación.** El legado ya es un kárdex informal: sus cinco campos coinciden con el Formato 12.1 de la RS 234-2006. El diseño aprovecha esa coincidencia para que la migración formalice lo existente (docs/09) y para que inventario, comprobantes y registros compartan los mismos datos (Matrices B y C).
2. **El XSD de UBL 2.1 es necesario, pero no suficiente.** El estudio de `UBL-Invoice-2.1.xsd` mostró que solo seis elementos raíz son obligatorios, mientras que el perfil SUNAT exige muchos más: 21 de los 56 datos de la Matriz A son obligatorios para SUNAT pero opcionales en el XSD. Por eso la validación se diseñó en capas (contrato → dominio → documento → autoridad).
3. **El "catálogo 8" tiene dos lecturas y ambas cambian el diseño.** El Anexo N.° 8 completo exige tratar los catálogos como datos versionados. El Catálogo N.° 08 (ISC) exige un motor que soporte tres sistemas de cálculo, que incluya el ISC en la base del IGV y que tenga parametrizado quién es sujeto del impuesto (SUP-03).
4. **Spec-First produjo artefactos verificables, no solo documentos:**
   - 11 contratos JSON Schema;
   - un OpenAPI 3.1 con 50 operaciones, validado sin errores;
   - un AsyncAPI 3.0 validado;
   - 6 ejemplos JSON válidos y 7 contraejemplos que fallan por la regla correcta;
   - 4 XML UBL 2.1 firmados que validan contra el XSD oficial y cuadran al céntimo.

   Todo se comprueba con un solo comando (`tools/validar_specs.py`).
5. **La arquitectura cumple la restricción "MVC hoy, hexagonal y microservicios mañana"** porque el Modelo nace como núcleo hexagonal (puertos y adaptadores). Cada operación del contrato declara su puerto, los eventos ya tienen contrato propio y las reglas de dependencia se verifican en CI.

## 10.2 Siguientes pasos

| Iteración | Objetivo | Criterio de salida |
|---|---|---|
| 0 | Aprobar esta especificación con gerencia y contador; ejecutar el guion DEMO (docs/03, sección 3.4); resolver los supuestos SUP-* | Specs firmados (versión 1.0) |
| 1 | Generar interfaces y SDK desde el OpenAPI; dominio, motor tributario y validador con las pruebas TC-01 a TC-16 | 100 % de los TC en verde |
| 2 | Módulo `cpe`: generador UBL, firma con el certificado real y envío al ambiente beta del OSE o de SUNAT | Los 4 escenarios aceptados en beta |
| 3 | Inventario y migración: ensayo con la exportación real del legado | Observados < 5 % y CTL-01 a CTL-05 conformes |
| 4 | Clientes Windows (MSI) y Android (APK); piloto en una caja | p95 < 1,5 s; cero diferencias de importes |
| 5 | Corrida en paralelo, corte y puesta en producción | Conciliación aprobada; primer CDR de producción aceptado |

## 10.3 Referencias

AsyncAPI Initiative. (2023). *AsyncAPI specification 3.0.0*. https://www.asyncapi.com/docs/reference/specification/v3.0.0

Brown, S. (2018). *The C4 model for visualising software architecture*. https://c4model.com

BYB Consultores. (2026, 6 de mayo). *SUNAT: obligación de facturas electrónicas y SIRE 2026*. https://bybconsultores.pe/comprobantes-de-pago/sunat-obligacion-facturas-electronicas-sire-2026/

Cloud Native Computing Foundation. (2019). *CloudEvents specification v1.0*. https://cloudevents.io

Cockburn, A. (2005). *Hexagonal architecture*. https://alistair.cockburn.us/hexagonal-architecture/

Decreto Supremo N.° 055-99-EF. (1999). *Texto Único Ordenado de la Ley del Impuesto General a las Ventas e Impuesto Selectivo al Consumo*. Ministerio de Economía y Finanzas.

Evans, E. (2003). *Domain-driven design: Tackling complexity in the heart of software*. Addison-Wesley.

International Organization for Standardization & International Electrotechnical Commission. (2015). *Information technology: Universal business language version 2.1 (UBL v2.1)* (ISO/IEC 19845:2015).

JSON Schema. (2020). *JSON Schema: Draft 2020-12*. https://json-schema.org/draft/2020-12

Krasner, G. E., & Pope, S. T. (1988). A cookbook for using the model-view controller user interface paradigm in Smalltalk-80. *Journal of Object-Oriented Programming, 1*(3), 26–49.

Ley N.° 29733. (2011). *Ley de Protección de Datos Personales*. Congreso de la República del Perú.

Ley N.° 30884. (2018). *Ley que regula el plástico de un solo uso y los recipientes o envases descartables*. Congreso de la República del Perú.

Martin, R. C. (2017). *Clean architecture: A craftsman's guide to software structure and design*. Prentice Hall.

Newman, S. (2019). *Monolith to microservices: Evolutionary patterns to transform your monolith*. O'Reilly Media.

Nottingham, M., Wilde, E., & Dalal, S. (2023). *Problem details for HTTP APIs* (RFC 9457). Internet Engineering Task Force. https://www.rfc-editor.org/rfc/rfc9457

Nubefact. (2026a). *Facturación electrónica: OSE y PSE autorizado por SUNAT* [Sitio web]. https://www.nubefact.com

Nubefact. (2026b). *Noticias tributarias* [Blog]. https://blog.nubefact.com/noticias-tributarias

OASIS. (2013). *Universal Business Language version 2.1* (OASIS Standard). https://docs.oasis-open.org/ubl/os-UBL-2.1/

OpenAPI Initiative. (2021). *OpenAPI specification v3.1.0*. https://spec.openapis.org/oas/v3.1.0

Project OpenUBL. (s. f.). *OpenUBL: herramientas de código abierto para UBL y SUNAT* [Repositorio]. GitHub. https://github.com/project-openubl

Sovos. (2026). *Perú: RS N.° 000048-2026/SUNAT y la boleta de venta electrónica*. https://sovos.com/es/cambios-regulatorios/iva/peru-boleta-venta-electronica-rs-000048-2026-sunat/

Superintendencia Nacional de Aduanas y de Administración Tributaria. (2006). *Resolución de Superintendencia N.° 234-2006/SUNAT: Establecen normas referidas a libros y registros vinculados a asuntos tributarios* (formatos y tablas). https://www.sunat.gob.pe/legislacion/superin/2006/234.htm

Superintendencia Nacional de Aduanas y de Administración Tributaria. (2012). *Resolución de Superintendencia N.° 097-2012/SUNAT: Crean el Sistema de Emisión Electrónica desarrollado desde los sistemas del contribuyente*.

Superintendencia Nacional de Aduanas y de Administración Tributaria. (2017a). *Anexo N.° 8: Catálogo de códigos* (Anexo E de la Resolución de Superintendencia N.° 245-2017/SUNAT). https://www.sunat.gob.pe/legislacion/superin/2017/anexoE-245-2017.pdf

Superintendencia Nacional de Aduanas y de Administración Tributaria. (2017b). *Guía de elaboración de documentos XML: Factura electrónica UBL 2.1* (versión 1.0). https://cpe.sunat.gob.pe/sites/default/files/inline-files/guia%2Bxml%2Bfactura%2Bversion%202-1%2B1%2B0%20%282%29_0%20%282%29.pdf

Superintendencia Nacional de Aduanas y de Administración Tributaria. (2017c). *Resolución de Superintendencia N.° 117-2017/SUNAT* [Sistema de Emisión Electrónica - Operador de Servicios Electrónicos].

Superintendencia Nacional de Aduanas y de Administración Tributaria. (2019a). *Anexo N.° 9-A: Estándar UBL 2.1* (Anexo VII de la Resolución de Superintendencia N.° 114-2019/SUNAT). https://ww3.sunat.gob.pe/legislacion/superin/2019/anexoVII-114-2019.pdf

Superintendencia Nacional de Aduanas y de Administración Tributaria. (2019b). *Anexos de la Resolución de Superintendencia N.° 150-2019/SUNAT* [Adecuación de los comprobantes electrónicos al ICBPER]. https://ww3.sunat.gob.pe/legislacion/superin/2019/anexos-150-2019.pdf

Superintendencia Nacional de Aduanas y de Administración Tributaria. (2020). *Anexo 4 de la Resolución de Superintendencia N.° 193-2020/SUNAT* [Forma de pago y cuotas en UBL 2.1]. https://www.sunat.gob.pe/legislacion/superin/2020/anexo4-193-2020.pdf

Superintendencia Nacional de Aduanas y de Administración Tributaria. (2023). *Anexos de la Resolución de Superintendencia N.° 000005-2023/SUNAT*. https://www.sunat.gob.pe/legislacion/superin/2023/anexo-000005-2023.pdf

Superintendencia Nacional de Aduanas y de Administración Tributaria. (2026a). *Resolución de Superintendencia N.° 000049-2026/SUNAT* [Programa PEI WEB] y su anexo. https://www.sunat.gob.pe/legislacion/superin/2026/000049-2026.pdf

Superintendencia Nacional de Aduanas y de Administración Tributaria. (2026b). *Resolución de Superintendencia N.° 000075-2026/SUNAT*. https://www.sunat.gob.pe/legislacion/superin/2026/000075-2026.pdf

Superintendencia Nacional de Aduanas y de Administración Tributaria. (2026c). *Resolución de Superintendencia N.° 000141-2026/SUNAT*. https://www.sunat.gob.pe/legislacion/superin/2026/000141-2026.pdf

Superintendencia Nacional de Aduanas y de Administración Tributaria. (2026d). *Resolución de Superintendencia N.° 000143-2026/SUNAT*. https://img.lpderecho.pe/wp-content/uploads/2026/08/Res.-000143-2026-Sunat-LPDerecho.pdf

Superintendencia Nacional de Aduanas y de Administración Tributaria. (s. f.). *Manual del programador: SEE - Sistemas del contribuyente*. https://cpe.sunat.gob.pe/sites/default/files/inline-files/manual_programador%20%281%29.pdf

World Wide Web Consortium. (2013). *XML signature syntax and processing version 1.1* (W3C Recommendation). https://www.w3.org/TR/xmldsig-core1/
