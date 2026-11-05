package pe.abc.comercial.cpe.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Lenguaje del módulo cpe: un comprobante ya numerado y calculado, listo para expresarse en UBL 2.1.
 * No contiene reglas tributarias (esas viven en facturacion); el módulo cpe solo lo representa, firma y valida.
 * Así cpe no depende de facturacion y puede extraerse como microservicio (docs/06 § 6.9).
 */
public record ComprobanteElectronico(String tipoDocumento,
                                     String numero,
                                     LocalDate fechaEmision,
                                     LocalTime horaEmision,
                                     LocalDate fechaVencimiento,
                                     String tipoOperacion,
                                     String moneda,
                                     List<Leyenda> leyendas,
                                     String ordenCompra,
                                     List<Referencia> guiasRemision,
                                     Emisor emisor,
                                     Parte adquirente,
                                     FormaPago formaPago,
                                     List<Subtotal> impuestos,
                                     BigDecimal totalImpuestos,
                                     BigDecimal totalValorVenta,
                                     BigDecimal totalPrecioVenta,
                                     BigDecimal importeTotal,
                                     List<Linea> lineas) {

    public record Leyenda(String codigo, String texto) {
    }

    public record Referencia(String tipo, String numero) {
    }

    public record Domicilio(String ubigeo, String codigoEstablecimiento, String provincia, String departamento,
                            String distrito, String direccion) {
    }

    public record Emisor(String ruc, String razonSocial, String nombreComercial, Domicilio domicilio) {
    }

    /** {@code direccion} nula: no se informa {@code cac:RegistrationAddress}. */
    public record Parte(String tipoDocumento, String numeroDocumento, String razonSocial, Domicilio direccion) {
    }

    public record FormaPago(String medio, BigDecimal montoPendiente, List<Cuota> cuotas) {
    }

    public record Cuota(BigDecimal monto, LocalDate fechaVencimiento) {
    }

    /** Código, nombre y código internacional del Catálogo N.° 05. */
    public record TributoUbl(String codigo, String nombre, String codigoInternacional) {
    }

    /**
     * Un {@code cac:TaxSubtotal}. Los campos opcionales nulos no se informan.
     *
     * @param base             {@code cbc:TaxableAmount}
     * @param porcentaje       {@code cbc:Percent}
     * @param afectacionIgv    {@code cbc:TaxExemptionReasonCode} (Cat. 07)
     * @param sistemaIsc       {@code cbc:TierRange} (Cat. 08)
     * @param unidadesBase     {@code cbc:BaseUnitMeasure} (ICBPER: número de bolsas)
     * @param montoPorUnidad   {@code cbc:PerUnitAmount} (ICBPER: tarifa)
     */
    public record Subtotal(TributoUbl tributo, BigDecimal base, BigDecimal monto, BigDecimal porcentaje,
                           String afectacionIgv, String sistemaIsc, BigDecimal unidadesBase, BigDecimal montoPorUnidad) {

        public static Subtotal cabecera(TributoUbl tributo, BigDecimal base, BigDecimal monto) {
            return new Subtotal(tributo, base, monto, null, null, null, null, null);
        }
    }

    /**
     * Un {@code cac:InvoiceLine}.
     *
     * @param precioReferencia {@code AlternativeConditionPrice} y su tipo (Cat. 16: 01 precio, 02 valor referencial)
     */
    public record Linea(int numero, String unidadMedida, BigDecimal cantidad, BigDecimal valorVenta,
                        BigDecimal precioReferencia, String tipoPrecio, BigDecimal totalImpuestos,
                        List<Subtotal> impuestos, String descripcion, String codigoProducto,
                        BigDecimal valorUnitario) {
    }
}
