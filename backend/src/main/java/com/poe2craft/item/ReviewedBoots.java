package com.poe2craft.item;

import java.util.Map;

/** Source-reviewed highest-tier ordinary Boots defence archetypes. */
public final class ReviewedBoots {
  public static final Map<String, String> BASES =
      Map.of(
          "tasalian", "Metadata/Items/Armours/Boots/FourBootsStr6Endgame",
          "drakeskin", "Metadata/Items/Armours/Boots/FourBootsDex6Endgame",
          "sekhema", "Metadata/Items/Armours/Boots/FourBootsInt6Endgame",
          "blacksteel-boots", "Metadata/Items/Armours/Boots/FourBootsStrDex4Endgame",
          "faithful", "Metadata/Items/Armours/Boots/FourBootsStrInt1Endgame",
          "daggerfoot", "Metadata/Items/Armours/Boots/FourBootsDexInt4Endgame");

  private ReviewedBoots() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
