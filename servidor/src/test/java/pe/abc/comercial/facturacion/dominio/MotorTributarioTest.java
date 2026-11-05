package pe.abc.comercial.facturacion.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.abc.comercial.facturacion.dominio.ResultadoCalculo.ItemCalculado;
import pe.abc.comercial.facturacion.dominio.ResultadoCalculo.Leyenda;
import pe.abc.comercial.facturacion.dominio.ResultadoCalculo.Totales;
import pe.abc.comercial.facturacion.dominio.catalogo.SistemaIsc;
import pe.abc.comercial.facturacion.dominio.catalogo.TipoAfectacionIgv;
import pe.abc.comercial.facturacion.dominio.catalogo.Tributo;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.abc.comercial.facturacion.dominio.Fabrica.CLIENTES_VARIOS;
import static pe.abc.comercial.facturacion.dominio.Fabrica.PARAMETROS;
import static pe.abc.comercial.facturacion.dominio.Fabrica.boleta;
import static pe.abc.comercial.facturacion.dominio.Fabrica.bolsas;
import static pe.abc.comercial.facturacion.dominio.Fabrica.conIsc;
import static pe.abc.comercial.facturacion.dominio.Fabrica.factura;
import static pe.abc.comercial.facturacion.dominio.Fabrica.gratuito;
import static pe.abc.comercial.facturacion.dominio.Fabrica.precio;
import static pe.abc.comercial.facturacion.dominio.Fabrica.valor;
import static pe.abc.comercial.facturacion.dominio.catalogo.TipoAfectacionIgv.EXONERADO_ONEROSA;
import static pe.abc.comercial.facturacion.dominio.catalogo.TipoAfectacionIgv.GRAVADO_BONIFICACION;
import static pe.abc.comercial.facturacion.dominio.catalogo.TipoAfectacionIgv.GRAVADO_ONEROSA;
import static pe.abc.comercial.facturacion.dominio.catalogo.TipoAfectacionIgv.INAFECTO_ONEROSA;

/** Motor tributario (docs/04 § 4.3) verificado con la Tabla 4.5 y los casos TC-07 y TC-09. */
@DisplayName("Motor tributario")
class MotorTributarioTest {

    private final MotorTributario motor = new MotorTributario();

    private ResultadoCalculo calcular(DatosComprobante datos) {
        return motor.calcular(datos, PARAMETROS);
    }

    @Nested
    @DisplayName("Tabla 4.5: los tres ejemplos de factura y boleta")
    class Tabla45 {

        @Test
        void f001_1_mayoristaAlCreditoConBonificacion() {
            Totales t = calcular(factura(valor("10", "150.00", GRAVADO_ONEROSA), valor("5", "85.00", GRAVADO_ONEROSA),
                    valor("8", "42.00", GRAVADO_ONEROSA), gratuito("6", "5.00", GRAVADO_BONIFICACION))).totales();

            assertThat(t.operacionesGravadas()).isEqualByComparingTo("2261.00");
            assertThat(t.operacionesGratuitas()).isEqualByComparingTo("30.00");
            assertThat(t.tributosOperacionesGratuitas()).isEqualByComparingTo("5.40");
            assertThat(t.igv()).isEqualByComparingTo("406.98");
            assertThat(t.importeTotal()).isEqualByComparingTo("2667.98");
        }

        @Test
        void f001_2_iscSistema01() {
            Totales t = calcular(factura(conIsc("24", "2.00", SistemaIsc.AL_VALOR, "25.00", null, null),
                    valor("1", "85.00", GRAVADO_ONEROSA))).totales();

            assertThat(t.operacionesGravadas()).isEqualByComparingTo("133.00");
            assertThat(t.isc()).isEqualByComparingTo("12.00");
            assertThat(t.igv()).isEqualByComparingTo("26.10");
            assertThat(t.importeTotal()).isEqualByComparingTo("171.10");
        }

        @Test
        void b001_1_precioConIgvExoneradoYBolsas() {
            Totales t = calcular(boleta(CLIENTES_VARIOS, precio("6", "4.20", GRAVADO_ONEROSA),
                    precio("3", "2.50", EXONERADO_ONEROSA), bolsas(2, "0.10"))).totales();

            assertThat(t.operacionesGravadas()).isEqualByComparingTo("21.53");
            assertThat(t.operacionesExoneradas()).isEqualByComparingTo("7.50");
            assertThat(t.igv()).isEqualByComparingTo("3.87");
            assertThat(t.icbper()).isEqualByComparingTo("1.00");
            assertThat(t.importeTotal()).isEqualByComparingTo("33.90");
        }
    }

