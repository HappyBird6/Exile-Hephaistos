package com.poe2craft.item;

import java.util.Map;

/** Source-reviewed highest-tier ordinary Boots defence archetypes. */
public final class ReviewedBoots {
  public static final Map<String, String> BASES = BaseRegistry.familyBases("boots");

  private ReviewedBoots() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
