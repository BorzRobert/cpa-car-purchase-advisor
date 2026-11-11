package ro.cpa.advisor;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Regulile de straturi din plan §2.2 ca test, ca sa nu se strice pe nesimtite:
 * api -> service -> repository, service -> domain, `ai` folosit doar de `service`, `domain` nu depinde de nimic.
 */
@AnalyzeClasses(packages = "ro.cpa.advisor", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // Straturile raman optionale: un strat fara clase (de exemplu `repository` intr-un test partial) nu trebuie sa pice testul.
    @ArchTest
    static final ArchRule layersAreRespected = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Api").definedBy("ro.cpa.advisor.api..")
            .layer("Service").definedBy("ro.cpa.advisor.service..")
            .layer("Repository").definedBy("ro.cpa.advisor.repository..")
            .layer("Ai").definedBy("ro.cpa.advisor.ai..")
            .whereLayer("Api").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Api")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Ai").mayOnlyBeAccessedByLayers("Service");

    @ArchTest
    static final ArchRule domainDependsOnNothing = noClasses()
            .that().resideInAPackage("ro.cpa.advisor.domain..")
            .should().dependOnClassesThat().resideOutsideOfPackages("ro.cpa.advisor.domain..", "java..");

    @ArchTest
    static final ArchRule controllersNeverTouchRepositoriesOrSpringAi = noClasses()
            .that().resideInAPackage("ro.cpa.advisor.api..")
            .should().dependOnClassesThat().resideInAnyPackage("ro.cpa.advisor.repository..", "org.springframework.ai..");

    // Spring AI trebuie sa ramana in spatele lui `AdvisorModel`: schimbarea bibliotecii sau a modelului atinge un singur pachet.
    @ArchTest
    static final ArchRule springAiLivesOnlyInTheAiPackage = noClasses()
            .that().resideOutsideOfPackage("ro.cpa.advisor.ai..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.ai..");
}
