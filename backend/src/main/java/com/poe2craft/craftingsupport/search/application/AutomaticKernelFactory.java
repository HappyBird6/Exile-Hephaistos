package com.poe2craft.craftingsupport.search.application;

import com.poe2craft.craftingsupport.search.domain.AutomaticCraftingKernel;
import com.poe2craft.shared.crafting.application.CraftingCatalogs;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import java.util.Map;

/**
 * 수동 작업대와 같은 규칙으로 자동 탐색 커널을 만든다.
 *
 * <p>기존 계약: Builds automatic search kernels from the same reviewed rules used by the manual
 * workbench.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code crafting}: 제작 ({@code CraftingOperations}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AutomaticKernelFactory(crafting)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code crafting}(제작).
 *   <li>{@code create(state, policy)}: 새 작업 또는 값을 생성한다. 입력: {@code state}(상태), {@code policy}(정책).
 *       반환: {@code AutomaticCraftingKernel}.
 * </ul>
 */
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
