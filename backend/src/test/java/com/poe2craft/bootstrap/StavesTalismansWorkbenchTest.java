package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.item.ItemState;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Staves·Talismans·작업대 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code newSourceClassesKeepTheirOwnPoolsAndValidateCraftingAtAllReviewedLevels()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class StavesTalismansWorkbenchTest {
  @Test
  void newSourceClassesKeepTheirOwnPoolsAndValidateCraftingAtAllReviewedLevels() {
    var config = new CatalogConfiguration();
    var catalog = config.itemCatalog(config.workbenchDefinitions());
    var service =
        config.workbenchService(
            catalog, config.workbenchSimulator(catalog, config.craftingEngine(catalog)));
    var bases = BaseRegistry.familyBases("staves", "talismans");
    assertThat(bases).hasSize(9);
    for (var entry : bases.entrySet()) {
      var base = BaseRegistry.require(entry.getKey());
      assertThat(base.policy().itemClass())
          .isEqualTo(base.family().equals("staves") ? "Staves" : "Talismans");
      assertThat(base.policy().socketExecutionMaximum()).isEqualTo(3);
      assertThat(base.policy().ordinaryCatalyst()).isFalse();
      for (int level : new int[] {1, 20, 82}) {
        var initial = service.initial(entry.getKey(), level);
        assertThat(initial.state().baseItemId()).isEqualTo(entry.getValue());
        assertThat(initial.state().itemLevel()).isEqualTo(level);
        assertThat(initial.modifiers().values().stream().filter(d -> d.weight() > 0).count())
            .isEqualTo(base.family().equals("staves") ? 185 : 158);
        var s = initial.state();
        var state =
            new ItemState(
                s.snapshotId(),
                s.baseItemId(),
                s.itemLevel(),
                s.rarity(),
                s.implicits(),
                List.of(),
                s.conditions());
        for (var action :
            List.of(
                WorkbenchCurrency.TRANSMUTATION,
                WorkbenchCurrency.AUGMENTATION,
                WorkbenchCurrency.REGAL,
                WorkbenchCurrency.EXALTED,
                WorkbenchCurrency.CHAOS,
                WorkbenchCurrency.ANNULMENT)) {
          var result = service.apply(state, action, Set.of(), new Random(61005));
          assertThat(result.applied())
              .as(entry.getKey() + " ilvl " + level + " " + action)
              .isTrue();
          assertThat(result.state().baseItemId()).isEqualTo(entry.getValue());
          state = result.state();
        }
      }
    }
  }
}
