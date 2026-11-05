package pe.abc.comercial.facturacion.dominio;

import pe.abc.comercial.facturacion.dominio.Partes.Adquirente;
import pe.abc.comercial.facturacion.dominio.catalogo.Moneda;
import pe.abc.comercial.facturacion.dominio.catalogo.SistemaIsc;
import pe.abc.comercial.facturacion.dominio.catalogo.TipoAfectacionIgv;
import pe.abc.comercial.facturacion.dominio.catalogo.TipoComprobante;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Datos de un comprobante antes de numerarlo: lo que envía el cliente, ya tipado.
 * El cliente nunca envía correlativo, impuestos ni totales (INV-03, VAL-NUM-02).
 */
public record DatosComprobante(TipoComprobante tipo,
                               String serie,
                               LocalDate fechaEmision,
                               LocalTime horaEmision,
                               LocalDate fechaVencimiento,
                               String tipoOperacion,
                               Moneda moneda,
                               BigDecimal tipoCambio,
                               Adquirente adquirente,
                               List<Item> items,
                               FormaPago formaPago,
                               List<GuiaRemision> guiasRemision,
                               String ordenCompra) {

    public DatosComprobante {
        items = List.copyOf(items);
        guiasRemision = guiasRemision == null ? List.of() : List.copyOf(guiasRemision);
    }

    /**
     * Ítem del comprobante. Exactamente uno de {@code valorUnitario} (sin impuestos), {@code precioUnitario}
     * (con impuestos) o {@code valorReferencialUnitario} (operaciones no onerosas) viene informado (VAL-ITM-04).
     */
    public record Item(String codigoProducto,
                       String descripcion,
                       String unidadMedida,
                       BigDecimal cantidad,
                       BigDecimal valorUnitario,
                       BigDecimal precioUnitario,
                       BigDecimal valorReferencialUnitario,
                       TipoAfectacionIgv afectacion,
                       Isc isc,
                       Integer bolsasIcbper) {
    }

    /** ISC del ítem según el Catálogo N.° 08 (VAL-TRB-03). {@code tasa} en porcentaje (p. ej. 25.00). */
    public record Isc(SistemaIsc sistema, BigDecimal tasa, BigDecimal montoFijoUnitario,
                      BigDecimal cantidadEnUnidadBase, BigDecimal precioVentaPublicoSugerido) {
    }

    /** Forma de pago (RS 193-2020). {@code montoPendiente} nulo = importe total. */
    public record FormaPago(Tipo tipo, BigDecimal montoPendiente, List<Cuota> cuotas) {

        public enum Tipo { CONTADO, CREDITO }

        public FormaPago {
            cuotas = cuotas == null ? List.of() : List.copyOf(cuotas);
        }
    }

    public record Cuota(int numero, BigDecimal monto, LocalDate fechaVencimiento) {
    }

    /** Guía de remisión relacionada ({@code cac:DespatchDocumentReference}). */
    public record GuiaRemision(String tipo, String numero) {
    }
}
