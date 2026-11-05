package pe.abc.comercial.facturacion.aplicacion.servicio;

import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.SolicitudEmision;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.CalculoVista;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.ComprobanteVista;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.DocumentoElectronicoVista;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.EmisorVista;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.Enlaces;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.ItemVista;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.LeyendaVista;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.TotalesVista;
import pe.abc.comercial.facturacion.dominio.Comprobante;
import pe.abc.comercial.facturacion.dominio.DatosComprobante;
import pe.abc.comercial.facturacion.dominio.ResultadoCalculo;
import pe.abc.comercial.facturacion.dominio.ResultadoCalculo.ItemCalculado;
import pe.abc.comercial.facturacion.dominio.ResultadoCalculo.Totales;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Traduce el agregado a las vistas del contrato de respuesta (importes como texto decimal). */
final class MapeadorVista {

    static final String ALGORITMO_FIRMA = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    private MapeadorVista() {
    }

    static ComprobanteVista comprobante(Comprobante c, SolicitudEmision solicitud) {
        DatosComprobante d = c.datos();
        var emisor = c.emisor();
        var domicilio = emisor.domicilio();
        var documento = c.documentoElectronico();
        String self = "/api/v1/comprobantes/" + c.id();
        return new ComprobanteVista(
                c.id().toString(), d.tipo().codigo(), d.serie(), c.correlativo(), c.numero(),
                d.fechaEmision().toString(), d.horaEmision().toString(),
                d.fechaVencimiento() == null ? null : d.fechaVencimiento().toString(),
                d.tipoOperacion(), d.moneda().codigo(), solicitud.tipoCambio(),
                new EmisorVista(emisor.ruc(), emisor.razonSocial(), emisor.nombreComercial(),
                        domicilio.codigoEstablecimiento(), domicilio.direccion() + ", " + domicilio.distrito(),
                        domicilio.ubigeo()),
                solicitud.cliente(),
                items(c.calculo()), totales(c.calculo().totales()), leyendas(c.calculo()),
                solicitud.formaPago(), solicitud.guiasRemision(), d.ordenCompra(), c.estado().name(),
                documento == null ? null : new DocumentoElectronicoVista(documento.nombreArchivo(), documento.hash(),
                        ALGORITMO_FIRMA, codigoQr(c)),
                new Enlaces(self, self + "/xml"));
    }

    static CalculoVista calculo(ResultadoCalculo r) {
        return new CalculoVista(items(r), totales(r.totales()), leyendas(r));
    }

    /** Contenido del QR de la representación impresa: RUC|TIPO|SERIE|NÚMERO|IGV|TOTAL|FECHA|TIPO DOC|DOC|HASH|. */
    static String codigoQr(Comprobante c) {
        var d = c.datos();
        var t = c.calculo().totales();
        return String.join("|", c.emisor().ruc(), d.tipo().codigo(), d.serie(), String.valueOf(c.correlativo()),
                importe(t.igv()), importe(t.importeTotal()), d.fechaEmision().toString(),
                d.adquirente().documento().tipo(), d.adquirente().documento().numero(),
                c.documentoElectronico().hash()) + "|";
    }

    private static List<ItemVista> items(ResultadoCalculo r) {
        return r.items().stream().map(MapeadorVista::item).toList();
    }

    private static ItemVista item(ItemCalculado i) {
        var item = i.item();
        boolean gratuita = item.afectacion().gratuita();
        return new ItemVista(i.numeroOrden(), item.codigoProducto(), item.descripcion(), item.unidadMedida(),
                item.cantidad().toPlainString(), unitario(i.valorUnitario()),
                gratuita ? null : unitario(i.precioUnitario()),
                gratuita ? unitario(item.valorReferencialUnitario()) : null,
                item.afectacion().codigo(),
                item.isc() == null ? null : new SolicitudEmision.Isc(item.isc().sistema().codigo(),
                        texto(item.isc().tasa()), texto(item.isc().montoFijoUnitario()),
                        texto(item.isc().cantidadEnUnidadBase()), texto(item.isc().precioVentaPublicoSugerido())),
                item.bolsasIcbper() == null ? null : new SolicitudEmision.Icbper(item.bolsasIcbper()),
                i.tributoIgv().codigo(), importe(i.valorVenta()), importe(i.baseImponibleIgv()),
                importe(i.porcentajeIgv()), importe(i.montoIgv()), importe(i.baseImponibleIsc()), importe(i.montoIsc()),
                importe(i.tarifaIcbper()), importe(i.montoIcbper()), importe(i.totalTributos()),
                importe(i.importeTotal()));
    }

    private static TotalesVista totales(Totales t) {
        return new TotalesVista(importe(t.operacionesGravadas()), importe(t.operacionesExoneradas()),
                importe(t.operacionesInafectas()), importe(t.operacionesExportacion()),
                importe(t.operacionesGratuitas()), importe(t.tributosOperacionesGratuitas()), importe(t.igv()),
                importe(t.isc()), importe(t.icbper()), importe(t.otrosTributos()), importe(t.totalTributos()),
                importe(t.totalValorVenta()), importe(t.totalPrecioVenta()), importe(t.importeTotal()));
    }

    private static List<LeyendaVista> leyendas(ResultadoCalculo r) {
        return r.leyendas().stream().map(l -> new LeyendaVista(l.codigo(), l.texto())).toList();
    }

    /** n(12,2). */
    static String importe(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** n(12,10) sin ceros sobrantes, con al menos 2 decimales: 177.0000000000 → 177.00. */
    static String unitario(BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        BigDecimal sinCeros = valor.stripTrailingZeros();
        return (sinCeros.scale() < 2 ? valor.setScale(2, RoundingMode.UNNECESSARY) : sinCeros).toPlainString();
    }

    private static String texto(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
