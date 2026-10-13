package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.BasicCurrencyState;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.item.*;
import java.math.BigInteger;
import java.util.*;

/** Exact one-axis mixtures inside a single mutually exclusive execution distribution. */
public final class DeferredMixtures {
  public static final String VERSION = "conditional-tier-orbit-mixture-v2";
  private final BasicTransitionClasses classes;
  // Generated posterior laws retain their decomposition into the original finite cells.
  private final Map<TierCell, Map<TierCell, Fraction>> decompositions = new HashMap<>();
  private final Set<TierCell> originalCells = new HashSet<>();
  private long distributions, mergedCells, mergedOrbitCells;

  public record Metrics(
      long distributions, long mergedCells, int generatedLaws, long mergedOrbitCells) {}

  public Metrics metrics() {
    return new Metrics(distributions, mergedCells, decompositions.size(), mergedOrbitCells);
  }

  public DeferredMixtures(BasicTransitionClasses classes) {
    this.classes = Objects.requireNonNull(classes);
  }

  private record Group(DeferredItem rest, BasicTransitionClasses.Role role) {}

  private record Slice(DeferredItem source, Fraction mass, TierCell law) {}

  private record Interval(String modifier, String stat) {}

  public Map<DeferredItem, Fraction> compact(Map<DeferredItem, Fraction> distribution) {
    var cursor = start(distribution);
    while (!cursor.complete()) cursor.step(32);
    return cursor.result();
  }

  public ExactMethodRunner.Compaction<DeferredItem> start(
      Map<DeferredItem, Fraction> distribution) {
    return new Pipeline(distribution);
  }

  private final class Pipeline implements ExactMethodRunner.Compaction<DeferredItem> {
    ExactMethodRunner.Compaction<DeferredItem> phase;
    boolean tiers;

    Pipeline(Map<DeferredItem, Fraction> input) {
      distributions++;
      phase = new OrbitCursor(input);
    }

    public boolean complete() {
      return tiers && phase.complete();
    }

    public void step(int maxInputs) {
      if (maxInputs < 1) throw new IllegalArgumentException("Positive mixture slice required");
      if (!phase.complete()) phase.step(maxInputs);
      if (phase.complete() && !tiers) {
        phase = new Cursor(phase.result());
        tiers = true;
      }
    }

    public Map<DeferredItem, Fraction> result() {
      if (!complete()) throw new IllegalStateException("Partial mixture pipeline");
      return phase.result();
    }
  }

  /** Restore finite graph identities without discarding the conditional mixture weights. */
  public Map<DeferredItem, Fraction> expand(Map<DeferredItem, Fraction> distribution) {
    var result = new LinkedHashMap<DeferredItem, Fraction>();
    for (var entry : distribution.entrySet()) {
      Map<DeferredItem, Fraction> current = Map.of(entry.getKey(), entry.getValue());
      for (var tier : entry.getKey().tiers().entrySet()) {
        var basis = decompositions.get(tier.getValue());
        if (basis == null) continue;
        var next = new LinkedHashMap<DeferredItem, Fraction>();
        for (var source : current.entrySet())
          for (var atom : basis.entrySet())
            next.merge(
                replaceLaw(source.getKey(), tier.getKey(), atom.getKey()),
                source.getValue().multiply(atom.getValue()),
                Fraction::add);
        current = next;
      }
      for (var orbit : entry.getKey().orbits().entrySet()) {
        if (!orbit.getValue().mixed()) continue;
        var next = new LinkedHashMap<DeferredItem, Fraction>();
        for (var source : current.entrySet())
          for (var atom : orbit.getValue().basis().entrySet())
            next.merge(
                withOrbit(withoutOrbit(source.getKey(), orbit.getKey()), atom.getKey()),
                source.getValue().multiply(atom.getValue()),
                Fraction::add);
        current = next;
      }
      current.forEach((state, mass) -> result.merge(state, mass, Fraction::add));
    }
    return result;
  }

