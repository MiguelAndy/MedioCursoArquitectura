package pe.abc.comercial.facturacion.dominio;

import pe.abc.comercial.facturacion.dominio.DatosComprobante.Cuota;
import pe.abc.comercial.facturacion.dominio.DatosComprobante.FormaPago;
import pe.abc.comercial.facturacion.dominio.DatosComprobante.Isc;
import pe.abc.comercial.facturacion.dominio.DatosComprobante.Item;
import pe.abc.comercial.facturacion.dominio.Partes.Adquirente;
import pe.abc.comercial.facturacion.dominio.Partes.DocumentoIdentidad;
import pe.abc.comercial.facturacion.dominio.catalogo.Moneda;
import pe.abc.comercial.facturacion.dominio.catalogo.SistemaIsc;
import pe.abc.comercial.facturacion.dominio.catalogo.TipoAfectacionIgv;
import pe.abc.comercial.facturacion.dominio.catalogo.TipoComprobante;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Object Mother del dominio: datos válidos que cada prueba modifica en un solo aspecto. */
final class Fabrica {

    static final LocalDate HOY = LocalDate.of(2026, 10, 22);
    static final ParametrosTributarios PARAMETROS = new ParametrosTributarios(new BigDecimal("0.18"),
            new BigDecimal("0.50"), new BigDecimal("0.847"), new BigDecimal("700.00"));
    static final Adquirente EMPRESA = new Adquirente(new DocumentoIdentidad("6", "20456789120"),
            "DISTRIBUIDORA SANTA ROSA DEL HUALLAGA E.I.R.L.", "JR. HUALLAGA 456, TINGO MARIA", "100601");
    static final Adquirente CLIENTES_VARIOS = new Adquirente(new DocumentoIdentidad("0", "00000000"),
            "CLIENTES VARIOS", null, null);
    static final FormaPago CONTADO = new FormaPago(FormaPago.Tipo.CONTADO, null, List.of());

    private Fabrica() {
    }

    static BigDecimal d(String valor) {
        return valor == null ? null : new BigDecimal(valor);
    }

    /** Ítem con valor unitario (sin impuestos). */
    static Item valor(String cantidad, String valorUnitario, TipoAfectacionIgv afectacion) {
        return new Item("P1", "PRODUCTO", "NIU", d(cantidad), d(valorUnitario), null, null, afectacion, null, null);
    }

    /** Ítem con precio unitario (con impuestos). */
    static Item precio(String cantidad, String precioUnitario, TipoAfectacionIgv afectacion) {
        return new Item("P1", "PRODUCTO", "NIU", d(cantidad), null, d(precioUnitario), null, afectacion, null, null);
    }

    static Item gratuito(String cantidad, String valorReferencial, TipoAfectacionIgv afectacion) {
        return new Item("P1", "BONIFICACION", "NIU", d(cantidad), null, null, d(valorReferencial), afectacion, null, null);
    }

    static Item conIsc(String cantidad, String valorUnitario, SistemaIsc sistema, String tasa, String montoFijo,
                       String pvp) {
        return new Item("P1", "BEBIDA", "NIU", d(cantidad), d(valorUnitario), null, null,
                TipoAfectacionIgv.GRAVADO_ONEROSA, new Isc(sistema, d(tasa), d(montoFijo), null, d(pvp)), null);
    }

    static Item bolsas(int cantidad, String precioUnitario) {
        return new Item("BOL", "BOLSA PLASTICA", "NIU", d(String.valueOf(cantidad)), null, d(precioUnitario), null,
                TipoAfectacionIgv.GRAVADO_ONEROSA, null, cantidad);
    }

    static DatosComprobante factura(Item... items) {
        return new DatosComprobante(TipoComprobante.FACTURA, "F001", HOY, LocalTime.of(10, 30), null, "0101",
                Moneda.PEN, null, EMPRESA, List.of(items), CONTADO, List.of(), null);
    }

    static DatosComprobante boleta(Adquirente adquirente, Item... items) {
        return new DatosComprobante(TipoComprobante.BOLETA, "B001", HOY, LocalTime.of(17, 45), null, "0101",
                Moneda.PEN, null, adquirente, List.of(items), null, List.of(), null);
    }

    static DatosComprobante credito(DatosComprobante base, String pendiente, Cuota... cuotas) {
        return new DatosComprobante(base.tipo(), base.serie(), base.fechaEmision(), base.horaEmision(),
                base.fechaVencimiento(), base.tipoOperacion(), base.moneda(), base.tipoCambio(), base.adquirente(),
                base.items(), new FormaPago(FormaPago.Tipo.CREDITO, d(pendiente), List.of(cuotas)),
                base.guiasRemision(), base.ordenCompra());
    }

    static Cuota cuota(int numero, String monto, LocalDate vence) {
        return new Cuota(numero, d(monto), vence);
    }
}
