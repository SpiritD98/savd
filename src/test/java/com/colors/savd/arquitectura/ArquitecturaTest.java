package com.colors.savd.arquitectura;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Reglas simples de arquitectura para el proyecto SAVD.
 */
@AnalyzeClasses(packages = "com.colors.savd")
public class ArquitecturaTest {

    /**
     * Las clases en el paquete controller deben terminar en 'Controller'.
     */
    @ArchTest
    static final ArchRule controllers_deben_terminar_en_Controller =
            classes()
                    .that().resideInAPackage("..controller..")
                    .and().haveSimpleNameNotEndingWith("Test")
                    .should().haveSimpleNameEndingWith("Controller");

    /**
     * Las clases en el paquete service no deben depender del paquete controller.
     */
    @ArchTest
    static final ArchRule services_no_deben_depender_de_controllers =
            noClasses()
                    .that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..");

    /**
     * Los repositorios no deben depender de services ni de controllers.
     */
    @ArchTest
    static final ArchRule repos_no_deben_depender_de_services_ni_controllers =
            noClasses()
                    .that().resideInAPackage("..repository..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..service..", "..controller..");

    /**
     * Todas las clases en el paquete repository deben terminar en 'Repository'.
     */
    @ArchTest
    static final ArchRule repos_no_deben_terminar_en_Repository = 
            classes()
                    .that().resideInAPackage("..repository..")
                    .and().resideOutsideOfPackage("..repository.projection..")
                    .should().haveSimpleNameEndingWith("Repository");
}

