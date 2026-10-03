package com.poe2craft.crafting.domain;

import com.poe2craft.item.ItemState;

/** Reviewed ordinary empty sockets only. Exceptional/socketed-Augment states remain unsupported. */
public final class AugmentSocketRules {
  public static final String STOCKY_BASE_ID = "Metadata/Items/Armours/Gloves/FourGlovesStr1";

  private AugmentSocketRules() {}

  public static boolean supportedState(ItemState state) {
    return state.augmentSockets() == null
        || (state.baseItemId().equals(STOCKY_BASE_ID)
            && (state.augmentSockets() == 0 || state.augmentSockets() == 1));
  }

  public static String refusal(ItemState state) {
    if (!state.baseItemId().equals(STOCKY_BASE_ID))
      return "Artificer's Orb cannot add sockets to this Jewellery base.";
    if (state.augmentSockets() == null)
      return "This saved item's socket count is unknown. Place a new Stocky Mitts base.";
    if (state.augmentSockets() >= 1)
      return "This base already has its ordinary maximum of one Augment Socket.";
    return "";
  }
}
