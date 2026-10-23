package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.crafting.domain.WorkbenchCurrency;
import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.item.*;
import java.math.*;
import java.util.*;

/** Job-fixed disjoint intervals; uncertain numeric dependencies retain individual rolls. */
public final class RollPartition {
  public static final String VERSION = "goal-reviewed-scalar-shared-pool-partition-v6";
  private final boolean exact;
  private final ItemState initial;
  private final Set<String> relevant, intervalSafe;
  private final Map<String, NavigableSet<BigInteger>> cuts;
  private final Map<String, NavigableSet<BigInteger>> automaticCuts;
  private final TierPartition tiers;
  private final OrbitPartition orbits;
  private final Set<WorkbenchCurrency> actions;

  private RollPartition(
      boolean exact,
      Set<String> relevant,
      Set<String> intervalSafe,
      Map<String, NavigableSet<BigInteger>> cuts) {
    this(
        exact,
        relevant,
        intervalSafe,
        cuts,
        TierPartition.empty(),
        OrbitPartition.empty(),
        Set.copyOf(BasicCurrencyTransitions.supportedActions()));
  }

  private RollPartition(
      boolean exact,
      Set<String> relevant,
      Set<String> intervalSafe,
      Map<String, NavigableSet<BigInteger>> cuts,
      TierPartition tiers,
      OrbitPartition orbits,
      Set<WorkbenchCurrency> actions) {
    this(exact, relevant, intervalSafe, cuts, tiers, orbits, actions, null);
  }

  private RollPartition(
      boolean exact,
      Set<String> relevant,
      Set<String> intervalSafe,
      Map<String, NavigableSet<BigInteger>> cuts,
      TierPartition tiers,
      OrbitPartition orbits,
      Set<WorkbenchCurrency> actions,
      ItemState initial) {
    this(exact, relevant, intervalSafe, cuts, tiers, orbits, actions, initial, cuts);
  }

  private RollPartition(
      boolean exact,
      Set<String> relevant,
      Set<String> intervalSafe,
      Map<String, NavigableSet<BigInteger>> cuts,
      TierPartition tiers,
      OrbitPartition orbits,
      Set<WorkbenchCurrency> actions,
      ItemState initial,
      Map<String, NavigableSet<BigInteger>> automaticCuts) {
    this.initial = initial;
    this.exact = exact;
    this.relevant = Set.copyOf(relevant);
    this.intervalSafe = Set.copyOf(intervalSafe);
    this.cuts = new HashMap<>();
    this.automaticCuts = new HashMap<>();
    automaticCuts.forEach(
        (key, value) ->
            this.automaticCuts.put(
                key, Collections.unmodifiableNavigableSet(new TreeSet<>(value))));
    this.tiers = tiers;
    this.orbits = orbits;
    this.actions = Set.copyOf(actions);
    cuts.forEach(
        (key, value) ->
            this.cuts.put(key, Collections.unmodifiableNavigableSet(new TreeSet<>(value))));
  }

  public ItemState initial() {
    return initial;
  }

  private RollPartition withInitial(ItemState root) {
    return new RollPartition(
        exact, relevant, intervalSafe, cuts, tiers, orbits, actions, root, automaticCuts);
  }

  public Object automaticRegion(String stat, long value) {
    return exact || (relevant.contains(stat) && !intervalSafe.contains(stat))
        ? List.of(stat, value)
        : region(stat, value, automaticCuts);
  }

  public static RollPartition exact() {
    return new RollPartition(true, Set.of(), Set.of(), Map.of());
  }

  public static RollPartition compile(
      ItemCatalog catalog, Map<String, GoalCatalog.Stat> known, GoalFilter goal, ItemState root) {
    if (root.catalystQuality() != null) return exact().withInitial(root);
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
        return exact().withInitial(root);
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
              if (mapping.isEmpty() || mapping.get().numerator() <= 0)
                return exact().withInitial(root);
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
    // A known point must remain disjoint in probability space. Its artificial
    // boundaries do not change the goal or any later transition, however.
    var automaticCuts = new HashMap<String, NavigableSet<BigInteger>>();
    cuts.forEach((stat, values) -> automaticCuts.put(stat, new TreeSet<>(values)));
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
    var result =
        new RollPartition(
            false,
            relevant,
            safe,
            cuts,
            TierPartition.empty(),
            OrbitPartition.empty(),
            Set.copyOf(BasicCurrencyTransitions.supportedActions()),
            root,
            automaticCuts);
    // The shared minimum-level grouping does not yet support multi-family generation groups.
    if (!root.baseItemId().equals(SolarAmulet.BASE_ID)
        || !simple
        || !safe.containsAll(relevant)
        || catalog.modifiers().values().stream()
            .anyMatch(
                d ->
                    d.layer() == ModifierDefinition.Layer.EXPLICIT
                        && d.weight() > 0
                        && d.requiredItemLevel() <= root.itemLevel()
                        && d.familyIds().size() != 1)) return result;
    var eligibility = new BasicCandidateEligibility(catalog, root);
    return new RollPartition(
        false,
        relevant,
        safe,
        cuts,
        TierPartition.compile(catalog, root, result, eligibility),
        OrbitPartition.compile(catalog, root, result, eligibility),
        Set.copyOf(BasicCurrencyTransitions.reachableActions(root)),
        root,
        automaticCuts);
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

  public TierPartition tiers() {
    return tiers;
  }

  public OrbitPartition orbits() {
    return orbits;
  }

  public boolean allows(WorkbenchCurrency action) {
    return actions.contains(action);
  }

  boolean relevant(String stat) {
    return relevant.contains(stat);
  }

  /** Existing tier eligibility is not inspected by the no-omen basic rules. */
  Object transitionRegion(ModifierDefinition definition, ModifierInstance modifier) {
    if (exact || definition.stats().size() != 1 || modifier.values().size() != 1)
      return List.of(modifier.modifierId(), new TreeMap<>(modifier.values()));
    var stat = definition.stats().getFirst().id();
    var value = modifier.values().get(stat);
    if (value == null) throw new IllegalArgumentException("Missing modifier stat");
    if (relevant.contains(stat) && !intervalSafe.contains(stat)) return List.of(stat, value);
    return tierRegion(stat, value);
  }

  Object tierRegion(String stat, long first) {
    return region(stat, first, cuts);
  }

  private Object region(String stat, long first, Map<String, NavigableSet<BigInteger>> boundaries) {
    if (!relevant.contains(stat)) return List.of();
    var floor =
        boundaries
            .getOrDefault(stat, Collections.emptyNavigableSet())
            .floor(BigInteger.valueOf(first));
    return List.of(
        stat, floor == null ? BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE) : floor);
  }
}
