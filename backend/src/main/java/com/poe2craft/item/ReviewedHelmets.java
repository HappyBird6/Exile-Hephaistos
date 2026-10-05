package com.poe2craft.item;

import java.util.Map;

/** Source-reviewed highest-tier ordinary Helmet defence archetypes. */
public final class ReviewedHelmets {
  public static final Map<String, String> BASES = BaseRegistry.familyBases("helmets");

  private ReviewedHelmets() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
