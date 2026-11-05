package pe.abc.comercial.cpe.infraestructura.ubl;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import pe.abc.comercial.cpe.aplicacion.GenerarDocumentoElectronicoService.ValidadorDocumentoPort;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Capa 3 de validación (docs/04 § 4.1):
 * <ul>
 *   <li>VAL-EST-02: el XML cumple {@code UBL-Invoice-2.1.xsd} (XSD oficial de specs/ubl, sin modificar).</li>
 *   <li>VAL-EST-03: están los datos que el perfil SUNAT exige y el XSD deja opcionales.</li>
 * </ul>
 * El documento se lee rechazando {@code <!DOCTYPE>} (prevención de XXE).
 */
public final class ValidadorDocumentoAdapter implements ValidadorDocumentoPort {

    public static final String RUTA_XSD = "/xsd/ubl-2.1/maindoc/UBL-Invoice-2.1.xsd";

    /** VAL-EST-03: XPath → descripción del dato exigido por SUNAT. */
    private static final Map<String, String> PERFIL_SUNAT = Map.of(
            "/inv:Invoice/cbc:UBLVersionID", "UBLVersionID",
            "/inv:Invoice/cbc:CustomizationID", "CustomizationID",
            "/inv:Invoice/cbc:InvoiceTypeCode/@listID", "InvoiceTypeCode@listID (tipo de operación)",
            "/inv:Invoice/cbc:DocumentCurrencyCode", "DocumentCurrencyCode",
            "/inv:Invoice/ext:UBLExtensions//ds:Signature", "firma digital",
            "/inv:Invoice/cac:AccountingSupplierParty//cac:RegistrationAddress/cbc:AddressTypeCode", "AddressTypeCode del emisor");

    private final Schema esquema;

    public ValidadorDocumentoAdapter() {
        URL xsd = ValidadorDocumentoAdapter.class.getResource(RUTA_XSD);
        if (xsd == null) {
            throw new IllegalStateException("No se encontró el XSD en el classpath: " + RUTA_XSD);
        }
        try {
            SchemaFactory fabrica = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar");
            this.esquema = fabrica.newSchema(xsd);
        } catch (SAXException e) {
            throw new IllegalStateException("No se pudo compilar el XSD UBL 2.1", e);
        }
    }

    @Override
    public List<String> validar(byte[] xml) {
        List<String> errores = new ArrayList<>();
        Document documento;
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setNamespaceAware(true);
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var lector = fabrica.newDocumentBuilder();
            lector.setErrorHandler(null);   // el error se informa en la lista, no en la consola
            documento = lector.parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            return List.of("VAL-EST-02: XML ilegible: " + e.getMessage());
        }

        try {
            var validador = esquema.newValidator();
            validador.setErrorHandler(new ErrorHandler() {
                @Override public void warning(SAXParseException e) { }
                @Override public void error(SAXParseException e) { errores.add("VAL-EST-02: " + e.getMessage()); }
                @Override public void fatalError(SAXParseException e) { errores.add("VAL-EST-02: " + e.getMessage()); }
            });
            validador.validate(new DOMSource(documento));
        } catch (Exception e) {
            errores.add("VAL-EST-02: " + e.getMessage());
        }

        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new Espacios());
        PERFIL_SUNAT.forEach((expresion, dato) -> {
            if (!existe(xpath, expresion, documento)) {
                errores.add("VAL-EST-03: falta " + dato);
            }
        });
        if (existe(xpath, "//cbc:InvoicedQuantity[not(@unitCode)]", documento)) {
            errores.add("VAL-EST-03: falta unitCode en una cantidad");
        }
        return errores;
    }

    private static boolean existe(XPath xpath, String expresion, Document documento) {
        try {
            return (Boolean) xpath.evaluate("boolean(" + expresion + ")", documento, XPathConstants.BOOLEAN);
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class Espacios implements NamespaceContext {
        private static final Map<String, String> URIS = Map.of(
                "inv", GeneradorUblAdapter.NS_INVOICE, "cac", GeneradorUblAdapter.NS_CAC,
                "cbc", GeneradorUblAdapter.NS_CBC, "ext", GeneradorUblAdapter.NS_EXT, "ds", GeneradorUblAdapter.NS_DS);

        @Override public String getNamespaceURI(String prefijo) { return URIS.getOrDefault(prefijo, XMLConstants.NULL_NS_URI); }
        @Override public String getPrefix(String uri) { return null; }
        @Override public Iterator<String> getPrefixes(String uri) { return null; }
    }
}
