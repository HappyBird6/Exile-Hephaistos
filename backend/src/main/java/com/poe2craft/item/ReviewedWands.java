package com.poe2craft.item;

import java.util.Map;

/** Source-reviewed ordinary Wand skill families; granted skills are display-only. */
public final class ReviewedWands {
  public static final Map<String, String> BASES = BaseRegistry.familyBases("wands");

  private ReviewedWands() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
