package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.crafting.infrastructure.WorkbenchDefinitionsLoader;
import com.poe2craft.shared.item.*;
import com.poe2craft.workbench.repeat.*;
import com.poe2craft.workbench.repeat.ChaosRepeatProtocol.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AmuletChaosRepeatTest {
  @Test
  void allReviewedAmuletsAcceptOneThousandChaosUsesWithCraftedOriginalModifiers() {
    var configuration = new CatalogConfiguration();
    var solar = configuration.itemCatalog(WorkbenchDefinitionsLoader.initialize());
    var service =
        configuration.workbenchService(
            solar, new WorkbenchSimulator(solar, new CraftingEngine(solar)));
    var repeat =
        new ChaosRepeatService(
            service,
            new ChaosDisplayResources(new com.fasterxml.jackson.databind.ObjectMapper()),
            "test");
    int checked = 0;
    for (var catalog : service.reviewedCatalogs()) {
      if (!Set.of("Amulet", "Amulets").contains(BaseRegistry.itemClass(catalog.base().id())))
        continue;
      var implicits = new ArrayList<ModifierInstance>();
      if (catalog.base().hasImplicit()) {
        var d = catalog.find(catalog.base().implicitModifierId()).orElseThrow();
        var v = new TreeMap<String, Long>();
        d.stats().forEach(s -> v.put(s.id(), s.max()));
        implicits.add(new ModifierInstance(d.id(), v));
      }
      var d =
          catalog.modifiers().values().stream()
              .filter(
                  m ->
                      m.layer() == ModifierDefinition.Layer.EXPLICIT
                          && m.weight() > 0
                          && m.requiredItemLevel() <= 100)
              .findFirst()
              .orElseThrow();
      var values = new TreeMap<String, Long>();
      d.stats().forEach(s -> values.put(s.id(), s.min()));
      var item =
          new ItemState(
              catalog.metadata().snapshotId(),
              catalog.base().id(),
              100,
              ItemState.Rarity.RARE,
              implicits,
              List.of(new ModifierInstance(d.id(), values).asCrafted()),
              Set.of());
      var outcome =
          repeat.run(
              new Input(
                  "test",
                  "sha",
                  new Concrete(item),
                  WorkbenchCurrency.CHAOS,
                  Set.of(),
                  "__no_possible_item_option__",
                  "ko",
                  1000),
              new SplittableRandom(731));
      assertThat(outcome.failure()).as(catalog.base().name()).isNull();
      assertThat(outcome.actualUses()).as(catalog.base().name()).isEqualTo(1000);
      assertThat(outcome.termination()).isEqualTo("LIMIT");
      checked++;
    }
    assertThat(checked).isGreaterThan(10);
  }
}
