package pe.abc.comercial;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import pe.abc.comercial.cpe.api.DocumentoElectronicoApi.DocumentoGenerado;
import pe.abc.comercial.cpe.infraestructura.ubl.ValidadorDocumentoAdapter;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RNF-03 de la fase 1: "regresión con los XML de referencia". Se emiten las solicitudes de
 * specs/ejemplos/json por el caso de uso real y el XML generado se compara con specs/ubl.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Regresión contra los XML de referencia de la fase 1 (RNF-03)")
class RegresionXmlReferenciaTest {

    private final Map<String, DocumentoGenerado> generados = new HashMap<>();

    @BeforeAll
    void emitirLasSolicitudesDeEjemplo() {
        Ensamble sistema = new Ensamble();
        // El orden reproduce la numeración de los ejemplos: F001-1, F001-2 y B001-1.
        for (String solicitud : new String[]{"01-emitir-factura-credito.request.json",
                "03-emitir-factura-isc.request.json", "02-emitir-boleta-icbper.request.json"}) {
            var resultado = sistema.servicio.emitir(sistema.solicitud(solicitud), "clave-" + solicitud);
            generados.put(resultado.comprobante().numero(), sistema.cpe.ultimo());
        }
    }

    @ParameterizedTest(name = "{0} equivale a specs/ubl/plantillas/{1}")
    @CsvSource({"F001-1, 20123456786-01-F001-1.xml", "F001-2, 20123456786-01-F001-2.xml",
            "B001-1, 20123456786-03-B001-1.xml"})
    void elXmlSinFirmaEsIgualALaPlantilla(String numero, String plantilla) {
        String esperado = Ensamble.leer(rutaSinFirma(plantilla));
        String generado = new String(generados.get(numero).xmlSinFirma(), StandardCharsets.UTF_8);

        Diff diferencias = DiffBuilder.compare(esperado).withTest(generado)
                .ignoreComments().ignoreWhitespace().checkForSimilar().build();

        assertThat(diferencias.hasDifferences()).as(diferencias.fullDescription()).isFalse();
    }

    @ParameterizedTest(name = "{0}: DigestValue idéntico al de specs/ubl/ejemplos/{1}")
    @CsvSource({"F001-1, 20123456786-01-F001-1.xml", "F001-2, 20123456786-01-F001-2.xml",
            "B001-1, 20123456786-03-B001-1.xml"})
    void elValorResumenDeLaFirmaCoincideConElDeLaFase1(String numero, String ejemplo) {
        Matcher digest = Pattern.compile("<ds:DigestValue>([^<]+)</ds:DigestValue>")
                .matcher(Ensamble.leer(Ensamble.UBL.resolve("ejemplos").resolve(ejemplo)));
        assertThat(digest.find()).isTrue();

        // Mismo DigestValue = misma forma canónica (C14N): el documento firmado es el mismo, bit a bit.
        assertThat(generados.get(numero).hash()).isEqualTo(digest.group(1));
    }

    @ParameterizedTest(name = "{0}: firmado, válido contra el XSD y con firma verificable")
    @CsvSource({"F001-1", "F001-2", "B001-1"})
    void elXmlFirmadoEsValidoYSuFirmaSeVerifica(String numero) throws Exception {
        byte[] firmado = generados.get(numero).xmlFirmado();

        assertThat(new ValidadorDocumentoAdapter().validar(firmado)).isEmpty();
        assertThat(firmaValida(firmado)).isTrue();
    }

    @Test
    void lasPlantillasSinFirmaNoCumplenElXsd() {
        // Hallazgo documentado: ext:ExtensionContent vacío es inválido; por eso VAL-EST-02 se aplica tras firmar.
        byte[] plantilla = Ensamble.leer(rutaSinFirma("20123456786-01-F001-1.xml"))
                .getBytes(StandardCharsets.UTF_8);

        assertThat(new ValidadorDocumentoAdapter().validar(plantilla))
                .anyMatch(e -> e.startsWith("VAL-EST-02") && e.contains("ExtensionContent"));
    }

    static boolean firmaValida(byte[] xml) throws Exception {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        Document documento = fabrica.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        Element firma = (Element) documento.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
        DOMValidateContext contexto = new DOMValidateContext(Ensamble.certificado().getPublicKey(), firma);
        return XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(contexto).validate(contexto);
    }

    static Path rutaSinFirma(String relativo) {
        Path p = Ensamble.UBL.resolve("fuentes-sin-firma").resolve(relativo);
        return Files.exists(p) ? p : Ensamble.UBL.resolve("plantillas").resolve(relativo);
    }
}
