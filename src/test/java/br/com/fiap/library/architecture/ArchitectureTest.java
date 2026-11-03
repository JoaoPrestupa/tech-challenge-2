package br.com.fiap.library.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "br.com.fiap.library", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_does_not_depend_on_outer_layers_or_frameworks = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..adapter..", "..infrastructure..",
                    "org.springframework..", "jakarta..", "com.nimbusds..", "tools.jackson..");

    @ArchTest
    static final ArchRule application_does_not_depend_on_adapters_or_frameworks = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter..", "..infrastructure..", "org.springframework..", "jakarta..", "com.nimbusds..");

    @ArchTest
    static final ArchRule adapters_do_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("..adapter..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule inbound_adapters_do_not_use_outbound_adapters = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..");

    @ArchTest
    static final ArchRule outbound_adapters_do_not_use_inbound_adapters = noClasses()
            .that().resideInAPackage("..adapter.out..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.in..");

    @ArchTest
    static final ArchRule jpa_entities_stay_in_the_persistence_adapter = noClasses()
            .that().resideOutsideOfPackage("..adapter.out.persistence..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");
}
