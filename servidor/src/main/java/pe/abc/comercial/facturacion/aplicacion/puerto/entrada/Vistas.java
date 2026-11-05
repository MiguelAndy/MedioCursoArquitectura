package pe.abc.comercial.facturacion.aplicacion.puerto.entrada;

import java.util.List;

/**
 * Respuestas de los casos de uso; reflejan {@code specs/schemas/comprobante.response.schema.json}.
 * Los importes son texto decimal (n(12,2) y n(12,10)) para no perder precisión en los clientes.
 */
public final class Vistas {

    private Vistas() {
    }

    public record ComprobanteVista(String id, String tipoComprobante, String serie, long correlativo, String numero,
                                   String fechaEmision, String horaEmision, String fechaVencimiento,
                                   String tipoOperacion, String moneda, String tipoCambio, EmisorVista emisor,
                                   SolicitudEmision.Cliente cliente, List<ItemVista> items, TotalesVista totales,
                                   List<LeyendaVista> leyendas, SolicitudEmision.FormaPago formaPago,
                                   List<SolicitudEmision.Guia> guiasRemision, String ordenCompra, String estado,
                                   DocumentoElectronicoVista documentoElectronico, Enlaces enlaces) {
    }

    public record EmisorVista(String ruc, String razonSocial, String nombreComercial, String codigoEstablecimiento,
                              String direccion, String ubigeo) {
    }

    public record ItemVista(int numeroOrden, String codigoProducto, String descripcion, String unidadMedida,
                            String cantidad, String valorUnitario, String precioUnitario,
                            String valorReferencialUnitario, String tipoAfectacionIgv, SolicitudEmision.Isc isc,
                            SolicitudEmision.Icbper icbper, String codigoTributoIgv, String valorVenta,
                            String baseImponibleIgv, String porcentajeIgv, String montoIgv, String baseImponibleIsc,
                            String montoIsc, String tarifaIcbper, String montoIcbper, String totalTributos,
                            String importeTotal) {
    }

    public record TotalesVista(String operacionesGravadas, String operacionesExoneradas, String operacionesInafectas,
                               String operacionesExportacion, String operacionesGratuitas,
                               String tributosOperacionesGratuitas, String igv, String isc, String icbper,
                               String otrosTributos, String totalTributos, String totalValorVenta,
                               String totalPrecioVenta, String importeTotal) {
    }

    public record LeyendaVista(String codigo, String texto) {
    }

    public record DocumentoElectronicoVista(String nombreArchivo, String hash, String algoritmoFirma, String codigoQr) {
    }

    public record Enlaces(String self, String xml) {
    }

    /** Respuesta de {@code POST /comprobantes/calculos} (esquema {@code ResultadoCalculo}). */
    public record CalculoVista(List<ItemVista> items, TotalesVista totales, List<LeyendaVista> leyendas) {
    }

    /** Resultado de emitir: {@code reintento} = misma Idempotency-Key y mismo cuerpo (HTTP 200 en vez de 201). */
    public record ResultadoEmision(ComprobanteVista comprobante, boolean reintento) {
    }

    /** XML firmado para descargar. */
    public record ArchivoXml(String nombreArchivo, byte[] contenido) {
    }
}
