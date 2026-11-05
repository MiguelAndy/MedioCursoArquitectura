package pe.abc.comercial.facturacion.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.abc.comercial.cpe.api.DocumentoElectronicoApi.DocumentoInvalidoException;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.CasosDeUso.ComprobanteNoEncontradoException;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.CasosDeUso.ConflictoIdempotenciaException;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.CasosDeUso.SolicitudRechazadaException;
import pe.abc.comercial.facturacion.web.ValidadorContrato.ContratoInvalidoException;

import java.util.List;

/**
 * Formato único de error: {@code specs/schemas/problema.schema.json} (RFC 9457). Cada error cita la regla VAL-*,
 * para que el cajero vea un mensaje comprensible y soporte sepa qué regla falló (RNF-10).
 */
@RestControllerAdvice
public class ManejadorProblemas {

    static final MediaType PROBLEMA = MediaType.valueOf("application/problem+json");

    public record ErrorRegla(String regla, String campo, String mensaje, String severidad) {
        static ErrorRegla error(String regla, String campo, String mensaje) {
            return new ErrorRegla(regla, campo, mensaje, "ERROR");
        }
    }

    public record Problema(String type, String title, int status, String detail, List<ErrorRegla> errores) {
    }

    /** Capa 1 → 400. */
    @ExceptionHandler(ContratoInvalidoException.class)
    ResponseEntity<Problema> contrato(ContratoInvalidoException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "/problemas/solicitud-invalida", "La solicitud no cumple su contrato",
                e.errores().stream().map(x -> ErrorRegla.error("VAL-EST-01", x.campo(), x.mensaje())).toList());
    }

    @ExceptionHandler({MissingRequestHeaderException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    ResponseEntity<Problema> malFormada(Exception e) {
        return respuesta(HttpStatus.BAD_REQUEST, "/problemas/solicitud-invalida", e.getMessage(), List.of());
    }

    /** Capa 2 → 422 (no se consume número). */
    @ExceptionHandler(SolicitudRechazadaException.class)
    ResponseEntity<Problema> negocio(SolicitudRechazadaException e) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "/problemas/validacion-negocio",
                "La solicitud incumple reglas de negocio",
                e.reglas().stream().map(r -> ErrorRegla.error(r.regla(), r.campo(), r.mensaje())).toList());
    }

    @ExceptionHandler(ComprobanteNoEncontradoException.class)
    ResponseEntity<Problema> noEncontrado(ComprobanteNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, "/problemas/no-encontrado", e.getMessage(), List.of());
    }

    @ExceptionHandler(ConflictoIdempotenciaException.class)
    ResponseEntity<Problema> conflicto(ConflictoIdempotenciaException e) {
        return respuesta(HttpStatus.CONFLICT, "/problemas/conflicto", e.getMessage(), List.of());
    }

    /** Capa 3: error interno, se alerta al equipo y el comprobante no se emite. */
    @ExceptionHandler(DocumentoInvalidoException.class)
    ResponseEntity<Problema> documento(DocumentoInvalidoException e) {
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "/problemas/documento-invalido",
                "El XML generado no cumple UBL 2.1; el comprobante no se emitió", List.of());
    }

    private static ResponseEntity<Problema> respuesta(HttpStatus estado, String tipo, String detalle,
                                                      List<ErrorRegla> errores) {
        return ResponseEntity.status(estado).contentType(PROBLEMA)
                .body(new Problema(tipo, estado.getReasonPhrase(), estado.value(), detalle, errores));
    }
}
