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

  public static boolean timeLost(String base) {
    return Set.of(
            "Metadata/Items/Jewels/JewelRadiusStr",
            "Metadata/Items/Jewels/JewelRadiusDex",
            "Metadata/Items/Jewels/JewelRadiusInt",
            "Metadata/Items/Jewels/JewelRadiusDiamond")
        .contains(base);
  }

  public static boolean supportedCrafting(String base) {
    return supported(base) || timeLost(base);
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
    return p + s <= 5 && (maxP + maxS > 4 ? p <= maxP && s <= maxS : p <= 3 && s <= 3);
  }

  /** Existing overflow occupies its own slots; it never increases the other side's cap. */
  public static int insertionCapacity(int p, int s, int maxP, int maxS) {
    return Math.min(5, maxP + maxS + Math.max(0, p - maxP) + Math.max(0, s - maxS));
  }

  /** User-specified Chaos rule: an existing over-cap side is immutable during replacement. */
  public static Set<ModifierDefinition.AffixType> chaosLockedSides(
      ItemState state, ItemCatalog catalog) {
    if (!supportedCrafting(state.baseItemId()) || state.rarity() != ItemState.Rarity.RARE)
      return Set.of();
    var slots = new ItemStateValidator(catalog).slots(state);
    var locked = java.util.EnumSet.noneOf(ModifierDefinition.AffixType.class);
    if (slots.usedPrefixes() > slots.maxPrefixes()) locked.add(ModifierDefinition.AffixType.PREFIX);
    if (slots.usedSuffixes() > slots.maxSuffixes()) locked.add(ModifierDefinition.AffixType.SUFFIX);
    return Set.copyOf(locked);
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
