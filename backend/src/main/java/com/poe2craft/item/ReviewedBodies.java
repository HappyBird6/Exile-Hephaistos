package com.poe2craft.item;

import java.util.Map;

/** Source-reviewed highest-tier ordinary Body defence archetypes. */
public final class ReviewedBodies {
  public static final Map<String, String> BASES =
      Map.of(
          "slipstrike", "Metadata/Items/Armours/BodyArmours/FourBodyDex6Endgame",
          "death-mail", "Metadata/Items/Armours/BodyArmours/FourBodyStrDex6Endgame",
          "sleek", "Metadata/Items/Armours/BodyArmours/FourBodyDexInt2Endgame",
          "vile", "Metadata/Items/Armours/BodyArmours/FourBodyInt3Endgame",
          "wolfskin", "Metadata/Items/Armours/BodyArmours/FourBodyStrInt2Endgame");

  private ReviewedBodies() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
