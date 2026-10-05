package com.poe2craft.item;

import java.util.Map;

/**
 * Reviewed independent Shield, Buckler and Focus classes; built-in properties and skills are
 * display-only.
 */
public final class ReviewedOffhands {
  public static final Map<String, String> BASES =
      Map.ofEntries(
          Map.entry(
              "tawhoan-tower-shield", "Metadata/Items/Armours/Shields/FourShieldStr10Endgame"),
          Map.entry("golden-targe", "Metadata/Items/Armours/Shields/FourShieldStrDex10Endgame"),
          Map.entry(
              "blacksteel-crest-shield", "Metadata/Items/Armours/Shields/FourShieldStrInt7Endgame"),
          Map.entry("desert-buckler", "Metadata/Items/Armours/Shields/FourShieldDex9Endgame"),
          Map.entry("tasalian-focus", "Metadata/Items/Armours/Focii/FourFocus10Endgame"));

  private ReviewedOffhands() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
