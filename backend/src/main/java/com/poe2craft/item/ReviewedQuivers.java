package com.poe2craft.item;

import java.util.Map;

/** Ordinary distinct-implicit Quiver representatives; effects are display-only. */
public final class ReviewedQuivers {
  public static final Map<String, String> BASES = BaseRegistry.familyBases("quivers");

  private ReviewedQuivers() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
