package com.poe2craft.crafting.application;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import java.util.List;
import org.junit.jupiter.api.Test;

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
