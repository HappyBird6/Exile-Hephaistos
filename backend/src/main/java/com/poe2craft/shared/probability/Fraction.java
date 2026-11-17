package com.poe2craft.shared.probability;

import java.math.BigInteger;
import java.util.Objects;

/**
 * 제작과 탐색에서 사용하는 정확한 음이 아닌 유리수 연산이다.
 *
 * <p>기존 계약: Exact nonnegative rational arithmetic shared by crafting and search.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code value}: 값 ({@code Value}).
 *   <li>{@code cachedHash}: 캐시된·해시 ({@code long}).
 *   <li>{@code ZERO}: 영 ({@code Fraction}).
 *   <li>{@code ONE}: 일 ({@code Fraction}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code Fraction(numerator, denominator)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
 *       numerator}(분자), {@code denominator}(분모).
 *   <li>{@code Fraction(numerator, denominator, reduced)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       numerator}(분자), {@code denominator}(분모), {@code reduced}(reduced). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code Fraction(exactSum)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code exactSum}(정확값·합계).
 *   <li>{@code canonical()}: 정규형 값을 조회·구성한다. 반환: {@code Value}.
 *   <li>{@code numerator()}: 분자 값을 조회·구성한다. 반환: {@code BigInteger}.
 *   <li>{@code denominator()}: 분모 값을 조회·구성한다. 반환: {@code BigInteger}.
 *   <li>{@code equals(other)}: 값의 동등성을 비교한다. 입력: {@code other}(비교대상). 반환: {@code boolean}.
 *   <li>{@code hashCode()}: 동등성 비교와 일치하는 해시를 반환한다. 반환: {@code int}.
 *   <li>{@code toString()}: 값을 문자열 표현으로 바꾼다. 반환: {@code String}.
 *   <li>{@code of(numerator, denominator)}: 입력값으로 검증된 값 객체를 만든다. 입력: {@code numerator}(분자), {@code
 *       denominator}(분모). 반환: {@code Fraction}.
 *   <li>{@code add(other)}: 값 또는 항목을 더한다. 입력: {@code other}(비교대상). 반환: {@code Fraction}.
 *   <li>{@code multiply(other)}: 두 값의 곱을 계산한다. 입력: {@code other}(비교대상). 반환: {@code Fraction}.
 *   <li>{@code multiplyEquals(other, expected)}: multiply·Equals 처리에서 {@code a.numerator}, {@code
 *       b.numerator}을(를) 사용한다. 입력: {@code other}(비교대상), {@code expected}(기대값). 반환: {@code boolean}.
 *   <li>{@code pow(exponent)}: pow 처리에서 {@code v.numerator}, {@code v.denominator}을(를) 사용한다. 입력:
 *       {@code exponent}(exponent). 반환: {@code Fraction}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code complement()}: complement 값을 조회·구성한다. 반환: {@code Fraction}.
 *   <li>{@code divide(other)}: 두 값의 비를 계산한다. 입력: {@code other}(비교대상). 반환: {@code Fraction}. 사용하는
 *       예외: {@code IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Value}: {@code Fraction}에서 사용하는 값의 불변 값이다.
 * </ul>
 */
public final class Fraction {
  /**
   * {@code Fraction}에서 사용하는 값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code numerator}: 분자 ({@code BigInteger}).
   *   <li>{@code denominator}: 분모 ({@code BigInteger}).
   *   <li>{@code reduced}: reduced ({@code boolean}).
   * </ul>
   */
  private record Value(BigInteger numerator, BigInteger denominator, boolean reduced) {}

  // Atomic immutable snapshots permit concurrent canonical reads while an exact sum is lazy.
  private volatile Value value;
  private volatile long cachedHash;
  public static final Fraction ZERO = of(0, 1);
  public static final Fraction ONE = of(1, 1);

  @com.fasterxml.jackson.annotation.JsonCreator
  public Fraction(
      @com.fasterxml.jackson.annotation.JsonProperty("numerator") BigInteger numerator,
      @com.fasterxml.jackson.annotation.JsonProperty("denominator") BigInteger denominator) {
    this(numerator, denominator, false);
  }

