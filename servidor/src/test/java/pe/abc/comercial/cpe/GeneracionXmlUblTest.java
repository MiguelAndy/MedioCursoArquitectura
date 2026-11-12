package pe.abc.comercial.cpe;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xmlunit.assertj3.XmlAssert;
import pe.abc.comercial.Ensamble;
import pe.abc.comercial.cpe.infraestructura.ubl.ValidadorDocumentoAdapter;

import java.nio.charset.StandardCharsets;
import org.w3c.dom.Document;
import pe.abc.comercial.cpe.api.ComprobanteElectronico;
import pe.abc.comercial.cpe.infraestructura.ubl.GeneradorUblAdapter;
import java.util.Locale;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de GENERACIÓN del XML UBL 2.1 (Invoice): cada sección del documento producido
 * por el caso de uso real, verificada con XPath. Complementa la regresión contra specs/ubl.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Generación del XML UBL 2.1 Invoice")
class GeneracionXmlUblTest {

    static final Map<String, String> NS = Map.of(
            "inv", "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2",
            "cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2",
            "cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2",
            "ext", "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2",
            "ds", "http://www.w3.org/2000/09/xmldsig#");

    private String factura;   // F001-1: crédito, bonificación, guía, orden de compra
    private String facturaIsc; // F001-2: ISC sistema 01
    private String boleta;    // B001-1: precio con IGV, exonerado, ICBPER

    @BeforeAll
    void generar() {
        Ensamble sistema = new Ensamble();
        factura = emitir(sistema, "01-emitir-factura-credito.request.json");
        facturaIsc = emitir(sistema, "03-emitir-factura-isc.request.json");
        boleta = emitir(sistema, "02-emitir-boleta-icbper.request.json");
    }

    static String emitir(Ensamble sistema, String solicitud) {
        sistema.servicio.emitir(sistema.solicitud(solicitud), solicitud);
        return new String(sistema.cpe.ultimo().xmlFirmado(), StandardCharsets.ISO_8859_1);
    }

    static XmlAssert xml(String documento) {
        return XmlAssert.assertThat(documento).withNamespaceContext(NS);
    }

    @Nested
    @DisplayName("Cabecera (Anexo N.° 9-A)")
    class Cabecera {

        @Test
        void versionPersonalizacionNumeroYFechas() {
            xml(factura).valueByXPath("/inv:Invoice/cbc:UBLVersionID").isEqualTo("2.1");
            xml(factura).valueByXPath("/inv:Invoice/cbc:CustomizationID/@schemeAgencyName").isEqualTo("PE:SUNAT");
            xml(factura).valueByXPath("/inv:Invoice/cbc:ID").isEqualTo("F001-1");
            xml(factura).valueByXPath("/inv:Invoice/cbc:IssueDate").isEqualTo("2026-10-22");
            xml(factura).valueByXPath("/inv:Invoice/cbc:IssueTime").isEqualTo("10:30:00");
            xml(factura).valueByXPath("/inv:Invoice/cbc:DueDate").isEqualTo("2026-12-21");
            xml(facturaIsc).doesNotHaveXPath("/inv:Invoice/cbc:DueDate");
        }

        @Test
        void tipoDeComprobanteYDeOperacion() {
            xml(factura).valueByXPath("/inv:Invoice/cbc:InvoiceTypeCode").isEqualTo("01");
            xml(factura).valueByXPath("/inv:Invoice/cbc:InvoiceTypeCode/@listID").isEqualTo("0101");
            xml(boleta).valueByXPath("/inv:Invoice/cbc:InvoiceTypeCode").isEqualTo("03");
        }

