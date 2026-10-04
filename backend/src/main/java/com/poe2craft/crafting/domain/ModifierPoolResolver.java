package com.poe2craft.crafting.domain;

import com.poe2craft.item.ItemCatalog;
import com.poe2craft.item.ItemState;
import com.poe2craft.item.ModifierDefinition;
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
    int p =
        state.rarity() == ItemState.Rarity.MAGIC
            ? catalog.base().magicPrefixes()
            : catalog.base().rarePrefixes();
    int s =
        state.rarity() == ItemState.Rarity.MAGIC
            ? catalog.base().magicSuffixes()
            : catalog.base().rareSuffixes();
    if (com.poe2craft.item.BasicJewel.supportedCrafting(state.baseItemId())
        && state.rarity() == ItemState.Rarity.RARE) {
      p +=
          com.poe2craft.item.BasicJewel.extra(
              state.modifierIds(), ModifierDefinition.AffixType.PREFIX);
      s +=
          com.poe2craft.item.BasicJewel.extra(
              state.modifierIds(), ModifierDefinition.AffixType.SUFFIX);
    }
    if (prefixCount >= p) candidates.andNot(prefixes);
    if (suffixCount >= s) candidates.andNot(suffixes);
    if (com.poe2craft.item.BasicJewel.supportedCrafting(state.baseItemId())
        && prefixCount + suffixCount >= p + s) candidates.clear();
    var result = candidates.stream().mapToObj(definitions::get).toList();
    return new Pool(result, result.stream().mapToLong(ModifierDefinition::weight).sum());
  }

  public record Pool(List<ModifierDefinition> candidates, long totalWeight) {}
}
