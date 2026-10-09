package com.poe2craft.item;

import java.util.Map;

/**
 * Ordinary endgame Quarterstaves and Spears with source-specific pools; combat effects are
 * display-only.
 */
public final class ReviewedQuarterstavesSpears {
  public static final Map<String, String> BASES =
      BaseRegistry.familyBases("quarterstaves", "spears");

  private ReviewedQuarterstavesSpears() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
