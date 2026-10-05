package com.poe2craft.item;

import java.util.Map;

/**
 * Ordinary highest-tier Crossbow representatives; weapon properties and mechanics are display-only.
 */
public final class ReviewedCrossbows {
  public static final Map<String, String> BASES =
      Map.ofEntries(
          Map.entry(
              "siege-crossbow",
              "Metadata/Items/Weapons/TwoHandWeapons/Crossbows/FourCrossbow7Endgame"),
          Map.entry(
              "gemini-crossbow",
              "Metadata/Items/Weapons/TwoHandWeapons/Crossbows/FourCrossbow5Endgame"),
          Map.entry(
              "elegant-crossbow",
              "Metadata/Items/Weapons/TwoHandWeapons/Crossbows/FourCrossbow10Endgame"),
          Map.entry(
              "flexed-crossbow",
              "Metadata/Items/Weapons/TwoHandWeapons/Crossbows/FourCrossbow2Endgame"),
          Map.entry(
              "desolate-crossbow",
              "Metadata/Items/Weapons/TwoHandWeapons/Crossbows/FourCrossbow8Endgame"),
          Map.entry(
              "engraved-crossbow",
              "Metadata/Items/Weapons/TwoHandWeapons/Crossbows/FourCrossbow4Endgame"));

  private ReviewedCrossbows() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
