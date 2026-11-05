package pe.abc.comercial.facturacion.aplicacion.servicio;

import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.SolicitudEmision;
import pe.abc.comercial.facturacion.dominio.DatosComprobante;
import pe.abc.comercial.facturacion.dominio.DatosComprobante.Cuota;
import pe.abc.comercial.facturacion.dominio.DatosComprobante.FormaPago;
import pe.abc.comercial.facturacion.dominio.DatosComprobante.GuiaRemision;
import pe.abc.comercial.facturacion.dominio.DatosComprobante.Isc;
import pe.abc.comercial.facturacion.dominio.DatosComprobante.Item;
import pe.abc.comercial.facturacion.dominio.Partes.Adquirente;
import pe.abc.comercial.facturacion.dominio.Partes.DocumentoIdentidad;
import pe.abc.comercial.facturacion.dominio.ReglaNegocioException;
import pe.abc.comercial.facturacion.dominio.ReglaNegocioException.Violacion;
import pe.abc.comercial.facturacion.dominio.catalogo.Moneda;
import pe.abc.comercial.facturacion.dominio.catalogo.SistemaIsc;
import pe.abc.comercial.facturacion.dominio.catalogo.TipoAfectacionIgv;
import pe.abc.comercial.facturacion.dominio.catalogo.TipoComprobante;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Traduce el comando (texto del contrato) al modelo de dominio (tipos y {@link BigDecimal}).
 * También aplica la regla de alcance de la fase 2 (VAL-ALC-01): lo que el contrato permite pero esta fase
 * todavía no implementa se <b>rechaza explícitamente</b>, nunca se ignora en silencio.
 */
final class MapeadorSolicitud {

    static final String REGLA_ALCANCE = "VAL-ALC-01";

    private MapeadorSolicitud() {
    }

    static DatosComprobante aDatos(SolicitudEmision s, LocalDate hoy, LocalTime ahora) {
        verificarAlcance(s);
        TipoComprobante tipo = TipoComprobante.desdeCodigo(s.tipoComprobante()).orElseThrow();
        Moneda moneda = Moneda.desdeCodigo(s.moneda()).orElseThrow();
        var c = s.cliente();
        Adquirente adquirente = new Adquirente(new DocumentoIdentidad(c.tipoDocumento(), c.numeroDocumento()),
                c.razonSocial(), c.direccion(), c.ubigeo());

        List<Item> items = s.items().stream().map(MapeadorSolicitud::item).toList();
        List<GuiaRemision> guias = s.guiasRemision() == null ? List.of()
                : s.guiasRemision().stream().map(g -> new GuiaRemision(g.tipo(), g.numero())).toList();

        return new DatosComprobante(tipo, s.serie(),
                s.fechaEmision() != null ? s.fechaEmision() : hoy,
                s.horaEmision() != null ? s.horaEmision() : ahora,
                s.fechaVencimiento(),
                s.tipoOperacion() != null ? s.tipoOperacion() : "0101",
                moneda, decimal(s.tipoCambio()), adquirente, items, formaPago(s.formaPago()), guias, s.ordenCompra());
    }

    private static Item item(SolicitudEmision.Item i) {
        TipoAfectacionIgv afectacion = TipoAfectacionIgv.desdeCodigo(i.tipoAfectacionIgv()).orElseThrow();
        Isc isc = i.isc() == null ? null : new Isc(SistemaIsc.desdeCodigo(i.isc().sistema()).orElseThrow(),
                decimal(i.isc().tasa()), decimal(i.isc().montoFijoUnitario()),
                decimal(i.isc().cantidadEnUnidadBase()), decimal(i.isc().precioVentaPublicoSugerido()));
        return new Item(i.codigoProducto(), i.descripcion(), i.unidadMedida(), decimal(i.cantidad()),
                decimal(i.valorUnitario()), decimal(i.precioUnitario()), decimal(i.valorReferencialUnitario()),
                afectacion, isc, i.icbper() == null ? null : i.icbper().cantidadBolsas());
    }

    private static FormaPago formaPago(SolicitudEmision.FormaPago f) {
        if (f == null) {
            return null;
        }
        List<Cuota> cuotas = new ArrayList<>();
        if (f.cuotas() != null) {
            for (int i = 0; i < f.cuotas().size(); i++) {
                var cuota = f.cuotas().get(i);
                cuotas.add(new Cuota(cuota.numero() != null ? cuota.numero() : i + 1, decimal(cuota.monto()),
                        cuota.fechaVencimiento()));
            }
        }
        return new FormaPago(FormaPago.Tipo.valueOf(f.tipo()), decimal(f.montoPendiente()), cuotas);
    }

    /** VAL-ALC-01: funcionalidades del contrato que se implementan en fases posteriores. */
    private static void verificarAlcance(SolicitudEmision s) {
        List<Violacion> v = new ArrayList<>();
        if (TipoComprobante.desdeCodigo(s.tipoComprobante()).isEmpty()) {
            v.add(fueraDeAlcance("/tipoComprobante", "las notas de crédito y débito (07, 08)"));
        }
        if (s.tipoOperacion() != null && !"0101".equals(s.tipoOperacion())) {
            v.add(fueraDeAlcance("/tipoOperacion", "el tipo de operación " + s.tipoOperacion() + " (solo 0101, venta interna)"));
        }
        if (s.documentoReferencia() != null) {
            v.add(fueraDeAlcance("/documentoReferencia", "los documentos de referencia de notas"));
        }
        if (s.detraccion() != null) {
            v.add(fueraDeAlcance("/detraccion", "la detracción (operación 1001)"));
        }
        if (s.cargosDescuentosGlobales() != null && !s.cargosDescuentosGlobales().isEmpty()) {
            v.add(fueraDeAlcance("/cargosDescuentosGlobales", "los cargos y descuentos globales"));
        }
        for (int i = 0; i < s.items().size(); i++) {
            var item = s.items().get(i);
            if (item.cargosDescuentos() != null && !item.cargosDescuentos().isEmpty()) {
                v.add(fueraDeAlcance("/items/" + i + "/cargosDescuentos", "los cargos y descuentos por ítem"));
            }
            if (TipoAfectacionIgv.desdeCodigo(item.tipoAfectacionIgv()).isEmpty()) {
                v.add(fueraDeAlcance("/items/" + i + "/tipoAfectacionIgv", "la afectación " + item.tipoAfectacionIgv() + " (IVAP)"));
            }
            if (item.isc() != null && item.precioUnitario() != null) {
                v.add(fueraDeAlcance("/items/" + i + "/isc", "el despeje del ISC desde un precio con impuestos"));
            }
        }
        if (Moneda.desdeCodigo(s.moneda()).isEmpty()) {
            v.add(fueraDeAlcance("/moneda", "la moneda " + s.moneda()));
        }
        if (!v.isEmpty()) {
            throw new ReglaNegocioException(v);
        }
    }

    private static Violacion fueraDeAlcance(String campo, String funcionalidad) {
        return new Violacion(REGLA_ALCANCE, campo, "La fase 2 todavía no implementa " + funcionalidad);
    }

    private static BigDecimal decimal(String valor) {
        return Optional.ofNullable(valor).map(BigDecimal::new).orElse(null);
    }
}
