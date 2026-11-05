package pe.abc.comercial.facturacion.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.abc.comercial.facturacion.dominio.DatosComprobante.Item;
import pe.abc.comercial.facturacion.dominio.Partes.Adquirente;
import pe.abc.comercial.facturacion.dominio.Partes.DocumentoIdentidad;
import pe.abc.comercial.facturacion.dominio.ReglaNegocioException.Violacion;
import pe.abc.comercial.facturacion.dominio.catalogo.Moneda;
import pe.abc.comercial.facturacion.dominio.catalogo.SistemaIsc;
import pe.abc.comercial.facturacion.dominio.catalogo.TipoAfectacionIgv;
import pe.abc.comercial.facturacion.dominio.catalogo.TipoComprobante;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static pe.abc.comercial.facturacion.dominio.Fabrica.CLIENTES_VARIOS;
import static pe.abc.comercial.facturacion.dominio.Fabrica.HOY;
import static pe.abc.comercial.facturacion.dominio.Fabrica.PARAMETROS;
import static pe.abc.comercial.facturacion.dominio.Fabrica.boleta;
import static pe.abc.comercial.facturacion.dominio.Fabrica.conIsc;
import static pe.abc.comercial.facturacion.dominio.Fabrica.credito;
import static pe.abc.comercial.facturacion.dominio.Fabrica.cuota;
import static pe.abc.comercial.facturacion.dominio.Fabrica.d;
import static pe.abc.comercial.facturacion.dominio.Fabrica.factura;
import static pe.abc.comercial.facturacion.dominio.Fabrica.gratuito;
import static pe.abc.comercial.facturacion.dominio.Fabrica.valor;
import static pe.abc.comercial.facturacion.dominio.catalogo.TipoAfectacionIgv.GRAVADO_BONIFICACION;
import static pe.abc.comercial.facturacion.dominio.catalogo.TipoAfectacionIgv.GRAVADO_ONEROSA;

/** Capa 2: reglas VAL-* del ValidadorNormativo (docs/04, Tablas 4.2 a 4.4). */
@DisplayName("Validador normativo (capa 2)")
class ValidadorNormativoTest {

    private final ValidadorNormativo validador = new ValidadorNormativo();
    private final MotorTributario motor = new MotorTributario();

    /** Ejecuta las dos fases de validación y devuelve las reglas violadas (vacío si es válido). */
    private List<Violacion> violaciones(DatosComprobante datos) {
        ReglaNegocioException e = catchThrowableOfType(ReglaNegocioException.class, () -> {
            validador.validarAntesDelCalculo(datos, HOY);
            validador.validarDespuesDelCalculo(datos, motor.calcular(datos, PARAMETROS), PARAMETROS);
        });
        return e == null ? List.of() : e.violaciones();
    }

    private List<String> reglas(DatosComprobante datos) {
        return violaciones(datos).stream().map(Violacion::regla).toList();
    }

    private static DatosComprobante cambiar(DatosComprobante b, Consumer<Object[]> cambio) {
        Object[] c = {b.tipo(), b.serie(), b.fechaEmision(), b.fechaVencimiento(), b.moneda(), b.tipoCambio(),
                b.adquirente(), b.items(), b.formaPago(), b.tipoOperacion()};
        cambio.accept(c);
        @SuppressWarnings("unchecked") List<Item> items = (List<Item>) c[7];
        return new DatosComprobante((TipoComprobante) c[0], (String) c[1], (LocalDate) c[2], b.horaEmision(),
                (LocalDate) c[3], (String) c[9], (Moneda) c[4], (java.math.BigDecimal) c[5], (Adquirente) c[6],
                items, (DatosComprobante.FormaPago) c[8], b.guiasRemision(), b.ordenCompra());
    }

    @Test
    void unaFacturaValidaNoTieneViolaciones() {
        assertThat(violaciones(factura(valor("10", "150.00", GRAVADO_ONEROSA)))).isEmpty();
    }