  private static DeferredItem withoutOrbit(DeferredItem source, String profile) {
    var removed = source.orbits().get(profile).representatives();
    var orbits = new HashMap<>(source.orbits());
    orbits.remove(profile);
    return new DeferredItem(
        replace(
            source.representative(),
            source.item().explicits().stream().filter(m -> !removed.contains(m)).toList()),
        source.rolls(),
        source.tiers(),
        orbits);
  }

  private static DeferredItem withOrbit(DeferredItem rest, OrbitCell cell) {
    var explicits = new ArrayList<>(rest.item().explicits());
    explicits.addAll(cell.representatives());
    var orbits = new HashMap<>(rest.orbits());
    orbits.put(cell.profile().id(), cell);
    return new DeferredItem(
        replace(rest.representative(), explicits), rest.rolls(), rest.tiers(), orbits);
  }

  private record OrbitGroup(DeferredItem rest, OrbitCell.Profile profile, int occupied) {}

  private record OrbitSlice(DeferredItem state, OrbitCell law, Fraction mass) {}

  private final class OrbitCursor implements ExactMethodRunner.Compaction<DeferredItem> {
    final LinkedHashMap<DeferredItem, Fraction> result;
    final TreeSet<String> profiles = new TreeSet<>();
    Iterator<Map.Entry<DeferredItem, Fraction>> scan;
    Iterator<String> axes;
    String profile;
    Map<OrbitGroup, List<OrbitSlice>> groups;
    Iterator<Map.Entry<OrbitGroup, List<OrbitSlice>>> merging;
    boolean complete;

    OrbitCursor(Map<DeferredItem, Fraction> input) {
      result = new LinkedHashMap<>(input);
      scan = result.entrySet().iterator();
      complete = input.size() < 2;
    }

    public boolean complete() {
      return complete;
    }

    public Map<DeferredItem, Fraction> result() {
      if (!complete) throw new IllegalStateException("Partial orbit mixture");
      return Collections.unmodifiableMap(result);
    }

    public void step(int maxInputs) {
      if (maxInputs < 1) throw new IllegalArgumentException("Positive orbit slice required");
      for (int n = 0; n < maxInputs && !complete; n++) advance();
    }

    void advance() {
      if (axes == null) {
        if (scan.hasNext()) {
          profiles.addAll(scan.next().getKey().orbits().keySet());
          return;
        }
        axes = profiles.iterator();
      }
      if (profile == null) {
        if (!axes.hasNext()) {
          complete = true;
          return;
        }
        profile = axes.next();
        groups = new LinkedHashMap<>();
        scan = result.entrySet().iterator();
        merging = null;
      }
      if (scan.hasNext()) {
        var entry = scan.next();
        var law = entry.getKey().orbits().get(profile);
        if (law != null && !entry.getValue().equals(Fraction.ZERO)) {
          var group =
              new OrbitGroup(withoutOrbit(entry.getKey(), profile), law.profile(), law.occupancy());
          groups
              .computeIfAbsent(group, ignored -> new ArrayList<>())
              .add(new OrbitSlice(entry.getKey(), law, entry.getValue()));
        }
        return;
      }
      if (merging == null) merging = groups.entrySet().iterator();
      if (merging.hasNext()) {
        var entry = merging.next();
        var slices = entry.getValue();
        if (slices.size() < 2) return;
        var mass = slices.stream().map(OrbitSlice::mass).reduce(Fraction.ZERO, Fraction::add);
        var law = new LinkedHashMap<OrbitCell, Fraction>();
        slices.forEach(s -> law.merge(s.law(), s.mass().divide(mass), Fraction::add));
        var mixed = withOrbit(entry.getKey().rest(), OrbitCell.mix(law));
        slices.forEach(s -> result.remove(s.state()));
        result.merge(mixed, mass, Fraction::add);
        mergedCells += slices.size() - 1;
        mergedOrbitCells += slices.size() - 1;
        return;
      }
      profile = null;
    }
  }

