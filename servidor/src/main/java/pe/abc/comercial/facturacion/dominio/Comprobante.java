package pe.abc.comercial.facturacion.dominio;

import pe.abc.comercial.facturacion.dominio.Partes.Emisor;

import java.util.Objects;
import java.util.UUID;

/**
 * Agregado raíz. Se crea ya numerado y calculado; un comprobante EMITIDO es inmutable (INV-01):
 * solo cambia su estado y se le adjunta, una única vez, su documento electrónico (INV-10).
 */
public final class Comprobante {

    /** Ciclo de vida (docs/04, Figura 4.1). */
    public enum Estado { EMITIDO, ENVIADO, ACEPTADO, OBSERVADO, RECHAZADO, EXCEPCION, BAJA_EN_PROCESO, ANULADO }

    /** XML firmado y su valor resumen (DigestValue). */
    public record DocumentoElectronico(String nombreArchivo, String hash, byte[] xml) {
    }

    private final UUID id;
    private final Emisor emisor;
    private final DatosComprobante datos;
    private final long correlativo;
    private final ResultadoCalculo calculo;
    private Estado estado;
    private DocumentoElectronico documentoElectronico;

    private Comprobante(UUID id, Emisor emisor, DatosComprobante datos, long correlativo, ResultadoCalculo calculo) {
        this.id = Objects.requireNonNull(id);
        this.emisor = Objects.requireNonNull(emisor);
        this.datos = Objects.requireNonNull(datos);
        this.correlativo = correlativo;
        this.calculo = Objects.requireNonNull(calculo);
        this.estado = Estado.EMITIDO;
    }

    /** INV-03: los totales vienen del motor tributario, nunca del cliente. */
    public static Comprobante emitir(UUID id, Emisor emisor, DatosComprobante datos, long correlativo,
                                     ResultadoCalculo calculo) {
        if (correlativo < 1 || correlativo > 99_999_999L) {
            throw new IllegalArgumentException("Correlativo fuera de rango (VAL-NUM-02): " + correlativo);
        }
        return new Comprobante(id, emisor, datos, correlativo, calculo);
    }

    public void adjuntarDocumentoElectronico(DocumentoElectronico documento) {
        if (this.documentoElectronico != null) {
            throw new IllegalStateException("El documento electrónico ya fue generado y no cambia (INV-10)");
        }
        this.documentoElectronico = Objects.requireNonNull(documento);
    }

    /** Serie-correlativo, p. ej. {@code F001-1} (VAL-NUM-01). */
    public String numero() {
        return datos.serie() + "-" + correlativo;
    }

    /** VAL-ENV-01: {@code RUC-TT-SERIE-NUMERO}. */
    public String nombreArchivo() {
        return emisor.ruc() + "-" + datos.tipo().codigo() + "-" + numero();
    }

    public UUID id() { return id; }
    public Emisor emisor() { return emisor; }
    public DatosComprobante datos() { return datos; }
    public long correlativo() { return correlativo; }
    public ResultadoCalculo calculo() { return calculo; }
    public Estado estado() { return estado; }
    public DocumentoElectronico documentoElectronico() { return documentoElectronico; }
}