    @Test
    void reportaTodasLasViolacionesALaVez() {
        var datos = cambiar(factura(valor("0", "10", GRAVADO_ONEROSA)), c -> {
            c[1] = "B001";                                   // VAL-NUM-01
            c[3] = HOY.minusDays(1);                         // VAL-FEC-03
            c[4] = Moneda.USD;                               // VAL-MON-02
            c[8] = null;                                     // VAL-PAG-01
        });

        assertThat(reglas(datos)).contains("VAL-NUM-01", "VAL-FEC-03", "VAL-MON-02", "VAL-ITM-02", "VAL-PAG-01");
    }

    @Test
    void tc03_rucConDigitoVerificadorIncorrecto() {
        var datos = cambiar(factura(valor("1", "10", GRAVADO_ONEROSA)),
                c -> c[6] = new Adquirente(new DocumentoIdentidad("6", "20123456789"), "X", null, null));

        assertThat(violaciones(datos)).singleElement().satisfies(v -> {
            assertThat(v.regla()).isEqualTo("VAL-ID-02");
            assertThat(v.campo()).isEqualTo("/cliente/numeroDocumento");
        });
    }

    @Test
    void tc02_facturaDeVentaInternaAClienteConDni() {
        var datos = cambiar(factura(valor("1", "10", GRAVADO_ONEROSA)),
                c -> c[6] = new Adquirente(new DocumentoIdentidad("1", "45678912"), "PERSONA", null, null));

        assertThat(reglas(datos)).containsExactly("VAL-ID-03");
    }

    @Test
    void vat_id_06_dniDeOchoDigitos() {
        var datos = boleta(new Adquirente(new DocumentoIdentidad("1", "4567891"), "PERSONA", null, null),
                valor("1", "10", GRAVADO_ONEROSA));

        assertThat(reglas(datos)).containsExactly("VAL-ID-06");
    }

    @Test
    void tc04_boletaMayorA700SinDocumento() {
        // 750.00 con IGV incluido → 750.00 > 700.00
        var datos = boleta(CLIENTES_VARIOS, Fabrica.precio("1", "750.00", GRAVADO_ONEROSA));

        assertThat(reglas(datos)).containsExactly("VAL-BOL-01");
    }

    @Test
    void boletaHasta700PuedeSerSinDocumentoYMayorConDocumento() {
        assertThat(violaciones(boleta(CLIENTES_VARIOS, Fabrica.precio("1", "700.00", GRAVADO_ONEROSA)))).isEmpty();
        assertThat(violaciones(boleta(new Adquirente(new DocumentoIdentidad("1", "45678912"), "ANA", null, null),
                Fabrica.precio("1", "750.00", GRAVADO_ONEROSA)))).isEmpty();
    }

    @Test
    void tc05_serieQueNoCorrespondeAlTipo() {
        var datos = cambiar(boleta(CLIENTES_VARIOS, valor("1", "10", GRAVADO_ONEROSA)), c -> c[1] = "F002");

        assertThat(reglas(datos)).containsExactly("VAL-NUM-01");
    }

    @Test
    void vat_fec_01_noSeEmiteConFechaFutura() {
        var datos = cambiar(factura(valor("1", "10", GRAVADO_ONEROSA)), c -> c[2] = HOY.plusDays(1));

        assertThat(reglas(datos)).containsExactly("VAL-FEC-01");
    }

    @Test
    void tc06_bonificacionConValorUnitario() {
        Item bonificacion = new Item("P", "BONIFICACION", "NIU", d("1"), d("5.00"), null, d("5.00"),
                GRAVADO_BONIFICACION, null, null);

        assertThat(violaciones(factura(bonificacion))).singleElement()
                .satisfies(v -> assertThat(v).extracting(Violacion::regla, Violacion::campo)
                        .containsExactly("VAL-ITM-04", "/items/0"));
    }

    @Test
    void vat_itm_04_gratuitaSinValorReferencialYOnerosaSinValor() {
        assertThat(reglas(factura(gratuito("1", "0", GRAVADO_BONIFICACION)))).containsExactly("VAL-ITM-04");
        Item sinValor = new Item("P", "X", "NIU", d("1"), null, null, null, GRAVADO_ONEROSA, null, null);
        assertThat(reglas(factura(sinValor))).containsExactly("VAL-ITM-04");
    }

