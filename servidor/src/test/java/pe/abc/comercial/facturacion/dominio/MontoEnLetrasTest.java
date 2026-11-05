package pe.abc.comercial.facturacion.dominio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.abc.comercial.facturacion.dominio.catalogo.Moneda;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Leyenda 1000: importe en letras (VAL-TOT-06). */
class MontoEnLetrasTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', textBlock = """
            0          | CERO
            1          | UNO
            16         | DIECISÉIS
            21         | VEINTIUNO
            31         | TREINTA Y UNO
            100        | CIEN
            101        | CIENTO UNO
            171        | CIENTO SETENTA Y UNO
            1000       | MIL
            2667       | DOS MIL SEISCIENTOS SESENTA Y SIETE
            21000      | VEINTIÚN MIL
            31000      | TREINTA Y UN MIL
            100000     | CIEN MIL
            1000000    | UN MILLÓN
            2500000    | DOS MILLONES QUINIENTOS MIL
            21000000   | VEINTIÚN MILLONES
            1000000000 | MIL MILLONES
            """)
    void convierteEnteros(long numero, String esperado) {
        assertThat(MontoEnLetras.enLetras(numero)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "{0} {1} → {2}")
    @CsvSource(delimiter = '|', textBlock = """
            2667.98 | PEN | DOS MIL SEISCIENTOS SESENTA Y SIETE CON 98/100 SOLES
            171.10  | PEN | CIENTO SETENTA Y UNO CON 10/100 SOLES
            33.90   | PEN | TREINTA Y TRES CON 90/100 SOLES
            0.99    | USD | CERO CON 99/100 DÓLARES AMERICANOS
            10.005  | EUR | DIEZ CON 01/100 EUROS
            """)
    void leyendasDeLosEjemplos(String importe, Moneda moneda, String esperado) {
        assertThat(MontoEnLetras.convertir(new BigDecimal(importe), moneda)).isEqualTo(esperado);
    }

    @Test
    void rechazaValoresInvalidos() {
        assertThatThrownBy(() -> MontoEnLetras.convertir(new BigDecimal("-1"), Moneda.PEN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MontoEnLetras.convertir(null, Moneda.PEN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MontoEnLetras.convertir(new BigDecimal("1000000000000"), Moneda.PEN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MontoEnLetras.enLetras(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
