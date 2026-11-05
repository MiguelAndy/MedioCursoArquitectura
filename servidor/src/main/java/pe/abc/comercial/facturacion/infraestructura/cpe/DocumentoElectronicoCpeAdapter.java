package pe.abc.comercial.facturacion.infraestructura.cpe;

import pe.abc.comercial.cpe.api.ComprobanteElectronico;
import pe.abc.comercial.cpe.api.ComprobanteElectronico.Domicilio;
import pe.abc.comercial.cpe.api.ComprobanteElectronico.Subtotal;
import pe.abc.comercial.cpe.api.ComprobanteElectronico.TributoUbl;
import pe.abc.comercial.cpe.api.DocumentoElectronicoApi;
import pe.abc.comercial.facturacion.aplicacion.puerto.salida.PuertosSalida.DocumentoElectronicoPort;
import pe.abc.comercial.facturacion.dominio.Comprobante;
import pe.abc.comercial.facturacion.dominio.DatosComprobante;
import pe.abc.comercial.facturacion.dominio.DatosComprobante.FormaPago;
import pe.abc.comercial.facturacion.dominio.ResultadoCalculo.ItemCalculado;
import pe.abc.comercial.facturacion.dominio.ResultadoCalculo.Totales;
import pe.abc.comercial.facturacion.dominio.catalogo.TipoAfectacionIgv;
import pe.abc.comercial.facturacion.dominio.catalogo.Tributo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Adaptador de {@code DocumentoElectronicoPort}: traduce el agregado al lenguaje de cpe y llama en proceso a
 * {@link DocumentoElectronicoApi} (docs/06, Tabla 6.3). En la fase 3 solo cambia el transporte (HTTP o gRPC).
 */
public final class DocumentoElectronicoCpeAdapter implements DocumentoElectronicoPort {

    private final DocumentoElectronicoApi cpe;

    public DocumentoElectronicoCpeAdapter(DocumentoElectronicoApi cpe) {
        this.cpe = Objects.requireNonNull(cpe);
    }

    @Override
    public Comprobante.DocumentoElectronico generar(Comprobante comprobante) {
        var generado = cpe.generar(traducir(comprobante));
        return new Comprobante.DocumentoElectronico(generado.nombreArchivo(), generado.hash(), generado.xmlFirmado());
    }

    static ComprobanteElectronico traducir(Comprobante c) {
        DatosComprobante d = c.datos();
        Totales t = c.calculo().totales();
        var emisor = c.emisor();
        var domicilio = emisor.domicilio();
        var adquirente = d.adquirente();

        return new ComprobanteElectronico(
                d.tipo().codigo(), c.numero(), d.fechaEmision(), d.horaEmision(), d.fechaVencimiento(),
                d.tipoOperacion(), d.moneda().codigo(),
                c.calculo().leyendas().stream().map(l -> new ComprobanteElectronico.Leyenda(l.codigo(), l.texto())).toList(),
                d.ordenCompra(),
                d.guiasRemision().stream().map(g -> new ComprobanteElectronico.Referencia(g.tipo(), g.numero())).toList(),
                new ComprobanteElectronico.Emisor(emisor.ruc(), emisor.razonSocial(), emisor.nombreComercial(),
                        new Domicilio(domicilio.ubigeo(), domicilio.codigoEstablecimiento(), domicilio.provincia(),
                                domicilio.departamento(), domicilio.distrito(), domicilio.direccion())),
                new ComprobanteElectronico.Parte(adquirente.documento().tipo(), adquirente.documento().numero(),
                        adquirente.razonSocial(), direccionDelAdquirente(d)),
                formaPago(d.formaPago(), t.importeTotal()),
                subtotalesCabecera(c.calculo().items(), t),
                t.totalTributos(), t.totalValorVenta(), t.totalPrecioVenta(), t.importeTotal(),
                c.calculo().items().stream().map(DocumentoElectronicoCpeAdapter::linea).toList());
    }

    /**
     * Aclaración A-02 de la fase 2 (TR-A18 es condicional): la dirección del adquirente se informa solo cuando el
     * comprobante sustenta un traslado de bienes (tiene guías de remisión). Minimiza datos personales (Ley 29733,
     * docs/06 § 6.11) y reproduce los XML de referencia de specs/ubl/fuentes-sin-firma.
     */
    static Domicilio direccionDelAdquirente(DatosComprobante d) {
        var adquirente = d.adquirente();
        if (d.guiasRemision().isEmpty() || adquirente.direccion() == null) {
            return null;
        }
        return new Domicilio(adquirente.ubigeo(), null, null, null, null, adquirente.direccion());
    }

