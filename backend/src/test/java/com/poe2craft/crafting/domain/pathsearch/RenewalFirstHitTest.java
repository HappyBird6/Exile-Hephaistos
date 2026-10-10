package com.poe2craft.crafting.domain.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import org.junit.jupiter.api.Test;

class RenewalFirstHitTest {
  @Test
  void independentBinaryEnumerationAndTwoPhaseFirstHit() {
    for (int depth = 0; depth <= 12; depth++) {
      int hits = 0;
      for (int path = 0; path < (1 << depth); path++) if (path != 0) hits++;
      var expected = Fraction.of(hits, 1 << depth);
      assertThat(
              RenewalFirstHit.at(Fraction.of(1, 2), Fraction.ZERO, 1, false, false, depth, 65536)
                  .hit())
          .isEqualTo(expected);
      assertThat(
              RenewalFirstHit.at(
                      Fraction.of(1, 2), Fraction.ZERO, 2, false, false, depth * 2L + 1, 65536)
                  .hit())
          .isEqualTo(expected);
    }
    assertThat(RenewalFirstHit.at(Fraction.ZERO, Fraction.ZERO, 2, false, true, 1, 65536).hit())
        .isEqualTo(Fraction.ONE);
  }

  @Test
  void unknownMassIsAbsorbingAndOutputBudgetNeverInventsSuccess() {
    var result =
        RenewalFirstHit.at(Fraction.of(1, 4), Fraction.of(1, 4), 1, false, false, 2, 65536);
    assertThat(result.hit()).isEqualTo(Fraction.of(3, 8));
    assertThat(result.unresolved()).isEqualTo(Fraction.of(3, 8));
    assertThat(result.active()).isEqualTo(Fraction.of(1, 4));
    var capped =
        RenewalFirstHit.at(Fraction.of(1, 3), Fraction.ZERO, 1, false, false, Long.MAX_VALUE, 8);
    assertThat(capped.hit()).isEqualTo(Fraction.of(65, 81));
    assertThat(capped.unresolved()).isEqualTo(Fraction.of(16, 81));
    assertThat(capped.active()).isEqualTo(Fraction.ZERO);
  }
}
