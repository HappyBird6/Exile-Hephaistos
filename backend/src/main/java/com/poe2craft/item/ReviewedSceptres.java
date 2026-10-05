package com.poe2craft.item;

import java.util.Map;

/** Reviewed ordinary Sceptre skill families; innate skill is display-only. */
public final class ReviewedSceptres {
  public static final Map<String, String> BASES = BaseRegistry.familyBases("sceptres");

  private ReviewedSceptres() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
