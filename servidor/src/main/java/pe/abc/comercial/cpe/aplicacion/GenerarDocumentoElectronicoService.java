package pe.abc.comercial.cpe.aplicacion;

import org.w3c.dom.Document;
import pe.abc.comercial.cpe.api.ComprobanteElectronico;
import pe.abc.comercial.cpe.api.DocumentoElectronicoApi;

import java.util.List;
import java.util.Objects;

/**
 * Implementa {@link DocumentoElectronicoApi}: UBL → firma → validación (capa 3).
 *
 * <p>Hallazgo de la fase 2: un XML sin firma no cumple el XSD (el {@code ext:ExtensionContent} vacío es inválido;
 * así ocurre con specs/ubl/fuentes-sin-firma). Por eso VAL-EST-02 se aplica al XML <b>firmado</b>.</p>
 */
public class GenerarDocumentoElectronicoService implements DocumentoElectronicoApi {

    /** Representa el comprobante en UBL 2.1 (adaptador JAXB). */
    public interface GeneradorUblPort {
        Document generar(ComprobanteElectronico comprobante);

        byte[] serializar(Document documento);
    }

    /** XMLDSig enveloped dentro de {@code ext:ExtensionContent} (ADR-04: el certificado vive en el servidor). */
    public interface FirmadorXmlPort {
        /** Firma el documento en el sitio y devuelve el {@code ds:DigestValue}. */
        String firmar(Document documento);
    }

    /** XSD OASIS + perfil SUNAT. */
    public interface ValidadorDocumentoPort {
        List<String> validar(byte[] xml);
    }

    private final GeneradorUblPort generador;
    private final FirmadorXmlPort firmador;
    private final ValidadorDocumentoPort validador;

    public GenerarDocumentoElectronicoService(GeneradorUblPort generador, FirmadorXmlPort firmador,
                                              ValidadorDocumentoPort validador) {
        this.generador = Objects.requireNonNull(generador);
        this.firmador = Objects.requireNonNull(firmador);
        this.validador = Objects.requireNonNull(validador);
    }

    @Override
    public DocumentoGenerado generar(ComprobanteElectronico comprobante) {
        Document documento = generador.generar(comprobante);
        byte[] sinFirma = generador.serializar(documento);
        String hash = firmador.firmar(documento);
        byte[] firmado = generador.serializar(documento);

        List<String> errores = validador.validar(firmado);
        if (!errores.isEmpty()) {
            throw new DocumentoInvalidoException("El XML de " + comprobante.numero() + " no cumple UBL 2.1", errores);
        }
        String nombreArchivo = comprobante.emisor().ruc() + "-" + comprobante.tipoDocumento() + "-" + comprobante.numero();
        return new DocumentoGenerado(nombreArchivo, sinFirma, firmado, hash);
    }
}
