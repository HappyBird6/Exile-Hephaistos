package com.poe2craft.item;

import java.util.Map;

/** Distinct source-reviewed Ring implicits; this is not the complete Ring catalog. */
public final class ReviewedRings {
  public static final Map<String, String> BASES = BaseRegistry.familyBases("rings");

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
