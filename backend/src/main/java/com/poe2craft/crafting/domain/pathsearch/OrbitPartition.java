package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.item.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Permutations are allowed only for identical complete basic-action weight profiles. */
public final class OrbitPartition {
  public static final String VERSION = "reachable-action-family-permutation-v2";

  private record Key(ModifierDefinition.AffixType affix, Map<Integer, Long> weights) {}

  private record Member(OrbitCell.Profile profile, int category) {}

  private final Map<String, Member> members;
  private final Map<OrbitCell.Profile, Map<Map<Integer, Integer>, OrbitCell>> cells =
      new HashMap<>();

  private OrbitPartition(Map<String, Member> members) {
    this.members = Map.copyOf(members);
  }

  static OrbitPartition empty() {
    return new OrbitPartition(Map.of());
  }

  private static int category(ModifierDefinition definition, List<WorkbenchCurrency> actions) {
    int mask = 0;
    for (int i = 0; i < actions.size(); i++)
      if (definition.requiredItemLevel() >= actions.get(i).minimumModifierLevel()) mask |= 1 << i;
    return mask;
  }

  static OrbitPartition compile(ItemCatalog catalog, ItemState root, RollPartition rolls) {
    var actions = BasicCurrencyTransitions.reachableActions(root);
    var families = new LinkedHashMap<Set<String>, List<ModifierDefinition>>();
    catalog.modifiers().values().stream()
        .filter(d -> d.layer() == ModifierDefinition.Layer.EXPLICIT)
        .forEach(d -> families.computeIfAbsent(d.familyIds(), ignored -> new ArrayList<>()).add(d));
    var initial = new HashSet<String>();
    root.explicits()
        .forEach(m -> initial.addAll(catalog.modifiers().get(m.modifierId()).familyIds()));
    var groups = new LinkedHashMap<Key, List<OrbitCell.Family>>();
    boolean uniform =
        Set.of(
                "UNVERIFIED_GAME_WEIGHTS_EXPLICIT_UNIFORM_ELIGIBLE_CANDIDATES",
                "USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS")
            .contains(catalog.metadata().weightPolicy());
    for (var entry : families.entrySet()) {
      var ids = entry.getKey();
      if (!Collections.disjoint(ids, initial)
          || entry.getValue().stream()
              .flatMap(d -> d.stats().stream())
              .anyMatch(s -> rolls.relevant(s.id()))
          || families.keySet().stream()
              .anyMatch(other -> !other.equals(ids) && !Collections.disjoint(ids, other))) continue;
      var eligible =
          entry.getValue().stream()
              .filter(d -> d.weight() > 0 && d.requiredItemLevel() <= root.itemLevel())
              .toList();
      if (eligible.isEmpty()
          || eligible.stream().anyMatch(d -> d.stats().size() != 1)
          || eligible.stream().map(ModifierDefinition::affixType).distinct().count() != 1) continue;
      var byCategory = new TreeMap<Integer, List<ModifierDefinition>>();
      eligible.forEach(
          d ->
              byCategory
                  .computeIfAbsent(category(d, actions), ignored -> new ArrayList<>())
                  .add(d));
      var weights = new TreeMap<Integer, Long>();
      var categories = new TreeMap<Integer, TierCell>();
      byCategory.forEach(
          (category, definitions) -> {
            long total = definitions.stream().mapToLong(d -> uniform ? 1 : d.weight()).sum();
            weights.put(category, total);
            categories.put(
                category,
                new TierCell(
                    definitions.stream()
                        .map(
                            d -> {
                              var range = d.stats().getFirst();
                              return new TierCell.Variant(
                                  d.id(),
                                  range.id(),
                                  range.min(),
                                  range.max(),
                                  Fraction.of(uniform ? 1 : d.weight(), total));
                            })
                        .toList()));
          });
      var key = new Key(eligible.getFirst().affixType(), Map.copyOf(weights));
      groups
          .computeIfAbsent(key, ignored -> new ArrayList<>())
          .add(new OrbitCell.Family(ids, categories));
    }
    var members = new HashMap<String, Member>();
    var identities = new HashMap<String, OrbitCell.Profile>();
    for (var entry : groups.entrySet()) {
      var sorted =
          entry.getValue().stream()
              .sorted(Comparator.comparing(f -> new TreeSet<>(f.ids()).toString()))
              .toList();
      // A finer partition keeps assignment metadata bounded even in future catalogs.
      for (int start = 0; start < sorted.size(); start += 6) {
        var batch = sorted.subList(start, Math.min(sorted.size(), start + 6));
        if (batch.size() < 2) continue;
        var signature =
            entry.getKey().affix()
                + ":"
                + new TreeMap<>(entry.getKey().weights())
                + ":"
                + batch.stream()
                    .map(f -> new TreeSet<>(f.ids()) + ":" + new TreeMap<>(f.categories()))
                    .toList();
        var profile =
            new OrbitCell.Profile(
                digest(signature), entry.getKey().affix(), batch, entry.getKey().weights());
        var old = identities.putIfAbsent(profile.id(), profile);
        if (old != null && !old.equals(profile))
          throw new IllegalStateException("Orbit identity collision");
        for (var family : batch)
          family
              .categories()
              .forEach(
                  (category, cell) ->
                      cell.variants()
                          .forEach(
                              v -> members.put(v.modifierId(), new Member(profile, category))));
      }
    }
    return new OrbitPartition(members);
  }

