package pe.abc.comercial.facturacion.dominio;

import java.math.BigDecimal;

/**
 * Parámetros vigentes a la fecha de emisión (módulo tributacion, ADR-08: datos, no constantes).
 *
 * @param tasaIgv                    TASA_IGV como fracción (0.18)
 * @param tarifaIcbper               TARIFA_ICBPER por bolsa (0.50)
 * @param factorIscPvp               FACTOR_ISC_PVP del sistema 03 del ISC
 * @param umbralIdentificacionBoleta UMBRAL_IDENTIFICACION_BOLETA en soles (700.00, VAL-BOL-01)
 */
public record ParametrosTributarios(BigDecimal tasaIgv, BigDecimal tarifaIcbper, BigDecimal factorIscPvp,
                                    BigDecimal umbralIdentificacionBoleta) {
}
