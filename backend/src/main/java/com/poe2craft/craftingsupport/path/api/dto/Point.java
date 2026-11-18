package com.poe2craft.craftingsupport.path.api.dto;

import com.poe2craft.craftingsupport.path.application.FirstHitCalculator;
import com.poe2craft.shared.probability.Fraction;

/**
 * attempts 시점의 성공 확률 하한·상한은 lower와 upper다. active, dead, unresolved는 진행 중·종료·미결 상태의 확률이며 status와
 * reachability는 계산 상태와 도달 가능성이다. of는 계산 지점을 응답으로 바꾼다.
 */
public record Point(
    String attempts,
    Probability lower,
    Probability upper,
    Probability active,
    Probability dead,
    Probability unresolved,
    String status,
    String reachability) {
  public static Point of(FirstHitCalculator.Point p) {
    return new Point(
        Long.toString(p.attempts()),
        Probability.of(p.lower()),
        Probability.of(p.upper()),
        Probability.of(p.active()),
        Probability.of(p.dead()),
        Probability.of(p.unresolved()),
        p.status(),
        !p.lower().equals(Fraction.ZERO)
            ? "REACHABLE_WITHIN_OBSERVATION"
            : !p.unresolved().equals(Fraction.ZERO) ? "UNKNOWN" : "NOT_REACHED_WITHIN_OBSERVATION");
  }
}
