package com.poe2craft.crafting.domain;

import com.poe2craft.item.ItemState;

/** User-selected ordinary equipment quality policy; inherited exceptional quality is preserved. */
public final class QualityCurrencyRules {
  public static final String VERSION = "user-selected-equipment-quality-maximum-v2";
  public static final int ORDINARY_MAXIMUM = 20;

  private QualityCurrencyRules() {}

  public static boolean equipment(String baseItemId) {
    return baseItemId.startsWith("Metadata/Items/Armours/") || martial(baseItemId);
  }

  private static boolean martial(String baseItemId) {
    return baseItemId.startsWith("Metadata/Items/Weapons/")
        && !baseItemId.contains("/Wands/")
        && !baseItemId.contains("/Staves/")
        && !baseItemId.contains("/Sceptres/");
  }

  public static boolean isQuality(WorkbenchCurrency action) {
    return action == WorkbenchCurrency.ARMOURERS_SCRAP
        || action == WorkbenchCurrency.BLACKSMITHS_WHETSTONE;
  }

  public static String refusal(ItemState state, WorkbenchCurrency action) {
    if (!isQuality(action)) return "A supported equipment quality currency is required.";
    if (!state.conditions().isEmpty()) return "This item cannot be modified.";
    if (state.rarity() == ItemState.Rarity.UNIQUE)
      return "Unique equipment remains outside the supported quality scope.";
    boolean armour = state.baseItemId().startsWith("Metadata/Items/Armours/");
    boolean martial = martial(state.baseItemId());
    if ((action == WorkbenchCurrency.ARMOURERS_SCRAP && !armour)
        || (action == WorkbenchCurrency.BLACKSMITHS_WHETSTONE && !martial))
      return "This quality currency does not support this item class.";
    if (state.equipmentQuality() == null) return "The item's current equipment quality is unknown.";
    if (state.equipmentQuality() >= ORDINARY_MAXIMUM)
      return "This item has reached the ordinary quality currency limit.";
    return "";
  }

  /**
   * One simulator use reaches the ordinary equipment cap, not an actual-game usage rule. The
   * source-reviewed 40 cap belongs to maximum-quality Amulets and does not extend this scope.
   */
  public static int nextQuality(ItemState state) {
    if (state.equipmentQuality() == null || state.equipmentQuality() >= ORDINARY_MAXIMUM)
      throw new IllegalArgumentException("Known equipment quality below 20 is required");
    return ORDINARY_MAXIMUM;
  }
}
