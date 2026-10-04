package com.poe2craft.item;

import java.util.Map;

/** Source-reviewed ordinary endgame Bow categories; local properties are display data. */
public final class ReviewedBows {
  public static final Map<String, String> BASES =
      Map.of(
          "warmonger", "Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow8Endgame",
          "guardian", "Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow3Endgame",
          "gemini", "Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow6Endgame",
          "fanatic", "Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow7Endgame",
          "obliterator", "Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow9Endgame");

  private ReviewedBows() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
