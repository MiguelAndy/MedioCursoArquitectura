package pe.abc.comercial;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import pe.abc.comercial.cpe.api.ComprobanteElectronico;
import pe.abc.comercial.cpe.api.DocumentoElectronicoApi;
import pe.abc.comercial.cpe.aplicacion.GenerarDocumentoElectronicoService;
import pe.abc.comercial.cpe.infraestructura.firma.AlmacenCertificado;
import pe.abc.comercial.cpe.infraestructura.firma.FirmadorXmlDsigAdapter;
import pe.abc.comercial.cpe.infraestructura.ubl.GeneradorUblAdapter;
import pe.abc.comercial.cpe.infraestructura.ubl.ValidadorDocumentoAdapter;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.SolicitudEmision;
import pe.abc.comercial.facturacion.aplicacion.servicio.EmitirComprobanteService;
import pe.abc.comercial.facturacion.dominio.ParametrosTributarios;
import pe.abc.comercial.facturacion.dominio.Partes.Domicilio;
import pe.abc.comercial.facturacion.dominio.Partes.Emisor;
import pe.abc.comercial.facturacion.infraestructura.cpe.DocumentoElectronicoCpeAdapter;
import pe.abc.comercial.facturacion.infraestructura.persistencia.AdaptadoresEnMemoria.ComprobanteRepositoryEnMemoria;
import pe.abc.comercial.facturacion.infraestructura.persistencia.AdaptadoresEnMemoria.NumeradorEnMemoria;
import pe.abc.comercial.facturacion.web.ValidadorContrato;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Ensambla el sistema real para las pruebas, sin Spring: dominio + aplicación + adaptadores de verdad
 * (JAXB, firma XMLDSig, validación XSD). Solo el reloj es fijo y el certificado es de prueba.
 */
public final class Ensamble {

    /** Raíz de los contratos de la fase 1 (el directorio de trabajo de Maven es servidor/). */
    public static final Path SPECS = Path.of("..", "specs");
    public static final Path EJEMPLOS_JSON = SPECS.resolve("ejemplos/json");
    public static final Path UBL = SPECS.resolve("ubl");

    public static final ZoneId LIMA = ZoneId.of("America/Lima");
    public static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-22T17:00:00Z"), LIMA);

    public static final Emisor EMISOR = new Emisor("20123456786", "ABC SOFTWARE CORP S.A.C.", "ABC ABARROTES",
            new Domicilio("JR. COMERCIO 123", "HUANUCO", "HUANUCO", "HUANUCO", "100101", "0000"));
    public static final ParametrosTributarios PARAMETROS = new ParametrosTributarios(new BigDecimal("0.18"),
            new BigDecimal("0.50"), new BigDecimal("0.847"), new BigDecimal("700.00"));

    public static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final AlmacenCertificado CERTIFICADO = AlmacenCertificado.dePrueba();

    /** Guarda cada documento que genera cpe (XML sin firma y firmado) para inspeccionarlo. */
    public static final class RegistroCpe implements DocumentoElectronicoApi {
        private final DocumentoElectronicoApi real;
        public final List<DocumentoGenerado> generados = new ArrayList<>();
        public final List<ComprobanteElectronico> recibidos = new ArrayList<>();

        RegistroCpe(DocumentoElectronicoApi real) {
            this.real = real;
        }

        @Override
        public DocumentoGenerado generar(ComprobanteElectronico comprobante) {
            recibidos.add(comprobante);
            DocumentoGenerado documento = real.generar(comprobante);
            generados.add(documento);
            return documento;
        }

        public DocumentoGenerado ultimo() {
            return generados.get(generados.size() - 1);
        }
    }

    public final RegistroCpe cpe = new RegistroCpe(cpeReal());
    public final EmitirComprobanteService servicio = new EmitirComprobanteService(new NumeradorEnMemoria(),
            new ComprobanteRepositoryEnMemoria(), new DocumentoElectronicoCpeAdapter(cpe),
            fecha -> PARAMETROS, () -> EMISOR, RELOJ);
    private final ValidadorContrato contrato = new ValidadorContrato();

    public static GenerarDocumentoElectronicoService cpeReal() {
        return new GenerarDocumentoElectronicoService(new GeneradorUblAdapter(),
                new FirmadorXmlDsigAdapter(CERTIFICADO.llave(), CERTIFICADO.certificado()),
                new ValidadorDocumentoAdapter());
    }

    public static java.security.PrivateKey llave() {
        return CERTIFICADO.llave();
    }

    public static java.security.cert.X509Certificate certificado() {
        return CERTIFICADO.certificado();
    }

    /** Lee una solicitud de specs/ejemplos/json, la valida con su JSON Schema y la convierte en comando. */
    public SolicitudEmision solicitud(String archivo) {
        return solicitud(leerJson(EJEMPLOS_JSON.resolve(archivo)));
    }

    public SolicitudEmision solicitud(JsonNode cuerpo) {
        contrato.validar(cuerpo);
        try {
            return JSON.treeToValue(cuerpo, SolicitudEmision.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static JsonNode leerJson(Path archivo) {
        try {
            return JSON.readTree(Files.readString(archivo));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String leer(Path archivo) {
        try {
            return Files.readString(archivo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
