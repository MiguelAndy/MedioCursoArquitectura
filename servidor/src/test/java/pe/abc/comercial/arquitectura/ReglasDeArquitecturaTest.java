package pe.abc.comercial.arquitectura;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Reglas de docs/06 § 6.5, ejecutadas tal como se especificaron en la fase 1:
 * "si alguna falla, la compilación en CI falla".
 */
@AnalyzeClasses(packages = "pe.abc.comercial", importOptions = ImportOption.DoNotIncludeTests.class)
class ReglasDeArquitecturaTest {

    @ArchTest
    static final ArchRule dominioSinFrameworks = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "jakarta.xml..", "com.fasterxml..",
                    "..web..", "..infraestructura..", "..aplicacion..");

    @ArchTest
    static final ArchRule capas = layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Controlador").definedBy("..web..")
            .layer("Aplicacion").definedBy("..aplicacion..")
            .layer("Dominio").definedBy("..dominio..")
            .layer("Infraestructura").definedBy("..infraestructura..")
            .whereLayer("Controlador").mayNotBeAccessedByAnyLayer()
            .whereLayer("Aplicacion").mayOnlyBeAccessedByLayers("Controlador", "Infraestructura")
            .whereLayer("Dominio").mayOnlyBeAccessedByLayers("Aplicacion", "Infraestructura")
            .whereLayer("Infraestructura").mayNotBeAccessedByAnyLayer();

    /** cpe no conoce a facturacion: así puede extraerse como cpe-service (docs/06 § 6.9). */
    @ArchTest
    static final ArchRule cpeIndependienteDeFacturacion = noClasses()
            .that().resideInAPackage("pe.abc.comercial.cpe..")
            .should().dependOnClassesThat().resideInAPackage("pe.abc.comercial.facturacion..");

    /** Los otros módulos solo usan la API pública de cpe. */
    @ArchTest
    static final ArchRule soloLaApiPublicaDeCpe = noClasses()
            .that().resideOutsideOfPackage("pe.abc.comercial.cpe..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "pe.abc.comercial.cpe.aplicacion..", "pe.abc.comercial.cpe.infraestructura..");

    @ArchTest
    static final ArchRule modulosSinCiclos = slices().matching("pe.abc.comercial.(*)..").should().beFreeOfCycles();
}
