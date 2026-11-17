package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.Exit;
import java.util.*;
import java.util.function.Function;

/** Session boundaries apply to complete conditional method laws, never unenumerated mass. */
public record RareOutcomePolicy(
    String version, boolean enabled, int groupPartsPerMillion, int maximumMethodPartsPerMillion) {
  public static final String VERSION = "rare-conditional-groups-v1";

  public RareOutcomePolicy {
    if (!VERSION.equals(version)
        || groupPartsPerMillion < 0
        || groupPartsPerMillion > 1_000_000
        || maximumMethodPartsPerMillion < 0
        || maximumMethodPartsPerMillion > 1_000_000)
      throw new IllegalArgumentException("Invalid rare outcome policy");
  }

  public static RareOutcomePolicy disabled() {
    return new RareOutcomePolicy(VERSION, false, 0, 0);
  }

  private static boolean within(CertifiedInterval mass, int parts) {
    return mass.upperUnits()
            .multiply(java.math.BigInteger.valueOf(1_000_000))
            .compareTo(java.math.BigInteger.valueOf(parts).shiftLeft(mass.fractionalBits()))
        <= 0;
  }

  /** Deterministic whole-group selection; upper bounds must prove both configured limits. */
  public <S, A> CertifiedMethodRunner.Observation<S, A> apply(
      CertifiedMethodRunner.Observation<S, A> point,
      Function<S, Object> equivalence,
      java.util.function.Predicate<S> goal,
      boolean includeActive) {
    if (!enabled || !point.exits().getOrDefault(Exit.REBASE, Map.of()).isEmpty()) return point;
    int bits = point.mass().fractionalBits();
    var groups = new LinkedHashMap<Object, CertifiedInterval>();
    var eligible = new LinkedHashMap<S, Object>();
    var laws = new ArrayList<Map<S, CertifiedInterval>>();
    laws.add(point.exits().getOrDefault(Exit.CHECKPOINT, Map.of()));
    if (includeActive) laws.add(point.active());
    for (var law : laws)
      law.forEach(
          (state, mass) -> {
            if (!goal.test(state)) {
              var key = equivalence.apply(state);
              eligible.put(state, key);
              groups.merge(key, mass, CertifiedInterval::add);
            }
          });
    var selected = new HashSet<Object>();
    var used = CertifiedInterval.zero(bits);
    // Treat equal upper-mass groups together: iteration/hash order must never choose a
    // different boundary after restore or on a different JVM.
    var ranked = new TreeMap<java.math.BigInteger, List<Object>>();
    groups.forEach(
        (key, mass) -> {
          if (within(mass, groupPartsPerMillion))
            ranked.computeIfAbsent(mass.upperUnits(), ignored -> new ArrayList<>()).add(key);
        });
    for (var ties : ranked.values()) {
      var mass = CertifiedInterval.zero(bits);
      for (var key : ties) mass = mass.add(groups.get(key));
      if (within(used.add(mass), maximumMethodPartsPerMillion)) {
        selected.addAll(ties);
        used = used.add(mass);
      }
    }
    var exits = new EnumMap<Exit, Map<S, CertifiedInterval>>(Exit.class);
    for (var kind : Exit.values())
      exits.put(kind, new LinkedHashMap<>(point.exits().getOrDefault(kind, Map.of())));
    var active = new LinkedHashMap<>(point.active());
    for (var law :
        includeActive
            ? List.of(exits.get(Exit.CHECKPOINT), active)
            : List.of(exits.get(Exit.CHECKPOINT))) {
      for (var entry : new ArrayList<>(law.entrySet())) {
        if (goal.test(entry.getKey())) {
          law.remove(entry.getKey());
          exits.get(Exit.GOAL).merge(entry.getKey(), entry.getValue(), CertifiedInterval::add);
        } else if (selected.contains(eligible.get(entry.getKey()))) {
          law.remove(entry.getKey());
          exits.get(Exit.REBASE).merge(entry.getKey(), entry.getValue(), CertifiedInterval::add);
        }
      }
    }
    return new CertifiedMethodRunner.Observation<>(
        point.cycles(), exits, active, point.initialUses(), point.cycleUses(), point.mass());
  }
}
