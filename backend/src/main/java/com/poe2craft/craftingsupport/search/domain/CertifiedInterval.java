package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.probability.Fraction;
import java.math.*;
import java.util.Optional;

/**
 * 정수 내림·올림 연산으로 정확값을 감싸는 음이 아닌 이진 유리수 구간이다.
 *
 * <p>기존 계약: Nonnegative dyadic interval. Every rounding operation uses integer floor/ceiling, never
 * double.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code fractionalBits}: fractional·비트수 ({@code int}).
 *   <li>{@code lowerUnits}: 하한·단위목록 ({@code BigInteger}).
 *   <li>{@code upperUnits}: 상한·단위목록 ({@code BigInteger}).
 *   <li>{@code VERSION}: 버전; 초기값 {@code "outward-dyadic-interval-v1"} ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CertifiedInterval(fractionalBits, lowerUnits, upperUnits)}: 전달받은 값으로 구성원을 초기화한다. 입력:
 *       {@code fractionalBits}(fractional·비트수), {@code lowerUnits}(하한·단위목록), {@code
 *       upperUnits}(상한·단위목록). 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code scale()}: 단위배율 값을 조회·구성한다. 반환: {@code BigInteger}.
 *   <li>{@code zero(bits)}: 영에 해당하는 값을 제공한다. 입력: {@code bits}(비트수). 반환: {@code CertifiedInterval}.
 *   <li>{@code one(bits)}: 일에 해당하는 값을 제공한다. 입력: {@code bits}(비트수). 반환: {@code CertifiedInterval}.
 *   <li>{@code ceil(numerator, denominator)}: ceil 처리에서 {@code numerator.divideAndRemainder}을(를)
 *       사용한다. 입력: {@code numerator}(분자), {@code denominator}(분모). 반환: {@code BigInteger}.
 *   <li>{@code enclose(value, bits)}: enclose 처리에서 {@code value.numerator}, {@code
 *       scaled.divide}을(를) 사용한다. 입력: {@code value}(값), {@code bits}(비트수). 반환: {@code
 *       CertifiedInterval}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code same(other)}: 동일의 입력에 따른 분기·결과를 처리한다. 입력: {@code other}(비교대상). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code add(other)}: 값 또는 항목을 더한다. 입력: {@code other}(비교대상). 반환: {@code CertifiedInterval}.
 *   <li>{@code multiply(other)}: 두 값의 곱을 계산한다. 입력: {@code other}(비교대상). 반환: {@code
 *       CertifiedInterval}.
 *   <li>{@code probability()}: 현재 조건의 확률을 조회한다. 반환: {@code CertifiedInterval}.
 *   <li>{@code complement()}: complement 값을 조회·구성한다. 반환: {@code CertifiedInterval}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code contains(value)}: 해당 항목의 포함 여부를 확인한다. 입력: {@code value}(값). 반환: {@code boolean}.
 *   <li>{@code lower()}: 하한 값을 조회·구성한다. 반환: {@code Fraction}.
 *   <li>{@code upper()}: 상한 값을 조회·구성한다. 반환: {@code Fraction}.
 *   <li>{@code exact()}: 정확값 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code zero()}: 영에 해당하는 값을 제공한다. 반환: {@code boolean}.
 *   <li>{@code confirmedPercent(decimals)}: confirmed·백분율 처리에서 {@code upperUnits.compareTo}, {@code
 *       lowerUnits.multiply}을(를) 사용한다. 입력: {@code decimals}(소수자릿수). 반환: {@code Optional<String>}.
 *       사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Precision}: {@code CertifiedInterval}에서 사용하는 정밀도의 불변 값이다.
 * </ul>
 */
public record CertifiedInterval(int fractionalBits, BigInteger lowerUnits, BigInteger upperUnits) {
  public static final String VERSION = "outward-dyadic-interval-v1";

  /**
   * {@code CertifiedInterval}에서 사용하는 정밀도의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code fractionalBits}: fractional·비트수 ({@code int}).
   *   <li>{@code percentDecimals}: 백분율·소수자릿수 ({@code int}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Precision(fractionalBits, percentDecimals)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       fractionalBits}(fractional·비트수), {@code percentDecimals}(백분율·소수자릿수). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   *   <li>{@code standard()}: standard 값을 조회·구성한다. 반환: {@code Precision}.
   * </ul>
   */
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
