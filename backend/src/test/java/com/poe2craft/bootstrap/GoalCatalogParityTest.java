package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.craftingsupport.goal.api.GoalFilterController;
import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalDefinitions;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.craftingsupport.goal.infrastructure.BundledGoalCatalogs;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 목표·카탈로그·동일성 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code everyStartingCatalogSourceIsDiscoverableWithoutRarityOrFamilyPruning()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
class GoalCatalogParityTest {
  /** 운영 작업대에서 얻은 스냅샷·버전으로 예제를 재생성하고 저장된 카탈로그와 평가 요청·응답 전체를 검증한다. */
  @Test
  void checkedInProductionExampleMatchesCurrentCatalogAndActualItemEvaluation() throws Exception {
    var cfg = new CatalogConfiguration();
    var solar = cfg.itemCatalog(cfg.workbenchDefinitions());
    var workbench =
        cfg.workbenchService(solar, cfg.workbenchSimulator(solar, cfg.craftingEngine(solar)));
    var service = new CraftingSupportConfiguration().goalFilterService(workbench);
    var json = new ObjectMapper();
    var controller = new GoalFilterController(service, json, workbench);
    var initial = workbench.initial("solar", 82).state();
    var context = new Context(initial.snapshotId(), initial.baseItemId(), initial.itemLevel());
    var catalog =
        controller.catalog(context.snapshotId(), context.baseItemId(), context.itemLevel());
    assertThat(catalog.issues()).isEmpty();
    assertThat(catalog.sourceModifiers()).isNotEmpty().isEqualTo(solar.modifiers());

    try (var stream =
        getClass().getResourceAsStream("/crafting/goalfilter/production-example.json")) {
      var saved = json.readTree(stream);
      // 예제의 실제 굴림과 목표를 재사용하고 현재 운영 버전을 가리키는 두 필드만 갱신한다.
      var request = (ObjectNode) saved.required("evaluateRequest").deepCopy();
      ((ObjectNode) request.required("item")).put("snapshotId", context.snapshotId());
      ((ObjectNode) request.required("goal")).put("catalogVersion", catalog.catalogVersion());
      var item = json.treeToValue(request.required("item"), ItemState.class);
      assertThat(item.explicits())
          .extracting(ModifierInstance::values)
          .containsExactlyInAnyOrder(
              Map.of("base_cold_damage_resistance_%", 10L),
              Map.of("base_resist_all_elements_%", 12L));
      assertThat(request.at("/goal/groups/0/entries/0/statId").textValue())
          .isEqualTo(GoalCatalogIndex.id("pseudo", "total_cold_resistance"));
      var evaluation = controller.evaluate(request);
      assertThat(evaluation.status()).isEqualTo(Status.MATCH);
      assertThat(evaluation.groups().getFirst().entries().getFirst().value())
          .isEqualByComparingTo("22");

      var sample = json.createObjectNode();
      sample.put("version", 1).put("synthetic", false);
      sample.set("context", json.valueToTree(context));
      sample.set("catalog", json.valueToTree(catalog));
      sample.set("evaluateRequest", request);
      sample.set("evaluateResponse", json.valueToTree(evaluation));
      Files.writeString(
          Path.of("build/goal-filter-production-example.json"),
          json.writerWithDefaultPrettyPrinter().writeValueAsString(sample));

      // 2개의 집합 필드(tags, familyIds)는 순서와 무관하게 비교하고 나머지 JSON은 그대로 비교한다.
      var type = new TypeReference<Map<String, ModifierDefinition>>() {};
      var actualModifiers = ((ObjectNode) sample.required("catalog")).remove("sourceModifiers");
      var savedModifiers = ((ObjectNode) saved.required("catalog")).remove("sourceModifiers");
      assertThat(json.convertValue(actualModifiers, type))
          .isEqualTo(json.convertValue(savedModifiers, type));
      assertThat(json.readTree(json.writeValueAsBytes(sample))).isEqualTo(saved);
    }
  }

