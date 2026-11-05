package pe.abc.comercial.facturacion.dominio;

import pe.abc.comercial.facturacion.dominio.DatosComprobante.Cuota;
import pe.abc.comercial.facturacion.dominio.DatosComprobante.FormaPago;
import pe.abc.comercial.facturacion.dominio.DatosComprobante.Isc;
import pe.abc.comercial.facturacion.dominio.DatosComprobante.Item;
import pe.abc.comercial.facturacion.dominio.ReglaNegocioException.Violacion;
import pe.abc.comercial.facturacion.dominio.catalogo.Moneda;
import pe.abc.comercial.facturacion.dominio.catalogo.TipoAfectacionIgv;
import pe.abc.comercial.facturacion.dominio.catalogo.TipoComprobante;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa 2 de validación (docs/04 § 4.1): reglas de negocio y tributarias VAL-* que el JSON Schema no puede expresar.
 * Reúne todas las violaciones; si hay alguna, lanza {@link ReglaNegocioException} (HTTP 422, no se consume número).
 */
public final class ValidadorNormativo {

    static final String OPERACION_VENTA_INTERNA = "0101";
    private static final int MAXIMO_ITEMS = 500;

    /** Reglas que se verifican antes de calcular (los datos deben ser calculables). */
    public void validarAntesDelCalculo(DatosComprobante d, LocalDate hoy) {
        List<Violacion> v = new ArrayList<>();
        validarIdentificacion(d, v);
        validarFechas(d, hoy, v);
        if (d.moneda() != Moneda.PEN && d.tipoCambio() == null) {
            v.add(new Violacion("VAL-MON-02", "/tipoCambio", "Una moneda distinta de PEN exige tipo de cambio"));
        }
        validarItems(d, v);
        if (d.tipo() == TipoComprobante.FACTURA && d.formaPago() == null) {
            v.add(new Violacion("VAL-PAG-01", "/formaPago", "La factura debe informar su forma de pago (contado o crédito)"));
        }
        lanzarSiHay(v);
    }

    /** Reglas que dependen de los importes calculados. */
    public void validarDespuesDelCalculo(DatosComprobante d, ResultadoCalculo calculo, ParametrosTributarios p) {
        List<Violacion> v = new ArrayList<>();
        BigDecimal total = calculo.totales().importeTotal();
        BigDecimal totalEnSoles = d.tipoCambio() == null ? total : total.multiply(d.tipoCambio());

        if (d.tipo() == TipoComprobante.BOLETA && !d.adquirente().documento().identificado()
                && totalEnSoles.compareTo(p.umbralIdentificacionBoleta()) > 0) {
            v.add(new Violacion("VAL-BOL-01", "/cliente/tipoDocumento",
                    "Una boleta mayor a S/ " + p.umbralIdentificacionBoleta() + " exige identificar al adquirente"));
        }
        validarCredito(d.formaPago(), total, v);
        lanzarSiHay(v);
    }

    // ---------------------------------------------------------------------------------------

    private static void validarIdentificacion(DatosComprobante d, List<Violacion> v) {
        if (d.serie() == null || d.serie().length() != 4 || d.serie().charAt(0) != d.tipo().letraSerie()) {
            v.add(new Violacion("VAL-NUM-01", "/serie",
                    "La serie de una " + d.tipo().nombre() + " debe iniciar con " + d.tipo().letraSerie() + " y tener 4 caracteres"));
        }
        var documento = d.adquirente().documento();
        if (documento.esRuc() && !Ruc.esValido(documento.numero())) {
            v.add(new Violacion("VAL-ID-02", "/cliente/numeroDocumento",
                    "El dígito verificador del RUC " + documento.numero() + " es incorrecto"));
        }
        if (d.tipo() == TipoComprobante.FACTURA && OPERACION_VENTA_INTERNA.equals(d.tipoOperacion()) && !documento.esRuc()) {
            v.add(new Violacion("VAL-ID-03", "/cliente/tipoDocumento",
                    "Una factura de venta interna se emite a un adquirente con RUC (tipo 6)"));
        }
        if (Partes.DocumentoIdentidad.DNI.equals(documento.tipo()) && !documento.numero().matches("\\d{8}")) {
            v.add(new Violacion("VAL-ID-06", "/cliente/numeroDocumento", "El DNI debe tener 8 dígitos"));
        }
    }

    private static void validarFechas(DatosComprobante d, LocalDate hoy, List<Violacion> v) {
        if (d.fechaEmision().isAfter(hoy)) {
            v.add(new Violacion("VAL-FEC-01", "/fechaEmision", "La fecha de emisión no puede ser posterior a " + hoy));
        }
        if (d.fechaVencimiento() != null && d.fechaVencimiento().isBefore(d.fechaEmision())) {
            v.add(new Violacion("VAL-FEC-03", "/fechaVencimiento", "El vencimiento no puede ser anterior a la emisión"));
        }
        if (d.formaPago() != null) {
            LocalDate anterior = d.fechaEmision();
            for (int i = 0; i < d.formaPago().cuotas().size(); i++) {
                Cuota cuota = d.formaPago().cuotas().get(i);
                if (!cuota.fechaVencimiento().isAfter(anterior)) {
                    v.add(new Violacion("VAL-FEC-03", "/formaPago/cuotas/" + i + "/fechaVencimiento",
                            "Las cuotas deben vencer después de la emisión y en fechas crecientes"));
                }
                anterior = cuota.fechaVencimiento();
            }
        }
    }

