package com.poe2craft.item;

import java.util.Map;

/** Ordinary representative endgame Maces; separate class pools retain canonical weapon stats. */
public final class ReviewedMaces {
  public static final Map<String, String> BASES =
      BaseRegistry.familyBases("one-hand-maces", "two-hand-maces");

  private ReviewedMaces() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
