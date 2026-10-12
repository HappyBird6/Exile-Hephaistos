package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.util.*;

/** Used only after one entire cycle proves every nonabsorbed branch returns to the full source. */
public final class RenewalMethodObservation {
  private RenewalMethodObservation() {}

  public static <S, A> Observation<S, A> repeat(Observation<S, A> cycle, S source, int count) {
    if (count < 1
        || cycle.cycles() != 1
        || !cycle.initialUses().isEmpty()
        || (!cycle.active().isEmpty() && !cycle.active().keySet().equals(Set.of(source)))
        || !cycle.mass().equals(Fraction.ONE))
      throw new IllegalArgumentException("Full return proof required");
    var r = cycle.active().getOrDefault(source, Fraction.ZERO);
    var survival = new Fraction(r.numerator().pow(count), r.denominator().pow(count));
    var stop = RenewalFirstHit.complement(r);
    var totalStop = RenewalFirstHit.complement(survival);
    var factor =
        r.equals(Fraction.ONE)
            ? Fraction.of(count, 1)
            : new Fraction(
                totalStop.numerator().multiply(stop.denominator()),
                totalStop.denominator().multiply(stop.numerator()));
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
