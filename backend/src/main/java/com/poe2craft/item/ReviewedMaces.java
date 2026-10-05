package com.poe2craft.item;

import java.util.Map;

/** Ordinary representative endgame Maces; separate class pools retain canonical weapon stats. */
public final class ReviewedMaces {
  public static final Map<String, String> BASES =
      Map.of(
          "fortified-hammer",
              "Metadata/Items/Weapons/OneHandWeapons/OneHandMaces/FourOneHandMace8Endgame",
          "strife-pick",
              "Metadata/Items/Weapons/OneHandWeapons/OneHandMaces/FourOneHandMace5Endgame",
          "akoyan-club",
              "Metadata/Items/Weapons/OneHandWeapons/OneHandMaces/FourOneHandMace10Endgame",
          "ruination-maul",
              "Metadata/Items/Weapons/TwoHandWeapons/TwoHandMaces/FourTwoHandMace8Endgame",
          "fanatic-greathammer",
              "Metadata/Items/Weapons/TwoHandWeapons/TwoHandMaces/FourTwoHandMace5Endgame",
          "tawhoan-greatclub",
              "Metadata/Items/Weapons/TwoHandWeapons/TwoHandMaces/FourTwoHandMace10Endgame");

  private ReviewedMaces() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
