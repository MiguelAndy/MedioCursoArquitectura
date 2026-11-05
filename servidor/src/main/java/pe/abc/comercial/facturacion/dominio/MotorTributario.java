package pe.abc.comercial.facturacion.dominio;

import pe.abc.comercial.facturacion.dominio.DatosComprobante.Isc;
import pe.abc.comercial.facturacion.dominio.DatosComprobante.Item;
import pe.abc.comercial.facturacion.dominio.ResultadoCalculo.ItemCalculado;
import pe.abc.comercial.facturacion.dominio.ResultadoCalculo.Leyenda;
import pe.abc.comercial.facturacion.dominio.ResultadoCalculo.Totales;
import pe.abc.comercial.facturacion.dominio.catalogo.TipoAfectacionIgv;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Motor tributario único (ADR-07): función pura {@code calcular(comprobante, parametros) → (ítems, totales, leyendas)}
 * que implementa el algoritmo de docs/04 § 4.3.
 *
 * <p>Redondeo HALF_UP: 2 decimales en importes y 10 en valores unitarios; se redondea por ítem y los totales
 * son sumas de ítems (VAL-TRB-08). Se usa {@link BigDecimal} en todo el cálculo.</p>
 */
public final class MotorTributario {

    static final String LEYENDA_GRATUITA = "TRANSFERENCIA GRATUITA DE UN BIEN Y/O SERVICIO PRESTADO GRATUITAMENTE";

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal CERO = r2(BigDecimal.ZERO);

    public ResultadoCalculo calcular(DatosComprobante datos, ParametrosTributarios parametros) {
        List<ItemCalculado> items = new ArrayList<>();
        for (int i = 0; i < datos.items().size(); i++) {
            items.add(calcularItem(i + 1, datos.items().get(i), parametros));
        }
        Totales totales = totalizar(items);
        return new ResultadoCalculo(items, totales, leyendas(totales, datos));
    }

    // ---------------------------------------------------------------------------------------
    // Ítems
    // ---------------------------------------------------------------------------------------

    private ItemCalculado calcularItem(int numero, Item item, ParametrosTributarios p) {
        TipoAfectacionIgv afectacion = item.afectacion();
        BigDecimal tasaIgv = afectacion.gravada() ? p.tasaIgv() : BigDecimal.ZERO;
        BigDecimal porcentajeIgv = r2(tasaIgv.multiply(CIEN));
        BigDecimal cantidad = item.cantidad();

        // VAL-TRB-06: ICBPER = bolsas × tarifa; no forma parte de la base del IGV.
        BigDecimal tarifaIcbper = item.bolsasIcbper() == null ? null : r2(p.tarifaIcbper());
        BigDecimal icbper = tarifaIcbper == null ? CERO : r2(tarifaIcbper.multiply(BigDecimal.valueOf(item.bolsasIcbper())));

        BigDecimal valorVenta;
        BigDecimal isc = CERO;
        BigDecimal igv;
        BigDecimal valorUnitario;
        BigDecimal precioUnitario;
        String tipoPrecio = "01";

        if (afectacion.gratuita()) {
            // VAL-ITM-04 y VAL-TRB-09: valor referencial; el IGV es referencial y no suma al total.
            valorVenta = r2(cantidad.multiply(item.valorReferencialUnitario()));
            igv = r2(valorVenta.multiply(tasaIgv));
            valorUnitario = CERO;
            precioUnitario = item.valorReferencialUnitario();
            tipoPrecio = "02";
        } else if (item.precioUnitario() != null) {
            // VAL-TRB-02: importe = r2(cantidad × precio); valor = r2(importe ÷ (1 + tasa)); IGV = importe − valor.
            BigDecimal factor = BigDecimal.ONE.add(tasaIgv);
            BigDecimal importe = r2(cantidad.multiply(item.precioUnitario()));
            valorVenta = importe.divide(factor, 2, RoundingMode.HALF_UP);
            igv = importe.subtract(valorVenta);
            valorUnitario = tasaIgv.signum() == 0 ? item.precioUnitario()
                    : item.precioUnitario().divide(factor, 10, RoundingMode.HALF_UP);
            precioUnitario = item.precioUnitario();
        } else {
            valorVenta = r2(cantidad.multiply(item.valorUnitario()));
            isc = calcularIsc(item, valorVenta, p);
            // VAL-TRB-01 y VAL-TRB-05: la base del IGV incluye el ISC.
            igv = r2(valorVenta.add(isc).multiply(tasaIgv));
            valorUnitario = item.valorUnitario();
            precioUnitario = valorVenta.add(isc).add(igv).divide(cantidad, 10, RoundingMode.HALF_UP);
        }

        BigDecimal totalTributos = igv.add(isc).add(icbper);
        BigDecimal importeTotal = afectacion.gratuita() ? CERO : valorVenta.add(totalTributos);
        return new ItemCalculado(numero, item, valorVenta, valorUnitario, precioUnitario, tipoPrecio,
                afectacion.tributo(), valorVenta.add(isc), porcentajeIgv, igv,
                item.isc() == null ? null : valorVenta, item.isc() == null ? null : isc,
                tarifaIcbper, item.bolsasIcbper() == null ? null : icbper, totalTributos, importeTotal);
    }

