package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.api.CraftingCatalogController;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.SolarAmulet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SolarCatalogConsistencyTest {
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void defaultSolarBootstrapUsesTheSameCatalogAsCurrencyExecution(boolean manualReveal) {
    var config = new CatalogConfiguration();
    var catalog = config.itemCatalog(config.workbenchDefinitions());
    var engine = config.craftingEngine(catalog);
    var service = config.workbenchService(catalog, config.workbenchSimulator(catalog, engine));
    var controller = new CraftingCatalogController(catalog, engine, service);
    var bootstrap = controller.initial(82, manualReveal);
    var workbench = service.initial("solar", 82, manualReveal);

    assertThat(bootstrap.state()).isEqualTo(workbench.state());
    assertThat(bootstrap.metadata()).isEqualTo(workbench.metadata());
    assertThat(bootstrap.modifiers()).isEqualTo(workbench.modifiers());
    assertThat(bootstrap.baseMaterialActions()).isEqualTo(workbench.baseMaterialActions());

    var bucket = bootstrap.state();
    var state =
        new ItemState(
            bucket.snapshotId(),
            bucket.baseItemId(),
            bucket.itemLevel(),
            bucket.rarity(),
            bucket.implicits(),
            List.of(),
            bucket.conditions());
    for (var action :
        List.of(
            WorkbenchCurrency.TRANSMUTATION,
            WorkbenchCurrency.AUGMENTATION,
            WorkbenchCurrency.REGAL,
            WorkbenchCurrency.EXALTED,
            WorkbenchCurrency.CHAOS,
            WorkbenchCurrency.DIVINE,
            WorkbenchCurrency.ANNULMENT)) {
      var result = service.apply(state, action, Set.of(), new Random(371));
      assertThat(result.applied()).as(action + ": " + result.reason()).isTrue();
      assertThat(result.state().baseItemId()).isEqualTo(SolarAmulet.BASE_ID);
      assertThat(result.state().snapshotId()).isEqualTo(bootstrap.metadata().snapshotId());
      state = result.state();
    }
  }
}
