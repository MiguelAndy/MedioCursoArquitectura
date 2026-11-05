package pe.abc.comercial.facturacion.dominio.catalogo;

import java.util.Arrays;
import java.util.Optional;

/** Catálogo N.° 08: sistemas de cálculo del ISC (VAL-TRB-03). Se informa en {@code cbc:TierRange}. */
public enum SistemaIsc {

    AL_VALOR("01"),
    ESPECIFICO("02"),
    PRECIO_VENTA_PUBLICO("03");

    private final String codigo;

    SistemaIsc(String codigo) {
        this.codigo = codigo;
    }

    public static Optional<SistemaIsc> desdeCodigo(String codigo) {
        return Arrays.stream(values()).filter(s -> s.codigo.equals(codigo)).findFirst();
    }

    public String codigo() { return codigo; }
}
