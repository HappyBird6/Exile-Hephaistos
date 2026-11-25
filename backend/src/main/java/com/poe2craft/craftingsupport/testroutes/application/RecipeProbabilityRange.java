package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.shared.probability.Fraction;
import java.math.BigDecimal;
import java.math.MathContext;

/** 열거된 배타적 분기와 아직 배정되지 않은 원래 질량으로 성공 확률의 보수적인 경계를 계산한다. */
record RecipeProbabilityRange(Fraction lower, Fraction upper) {
  /** 실제 성공·실패로 관측한 질량을 사용한다. 모두 열거했으면 티어별 최대값도 상한으로 사용할 수 있다. */
  static RecipeProbabilityRange event(
      RevealTierMass hits, boolean complete, Fraction parentCap, Fraction failures) {
    var observedMax = hits.maximum();
    Fraction cap =
        complete && observedMax != null
            ? min(parentCap, bound(observedMax, true))
            : subtract(parentCap, bound(failures, false));
    return new RecipeProbabilityRange(bound(hits.minimum(), false), cap);
  }

  /** 검증된 분모·분자의 서로 반대 경계를 사용한다. 분모가 0에 가까워질 수 있다면 조건부 상한은 1이다. */
  RecipeProbabilityRange given(RecipeProbabilityRange denominator) {
    var lo =
        Fraction.ZERO.equals(denominator.upper) ? Fraction.ZERO : lower.divide(denominator.upper);
    var hi =
        Fraction.ZERO.equals(denominator.lower) ? Fraction.ONE : upper.divide(denominator.lower);
    return new RecipeProbabilityRange(min(lo, Fraction.ONE), min(hi, Fraction.ONE));
  }

  /** 미적중 준비 조건을 반영한다. 미적중 분모가 미상이면 앞에서 이미 확정한 경로 상한만 적용한다. */
  RecipeProbabilityRange displayed(Fraction factorLow, Fraction factorHigh, Fraction prefixCap) {
    var hi = factorHigh == null ? prefixCap : min(prefixCap, upper.multiply(factorHigh));
    return new RecipeProbabilityRange(min(Fraction.ONE, lower.multiply(factorLow)), hi);
  }

  /** 양 끝점이 같으면 확률을 하나로 확정한다. */
  Fraction exact() {
    return lower.equals(upper) ? lower : null;
  }

  /** 양 끝을 바깥쪽으로 반올림하여 직렬화된 범위에도 실제 확률이 포함되게 한다. */
  RecipeRoutePlanner.ProbabilityBounds wire() {
    return new RecipeRoutePlanner.ProbabilityBounds(outward(lower, false), outward(upper, true));
  }

  /** 분수에서 IEEE 754 수치로 바꾼 근사값이 실제 경계 안쪽이면 인접한 바깥 수를 사용한다. */
  private static double outward(Fraction f, boolean upwards) {
    if (Fraction.ZERO.equals(f)) return 0;
    if (Fraction.ONE.equals(f)) return 1;
    var num = new BigDecimal(f.numerator());
    var den = new BigDecimal(f.denominator());
    double rounded = num.divide(den, MathContext.DECIMAL128).doubleValue();
    int comparison = new BigDecimal(rounded).multiply(den).compareTo(num);
    if (upwards && comparison < 0) return Math.nextUp(rounded);
    if (!upwards && comparison > 0) return Math.nextDown(rounded);
    return rounded;
  }

  /** 반복 확률의 수천 자리 분모가 단계마다 다시 곱해지지 않게 구간 값만 바깥쪽 이진 격자로 반올림한다. */
  static Fraction bound(Fraction value, boolean upper) {
    if (value.denominator().bitLength() <= 128) return value;
    // 매우 작은 양의 획득률도 하한이 0으로 사라지지 않게 유효 숫자 96비트를 유지한다.
    int precision =
        96 + Math.max(0, value.denominator().bitLength() - value.numerator().bitLength());
    var scale = java.math.BigInteger.ONE.shiftLeft(precision);
    var parts = value.numerator().multiply(scale).divideAndRemainder(value.denominator());
    return new Fraction(
        upper && parts[1].signum() != 0 ? parts[0].add(java.math.BigInteger.ONE) : parts[0], scale);
  }

  /** 음수가 될 수 없는 잔여 질량이다. 구간의 서로 다른 끝을 뺄 때에만 0을 바닥으로 둔다. */
  static Fraction subtract(Fraction total, Fraction part) {
    if (Fraction.ZERO.equals(part)) return total;
    if (total.equals(part)) return Fraction.ZERO;
    var gcd = total.denominator().gcd(part.denominator());
    var leftScale = part.denominator().divide(gcd);
    var rightScale = total.denominator().divide(gcd);
    var numerator =
        total.numerator().multiply(leftScale).subtract(part.numerator().multiply(rightScale));
    return numerator.signum() > 0
        ? new Fraction(numerator, total.denominator().multiply(leftScale))
        : Fraction.ZERO;
  }

  /** 분모가 다른 두 정확한 경계 중 작은 값을 택한다. */
  static Fraction min(Fraction a, Fraction b) {
    return a == b
            || a.numerator()
                    .multiply(b.denominator())
                    .compareTo(b.numerator().multiply(a.denominator()))
                <= 0
        ? a
        : b;
  }
}
