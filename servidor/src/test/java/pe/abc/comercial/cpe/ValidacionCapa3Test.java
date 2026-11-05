package pe.abc.comercial.cpe;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.w3c.dom.Document;
import pe.abc.comercial.Ensamble;
import pe.abc.comercial.cpe.api.ComprobanteElectronico;
import pe.abc.comercial.cpe.api.DocumentoElectronicoApi.DocumentoInvalidoException;
import pe.abc.comercial.cpe.aplicacion.GenerarDocumentoElectronicoService;
import pe.abc.comercial.cpe.infraestructura.firma.FirmadorXmlDsigAdapter;
import pe.abc.comercial.cpe.infraestructura.ubl.GeneradorUblAdapter;
import pe.abc.comercial.cpe.infraestructura.ubl.ValidadorDocumentoAdapter;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Capa 3 (docs/04 § 4.1): XSD OASIS (VAL-EST-02), perfil SUNAT (VAL-EST-03) y garantía de no emitir XML inválido. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Validación del documento (capa 3)")
class ValidacionCapa3Test {

    private final ValidadorDocumentoAdapter validador = new ValidadorDocumentoAdapter();
    private String valido;
    private ComprobanteElectronico comprobante;

    @BeforeAll
    void generar() {
        Ensamble sistema = new Ensamble();
        valido = GeneracionXmlUblTest.emitir(sistema, "01-emitir-factura-credito.request.json");
        comprobante = sistema.cpe.recibidos.get(0);
    }

    private List<String> validar(String xml) {
        return validador.validar(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void aceptaElXmlFirmadoGenerado() {
        assertThat(validar(valido)).isEmpty();
    }

    @Test
    void vat_est_02_rechazaElementosFaltantesDesordenadosEInventados() {
        assertThat(validar(valido.replace("<cbc:ID>F001-1</cbc:ID>", ""))).anyMatch(e -> e.startsWith("VAL-EST-02"));
        assertThat(validar(valido.replace("<cbc:LineCountNumeric>4</cbc:LineCountNumeric>",
                "<cbc:LineCountNumeric>4</cbc:LineCountNumeric><cbc:Inventado>x</cbc:Inventado>")))
                .anyMatch(e -> e.startsWith("VAL-EST-02"));
        assertThat(validar(valido.replace("<cbc:IssueDate>2026-10-22</cbc:IssueDate>",
                "<cbc:IssueDate>22/10/2026</cbc:IssueDate>"))).anyMatch(e -> e.contains("22/10/2026"));
    }

    @Test
    void vat_est_03_exigeLosDatosDelPerfilSunatQueElXsdDejaOpcionales() {
        String sinPerfil = valido.replaceAll("<cbc:UBLVersionID>2.1</cbc:UBLVersionID>", "")
                .replaceAll("<cbc:AddressTypeCode[^>]*>0000</cbc:AddressTypeCode>", "")
                .replaceAll(" unitCode=\"BX\"", "");

        assertThat(validar(sinPerfil)).contains("VAL-EST-03: falta UBLVersionID",
                "VAL-EST-03: falta AddressTypeCode del emisor", "VAL-EST-03: falta unitCode en una cantidad");
    }

    @Test
    void vat_est_03_unXmlSinFirmaNoSeAcepta() {
        String sinFirma = valido.replaceAll("(?s)<ds:Signature .*</ds:Signature>", "");

        assertThat(validar(sinFirma)).contains("VAL-EST-03: falta firma digital");
    }

    @Test
    void rechazaDoctypeParaPrevenirXxe() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///C:/Windows/win.ini\">]><x>&e;</x>";

        assertThat(validar(xxe)).singleElement().asString().startsWith("VAL-EST-02: XML ilegible").contains("DOCTYPE");
    }

    @Test
    void nuncaSeEntregaUnXmlInvalido() {
        // Doble de prueba: un generador defectuoso que borra el número del comprobante (cbc:ID es obligatorio).
        GeneradorUblAdapter real = new GeneradorUblAdapter();
        var defectuoso = new GenerarDocumentoElectronicoService.GeneradorUblPort() {
            @Override
            public Document generar(ComprobanteElectronico c) {
                Document documento = real.generar(c);
                var id = documento.getElementsByTagNameNS(GeneradorUblAdapter.NS_CBC, "ID").item(0);
                id.getParentNode().removeChild(id);
                return documento;
            }

            @Override
            public byte[] serializar(Document documento) {
                return real.serializar(documento);
            }
        };
        var cpe = new GenerarDocumentoElectronicoService(defectuoso,
                new FirmadorXmlDsigAdapter(Ensamble.llave(), Ensamble.certificado()),
                validador);

        assertThatThrownBy(() -> cpe.generar(comprobante))
                .isInstanceOf(DocumentoInvalidoException.class)
                .hasMessageContaining("no cumple UBL 2.1")
                .satisfies(e -> assertThat(((DocumentoInvalidoException) e).errores()).isNotEmpty());
    }
}
