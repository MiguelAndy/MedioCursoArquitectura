package pe.abc.comercial.facturacion.dominio;

import pe.abc.comercial.facturacion.dominio.DatosComprobante.Item;
import pe.abc.comercial.facturacion.dominio.catalogo.Tributo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Salida pura del {@link MotorTributario}: ítems calculados, totales y leyendas (docs/04 § 4.3).
 */
public record ResultadoCalculo(List<ItemCalculado> items, Totales totales, List<Leyenda> leyendas) {

    public ResultadoCalculo {
        items = List.copyOf(items);
        leyendas = List.copyOf(leyendas);
    }

    /**
     * Ítem con sus importes.
     *
     * @param valorUnitario   sin impuestos ({@code cac:Price}); 0.00 en las gratuitas
     * @param precioUnitario  con impuestos ({@code AlternativeConditionPrice}, tipo 01), o el valor
     *                        referencial en las gratuitas (tipo 02)
     * @param baseImponibleIgv valor de venta + ISC (VAL-TRB-05)
     */
    public record ItemCalculado(int numeroOrden,
                                Item item,
                                BigDecimal valorVenta,
                                BigDecimal valorUnitario,
                                BigDecimal precioUnitario,
                                String tipoPrecio,
                                Tributo tributoIgv,
                                BigDecimal baseImponibleIgv,
                                BigDecimal porcentajeIgv,
                                BigDecimal montoIgv,
                                BigDecimal baseImponibleIsc,
                                BigDecimal montoIsc,
                                BigDecimal tarifaIcbper,
                                BigDecimal montoIcbper,
                                BigDecimal totalTributos,
                                BigDecimal importeTotal) {

        public boolean tieneIsc() {
            return item.isc() != null;
        }

        public boolean tieneIcbper() {
            return item.bolsasIcbper() != null;
        }
    }

    /** Totales del comprobante (esquema {@code Totales}). */
    public record Totales(BigDecimal operacionesGravadas,
                          BigDecimal operacionesExoneradas,
                          BigDecimal operacionesInafectas,
                          BigDecimal operacionesExportacion,
                          BigDecimal operacionesGratuitas,
                          BigDecimal tributosOperacionesGratuitas,
                          BigDecimal igv,
                          BigDecimal isc,
                          BigDecimal baseIsc,
                          BigDecimal icbper,
                          BigDecimal otrosTributos,
                          BigDecimal totalTributos,
                          BigDecimal totalValorVenta,
                          BigDecimal totalPrecioVenta,
                          BigDecimal importeTotal) {
    }

    /** Leyenda del Catálogo N.° 52 ({@code cbc:Note}). */
    public record Leyenda(String codigo, String texto) {

        public static final String MONTO_EN_LETRAS = "1000";
        public static final String TRANSFERENCIA_GRATUITA = "1002";
    }
}