  private static DeferredItem replaceLaw(DeferredItem source, String modifier, TierCell law) {
    var fractured =
        source.item().explicits().stream()
            .filter(m -> m.modifierId().equals(modifier))
            .findFirst()
            .orElseThrow()
            .fractured();
    var rest = without(source, modifier);
    var representative = law.representative(fractured);
    var explicits = new ArrayList<>(rest.item().explicits());
    explicits.add(representative);
    var rolls = new HashMap<>(rest.rolls());
    var tiers = new HashMap<>(rest.tiers());
    if (law.variants().size() == 1) {
      var v = law.variants().getFirst();
      if (v.min() < v.max())
        rolls.put(v.modifierId(), new DeferredItem.Band(v.stat(), v.min(), v.max()));
    } else tiers.put(representative.modifierId(), law);
    return new DeferredItem(replace(rest.representative(), explicits), rolls, tiers, rest.orbits());
  }

  private final class Cursor implements ExactMethodRunner.Compaction<DeferredItem> {
    final LinkedHashMap<DeferredItem, Fraction> result;
    final TreeSet<String> families = new TreeSet<>();
    Iterator<Map.Entry<DeferredItem, Fraction>> scan;
    Iterator<String> axes;
    String family;
    LinkedHashMap<Group, List<Slice>> groups;
    Iterator<Map.Entry<Group, List<Slice>>> merging;
    boolean complete;

    Cursor(Map<DeferredItem, Fraction> distribution) {
      result = new LinkedHashMap<>(distribution);
      scan = result.entrySet().iterator();
      complete = distribution.size() < 2;
    }

    public boolean complete() {
      return complete;
    }

    public Map<DeferredItem, Fraction> result() {
      if (!complete) throw new IllegalStateException("Partial conditional mixture");
      return Collections.unmodifiableMap(result);
    }

    public void step(int maxInputs) {
      if (maxInputs < 1) throw new IllegalArgumentException("Positive mixture slice required");
      for (int n = 0; n < maxInputs && !complete; n++) advance();
    }

    void advance() {
      if (axes == null) {
        if (scan.hasNext()) {
          var state = scan.next().getKey();
          for (var modifier : state.item().explicits())
            if (!inOrbit(state, modifier) && modifier.values().size() == 1)
              families.add(classes.retainedRole(modifier).family());
          return;
        }
        axes = families.iterator();
      }
      if (family == null) {
        if (!axes.hasNext()) {
          complete = true;
          return;
        }
        family = axes.next();
        groups = new LinkedHashMap<>();
        scan = result.entrySet().iterator();
        merging = null;
      }
      if (scan.hasNext()) {
        var entry = scan.next();
        if (entry.getValue().equals(Fraction.ZERO)) return;
        var state = entry.getKey();
        for (var modifier : state.item().explicits()) {
          if (inOrbit(state, modifier) || modifier.values().size() != 1) continue;
          var role = classes.retainedRole(modifier);
          if (!role.family().equals(family)) continue;
          var law = law(state, modifier);
          // Validate both ends; compiled goal regions contain no internal observation cuts.
          if (law.variants().stream()
              .anyMatch(
                  v ->
                      !classes.retainedRole(v.item(v.min(), modifier.fractured())).equals(role)
                          || !classes
                              .retainedRole(v.item(v.max(), modifier.fractured()))
                              .equals(role))) continue;
          var key = new Group(without(state, modifier.modifierId()), role);
          groups
              .computeIfAbsent(key, ignored -> new ArrayList<>())
              .add(new Slice(state, entry.getValue(), law));
        }
        return;
      }
      if (merging == null) merging = groups.entrySet().iterator();
      if (merging.hasNext()) {
        var entry = merging.next();
        var slices = entry.getValue();
        if (slices.size() < 2) return;
        mergedCells += slices.size() - 1;
        var mass = slices.stream().map(Slice::mass).reduce(Fraction.ZERO, Fraction::add);
        var law = mix(slices, mass);
        if (!decompositions.containsKey(law) && !originalCells.contains(law)) {
          var basis = new LinkedHashMap<TierCell, Fraction>();
          for (var slice : slices) {
            if (!decompositions.containsKey(slice.law())) originalCells.add(slice.law());
            decompositions
                .getOrDefault(slice.law(), Map.of(slice.law(), Fraction.ONE))
                .forEach(
                    (atom, weight) ->
                        basis.merge(
                            atom, slice.mass().divide(mass).multiply(weight), Fraction::add));
          }
          if (!originalCells.contains(law)) decompositions.put(law, Map.copyOf(basis));
        }
        var group = entry.getKey();
        var representative = law.representative(group.role().fractured());
        var explicits = new ArrayList<>(group.rest().item().explicits());
        explicits.add(representative);
        var tiers = new HashMap<>(group.rest().tiers());
        tiers.put(representative.modifierId(), law);
        var mixed =
            new DeferredItem(
                replace(group.rest().representative(), explicits),
                group.rest().rolls(),
                tiers,
                group.rest().orbits());
        slices.forEach(slice -> result.remove(slice.source()));
        result.merge(mixed, mass, Fraction::add);
        return;
      }
      family = null;
    }
  }

