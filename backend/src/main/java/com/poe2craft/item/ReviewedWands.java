package com.poe2craft.item;

import java.util.Map;

/** Source-reviewed ordinary Wand skill families; granted skills are display-only. */
public final class ReviewedWands {
  public static final Map<String, String> BASES =
      Map.ofEntries(
          Map.entry("bone", "Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand2"),
          Map.entry("siphoning", "Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand4"),
          Map.entry("volatile", "Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand5"),
          Map.entry("galvanic", "Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand6"),
          Map.entry("acrid", "Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand7"),
          Map.entry("offering", "Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand8"),
          Map.entry("critical", "Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand11"),
          Map.entry("primordial", "Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand12"),
          Map.entry("dueling", "Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand13"));

  private ReviewedWands() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
