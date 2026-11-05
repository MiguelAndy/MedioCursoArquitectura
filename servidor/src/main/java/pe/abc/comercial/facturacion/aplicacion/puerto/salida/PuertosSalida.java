package pe.abc.comercial.facturacion.aplicacion.puerto.salida;

import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.SolicitudEmision;
import pe.abc.comercial.facturacion.dominio.Comprobante;
import pe.abc.comercial.facturacion.dominio.ParametrosTributarios;
import pe.abc.comercial.facturacion.dominio.Partes.Emisor;
import pe.abc.comercial.facturacion.dominio.catalogo.TipoComprobante;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Puertos de salida del módulo facturacion (docs/06, Tabla 6.3). Los implementa la infraestructura. */
public final class PuertosSalida {

    private PuertosSalida() {
    }

    /** VAL-NUM-02: correlativo consecutivo y único por (RUC, tipo, serie). */
    public interface NumeradorPort {
        long siguiente(String rucEmisor, TipoComprobante tipo, String serie);
    }

    public interface ComprobanteRepository {
        void guardar(Comprobante comprobante, String claveIdempotencia, SolicitudEmision solicitud);

        Optional<Comprobante> porId(UUID id);

        Optional<RegistroIdempotencia> porClaveIdempotencia(String clave);

        /** Solicitud original del comprobante (para devolver los datos tal como se recibieron). */
        SolicitudEmision solicitudDe(UUID comprobanteId);
    }

    /** Lo que se recuerda de cada Idempotency-Key: el comprobante creado y el cuerpo recibido. */
    public record RegistroIdempotencia(UUID comprobanteId, SolicitudEmision solicitud) {
    }

    /** Genera y firma el XML UBL (en fase 1: llamada en proceso a {@code cpe.api.DocumentoElectronicoApi}). */
    public interface DocumentoElectronicoPort {
        Comprobante.DocumentoElectronico generar(Comprobante comprobante);
    }

    /** Parámetros vigentes a una fecha (módulo tributacion, ADR-08). */
    public interface ParametrosTributariosPort {
        ParametrosTributarios vigentesAl(LocalDate fecha);
    }

    /** Datos de la empresa emisora (módulo configuracion). */
    public interface EmisorPort {
        Emisor emisor();
    }
}
