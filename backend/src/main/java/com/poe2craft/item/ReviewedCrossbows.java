package com.poe2craft.item;

import java.util.Map;

/**
 * Ordinary highest-tier Crossbow representatives; weapon properties and mechanics are display-only.
 */
public final class ReviewedCrossbows {
  public static final Map<String, String> BASES = BaseRegistry.familyBases("crossbows");

  private ReviewedCrossbows() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
