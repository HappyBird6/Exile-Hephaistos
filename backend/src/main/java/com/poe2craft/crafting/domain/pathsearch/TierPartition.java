package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.item.*;
import java.math.BigInteger;
import java.util.*;

/** Job-fixed disjoint tier cells, split at every supported currency eligibility boundary. */
public final class TierPartition {
  public static final String VERSION = "family-goal-reachable-currency-tier-partition-v2";

  public record BandKey(String modifier, long min, long max) {}

  public record Member(TierCell cell, Fraction cellWeight, Fraction intervalWeight) {}

  private record Key(
      Object goalRegion,
      ModifierDefinition.AffixType affix,
      Set<String> families,
      Set<String> tags,
      List<Boolean> currencyEligibility,
      String initialSingleton) {}

  private record Weighted(ModifierDefinition definition, long min, long max, Fraction weight) {}

  private final Map<BandKey, Member> members;

  private TierPartition(Map<BandKey, Member> members) {
    this.members = Map.copyOf(members);
  }

  static TierPartition empty() {
    return new TierPartition(Map.of());
  }

  public Optional<Member> member(String modifier, long min, long max) {
    return Optional.ofNullable(members.get(new BandKey(modifier, min, max)));
  }

  static TierPartition compile(ItemCatalog catalog, ItemState root, RollPartition rolls) {
    var groups = new LinkedHashMap<Key, List<Weighted>>();
    var fixed = new HashSet<BandKey>();
    root.explicits()
        .forEach(
            m -> m.values().values().forEach(v -> fixed.add(new BandKey(m.modifierId(), v, v))));
    boolean uniform =
        Set.of(
                "UNVERIFIED_GAME_WEIGHTS_EXPLICIT_UNIFORM_ELIGIBLE_CANDIDATES",
                "USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS")
            .contains(catalog.metadata().weightPolicy());
    for (var definition : catalog.modifiers().values()) {
      if (definition.layer() != ModifierDefinition.Layer.EXPLICIT
          || definition.weight() <= 0
          || definition.requiredItemLevel() > root.itemLevel()
          || definition.stats().size() != 1) continue;
      var range = definition.stats().getFirst();
      for (long first = range.min(); ; ) {
        long last = rolls.end(definition, first);
        var band = new BandKey(definition.id(), first, last);
        var key =
            new Key(
                rolls.tierRegion(range.id(), first),
                definition.affixType(),
                definition.familyIds(),
                definition.tags(),
                BasicCurrencyTransitions.reachableActions(root).stream()
                    .map(a -> definition.requiredItemLevel() >= a.minimumModifierLevel())
                    .toList(),
                fixed.contains(band) ? definition.id() + ":" + first : "");
        var width =
            BigInteger.valueOf(last).subtract(BigInteger.valueOf(first)).add(BigInteger.ONE);
        var totalWidth =
            BigInteger.valueOf(range.max())
                .subtract(BigInteger.valueOf(range.min()))
                .add(BigInteger.ONE);
        var weight =
            new Fraction(
                BigInteger.valueOf(uniform ? 1 : definition.weight()).multiply(width), totalWidth);
        groups
            .computeIfAbsent(key, ignored -> new ArrayList<>())
            .add(new Weighted(definition, first, last, weight));
        if (last == range.max()) break;
        first = last + 1;
      }
    }
    var members = new HashMap<BandKey, Member>();
    for (var group : groups.values()) {
      if (group.size() < 2)
        continue; // Keep the established roll-only representation for one interval.
      var total = group.stream().map(Weighted::weight).reduce(Fraction.ZERO, Fraction::add);
      var cell =
          new TierCell(
              group.stream()
                  .map(
                      v ->
                          new TierCell.Variant(
                              v.definition().id(),
                              v.definition().stats().getFirst().id(),
                              v.min(),
                              v.max(),
                              new Fraction(
                                  v.weight().numerator().multiply(total.denominator()),
                                  v.weight().denominator().multiply(total.numerator()))))
                  .toList());
      for (var v : group)
        members.put(
            new BandKey(v.definition().id(), v.min(), v.max()),
            new Member(cell, total, v.weight()));
    }
    return new TierPartition(members);
  }
}
