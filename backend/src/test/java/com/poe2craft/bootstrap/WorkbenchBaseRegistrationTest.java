package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.json.BundledJson;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 작업대·베이스·Registration 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code everyRegisteredBaseUsesTheSameCatalogForInitialCraftingDisplayAndContinuation()}:
 *       이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code verifyMaterialRow(key, simulator, row)}: 실제 베이스별 제작 후보, 요구 레벨과 수치 모델을 브라우저 생성본과
 *       대조한다.
 * </ul>
 */
class WorkbenchBaseRegistrationTest {
  @Test
  void everyRegisteredBaseUsesTheSameCatalogForInitialCraftingDisplayAndContinuation()
      throws Exception {
    var browser =
        BundledJson.mapper()
            .readTree(
                Path.of("../frontend/src/features/crafting/materialVerification.json").toFile());
    var config = new CatalogConfiguration();
    var catalog = config.itemCatalog(config.workbenchDefinitions());
    var service =
        config.workbenchService(
            catalog, config.workbenchSimulator(catalog, config.craftingEngine(catalog)));
    var ids = new HashSet<String>();
    for (var key : BaseRegistry.registeredKeys()) {
      var initial = service.initial(key, 100, true);
      var b = initial.state();
      var state =
          new ItemState(
              b.snapshotId(),
              b.baseItemId(),
              b.itemLevel(),
              b.rarity(),
              b.implicits(),
              List.of(),
              b.conditions());
      verifyMaterialRow(
          key,
          service.simulator(state),
          browser.path("rows").get(browser.path("byBase").required(key).asInt()));
      assertThat(ids.add(b.baseItemId())).as(key).isTrue();
      assertThat(service.simulator(state).catalog().metadata().snapshotId())
          .as(key)
          .isEqualTo(b.snapshotId());
      assertThat(service.qualityDisplay(state).state()).isEqualTo(state);
      assertThat(service.reviewedCatalogs())
          .anyMatch(c -> c.base().id().equals(state.baseItemId()));
    }
    assertThat(service.reviewedCatalogs()).hasSize(ids.size());
    assertThatThrownBy(() -> service.registerReviewedBase("solar", catalog, Map.of(), Map.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(service.initial("solar", 100).state().baseItemId()).isEqualTo(catalog.base().id());
    assertThatThrownBy(() -> service.initial("removed-season-base", 100))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static void verifyMaterialRow(String key, WorkbenchSimulator simulator, JsonNode row) {
    var targets = row.required("targets");
    var actions = new TreeSet<String>();
    targets.fieldNames().forEachRemaining(actions::add);
    assertThat(actions)
        .as(key)
        .containsExactlyInAnyOrderElementsOf(
            simulator.baseMaterialActions().stream()
                .filter(action -> action.catalystType() == null)
                .map(Enum::name)
                .toList());
    var essences = simulator.referenceEssenceTargets();
    for (var name : actions) {
      var currency = WorkbenchCurrency.valueOf(name);
      var rule = targets.get(name);
      var ids = new ArrayList<String>();
      int level = currency.minimumModifierLevel();
      for (var id : rule.required("ids")) {
        ids.add(id.asText());
        var definition = simulator.catalog().find(id.asText()).orElseThrow();
        level = Math.max(level, definition.requiredItemLevel());
        if (definition.stats().size() == 1) {
          var model = simulator.singleStatRollAssumption(definition);
          if (model != null)
            assertThat(model.id())
                .as("%s %s", key, name)
                .isEqualTo(
                    row.required("coupled").asBoolean()
                        ? "assumed-source-integer-roll-v1"
                        : "uniform-integer-roll-v1");
        }
      }
      assertThat(rule.required("level").asInt()).as("%s %s", key, name).isEqualTo(level);
      if (essences.containsKey(name))
        assertThat(ids)
            .as("%s %s", key, name)
            .containsExactlyInAnyOrderElementsOf(essences.get(name));
    }
  }
}
