package com.poe2craft.crafting.domain.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class RenewalMethodObservationTest {
  @Test
  void proportionalActiveComponentsStillRequireCompleteMassValidation() {
    var exits = new EnumMap<Exit, Map<String, Fraction>>(Exit.class);
    for (var kind : Exit.values()) exits.put(kind, Map.of());
    var before =
        new Observation<String, String>(
            1, exits, Map.of("x", Fraction.of(1, 2)), Map.of(), Map.of());
    var after =
        new Observation<String, String>(
            2, exits, Map.of("x", Fraction.of(1, 4)), Map.of(), Map.of());
    assertThat(RenewalMethodObservation.repeatInvariant(before, after, 100)).isEmpty();
  }

  @Test
  void changedExitRepresentationDoesNotProveRenewal() {
    var beforeExits = new EnumMap<Exit, Map<String, Fraction>>(Exit.class);
    var afterExits = new EnumMap<Exit, Map<String, Fraction>>(Exit.class);
    for (var kind : Exit.values()) {
      beforeExits.put(kind, Map.of());
      afterExits.put(kind, Map.of());
    }
    beforeExits.put(Exit.GOAL, Map.of("original-cell", Fraction.of(1, 2)));
    afterExits.put(Exit.GOAL, Map.of("mixture-cell", Fraction.of(3, 4)));
    var before =
        new Observation<String, String>(
            1,
            beforeExits,
            Map.of("active", Fraction.of(1, 2)),
            Map.of(),
            Map.of("try", Fraction.ONE));
    var after =
        new Observation<String, String>(
            2,
            afterExits,
            Map.of("active", Fraction.of(1, 4)),
            Map.of(),
            Map.of("try", Fraction.of(3, 2)));
    assertThat(RenewalMethodObservation.repeatInvariant(before, after, 100)).isEmpty();
  }

  @Test
  void invariantConditionalLawAfterPreparationMatchesFullMarkovWithDistinctStatesAndCosts() {
    var runner =
        new ExactMethodRunner<String, String>(
            "root",
            new Template<>("prepare-then-retry", List.of(s -> "prepare"), List.of(s -> "try")),
            (state, action) ->
                action.equals("prepare")
                    ? new Kernel<>(
                        List.of(
                            new Outcome<>("goal", Fraction.of(1, 4)),
                            new Outcome<>("x", Fraction.of(1, 4)),
                            new Outcome<>("y", Fraction.of(1, 2))),
                        Fraction.ZERO,
                        false)
                    : new Kernel<>(
                        List.of(
                            new Outcome<>("goal", Fraction.of(1, 4)),
                            new Outcome<>("lost", Fraction.of(1, 4)),
                            new Outcome<>("x", Fraction.of(1, 6)),
                            new Outcome<>("y", Fraction.of(1, 3))),
                        Fraction.ZERO,
                        false),
            state ->
                state.equals("goal")
                    ? Optional.of(Exit.GOAL)
                    : state.equals("lost") ? Optional.of(Exit.LOSS) : Optional.empty());
    while (!runner.atBoundary()) runner.step();
    var prepared = runner.observation();
    assertThat(prepared.active()).hasSize(2);
    runner.beginCycle();
    while (!runner.atBoundary()) runner.step();
    var one = runner.observation();
    var hundred = RenewalMethodObservation.repeatInvariant(prepared, one, 100).orElseThrow();
    var power = java.math.BigInteger.ONE.shiftLeft(100);
    // Independent closed form: preparation succeeds with 1/4; each later trial absorbs
    // half of the remaining 3/4, equally into goal and loss.
    assertThat(hundred.exits().get(Exit.GOAL).get("goal"))
        .isEqualTo(
            new Fraction(
                power
                    .multiply(java.math.BigInteger.valueOf(5))
                    .subtract(java.math.BigInteger.valueOf(3)),
                power.multiply(java.math.BigInteger.valueOf(8))));
    assertThat(hundred.initialUses()).isEqualTo(Map.of("prepare", Fraction.ONE));
    assertThat(hundred.cycleUses().get("try"))
        .isEqualTo(
            new Fraction(
                power.subtract(java.math.BigInteger.ONE).multiply(java.math.BigInteger.valueOf(3)),
                power.multiply(java.math.BigInteger.TWO)));
    for (int cycle = 2; cycle <= 500; cycle++) {
      runner.beginCycle();
      while (!runner.atBoundary()) runner.step();
      if (List.of(100, 300, 500).contains(cycle))
        assertThat(RenewalMethodObservation.repeatInvariant(prepared, one, cycle))
            .contains(runner.observation());
    }
  }

  @Test
  void equalDisplayProgressDoesNotProveAnInvariantFullStateLaw() {
    var runner =
        new ExactMethodRunner<String, String>(
            "same-goal-tier-one",
            new Template<>("alternating", List.of(), List.of(s -> "change")),
            (state, action) ->
                new Kernel<>(
                    List.of(
                        new Outcome<>(
                            state.equals("same-goal-tier-one")
                                ? "same-goal-tier-two"
                                : "same-goal-tier-one",
                            Fraction.ONE)),
                    Fraction.ZERO,
                    false),
            state -> Optional.empty());
    var before = runner.observation();
    runner.beginCycle();
    while (!runner.atBoundary()) runner.step();
    assertThat(RenewalMethodObservation.repeatInvariant(before, runner.observation(), 100))
        .isEmpty();
  }

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
