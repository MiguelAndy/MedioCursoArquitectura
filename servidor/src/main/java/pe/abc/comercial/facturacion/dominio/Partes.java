package pe.abc.comercial.facturacion.dominio;

/**
 * Participantes del comprobante: emisor (configuración de la empresa) y adquirente (cliente).
 * Se agrupan en un archivo porque son objetos de valor pequeños e inseparables del agregado.
 */
public final class Partes {

    private Partes() {
    }

    /** Documento de identidad del Catálogo N.° 06 ({@code tipo} = código: 0, 1, 4, 6, 7...). */
    public record DocumentoIdentidad(String tipo, String numero) {

        public static final String RUC = "6";
        public static final String DNI = "1";

        public boolean esRuc() {
            return RUC.equals(tipo);
        }

        /** Un adquirente "no identificado" usa el tipo 0 (o "-"), p. ej. CLIENTES VARIOS (SUP-08). */
        public boolean identificado() {
            return !"0".equals(tipo) && !"-".equals(tipo);
        }
    }

    /** Adquirente. {@code direccion} y {@code ubigeo} son opcionales (TR-A18). */
    public record Adquirente(DocumentoIdentidad documento, String razonSocial, String direccion, String ubigeo) {
    }

    /** Domicilio fiscal del emisor (TR-A14, TR-A15). */
    public record Domicilio(String direccion, String distrito, String provincia, String departamento,
                            String ubigeo, String codigoEstablecimiento) {
    }

    /** Emisor: la empresa (módulo configuracion). */
    public record Emisor(String ruc, String razonSocial, String nombreComercial, Domicilio domicilio) {
    }
}
