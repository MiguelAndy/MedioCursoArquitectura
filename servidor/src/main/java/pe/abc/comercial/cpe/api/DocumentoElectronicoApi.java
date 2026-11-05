package pe.abc.comercial.cpe.api;

/**
 * API pública del módulo cpe (docs/06, Tabla 6.3): genera el XML UBL 2.1, lo firma y lo valida.
 * En la fase 1 se llama en proceso; en la fase 3 será un cliente HTTP/gRPC hacia cpe-service,
 * sin cambiar este contrato.
 */
public interface DocumentoElectronicoApi {

    /**
     * @throws DocumentoInvalidoException si el XML firmado no cumple el XSD o el perfil SUNAT (capa 3):
     *                                    es un error interno, el comprobante no se emite.
     */
    DocumentoGenerado generar(ComprobanteElectronico comprobante);

    /**
     * Resultado de la generación.
     *
     * @param xmlSinFirma XML antes de firmar (equivale a specs/ubl/plantillas)
     * @param xmlFirmado  XML firmado con XMLDSig (equivale a specs/ubl/ejemplos)
     * @param hash        {@code ds:DigestValue} de la firma (INV-10)
     */
    record DocumentoGenerado(String nombreArchivo, byte[] xmlSinFirma, byte[] xmlFirmado, String hash) {
    }

    /** Capa 3 (VAL-EST-02 / VAL-EST-03) rechazó el documento. */
    class DocumentoInvalidoException extends RuntimeException {
        private final transient java.util.List<String> errores;

        public DocumentoInvalidoException(String mensaje, java.util.List<String> errores) {
            super(mensaje + ": " + String.join("; ", errores));
            this.errores = java.util.List.copyOf(errores);
        }

        public java.util.List<String> errores() {
            return errores;
        }
    }
}
