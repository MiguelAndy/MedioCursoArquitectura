package pe.abc.comercial.cpe.infraestructura.firma;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import pe.abc.comercial.cpe.aplicacion.GenerarDocumentoElectronicoService.FirmadorXmlPort;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;

/**
 * Adaptador de {@code FirmadorXmlPort} (docs/06, Tabla 6.3): XMLDSig "enveloped" con el JDK (JSR 105),
 * dentro de {@code ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent} (Anexo N.° 9-A, campo 9).
 * Es la herramienta de conformidad de la fase 1 ({@code tools/conformidad/FirmarXmlPrueba.java}) llevada a producción.
 */
public final class FirmadorXmlDsigAdapter implements FirmadorXmlPort {

    static {
        // Base64 sin saltos de línea en DigestValue, SignatureValue y X509Certificate (igual que la fase 1).
        System.setProperty("com.sun.org.apache.xml.internal.security.ignoreLineBreaks", "true");
    }

    private static final String NS_EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    private static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    /** Debe coincidir con cac:Signature/.../cbc:URI ("#SignatureABC"). */
    public static final String ID_FIRMA = "SignatureABC";

    private final PrivateKey llave;
    private final X509Certificate certificado;

    public FirmadorXmlDsigAdapter(PrivateKey llave, X509Certificate certificado) {
        this.llave = Objects.requireNonNull(llave);
        this.certificado = Objects.requireNonNull(certificado);
    }

    @Override
    public String firmar(Document documento) {
        NodeList contenedores = documento.getElementsByTagNameNS(NS_EXT, "ExtensionContent");
        if (contenedores.getLength() == 0) {
            throw new IllegalStateException("El XML no tiene ext:ExtensionContent donde alojar la firma");
        }
        Node contenedor = contenedores.item(0);
        try {
            XMLSignatureFactory fabrica = XMLSignatureFactory.getInstance("DOM");
            Reference referencia = fabrica.newReference("", fabrica.newDigestMethod(DigestMethod.SHA256, null),
                    List.of(fabrica.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);
            SignedInfo infoFirmada = fabrica.newSignedInfo(
                    fabrica.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                    fabrica.newSignatureMethod(RSA_SHA256, null), List.of(referencia));
            KeyInfoFactory kif = fabrica.getKeyInfoFactory();
            var infoLlave = kif.newKeyInfo(List.of(kif.newX509Data(
                    List.of(certificado.getSubjectX500Principal().getName(), certificado))));

            DOMSignContext contexto = new DOMSignContext(llave, contenedor);
            contexto.setDefaultNamespacePrefix("ds");
            fabrica.newXMLSignature(infoFirmada, infoLlave, null, ID_FIRMA, null).sign(contexto);

            return documento.getElementsByTagNameNS(XMLSignature.XMLNS, "DigestValue").item(0).getTextContent();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el XML", e);
        }
    }

    public X509Certificate certificado() {
        return certificado;
    }
}
