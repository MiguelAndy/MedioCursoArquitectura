# 7. Entregable 3: especificación de endpoints y servicios (contratos API)

## 7.1 Enfoque y convenciones

- **Fuente de verdad:** `specs/openapi/abc-comercial-api.yaml` (OpenAPI 3.1, validado con openapi-spec-validator y Redocly sin errores). Los esquemas viven en `specs/schemas/` y el OpenAPI solo los referencia.
- **Base:** `https://servidor-abc:8443/api/v1`, solo HTTPS. La versión mayor va en la ruta; los cambios compatibles (campos opcionales nuevos) no cambian de versión.
- **Seguridad:** `Authorization: Bearer <JWT>`. Cada operación declara los roles permitidos (`x-roles`).
- **Formatos:** JSON UTF-8; fechas `YYYY-MM-DD`; importes como texto decimal con el formato SUNAT n(12,2) y n(12,10).
- **Errores:** `application/problem+json` (RFC 9457) con `errores[]` que citan la regla `VAL-*` o `MIG-*`, el campo (JSON Pointer), la norma y el código SUNAT relacionado.
- **Idempotencia:** `POST /comprobantes` exige `Idempotency-Key`. Misma clave y mismo cuerpo → 200 con el comprobante existente; misma clave con otro cuerpo → 409.
- **Concurrencia:** `ETag` en las lecturas y `If-Match` en las actualizaciones (412 si alguien modificó antes).
- **Paginación:** `pagina` (desde 0) y `tamano` (hasta 200); respuesta `{ elementos, pagina: { pagina, tamano, totalElementos } }`.
- **Trazabilidad en el contrato:** cada operación declara `x-modulo` (contexto), `x-puerto-entrada` (caso de uso hexagonal), `x-casos-uso` y `x-reglas`. Así el contrato muestra la arquitectura que pide el enunciado (REQ-10).
- **Asincronía:** las operaciones largas (envío de resúmenes, carga de lotes, importaciones) responden `202 Accepted` con un recurso que se consulta después.

## 7.2 Catálogo de endpoints

La Tabla 7.1 se genera desde el OpenAPI con `python tools/generar_matrices.py` y nunca se edita a mano.

**Tabla 7.1.** *Operaciones de la API (generada desde el contrato)*

