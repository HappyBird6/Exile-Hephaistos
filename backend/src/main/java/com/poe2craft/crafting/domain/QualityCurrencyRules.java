package com.poe2craft.crafting.domain;

import com.poe2craft.item.ItemState;

/** Ordinary quality currencies; source rolls and inherited exceptional quality are preserved. */
public final class QualityCurrencyRules {
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
    if (!state.conditions().isEmpty()) return "This item cannot be modified.";
    boolean armour = state.baseItemId().startsWith("Metadata/Items/Armours/");
    boolean martial = martial(state.baseItemId());
    if ((action == WorkbenchCurrency.ARMOURERS_SCRAP && !armour)
        || (action == WorkbenchCurrency.BLACKSMITHS_WHETSTONE && !martial))
      return "This quality currency does not support this item class.";
    if (state.equipmentQuality() == null) return "The item's current equipment quality is unknown.";
    if (state.equipmentQuality() >= 20)
      return "This item has reached the ordinary quality currency limit.";
    return "The per-use equipment quality increment has not been verified.";
  }
}
