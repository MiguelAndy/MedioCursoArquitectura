package pe.abc.comercial.facturacion.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SpecVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import pe.abc.comercial.Ensamble;
import pe.abc.comercial.cpe.infraestructura.ubl.ValidadorDocumentoAdapter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API REST contra el contrato {@code specs/openapi/abc-comercial-api.yaml}: casos de prueba TC-* de docs/04 § 4.6,
 * contraejemplos de specs/ejemplos/json/invalidos y validación de la respuesta con su JSON Schema.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API de comprobantes")
class ComprobanteApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    private ResultActions emitir(JsonNode cuerpo, String clave) throws Exception {
        return mvc.perform(post("/api/v1/comprobantes").header("Idempotency-Key", clave)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo)));
    }

    private ResultActions emitir(String ejemplo) throws Exception {
        return emitir(ejemplo(ejemplo), UUID.randomUUID().toString());
    }

    private static ObjectNode ejemplo(String archivo) {
        return (ObjectNode) Ensamble.leerJson(Ensamble.EJEMPLOS_JSON.resolve(archivo));
    }

    private JsonNode cuerpo(MvcResult resultado) throws Exception {
        return json.readTree(resultado.getResponse().getContentAsByteArray());
    }

    @Nested
    @DisplayName("POST /comprobantes")
    class Emitir {

        @Test
        void tc01_facturaAlCreditoConBonificacion() throws Exception {
            emitir("01-emitir-factura-credito.request.json")
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", startsWith("/api/v1/comprobantes/")))
                    .andExpect(jsonPath("$.numero", matchesPattern("F001-\\d+")))
                    .andExpect(jsonPath("$.estado").value("EMITIDO"))
                    .andExpect(jsonPath("$.totales.operacionesGravadas").value("2261.00"))
                    .andExpect(jsonPath("$.totales.igv").value("406.98"))
                    .andExpect(jsonPath("$.totales.operacionesGratuitas").value("30.00"))
                    .andExpect(jsonPath("$.totales.importeTotal").value("2667.98"))
                    .andExpect(jsonPath("$.leyendas[*].codigo").value(org.hamcrest.Matchers.contains("1000", "1002")))
                    .andExpect(jsonPath("$.items[3].tipoAfectacionIgv").value("15"))
                    .andExpect(jsonPath("$.items[3].codigoTributoIgv").value("9996"))
                    .andExpect(jsonPath("$.documentoElectronico.hash").isNotEmpty())
                    .andExpect(jsonPath("$.documentoElectronico.codigoQr", startsWith("20123456786|01|F001|")));
        }

        @Test
        void laRespuestaCumpleSuJsonSchema() throws Exception {
            JsonNode respuesta = cuerpo(emitir("02-emitir-boleta-icbper.request.json").andReturn());
            var esquema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(SchemaLocation.of("classpath:schemas/comprobante.response.schema.json"));

            assertThat(esquema.validate(respuesta)).isEmpty();
        }

        @Test
        void tc12_idempotencia() throws Exception {
            String clave = UUID.randomUUID().toString();
            ObjectNode solicitud = ejemplo("03-emitir-factura-isc.request.json");

            String primero = cuerpo(emitir(solicitud, clave).andExpect(status().isCreated()).andReturn()).get("id").asText();
            String segundo = cuerpo(emitir(solicitud, clave).andExpect(status().isOk()).andReturn()).get("id").asText();
            assertThat(segundo).isEqualTo(primero);   // un solo comprobante

            emitir(solicitud.put("ordenCompra", "OC-OTRA"), clave)
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }

        @Test
        void sinIdempotencyKeyResponde400() throws Exception {
            mvc.perform(post("/api/v1/comprobantes").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(ejemplo("03-emitir-factura-isc.request.json"))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Capa 1: contrato JSON Schema → 400")
    class Contrato {

        @ParameterizedTest(name = "{0} → 400 en {1}")
        @CsvSource({"i01-boleta-con-serie-de-factura.json, /serie",
                "i02-factura-a-cliente-con-dni.json, /cliente/tipoDocumento",
                "i03-gratuita-con-valor-unitario.json, /items/0",
                "i04-nota-credito-sin-referencia.json, /documentoReferencia",
                "i05-credito-sin-cuotas.json, /formaPago",
                "i06-isc-al-valor-sin-tasa.json, /items/0/isc",
                "i07-moneda-extranjera-sin-tipo-cambio.json, /tipoCambio"})
        void losContraejemplosDeLaFase1SeRechazan(String archivo, String campo) throws Exception {
            emitir(ejemplo("invalidos/" + archivo), UUID.randomUUID().toString())
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.errores[*].regla", hasItem("VAL-EST-01")))
                    .andExpect(jsonPath("$.errores[*].campo", hasItem(startsWith(campo))));
        }
    }

    @Nested
    @DisplayName("Capa 2: reglas de negocio → 422")
    class Negocio {

        @Test
        void tc03_rucConDigitoVerificadorIncorrecto() throws Exception {
            ObjectNode solicitud = ejemplo("03-emitir-factura-isc.request.json");
            ((ObjectNode) solicitud.get("cliente")).put("numeroDocumento", "20123456789");

            emitir(solicitud, UUID.randomUUID().toString())
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.type").value("/problemas/validacion-negocio"))
                    .andExpect(jsonPath("$.errores[0].regla").value("VAL-ID-02"))
                    .andExpect(jsonPath("$.errores[0].campo").value("/cliente/numeroDocumento"));
        }

        @Test
        void tc04_boletaMayorA700SinDocumento() throws Exception {
            ObjectNode solicitud = ejemplo("02-emitir-boleta-icbper.request.json");
            ((ObjectNode) solicitud.get("items").get(0)).put("precioUnitario", "130.00");   // 6 × 130 = 780

            emitir(solicitud, UUID.randomUUID().toString())
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errores[0].regla").value("VAL-BOL-01"));
        }

        @Test
        void vat_alc_01_loQueLaFase2NoImplementaSeRechazaExplicitamente() throws Exception {
            ObjectNode solicitud = ejemplo("03-emitir-factura-isc.request.json").put("tipoOperacion", "0112");

            emitir(solicitud, UUID.randomUUID().toString())
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errores[0].regla").value("VAL-ALC-01"))
                    .andExpect(jsonPath("$.errores[0].campo").value("/tipoOperacion"));
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        void obtenerYDescargarElXmlFirmado() throws Exception {
            JsonNode emitido = cuerpo(emitir("01-emitir-factura-credito.request.json").andReturn());
            String self = emitido.get("enlaces").get("self").asText();

            mvc.perform(get(self)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(emitido.get("id").asText()))
                    .andExpect(jsonPath("$.cliente.razonSocial").value("DISTRIBUIDORA SANTA ROSA DEL HUALLAGA E.I.R.L."));

            MvcResult xml = mvc.perform(get(emitido.get("enlaces").get("xml").asText()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                    .andExpect(header().string("Content-Disposition",
                            "attachment; filename=\"" + emitido.get("documentoElectronico").get("nombreArchivo").asText() + ".xml\""))
                    .andReturn();
            byte[] contenido = xml.getResponse().getContentAsByteArray();
            assertThat(new ValidadorDocumentoAdapter().validar(contenido)).isEmpty();
            assertThat(new String(contenido, java.nio.charset.StandardCharsets.UTF_8))
                    .contains("<ds:DigestValue>" + emitido.get("documentoElectronico").get("hash").asText());
        }

        @Test
        void comprobanteInexistente() throws Exception {
            mvc.perform(get("/api/v1/comprobantes/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }

        @Test
        void calcularSinEmitir() throws Exception {
            mvc.perform(post("/api/v1/comprobantes/calculos").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(ejemplo("02-emitir-boleta-icbper.request.json"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totales.importeTotal").value("33.90"))
                    .andExpect(jsonPath("$.totales.icbper").value("1.00"))
                    .andExpect(jsonPath("$.items[2].montoIcbper").value("1.00"))
                    .andExpect(jsonPath("$.leyendas[0].texto").value("SON: TREINTA Y TRES CON 90/100 SOLES"));
        }
    }
}
