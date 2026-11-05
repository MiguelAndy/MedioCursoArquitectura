package pe.abc.comercial.facturacion.infraestructura.persistencia;

import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.SolicitudEmision;
import pe.abc.comercial.facturacion.aplicacion.puerto.salida.PuertosSalida.ComprobanteRepository;
import pe.abc.comercial.facturacion.aplicacion.puerto.salida.PuertosSalida.NumeradorPort;
import pe.abc.comercial.facturacion.aplicacion.puerto.salida.PuertosSalida.RegistroIdempotencia;
import pe.abc.comercial.facturacion.dominio.Comprobante;
import pe.abc.comercial.facturacion.dominio.catalogo.TipoComprobante;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptadores de persistencia de la fase 2: en memoria. La fase 1 especifica PostgreSQL con
 * {@code UPDATE serie ... RETURNING} (docs/04 § 4.4); como los casos de uso dependen solo de los puertos,
 * reemplazarlos por JPA no cambia ni el dominio ni la aplicación.
 */
public final class AdaptadoresEnMemoria {

    private AdaptadoresEnMemoria() {
    }

    /** VAL-NUM-02 / INV-05: el correlativo de cada (RUC, tipo, serie) solo crece. */
    public static final class NumeradorEnMemoria implements NumeradorPort {
        private final Map<String, Long> ultimos = new ConcurrentHashMap<>();

        @Override
        public long siguiente(String rucEmisor, TipoComprobante tipo, String serie) {
            return ultimos.merge(rucEmisor + "|" + tipo.codigo() + "|" + serie, 1L, Long::sum);
        }
    }

    public static final class ComprobanteRepositoryEnMemoria implements ComprobanteRepository {
        private final Map<UUID, Comprobante> comprobantes = new ConcurrentHashMap<>();
        private final Map<UUID, SolicitudEmision> solicitudes = new ConcurrentHashMap<>();
        private final Map<String, RegistroIdempotencia> idempotencia = new ConcurrentHashMap<>();

        @Override
        public void guardar(Comprobante comprobante, String claveIdempotencia, SolicitudEmision solicitud) {
            comprobantes.put(comprobante.id(), comprobante);
            solicitudes.put(comprobante.id(), solicitud);
            idempotencia.put(claveIdempotencia, new RegistroIdempotencia(comprobante.id(), solicitud));
        }

        @Override
        public Optional<Comprobante> porId(UUID id) {
            return Optional.ofNullable(comprobantes.get(id));
        }

        @Override
        public Optional<RegistroIdempotencia> porClaveIdempotencia(String clave) {
            return Optional.ofNullable(idempotencia.get(clave));
        }

        @Override
        public SolicitudEmision solicitudDe(UUID comprobanteId) {
            return solicitudes.get(comprobanteId);
        }
    }
}