    @Nested
    @DisplayName("Ítems")
    class Items {

        @Test
        void tc07_iscAlValor25PorCiento() {
            ItemCalculado i = calcular(factura(conIsc("24", "2.00", SistemaIsc.AL_VALOR, "25.00", null, null)))
                    .items().get(0);

            assertThat(i.montoIsc()).isEqualByComparingTo("12.00");
            assertThat(i.baseImponibleIgv()).isEqualByComparingTo("60.00");   // VAL-TRB-05: la base incluye el ISC
            assertThat(i.montoIgv()).isEqualByComparingTo("10.80");
            assertThat(i.precioUnitario()).isEqualByComparingTo("2.95");     // (48 + 12 + 10.80) / 24
        }

        @Test
        void iscSistema02MontoFijoPorUnidadBase() {
            ItemCalculado i = calcular(factura(conIsc("12", "5.00", SistemaIsc.ESPECIFICO, null, "1.50", null)))
                    .items().get(0);

            assertThat(i.montoIsc()).isEqualByComparingTo("18.00");          // 12 × 1.50
            assertThat(i.montoIgv()).isEqualByComparingTo("14.04");          // (60 + 18) × 18 %
        }

        @Test
        void iscSistema03PrecioDeVentaAlPublico() {
            ItemCalculado i = calcular(factura(conIsc("10", "8.00", SistemaIsc.PRECIO_VENTA_PUBLICO, "30.00", null, "12.00")))
                    .items().get(0);

            assertThat(i.montoIsc()).isEqualByComparingTo("30.49");          // 10 × 12.00 × 0.847 × 30 % = 30.492
        }

        @Test
        void tc09_bolsasPaganIcbperFueraDeLaBaseDelIgv() {
            ItemCalculado i = calcular(boleta(CLIENTES_VARIOS, bolsas(2, "0.10"))).items().get(0);

            assertThat(i.montoIcbper()).isEqualByComparingTo("1.00");
            assertThat(i.tarifaIcbper()).isEqualByComparingTo("0.50");
            assertThat(i.valorVenta()).isEqualByComparingTo("0.17");
            assertThat(i.montoIgv()).isEqualByComparingTo("0.03");          // 0.20 − 0.17: el ICBPER no está en la base
            assertThat(i.totalTributos()).isEqualByComparingTo("1.03");
        }

        @Test
        void vat_trb_02_precioConIgvSeDespeja() {
            ItemCalculado i = calcular(boleta(CLIENTES_VARIOS, precio("6", "4.20", GRAVADO_ONEROSA))).items().get(0);

            assertThat(i.valorVenta()).isEqualByComparingTo("21.36");        // r2(25.20 / 1.18)
            assertThat(i.montoIgv()).isEqualByComparingTo("3.84");           // 25.20 − 21.36
            assertThat(i.valorUnitario()).hasToString("3.5593220339");       // r10(4.20 / 1.18)
        }

        @Test
        void vat_itm_04_gratuitaGravadaLlevaIgvReferencialYNoSumaAlTotal() {
            ResultadoCalculo r = calcular(factura(gratuito("6", "5.00", GRAVADO_BONIFICACION)));
            ItemCalculado i = r.items().get(0);

            assertThat(i.tributoIgv()).isEqualTo(Tributo.GRATUITO);
            assertThat(i.tipoPrecio()).isEqualTo("02");
            assertThat(i.valorUnitario()).isEqualByComparingTo("0.00");
            assertThat(i.montoIgv()).isEqualByComparingTo("5.40");
            assertThat(i.importeTotal()).isEqualByComparingTo("0.00");
            assertThat(r.totales().importeTotal()).isEqualByComparingTo("0.00");   // VAL-TRB-09
            assertThat(r.totales().totalTributos()).isEqualByComparingTo("0.00");
        }

        @Test
        void gratuitaInafectaNoLlevaIgvReferencial() {
            ItemCalculado i = calcular(factura(gratuito("3", "4.00", TipoAfectacionIgv.INAFECTO_RETIRO_MUESTRAS)))
                    .items().get(0);

            assertThat(i.montoIgv()).isEqualByComparingTo("0.00");
            assertThat(i.porcentajeIgv()).isEqualByComparingTo("0.00");
        }

