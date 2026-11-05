package pe.abc.comercial.facturacion.dominio.catalogo;

import java.util.Arrays;
import java.util.Optional;

/**
 * Catálogo N.° 07: tipo de afectación del IGV.
 *
 * <ul>
 *   <li>{@code gravada}: se le aplica la tasa del IGV (en las gratuitas, como IGV referencial).</li>
 *   <li>{@code gratuita}: operación no onerosa (VAL-ITM-04): lleva valor referencial y no suma al total (VAL-TRB-09).</li>
 * </ul>
 * La afectación 17 (IVAP) no se incluye: requiere el tributo 1016, fuera del alcance de la fase 2.
 */
public enum TipoAfectacionIgv {

    GRAVADO_ONEROSA("10", Tributo.IGV, true, false),
    GRAVADO_RETIRO_PREMIO("11", Tributo.GRATUITO, true, true),
    GRAVADO_RETIRO_DONACION("12", Tributo.GRATUITO, true, true),
    GRAVADO_RETIRO("13", Tributo.GRATUITO, true, true),
    GRAVADO_RETIRO_PUBLICIDAD("14", Tributo.GRATUITO, true, true),
    GRAVADO_BONIFICACION("15", Tributo.GRATUITO, true, true),
    GRAVADO_RETIRO_TRABAJADORES("16", Tributo.GRATUITO, true, true),
    EXONERADO_ONEROSA("20", Tributo.EXONERADO, false, false),
    EXONERADO_GRATUITA("21", Tributo.GRATUITO, false, true),
    INAFECTO_ONEROSA("30", Tributo.INAFECTO, false, false),
    INAFECTO_RETIRO_BONIFICACION("31", Tributo.GRATUITO, false, true),
    INAFECTO_RETIRO("32", Tributo.GRATUITO, false, true),
    INAFECTO_RETIRO_MUESTRAS("33", Tributo.GRATUITO, false, true),
    INAFECTO_RETIRO_CONVENIO("34", Tributo.GRATUITO, false, true),
    INAFECTO_RETIRO_PREMIO("35", Tributo.GRATUITO, false, true),
    INAFECTO_RETIRO_PUBLICIDAD("36", Tributo.GRATUITO, false, true),
    INAFECTO_TRANSFERENCIA_GRATUITA("37", Tributo.GRATUITO, false, true),
    EXPORTACION("40", Tributo.EXPORTACION, false, false);

    private final String codigo;
    private final Tributo tributo;
    private final boolean gravada;
    private final boolean gratuita;

    TipoAfectacionIgv(String codigo, Tributo tributo, boolean gravada, boolean gratuita) {
        this.codigo = codigo;
        this.tributo = tributo;
        this.gravada = gravada;
        this.gratuita = gratuita;
    }

    public static Optional<TipoAfectacionIgv> desdeCodigo(String codigo) {
        return Arrays.stream(values()).filter(a -> a.codigo.equals(codigo)).findFirst();
    }

    public String codigo() { return codigo; }

    /** Tributo con el que se informa la línea ({@code TaxScheme/cbc:ID}). */
    public Tributo tributo() { return tributo; }

    public boolean gravada() { return gravada; }

    public boolean gratuita() { return gratuita; }
}
