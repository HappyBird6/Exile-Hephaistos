package com.poe2craft;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  @Test
  void modulesHaveNoCyclesAndDomainsStayPure() {
    var classes =
        new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.poe2craft");
    slices().matching("com.poe2craft.(*)..").should().beFreeOfCycles().check(classes);
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
            "com.openai..")
        .allowEmptyShould(true)
        .check(classes);
    String[] modules = {
      "season",
      "modifier",
      "item",
      "currency",
      "currencyrule",
      "price",
      "crafting",
      "preset",
      "ai",
      "user"
    };
    var allowed =
        java.util.Map.of(
            "season", java.util.Set.<String>of(),
            "modifier", java.util.Set.of("season"),
            "item", java.util.Set.of("season", "modifier"),
            "currency", java.util.Set.of("season"),
            "currencyrule", java.util.Set.of("season", "currency", "modifier"),
            "price", java.util.Set.of("season", "currency"),
            "crafting",
                java.util.Set.of("item", "modifier", "currency", "currencyrule", "season", "price"),
            "preset", java.util.Set.of("item", "modifier", "season"),
            "ai",
                java.util.Set.of(
                    "item", "modifier", "currency", "preset", "crafting", "season", "price"),
            "user", java.util.Set.<String>of());
    for (String source : modules) {
      for (String target : modules) {
        if (!source.equals(target) && !allowed.get(source).contains(target)) {
          noClasses()
              .that()
              .resideInAPackage("com.poe2craft." + source + "..")
              .should()
              .dependOnClassesThat()
              .resideInAPackage("com.poe2craft." + target + "..")
              .allowEmptyShould(true)
              .check(classes);
        }
      }
    }
    noClasses()
        .that()
        .resideInAnyPackage("..domain..", "..api..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..application..", "..infrastructure..", "..presentation..")
        .allowEmptyShould(true)
        .check(classes);
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..infrastructure..", "..presentation..")
        .allowEmptyShould(true)
        .check(classes);
    for (String module : modules) {
      noClasses()
          .that()
          .resideOutsideOfPackage("com.poe2craft." + module + "..")
          .and()
          .resideOutsideOfPackage("com.poe2craft.bootstrap..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "com.poe2craft." + module + ".domain..", "com.poe2craft." + module + ".application..",
              "com.poe2craft." + module + ".infrastructure..",
                  "com.poe2craft." + module + ".presentation..")
          .allowEmptyShould(true)
          .check(classes);
    }
  }
}
