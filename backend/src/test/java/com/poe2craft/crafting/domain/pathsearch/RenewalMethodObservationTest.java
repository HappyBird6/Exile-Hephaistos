package com.poe2craft.crafting.domain.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class RenewalMethodObservationTest {
  @Test
  void provenFullReturnMatchesExplicitMarkovAtAllRequestedCycleCounts() {
    var runner =
        new ExactMethodRunner<String, String>(
            "root",
            new Template<>("return", List.of(), List.of(s -> "add", s -> "remove")),
            (state, action) ->
                action.equals("add")
                    ? new Kernel<>(
                        List.of(
                            new Outcome<>("hit", Fraction.of(1, 4)),
                            new Outcome<>("lost", Fraction.of(1, 4)),
                            new Outcome<>("miss", Fraction.of(1, 2))),
                        Fraction.ZERO,
                        false)
                    : new Kernel<>(
                        List.of(new Outcome<>("root", Fraction.ONE)), Fraction.ZERO, false),
            state ->
                state.equals("hit")
                    ? Optional.of(Exit.GOAL)
                    : state.equals("lost") ? Optional.of(Exit.LOSS) : Optional.empty());
    runner.beginCycle();
    while (!runner.atBoundary()) runner.step();
    var one = runner.observation();
    for (int i = 2; i <= 500; i++) {
      runner.beginCycle();
      while (!runner.atBoundary()) runner.step();
      if (List.of(100, 300, 500).contains(i))
        assertThat(RenewalMethodObservation.repeat(one, "root", i)).isEqualTo(runner.observation());
    }
    var incorrect =
        new Observation<>(
            1,
            one.exits(),
            Map.of("different", Fraction.of(1, 2)),
            one.initialUses(),
            one.cycleUses());
    assertThatThrownBy(() -> RenewalMethodObservation.repeat(incorrect, "root", 100))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
