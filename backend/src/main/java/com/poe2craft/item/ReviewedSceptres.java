package com.poe2craft.item;

import java.util.Map;

/** Reviewed representative of the Skeletal Warrior family; innate skill is display-only. */
public final class ReviewedSceptres {
  public static final Map<String, String> BASES =
      Map.of("hallowed", "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre13");

  private ReviewedSceptres() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