<!-- inicio:tabla-endpoints -->
| Método | Ruta | operationId | CU | Módulo | Puerto de entrada | Roles |
|---|---|---|---|---|---|---|
| POST | `/auth/token` | obtenerToken | CU-27 | seguridad | AutenticarUsuarioUseCase | público |
| POST | `/auth/token/renovacion` | renovarToken |  | seguridad | AutenticarUsuarioUseCase | público |
| GET | `/configuracion/empresa` | obtenerEmpresa | CU-24 | configuracion | ConsultarEmpresaUseCase | autenticado |
| GET | `/configuracion/series` | listarSeries | CU-24 | facturacion | GestionarSeriesUseCase | autenticado |
| POST | `/configuracion/series` | crearSerie | CU-24 | facturacion | GestionarSeriesUseCase | ADMINISTRADOR |
| POST | `/configuracion/certificados` | cargarCertificado | CU-25 | cpe | GestionarCertificadoUseCase | ADMINISTRADOR |
| GET | `/configuracion/certificados/vigente` | obtenerCertificadoVigente | CU-25 | cpe | GestionarCertificadoUseCase | autenticado |
| PUT | `/configuracion/canal-envio` | configurarCanalEnvio | CU-26 | cpe | ConfigurarCanalEnvioUseCase | ADMINISTRADOR |
| GET | `/catalogos/{numero}` | obtenerCatalogo | CU-12 | tributacion | ConsultarCatalogoUseCase | autenticado |
| GET | `/parametros-tributarios` | listarParametrosTributarios |  | tributacion | ConsultarParametrosTributariosUseCase | autenticado |
| GET | `/terceros` | buscarTerceros | CU-11 | terceros | BuscarTercerosUseCase | autenticado |
| POST | `/terceros` | registrarTercero | CU-11 | terceros | RegistrarTerceroUseCase | autenticado |
| GET | `/terceros/{terceroId}` | obtenerTercero |  | terceros | BuscarTercerosUseCase | autenticado |
| PUT | `/terceros/{terceroId}` | actualizarTercero |  | terceros | RegistrarTerceroUseCase | autenticado |
| GET | `/consultas/ruc/{ruc}` | consultarRuc | CU-11 | terceros | ConsultarRucUseCase | autenticado |
| GET | `/productos` | buscarProductos | CU-12 | catalogo | BuscarProductosUseCase | autenticado |
| POST | `/productos` | crearProducto | CU-12 | catalogo | GestionarProductoUseCase | ADMINISTRADOR, CONTADOR |
| GET | `/productos/{productoId}` | obtenerProducto |  | catalogo | BuscarProductosUseCase | autenticado |
| PUT | `/productos/{productoId}` | actualizarProducto |  | catalogo | GestionarProductoUseCase | ADMINISTRADOR, CONTADOR |
| POST | `/productos/importaciones` | importarProductos | CU-13 | catalogo | ImportarProductosUseCase | ADMINISTRADOR |
| GET | `/inventario/existencias` | consultarExistencias | CU-16 | inventario | ConsultarExistenciasUseCase | autenticado |
| GET | `/inventario/movimientos` | listarMovimientos | CU-16 | inventario | ConsultarExistenciasUseCase | autenticado |
| POST | `/inventario/movimientos` | registrarMovimiento | CU-14, CU-15 | inventario | RegistrarMovimientoUseCase | ALMACENERO, ADMINISTRADOR |
| GET | `/inventario/kardex/{productoId}` | obtenerKardex | CU-16, CU-21 | inventario | ConsultarKardexUseCase | autenticado |
| POST | `/comprobantes/calculos` | calcularComprobante | CU-01, CU-02, CU-03 | facturacion | CalcularComprobanteUseCase | autenticado |
| GET | `/comprobantes` | listarComprobantes | CU-10 | facturacion | ConsultarComprobantesUseCase | autenticado |
| POST | `/comprobantes` | emitirComprobante | CU-01, CU-02, CU-03, CU-04, CU-07 | facturacion | EmitirComprobanteUseCase | CAJERO, VENDEDOR, CONTADOR |
| GET | `/comprobantes/{comprobanteId}` | obtenerComprobante | CU-10 | facturacion | ConsultarComprobantesUseCase | autenticado |
| GET | `/comprobantes/{comprobanteId}/xml` | descargarXml | CU-09 | cpe | DescargarDocumentoElectronicoUseCase | autenticado |
| GET | `/comprobantes/{comprobanteId}/cdr` | descargarCdr | CU-08 | cpe | DescargarDocumentoElectronicoUseCase | autenticado |
| GET | `/comprobantes/{comprobanteId}/representacion-impresa` | descargarRepresentacionImpresa | CU-09 | cpe | GenerarRepresentacionImpresaUseCase | autenticado |
| POST | `/comprobantes/{comprobanteId}/reenvios` | reenviarComprobante | CU-07 | cpe | EnviarComprobanteUseCase | autenticado |
| POST | `/comprobantes/{comprobanteId}/consultas-sunat` | consultarEstadoSunat | CU-08 | cpe | ConsultarEstadoCpeUseCase | autenticado |
| POST | `/comprobantes/{comprobanteId}/notificaciones` | notificarCliente | CU-09 | notificaciones | NotificarComprobanteUseCase | autenticado |
| POST | `/comunicaciones-baja` | comunicarBaja | CU-05 | cpe | AnularComprobanteUseCase | CONTADOR, ADMINISTRADOR |
| GET | `/comunicaciones-baja/{bajaId}` | obtenerComunicacionBaja | CU-05 | cpe | AnularComprobanteUseCase | autenticado |
| POST | `/resumenes-diarios` | generarResumenDiario | CU-06 | cpe | GenerarResumenDiarioUseCase | CONTADOR, ADMINISTRADOR |
| GET | `/resumenes-diarios/{resumenId}` | obtenerResumenDiario | CU-06 | cpe | GenerarResumenDiarioUseCase | autenticado |
| GET | `/registros/ventas` | obtenerRegistroVentas | CU-20 | registros | GenerarRegistroVentasUseCase | autenticado |
| GET | `/registros/inventario-unidades-fisicas` | obtenerRegistroInventario | CU-21 | registros | GenerarRegistroInventarioUseCase | autenticado |
| POST | `/migraciones/lotes` | crearLoteMigracion | CU-17 | migracion | CargarLoteLegadoUseCase | ADMINISTRADOR |
| GET | `/migraciones/lotes/{loteId}` | obtenerLote | CU-17, CU-18 | migracion | ConsultarLoteUseCase | autenticado |
| GET | `/migraciones/lotes/{loteId}/registros` | listarRegistrosLote | CU-18 | migracion | ConsultarLoteUseCase | autenticado |
| PATCH | `/migraciones/registros/{registroId}` | resolverRegistro | CU-18 | migracion | ResolverRegistroObservadoUseCase | ADMINISTRADOR, CONTADOR |
| GET | `/migraciones/lotes/{loteId}/conciliacion` | obtenerConciliacion | CU-19 | migracion | ConciliarLoteUseCase | autenticado |
| POST | `/migraciones/lotes/{loteId}/confirmacion` | confirmarLote | CU-19 | migracion | ConfirmarLoteUseCase | GERENTE, CONTADOR |
| GET | `/migraciones/linaje` | consultarLinaje | CU-19 | migracion | ConsultarLinajeUseCase | autenticado |
| GET | `/auditoria/eventos` | consultarAuditoria | CU-28 | seguridad | ConsultarAuditoriaUseCase | ADMINISTRADOR, CONTADOR, GERENTE |
| GET | `/sistema/salud` | obtenerSalud |  | plataforma |  | público |
| GET | `/sistema/version-cliente` | obtenerVersionCliente | CU-29 | plataforma |  | público |
<!-- fin:tabla-endpoints -->

