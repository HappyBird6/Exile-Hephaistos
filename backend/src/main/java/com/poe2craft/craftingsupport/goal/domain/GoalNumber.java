package com.poe2craft.craftingsupport.goal.domain;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * 목표 경계 비교에 사용하는 정확한 유리수 관측값이다.
 *
 * <p>기존 계약: Rational observations: decimal presentation never decides an inclusive goal boundary.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code numerator}: 분자 ({@code BigDecimal}).
 *   <li>{@code denominator}: 분모 ({@code BigDecimal}).
 *   <li>{@code ZERO}: 영 ({@code GoalNumber}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code GoalNumber(numerator, denominator)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code numerator}(분자),
 *       {@code denominator}(분모). 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code of(value)}: 입력값으로 검증된 값 객체를 만든다. 입력: {@code value}(값). 반환: {@code GoalNumber}.
 *   <li>{@code add(other)}: 값 또는 항목을 더한다. 입력: {@code other}(비교대상). 반환: {@code GoalNumber}.
 *   <li>{@code multiply(coefficient)}: 두 값의 곱을 계산한다. 입력: {@code coefficient}(계수). 반환: {@code
 *       GoalNumber}.
 *   <li>{@code within(range)}: within 처리에서 {@code range.min}, {@code numerator.compareTo}을(를) 사용한다.
 *       입력: {@code range}(범위). 반환: {@code boolean}.
 *   <li>{@code display()}: 표시 값을 조회·구성한다. 반환: {@code BigDecimal}.
 *   <li>{@code ratio()}: 비율 값을 조회·구성한다. 반환: {@code Ratio}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Ratio}: {@code GoalNumber}에서 사용하는 비율의 불변 값이다.
 * </ul>
 */
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

  /**
   * {@code GoalNumber}에서 사용하는 비율의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code numerator}: 분자 ({@code String}).
   *   <li>{@code denominator}: 분모 ({@code String}).
   * </ul>
   */
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
