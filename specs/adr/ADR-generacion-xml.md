# ADR-11: Generación de XML UBL 2.1 con Clases JAXB (xjc), Marshaller y Validación contra Esquema XSD

## Estado
Aceptado (Vigente)

## Fecha
2026-11-04

## Contexto
El sistema de facturación electrónica de ABC Software Corp debe emitir comprobantes de pago electrónicos (facturas, boletas de venta, notas de crédito y débito) en cumplimiento con el estándar OASIS UBL 2.1 y la normativa técnica de SUNAT (Resolución de Superintendencia N.° 097-2012/SUNAT y modificatorias).

Los esquemas XML Schema Definition (XSD) oficiales de UBL 2.1 (`xsdrt` / `specs/ubl/xsd-oasis-ubl-2.1/`) imponen requerimientos técnicos estrictos:
1. **Secuencia estricta de elementos (`xsd:sequence`):** La alteración del orden de las etiquetas XML invalida inmediatamente el comprobante ante los validadores de SUNAT u OSE.
2. **Estructura compleja de tipos de datos:** Jerarquía extensa de tipos complejos agregados (`cac`), componentes básicos (`cbc`) y extensiones (`ext`), sumando más de 1 200 definiciones.
3. **Serialización canónica para firma digital:** La firma enveloped en `ext:ExtensionContent` requiere que la estructura y los prefijos de espacios de nombres (`xmlns:cac`, `xmlns:cbc`, `xmlns:ext`, `xmlns:ds`) se declaren de forma determinista para que la canonización (C14N) y el cálculo de resumen (`ds:DigestValue`) sean consistentes y reproducibles.

Se requiere formalizar la decisión arquitectónica para la técnica de generación del XML UBL 2.1, garantizando tipado estático, orden normativo, mantenibilidad y validación previa a la firma digital.

## Decisión
Se adopta la generación de documentos XML UBL 2.1 mediante **JAXB (Java Architecture for XML Binding)** utilizando clases generadas con el compilador `xjc` a partir de los esquemas XSD oficiales `xsdrt`, serialización mediante `Marshaller` y validación contra esquema XSD previa a la firma digital:

1. **Generación de Clases a partir del XSD (`xjc`):**
   - Se utiliza el compilador `xjc` (integrado mediante `jaxb-maven-plugin`) para compilar los esquemas XSD oficiales (`UBL-Invoice-2.1.xsd`, `UBL-CreditNote-2.1.xsd`, `UBL-DebitNote-2.1.xsd` y sus componentes comunes).
   - Las clases generadas (como `oasis.names.specification.ubl.schema.xsd.invoice_2.InvoiceType`) constituyen el modelo canónico UBL en tiempo de compilación. No se mantienen clases XML escritas manualmente.

2. **Serialización mediante `Marshaller`:**
   - La serialización desde el modelo generado hacia el árbol XML se realiza con `jakarta.xml.bind.Marshaller`.
   - Se configura codificación `UTF-8`, formateo con sangría de 2 espacios (`JAXB_FORMATTED_OUTPUT`, `org.glassfish.jaxb.indentString`) y asignación explícita de prefijos canónicos (`NamespacePrefixMapper`) para los espacios de nombres `cac`, `cbc`, `ext` y `ds`.
   - El mapeo del modelo de dominio (`ComprobanteElectronico`) a las clases JAXB se centraliza en un componente especializado (`InvoiceUblMapper`).

3. **Validación contra el Esquema XSD antes de Firmar:**
   - Se emplea el validador de esquemas (`javax.xml.validation.SchemaFactory` con el catálogo XSD de OASIS UBL 2.1 `xsdrt`) asociado al `Marshaller` o ejecutado sobre el documento en memoria inmediatamente tras la serialización.
   - Todo comprobante se valida estructuralmente contra el XSD antes de invocar el adaptador de firma digital (`FirmadorXmlPort`). Si el documento no cumple la especificación XSD, el proceso se interrumpe antes de consumir el certificado digital ni asignar recursos criptográficos.

4. **Aislamiento Arquitectónico en Hexágono:**
   - La generación XML se encapsula detrás del puerto secundario `GeneradorUblPort` dentro del módulo `cpe`.
   - La implementación concreta (`GeneradorUblAdapter`) aísla al dominio y a la aplicación de los detalles de serialización DOM y dependencias JAXB.

## Consecuencias

### Positivas
- **Seguridad de Tipos en Compilación:** Detección de incompatibilidades de tipos y cardinalidades en tiempo de desarrollo.
- **Cumplimiento Automático de `xsd:sequence`:** El compilador `xjc` preserva rigurosamente el orden de los elementos según los esquemas OASIS.
- **Validación Temprana (Capa 3):** Garantía de validez estructural previa al consumo de recursos de firma criptográfica y outbox.
- **Canonización Determinista:** Producción de XML homogéneo con prefijos de namespace consistentes para la verificación de firma digital.

### Alternativas Descartadas
- **Plantillas de Texto (FreeMarker / Velocity / Thymeleaf):** Descartadas por falta de validación de tipos, riesgo de generación de XML mal formado ante caracteres especiales y fragilidad ante cambios normativos en el XSD.
- **Construcción Manual DOM (`DocumentBuilder`):** Descartada por alta verbosidad, propensión a errores en el orden de nodos y alto costo de mantenimiento para más de un millar de elementos UBL.
- **Concatenación de Strings (`StringBuilder`):** Descartada por violar principios básicos de arquitectura, riesgos de inyección XML y falta total de validación estructural.

