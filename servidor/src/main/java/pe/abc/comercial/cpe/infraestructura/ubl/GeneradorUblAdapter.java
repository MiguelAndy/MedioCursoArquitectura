package pe.abc.comercial.cpe.infraestructura.ubl;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import oasis.names.specification.ubl.schema.xsd.invoice_2.ObjectFactory;
import org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import pe.abc.comercial.cpe.api.ComprobanteElectronico;
import pe.abc.comercial.cpe.aplicacion.GenerarDocumentoElectronicoService.GeneradorUblPort;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

/**
 * Serializa el {@code InvoiceType} con JAXB y lo carga como DOM para firmarlo.
 *
 * <p>La salida usa la misma forma que specs/ubl/fuentes-sin-firma (indentación de 2 espacios y los namespaces
 * {@code cac, cbc, ext, ds} declarados en la raíz): así su forma canónica (C14N) es idéntica y el
 * {@code DigestValue} de la firma coincide con el de los ejemplos firmados de la fase 1.</p>
 */
public final class GeneradorUblAdapter implements GeneradorUblPort {

    public static final String NS_INVOICE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    public static final String NS_CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    public static final String NS_CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    public static final String NS_EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    public static final String NS_DS = "http://www.w3.org/2000/09/xmldsig#";

    private static final Map<String, String> PREFIJOS = Map.of(
            NS_INVOICE, "", NS_CAC, "cac", NS_CBC, "cbc", NS_EXT, "ext", NS_DS, "ds");

    private final JAXBContext contexto;
    private final InvoiceUblMapper mapper = new InvoiceUblMapper();
    private final ObjectFactory fabrica = new ObjectFactory();
    private final MessageSource messageSource;

    public GeneradorUblAdapter() {
        this(crearMessageSourcePorDefecto());
    }

    public GeneradorUblAdapter(MessageSource messageSource) {
        this.messageSource = messageSource != null ? messageSource : crearMessageSourcePorDefecto();
        try {
            this.contexto = JAXBContext.newInstance(ObjectFactory.class);
        } catch (JAXBException e) {
            String mensaje = obtenerMensaje("error.xml.jaxb", "No se pudo crear el contexto JAXB de UBL 2.1");
            throw new IllegalStateException(mensaje, e);
        }
    }

    public MessageSource getMessageSource() {
        return this.messageSource;
    }

    public String obtenerMensaje(String codigo, String porDefecto, Object... args) {
        return obtenerMensaje(codigo, LocaleContextHolder.getLocale(), porDefecto, args);
    }

    public String obtenerMensaje(String codigo, Locale locale, String porDefecto, Object... args) {
        try {
            return messageSource.getMessage(codigo, args, porDefecto, locale != null ? locale : Locale.getDefault());
        } catch (Exception ex) {
            return porDefecto;
        }
    }

    private static MessageSource crearMessageSourcePorDefecto() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setUseCodeAsDefaultMessage(true);
        return source;
    }

    @Override
    public Document generar(ComprobanteElectronico comprobante) {
        try {
            Marshaller marshaller = contexto.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty("org.glassfish.jaxb.indentString", "  ");
            marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", new Prefijos());
            StringWriter xml = new StringWriter();
            marshaller.marshal(fabrica.createInvoice(mapper.mapear(comprobante)), xml);
            return parsear(xml.toString());
        } catch (JAXBException e) {
            String mensaje = obtenerMensaje("error.xml.generacion", "No se pudo serializar el comprobante " + comprobante.numero(), comprobante.numero());
            throw new IllegalStateException(mensaje, e);
        } catch (Exception e) {
            String mensaje = obtenerMensaje("IOerror", "Error de entrada salida");
            throw new IllegalStateException(mensaje, e);
        }
    }

    @Override
    public byte[] serializar(Document documento) {
        try {
            var transformador = TransformerFactory.newInstance().newTransformer();
            transformador.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            transformador.transform(new DOMSource(documento), new StreamResult(salida));
            return salida.toByteArray();
        } catch (Exception e) {
            String mensaje = obtenerMensaje("IOerror", "Error de entrada salida");
            throw new IllegalStateException(mensaje, e);
        }
    }

    private Document parsear(String xml) {
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setNamespaceAware(true);
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return fabrica.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            String mensaje = obtenerMensaje("error.xml.parseo", "XML generado ilegible");
            throw new IllegalStateException(mensaje, e);
        }
    }

    /** Prefijos convencionales y namespaces declarados en la raíz, igual que en specs/ubl/fuentes-sin-firma. */
    private static final class Prefijos extends NamespacePrefixMapper {
        @Override
        public String getPreferredPrefix(String uri, String sugerencia, boolean requierePrefijo) {
            String prefijo = PREFIJOS.get(uri);
            return prefijo == null || (prefijo.isEmpty() && requierePrefijo) ? sugerencia : prefijo;
        }

        @Override
        public String[] getPreDeclaredNamespaceUris() {
            return new String[]{NS_CAC, NS_CBC, NS_EXT, NS_DS};
        }
    }
}