    private static ComprobanteElectronico.FormaPago formaPago(FormaPago f, BigDecimal importeTotal) {
        if (f == null) {
            return null;
        }
        if (f.tipo() == FormaPago.Tipo.CONTADO) {
            return new ComprobanteElectronico.FormaPago("Contado", null, List.of());
        }
        return new ComprobanteElectronico.FormaPago("Credito",
                f.montoPendiente() != null ? f.montoPendiente() : importeTotal,
                f.cuotas().stream().map(q -> new ComprobanteElectronico.Cuota(q.monto(), q.fechaVencimiento())).toList());
    }

    /** Un subtotal por tributo presente, en el orden del Catálogo N.° 05 de {@link Tributo}. */
    private static List<Subtotal> subtotalesCabecera(List<ItemCalculado> items, Totales t) {
        List<Subtotal> subtotales = new ArrayList<>();
        agregarSi(subtotales, items, i -> !i.item().afectacion().gratuita() && i.item().afectacion().gravada(),
                Tributo.IGV, t.operacionesGravadas(), t.igv());
        agregarSi(subtotales, items, ItemCalculado::tieneIsc, Tributo.ISC, t.baseIsc(), t.isc());
        agregarSi(subtotales, items, i -> i.item().afectacion() == TipoAfectacionIgv.EXPORTACION,
                Tributo.EXPORTACION, t.operacionesExportacion(), t.otrosTributos());
        agregarSi(subtotales, items, i -> i.item().afectacion().gratuita(),
                Tributo.GRATUITO, t.operacionesGratuitas(), t.tributosOperacionesGratuitas());
        agregarSi(subtotales, items, i -> i.item().afectacion() == TipoAfectacionIgv.EXONERADO_ONEROSA,
                Tributo.EXONERADO, t.operacionesExoneradas(), t.otrosTributos());
        agregarSi(subtotales, items, i -> i.item().afectacion() == TipoAfectacionIgv.INAFECTO_ONEROSA,
                Tributo.INAFECTO, t.operacionesInafectas(), t.otrosTributos());
        agregarSi(subtotales, items, ItemCalculado::tieneIcbper, Tributo.ICBPER, null, t.icbper());
        return subtotales;
    }

    private static void agregarSi(List<Subtotal> subtotales, List<ItemCalculado> items, Predicate<ItemCalculado> hay,
                                  Tributo tributo, BigDecimal base, BigDecimal monto) {
        if (items.stream().anyMatch(hay)) {
            subtotales.add(Subtotal.cabecera(tributoUbl(tributo), base, monto));
        }
    }

    private static ComprobanteElectronico.Linea linea(ItemCalculado i) {
        var item = i.item();
        List<Subtotal> impuestos = new ArrayList<>();
        impuestos.add(new Subtotal(tributoUbl(item.afectacion().tributo()), i.baseImponibleIgv(), i.montoIgv(),
                i.porcentajeIgv(), item.afectacion().codigo(), null, null, null));
        if (i.tieneIsc()) {
            impuestos.add(new Subtotal(tributoUbl(Tributo.ISC), i.baseImponibleIsc(), i.montoIsc(),
                    item.isc().tasa() == null ? null : item.isc().tasa().setScale(2, RoundingMode.HALF_UP),
                    null, item.isc().sistema().codigo(), null, null));
        }
        if (i.tieneIcbper()) {
            impuestos.add(new Subtotal(tributoUbl(Tributo.ICBPER), null, i.montoIcbper(), null, null, null,
                    BigDecimal.valueOf(item.bolsasIcbper()), i.tarifaIcbper()));
        }
        return new ComprobanteElectronico.Linea(i.numeroOrden(), item.unidadMedida(), item.cantidad(), i.valorVenta(),
                i.precioUnitario(), i.tipoPrecio(), i.totalTributos(), impuestos, item.descripcion(),
                item.codigoProducto(), i.valorUnitario());
    }

    private static TributoUbl tributoUbl(Tributo t) {
        return new TributoUbl(t.codigo(), t.nombre(), t.codigoInternacional());
    }
}