    @Test
    void vat_itm_05_exportacionSoloConOperacion02xx() {
        assertThat(reglas(factura(valor("1", "10", TipoAfectacionIgv.EXPORTACION)))).containsExactly("VAL-ITM-05");
    }

    @Test
    void vat_itm_01_almenosUnItem() {
        assertThat(reglas(cambiar(factura(valor("1", "1", GRAVADO_ONEROSA)), c -> c[7] = List.of())))
                .contains("VAL-ITM-01");
    }

    @Test
    void tc08_iscSistema01SinTasa() {
        assertThat(violaciones(factura(conIsc("1", "10", SistemaIsc.AL_VALOR, null, null, null)))).singleElement()
                .satisfies(v -> assertThat(v).extracting(Violacion::regla, Violacion::campo)
                        .containsExactly("VAL-TRB-03", "/items/0/isc"));
    }

    @ParameterizedTest
    @CsvSource({"ESPECIFICO", "PRECIO_VENTA_PUBLICO"})
    void vat_trb_03_iscSinSusDatos(SistemaIsc sistema) {
        assertThat(reglas(factura(conIsc("1", "10", sistema, null, null, null)))).containsExactly("VAL-TRB-03");
    }

    @Test
    void vat_trb_04_iscSoloEnItemsGravadosOnerosos() {
        Item exoneradoConIsc = new Item("P", "X", "NIU", d("1"), d("10"), null, null,
                TipoAfectacionIgv.EXONERADO_ONEROSA, new DatosComprobante.Isc(SistemaIsc.AL_VALOR, d("10"), null, null, null), null);

        assertThat(reglas(factura(exoneradoConIsc))).containsExactly("VAL-TRB-04");
    }

    @Test
    void tc10_creditoSinCuotas() {
        assertThat(reglas(credito(factura(valor("1", "100", GRAVADO_ONEROSA)), null))).containsExactly("VAL-PAG-02");
    }

    @Test
    void vat_pag_02_montoPendienteMayorAlTotal() {
        var datos = credito(factura(valor("1", "100", GRAVADO_ONEROSA)), "200.00", cuota(1, "200.00", HOY.plusDays(30)));

        assertThat(reglas(datos)).containsExactly("VAL-PAG-02");
    }

    @Test
    void vat_pag_03_lasCuotasSumanElPendiente() {
        var datos = credito(factura(valor("1", "100", GRAVADO_ONEROSA)), "118.00",
                cuota(1, "50.00", HOY.plusDays(30)), cuota(2, "50.00", HOY.plusDays(60)));

        assertThat(violaciones(datos)).singleElement()
                .satisfies(v -> assertThat(v.mensaje()).contains("100.00").contains("118.00"));
    }

    @Test
    void creditoSinMontoPendienteUsaElImporteTotal() {
        var datos = credito(factura(valor("1", "100", GRAVADO_ONEROSA)), null, cuota(1, "118.00", HOY.plusDays(30)));

        assertThatCode(() -> violaciones(datos)).doesNotThrowAnyException();
        assertThat(violaciones(datos)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void vat_fec_03_cuotasPosterioresALaEmisionYCrecientes(int dias) {
        var datos = credito(factura(valor("1", "100", GRAVADO_ONEROSA)), "118.00",
                cuota(1, "59.00", HOY.plusDays(dias)), cuota(2, "59.00", HOY.plusDays(30)));

        assertThat(reglas(datos)).containsExactly("VAL-FEC-03");
    }

    @Test
    void rucValidoConDigitoVerificador() {
        assertThat(Ruc.esValido("20123456786")).isTrue();
        assertThat(Ruc.esValido("20456789120")).isTrue();
        assertThat(Ruc.esValido("20123456789")).isFalse();     // TC-03
        assertThat(Ruc.esValido("30123456786")).isFalse();     // VAL-ID-01: prefijo
        assertThat(Ruc.esValido(null)).isFalse();
        assertThat(Ruc.digitoVerificador("20131312955")).isEqualTo(5);
    }
}
