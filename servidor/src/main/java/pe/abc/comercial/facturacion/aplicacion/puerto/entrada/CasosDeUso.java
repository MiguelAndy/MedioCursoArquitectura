package pe.abc.comercial.facturacion.aplicacion.puerto.entrada;

import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.ArchivoXml;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.CalculoVista;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.ComprobanteVista;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.ResultadoEmision;

import java.util.UUID;

/**
 * Puertos de entrada del módulo facturacion. Cada uno corresponde al {@code x-puerto-entrada}
 * de una operación de {@code specs/openapi/abc-comercial-api.yaml}.
 */
public final class CasosDeUso {

    private CasosDeUso() {
    }

    /** {@code POST /comprobantes} (emitirComprobante). */
    public interface EmitirComprobanteUseCase {
        ResultadoEmision emitir(SolicitudEmision solicitud, String claveIdempotencia);
    }

    /** {@code POST /comprobantes/calculos} (calcularComprobante): mismo motor, sin numerar ni emitir. */
    public interface CalcularComprobanteUseCase {
        CalculoVista calcular(SolicitudEmision solicitud);
    }

    /** {@code GET /comprobantes/{comprobanteId}} (obtenerComprobante). */
    public interface ConsultarComprobantesUseCase {
        ComprobanteVista obtener(UUID comprobanteId);
    }

    /** {@code GET /comprobantes/{comprobanteId}/xml} (descargarXml). */
    public interface DescargarDocumentoElectronicoUseCase {
        ArchivoXml descargarXml(UUID comprobanteId);
    }

    /** 422: la solicitud incumple reglas de negocio VAL-* (capa 2); se informan todas. */
    public static class SolicitudRechazadaException extends RuntimeException {
        public record ReglaIncumplida(String regla, String campo, String mensaje) {
        }

        private final transient java.util.List<ReglaIncumplida> reglas;

        public SolicitudRechazadaException(java.util.List<ReglaIncumplida> reglas, Throwable causa) {
            super(causa.getMessage(), causa);
            this.reglas = java.util.List.copyOf(reglas);
        }

        public java.util.List<ReglaIncumplida> reglas() {
            return reglas;
        }
    }

    /** 404: el comprobante no existe. */
    public static class ComprobanteNoEncontradoException extends RuntimeException {
        public ComprobanteNoEncontradoException(UUID id) {
            super("No existe el comprobante " + id);
        }
    }

    /** 409: la misma Idempotency-Key llegó con otro cuerpo (docs/04 § 4.4). */
    public static class ConflictoIdempotenciaException extends RuntimeException {
        public ConflictoIdempotenciaException(String clave) {
            super("La Idempotency-Key " + clave + " ya se usó con una solicitud distinta");
        }
    }
}
