package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.AugmentCraftingRules;
import com.poe2craft.shared.item.ItemState;

/** User-selected ordinary equipment quality policy; inherited exceptional quality is preserved. */
public final class QualityCurrencyRules {
  public static final String VERSION = "user-selected-equipment-quality-maximum-v2";
  public static final int ORDINARY_MAXIMUM = 20;

  private QualityCurrencyRules() {}

  public static boolean equipment(String baseItemId) {
    return baseItemId.startsWith("Metadata/Items/Armours/")
        || baseItemId.startsWith("Metadata/Items/Weapons/");
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
    if (state.equipmentQuality() >= maximum(state))
      return "This item has reached the ordinary quality currency limit.";
    return "";
  }

  public static int maximum(ItemState state) {
    return Math.max(ORDINARY_MAXIMUM, AugmentCraftingRules.maximumQuality(state));
  }

  /** One simulator use reaches the equipped quality cap. Existing over-cap quality is preserved. */
  public static int nextQuality(ItemState state) {
    if (state.equipmentQuality() == null || state.equipmentQuality() >= maximum(state))
      throw new IllegalArgumentException(
          "Known equipment quality below the active limit is required");
    return maximum(state);
  }
}
