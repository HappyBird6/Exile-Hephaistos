package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.probability.Fraction;

/**
 * 완료된 전체 상태 갱신 증명을 전제로 최초 성공 분포를 계산한다.
 *
 * <p>기존 계약: Only callers holding a completed full-state renewal proof may use this distribution.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code RenewalFirstHit()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code complement(value)}: complement 처리에서 {@code value.complement}을(를) 사용한다. 입력: {@code
 *       value}(값). 반환: {@code Fraction}.
 *   <li>{@code at(success, unknown, cycleLength, rootHit, emptyHit, attempts, fractionBits)}: 기준
 *       처리에서 {@code success.add}, {@code survival.equals}을(를) 사용한다. 입력: {@code success}(성공), {@code
 *       unknown}(미확정), {@code cycleLength}(주기·길이), {@code rootHit}(시작점·목표도달), {@code
 *       emptyHit}(빈값·목표도달), {@code attempts}(시도수), {@code fractionBits}(유리수·비트수). 반환: {@code Mass}.
 *       사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Mass}: {@code RenewalFirstHit}에서 사용하는 확률질량의 불변 값이다.
 * </ul>
 */
public final class RenewalFirstHit {
  private RenewalFirstHit() {}

  /**
   * {@code RenewalFirstHit}에서 사용하는 확률질량의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code hit}: 목표도달 ({@code Fraction}).
   *   <li>{@code active}: 활성 ({@code Fraction}).
   *   <li>{@code dead}: 종료된 ({@code Fraction}).
   *   <li>{@code unresolved}: 미해결 ({@code Fraction}).
   * </ul>
   */
  public record Mass(Fraction hit, Fraction active, Fraction dead, Fraction unresolved) {}

  public static Fraction complement(Fraction value) {
    return value.complement();
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
    var active = survival.pow(computed);
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
