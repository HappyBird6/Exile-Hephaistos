package com.poe2craft.shared.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.ruleset.SealedResources;
import java.util.*;

/**
 * Reviewed socket-dependent limits and pools; equipped identities, never display text, select
 * rules.
 */
public final class AugmentCraftingRules {
  private AugmentCraftingRules() {}

  public record Effect(
      int craftedBonus,
      int suffixBonus,
      int maximumQuality,
      int runeEffectPercent,
      boolean idolBonded,
      String marker,
      String pool) {}

  public record Document(
      int schemaVersion,
      Set<String> socketBound,
      Set<String> craftedModifierIds,
      List<ModifierDefinition> modifiers,
      Map<String, Map<String, Map<String, Integer>>> pools,
      Map<String, Effect> effects,
      List<ModifierMagnitudes.Rule> magnitudeRules) {}

  private static final Effect NONE = new Effect(0, 0, 0, 0, false, null, null);
  private static final Document DATA = load();

  private static Document load() {
    try (var input = SealedResources.open("/catalog/augment-crafting.json")) {
      var result = new ObjectMapper().readValue(input, Document.class);
      if (result.schemaVersion() != 1
          || result.modifiers().stream()
              .anyMatch(d -> d.weight() != 0 || !d.tags().contains("augment_pool")))
        throw new IllegalArgumentException("Invalid Augment crafting registry");
      return result;
    } catch (java.io.IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static List<ModifierMagnitudes.Rule> magnitudeRules() {
    return DATA.magnitudeRules();
  }

  public static Effect effect(String id) {
    return DATA.effects().getOrDefault(id, NONE);
  }

  public static boolean socketBound(String id) {
    return DATA.socketBound().contains(id);
  }

  public static int suffixBonus(ItemState state) {
    return state.installedAugments().stream()
        .mapToInt(a -> effect(a.augmentId()).suffixBonus())
        .sum();
  }

  public static int craftedLimit(ItemState state) {
    return 1
        + state.installedAugments().stream()
            .mapToInt(a -> effect(a.augmentId()).craftedBonus())
            .sum();
  }

  public static boolean crafted(ModifierInstance modifier, ItemCatalog catalog) {
    return modifier.crafted()
        || DATA.craftedModifierIds().contains(modifier.modifierId())
        || catalog.find(modifier.modifierId()).map(d -> d.tags().contains("crafted")).orElse(false);
  }

  public static int maximumQuality(ItemState state) {
    return state.installedAugments().stream()
        .mapToInt(a -> effect(a.augmentId()).maximumQuality())
        .max()
        .orElse(0);
  }

  public static Set<String> pools(ItemState state) {
    var result = new TreeSet<String>();
    for (var augment : state.installedAugments()) {
      var pool = effect(augment.augmentId()).pool();
      if (pool != null) result.add(pool);
    }
    return Set.copyOf(result);
  }

  public static boolean affectsSampling(ItemState state) {
    return suffixBonus(state) > 0 || !pools(state).isEmpty();
  }

  /**
   * Permanent source definitions are installed with zero weight; equipment activates their source
   * spawn weights.
   */
  public static ItemCatalog enrich(ItemCatalog catalog) {
    var ids = new HashSet<String>();
    DATA.pools()
        .getOrDefault(catalog.base().id(), Map.of())
        .values()
        .forEach(p -> ids.addAll(p.keySet()));
    ids.removeAll(catalog.modifiers().keySet());
    if (ids.isEmpty()) return catalog;
    var definitions = new ArrayList<>(catalog.modifiers().values());
    DATA.modifiers().stream().filter(d -> ids.contains(d.id())).forEach(definitions::add);
    return rebuilt(catalog, catalog.base(), definitions, false);
  }

  /**
   * An ephemeral sampler view adapts the existing bucket engine without putting socket state into
   * search buckets.
   */
  public static ItemCatalog forItem(ItemCatalog catalog, ItemState state) {
    if (catalog.augmentContextApplied() || !affectsSampling(state)) return catalog;
    var selected = new HashMap<String, Integer>();
    var pools = DATA.pools().getOrDefault(catalog.base().id(), Map.of());
    for (var pool : pools(state)) selected.putAll(pools.getOrDefault(pool, Map.of()));
    var definitions =
        catalog.modifiers().values().stream()
            .map(
                d ->
                    selected.containsKey(d.id())
                        ? new ModifierDefinition(
                            d.id(),
                            d.name(),
                            d.layer(),
                            d.affixType(),
                            d.familyIds(),
                            d.requiredItemLevel(),
                            selected.get(d.id()),
                            d.tier(),
                            d.text(),
                            d.stats(),
                            d.tags(),
                            d.sourceUrl())
                        : d)
            .toList();
    var base = catalog.base();
    int suffix = suffixBonus(state);
    var expanded =
        new ItemCatalog.BaseItem(
            base.id(),
            base.name(),
            base.sourceUrl(),
            base.implicitModifierId(),
            base.magicPrefixes(),
            base.magicSuffixes() + suffix,
            base.rarePrefixes(),
            base.rareSuffixes() + suffix);
    return rebuilt(catalog, expanded, definitions, true);
  }

  private static ItemCatalog rebuilt(
      ItemCatalog original,
      ItemCatalog.BaseItem base,
      List<ModifierDefinition> definitions,
      boolean contextual) {
    var old = original.metadata();
    var prefixes =
        definitions.stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
            .toList();
    var suffixes =
        definitions.stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.SUFFIX)
            .toList();
    var metadata =
        new ItemCatalog.Metadata(
            old.snapshotId(),
            old.retrievedAt(),
            old.sourceUrl(),
            old.weightPolicy(),
            old.rawSha256(),
            old.detailsSha256(),
            prefixes.size(),
            suffixes.size(),
            prefixes.stream().mapToLong(ModifierDefinition::weight).sum(),
            suffixes.stream().mapToLong(ModifierDefinition::weight).sum());
    return new ItemCatalog(
        metadata, base, definitions, original.compatibleSnapshotIds(), contextual);
  }
}