  private static boolean inOrbit(DeferredItem state, ModifierInstance modifier) {
    return state.orbits().values().stream()
        .anyMatch(orbit -> orbit.representatives().contains(modifier));
  }

  private static TierCell law(DeferredItem state, ModifierInstance modifier) {
    var existing = state.tiers().get(modifier.modifierId());
    if (existing != null) return existing;
    var band = state.rolls().get(modifier.modifierId());
    var value = modifier.values().entrySet().iterator().next();
    return new TierCell(
        List.of(
            new TierCell.Variant(
                modifier.modifierId(),
                value.getKey(),
                band == null ? value.getValue() : band.min(),
                band == null ? value.getValue() : band.max(),
                Fraction.ONE)));
  }

  private static DeferredItem without(DeferredItem state, String modifier) {
    var rolls = new HashMap<>(state.rolls());
    var tiers = new HashMap<>(state.tiers());
    rolls.remove(modifier);
    tiers.remove(modifier);
    return new DeferredItem(
        replace(
            state.representative(),
            state.item().explicits().stream()
                .filter(m -> !m.modifierId().equals(modifier))
                .toList()),
        rolls,
        tiers,
        state.orbits());
  }

  private static BasicCurrencyState replace(
      BasicCurrencyState state, List<ModifierInstance> explicits) {
    var i = state.item();
    return new BasicCurrencyState(
        new ItemState(
            i.snapshotId(),
            i.baseItemId(),
            i.itemLevel(),
            i.rarity(),
            i.implicits(),
            explicits,
            i.conditions(),
            i.augmentSockets(),
            i.catalystQuality()),
        state.provenance());
  }

  private static TierCell mix(List<Slice> slices, Fraction mass) {
    var events =
        new TreeMap<Interval, TreeSet<BigInteger>>(
            Comparator.comparing(Interval::modifier).thenComparing(Interval::stat));
    for (var slice : slices)
      for (var variant : slice.law().variants()) {
        var points =
            events.computeIfAbsent(
                new Interval(variant.modifierId(), variant.stat()), ignored -> new TreeSet<>());
        points.add(BigInteger.valueOf(variant.min()));
        points.add(BigInteger.valueOf(variant.max()).add(BigInteger.ONE));
      }
    var variants = new ArrayList<TierCell.Variant>();
    for (var event : events.entrySet()) {
      var boundaries = new ArrayList<>(event.getValue());
      for (int n = 0; n + 1 < boundaries.size(); n++) {
        var first = boundaries.get(n);
        var end = boundaries.get(n + 1);
        var density = Fraction.ZERO;
        for (var slice : slices)
          for (var v : slice.law().variants())
            if (v.modifierId().equals(event.getKey().modifier())
                && v.stat().equals(event.getKey().stat())
                && BigInteger.valueOf(v.min()).compareTo(first) <= 0
                && BigInteger.valueOf(v.max()).compareTo(first) >= 0)
              density =
                  density.add(
                      slice
                          .mass()
                          .multiply(v.probability())
                          .divide(mass)
                          .divide(new Fraction(v.size(), BigInteger.ONE)));
        if (!density.equals(Fraction.ZERO))
          variants.add(
              new TierCell.Variant(
                  event.getKey().modifier(),
                  event.getKey().stat(),
                  first.longValueExact(),
                  end.subtract(BigInteger.ONE).longValueExact(),
                  density.multiply(new Fraction(end.subtract(first), BigInteger.ONE))));
      }
    }
    return new TierCell(variants);
  }
}
