package com.poe2craft.crafting.domain;

import com.poe2craft.item.ItemState;

/** Reviewed ordinary empty sockets only. Exceptional/socketed-Augment states remain unsupported. */
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
    if (com.poe2craft.item.BaseRegistry.socketExecutionMaximum(state.baseItemId()) == null)
      return "Artificer's Orb socket rules are not supported for this equipment base.";
    if (state.augmentSockets() == null)
      return "This saved item's socket count is unknown. Place a new Stocky Mitts base.";
    if (state.augmentSockets()
        >= com.poe2craft.item.BaseRegistry.socketExecutionMaximum(state.baseItemId()))
      return "This base already has its ordinary maximum of one Augment Socket.";
    return "";
  }
}
