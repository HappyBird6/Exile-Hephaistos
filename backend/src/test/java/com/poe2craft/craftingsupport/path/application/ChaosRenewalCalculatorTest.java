package com.poe2craft.craftingsupport.path.application;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.probability.Fraction;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 전체 상태의 갱신 조건이 증명된 카오스 반복 확률을 계산한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code zeroOneAndAlreadyHitDoNotAllocateHugePowers()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다.
 * </ul>
 */
class ChaosRenewalCalculatorTest {
  @Test
  void zeroOneAndAlreadyHitDoNotAllocateHugePowers() {
    for (var p : List.of(Fraction.ZERO, Fraction.ONE)) {
      var result =
          ChaosRenewalCalculator.calculate(
              p, false, List.of(0L, Long.MAX_VALUE), 65536, () -> false);
      assertThat(result.points().getFirst().lower()).isEqualTo(Fraction.ZERO);
      assertThat(result.points().getLast().lower()).isEqualTo(p);
      assertThat(result.points().getLast().unresolved()).isEqualTo(Fraction.ZERO);
    }
    var hit =
        ChaosRenewalCalculator.calculate(
            Fraction.of(1, 2), true, List.of(0L, Long.MAX_VALUE), 65536, () -> true);
    assertThat(hit.points().getFirst().lower()).isEqualTo(Fraction.ONE);
    assertThat(hit.points().getLast().lower()).isEqualTo(Fraction.ONE);
  }
}
