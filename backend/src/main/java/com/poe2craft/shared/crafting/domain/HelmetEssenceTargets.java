package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ReviewedHelmets;

/** Source-matched Rusted Greathelm explicit affixes; base Armour is display-only. */
public final class HelmetEssenceTargets {
  public static final String BASE_ID = "Metadata/Items/Armours/Helmets/FourHelmetStr1";

  private HelmetEssenceTargets() {}

  public static boolean supports(String id) {
    return ReviewedHelmets.supports(id)
        || id.equals(BASE_ID)
        || id.equals("Metadata/Items/Armours/Helmets/FourHelmetStr7Endgame");
  }
}
