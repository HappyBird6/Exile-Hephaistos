package com.poe2craft.item;

import java.util.Map;

/** Distinct source-reviewed Belt implicits; Charm slot range remains an unsupplied property. */
public final class ReviewedBelts {
  public static final Map<String, String> BASES =
      Map.ofEntries(
          Map.entry("linen-belt", "Metadata/Items/Belts/FourBelt2"),
          Map.entry("wide-belt", "Metadata/Items/Belts/FourBelt3"),
          Map.entry("long-belt", "Metadata/Items/Belts/FourBelt4"),
          Map.entry("plate-belt", "Metadata/Items/Belts/FourBelt5"),
          Map.entry("ornate-belt", "Metadata/Items/Belts/FourBelt6"),
          Map.entry("mail-belt", "Metadata/Items/Belts/FourBelt7"),
          Map.entry("double-belt", "Metadata/Items/Belts/FourBelt8"),
          Map.entry("heavy-belt", "Metadata/Items/Belts/FourBelt9"),
          Map.entry("utility-belt", "Metadata/Items/Belts/FourBelt10"),
          Map.entry("fine-belt", "Metadata/Items/Belts/FourBelt11"),
          Map.entry("invoking-belt", "Metadata/Items/Belts/FourBeltB2"),
          Map.entry("sinew-belt", "Metadata/Items/Belts/FourBeltB3"),
          Map.entry("forking-belt", "Metadata/Items/Belts/FourBeltB4"));

  private ReviewedBelts() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
