package com.poe2craft.item;

import java.util.Map;

/** Distinct source-reviewed Ring implicits; this is not the complete Ring catalog. */
public final class ReviewedRings {
  public static final Map<String, String> BASES =
      Map.of(
          "kinetic",
          "Metadata/Items/Rings/FourRingB4",
          "vitalic",
          "Metadata/Items/Rings/FourRingB2",
          "mnemonic",
          "Metadata/Items/Rings/FourRingB3",
          "pearl",
          "Metadata/Items/Rings/FourRing8",
          "amethyst",
          "Metadata/Items/Rings/FourRing6",
          "prismatic",
          "Metadata/Items/Rings/FourRing9",
          "ruby-ring",
          "Metadata/Items/Rings/FourRing3",
          "two-stone-fire-cold",
          "Metadata/Items/Rings/FourRing13a");

  private ReviewedRings() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }

  public static boolean reviewedImplicit(ModifierDefinition d) {
    return java.util.Set.of(
                "kinetic:implicit:physicaldamage",
                "vitalic:implicit:increasedlife",
                "mnemonic:implicit:maximummanaincreasepercent",
                "pearl:implicit:increasedcastspeed",
                "amethyst:implicit:chaosresistance",
                "prismatic:implicit:allresistances",
                "ruby-ring:implicit:fireresistance",
                "two-stone-fire-cold:implicit:fireandcoldresistance")
            .contains(d.id())
        && d.layer() == ModifierDefinition.Layer.IMPLICIT;
  }
}
