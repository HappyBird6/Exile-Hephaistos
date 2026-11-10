package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

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
