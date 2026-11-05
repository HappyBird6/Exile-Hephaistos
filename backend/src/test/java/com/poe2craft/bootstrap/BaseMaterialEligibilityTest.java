package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import org.junit.jupiter.api.Test;

class BaseMaterialEligibilityTest {
  private final CatalogCraftingService service;

  BaseMaterialEligibilityTest() {
    var solar = ItemCatalogLoader.loadDefault();
    service =
        new CatalogConfiguration()
            .workbenchService(solar, new WorkbenchSimulator(solar, new CraftingEngine(solar)));
  }

  @Test
  void listsReviewedMaterialsForEveryBaseWithoutItemLevelOrRarityGating() {
    for (String key : BaseRegistry.registeredKeys()) {
      var low = service.initial(key, 1);
      var high = service.initial(key, 100);
      assertThat(low.baseMaterialActions())
          .as(key)
          .isEqualTo(high.baseMaterialActions())
          .doesNotHaveDuplicates();
      String id = low.state().baseItemId();
      for (var action : WorkbenchCurrency.values()) {
        if (action.catalystType() != null) {
          assertThat(low.baseMaterialActions().contains(action))
              .as(key + ":" + action)
              .isEqualTo(
                  action.refinedCatalyst()
                      ? BaseRegistry.refinedCatalyst(id)
                      : BaseRegistry.ordinaryCatalyst(id));
        }
      }
      if (!BasicJewel.supportedCrafting(id))
        assertThat(low.baseMaterialActions()).noneMatch(WorkbenchCurrency::isLiquid);
      else
        assertThat(low.baseMaterialActions())
            .noneMatch(a -> a.name().contains("ESSENCE_") || a.isAlloy());
    }
  }

  @Test
  void keepsSupportedMaterialsVisibleBeforeTheirCurrentExecutionConditionsAreMet() {
    var solar = service.initial("solar", 82);
    assertThat(solar.state().rarity()).isEqualTo(ItemState.Rarity.NORMAL);
    assertThat(solar.baseMaterialActions())
        .contains(WorkbenchCurrency.GREATER_ESSENCE_BODY, WorkbenchCurrency.CATALYST_FLESH)
        .doesNotContain(
            WorkbenchCurrency.PERFECT_ESSENCE_BODY, WorkbenchCurrency.REFINED_CATALYST_FLESH);
    assertThat(service.initial("ruby", 82).baseMaterialActions())
        .contains(WorkbenchCurrency.REFINED_CATALYST_FLESH, WorkbenchCurrency.DILUTED_LIQUID_IRE)
        .doesNotContain(WorkbenchCurrency.CATALYST_FLESH, WorkbenchCurrency.GREATER_ESSENCE_BODY);
  }
}
