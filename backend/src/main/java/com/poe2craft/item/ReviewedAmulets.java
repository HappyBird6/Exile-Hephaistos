package com.poe2craft.item;

import java.util.Map;

/** Distinct source-reviewed Amulet implicits; not the complete Amulet catalog. */
public final class ReviewedAmulets {
  public static final Map<String, String> BASES =
      Map.of(
          "stellar",
          "Metadata/Items/Amulets/FourAmulet8",
          "amber",
          "Metadata/Items/Amulets/FourAmulet3",
          "bloodstone",
          "Metadata/Items/Amulets/FourAmulet7",
          "lunar",
          "Metadata/Items/Amulets/FourAmulet6",
          "azure",
          "Metadata/Items/Amulets/FourAmulet2",
          "crimson",
          "Metadata/Items/Amulets/FourAmulet1",
          "pearlescent",
          "Metadata/Items/Amulets/FourAmulet11");

  private ReviewedAmulets() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }

  public static boolean reviewedImplicit(ModifierDefinition d) {
    return java.util.Set.of(
                "stellar:implicit:allattributes",
                "amber:implicit:strength",
                "bloodstone:implicit:increasedlife",
                "lunar:implicit:increasedenergyshield",
                "azure:implicit:manaregeneration",
                "crimson:implicit:liferegeneration",
                "pearlescent:implicit:allresistances")
            .contains(d.id())
        && d.layer() == ModifierDefinition.Layer.IMPLICIT;
  }
}