## 7.3 Operaciones críticas

### `POST /comprobantes`: emitir un comprobante

| Elemento | Especificación |
|---|---|
| Caso de uso / puerto | CU-01 a CU-04 / `EmitirComprobanteUseCase` |
| Cabeceras | `Authorization`, `Idempotency-Key` (UUID, obligatoria) |
| Cuerpo | `EmitirComprobanteRequest` (`comprobante-emision.request.schema.json`) |
| 201 | `Comprobante` con estado `EMITIDO`, `numero`, `documentoElectronico.hash`, `codigoQr` y `enlaces` (xml, cdr, pdf); cabeceras `Location` y `ETag` |
| 200 | Reintento idempotente: el mismo comprobante |
| 400 | No cumple el JSON Schema (VAL-EST-01) |
| 409 | `Idempotency-Key` reutilizada con otro cuerpo; serie inactiva o de otro punto de emisión |
| 422 | Viola reglas de negocio o normativas (lista de `VAL-*`); **no consume número** |
| 503 | Base de datos o certificado no disponibles; la venta se reintenta con la misma clave |

Ejemplo de respuesta 422:

```json
{
  "type": "/problemas/validacion-negocio",
  "title": "El comprobante no cumple las reglas de emisión",
  "status": 422,
  "errores": [
    {
      "regla": "VAL-BOL-01",
      "campo": "/cliente/numeroDocumento",
      "mensaje": "Para boletas mayores a S/ 700 se requiere el tipo y número de documento del adquirente.",
      "severidad": "ERROR",
      "fuenteNormativa": "RS 097-2012/SUNAT, anexo de boleta (modif. RS 114-2019/SUNAT), campo 11"
    }
  ]
}
```

### `POST /comprobantes/calculos`: simular sin emitir

Usa el mismo cuerpo que la emisión y devuelve `ResultadoCalculo` (ítems calculados, totales, leyendas y advertencias). No numera ni persiste nada. Las vistas lo llaman cada vez que cambia el carrito, así que lo que ve el cajero es exactamente lo que irá en el XML (ADR-07).

### `GET /comprobantes/{id}` y descargas

`Comprobante` con `estado`, `respuestaSunat` (código, descripción y observaciones del CDR) e `historialEnvios`. Las descargas usan `/xml` (`application/xml`), `/cdr` (`application/zip`) y `/representacion-impresa?formato=A4|TICKET_80MM|TICKET_58MM` (`application/pdf`).

### `POST /comunicaciones-baja` y `POST /resumenes-diarios`

Generan el documento RA o RC, lo firman, lo envían con `sendSummary` y responden `202` con el recurso y el `ticket`. El estado se consulta en `GET /comunicaciones-baja/{id}` o `GET /resumenes-diarios/{id}`, que el servidor actualiza con `getStatus` (VAL-ENV-04).

### `GET /consultas/ruc/{ruc}`

Devuelve `formatoValido`, `digitoVerificadorValido`, `encontrado`, estado, condición, dirección y `aptoParaFactura`, más las advertencias (por ejemplo, NO HABIDO). Si las fuentes externas no responden, contesta 503 con `Retry-After`.

### Migración: `/migraciones/*`

