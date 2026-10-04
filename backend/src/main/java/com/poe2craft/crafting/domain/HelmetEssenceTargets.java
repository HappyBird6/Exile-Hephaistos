package com.poe2craft.crafting.domain;

/** Source-matched Rusted Greathelm explicit affixes; base Armour is display-only. */
public final class HelmetEssenceTargets {
  public static final String BASE_ID = "Metadata/Items/Armours/Helmets/FourHelmetStr1";

  private HelmetEssenceTargets() {}

  public static boolean supports(String id) {
    return id.equals(BASE_ID) || id.equals("Metadata/Items/Armours/Helmets/FourHelmetStr7Endgame");
  }
}
