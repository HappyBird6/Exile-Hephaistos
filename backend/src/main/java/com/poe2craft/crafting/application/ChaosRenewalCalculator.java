package com.poe2craft.crafting.application;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Only consumes a domain-verified constant post-removal kernel, never an assumed generic p. */
final class ChaosRenewalCalculator {
  static FirstHitCalculator.Result calculate(
      Fraction p,
      boolean alreadyHit,
      List<Long> observations,
      int fractionBits,
      BooleanSupplier cancelled) {
    var sorted = FirstHitCalculator.observationPoints(observations);
    var failure = new Fraction(p.denominator().subtract(p.numerator()), p.denominator());
    int safeAttempts = fractionBits / 4 / failure.denominator().bitLength();
    var points = new ArrayList<FirstHitCalculator.Point>();
    var previousHit = Fraction.ZERO;
    int observedBits = 1;
    String reason = "PROVEN_SINGLE_EXPLICIT_CHAOS_RENEWAL";
    for (long n : sorted) {
      Fraction hit, active, unresolved;
      if (alreadyHit) {
        hit = Fraction.ONE;
        active = Fraction.ZERO;
        unresolved = Fraction.ZERO;
      } else if (n == 0 || p.equals(Fraction.ZERO)) {
        hit = Fraction.ZERO;
        active = Fraction.ONE;
        unresolved = Fraction.ZERO;
      } else if (p.equals(Fraction.ONE)) {
        hit = Fraction.ONE;
        active = Fraction.ZERO;
        unresolved = Fraction.ZERO;
      } else if (cancelled.getAsBoolean()) {
        hit = previousHit;
        active = Fraction.ZERO;
        unresolved = new Fraction(hit.denominator().subtract(hit.numerator()), hit.denominator());
        reason = "RENEWAL_CANCELLED";
      } else {
        int computed = (int) Math.min(n, safeAttempts);
        var survival =
            new Fraction(failure.numerator().pow(computed), failure.denominator().pow(computed));
        hit =
            new Fraction(
                survival.denominator().subtract(survival.numerator()), survival.denominator());
        if (n > safeAttempts) {
          active = Fraction.ZERO;
          unresolved = survival;
          reason = "RENEWAL_FRACTION_OUTPUT_LIMIT";
        } else {
          active = survival;
          unresolved = Fraction.ZERO;
        }
      }
      previousHit = hit;
      var upper = hit.add(unresolved);
      observedBits =
          Math.max(
              observedBits,
              Math.max(hit.denominator().bitLength(), upper.denominator().bitLength()));
      points.add(
          new FirstHitCalculator.Point(
              n,
              hit,
              upper,
              active,
              Fraction.ZERO,
              unresolved,
              unresolved.equals(Fraction.ZERO)
                  ? "COMPLETE"
                  : hit.equals(Fraction.ZERO) ? "UNKNOWN" : "PARTIAL"));
    }
    return new FirstHitCalculator.Result(List.copyOf(points), 0, 1, observedBits, reason);
  }
}