1. `POST /migraciones/lotes` (multipart: archivo + `CrearLoteRequest`) → 202 `LoteMigracion` en estado `CARGADO`.
2. `GET /migraciones/lotes/{id}/registros?estado=OBSERVADO` → registros por revisar.
3. `PATCH /migraciones/registros/{id}` (`application/merge-patch+json`, `ResolucionRegistro` con justificación obligatoria).
4. `GET /migraciones/lotes/{id}/conciliacion` → controles CTL-01 a CTL-05.
5. `POST /migraciones/lotes/{id}/confirmacion` → 202; o 409 si la conciliación no es conforme o quedan registros observados.
6. `GET /migraciones/linaje?idLegado=...` o `?entidad=MovimientoInventario&entidadId=...` → linaje en ambos sentidos.

## 7.4 Códigos HTTP

**Tabla 7.2.** *Uso de los códigos de estado*

| Código | Significado en esta API |
|---|---|
| 200 / 201 / 202 | Correcto / creado (`Location`) / aceptado para proceso asíncrono |
| 400 | No cumple el contrato (capa 1) |
| 401 / 403 | Sin token o expirado / rol insuficiente |
| 404 | Recurso inexistente |
| 409 | Conflicto de estado (duplicado, idempotencia, lote no conforme, stock) |
| 412 | `If-Match` desactualizado |
| 422 | Regla de negocio o normativa (capa 2) |
| 426 | Cliente MSI/APK por debajo de la versión mínima |
| 429 | Límite de solicitudes (endpoints públicos) |
| 503 | Dependencia externa caída (padrón RUC, OSE/SUNAT); incluye `Retry-After` |

## 7.5 Servicios internos: puertos del núcleo

Son los contratos entre los controladores (MVC) y el Modelo, y entre el Modelo y la infraestructura. Están escritos en Java como interfaces, sin anotaciones de frameworks.

```java
// ---- Puertos de entrada (facturacion) ----
public interface EmitirComprobanteUseCase {
    ComprobanteEmitido emitir(EmitirComprobanteComando comando, ClaveIdempotencia clave);
}
public interface CalcularComprobanteUseCase {
    ResultadoCalculo calcular(EmitirComprobanteComando comando);
}
public interface ConsultarComprobantesUseCase {
    Optional<VistaComprobante> obtener(ComprobanteId id);
    Pagina<ResumenComprobante> buscar(FiltroComprobantes filtro, Paginacion paginacion);
}

// ---- Puertos de salida (facturacion) ----
public interface ComprobanteRepository {
    void guardar(Comprobante comprobante);
    Optional<Comprobante> buscar(ComprobanteId id);
    Optional<Comprobante> buscarPorNumero(TipoComprobante tipo, NumeroComprobante numero);
}
public interface NumeradorPort {
    NumeroComprobante siguiente(String serie);      // atómico, dentro de la transacción de emisión
}
public interface DocumentoElectronicoPort {
    DocumentoFirmado generarYFirmar(ComprobanteSnapshot snapshot);   // UBL 2.1 + XMLDSig (módulo cpe)
}
public interface ParametrosTributariosPort {
    ParametrosVigentes vigentesAl(LocalDate fecha);                  // IGV, ICBPER, TasaIsc, umbrales
}

// ---- Puertos de salida (cpe) ----
public interface GeneradorUblPort {
    Document generar(ComprobanteElectronico comprobante);
    byte[] serializar(Document documento);
}
public interface FirmadorXmlPort {
    XmlFirmado firmar(Document ubl, CertificadoRef certificado);
}
public interface CanalEnvioCpePort {
    RespuestaCdr enviarComprobante(ArchivoZip zip);                  // sendBill
    Ticket enviarResumen(ArchivoZip zip);                             // sendSummary (RC / RA)
    EstadoTicket consultarTicket(Ticket ticket);                      // getStatus: 0 | 98 | 99
    RespuestaCdr consultarCdr(Ruc emisor, TipoComprobante tipo, NumeroComprobante numero); // getStatusCdr
}

// ---- Puertos de salida (terceros) ----
public interface ConsultaContribuyentePort {
    Optional<Contribuyente> consultar(Ruc ruc);
}
```

## 7.6 Contrato con SUNAT / OSE (servicio SOAP `billService`)

**Tabla 7.3.** *Operaciones del servicio web del SEE*

| Operación | Uso | Entrada | Salida |
|---|---|---|---|
| `sendBill` | Factura, nota o boleta enviada en forma individual | `fileName` (`RUC-TT-SERIE-NUMERO.zip`), `contentFile` (ZIP en Base64) | `applicationResponse`: ZIP con el CDR `R-RUC-TT-SERIE-NUMERO.xml` |
| `sendSummary` | Resumen diario (RC) y comunicación de baja (RA) | `fileName` (`RUC-RC-AAAAMMDD-n.zip`) y `contentFile` | `ticket` |
| `getStatus` | Resultado de un ticket | `ticket` | `statusCode` (0, 98, 99) + `content` (ZIP con el CDR) |
| `getStatusCdr` | Recuperar el CDR de un comprobante | RUC, tipo, serie y número | Código, mensaje y CDR |

