package pe.abc.comercial.facturacion.dominio;

import java.util.List;

/**
 * Una o más reglas VAL-* no se cumplen (capa 2 → HTTP 422). Reúne todas las violaciones encontradas.
 */
public class ReglaNegocioException extends RuntimeException {

    /** Violación de una regla, con el JSON Pointer del campo (p. ej. {@code /items/0/cantidad}). */
    public record Violacion(String regla, String campo, String mensaje) {
    }

    private final transient List<Violacion> violaciones;

    public ReglaNegocioException(List<Violacion> violaciones) {
        super(violaciones.stream().map(v -> v.regla() + ": " + v.mensaje()).reduce((a, b) -> a + "; " + b).orElse(""));
        this.violaciones = List.copyOf(violaciones);
    }

    public ReglaNegocioException(String regla, String campo, String mensaje) {
        this(List.of(new Violacion(regla, campo, mensaje)));
    }

    public List<Violacion> violaciones() {
        return violaciones;
    }
}
