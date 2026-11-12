package io.github.keel;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 模块边界与分层约束。规则失败即阻断构建。
 */
@AnalyzeClasses(packages = "io.github.keel", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule commonModulesMustNotDependOnBusinessModules =
            noClasses().that().resideInAPackage("io.github.keel.common..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "io.github.keel.iam..",
                            "io.github.keel.system..",
                            "io.github.keel.audit..");

    @ArchTest
    static final ArchRule iamMustNotDependOnOtherBusinessModules =
            noClasses().that().resideInAPackage("io.github.keel.iam..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "io.github.keel.system..",
                            "io.github.keel.audit..");

    @ArchTest
    static final ArchRule systemMustNotDependOnOtherBusinessModules =
            noClasses().that().resideInAPackage("io.github.keel.system..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "io.github.keel.iam..",
                            "io.github.keel.audit..");

    @ArchTest
    static final ArchRule auditMustNotDependOnOtherBusinessModules =
            noClasses().that().resideInAPackage("io.github.keel.audit..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "io.github.keel.iam..",
                            "io.github.keel.system..");

    @ArchTest
    static final ArchRule interfacesMustNotBeDependedOnByLowerLayers =
            noClasses().that().resideInAnyPackage(
                            "io.github.keel..application..",
                            "io.github.keel..domain..",
                            "io.github.keel..infrastructure..")
                    .should().dependOnClassesThat().resideInAPackage("io.github.keel..interfaces..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domainMustNotDependOnInfrastructureOrInterfaces =
            noClasses().that().resideInAPackage("io.github.keel..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "io.github.keel..infrastructure..",
                            "io.github.keel..interfaces..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule applicationMustNotDependOnWebFramework =
            noClasses().that().resideInAPackage("io.github.keel..application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.web..",
                            "jakarta.servlet..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule apiMustNotDependOnWebOrPersistenceFramework =
            noClasses().that().resideInAPackage("io.github.keel..api..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.web..",
                            "jakarta.servlet..",
                            "com.baomidou..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule commonCoreMustNotDependOnFrameworks =
            noClasses().that().resideInAPackage("io.github.keel.common.core..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "com.baomidou..",
                            "jakarta..");
}
