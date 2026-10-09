package com.poe2craft.item;

import java.util.Map;

/**
 * Reviewed independent Shield, Buckler and Focus classes; built-in properties and skills are
 * display-only.
 */
public final class ReviewedOffhands {
  public static final Map<String, String> BASES =
      BaseRegistry.familyBases("shields", "bucklers", "foci");

  private ReviewedOffhands() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
