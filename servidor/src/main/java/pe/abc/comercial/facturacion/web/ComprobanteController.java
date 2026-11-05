package pe.abc.comercial.facturacion.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.CasosDeUso.CalcularComprobanteUseCase;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.CasosDeUso.ConsultarComprobantesUseCase;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.CasosDeUso.DescargarDocumentoElectronicoUseCase;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.CasosDeUso.EmitirComprobanteUseCase;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.SolicitudEmision;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.CalculoVista;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.ComprobanteVista;

import java.net.URI;
import java.util.UUID;

/**
 * Controlador (MVC) = adaptador primario (hexagonal). Atiende las operaciones de
 * {@code specs/openapi/abc-comercial-api.yaml} y solo conoce los puertos de entrada (docs/06, Tabla 6.2).
 */
@RestController
@RequestMapping("/api/v1/comprobantes")
public class ComprobanteController {

    private final EmitirComprobanteUseCase emitir;
    private final CalcularComprobanteUseCase calcular;
    private final ConsultarComprobantesUseCase consultar;
    private final DescargarDocumentoElectronicoUseCase descargar;
    private final ValidadorContrato contrato;
    private final ObjectMapper json;

    public ComprobanteController(EmitirComprobanteUseCase emitir, CalcularComprobanteUseCase calcular,
                                 ConsultarComprobantesUseCase consultar, DescargarDocumentoElectronicoUseCase descargar,
                                 ValidadorContrato contrato, ObjectMapper json) {
        this.emitir = emitir;
        this.calcular = calcular;
        this.consultar = consultar;
        this.descargar = descargar;
        this.contrato = contrato;
        this.json = json;
    }

    /** emitirComprobante → EmitirComprobanteUseCase. 201 nuevo; 200 reintento idempotente. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ComprobanteVista> emitir(@RequestHeader("Idempotency-Key") String claveIdempotencia,
                                                   @RequestBody JsonNode cuerpo) {
        var resultado = emitir.emitir(solicitud(cuerpo), claveIdempotencia);
        var comprobante = resultado.comprobante();
        if (resultado.reintento()) {
            return ResponseEntity.ok(comprobante);
        }
        return ResponseEntity.created(URI.create(comprobante.enlaces().self())).body(comprobante);
    }

    /** calcularComprobante → CalcularComprobanteUseCase (mismo motor, sin numerar). */
    @PostMapping(path = "/calculos", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CalculoVista calcular(@RequestBody JsonNode cuerpo) {
        return calcular.calcular(solicitud(cuerpo));
    }

    /** obtenerComprobante → ConsultarComprobantesUseCase. */
    @GetMapping("/{comprobanteId}")
    public ComprobanteVista obtener(@PathVariable UUID comprobanteId) {
        return consultar.obtener(comprobanteId);
    }

    /** descargarXml → DescargarDocumentoElectronicoUseCase. */
    @GetMapping(path = "/{comprobanteId}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> descargarXml(@PathVariable UUID comprobanteId) {
        var archivo = descargar.descargarXml(comprobanteId);
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombreArchivo()).build().toString())
                .contentType(MediaType.APPLICATION_XML)
                .body(archivo.contenido());
    }

    private SolicitudEmision solicitud(JsonNode cuerpo) {
        contrato.validar(cuerpo);
        try {
            return json.treeToValue(cuerpo, SolicitudEmision.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cuerpo ilegible", e);
        }
    }
}
