package pe.abc.comercial.cpe.infraestructura.firma;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Carga la llave y el certificado de un almacén PKCS#12 (.pfx/.p12).
 *
 * <p>Si no se configura un almacén, se genera un <b>certificado de prueba autofirmado</b> con {@code keytool}
 * (igual que {@code tools/validar_specs.py --firmar}). No es válido ante SUNAT y nunca se versiona.</p>
 */
public record AlmacenCertificado(PrivateKey llave, X509Certificate certificado) {

    public static final String ALIAS_PRUEBA = "abc-prueba";
    private static final String CLAVE_PRUEBA = "prueba-no-valida-sunat";
    private static final String DN_PRUEBA =
            "CN=ABC SOFTWARE CORP SAC - CERTIFICADO DE PRUEBA, OU=20123456786, O=ABC SOFTWARE CORP SAC, C=PE";

    public static AlmacenCertificado cargar(Path almacen, String clave, String alias) {
        try (InputStream entrada = Files.newInputStream(almacen)) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(entrada, clave.toCharArray());
            return new AlmacenCertificado((PrivateKey) ks.getKey(alias, clave.toCharArray()),
                    (X509Certificate) ks.getCertificate(alias));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer el certificado " + almacen, e);
        }
    }

    public static AlmacenCertificado dePrueba() {
        try {
            Path carpeta = Files.createTempDirectory("abc-certificado-prueba");
            Path almacen = carpeta.resolve("certificado-prueba.p12");
            Path keytool = Path.of(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name").toLowerCase().contains("win") ? "keytool.exe" : "keytool");
            Process proceso = new ProcessBuilder(keytool.toString(), "-genkeypair", "-alias", ALIAS_PRUEBA,
                    "-keyalg", "RSA", "-keysize", "2048", "-validity", "365", "-storetype", "PKCS12",
                    "-keystore", almacen.toString(), "-storepass", CLAVE_PRUEBA, "-keypass", CLAVE_PRUEBA,
                    "-dname", DN_PRUEBA).redirectErrorStream(true).start();
            if (proceso.waitFor() != 0) {
                throw new IllegalStateException("keytool falló: " + new String(proceso.getInputStream().readAllBytes()));
            }
            AlmacenCertificado resultado = cargar(almacen, CLAVE_PRUEBA, ALIAS_PRUEBA);
            Files.deleteIfExists(almacen);
            Files.deleteIfExists(carpeta);
            return resultado;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el certificado de prueba", e);
        }
    }
}