  private static String digest(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public void validateSource(DeferredItem source) {
    var represented = new HashSet<String>();
    source
        .orbits()
        .values()
        .forEach(
            c ->
                c.representatives()
                    .forEach(
                        m -> {
                          var member = members.get(m.modifierId());
                          if (member == null || !member.profile().equals(c.profile()))
                            throw new IllegalArgumentException("Different orbit partition");
                          represented.add(m.modifierId());
                        }));
    if (source.item().explicits().stream()
        .anyMatch(
            m -> members.containsKey(m.modifierId()) && !represented.contains(m.modifierId())))
      throw new IllegalArgumentException(
          "A fixed observed family must not be treated as an anonymous orbit");
  }

  public DeferredItem normalize(
      BasicCurrencyState state, Map<String, DeferredItem.Band> rolls, Map<String, TierCell> tiers) {
    var counts = new LinkedHashMap<OrbitCell.Profile, Map<Integer, Integer>>();
    var removed = new HashSet<String>();
    for (var modifier : state.item().explicits()) {
      var member = members.get(modifier.modifierId());
      if (member == null) continue;
      if (modifier.fractured())
        throw new IllegalStateException("Initial fractured family entered an orbit");
      removed.add(modifier.modifierId());
      counts
          .computeIfAbsent(member.profile(), ignored -> new TreeMap<>())
          .merge(member.category(), 1, Integer::sum);
    }
    if (counts.isEmpty()) return new DeferredItem(state, rolls, tiers);
    var orbitLaws = new TreeMap<String, OrbitCell>();
    var explicits =
        new ArrayList<>(
            state.item().explicits().stream()
                .filter(m -> !removed.contains(m.modifierId()))
                .toList());
    counts.forEach(
        (profile, values) -> {
          var key = Map.copyOf(values);
          var cell =
              cells
                  .computeIfAbsent(profile, ignored -> new HashMap<>())
                  .computeIfAbsent(key, ignored -> new OrbitCell(profile, key));
          orbitLaws.put(profile.id(), cell);
          explicits.addAll(cell.representatives());
        });
    var keptRolls = new HashMap<>(rolls);
    keptRolls.keySet().removeAll(removed);
    var keptTiers = new HashMap<>(tiers);
    keptTiers.keySet().removeAll(removed);
    var item = state.item();
    var representative =
        new BasicCurrencyState(
            new ItemState(
                item.snapshotId(),
                item.baseItemId(),
                item.itemLevel(),
                item.rarity(),
                item.implicits(),
                explicits,
                item.conditions(),
                item.augmentSockets(),
                item.catalystQuality()),
            state.provenance());
    return new DeferredItem(representative, keptRolls, keptTiers, orbitLaws);
  }
}
