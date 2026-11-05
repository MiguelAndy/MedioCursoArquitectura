package pe.abc.comercial.cpe.infraestructura.ubl;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.AddressLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.AddressType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.AttachmentType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.CountryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.CustomerPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.DocumentReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.ExternalReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.InvoiceLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.ItemIdentificationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.ItemType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.MonetaryTotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.OrderReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyIdentificationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyLegalEntityType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyNameType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PaymentTermsType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PriceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PricingReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.SignatureType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxCategoryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxSubtotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxTotalType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.AddressTypeCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.BaseUnitMeasureType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.CityNameType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.CountrySubentityType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.CustomizationIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.DescriptionType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.DistrictType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.DocumentCurrencyCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.DocumentTypeCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.DueDateType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.IDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.IdentificationCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.InvoiceTypeCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.InvoicedQuantityType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.IssueDateType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.IssueTimeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.LineCountNumericType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.LineExtensionAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.LineType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.NameType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.NoteType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PayableAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PaymentDueDateType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PaymentMeansIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PerUnitAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PercentType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PriceAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PriceTypeCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.RegistrationNameType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxExemptionReasonCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxInclusiveAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxTypeCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxableAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TierRangeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.UBLVersionIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.URIType;
import oasis.names.specification.ubl.schema.xsd.commonextensioncomponents_2.ExtensionContentType;
import oasis.names.specification.ubl.schema.xsd.commonextensioncomponents_2.UBLExtensionType;
import oasis.names.specification.ubl.schema.xsd.commonextensioncomponents_2.UBLExtensionsType;
import oasis.names.specification.ubl.schema.xsd.invoice_2.InvoiceType;
import oasis.names.specification.ubl.schema.xsd.unqualifieddatatypes_2.DateType;
import pe.abc.comercial.cpe.api.ComprobanteElectronico;
import pe.abc.comercial.cpe.api.ComprobanteElectronico.Domicilio;
import pe.abc.comercial.cpe.api.ComprobanteElectronico.Linea;
import pe.abc.comercial.cpe.api.ComprobanteElectronico.Parte;
import pe.abc.comercial.cpe.api.ComprobanteElectronico.Subtotal;
import un.unece.uncefact.data.specification.corecomponenttypeschemamodule._2.AmountType;
import un.unece.uncefact.data.specification.corecomponenttypeschemamodule._2.CodeType;
import un.unece.uncefact.data.specification.corecomponenttypeschemamodule._2.IdentifierType;
import un.unece.uncefact.data.specification.corecomponenttypeschemamodule._2.NumericType;
import un.unece.uncefact.data.specification.corecomponenttypeschemamodule._2.TextType;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.Supplier;

/**
 * Traduce {@link ComprobanteElectronico} a {@link InvoiceType}, cuyas clases se <b>generan desde
 * specs/ubl/xsd-oasis-ubl-2.1/maindoc/UBL-Invoice-2.1.xsd</b>. Los atributos de catálogo siguen el perfil SUNAT
 * y la matriz de trazabilidad de docs/08 (TR-A*).
 */
public final class InvoiceUblMapper {

    public static final String ID_FIRMA = "IDSignABC";
    public static final String URI_FIRMA = "#SignatureABC";

    private static final String SUNAT = "PE:SUNAT";
    private static final String UNECE = "United Nations Economic Commission for Europe";
    private static final String CATALOGO = "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo";

    private final DatatypeFactory fechas;