        @Test
        void leyendas1000Y1002() {
            xml(factura).valueByXPath("/inv:Invoice/cbc:Note[@languageLocaleID='1000']")
                    .isEqualTo("SON: DOS MIL SEISCIENTOS SESENTA Y SIETE CON 98/100 SOLES");
            xml(factura).valueByXPath("/inv:Invoice/cbc:Note[@languageLocaleID='1002']")
                    .isEqualTo("TRANSFERENCIA GRATUITA DE UN BIEN Y/O SERVICIO PRESTADO GRATUITAMENTE");
            xml(boleta).doesNotHaveXPath("/inv:Invoice/cbc:Note[@languageLocaleID='1002']");
        }

        @Test
        void monedaNumeroDeLineasOrdenDeCompraYGuia() {
            xml(factura).valueByXPath("/inv:Invoice/cbc:DocumentCurrencyCode").isEqualTo("PEN");
            xml(factura).valueByXPath("/inv:Invoice/cbc:LineCountNumeric").isEqualTo("4");
            xml(factura).valueByXPath("/inv:Invoice/cac:OrderReference/cbc:ID").isEqualTo("OC-0099");
            xml(factura).valueByXPath("/inv:Invoice/cac:DespatchDocumentReference/cbc:ID").isEqualTo("T001-45");
            xml(factura).valueByXPath("/inv:Invoice/cac:DespatchDocumentReference/cbc:DocumentTypeCode").isEqualTo("09");
        }
    }

    @Nested
    @DisplayName("Firma digital (ADR-04)")
    class Firma {

        @Test
        void laFirmaVaEnUblExtensionsConElIdReferenciado() {
            xml(factura).hasXPath("/inv:Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/ds:Signature");
            xml(factura).valueByXPath("//ds:Signature/@Id").isEqualTo("SignatureABC");
            xml(factura).valueByXPath("//ds:SignatureMethod/@Algorithm")
                    .isEqualTo("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
            xml(factura).hasXPath("//ds:KeyInfo/ds:X509Data/ds:X509Certificate");
        }

        @Test
        void cacSignatureIdentificaAlFirmante() {
            xml(factura).valueByXPath("/inv:Invoice/cac:Signature/cbc:ID").isEqualTo("IDSignABC");
            xml(factura).valueByXPath("/inv:Invoice/cac:Signature/cac:SignatoryParty/cac:PartyIdentification/cbc:ID")
                    .isEqualTo("20123456786");
            xml(factura).valueByXPath("//cac:DigitalSignatureAttachment/cac:ExternalReference/cbc:URI")
                    .isEqualTo("#SignatureABC");
        }
    }

    @Nested
    @DisplayName("Emisor y adquirente")
    class Partes {

        @Test
        void emisorConRucYDomicilioFiscal() {
            String emisor = "/inv:Invoice/cac:AccountingSupplierParty/cac:Party";
            xml(boleta).valueByXPath(emisor + "/cac:PartyIdentification/cbc:ID/@schemeID").isEqualTo("6");
            xml(boleta).valueByXPath(emisor + "/cac:PartyName/cbc:Name").isEqualTo("ABC ABARROTES");
            xml(boleta).valueByXPath(emisor + "//cac:RegistrationAddress/cbc:AddressTypeCode").isEqualTo("0000");
            xml(boleta).valueByXPath(emisor + "//cac:RegistrationAddress/cbc:ID").isEqualTo("100101");
        }

        @Test
        void adquirenteNoIdentificadoEnBoleta() {
            String cliente = "/inv:Invoice/cac:AccountingCustomerParty/cac:Party";
            xml(boleta).valueByXPath(cliente + "/cac:PartyIdentification/cbc:ID/@schemeID").isEqualTo("0");
            xml(boleta).valueByXPath(cliente + "/cac:PartyLegalEntity/cbc:RegistrationName").isEqualTo("CLIENTES VARIOS");
        }

        @Test
        void aclaracionA02_direccionDelAdquirenteSoloConGuiaDeRemision() {
            String direccion = "/inv:Invoice/cac:AccountingCustomerParty//cac:RegistrationAddress";
            xml(factura).valueByXPath(direccion + "/cac:AddressLine/cbc:Line").isEqualTo("JR. HUALLAGA 456, TINGO MARIA");
            xml(factura).doesNotHaveXPath(direccion + "/cbc:AddressTypeCode");
            xml(facturaIsc).doesNotHaveXPath(direccion);   // trae dirección pero no guía
        }
    }