    /** VAL-TRB-03: ISC según el sistema del Catálogo N.° 08. */
    static BigDecimal calcularIsc(Item item, BigDecimal valorVenta, ParametrosTributarios p) {
        Isc isc = item.isc();
        if (isc == null) {
            return CERO;
        }
        return switch (isc.sistema()) {
            case AL_VALOR -> r2(valorVenta.multiply(porcentaje(isc.tasa())));
            case ESPECIFICO -> r2((isc.cantidadEnUnidadBase() != null ? isc.cantidadEnUnidadBase() : item.cantidad())
                    .multiply(isc.montoFijoUnitario()));
            case PRECIO_VENTA_PUBLICO -> r2(item.cantidad().multiply(isc.precioVentaPublicoSugerido())
                    .multiply(p.factorIscPvp()).multiply(porcentaje(isc.tasa())));
        };
    }

    // ---------------------------------------------------------------------------------------
    // Totales y leyendas
    // ---------------------------------------------------------------------------------------

    private static Totales totalizar(List<ItemCalculado> items) {
        Predicate<ItemCalculado> onerosa = i -> !i.item().afectacion().gratuita();
        BigDecimal gravadas = suma(items, i -> onerosa.test(i) && i.item().afectacion().gravada(), ItemCalculado::valorVenta);
        BigDecimal exoneradas = suma(items, afectacion(TipoAfectacionIgv.EXONERADO_ONEROSA), ItemCalculado::valorVenta);
        BigDecimal inafectas = suma(items, afectacion(TipoAfectacionIgv.INAFECTO_ONEROSA), ItemCalculado::valorVenta);
        BigDecimal exportacion = suma(items, afectacion(TipoAfectacionIgv.EXPORTACION), ItemCalculado::valorVenta);
        BigDecimal gratuitas = suma(items, onerosa.negate(), ItemCalculado::valorVenta);
        BigDecimal tributosGratuitas = suma(items, onerosa.negate(), ItemCalculado::montoIgv);

        BigDecimal igv = suma(items, onerosa, ItemCalculado::montoIgv);
        BigDecimal isc = suma(items, ItemCalculado::tieneIsc, ItemCalculado::montoIsc);
        BigDecimal baseIsc = suma(items, ItemCalculado::tieneIsc, ItemCalculado::baseImponibleIsc);
        BigDecimal icbper = suma(items, ItemCalculado::tieneIcbper, ItemCalculado::montoIcbper);
        BigDecimal otros = CERO;

        BigDecimal totalTributos = igv.add(isc).add(icbper).add(otros);                    // VAL-TOT-02
        BigDecimal totalValorVenta = gravadas.add(exoneradas).add(inafectas).add(exportacion); // VAL-TOT-01
        BigDecimal totalPrecioVenta = totalValorVenta.add(totalTributos);                  // VAL-TOT-03
        return new Totales(gravadas, exoneradas, inafectas, exportacion, gratuitas, tributosGratuitas,
                igv, isc, baseIsc, icbper, otros, totalTributos, totalValorVenta, totalPrecioVenta,
                totalPrecioVenta);                                                          // VAL-TOT-04
    }

    private static List<Leyenda> leyendas(Totales totales, DatosComprobante datos) {
        List<Leyenda> leyendas = new ArrayList<>();
        // VAL-TOT-06: la leyenda 1000 siempre está presente y coincide con el importe total.
        leyendas.add(new Leyenda(Leyenda.MONTO_EN_LETRAS,
                "SON: " + MontoEnLetras.convertir(totales.importeTotal(), datos.moneda())));
        if (totales.operacionesGratuitas().signum() > 0) {
            leyendas.add(new Leyenda(Leyenda.TRANSFERENCIA_GRATUITA, LEYENDA_GRATUITA));
        }
        return leyendas;
    }

    private static Predicate<ItemCalculado> afectacion(TipoAfectacionIgv afectacion) {
        return i -> i.item().afectacion() == afectacion;
    }

    private static BigDecimal suma(List<ItemCalculado> items, Predicate<ItemCalculado> filtro,
                                   Function<ItemCalculado, BigDecimal> importe) {
        return items.stream().filter(filtro).map(importe).reduce(CERO, BigDecimal::add);
    }

    private static BigDecimal porcentaje(BigDecimal tasa) {
        return tasa.divide(CIEN);
    }

    static BigDecimal r2(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
