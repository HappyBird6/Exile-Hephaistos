package com.poe2craft.item;

import java.util.Map;

/** Source-reviewed ordinary endgame Bow categories; local properties are display data. */
public final class ReviewedBows {
  public static final Map<String, String> BASES = BaseRegistry.familyBases("bows");

  private ReviewedBows() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
