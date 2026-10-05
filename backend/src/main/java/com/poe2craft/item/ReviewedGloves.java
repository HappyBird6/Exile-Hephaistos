package com.poe2craft.item;

import java.util.Map;

/** Source-reviewed endgame Gloves; no socket or Runeforged scope is inherited. */
public final class ReviewedGloves {
  public static final Map<String, String> BASES = BaseRegistry.familyBases("gloves");

  private ReviewedGloves() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
