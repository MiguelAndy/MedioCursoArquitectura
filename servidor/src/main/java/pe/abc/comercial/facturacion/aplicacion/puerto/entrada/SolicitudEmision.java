package pe.abc.comercial.facturacion.aplicacion.puerto.entrada;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Comando de {@code EmitirComprobanteUseCase} y {@code CalcularComprobanteUseCase}: refleja
 * {@code specs/schemas/comprobante-emision.request.schema.json} (los importes viajan como texto decimal).
 * Cuando llega aquí, la solicitud ya cumplió su JSON Schema (capa 1, VAL-EST-01).
 */
public record SolicitudEmision(String tipoComprobante,
                               String serie,
                               LocalDate fechaEmision,
                               LocalTime horaEmision,
                               LocalDate fechaVencimiento,
                               String tipoOperacion,
                               String moneda,
                               String tipoCambio,
                               Cliente cliente,
                               List<Item> items,
                               List<Map<String, Object>> cargosDescuentosGlobales,
                               FormaPago formaPago,
                               Map<String, Object> documentoReferencia,
                               List<Guia> guiasRemision,
                               String ordenCompra,
                               Map<String, Object> detraccion,
                               String observaciones,
                               String puntoEmisionId,
                               Boolean notificarCliente) {

    public record Cliente(String terceroId, String tipoDocumento, String numeroDocumento, String razonSocial,
                          String direccion, String ubigeo, String correo) {
    }

    public record Item(String productoId, String codigoProducto, String codigoProductoSunat, String codigoGtin,
                       String descripcion, String unidadMedida, String cantidad, String valorUnitario,
                       String precioUnitario, String valorReferencialUnitario, String tipoAfectacionIgv,
                       Isc isc, Icbper icbper, List<Map<String, Object>> cargosDescuentos) {
    }

    public record Isc(String sistema, String tasa, String montoFijoUnitario, String cantidadEnUnidadBase,
                      String precioVentaPublicoSugerido) {
    }

    public record Icbper(Integer cantidadBolsas) {
    }

    public record FormaPago(String tipo, String montoPendiente, List<Cuota> cuotas) {
    }

    public record Cuota(Integer numero, String monto, LocalDate fechaVencimiento) {
    }

    public record Guia(String tipo, String numero) {
    }
}
