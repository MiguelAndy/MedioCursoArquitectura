package pe.abc.comercial.facturacion.dominio.catalogo;

/**
 * Catálogo N.° 05: códigos de tributos.
 * El orden de declaración es el orden en que se informan los {@code cac:TaxSubtotal} de la cabecera,
 * el mismo de los XML de referencia de specs/ubl/fuentes-sin-firma.
 */
public enum Tributo {

    IGV("1000", "IGV", "VAT"),
    ISC("2000", "ISC", "EXC"),
    EXPORTACION("9995", "EXP", "FRE"),
    GRATUITO("9996", "GRA", "FRE"),
    EXONERADO("9997", "EXO", "VAT"),
    INAFECTO("9998", "INA", "FRE"),
    ICBPER("7152", "ICBPER", "OTH");

    private final String codigo;
    private final String nombre;
    private final String codigoInternacional;

    Tributo(String codigo, String nombre, String codigoInternacional) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.codigoInternacional = codigoInternacional;
    }

    public String codigo() { return codigo; }

    /** {@code cbc:Name} del TaxScheme. */
    public String nombre() { return nombre; }

    /** UN/ECE 5153 ({@code cbc:TaxTypeCode}). */
    public String codigoInternacional() { return codigoInternacional; }
}
