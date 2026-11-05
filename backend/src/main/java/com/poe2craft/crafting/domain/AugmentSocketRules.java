package com.poe2craft.crafting.domain;

import com.poe2craft.item.ItemState;

/** Workbench policy: ordinary capacity plus one; not an in-game Artificer guarantee. */
public final class AugmentSocketRules {
  public static final String STOCKY_BASE_ID = "Metadata/Items/Armours/Gloves/FourGlovesStr1";

  private AugmentSocketRules() {}

  public static boolean supportedState(ItemState state) {
    return state.augmentSockets() == null
        || (com.poe2craft.item.BaseRegistry.socketExecutionMaximum(state.baseItemId()) != null
            && state.augmentSockets() >= 0
            && state.augmentSockets()
                <= com.poe2craft.item.BaseRegistry.socketExecutionMaximum(state.baseItemId()));
  }

  public static String refusal(ItemState state) {
    if (state.observations() != null && state.observations().socketCount() != null)
      return "The imported socket count is preserved, but its Artificer capacity has not been established.";
    if (com.poe2craft.item.BaseRegistry.socketExecutionMaximum(state.baseItemId()) == null)
      return "Artificer's Orb socket rules are not supported for this equipment base.";
    if (state.augmentSockets() == null)
      return "This saved item's socket count is unknown. Place a new supported equipment base.";
    if (state.augmentSockets()
        >= com.poe2craft.item.BaseRegistry.socketExecutionMaximum(state.baseItemId()))
      return "This base has reached the workbench socket maximum (ordinary capacity plus one).";
    return "";
  }
}
