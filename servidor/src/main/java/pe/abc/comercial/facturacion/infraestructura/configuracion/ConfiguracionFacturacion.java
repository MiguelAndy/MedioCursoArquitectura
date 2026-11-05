package pe.abc.comercial.facturacion.infraestructura.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.abc.comercial.cpe.api.DocumentoElectronicoApi;
import pe.abc.comercial.facturacion.aplicacion.puerto.salida.PuertosSalida.EmisorPort;
import pe.abc.comercial.facturacion.aplicacion.puerto.salida.PuertosSalida.ParametrosTributariosPort;
import pe.abc.comercial.facturacion.aplicacion.servicio.EmitirComprobanteService;
import pe.abc.comercial.facturacion.dominio.ParametrosTributarios;
import pe.abc.comercial.facturacion.dominio.Partes.Domicilio;
import pe.abc.comercial.facturacion.dominio.Partes.Emisor;
import pe.abc.comercial.facturacion.infraestructura.cpe.DocumentoElectronicoCpeAdapter;
import pe.abc.comercial.facturacion.infraestructura.persistencia.AdaptadoresEnMemoria.ComprobanteRepositoryEnMemoria;
import pe.abc.comercial.facturacion.infraestructura.persistencia.AdaptadoresEnMemoria.NumeradorEnMemoria;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;

/**
 * Ensambla el módulo facturacion: el núcleo no conoce Spring, aquí se le inyectan sus adaptadores.
 * Emisor y parámetros tributarios vienen de {@code application.yml} (en fase 1: módulos configuracion y tributacion).
 */
@Configuration
@EnableConfigurationProperties({ConfiguracionFacturacion.EmisorProperties.class,
        ConfiguracionFacturacion.TributacionProperties.class})
public class ConfiguracionFacturacion {

    @ConfigurationProperties("abc.emisor")
    public record EmisorProperties(String ruc, String razonSocial, String nombreComercial, Domicilio domicilio) {
    }

    @ConfigurationProperties("abc.tributacion")
    public record TributacionProperties(BigDecimal tasaIgv, BigDecimal tarifaIcbper, BigDecimal factorIscPvp,
                                        BigDecimal umbralIdentificacionBoleta) {
    }

    /** VAL-FEC-01: la fecha del servidor es la de America/Lima. */
    @Bean
    Clock reloj() {
        return Clock.system(ZoneId.of("America/Lima"));
    }

    @Bean
    EmitirComprobanteService emitirComprobanteService(DocumentoElectronicoApi cpe, EmisorProperties emisor,
                                                      TributacionProperties tributacion, Clock reloj) {
        EmisorPort empresa = () -> new Emisor(emisor.ruc(), emisor.razonSocial(), emisor.nombreComercial(),
                emisor.domicilio());
        ParametrosTributariosPort parametros = fecha -> new ParametrosTributarios(tributacion.tasaIgv(),
                tributacion.tarifaIcbper(), tributacion.factorIscPvp(), tributacion.umbralIdentificacionBoleta());
        return new EmitirComprobanteService(new NumeradorEnMemoria(), new ComprobanteRepositoryEnMemoria(),
                new DocumentoElectronicoCpeAdapter(cpe), parametros, empresa, reloj);
    }
}
