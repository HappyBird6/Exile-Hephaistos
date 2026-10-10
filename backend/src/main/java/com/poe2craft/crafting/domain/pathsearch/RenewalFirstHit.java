package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;

/** Only callers holding a completed full-state renewal proof may use this distribution. */
public final class RenewalFirstHit {
  private RenewalFirstHit() {}

  public record Mass(Fraction hit, Fraction active, Fraction dead, Fraction unresolved) {}

  public static Fraction complement(Fraction value) {
    return new Fraction(value.denominator().subtract(value.numerator()), value.denominator());
  }

  public static Mass at(
      Fraction success,
      Fraction unknown,
      int cycleLength,
      boolean rootHit,
      boolean emptyHit,
      long attempts,
      int fractionBits) {
    if (attempts < 0 || (cycleLength != 1 && cycleLength != 2))
      throw new IllegalArgumentException("Invalid observation");
    if (rootHit || (cycleLength == 2 && emptyHit && attempts >= 1))
      return new Mass(Fraction.ONE, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO);
    long cycles = attempts / cycleLength;
    var survival = complement(success.add(unknown));
    if (cycles == 0 || survival.equals(Fraction.ONE))
      return new Mass(Fraction.ZERO, Fraction.ONE, Fraction.ZERO, Fraction.ZERO);
    if (survival.equals(Fraction.ZERO))
      return new Mass(success, Fraction.ZERO, Fraction.ZERO, unknown);
    // Arithmetic/output cap is distinct from a game attempt limit. Retain honest unknown bounds.
    int computed =
        (int) Math.min(cycles, fractionBits / Math.max(1, survival.denominator().bitLength()));
    var active =
        new Fraction(survival.numerator().pow(computed), survival.denominator().pow(computed));
    var absorbed = complement(active);
    var total = success.add(unknown);
    var hit =
        total.equals(Fraction.ZERO)
            ? Fraction.ZERO
            : absorbed.multiply(
                new Fraction(
                    success.numerator().multiply(total.denominator()),
                    success.denominator().multiply(total.numerator())));
    var unresolved =
        new Fraction(
            absorbed
                .numerator()
                .multiply(hit.denominator())
                .subtract(hit.numerator().multiply(absorbed.denominator())),
            absorbed.denominator().multiply(hit.denominator()));
    return cycles > computed
        ? new Mass(hit, Fraction.ZERO, Fraction.ZERO, complement(hit))
        : new Mass(hit, active, Fraction.ZERO, unresolved);
  }
}
