package com.poe2craft.item;

import java.util.Map;

/** Source-reviewed highest-tier ordinary Helmet defence archetypes. */
public final class ReviewedHelmets {
  public static final Map<String, String> BASES =
      Map.of(
          "freebooter",
          "Metadata/Items/Armours/Helmets/FourHelmetDex7Endgame",
          "gladiatorial",
          "Metadata/Items/Armours/Helmets/FourHelmetStrDex6Endgame",
          "grinning",
          "Metadata/Items/Armours/Helmets/FourHelmetDexInt6Endgame",
          "ancestral",
          "Metadata/Items/Armours/Helmets/FourHelmetInt8Endgame",
          "cryptic",
          "Metadata/Items/Armours/Helmets/FourHelmetStrInt6Endgame");

  private ReviewedHelmets() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