    @Nested
    @DisplayName("Forma de pago")
    class FormaPago {

        @Test
        void creditoConMontoPendienteYCuotas() {
            xml(factura).nodesByXPath("/inv:Invoice/cac:PaymentTerms").hasSize(3);
            xml(factura).valueByXPath("/inv:Invoice/cac:PaymentTerms[1]/cbc:PaymentMeansID").isEqualTo("Credito");
            xml(factura).valueByXPath("/inv:Invoice/cac:PaymentTerms[1]/cbc:Amount").isEqualTo("2667.98");
            xml(factura).valueByXPath("/inv:Invoice/cac:PaymentTerms[3]/cbc:PaymentMeansID").isEqualTo("Cuota002");
            xml(factura).valueByXPath("/inv:Invoice/cac:PaymentTerms[3]/cbc:PaymentDueDate").isEqualTo("2026-12-21");
        }

        @Test
        void contadoSinMontoYBoletaSinFormaDePago() {
            xml(facturaIsc).valueByXPath("/inv:Invoice/cac:PaymentTerms/cbc:PaymentMeansID").isEqualTo("Contado");
            xml(facturaIsc).doesNotHaveXPath("/inv:Invoice/cac:PaymentTerms/cbc:Amount");
            xml(boleta).doesNotHaveXPath("/inv:Invoice/cac:PaymentTerms");
        }
    }

    @Nested
    @DisplayName("Tributos y totales")
    class Tributos {

        @Test
        void subtotalesDeCabeceraEnOrdenDelCatalogo05() {
            xml(boleta).nodesByXPath("/inv:Invoice/cac:TaxTotal/cac:TaxSubtotal/cac:TaxCategory/cac:TaxScheme/cbc:ID")
                    .extractingText().containsExactly("1000", "9997", "7152");
            xml(facturaIsc).nodesByXPath("/inv:Invoice/cac:TaxTotal/cac:TaxSubtotal/cac:TaxCategory/cac:TaxScheme/cbc:ID")
                    .extractingText().containsExactly("1000", "2000");
            xml(factura).nodesByXPath("/inv:Invoice/cac:TaxTotal/cac:TaxSubtotal/cac:TaxCategory/cac:TaxScheme/cbc:ID")
                    .extractingText().containsExactly("1000", "9996");
        }

        @Test
        void vat_trb_09_elTributoGratuitoNoSumaAlTotalDeTributos() {
            xml(factura).valueByXPath("/inv:Invoice/cac:TaxTotal/cbc:TaxAmount").isEqualTo("406.98");
            xml(factura).valueByXPath("/inv:Invoice/cac:TaxTotal/cac:TaxSubtotal[2]/cbc:TaxAmount").isEqualTo("5.40");
        }

        @Test
        void iscEnLaLineaConTasaYSistemaDelCatalogo08() {
            String isc = "/inv:Invoice/cac:InvoiceLine[1]/cac:TaxTotal/cac:TaxSubtotal[cac:TaxCategory/cac:TaxScheme/cbc:ID='2000']";
            xml(facturaIsc).valueByXPath(isc + "/cbc:TaxAmount").isEqualTo("12.00");
            xml(facturaIsc).valueByXPath(isc + "/cac:TaxCategory/cbc:Percent").isEqualTo("25.00");
            xml(facturaIsc).valueByXPath(isc + "/cac:TaxCategory/cbc:TierRange").isEqualTo("01");
            xml(facturaIsc).valueByXPath("/inv:Invoice/cac:InvoiceLine[1]/cac:TaxTotal/cac:TaxSubtotal[1]/cbc:TaxableAmount")
                    .isEqualTo("60.00");   // base del IGV con ISC
        }

