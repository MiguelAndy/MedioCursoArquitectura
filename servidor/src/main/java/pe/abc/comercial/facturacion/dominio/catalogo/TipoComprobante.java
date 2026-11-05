package pe.abc.comercial.facturacion.dominio.catalogo;

import java.util.Arrays;
import java.util.Optional;

/** Catálogo N.° 01: comprobantes que emite la fase 2 (las notas 07/08 quedan para la fase 3). */
public enum TipoComprobante {

    FACTURA("01", 'F', "Factura"),
    BOLETA("03", 'B', "Boleta de Venta");

    private final String codigo;
    private final char letraSerie;
    private final String nombre;

    TipoComprobante(String codigo, char letraSerie, String nombre) {
        this.codigo = codigo;
        this.letraSerie = letraSerie;
        this.nombre = nombre;
    }

    public static Optional<TipoComprobante> desdeCodigo(String codigo) {
        return Arrays.stream(values()).filter(t -> t.codigo.equals(codigo)).findFirst();
    }

    public String codigo() { return codigo; }

    /** Letra con la que inicia la serie (VAL-NUM-01). */
    public char letraSerie() { return letraSerie; }

    public String nombre() { return nombre; }
}
