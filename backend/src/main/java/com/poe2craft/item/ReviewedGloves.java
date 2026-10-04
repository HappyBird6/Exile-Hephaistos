package com.poe2craft.item;

import java.util.Map;

/** Source-reviewed endgame Gloves; no socket or Runeforged scope is inherited. */
public final class ReviewedGloves {
  public static final Map<String, String> BASES =
      Map.of(
          "massive", "Metadata/Items/Armours/Gloves/FourGlovesStr6Endgame",
          "sirenscale", "Metadata/Items/Armours/Gloves/FourGlovesInt6Endgame",
          "adherent", "Metadata/Items/Armours/Gloves/FourGlovesStrInt4Endgame");

  private ReviewedGloves() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
