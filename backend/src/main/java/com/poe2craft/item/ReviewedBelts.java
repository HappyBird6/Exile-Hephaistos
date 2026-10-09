package com.poe2craft.item;

import java.util.Map;

/** Distinct source-reviewed Belt implicits; Charm slot range remains an unsupplied property. */
public final class ReviewedBelts {
  public static final Map<String, String> BASES = BaseRegistry.familyBases("belts");

  private ReviewedBelts() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
