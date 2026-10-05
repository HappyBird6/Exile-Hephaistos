package com.poe2craft.item;

import java.util.Map;

/**
 * Ordinary endgame Quarterstaves and Spears with source-specific pools; combat effects are
 * display-only.
 */
public final class ReviewedQuarterstavesSpears {
  public static final Map<String, String> BASES =
      Map.ofEntries(
          Map.entry(
              "aegis-quarterstaff",
              "Metadata/Items/Weapons/TwoHandWeapons/Staves/FourQuarterstaff8Endgame"),
          Map.entry(
              "bolting-quarterstaff",
              "Metadata/Items/Weapons/TwoHandWeapons/Staves/FourQuarterstaff4Endgame"),
          Map.entry(
              "dreaming-quarterstaff",
              "Metadata/Items/Weapons/TwoHandWeapons/Staves/FourQuarterstaff10Endgame"),
          Map.entry(
              "grand-spear",
              "Metadata/Items/Weapons/OneHandWeapons/OneHandSpears/FourSpear8Endgame"),
          Map.entry(
              "flying-spear",
              "Metadata/Items/Weapons/OneHandWeapons/OneHandSpears/FourSpear5Endgame"),
          Map.entry(
              "akoyan-spear",
              "Metadata/Items/Weapons/OneHandWeapons/OneHandSpears/FourSpear10Endgame"));

  private ReviewedQuarterstavesSpears() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
