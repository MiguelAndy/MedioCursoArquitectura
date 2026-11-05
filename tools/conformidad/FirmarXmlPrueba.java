/*
 * Herramienta de conformidad del contrato FirmadorXmlPort (spike técnico, NO es lógica de negocio).
 *
 * Firma un XML UBL 2.1 con XMLDSig "enveloped" dentro de ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent,
 * tal como exige SUNAT (Anexo N.° 9-A, campo 9 "Firma digital"), y verifica la firma recién creada.
 * Usa solo el JDK (javax.xml.crypto.dsig, JSR 105). Se ejecuta en modo "source launcher" (Java 11+):
 *
 *   java FirmarXmlPrueba.java <entrada.xml> <salida.xml> <almacen.p12> <clave> <alias>
 *
 * El certificado de prueba es autofirmado y se genera al vuelo con keytool (ver tools/validar_specs.py);
 * NO es válido ante SUNAT y nunca se versiona (.gitignore excluye *.p12).
 */
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FirmarXmlPrueba {

    private static final String NS_EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    private static final String NS_DS = XMLSignature.XMLNS;
    private static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    // Debe coincidir con cac:Signature/cac:DigitalSignatureAttachment/cac:ExternalReference/cbc:URI ("#SignatureABC").
    private static final String ID_FIRMA = "SignatureABC";

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.err.println("Uso: java FirmarXmlPrueba.java <entrada.xml> <salida.xml> <almacen.p12> <clave> <alias>");
            System.exit(2);
        }
        // Base64 sin saltos de línea en DigestValue, SignatureValue y X509Certificate (debe fijarse antes de cargar XMLDSig).
        System.setProperty("com.sun.org.apache.xml.internal.security.ignoreLineBreaks", "true");
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = dbf.newDocumentBuilder().parse(new File(args[0]));

        KeyStore almacen = KeyStore.getInstance("PKCS12");
        try (FileInputStream in = new FileInputStream(args[2])) {
            almacen.load(in, args[3].toCharArray());
        }
        PrivateKey llave = (PrivateKey) almacen.getKey(args[4], args[3].toCharArray());
        X509Certificate certificado = (X509Certificate) almacen.getCertificate(args[4]);

        NodeList contenedores = doc.getElementsByTagNameNS(NS_EXT, "ExtensionContent");
        if (contenedores.getLength() == 0) {
            throw new IllegalStateException("El XML no tiene ext:ExtensionContent donde alojar la firma");
        }
        Node contenedor = contenedores.item(0);

        XMLSignatureFactory fabrica = XMLSignatureFactory.getInstance("DOM");
        Reference referencia = fabrica.newReference(
                "",
                fabrica.newDigestMethod(DigestMethod.SHA256, null),
                List.of(fabrica.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                null,
                null);
        SignedInfo infoFirmada = fabrica.newSignedInfo(
                fabrica.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fabrica.newSignatureMethod(RSA_SHA256, null),
                List.of(referencia));
        KeyInfoFactory kif = fabrica.getKeyInfoFactory();
        X509Data datosX509 = kif.newX509Data(List.of(certificado.getSubjectX500Principal().getName(), certificado));
        KeyInfo infoLlave = kif.newKeyInfo(List.of(datosX509));

        DOMSignContext contexto = new DOMSignContext(llave, contenedor);
        contexto.setDefaultNamespacePrefix("ds");
        XMLSignature firma = fabrica.newXMLSignature(infoFirmada, infoLlave, null, ID_FIRMA, null);
        firma.sign(contexto);

        TransformerFactory.newInstance().newTransformer()
                .transform(new DOMSource(doc), new StreamResult(new File(args[1])));

        // Verificación inmediata sobre el archivo escrito (no sobre el DOM en memoria).
        Document firmado = dbf.newDocumentBuilder().parse(new File(args[1]));
        Element nodoFirma = (Element) firmado.getElementsByTagNameNS(NS_DS, "Signature").item(0);
        DOMValidateContext validacion = new DOMValidateContext(certificado.getPublicKey(), nodoFirma);
        boolean valida = fabrica.unmarshalXMLSignature(validacion).validate(validacion);
        String valorResumen = firmado.getElementsByTagNameNS(NS_DS, "DigestValue").item(0).getTextContent();

        System.out.println(new File(args[1]).getName()
                + " | firma valida: " + valida
                + " | DigestValue (valor resumen): " + valorResumen
                + " | huella cert SHA-256: " + huella(certificado));
        if (!valida) {
            System.exit(1);
        }
    }

    private static String huella(X509Certificate certificado) throws Exception {
        byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(certificado.getEncoded());
        return Base64.getEncoder().encodeToString(digest);
    }
}
