package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class StavesTalismansWorkbenchTest {
  @Test
  void newSourceClassesKeepTheirOwnPoolsAndValidateCraftingAtAllReviewedLevels() {
    var config = new CraftingConfiguration();
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
      assertThat(base.policy().socketExecutionMaximum()).isNull();
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
