package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.item.*;
import java.math.*;
import java.util.*;

/** Job-fixed disjoint intervals; uncertain numeric dependencies retain individual rolls. */
public final class RollPartition {
  public static final String VERSION = "goal-and-initial-roll-partition-v1";
  private final boolean exact;
  private final Set<String> relevant, intervalSafe;
  private final Map<String, NavigableSet<BigInteger>> cuts;

  private RollPartition(
      boolean exact,
      Set<String> relevant,
      Set<String> intervalSafe,
      Map<String, NavigableSet<BigInteger>> cuts) {
    this.exact = exact;
    this.relevant = Set.copyOf(relevant);
    this.intervalSafe = Set.copyOf(intervalSafe);
    this.cuts = new HashMap<>();
    cuts.forEach(
        (key, value) ->
            this.cuts.put(key, Collections.unmodifiableNavigableSet(new TreeSet<>(value))));
  }

  public static RollPartition exact() {
    return new RollPartition(true, Set.of(), Set.of(), Map.of());
  }

  public static RollPartition compile(
      ItemCatalog catalog, Map<String, GoalCatalog.Stat> known, GoalFilter goal, ItemState root) {
    if (!root.baseItemId().equals(SolarAmulet.BASE_ID) || root.catalystQuality() != null)
      return exact();
    var relevant = new HashSet<String>();
    var pending = new ArrayDeque<String>();
    goal.groups().stream()
        .filter(g -> !g.disabled())
        .flatMap(g -> g.entries().stream())
        .filter(e -> !e.disabled())
        .forEach(e -> pending.add(e.statId()));
    var seen = new HashSet<String>();
    while (!pending.isEmpty()) {
      var id = pending.remove();
      if (!seen.add(id)) continue;
      var stat = known.get(id);
      if (stat == null || stat.support().evaluation() != GoalFilter.Capability.SUPPORTED)
        return exact();
      relevant.addAll(stat.sourceStatIds());
      stat.contributions().forEach(c -> pending.add(c.statId()));
    }
    var safe = new HashSet<String>();
    var cuts = new HashMap<String, NavigableSet<BigInteger>>();
    boolean simple =
        goal.groups().stream()
            .filter(g -> !g.disabled())
            .allMatch(
                g ->
                    g.type() != GoalFilter.Type.WEIGHTED_V1
                        && g.type() != GoalFilter.Type.WEIGHTED_V2
                        && g.entries().stream()
                            .filter(e -> !e.disabled())
                            .allMatch(
                                e -> known.get(e.statId()).kind() == GoalCatalog.Kind.EXPLICIT));
    if (simple)
      for (var group : goal.groups())
        if (!group.disabled())
          for (var entry : group.entries())
            if (!entry.disabled()) {
              var mapping = GoalCatalogIndex.mapping(known.get(entry.statId()));
              if (mapping.isEmpty() || mapping.get().numerator() <= 0) return exact();
              var m = mapping.get();
              var producers =
                  catalog.modifiers().values().stream()
                      .filter(
                          d ->
                              d.layer() == ModifierDefinition.Layer.EXPLICIT
                                  && d.stats().stream()
                                      .anyMatch(s -> s.id().equals(m.sourceStatId())))
                      .toList();
              if (producers.isEmpty()) continue;
              var common = new HashSet<>(producers.getFirst().familyIds());
              producers.forEach(d -> common.retainAll(d.familyIds()));
              if (common.isEmpty())
                continue; // Several coexisting contributors can cross a threshold jointly.
              safe.add(m.sourceStatId());
              var boundaries = cuts.computeIfAbsent(m.sourceStatId(), ignored -> new TreeSet<>());
              if (entry.range() != null && entry.range().min() != null)
                boundaries.add(raw(entry.range().min(), m, RoundingMode.CEILING));
              if (entry.range() != null && entry.range().max() != null)
                boundaries.add(raw(entry.range().max(), m, RoundingMode.FLOOR).add(BigInteger.ONE));
            }
    // Initial deterministic values must never overlap a generated uniform cell.
    for (var modifier : root.explicits())
      modifier
          .values()
          .forEach(
              (stat, value) -> {
                var boundaries = cuts.computeIfAbsent(stat, ignored -> new TreeSet<>());
                boundaries.add(BigInteger.valueOf(value));
                boundaries.add(BigInteger.valueOf(value).add(BigInteger.ONE));
              });
    return new RollPartition(false, relevant, safe, cuts);
  }

  private static BigInteger raw(
      BigDecimal value, GoalCatalog.Mapping mapping, RoundingMode rounding) {
    return value
        .multiply(BigDecimal.valueOf(mapping.denominator()))
        .divide(BigDecimal.valueOf(mapping.numerator()), 0, rounding)
        .toBigIntegerExact();
  }

  public long end(ModifierDefinition candidate, Long first) {
    var range = candidate.stats().getFirst();
    if (exact || relevant.contains(range.id()) && !intervalSafe.contains(range.id())) return first;
    var boundary =
        cuts.getOrDefault(range.id(), Collections.emptyNavigableSet())
            .higher(BigInteger.valueOf(first));
    return boundary == null
        ? range.max()
        : BigInteger.valueOf(range.max()).min(boundary.subtract(BigInteger.ONE)).longValueExact();
  }
}
