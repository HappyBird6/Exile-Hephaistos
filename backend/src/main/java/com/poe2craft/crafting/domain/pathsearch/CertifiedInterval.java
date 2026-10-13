package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import java.math.*;
import java.util.Optional;

/**
 * Nonnegative dyadic interval. Every rounding operation uses integer floor/ceiling, never double.
 */
public record CertifiedInterval(int fractionalBits, BigInteger lowerUnits, BigInteger upperUnits) {
  public static final String VERSION = "outward-dyadic-interval-v1";

  public record Precision(int fractionalBits, int percentDecimals) {
    public Precision {
      if (fractionalBits < 16
          || fractionalBits > 256
          || percentDecimals < 0
          || percentDecimals > 12)
        throw new IllegalArgumentException("Unsupported precision policy");
    }

    public static Precision standard() {
      return new Precision(64, 2);
    }
  }

  public CertifiedInterval {
    if (fractionalBits < 16
        || fractionalBits > 256
        || lowerUnits.signum() < 0
        || upperUnits.compareTo(lowerUnits) < 0)
      throw new IllegalArgumentException("Invalid nonnegative interval");
  }

  private BigInteger scale() {
    return BigInteger.ONE.shiftLeft(fractionalBits);
  }

  public static CertifiedInterval zero(int bits) {
    return new CertifiedInterval(bits, BigInteger.ZERO, BigInteger.ZERO);
  }

  public static CertifiedInterval one(int bits) {
    new Precision(bits, 0);
    var one = BigInteger.ONE.shiftLeft(bits);
    return new CertifiedInterval(bits, one, one);
  }

  private static BigInteger ceil(BigInteger numerator, BigInteger denominator) {
    var division = numerator.divideAndRemainder(denominator);
    return division[1].signum() == 0 ? division[0] : division[0].add(BigInteger.ONE);
  }

  public static CertifiedInterval enclose(Fraction value, int bits) {
    new Precision(bits, 0);
    if (value.numerator().signum() < 0) throw new IllegalArgumentException("Negative value");
    var scaled = value.numerator().shiftLeft(bits);
    return new CertifiedInterval(
        bits, scaled.divide(value.denominator()), ceil(scaled, value.denominator()));
  }

  private void same(CertifiedInterval other) {
    if (fractionalBits != other.fractionalBits)
      throw new IllegalArgumentException("Precision mismatch");
  }

  public CertifiedInterval add(CertifiedInterval other) {
    same(other);
    return new CertifiedInterval(
        fractionalBits, lowerUnits.add(other.lowerUnits), upperUnits.add(other.upperUnits));
  }

  public CertifiedInterval multiply(CertifiedInterval other) {
    same(other);
    return new CertifiedInterval(
        fractionalBits,
        lowerUnits.multiply(other.lowerUnits).shiftRight(fractionalBits),
        ceil(upperUnits.multiply(other.upperUnits), scale()));
  }

  /** Intersect only when the caller independently knows the quantity is a probability. */
  public CertifiedInterval probability() {
    return new CertifiedInterval(fractionalBits, lowerUnits, upperUnits.min(scale()));
  }

  public CertifiedInterval complement() {
    if (upperUnits.compareTo(scale()) > 0)
      throw new IllegalArgumentException("Unit interval required");
    return new CertifiedInterval(
        fractionalBits, scale().subtract(upperUnits), scale().subtract(lowerUnits));
  }

  public boolean contains(Fraction value) {
    var scaled = value.numerator().shiftLeft(fractionalBits);
    return lowerUnits.multiply(value.denominator()).compareTo(scaled) <= 0
        && upperUnits.multiply(value.denominator()).compareTo(scaled) >= 0;
  }

  public Fraction lower() {
    return new Fraction(lowerUnits, scale());
  }

  public Fraction upper() {
    return new Fraction(upperUnits, scale());
  }

  public boolean exact() {
    return lowerUnits.equals(upperUnits);
  }

  public boolean zero() {
    return upperUnits.signum() == 0;
  }

  public Optional<String> confirmedPercent(int decimals) {
    new Precision(fractionalBits, decimals);
    if (upperUnits.compareTo(scale()) > 0)
      throw new IllegalArgumentException("Probability required");
    var denominator = new BigDecimal(scale());
    var lower =
        new BigDecimal(lowerUnits.multiply(BigInteger.valueOf(100)))
            .divide(denominator, decimals, RoundingMode.HALF_UP);
    var upper =
        new BigDecimal(upperUnits.multiply(BigInteger.valueOf(100)))
            .divide(denominator, decimals, RoundingMode.HALF_UP);
    return lower.equals(upper) ? Optional.of(lower.toPlainString()) : Optional.empty();
  }
}