  @Test
  void everyStartingCatalogSourceIsDiscoverableWithoutRarityOrFamilyPruning() throws Exception {
    var cfg = new CatalogConfiguration();
    var solar = cfg.itemCatalog(cfg.workbenchDefinitions());
    var workbench =
        cfg.workbenchService(solar, cfg.workbenchSimulator(solar, cfg.craftingEngine(solar)));
    var catalogs = BundledGoalCatalogs.load(workbench);
    assertThat(catalogs).hasSize(179);
    var index = new GoalCatalogIndex(catalogs, GoalDefinitionsLoader.load());
    for (var source : catalogs) {
      for (int level : new int[] {1, 82}) {
        var goalCatalog =
            index.catalog(new Context(source.metadata().snapshotId(), source.base().id(), level));
        assertThat(goalCatalog.issues()).isEmpty();
        assertThat(goalCatalog.sourceModifiers()).isEqualTo(source.modifiers());
        var ids = goalCatalog.stats().stream().map(GoalCatalog.Stat::statId).toList();
        for (var modifier : source.modifiers().values())
          for (var stat : modifier.stats())
            assertThat(ids).contains(GoalCatalogIndex.id(modifier.layer().name(), stat.id()));
      }
    }
    var service = new GoalFilterService(index);
    var json = new ObjectMapper();
    for (String key : List.of("solar", "stocky", "amber")) {
      var initial = workbench.initial(key, 82);
      var state = initial.state();
      var context = new Context(state.snapshotId(), state.baseItemId(), 82);
      var catalog = service.catalog(context);
      var definition =
          initial.modifiers().values().stream()
              .filter(
                  d ->
                      d.layer() == ModifierDefinition.Layer.EXPLICIT
                          && d.stats().size() == 1
                          && d.stats().getFirst().id().equals("base_maximum_life")
                          && d.weight() > 0
                          && d.requiredItemLevel() <= 82)
              .findFirst()
              .orElseThrow();
      long value = definition.stats().getFirst().max();
      var item =
          new ItemState(
              state.snapshotId(),
              state.baseItemId(),
              82,
              ItemState.Rarity.RARE,
              state.implicits(),
              List.of(new ModifierInstance(definition.id(), Map.of("base_maximum_life", value))),
              Set.of());
      var stat =
          catalog.stats().stream()
              .filter(s -> s.statId().equals(GoalCatalogIndex.id("explicit", "base_maximum_life")))
              .findFirst()
              .orElseThrow();
      var goal =
          new GoalFilter(
              1,
              catalog.catalogVersion(),
              new General(
                  state.baseItemId(), new Range(null, null), List.of(ItemState.Rarity.RARE)),
              List.of(
                  new Group(
                      "g",
                      Type.AND,
                      false,
                      null,
                      List.of(
                          new Entry(
                              "e",
                              stat.statId(),
                              stat.unit(),
                              new Range(BigDecimal.valueOf(value), BigDecimal.valueOf(value)),
                              null,
                              false)))));
      var roundTrip = json.readValue(json.writeValueAsBytes(goal), GoalFilter.class);
      assertThat(service.evaluate(item, roundTrip).status()).isEqualTo(Status.MATCH);
      // A deliberately unreviewed definition set keeps this rejection contract independent
      // of growing production coverage.
      var unreviewedService =
          new GoalFilterService(
              new GoalCatalogIndex(
                  catalogs, new GoalDefinitions(1, "unreviewed-contract", List.of(), List.of())));
      var unreviewedCatalog = unreviewedService.catalog(context);
      var unsupported =
          unreviewedCatalog.stats().stream()
              .filter(
                  s ->
                      s.kind() == GoalCatalog.Kind.EXPLICIT
                          && s.eligible()
                          && s.support().evaluation() == Capability.UNSUPPORTED)
              .findFirst()
              .orElseThrow();
      var unreviewed =
          new GoalFilter(
              1,
              unreviewedCatalog.catalogVersion(),
              goal.general(),
              List.of(
                  new Group(
                      "g",
                      Type.AND,
                      false,
                      null,
                      List.of(
                          new Entry(
                              "e",
                              unsupported.statId(),
                              unsupported.unit(),
                              new Range(null, null),
                              null,
                              false)))));
      assertThat(unreviewedService.evaluate(item, unreviewed).status())
          .isEqualTo(Status.UNSUPPORTED);
    }
  }
}