        @Test
        void icbperConBolsasYTarifaSinBaseImponible() {
            String icbper = "/inv:Invoice/cac:InvoiceLine[3]/cac:TaxTotal/cac:TaxSubtotal[cac:TaxCategory/cac:TaxScheme/cbc:ID='7152']";
            xml(boleta).valueByXPath(icbper + "/cbc:BaseUnitMeasure").isEqualTo("2");
            xml(boleta).valueByXPath(icbper + "/cbc:BaseUnitMeasure/@unitCode").isEqualTo("NIU");
            xml(boleta).valueByXPath(icbper + "/cac:TaxCategory/cbc:PerUnitAmount").isEqualTo("0.50");
            xml(boleta).doesNotHaveXPath(icbper + "/cbc:TaxableAmount");
        }

        @Test
        void totalesYCuadreDeLineas() {
            xml(boleta).valueByXPath("/inv:Invoice/cac:LegalMonetaryTotal/cbc:LineExtensionAmount").isEqualTo("29.03");
            xml(boleta).valueByXPath("/inv:Invoice/cac:LegalMonetaryTotal/cbc:PayableAmount").isEqualTo("33.90");
            xml(boleta).valueByXPath("count(//*[@currencyID and @currencyID != 'PEN'])").isEqualTo("0");
        }
    }

    @Nested
    @DisplayName("Líneas")
    class Lineas {

        @Test
        void lineaGratuitaConPrecioReferencialTipo02YValorCero() {
            String linea = "/inv:Invoice/cac:InvoiceLine[4]";
            xml(factura).valueByXPath(linea + "/cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceTypeCode")
                    .isEqualTo("02");
            xml(factura).valueByXPath(linea + "/cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceAmount")
                    .isEqualTo("5.00");
            xml(factura).valueByXPath(linea + "/cac:Price/cbc:PriceAmount").isEqualTo("0.00");
            xml(factura).valueByXPath(linea + "//cbc:TaxExemptionReasonCode").isEqualTo("15");
        }

        @Test
        void lineaConPrecioConIgvDesglosaElValorUnitarioA10Decimales() {
            String linea = "/inv:Invoice/cac:InvoiceLine[1]";
            xml(boleta).valueByXPath(linea + "/cbc:LineExtensionAmount").isEqualTo("21.36");
            xml(boleta).valueByXPath(linea + "/cac:Price/cbc:PriceAmount").isEqualTo("3.5593220339");
            xml(boleta).valueByXPath(linea + "/cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceAmount")
                    .isEqualTo("4.20");
        }

        @Test
        void cantidadUnidadYDescripcion() {
            xml(factura).valueByXPath("/inv:Invoice/cac:InvoiceLine[1]/cbc:InvoicedQuantity").isEqualTo("10");
            xml(factura).valueByXPath("/inv:Invoice/cac:InvoiceLine[1]/cbc:InvoicedQuantity/@unitCode").isEqualTo("BX");
            xml(factura).valueByXPath("/inv:Invoice/cac:InvoiceLine[1]/cac:Item/cac:SellersItemIdentification/cbc:ID")
                    .isEqualTo("LEC-EVA-48");
        }
    }

    @Test
    void comprobanteEnDolaresLlevaLaMonedaEnTodosLosImportes() {
        Ensamble sistema = new Ensamble();
        ObjectNode cuerpo = (ObjectNode) Ensamble.leerJson(Ensamble.EJEMPLOS_JSON.resolve("03-emitir-factura-isc.request.json"));
        cuerpo.put("moneda", "USD").put("tipoCambio", "3.750");
        sistema.servicio.emitir(sistema.solicitud(cuerpo), "usd");
        String xml = new String(sistema.cpe.ultimo().xmlFirmado(), StandardCharsets.ISO_8859_1);

        xml(xml).valueByXPath("count(//*[@currencyID and @currencyID != 'USD'])").isEqualTo("0");
        xml(xml).valueByXPath("/inv:Invoice/cbc:Note").isEqualTo("SON: CIENTO SETENTA Y UNO CON 10/100 DÓLARES AMERICANOS");
        assertThat(new ValidadorDocumentoAdapter().validar(xml.getBytes(StandardCharsets.ISO_8859_1))).isEmpty();
    }

