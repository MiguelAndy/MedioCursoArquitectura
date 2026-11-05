package pe.abc.comercial.facturacion.aplicacion.servicio;

import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.CasosDeUso.CalcularComprobanteUseCase;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.CasosDeUso.ComprobanteNoEncontradoException;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.CasosDeUso.ConflictoIdempotenciaException;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.CasosDeUso.ConsultarComprobantesUseCase;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.CasosDeUso.DescargarDocumentoElectronicoUseCase;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.CasosDeUso.EmitirComprobanteUseCase;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.CasosDeUso.SolicitudRechazadaException;
import pe.abc.comercial.facturacion.dominio.ReglaNegocioException;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.SolicitudEmision;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.ArchivoXml;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.CalculoVista;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.ComprobanteVista;
import pe.abc.comercial.facturacion.aplicacion.puerto.entrada.Vistas.ResultadoEmision;
import pe.abc.comercial.facturacion.aplicacion.puerto.salida.PuertosSalida.ComprobanteRepository;
import pe.abc.comercial.facturacion.aplicacion.puerto.salida.PuertosSalida.DocumentoElectronicoPort;
import pe.abc.comercial.facturacion.aplicacion.puerto.salida.PuertosSalida.EmisorPort;
import pe.abc.comercial.facturacion.aplicacion.puerto.salida.PuertosSalida.NumeradorPort;
import pe.abc.comercial.facturacion.aplicacion.puerto.salida.PuertosSalida.ParametrosTributariosPort;
import pe.abc.comercial.facturacion.dominio.Comprobante;
import pe.abc.comercial.facturacion.dominio.DatosComprobante;
import pe.abc.comercial.facturacion.dominio.MotorTributario;
import pe.abc.comercial.facturacion.dominio.ParametrosTributarios;
import pe.abc.comercial.facturacion.dominio.ResultadoCalculo;
import pe.abc.comercial.facturacion.dominio.ValidadorNormativo;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Casos de uso de emisión (docs/06 § 6.7): validar → calcular → numerar → generar y firmar el XML → guardar.
 *
 * <p>El número se consume solo después de pasar las capas 1 y 2: un error de validación no gasta correlativos
 * (docs/04 § 4.4). No usa Spring: la infraestructura lo instancia e inyecta sus puertos.</p>
 */
public class EmitirComprobanteService implements EmitirComprobanteUseCase, CalcularComprobanteUseCase,
        ConsultarComprobantesUseCase, DescargarDocumentoElectronicoUseCase {

    private final NumeradorPort numerador;
    private final ComprobanteRepository repositorio;
    private final DocumentoElectronicoPort documentoElectronico;
    private final ParametrosTributariosPort parametros;
    private final EmisorPort emisor;
    private final Clock reloj;
    private final MotorTributario motor = new MotorTributario();
    private final ValidadorNormativo validador = new ValidadorNormativo();

    public EmitirComprobanteService(NumeradorPort numerador, ComprobanteRepository repositorio,
                                    DocumentoElectronicoPort documentoElectronico, ParametrosTributariosPort parametros,
                                    EmisorPort emisor, Clock reloj) {
        this.numerador = Objects.requireNonNull(numerador);
        this.repositorio = Objects.requireNonNull(repositorio);
        this.documentoElectronico = Objects.requireNonNull(documentoElectronico);
        this.parametros = Objects.requireNonNull(parametros);
        this.emisor = Objects.requireNonNull(emisor);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public synchronized ResultadoEmision emitir(SolicitudEmision solicitud, String claveIdempotencia) {
        var previo = repositorio.porClaveIdempotencia(claveIdempotencia);
        if (previo.isPresent()) {
            if (!previo.get().solicitud().equals(solicitud)) {
                throw new ConflictoIdempotenciaException(claveIdempotencia);
            }
            return new ResultadoEmision(obtener(previo.get().comprobanteId()), true);
        }

        DatosComprobante datos = aDatos(solicitud);
        ResultadoCalculo calculo = validarYCalcular(datos);

        var empresa = emisor.emisor();
        long correlativo = numerador.siguiente(empresa.ruc(), datos.tipo(), datos.serie());
        Comprobante comprobante = Comprobante.emitir(UUID.randomUUID(), empresa, datos, correlativo, calculo);
        comprobante.adjuntarDocumentoElectronico(documentoElectronico.generar(comprobante));

        repositorio.guardar(comprobante, claveIdempotencia, solicitud);
        return new ResultadoEmision(MapeadorVista.comprobante(comprobante, solicitud), false);
    }

    @Override
    public CalculoVista calcular(SolicitudEmision solicitud) {
        return MapeadorVista.calculo(validarYCalcular(aDatos(solicitud)));
    }

    @Override
    public ComprobanteVista obtener(UUID comprobanteId) {
        Comprobante comprobante = buscar(comprobanteId);
        SolicitudEmision solicitud = repositorio.solicitudDe(comprobanteId);
        return MapeadorVista.comprobante(comprobante, solicitud);
    }

    @Override
    public ArchivoXml descargarXml(UUID comprobanteId) {
        var documento = buscar(comprobanteId).documentoElectronico();
        return new ArchivoXml(documento.nombreArchivo() + ".xml", documento.xml());
    }

    private DatosComprobante aDatos(SolicitudEmision solicitud) {
        return rechazarSiIncumple(() -> MapeadorSolicitud.aDatos(solicitud, LocalDate.now(reloj),
                LocalTime.now(reloj).truncatedTo(ChronoUnit.SECONDS)));
    }

    private ResultadoCalculo validarYCalcular(DatosComprobante datos) {
        return rechazarSiIncumple(() -> {
            validador.validarAntesDelCalculo(datos, LocalDate.now(reloj));
            ParametrosTributarios vigentes = parametros.vigentesAl(datos.fechaEmision());
            ResultadoCalculo calculo = motor.calcular(datos, vigentes);
            validador.validarDespuesDelCalculo(datos, calculo, vigentes);
            return calculo;
        });
    }

    /** Traduce la excepción del dominio al contrato del puerto de entrada (el controlador no conoce el dominio). */
    private static <T> T rechazarSiIncumple(java.util.function.Supplier<T> accion) {
        try {
            return accion.get();
        } catch (ReglaNegocioException e) {
            throw new SolicitudRechazadaException(e.violaciones().stream()
                    .map(v -> new SolicitudRechazadaException.ReglaIncumplida(v.regla(), v.campo(), v.mensaje()))
                    .toList(), e);
        }
    }

    private Comprobante buscar(UUID id) {
        return repositorio.porId(id).orElseThrow(() -> new ComprobanteNoEncontradoException(id));
    }
}