  // Only arithmetic below may assert reducedness, from already canonical operands.
  private Fraction(BigInteger numerator, BigInteger denominator, boolean reduced) {
    if (denominator.signum() <= 0 || numerator.signum() < 0)
      throw new IllegalArgumentException("Invalid probability fraction");
    if (reduced) value = new Value(numerator, denominator, true);
    else {
      var divisor = numerator.gcd(denominator);
      value = new Value(numerator.divide(divisor), denominator.divide(divisor), true);
    }
  }

  private Fraction(Value exactSum) {
    value = exactSum;
  }

  private Value canonical() {
    var current = value;
    if (current.reduced()) return current;
    var divisor = current.numerator().gcd(current.denominator());
    var result =
        new Value(current.numerator().divide(divisor), current.denominator().divide(divisor), true);
    value = result;
    return result;
  }

  @com.fasterxml.jackson.annotation.JsonProperty
  public BigInteger numerator() {
    return canonical().numerator();
  }

  @com.fasterxml.jackson.annotation.JsonProperty
  public BigInteger denominator() {
    return canonical().denominator();
  }

  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof Fraction f && canonical().equals(f.canonical());
  }

  @Override
  public int hashCode() {
    var known = cachedHash;
    if (known != 0) return (int) known;
    var v = canonical();
    int result = 31 * v.numerator().hashCode() + v.denominator().hashCode();
    // The high bit is a presence marker; all 32-bit hash values, including zero, remain valid.
    cachedHash = 0x100000000L | Integer.toUnsignedLong(result);
    return result;
  }

  @Override
  public String toString() {
    var v = canonical();
    return "Fraction[numerator=" + v.numerator() + ", denominator=" + v.denominator() + "]";
  }

  public static Fraction of(long numerator, long denominator) {
    return new Fraction(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
  }

  public Fraction add(Fraction other) {
    Objects.requireNonNull(other);
    var a = value;
    var b = other.value;
    var numerator = a.numerator();
    var denominator = a.denominator();
    if (numerator.signum() == 0) return other;
    if (b.numerator().signum() == 0) return this;
    if (denominator.equals(b.denominator()))
      return new Fraction(new Value(numerator.add(b.numerator()), denominator, false));
    // Markov exit laws often share a large denominator. Form their LCM instead
    // of squaring that denominator before reducing every accumulated outcome.
    var common = denominator.gcd(b.denominator());
    var leftScale = b.denominator().divide(common);
    var rightScale = denominator.divide(common);
    var sum = numerator.multiply(leftScale).add(b.numerator().multiply(rightScale));
    return new Fraction(new Value(sum, rightScale.multiply(b.denominator()), false));
  }

  public Fraction multiply(Fraction other) {
    Objects.requireNonNull(other);
    var a = canonical();
    var b = other.canonical();
    var numerator = a.numerator();
    var denominator = a.denominator();
    if (numerator.signum() == 0 || b.numerator().signum() == 0) return ZERO;
    if (equals(ONE)) return other;
    if (other.equals(ONE)) return this;
    var left = numerator.gcd(b.denominator());
    var right = b.numerator().gcd(denominator);
    return new Fraction(
        numerator.divide(left).multiply(b.numerator().divide(right)),
        denominator.divide(right).multiply(b.denominator().divide(left)),
        true);
  }

  /** Exact product comparison without constructing and reducing a temporary rational. */
  public boolean multiplyEquals(Fraction other, Fraction expected) {
    var a = value;
    var b = Objects.requireNonNull(other).value;
    var c = Objects.requireNonNull(expected).value;
    return a.numerator()
        .multiply(b.numerator())
        .multiply(c.denominator())
        .equals(c.numerator().multiply(a.denominator()).multiply(b.denominator()));
  }

  public Fraction pow(int exponent) {
    if (exponent < 0) throw new IllegalArgumentException("Negative exponent");
    var v = canonical();
    return new Fraction(v.numerator().pow(exponent), v.denominator().pow(exponent), true);
  }

  public Fraction complement() {
    var v = canonical();
    return new Fraction(v.denominator().subtract(v.numerator()), v.denominator(), true);
  }

  public Fraction divide(Fraction other) {
    var v = Objects.requireNonNull(other).canonical();
    if (v.numerator().signum() == 0) throw new IllegalArgumentException("Division by zero");
    return multiply(new Fraction(v.denominator(), v.numerator(), true));
  }
}
