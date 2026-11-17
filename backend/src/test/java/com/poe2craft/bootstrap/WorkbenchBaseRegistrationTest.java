package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.*;
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
 * </ul>
 */
class WorkbenchBaseRegistrationTest {
  @Test
  void everyRegisteredBaseUsesTheSameCatalogForInitialCraftingDisplayAndContinuation() {
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
}