        @ParameterizedTest(name = "{0} × {1} afectación {2} → valor {3}, IGV {4}")
        @CsvSource({"1, 0.25, 10, 0.25, 0.05",      // 0.045 → HALF_UP → 0.05
                "3, 33.333, 10, 100.00, 18.00",     // 99.999 → 100.00
                "2, 45.00, 20, 90.00, 0.00",
                "1, 350.00, 30, 350.00, 0.00"})
        void redondeaHalfUpPorItem(String cantidad, String valorUnitario, String afectacion, String vv, String igv) {
            ItemCalculado i = calcular(factura(valor(cantidad, valorUnitario,
                    TipoAfectacionIgv.desdeCodigo(afectacion).orElseThrow()))).items().get(0);

            assertThat(i.valorVenta()).hasToString(vv);
            assertThat(i.montoIgv()).hasToString(igv);
        }
    }

    @Nested
    @DisplayName("Totales y leyendas")
    class TotalesYLeyendas {

        @Test
        void vat_trb_08_elTotalEsLaSumaDeItemsRedondeados() {
            // Sobre el total sería 0.50 × 18 % = 0.09; por ítem: 0.05 + 0.05 = 0.10.
            Totales t = calcular(factura(valor("1", "0.25", GRAVADO_ONEROSA), valor("1", "0.25", GRAVADO_ONEROSA)))
                    .totales();

            assertThat(t.igv()).isEqualByComparingTo("0.10");
        }

        @Test
        void separaGravadasExoneradasEInafectas() {
            Totales t = calcular(factura(valor("1", "100", GRAVADO_ONEROSA), valor("1", "50", EXONERADO_ONEROSA),
                    valor("1", "30", INAFECTO_ONEROSA))).totales();

            assertThat(t.operacionesGravadas()).isEqualByComparingTo("100");
            assertThat(t.operacionesExoneradas()).isEqualByComparingTo("50");
            assertThat(t.operacionesInafectas()).isEqualByComparingTo("30");
            assertThat(t.totalValorVenta()).isEqualByComparingTo("180");      // VAL-TOT-01
            assertThat(t.totalTributos()).isEqualByComparingTo("18");         // VAL-TOT-02
            assertThat(t.totalPrecioVenta()).isEqualByComparingTo("198");     // VAL-TOT-03
            assertThat(t.importeTotal()).isEqualByComparingTo("198");         // VAL-TOT-04
        }

        @Test
        void vat_tot_06_leyenda1000SiempreY1002SiHayGratuitas() {
            ResultadoCalculo conGratuita = calcular(factura(valor("10", "150.00", GRAVADO_ONEROSA),
                    gratuito("6", "5.00", GRAVADO_BONIFICACION)));
            ResultadoCalculo sinGratuita = calcular(factura(valor("1", "100.00", GRAVADO_ONEROSA)));

            assertThat(conGratuita.leyendas()).extracting(Leyenda::codigo).containsExactly("1000", "1002");
            assertThat(conGratuita.leyendas().get(0).texto()).isEqualTo("SON: MIL SETECIENTOS SETENTA CON 00/100 SOLES");
            assertThat(sinGratuita.leyendas()).extracting(Leyenda::codigo).containsExactly("1000");
            assertThat(sinGratuita.leyendas().get(0).texto()).isEqualTo("SON: CIENTO DIECIOCHO CON 00/100 SOLES");
        }

        @Test
        void numeraLosItemsDesdeUno() {
            ResultadoCalculo r = calcular(factura(valor("1", "1", GRAVADO_ONEROSA), valor("1", "2", GRAVADO_ONEROSA)));

            assertThat(r.items()).extracting(ItemCalculado::numeroOrden).containsExactly(1, 2);
        }

        @Test
        void otraTasaDeIgvVieneDeLosParametros() {
            var parametros = new ParametrosTributarios(new BigDecimal("0.10"), PARAMETROS.tarifaIcbper(),
                    PARAMETROS.factorIscPvp(), PARAMETROS.umbralIdentificacionBoleta());

            ItemCalculado i = motor.calcular(factura(valor("1", "100.00", GRAVADO_ONEROSA)), parametros).items().get(0);

            assertThat(i.montoIgv()).isEqualByComparingTo("10.00");           // ADR-08: tasa como dato, no constante
            assertThat(i.porcentajeIgv()).isEqualByComparingTo("10.00");
        }
    }
}
