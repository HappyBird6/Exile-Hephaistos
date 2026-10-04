package com.poe2craft.item;

import java.math.BigInteger;

/** User-selected simulator assumption; replace after game precision is verified. */
public final class QualityRoundingPolicy {
  private QualityRoundingPolicy() {}

  public static long roundRatio(BigInteger numerator, BigInteger denominator) {
    if (denominator.signum() <= 0)
      throw new IllegalArgumentException("Positive denominator required");
    return numerator
        .abs()
        .add(denominator.divide(BigInteger.TWO))
        .divide(denominator)
        .multiply(BigInteger.valueOf(numerator.signum()))
        .longValueExact();
  }
}
