package com.poe2craft.shared.item;

import java.math.BigInteger;

/**
 * 검증 전까지 사용하는 명시적인 앱 수치 반올림 가정이다.
 *
 * <p>기존 계약: User-selected simulator assumption; replace after game precision is verified.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code QualityRoundingPolicy()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code roundRatio(numerator, denominator)}: 반올림·비율 처리에서 {@code denominator.signum}, {@code
 *       numerator.abs}을(를) 사용한다. 입력: {@code numerator}(분자), {@code denominator}(분모). 반환: {@code
 *       long}. 사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 */
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
