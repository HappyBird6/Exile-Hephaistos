package com.poe2craft.crafting.domain.goalfilter;

import java.math.BigDecimal;
import java.math.MathContext;

/** Rational observations: decimal presentation never decides an inclusive goal boundary. */
public record GoalNumber(BigDecimal numerator, BigDecimal denominator) {
  public static final GoalNumber ZERO = of(BigDecimal.ZERO);

  public GoalNumber {
    if (numerator == null || denominator == null || denominator.signum() <= 0)
      throw new IllegalArgumentException("Positive denominator required");
    if (numerator.stripTrailingZeros().scale() <= 0
        && denominator.stripTrailingZeros().scale() <= 0) {
      var n = numerator.toBigIntegerExact();
      var d = denominator.toBigIntegerExact();
      var gcd = n.gcd(d);
      numerator = new BigDecimal(n.divide(gcd));
      denominator = new BigDecimal(d.divide(gcd));
    }
  }

  public static GoalNumber of(BigDecimal value) {
    return new GoalNumber(value, BigDecimal.ONE);
  }

  public GoalNumber add(GoalNumber other) {
    return new GoalNumber(
        numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
        denominator.multiply(other.denominator));
  }

  public GoalNumber multiply(BigDecimal coefficient) {
    return new GoalNumber(numerator.multiply(coefficient), denominator);
  }

  public boolean within(GoalFilter.Range range) {
    return (range.min() == null || numerator.compareTo(range.min().multiply(denominator)) >= 0)
        && (range.max() == null || numerator.compareTo(range.max().multiply(denominator)) <= 0);
  }

  public BigDecimal display() {
    try {
      return numerator.divide(denominator);
    } catch (ArithmeticException repeating) {
      return numerator.divide(denominator, MathContext.DECIMAL128);
    }
  }

  public record Ratio(String numerator, String denominator) {}

  /** Only non-terminating presentation needs an explicit exact fraction alongside it. */
  public Ratio ratio() {
    try {
      numerator.divide(denominator);
      return null;
    } catch (ArithmeticException repeating) {
      return new Ratio(numerator.toPlainString(), denominator.toPlainString());
    }
  }
}