    private static void validarItems(DatosComprobante d, List<Violacion> v) {
        if (d.items().isEmpty() || d.items().size() > MAXIMO_ITEMS) {
            v.add(new Violacion("VAL-ITM-01", "/items", "El comprobante debe tener entre 1 y 500 ítems"));
        }
        for (int i = 0; i < d.items().size(); i++) {
            Item item = d.items().get(i);
            String campo = "/items/" + i;
            if (item.cantidad().signum() <= 0) {
                v.add(new Violacion("VAL-ITM-02", campo + "/cantidad", "La cantidad debe ser mayor que 0"));
            }
            validarValorDelItem(item, campo, v);
            if (item.afectacion() == TipoAfectacionIgv.EXPORTACION && !d.tipoOperacion().startsWith("02")) {
                v.add(new Violacion("VAL-ITM-05", campo + "/tipoAfectacionIgv",
                        "La afectación 40 (exportación) solo se usa con un tipo de operación 02xx"));
            }
            if (item.isc() != null) {
                validarIsc(item, campo, v);
            }
        }
    }

    /** VAL-ITM-04: las no onerosas llevan solo valor referencial (> 0); las onerosas, valor o precio unitario. */
    private static void validarValorDelItem(Item item, String campo, List<Violacion> v) {
        int informados = (item.valorUnitario() != null ? 1 : 0) + (item.precioUnitario() != null ? 1 : 0);
        if (item.afectacion().gratuita()) {
            if (item.valorReferencialUnitario() == null || item.valorReferencialUnitario().signum() <= 0 || informados > 0) {
                v.add(new Violacion("VAL-ITM-04", campo,
                        "Una operación no onerosa lleva valor referencial mayor que 0 y no lleva valor ni precio unitario"));
            }
        } else if (informados != 1 || item.valorReferencialUnitario() != null) {
            v.add(new Violacion("VAL-ITM-04", campo,
                    "Un ítem oneroso lleva exactamente uno: valor unitario o precio unitario"));
        }
    }

    /** VAL-TRB-03 y VAL-TRB-04. */
    private static void validarIsc(Item item, String campo, List<Violacion> v) {
        Isc isc = item.isc();
        boolean completo = switch (isc.sistema()) {
            case AL_VALOR -> isc.tasa() != null;
            case ESPECIFICO -> isc.montoFijoUnitario() != null;
            case PRECIO_VENTA_PUBLICO -> isc.tasa() != null && isc.precioVentaPublicoSugerido() != null;
        };
        if (!completo) {
            v.add(new Violacion("VAL-TRB-03", campo + "/isc",
                    "Faltan los datos del sistema " + isc.sistema().codigo() + " del ISC (Catálogo N.° 08)"));
        }
        if (item.afectacion() != TipoAfectacionIgv.GRAVADO_ONEROSA) {
            v.add(new Violacion("VAL-TRB-04", campo + "/isc", "Solo un ítem gravado oneroso (10) admite ISC"));
        }
    }

    /** VAL-PAG-02 y VAL-PAG-03. */
    private static void validarCredito(FormaPago formaPago, BigDecimal total, List<Violacion> v) {
        if (formaPago == null || formaPago.tipo() != FormaPago.Tipo.CREDITO) {
            return;
        }
        BigDecimal pendiente = formaPago.montoPendiente() != null ? formaPago.montoPendiente() : total;
        if (formaPago.cuotas().isEmpty()) {
            v.add(new Violacion("VAL-PAG-02", "/formaPago/cuotas", "El crédito debe tener al menos una cuota"));
        }
        if (pendiente.signum() <= 0 || pendiente.compareTo(total) > 0) {
            v.add(new Violacion("VAL-PAG-02", "/formaPago/montoPendiente",
                    "El monto pendiente debe ser mayor que 0 y no superar el importe total (" + total + ")"));
        }
        BigDecimal sumaCuotas = formaPago.cuotas().stream().map(Cuota::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!formaPago.cuotas().isEmpty() && sumaCuotas.compareTo(pendiente) != 0) {
            v.add(new Violacion("VAL-PAG-03", "/formaPago/cuotas",
                    "La suma de las cuotas (" + sumaCuotas + ") debe ser igual al monto pendiente (" + pendiente + ")"));
        }
    }

    private static void lanzarSiHay(List<Violacion> violaciones) {
        if (!violaciones.isEmpty()) {
            throw new ReglaNegocioException(violaciones);
        }
    }
}
