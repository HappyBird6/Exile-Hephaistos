package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pre-indexes eligibility and family conflicts once per immutable snapshot. */
public final class ModifierPoolResolver {
  private final ItemCatalog catalog;
  private final List<ModifierDefinition> definitions;
  private final BitSet[] levels = new BitSet[101];
  private final BitSet prefixes = new BitSet();
  private final BitSet suffixes = new BitSet();
  private final Map<String, BitSet> families = new HashMap<>();

  public ModifierPoolResolver(ItemCatalog catalog) {
    this.catalog = catalog;
    definitions =
        catalog.modifiers().values().stream()
            .filter(d -> d.layer() == ModifierDefinition.Layer.EXPLICIT)
            .toList();
    for (int level = 1; level <= 100; level++) {
      levels[level] = new BitSet();
      for (int i = 0; i < definitions.size(); i++) {
        var d = definitions.get(i);
        if (d.requiredItemLevel() <= level && d.weight() > 0) levels[level].set(i);
      }
    }
    for (int i = 0; i < definitions.size(); i++) {
      var d = definitions.get(i);
      (d.affixType() == ModifierDefinition.AffixType.PREFIX ? prefixes : suffixes).set(i);
      for (String family : d.familyIds())
        families.computeIfAbsent(family, key -> new BitSet()).set(i);
    }
  }

  public Pool resolve(StateBucket state) {
    if (state.rarity() != ItemState.Rarity.MAGIC && state.rarity() != ItemState.Rarity.RARE) {
      return new Pool(List.of(), 0);
    }
    var candidates = (BitSet) levels[state.itemLevel()].clone();
    int prefixCount = 0, suffixCount = 0;
    for (String id : state.modifierIds()) {
      var d = catalog.find(id).orElseThrow();
      if (d.affixType() == ModifierDefinition.AffixType.PREFIX) prefixCount++;
      else suffixCount++;
      for (String family : d.familyIds()) {
        var blocked = families.get(family);
        if (blocked != null) candidates.andNot(blocked);
      }
    }
    var limits = limits(state);
    int p = limits[0], s = limits[1];
    if (prefixCount >= p) candidates.andNot(prefixes);
    if (suffixCount >= s) candidates.andNot(suffixes);
    if (BasicJewel.supportedCrafting(state.baseItemId())) {
      int totalCapacity =
          state.rarity() == ItemState.Rarity.RARE
              ? BasicJewel.insertionCapacity(prefixCount, suffixCount, p, s)
              : p + s;
      if (prefixCount + suffixCount >= totalCapacity) candidates.clear();
    }
    var result = candidates.stream().mapToObj(definitions::get).toList();
    return new Pool(result, result.stream().mapToLong(ModifierDefinition::weight).sum());
  }

  /** Common level/family/capacity diagnostics, also used for fixed reference outcomes. */
  public List<String> exclusions(
      StateBucket state,
      ModifierDefinition definition,
      int reservedPrefixes,
      int reservedSuffixes,
      boolean replacingReserved) {
    var reasons = new java.util.ArrayList<String>();
    if (definition.requiredItemLevel() > state.itemLevel()) reasons.add("ITEM_LEVEL");
    var existing = state.modifierIds().stream().map(id -> catalog.find(id).orElseThrow()).toList();
    if (existing.stream()
        .anyMatch(d -> !java.util.Collections.disjoint(d.familyIds(), definition.familyIds())))
      reasons.add("FAMILY_OCCUPIED");
    int prefixes =
        reservedPrefixes
            + (int)
                existing.stream()
                    .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
                    .count();
    int suffixes = reservedSuffixes + existing.size() - (prefixes - reservedPrefixes);
    var limits = limits(state);
    int p = limits[0], s = limits[1];
    if (!replacingReserved
        && ((definition.affixType() == ModifierDefinition.AffixType.PREFIX
                ? prefixes >= p
                : suffixes >= s)
            || (BasicJewel.supportedCrafting(state.baseItemId())
                && prefixes + suffixes
                    >= (state.rarity() == ItemState.Rarity.RARE
                        ? BasicJewel.insertionCapacity(prefixes, suffixes, p, s)
                        : p + s)))) reasons.add("AFFIX_FULL");
    return List.copyOf(reasons);
  }

  private int[] limits(StateBucket state) {
    int p =
        state.rarity() == ItemState.Rarity.MAGIC
            ? catalog.base().magicPrefixes()
            : catalog.base().rarePrefixes();
    int s =
        state.rarity() == ItemState.Rarity.MAGIC
            ? catalog.base().magicSuffixes()
            : catalog.base().rareSuffixes();
    if (BasicJewel.supportedCrafting(state.baseItemId())
        && state.rarity() == ItemState.Rarity.RARE) {
      p += BasicJewel.extra(state.modifierIds(), ModifierDefinition.AffixType.PREFIX);
      s += BasicJewel.extra(state.modifierIds(), ModifierDefinition.AffixType.SUFFIX);
    }
    return new int[] {p, s};
  }

  public record Pool(List<ModifierDefinition> candidates, long totalWeight) {}
}