- **Seguridad:** WS-Security `UsernameToken` con RUC + usuario SOL secundario (en beta: `RUC` + `MODDATOS` / `moddatos`). Ambientes: beta `https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService`; producción `https://e-factura.sunat.gob.pe/ol-ti-itcpfegem/billService`. Un OSE expone su propio *endpoint* con la misma interfaz.

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ser="http://service.sunat.gob.pe"
                  xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
  <soapenv:Header>
    <wsse:Security>
      <wsse:UsernameToken>
        <wsse:Username>20123456786MODDATOS</wsse:Username>
        <wsse:Password>moddatos</wsse:Password>
      </wsse:UsernameToken>
    </wsse:Security>
  </soapenv:Header>
  <soapenv:Body>
    <ser:sendBill>
      <fileName>20123456786-01-F001-1.zip</fileName>
      <contentFile>UEsDBBQAAAAIA...(ZIP en Base64)</contentFile>
    </ser:sendBill>
  </soapenv:Body>
</soapenv:Envelope>
```

**Tabla 7.4.** *De la respuesta de SUNAT/OSE al estado del comprobante*

| Respuesta | Estado | Acción del sistema |
|---|---|---|
| CDR con `ResponseCode` 0 | ACEPTADO | Guardar el CDR; publicar `ResultadoSunat`; notificar al cliente si se pidió |
| CDR con observaciones (código 4000 o más) | OBSERVADO | Válido; registrar las observaciones y alertar al contador para corregir a futuro |
| CDR de rechazo (2000–3999) | RECHAZADO | Número consumido; alertar; emitir un comprobante nuevo (VAL-NUM-05) |
| Error SOAP 0100–0999 (SUNAT) | EXCEPCION | Reintento automático con *backoff*, mismo número |
| Error SOAP 1000–1999 (formato del contribuyente) | EXCEPCION | Bloquear la cola; corregir el generador y reenviar con el mismo número |
| Tiempo agotado o sin conexión | EXCEPCION | **Antes de reenviar, consultar `getStatusCdr`**: el documento pudo haber llegado, y reenviarlo con otros datos daría el error 1033 ("registrado previamente con otros datos") |
| Ticket con `statusCode` 98 | ENVIADO | Volver a consultar con intervalo creciente |
| Ticket con `statusCode` 0 / 99 | ACEPTADO o ANULADO / rechazo del resumen | Procesar el CDR; si es 99, corregir y reenviar (puede usarse el mismo nombre de archivo) |

## 7.7 Otros servicios externos

| Servicio | Puerto | Contrato | Política ante fallas |
|---|---|---|---|
| Padrón reducido del RUC (archivo publicado por SUNAT) | `ConsultaContribuyentePort` | Descarga diaria, indexada localmente | Si falla la descarga, se usa la del día anterior y se registra una advertencia |
| Servicio externo de consulta de RUC/DNI (respaldo) | `ConsultaContribuyentePort` | HTTPS/JSON con clave de API | Disyuntor (*circuit breaker*) y caché de 24 horas |
| Correo (SMTP) y enlace de WhatsApp | `NotificadorPort` | SMTP con TLS / enlace firmado de descarga | Cola con reintentos |

## 7.8 Contratos asíncronos

`specs/asyncapi/abc-comercial-eventos.yaml` (AsyncAPI 3.0, validado con el CLI oficial) define 6 canales y 12 operaciones. Las cargas se reutilizan de `evento.schema.json` y el sobre es CloudEvents 1.0. En la fase 1 el transporte es el bus interno con outbox; en la fase 3, un broker AMQP. **El contrato no cambia** entre fases: es la costura que hace barata la extracción de microservicios.

## 7.9 Versionado y pruebas de contrato

- Se aplica SemVer al contrato: los cambios incompatibles crean `/api/v2` y la versión anterior se mantiene durante un periodo de transición. Los clientes envían su versión al iniciar sesión y el servidor puede exigir actualización (426).
- En CI:
  - `tools/validar_specs.py` valida esquemas, ejemplos, XSD, firma y cuadres.
  - Redocly hace el *lint* del OpenAPI.
  - Una prueba de propiedades (Schemathesis) ataca la API desplegada contra su OpenAPI.
  - Los ejemplos JSON sirven como pruebas de aceptación del dominio (Tabla 4.6).
