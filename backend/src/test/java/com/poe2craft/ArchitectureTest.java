package com.poe2craft;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Architecture 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code CLASSES}: 동치류목록 ({@code JavaClasses}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>controllersDeclareOnlyEndpoints는 모든 REST 컨트롤러의 중첩 타입과 매핑 없는 메서드를 검사한다.
 *   <li>{@code bundledConsumersCannotBypassRulesetSeals()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code servicesAreIndependentAndCommonCodeCannotDependOnConsumers()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code domainModelsStayPureAndApplicationCodeDoesNotDependOnAdapters()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
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

  @Test
  void controllersDeclareOnlyEndpoints() {
    for (var type : CLASSES) {
      if (!type.isAnnotatedWith(RestController.class)) continue;
      var controller = type.reflect();
      org.assertj.core.api.Assertions.assertThat(controller.getDeclaredClasses())
          .as(controller.getName())
          .isEmpty();
      for (var method : controller.getDeclaredMethods()) {
        if (method.isSynthetic()) continue;
        org.assertj.core.api.Assertions.assertThat(
                AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class))
            .as(method.toString())
            .isTrue();
      }
    }
  }
}
