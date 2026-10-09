package com.poe2craft.crafting.domain;

import java.util.List;
import java.util.Map;

/** Individually verified Rusted Cuirass results; Delirium's Notable pool is not modeled. */
public final class BodyEssenceTargets {
  public static final String BASE_ID = "Metadata/Items/Armours/BodyArmours/FourBodyStr1";
  public static final Map<WorkbenchCurrency, List<String>> REPLACEMENTS =
      Map.of(
          WorkbenchCurrency.PERFECT_ESSENCE_BODY,
          List.of("rusted-cuirass:prefix:essence-maximum-life-percent"),
          WorkbenchCurrency.PERFECT_ESSENCE_RUIN,
          List.of("rusted-cuirass:prefix:essence-physical-taken-as-chaos"),
          WorkbenchCurrency.PERFECT_ESSENCE_SEEKING,
          List.of("rusted-cuirass:suffix:essence-reduced-incoming-critical-damage"));

  private BodyEssenceTargets() {}

  public static boolean supports(String id) {
    return com.poe2craft.item.ReviewedBodies.supports(id)
        || id.equals(BASE_ID)
        || id.equals("Metadata/Items/Armours/BodyArmours/FourBodyStr3Endgame");
  }
}
