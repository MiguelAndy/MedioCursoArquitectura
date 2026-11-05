package pe.abc.comercial.facturacion.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.PathType;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Capa 1 (VAL-EST-01): valida el cuerpo con el JSON Schema 2020-12 de la fase 1
 * ({@code specs/schemas/comprobante-emision.request.schema.json}), el mismo que usan los clientes Windows y Android.
 * No se reescriben las reglas del contrato en Java: se aplica el contrato.
 */
@Component
public class ValidadorContrato {

    public static final String ESQUEMA_SOLICITUD = "classpath:schemas/comprobante-emision.request.schema.json";

    /** Error de contrato con el JSON Pointer del campo. */
    public record ErrorContrato(String campo, String mensaje) {
    }

    /** 400: la solicitud no cumple su JSON Schema. */
    public static class ContratoInvalidoException extends RuntimeException {
        private final transient List<ErrorContrato> errores;

        public ContratoInvalidoException(List<ErrorContrato> errores) {
            super("La solicitud no cumple el contrato: " + errores);
            this.errores = List.copyOf(errores);
        }

        public List<ErrorContrato> errores() {
            return errores;
        }
    }

    private final JsonSchema esquema;

    public ValidadorContrato() {
        SchemaValidatorsConfig configuracion = SchemaValidatorsConfig.builder()
                .pathType(PathType.JSON_POINTER)
                .build();
        this.esquema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(SchemaLocation.of(ESQUEMA_SOLICITUD), configuracion);
    }

    public void validar(JsonNode solicitud) {
        List<ErrorContrato> errores = esquema.validate(solicitud).stream()
                .map(m -> new ErrorContrato(campo(m), m.getMessage()))
                .distinct()
                .sorted(Comparator.comparing(ErrorContrato::campo))
                .toList();
        if (!errores.isEmpty()) {
            throw new ContratoInvalidoException(errores);
        }
    }

    /** JSON Pointer del campo; si falta un campo obligatorio, se señala ese campo y no su objeto padre. */
    private static String campo(com.networknt.schema.ValidationMessage mensaje) {
        String ubicacion = mensaje.getInstanceLocation().toString();
        if ("required".equals(mensaje.getType()) && mensaje.getProperty() != null) {
            return ubicacion + "/" + mensaje.getProperty();
        }
        return ubicacion;
    }
}
