package pe.abc.comercial.facturacion.dominio.catalogo;

import java.util.Arrays;
import java.util.Optional;

/** Catálogo N.° 02 (ISO 4217), con el nombre usado en la leyenda 1000 (monto en letras). */
public enum Moneda {

    PEN("SOLES"),
    USD("DÓLARES AMERICANOS"),
    EUR("EUROS");

    private final String nombreEnLetras;

    Moneda(String nombreEnLetras) {
        this.nombreEnLetras = nombreEnLetras;
    }

    public static Optional<Moneda> desdeCodigo(String codigo) {
        return Arrays.stream(values()).filter(m -> m.name().equals(codigo)).findFirst();
    }

    public String codigo() { return name(); }

    public String nombreEnLetras() { return nombreEnLetras; }
}