    @Test
    @DisplayName("Generación orientada a objetos (JAXB) instanciando comprobante y validando <cbc:ID>")
    void debeGenerarXmlUblConInstanciacionDeObjetosYValidarId() {
        GeneradorUblAdapter adapter = new GeneradorUblAdapter();
        Ensamble sistema = new Ensamble();
        var solicitud = sistema.solicitud("01-emitir-factura-credito.request.json");
        sistema.servicio.emitir(solicitud, "clave-test-directo");
        ComprobanteElectronico comprobante = sistema.cpe.recibidos.get(0);

        Document doc = adapter.generar(comprobante);
        byte[] xmlBytes = adapter.serializar(doc);
        String xmlContent = new String(xmlBytes, StandardCharsets.ISO_8859_1);

        // AssertJ
        assertThat(xmlContent).isNotEmpty();
        assertThat(xmlContent).contains("<cbc:ID>F001-1</cbc:ID>");
        assertThat(xmlContent).contains("<cbc:UBLVersionID>2.1</cbc:UBLVersionID>");

        // XMLUnit
        xml(xmlContent).hasXPath("/inv:Invoice/cbc:ID");
        xml(xmlContent).valueByXPath("/inv:Invoice/cbc:ID").isEqualTo("F001-1");
    }

    @Test
    @DisplayName("Soporte multi-idioma (i18n) para mensajes de error")
    void debeSoportarMensajesMultiIdiomaI18n() {
        GeneradorUblAdapter adapter = new GeneradorUblAdapter();
        String msgEs = adapter.obtenerMensaje("IOerror", Locale.forLanguageTag("es"), "default");
        String msgEn = adapter.obtenerMensaje("IOerror", Locale.ENGLISH, "default");

        assertThat(msgEs).isEqualTo("Error de entrada salida");
        assertThat(msgEn).isEqualTo("Input output error");

        String msgIoEs = adapter.obtenerMensaje("IOerror_es", Locale.forLanguageTag("es"), "default");
        String msgIoEn = adapter.obtenerMensaje("IOerror_en", Locale.ENGLISH, "default");
        assertThat(msgIoEs).isEqualTo("Error de entrada salida");
        assertThat(msgIoEn).isEqualTo("Input output error");
    }

    @Test
    @DisplayName("Aislado: InvoiceType instanciado directamente se serializa con Marshaller y contiene <cbc:ID>")
    void invoiceTypeInstanciadoDirectamenteGeneraCbcId() throws Exception {
        var idType = new oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.IDType();
        idType.setValue("F001-99");
        var invoice = new oasis.names.specification.ubl.schema.xsd.invoice_2.InvoiceType();
        invoice.setID(idType);

        var fabrica = new oasis.names.specification.ubl.schema.xsd.invoice_2.ObjectFactory();
        var marshaller = jakarta.xml.bind.JAXBContext
                .newInstance(oasis.names.specification.ubl.schema.xsd.invoice_2.ObjectFactory.class)
                .createMarshaller();
        marshaller.setProperty(jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        java.io.StringWriter salida = new java.io.StringWriter();
        marshaller.marshal(fabrica.createInvoice(invoice), salida);
        String generado = salida.toString();

        assertThat(generado).contains("Invoice").contains("F001-99");
        xml(generado).valueByXPath("/inv:Invoice/cbc:ID").isEqualTo("F001-99");
        xml(generado).nodesByXPath("/inv:Invoice/cbc:ID").hasSize(1);
    }
}
