package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.util.*;

/** Exact renewal shortcuts with full-state return or invariant conditional-distribution proofs. */
public final class RenewalMethodObservation {
  private RenewalMethodObservation() {}

  /**
   * Caller must supply adjacent boundaries of the same time-homogeneous full-state cycle. If
   * T*u=q*u exactly, linearity proves all subsequent live distributions q^k*u. No equality of
   * displayed goal buckets or empirical sampling is sufficient.
   */
  public static <S, A> Optional<Observation<S, A>> repeatInvariant(
      Observation<S, A> before, Observation<S, A> after, int count) {
    if (after.cycles() != before.cycles() + 1
        || count < after.cycles()
        || before.active().isEmpty()
        || !before.initialUses().equals(after.initialUses())
        || !before.mass().equals(Fraction.ONE)
        || !after.mass().equals(Fraction.ONE)) return Optional.empty();
    var initialMass = before.active().values().stream().reduce(Fraction.ZERO, Fraction::add);
    var nextMass = after.active().values().stream().reduce(Fraction.ZERO, Fraction::add);
    var q = divide(nextMass, initialMass);
    if (q.numerator().compareTo(q.denominator()) > 0) return Optional.empty();
    var expected = scale(before.active(), q);
    if (!expected.equals(after.active())) return Optional.empty();
    int repeats = Math.toIntExact(count - before.cycles());
    var survival = q.pow(repeats);
    var factor =
        q.equals(Fraction.ONE)
            ? Fraction.of(repeats, 1)
            : divide(RenewalFirstHit.complement(survival), RenewalFirstHit.complement(q));
    var exits = new EnumMap<Exit, Map<S, Fraction>>(Exit.class);
    for (var kind : Exit.values())
      exits.put(kind, accumulated(before.exits().get(kind), after.exits().get(kind), factor));
    var result =
        new Observation<>(
            count,
            Map.copyOf(exits),
            scale(before.active(), survival),
            before.initialUses(),
            accumulated(before.cycleUses(), after.cycleUses(), factor));
    if (!result.mass().equals(Fraction.ONE))
      throw new IllegalStateException("Invariant renewal mass not conserved");
    return Optional.of(result);
  }

  private static Fraction divide(Fraction a, Fraction b) {
    return a.divide(b);
  }

  private static <K> Map<K, Fraction> scale(Map<K, Fraction> values, Fraction factor) {
    var scaled = new LinkedHashMap<K, Fraction>();
    values.forEach(
        (key, value) -> {
          var mass = value.multiply(factor);
          if (!mass.equals(Fraction.ZERO)) scaled.put(key, mass);
        });
    return Map.copyOf(scaled);
  }

  private static <K> Map<K, Fraction> accumulated(
      Map<K, Fraction> before, Map<K, Fraction> after, Fraction factor) {
    var result = new LinkedHashMap<K, Fraction>(before);
    var keys = new HashSet<K>(before.keySet());
    keys.addAll(after.keySet());
    for (var key : keys) {
      var a = before.getOrDefault(key, Fraction.ZERO);
      var b = after.getOrDefault(key, Fraction.ZERO);
      var increment =
          new Fraction(
              b.numerator()
                  .multiply(a.denominator())
                  .subtract(a.numerator().multiply(b.denominator())),
              a.denominator().multiply(b.denominator()));
      var value = a.add(increment.multiply(factor));
      if (!value.equals(Fraction.ZERO)) result.put(key, value);
    }
    return Map.copyOf(result);
  }

  public static <S, A> Observation<S, A> repeat(Observation<S, A> cycle, S source, int count) {
    if (count < 1
        || cycle.cycles() != 1
        || !cycle.initialUses().isEmpty()
        || (!cycle.active().isEmpty() && !cycle.active().keySet().equals(Set.of(source)))
        || !cycle.mass().equals(Fraction.ONE))
      throw new IllegalArgumentException("Full return proof required");
    var r = cycle.active().getOrDefault(source, Fraction.ZERO);
    var survival = r.pow(count);
    var stop = RenewalFirstHit.complement(r);
    var totalStop = RenewalFirstHit.complement(survival);
    var factor = r.equals(Fraction.ONE) ? Fraction.of(count, 1) : totalStop.divide(stop);
    var exits = new EnumMap<Exit, Map<S, Fraction>>(Exit.class);
    cycle
        .exits()
        .forEach(
            (kind, values) -> {
              var scaled = new LinkedHashMap<S, Fraction>();
              values.forEach((state, mass) -> scaled.put(state, mass.multiply(factor)));
              exits.put(kind, Map.copyOf(scaled));
            });
    var uses = new LinkedHashMap<A, Fraction>();
    cycle.cycleUses().forEach((action, mass) -> uses.put(action, mass.multiply(factor)));
    var result =
        new Observation<>(
            count,
            Map.copyOf(exits),
            survival.equals(Fraction.ZERO) ? Map.<S, Fraction>of() : Map.of(source, survival),
            Map.<A, Fraction>of(),
            Map.copyOf(uses));
    if (!result.mass().equals(Fraction.ONE))
      throw new IllegalStateException("Renewal mass not conserved");
    return result;
  }
}
