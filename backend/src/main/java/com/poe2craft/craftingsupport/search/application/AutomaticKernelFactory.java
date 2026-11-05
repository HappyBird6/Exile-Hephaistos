package com.poe2craft.craftingsupport.search.application;

import com.poe2craft.craftingsupport.search.domain.AutomaticCraftingKernel;
import com.poe2craft.shared.crafting.application.CraftingCatalogs;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import java.util.Map;

/** Builds automatic search kernels from the same reviewed rules used by the manual workbench. */
public final class AutomaticKernelFactory {
  private final CraftingOperations crafting;

  public AutomaticKernelFactory(CraftingOperations crafting) {
    this.crafting = java.util.Objects.requireNonNull(crafting);
  }

  /** Shared reviewed rules, without applying a manual crafting action. */
  public AutomaticCraftingKernel create(ItemState state, AutomaticCurrencyPolicy policy) {
    var catalog =
        crafting.reviewedCatalogs().stream()
            .filter(c -> c.base().id().equals(state.baseItemId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported crafting base"));
    var family =
        BaseRegistry.topBases().values().stream()
            .filter(b -> b.id().equals(state.baseItemId()))
            .map(BaseRegistry.Base::family)
            .findFirst()
            .orElseGet(
                () -> {
                  if (state.baseItemId().equals(SolarAmulet.BASE_ID)) return "amulets";
                  return Map.of(
                          CraftingCatalogs.STOCKY_BASE_ID,
                          "gloves",
                          RingEssenceTargets.BASE_ID,
                          "rings",
                          HelmetEssenceTargets.BASE_ID,
                          "helmets",
                          BeltEssenceTargets.BASE_ID,
                          "belts",
                          SceptreEssenceTargets.BASE_ID,
                          "sceptres",
                          BodyEssenceTargets.BASE_ID,
                          "body-armours",
                          WandEssenceTargets.BASE_ID,
                          "wands",
                          BowEssenceTargets.BASE_ID,
                          "bows")
                      .getOrDefault(state.baseItemId(), "unsupported");
                });
    return new AutomaticCraftingKernel(policy, catalog, crafting.simulator(state), family);
  }
}
