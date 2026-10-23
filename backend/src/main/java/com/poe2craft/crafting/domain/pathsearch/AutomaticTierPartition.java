package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.item.*;
import java.math.BigInteger;
import java.util.*;

/** A fresh posterior over the actual eligible pool, never a job-global tier prior. */
public final class AutomaticTierPartition {
  private record Key(
      String stat,
      Object region,
      ModifierDefinition.AffixType side,
      Set<String> families,
      Set<String> tags,
      String fixed) {}

  private record Slice(ModifierDefinition definition, long min, long max, Fraction weight) {}

  private final RollPartition rolls;
  private final Set<TierPartition.BandKey> fixed;

  public AutomaticTierPartition(RollPartition rolls) {
    this.rolls = rolls;
    var initial = new HashSet<TierPartition.BandKey>();
    if (rolls.initial() != null)
      rolls
          .initial()
          .explicits()
          .forEach(
              m ->
                  m.values()
                      .values()
                      .forEach(v -> initial.add(new TierPartition.BandKey(m.modifierId(), v, v))));
    fixed = Set.copyOf(initial);
  }

  public Map<TierPartition.BandKey, TierPartition.Member> members(
      List<ModifierDefinition> candidates, boolean uniform, long budget) {
    var groups = new LinkedHashMap<Key, List<Slice>>();
    long visited = 0;
    for (var definition : candidates) {
      if (definition.stats().size() != 1 || (!uniform && definition.weight() == 0)) continue;
      var range = definition.stats().getFirst();
      var width =
          BigInteger.valueOf(range.max())
              .subtract(BigInteger.valueOf(range.min()))
              .add(BigInteger.ONE);
      for (long first = range.min(); ; ) {
        if (++visited > budget) return Map.of(); // No partially compiled prior is ever emitted.
        long last = rolls.end(definition, first);
        var band = new TierPartition.BandKey(definition.id(), first, last);
        var key =
            new Key(
                range.id(),
                rolls.automaticRegion(range.id(), first),
                definition.affixType(),
                definition.familyIds(),
                definition.tags(),
                fixed.contains(band) ? definition.id() + ":" + first : "");
        var weight =
            new Fraction(
                BigInteger.valueOf(uniform ? 1 : definition.weight())
                    .multiply(
                        BigInteger.valueOf(last)
                            .subtract(BigInteger.valueOf(first))
                            .add(BigInteger.ONE)),
                width);
        groups
            .computeIfAbsent(key, ignored -> new ArrayList<>())
            .add(new Slice(definition, first, last, weight));
        if (last == range.max()) break;
        first = last + 1;
      }
    }
    var result = new HashMap<TierPartition.BandKey, TierPartition.Member>();
    for (var slices : groups.values()) {
      if (slices.size() < 2) continue;
      var total = slices.stream().map(Slice::weight).reduce(Fraction.ZERO, Fraction::add);
      var cell =
          new TierCell(
              slices.stream()
                  .map(
                      s ->
                          new TierCell.Variant(
                              s.definition().id(),
                              s.definition().stats().getFirst().id(),
                              s.min(),
                              s.max(),
                              s.weight().divide(total)))
                  .toList());
      for (var slice : slices)
        result.put(
            new TierPartition.BandKey(slice.definition().id(), slice.min(), slice.max()),
            new TierPartition.Member(cell, total, slice.weight()));
    }
    return Map.copyOf(result);
  }
}