    public InvoiceUblMapper() {
        try {
            this.fechas = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    public InvoiceType mapear(ComprobanteElectronico c) {
        String moneda = c.moneda();
        InvoiceType invoice = new InvoiceType();

        // Contenedor de la firma (Anexo 9-A, campo 9): se llena al firmar.
        UBLExtensionType extension = new UBLExtensionType();
        extension.setExtensionContent(new ExtensionContentType());
        UBLExtensionsType extensiones = new UBLExtensionsType();
        extensiones.getUBLExtension().add(extension);
        invoice.setUBLExtensions(extensiones);

        invoice.setUBLVersionID(id(UBLVersionIDType::new, "2.1"));
        CustomizationIDType personalizacion = id(CustomizationIDType::new, "2.0");
        personalizacion.setSchemeAgencyName(SUNAT);
        invoice.setCustomizationID(personalizacion);
        invoice.setID(id(IDType::new, c.numero()));
        invoice.setIssueDate(fecha(IssueDateType::new, c.fechaEmision()));
        invoice.setIssueTime(hora(c.horaEmision()));
        if (c.fechaVencimiento() != null) {
            invoice.setDueDate(fecha(DueDateType::new, c.fechaVencimiento()));
        }
        invoice.setInvoiceTypeCode(tipoDocumento(c));
        c.leyendas().forEach(l -> {
            NoteType nota = texto(NoteType::new, l.texto());
            nota.setLanguageLocaleID(l.codigo());
            invoice.getNote().add(nota);
        });
        DocumentCurrencyCodeType divisa = codigo(DocumentCurrencyCodeType::new, moneda);
        divisa.setListID("ISO 4217 Alpha");
        divisa.setListName("Currency");
        divisa.setListAgencyName(UNECE);
        invoice.setDocumentCurrencyCode(divisa);
        invoice.setLineCountNumeric(numero(LineCountNumericType::new, BigDecimal.valueOf(c.lineas().size())));

        if (c.ordenCompra() != null) {
            OrderReferenceType orden = new OrderReferenceType();
            orden.setID(id(IDType::new, c.ordenCompra()));
            invoice.setOrderReference(orden);
        }
        c.guiasRemision().forEach(g -> invoice.getDespatchDocumentReference().add(guia(g)));
        invoice.getSignature().add(firma(c.emisor()));

        SupplierPartyType emisor = new SupplierPartyType();
        emisor.setParty(parteEmisor(c.emisor()));
        invoice.setAccountingSupplierParty(emisor);
        CustomerPartyType adquirente = new CustomerPartyType();
        adquirente.setParty(parte(c.adquirente()));
        invoice.setAccountingCustomerParty(adquirente);

        if (c.formaPago() != null) {
            terminosDePago(c.formaPago(), moneda, invoice);
        }
        invoice.getTaxTotal().add(totalImpuestos(c.totalImpuestos(), c.impuestos(), moneda));

        MonetaryTotalType totales = new MonetaryTotalType();
        totales.setLineExtensionAmount(importe(LineExtensionAmountType::new, c.totalValorVenta(), moneda));
        totales.setTaxInclusiveAmount(importe(TaxInclusiveAmountType::new, c.totalPrecioVenta(), moneda));
        totales.setPayableAmount(importe(PayableAmountType::new, c.importeTotal(), moneda));
        invoice.setLegalMonetaryTotal(totales);

        c.lineas().forEach(l -> invoice.getInvoiceLine().add(linea(l, moneda)));
        return invoice;
    }

    // ---------------------------------------------------------------------------------------
    // Cabecera
    // ---------------------------------------------------------------------------------------

    private InvoiceTypeCodeType tipoDocumento(ComprobanteElectronico c) {
        InvoiceTypeCodeType tipo = codigo(InvoiceTypeCodeType::new, c.tipoDocumento());
        tipo.setListAgencyName(SUNAT);
        tipo.setListName("Tipo de Documento");
        tipo.setListURI(CATALOGO + "01");
        tipo.setListID(c.tipoOperacion());
        tipo.setName("Tipo de Operacion");
        tipo.setListSchemeURI(CATALOGO + "51");
        return tipo;
    }

    private DocumentReferenceType guia(ComprobanteElectronico.Referencia guia) {
        DocumentReferenceType referencia = new DocumentReferenceType();
        referencia.setID(id(IDType::new, guia.numero()));
        DocumentTypeCodeType tipo = codigo(DocumentTypeCodeType::new, guia.tipo());
        tipo.setListAgencyName(SUNAT);
        tipo.setListName("Tipo de Documento");
        tipo.setListURI(CATALOGO + "01");
        referencia.setDocumentTypeCode(tipo);
        return referencia;
    }

    /** {@code cac:Signature}: referencia a la firma que va en ext:UBLExtensions. */
    private SignatureType firma(ComprobanteElectronico.Emisor e) {
        PartyType firmante = new PartyType();
        PartyIdentificationType identificacion = new PartyIdentificationType();
        identificacion.setID(id(IDType::new, e.ruc()));
        firmante.getPartyIdentification().add(identificacion);
        PartyNameType nombre = new PartyNameType();
        nombre.setName(texto(NameType::new, e.razonSocial()));
        firmante.getPartyName().add(nombre);

        ExternalReferenceType externa = new ExternalReferenceType();
        externa.setURI(id(URIType::new, URI_FIRMA));
        AttachmentType adjunto = new AttachmentType();
        adjunto.setExternalReference(externa);

        SignatureType firma = new SignatureType();
        firma.setID(id(IDType::new, ID_FIRMA));
        firma.setSignatoryParty(firmante);
        firma.setDigitalSignatureAttachment(adjunto);
        return firma;
    }

    // ---------------------------------------------------------------------------------------
    // Partes
    // ---------------------------------------------------------------------------------------

    private PartyType parteEmisor(ComprobanteElectronico.Emisor e) {
        PartyType parte = identificada("6", e.ruc());
        PartyNameType nombre = new PartyNameType();
        nombre.setName(texto(NameType::new, e.nombreComercial()));
        parte.getPartyName().add(nombre);
        parte.getPartyLegalEntity().add(entidadLegal(e.razonSocial(), e.domicilio()));
        return parte;
    }

    private PartyType parte(Parte p) {
        PartyType parte = identificada(p.tipoDocumento(), p.numeroDocumento());
        parte.getPartyLegalEntity().add(entidadLegal(p.razonSocial(), p.direccion()));
        return parte;
    }

    private PartyType identificada(String tipoDocumento, String numero) {
        IDType documento = id(IDType::new, numero);
        documento.setSchemeID(tipoDocumento);
        documento.setSchemeName("Documento de Identidad");
        documento.setSchemeAgencyName(SUNAT);
        documento.setSchemeURI(CATALOGO + "06");
        PartyIdentificationType identificacion = new PartyIdentificationType();
        identificacion.setID(documento);
        PartyType parte = new PartyType();
        parte.getPartyIdentification().add(identificacion);
        return parte;
    }

    private PartyLegalEntityType entidadLegal(String razonSocial, Domicilio domicilio) {
        PartyLegalEntityType entidad = new PartyLegalEntityType();
        entidad.setRegistrationName(texto(RegistrationNameType::new, razonSocial));
        if (domicilio != null) {
            entidad.setRegistrationAddress(direccion(domicilio));
        }
        return entidad;
    }

    private AddressType direccion(Domicilio d) {
        AddressType direccion = new AddressType();
        if (d.ubigeo() != null) {
            IDType ubigeo = id(IDType::new, d.ubigeo());
            ubigeo.setSchemeName("Ubigeos");
            ubigeo.setSchemeAgencyName("PE:INEI");
            direccion.setID(ubigeo);
        }
        if (d.codigoEstablecimiento() != null) {
            AddressTypeCodeType establecimiento = codigo(AddressTypeCodeType::new, d.codigoEstablecimiento());
            establecimiento.setListAgencyName(SUNAT);
            establecimiento.setListName("Establecimientos anexos");
            direccion.setAddressTypeCode(establecimiento);
        }
        if (d.provincia() != null) {
            direccion.setCityName(texto(CityNameType::new, d.provincia()));
        }
        if (d.departamento() != null) {
            direccion.setCountrySubentity(texto(CountrySubentityType::new, d.departamento()));
        }
        if (d.distrito() != null) {
            direccion.setDistrict(texto(DistrictType::new, d.distrito()));
        }
        AddressLineType linea = new AddressLineType();
        linea.setLine(texto(LineType::new, d.direccion()));
        direccion.getAddressLine().add(linea);
        IdentificationCodeType pais = codigo(IdentificationCodeType::new, "PE");
        pais.setListID("ISO 3166-1");
        pais.setListAgencyName(UNECE);
        pais.setListName("Country");
        CountryType country = new CountryType();
        country.setIdentificationCode(pais);
        direccion.setCountry(country);
        return direccion;
    }

    // ---------------------------------------------------------------------------------------
    // Pago, impuestos y líneas
    // ---------------------------------------------------------------------------------------

    private void terminosDePago(ComprobanteElectronico.FormaPago f, String moneda, InvoiceType invoice) {
        PaymentTermsType principal = termino(f.medio());
        if (f.montoPendiente() != null) {
            principal.setAmount(importe(oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.AmountType::new,
                    f.montoPendiente(), moneda));
        }
        invoice.getPaymentTerms().add(principal);
        for (int i = 0; i < f.cuotas().size(); i++) {
            var cuota = f.cuotas().get(i);
            PaymentTermsType termino = termino(String.format("Cuota%03d", i + 1));
            termino.setAmount(importe(oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.AmountType::new,
                    cuota.monto(), moneda));
            termino.setPaymentDueDate(fecha(PaymentDueDateType::new, cuota.fechaVencimiento()));
            invoice.getPaymentTerms().add(termino);
        }
    }

    private PaymentTermsType termino(String medio) {
        PaymentTermsType termino = new PaymentTermsType();
        termino.setID(id(IDType::new, "FormaPago"));
        termino.getPaymentMeansID().add(id(PaymentMeansIDType::new, medio));
        return termino;
    }

    private TaxTotalType totalImpuestos(BigDecimal total, java.util.List<Subtotal> subtotales, String moneda) {
        TaxTotalType impuestos = new TaxTotalType();
        impuestos.setTaxAmount(importe(TaxAmountType::new, total, moneda));
        subtotales.forEach(s -> impuestos.getTaxSubtotal().add(subtotal(s, moneda)));
        return impuestos;
    }

    private TaxSubtotalType subtotal(Subtotal s, String moneda) {
        TaxSubtotalType subtotal = new TaxSubtotalType();
        if (s.base() != null) {
            subtotal.setTaxableAmount(importe(TaxableAmountType::new, s.base(), moneda));
        }
        subtotal.setTaxAmount(importe(TaxAmountType::new, s.monto(), moneda));
        if (s.unidadesBase() != null) {
            BaseUnitMeasureType unidades = new BaseUnitMeasureType();
            unidades.setValue(s.unidadesBase());
            unidades.setUnitCode("NIU");
            subtotal.setBaseUnitMeasure(unidades);
        }

        TaxCategoryType categoria = new TaxCategoryType();
        if (s.porcentaje() != null) {
            categoria.setPercent(numero(PercentType::new, s.porcentaje()));
        }
        if (s.montoPorUnidad() != null) {
            categoria.setPerUnitAmount(importe(PerUnitAmountType::new, s.montoPorUnidad(), moneda));
        }
        if (s.afectacionIgv() != null) {
            TaxExemptionReasonCodeType afectacion = codigo(TaxExemptionReasonCodeType::new, s.afectacionIgv());
            afectacion.setListAgencyName(SUNAT);
            afectacion.setListName("Afectacion del IGV");
            afectacion.setListURI(CATALOGO + "07");
            categoria.setTaxExemptionReasonCode(afectacion);
        }
        if (s.sistemaIsc() != null) {
            categoria.setTierRange(texto(TierRangeType::new, s.sistemaIsc()));
        }
        IDType idTributo = id(IDType::new, s.tributo().codigo());
        idTributo.setSchemeName("Codigo de tributos");
        idTributo.setSchemeAgencyName(SUNAT);
        idTributo.setSchemeURI(CATALOGO + "05");
        TaxSchemeType esquema = new TaxSchemeType();
        esquema.setID(idTributo);
        esquema.setName(texto(NameType::new, s.tributo().nombre()));
        esquema.setTaxTypeCode(codigo(TaxTypeCodeType::new, s.tributo().codigoInternacional()));
        categoria.setTaxScheme(esquema);
        subtotal.setTaxCategory(categoria);
        return subtotal;
    }

    private InvoiceLineType linea(Linea l, String moneda) {
        InvoiceLineType linea = new InvoiceLineType();
        linea.setID(id(IDType::new, String.valueOf(l.numero())));
        InvoicedQuantityType cantidad = new InvoicedQuantityType();
        cantidad.setValue(l.cantidad());
        cantidad.setUnitCode(l.unidadMedida());
        cantidad.setUnitCodeListID("UN/ECE rec 20");
        cantidad.setUnitCodeListAgencyName(UNECE);
        linea.setInvoicedQuantity(cantidad);
        linea.setLineExtensionAmount(importe(LineExtensionAmountType::new, l.valorVenta(), moneda));

        PriceType alternativo = new PriceType();
        alternativo.setPriceAmount(importe(PriceAmountType::new, unitario(l.precioReferencia()), moneda));
        PriceTypeCodeType tipoPrecio = codigo(PriceTypeCodeType::new, l.tipoPrecio());
        tipoPrecio.setListName("Tipo de Precio");
        tipoPrecio.setListAgencyName(SUNAT);
        tipoPrecio.setListURI(CATALOGO + "16");
        alternativo.setPriceTypeCode(tipoPrecio);
        PricingReferenceType referencia = new PricingReferenceType();
        referencia.getAlternativeConditionPrice().add(alternativo);
        linea.setPricingReference(referencia);

        linea.getTaxTotal().add(totalImpuestos(l.totalImpuestos(), l.impuestos(), moneda));

        ItemType item = new ItemType();
        item.getDescription().add(texto(DescriptionType::new, l.descripcion()));
        if (l.codigoProducto() != null) {
            ItemIdentificationType codigo = new ItemIdentificationType();
            codigo.setID(id(IDType::new, l.codigoProducto()));
            item.setSellersItemIdentification(codigo);
        }
        linea.setItem(item);

        PriceType precio = new PriceType();
        precio.setPriceAmount(importe(PriceAmountType::new, unitario(l.valorUnitario()), moneda));
        linea.setPrice(precio);
        return linea;
    }

    // ---------------------------------------------------------------------------------------
    // Tipos de datos básicos (CCTS)
    // ---------------------------------------------------------------------------------------

    /** n(12,10) sin ceros sobrantes y con al menos 2 decimales: 177.0000000000 → 177.00. */
    static BigDecimal unitario(BigDecimal valor) {
        BigDecimal sinCeros = valor.stripTrailingZeros();
        return sinCeros.scale() < 2 ? valor.setScale(2, RoundingMode.HALF_UP) : sinCeros;
    }

    private static <T extends IdentifierType> T id(Supplier<T> f, String valor) {
        T t = f.get();
        t.setValue(valor);
        return t;
    }

    private static <T extends CodeType> T codigo(Supplier<T> f, String valor) {
        T t = f.get();
        t.setValue(valor);
        return t;
    }

    private static <T extends TextType> T texto(Supplier<T> f, String valor) {
        T t = f.get();
        t.setValue(valor);
        return t;
    }

    private static <T extends NumericType> T numero(Supplier<T> f, BigDecimal valor) {
        T t = f.get();
        t.setValue(valor);
        return t;
    }

    private static <T extends AmountType> T importe(Supplier<T> f, BigDecimal valor, String moneda) {
        T t = f.get();
        t.setValue(valor);
        t.setCurrencyID(moneda);
        return t;
    }

    private <T extends DateType> T fecha(Supplier<T> f, LocalDate fecha) {
        T t = f.get();
        t.setValue(fechas.newXMLGregorianCalendarDate(fecha.getYear(), fecha.getMonthValue(), fecha.getDayOfMonth(),
                DatatypeConstants.FIELD_UNDEFINED));
        return t;
    }

    private IssueTimeType hora(LocalTime hora) {
        IssueTimeType t = new IssueTimeType();
        t.setValue(fechas.newXMLGregorianCalendarTime(hora.getHour(), hora.getMinute(), hora.getSecond(),
                DatatypeConstants.FIELD_UNDEFINED));
        return t;
    }
}
