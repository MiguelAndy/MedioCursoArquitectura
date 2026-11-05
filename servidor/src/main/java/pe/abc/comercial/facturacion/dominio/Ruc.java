package pe.abc.comercial.facturacion.dominio;

import java.util.regex.Pattern;

/** Reglas del RUC: VAL-ID-01 (formato y prefijo) y VAL-ID-02 (dígito verificador módulo 11). */
public final class Ruc {

    private static final Pattern FORMATO = Pattern.compile("^(10|15|16|17|20)\\d{9}$");
    private static final int[] PESOS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private Ruc() {
    }

    /** VAL-ID-01. */
    public static boolean formatoValido(String ruc) {
        return ruc != null && FORMATO.matcher(ruc).matches();
    }

    /** VAL-ID-01 y VAL-ID-02. */
    public static boolean esValido(String ruc) {
        return formatoValido(ruc) && digitoVerificador(ruc) == Character.getNumericValue(ruc.charAt(10));
    }

    /** d = 11 − (Σ dígito × peso mod 11); si d = 10 → 0 y si d = 11 → 1. */
    public static int digitoVerificador(String ruc) {
        int suma = 0;
        for (int i = 0; i < PESOS.length; i++) {
            suma += Character.getNumericValue(ruc.charAt(i)) * PESOS[i];
        }
        int d = 11 - (suma % 11);
        return d == 10 ? 0 : d == 11 ? 1 : d;
    }
}
