package com.poe2craft;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(new ImportOption.DoNotIncludeTests())
          .importPackages("com.poe2craft");

  @Test
  void bundledConsumersCannotBypassRulesetSeals() {
    noClasses()
        .that()
        .resideInAPackage("com.poe2craft..")
        .and()
        .doNotHaveFullyQualifiedName("com.poe2craft.shared.ruleset.RulesetManifestLoader")
        .and()
        .doNotHaveFullyQualifiedName("com.poe2craft.shared.ruleset.SealedResources")
        .and()
        .doNotHaveFullyQualifiedName("com.poe2craft.shared.ruleset.SealedResources$Bundle")
        .and()
        .doNotHaveFullyQualifiedName("com.poe2craft.shared.ruleset.SealedResources$Dependencies")
        .should()
        .callMethod(Class.class, "getResourceAsStream", String.class)
        .check(CLASSES);
  }

  @Test
  void servicesAreIndependentAndCommonCodeCannotDependOnConsumers() {
    slices().matching("com.poe2craft.(*)..").should().beFreeOfCycles().check(CLASSES);
    for (String service : new String[] {"workbench", "craftingsupport"}) {
      String other = service.equals("workbench") ? "craftingsupport" : "workbench";
      noClasses()
          .that()
          .resideInAPackage("com.poe2craft." + service + "..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("com.poe2craft." + other + "..", "com.poe2craft.bootstrap..")
          .check(CLASSES);
    }
    noClasses()
        .that()
        .resideInAnyPackage("com.poe2craft.shared..", "com.poe2craft.configuration..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "com.poe2craft.workbench..",
            "com.poe2craft.craftingsupport..",
            "com.poe2craft.bootstrap..")
        .check(CLASSES);
    noClasses()
        .that()
        .resideOutsideOfPackages(
            "com.poe2craft.bootstrap..", "com.poe2craft.shared.crafting.application..")
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("com.poe2craft.shared.crafting.application.CatalogCraftingService")
        .check(CLASSES);
  }

  @Test
  void domainModelsStayPureAndApplicationCodeDoesNotDependOnAdapters() {
    noClasses()
        .that()
        .haveNameMatching(
            "com\\.poe2craft\\.shared\\.item\\.(ItemState|ModifierInstance|ModifierDefinition|ItemCatalog|ItemStateValidator|SolarAmulet)(\\$.*)?")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "com.fasterxml.jackson..",
            "jakarta.persistence..",
            "org.jooq..",
            "java.net..",
            "java.sql..",
            "com.poe2craft.shared.item.infrastructure..")
        .check(CLASSES);
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "org.jooq..",
            "java.net..",
            "java.sql..",
            "..application..",
            "..infrastructure..",
            "..api..",
            "com.poe2craft.configuration..")
        .check(CLASSES);
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..infrastructure..", "..api..")
        .check(CLASSES);
  }
}
