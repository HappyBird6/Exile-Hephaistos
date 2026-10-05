package com.poe2craft.item;

import java.util.Map;

/** Reviewed ordinary Sceptre skill families; innate skill is display-only. */
public final class ReviewedSceptres {
  public static final Map<String, String> BASES =
      Map.ofEntries(
          Map.entry("hallowed", "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre13"),
          Map.entry("stoic", "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre2"),
          Map.entry("omen", "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre4"),
          Map.entry("shrine-fire", "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre6a"),
          Map.entry("shrine-ice", "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre6b"),
          Map.entry(
              "shrine-lightning", "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre6c"),
          Map.entry("clasped", "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre8"),
          Map.entry("wrath", "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre10"));

  private ReviewedSceptres() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
