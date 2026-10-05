package com.poe2craft.item;

import java.util.Map;

/** Source-reviewed highest-tier ordinary Body defence archetypes. */
public final class ReviewedBodies {
  public static final Map<String, String> BASES = BaseRegistry.familyBases("body");

  private ReviewedBodies() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
