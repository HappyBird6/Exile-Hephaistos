package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.item.ItemState;

/** Reviewed ordinary empty sockets only. Exceptional/socketed-Augment states remain unsupported. */
public final class AugmentSocketRules {
  public static final String STOCKY_BASE_ID = "Metadata/Items/Armours/Gloves/FourGlovesStr1";

  private AugmentSocketRules() {}

  public static boolean supportedState(ItemState state) {
    return state.augmentSockets() == null
        || (BaseRegistry.socketExecutionMaximum(state.baseItemId()) != null
            && state.augmentSockets() >= 0
            && state.augmentSockets() <= BaseRegistry.socketExecutionMaximum(state.baseItemId()));
  }

  public static String refusal(ItemState state) {
    if (state.observations() != null && state.observations().socketCount() != null)
      return "The imported socket count is preserved, but its Artificer capacity has not been established.";
    if (BaseRegistry.socketExecutionMaximum(state.baseItemId()) == null)
      return "Artificer's Orb socket rules are not supported for this equipment base.";
    if (state.augmentSockets() == null)
      return "This saved item's socket count is unknown. Place a new Stocky Mitts base.";
    if (state.augmentSockets() >= BaseRegistry.socketExecutionMaximum(state.baseItemId()))
      return "This base already has its ordinary maximum of one Augment Socket.";
    return "";
  }
}
