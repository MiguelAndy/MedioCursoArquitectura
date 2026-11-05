package pe.abc.comercial.facturacion.dominio;

import pe.abc.comercial.facturacion.dominio.catalogo.Moneda;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Importe en letras para la leyenda 1000 (Catálogo N.° 52, VAL-TOT-06).
 * Ejemplo: 2667.98 PEN → "DOS MIL SEISCIENTOS SESENTA Y SIETE CON 98/100 SOLES".
 */
public final class MontoEnLetras {

    private static final long MAXIMO = 999_999_999_999L;

    private static final String[] HASTA_VEINTINUEVE = {
            "", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE",
            "DIECIOCHO", "DIECINUEVE", "VEINTE", "VEINTIUNO", "VEINTIDÓS", "VEINTITRÉS",
            "VEINTICUATRO", "VEINTICINCO", "VEINTISÉIS", "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] DECENAS = {
            "", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"};
    private static final String[] CENTENAS = {
            "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    private MontoEnLetras() {
    }

    public static String convertir(BigDecimal importe, Moneda moneda) {
        if (importe == null || moneda == null) {
            throw new IllegalArgumentException("El importe y la moneda son obligatorios");
        }
        if (importe.signum() < 0) {
            throw new IllegalArgumentException("El importe no puede ser negativo: " + importe);
        }
        BigDecimal redondeado = importe.setScale(2, RoundingMode.HALF_UP);
        BigDecimal entero = redondeado.setScale(0, RoundingMode.DOWN);
        if (entero.compareTo(BigDecimal.valueOf(MAXIMO)) > 0) {
            throw new IllegalArgumentException("Importe demasiado grande: " + importe);
        }
        int centimos = redondeado.subtract(entero).movePointRight(2).intValueExact();
        return enLetras(entero.longValueExact()) + " CON " + String.format("%02d", centimos) + "/100 "
                + moneda.nombreEnLetras();
    }

    /** 21 → VEINTIUNO, 100 → CIEN, 21000 → VEINTIÚN MIL, 1000000 → UN MILLÓN. */
    public static String enLetras(long numero) {
        if (numero < 0 || numero > MAXIMO) {
            throw new IllegalArgumentException("Número fuera de rango: " + numero);
        }
        return numero == 0 ? "CERO" : entero(numero, false);
    }

    /** {@code apocopar}: precede a MIL o MILLONES, por eso UNO pasa a UN (VEINTIÚN MIL). */
    private static String entero(long numero, boolean apocopar) {
        long millones = numero / 1_000_000;
        long miles = (numero % 1_000_000) / 1000;
        long unidades = numero % 1000;
        StringBuilder texto = new StringBuilder();
        if (millones == 1) {
            texto.append("UN MILLÓN");
        } else if (millones > 1) {
            texto.append(entero(millones, true)).append(" MILLONES");
        }
        if (miles == 1) {
            agregar(texto, "MIL");
        } else if (miles > 1) {
            agregar(texto, menorQueMil((int) miles, true) + " MIL");
        }
        if (unidades > 0) {
            agregar(texto, menorQueMil((int) unidades, apocopar));
        }
        return texto.toString();
    }

    private static String menorQueMil(int numero, boolean apocopar) {
        if (numero == 100) {
            return "CIEN";
        }
        StringBuilder texto = new StringBuilder(CENTENAS[numero / 100]);
        if (numero % 100 > 0) {
            agregar(texto, menorQueCien(numero % 100, apocopar));
        }
        return texto.toString();
    }

    private static String menorQueCien(int numero, boolean apocopar) {
        if (numero < 30) {
            if (apocopar && numero == 1) {
                return "UN";
            }
            return apocopar && numero == 21 ? "VEINTIÚN" : HASTA_VEINTINUEVE[numero];
        }
        int unidad = numero % 10;
        String decena = DECENAS[numero / 10];
        if (unidad == 0) {
            return decena;
        }
        return decena + " Y " + (apocopar && unidad == 1 ? "UN" : HASTA_VEINTINUEVE[unidad]);
    }

    private static void agregar(StringBuilder texto, String parte) {
        if (!texto.isEmpty()) {
            texto.append(' ');
        }
        texto.append(parte);
    }
}
