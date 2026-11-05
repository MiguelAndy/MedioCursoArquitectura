package pe.abc.comercial.cpe.infraestructura;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import pe.abc.comercial.cpe.api.DocumentoElectronicoApi;
import pe.abc.comercial.cpe.aplicacion.GenerarDocumentoElectronicoService;
import pe.abc.comercial.cpe.infraestructura.firma.AlmacenCertificado;
import pe.abc.comercial.cpe.infraestructura.firma.FirmadorXmlDsigAdapter;
import pe.abc.comercial.cpe.infraestructura.ubl.GeneradorUblAdapter;
import pe.abc.comercial.cpe.infraestructura.ubl.ValidadorDocumentoAdapter;

import java.nio.file.Path;

/**
 * Ensambla el módulo cpe. El certificado se lee de {@code abc.cpe.firma.almacen} (PKCS#12, ADR-04);
 * si no se configura, se usa un certificado de prueba autofirmado generado al iniciar.
 */
@Configuration
public class ConfiguracionCpe {

    @Bean
    AlmacenCertificado almacenCertificado(@Value("${abc.cpe.firma.almacen:}") String almacen,
                                          @Value("${abc.cpe.firma.clave:}") String clave,
                                          @Value("${abc.cpe.firma.alias:}") String alias) {
        return almacen.isBlank() ? AlmacenCertificado.dePrueba() : AlmacenCertificado.cargar(Path.of(almacen), clave, alias);
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    @Bean
    GeneradorUblAdapter generadorUblAdapter(MessageSource messageSource) {
        return new GeneradorUblAdapter(messageSource);
    }

    @Bean
    DocumentoElectronicoApi documentoElectronicoApi(AlmacenCertificado certificado, GeneradorUblAdapter generadorUblAdapter) {
        return new GenerarDocumentoElectronicoService(generadorUblAdapter,
                new FirmadorXmlDsigAdapter(certificado.llave(), certificado.certificado()),
                new ValidadorDocumentoAdapter());
    }
}
