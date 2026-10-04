package com.poe2craft.item;

import java.util.Collection;
import java.util.Set;

/** Reviewed Basic Jewel identities and persistent Crafted mechanics; no passive-tree simulation. */
public final class BasicJewel {
  private static final Set<String> BASES =
      Set.of(
          "Metadata/Items/Jewels/JewelStr",
          "Metadata/Items/Jewels/JewelDex",
          SapphireJewel.BASE_ID,
          "Metadata/Items/Jewels/JewelDiamond");

  private BasicJewel() {}

  public static boolean supported(String base) {
    return BASES.contains(base);
  }

  public static int extra(Collection<String> ids, ModifierDefinition.AffixType side) {
    String code =
        side == ModifierDefinition.AffixType.PREFIX
            ? ":crafted:CraftedJewelAdditionalPrefixAllowed"
            : ":crafted:CraftedJewelAdditionalSuffixAllowed";
    return ids.stream().anyMatch(id -> id.endsWith(code)) ? 1 : 0;
  }

  /** Cap loss preserves reachable existing overflow, while insertion still uses current caps. */
  public static boolean existingCapacity(int p, int s, int maxP, int maxS) {
    return maxP + maxS > 4 ? p <= maxP && s <= maxS : p <= 3 && s <= 3 && p + s <= 4;
  }

  public static long effect(ItemState state, ModifierDefinition.AffixType side) {
    String code =
        side == ModifierDefinition.AffixType.PREFIX
            ? ":crafted:CraftedJewelPrefixEffect"
            : ":crafted:CraftedJewelSuffixEffect";
    return state.explicits().stream()
        .filter(m -> m.modifierId().endsWith(code))
        .mapToLong(m -> m.values().get("display_source_value"))
        .findFirst()
        .orElse(0);
  }
}
