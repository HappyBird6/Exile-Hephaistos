package com.poe2craft.crafting.domain.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ExactMethodRunnerTest {
  private static final Fraction HALF = Fraction.of(1, 2);

  private static Kernel<String> kernel(String... states) {
    return new Kernel<>(
        Arrays.stream(states)
            .map(state -> new Outcome<>(state, Fraction.of(1, states.length)))
            .toList(),
        Fraction.ZERO,
        false);
  }

  private static Optional<Exit> classify(String state) {
    return state.equals("goal") ? Optional.of(Exit.GOAL) : Optional.empty();
  }

  private static void finish(ExactMethodRunner<?, ?> runner) {
    while (!runner.atBoundary()) runner.step();
  }

  @Test
  void preparationAndCyclesHaveSeparateUsageAndFirstHitStopsBeforeRemoval() {
    var runner =
        new ExactMethodRunner<>(
            "empty",
            new Template<String, String>(
                "prepare-and-repeat", List.of(s -> "prepare"), List.of(s -> "exalt", s -> "annul")),
            (state, action) -> action.equals("exalt") ? kernel("goal", "miss") : kernel("ready"),
            ExactMethodRunnerTest::classify);
    assertThatThrownBy(runner::observation).isInstanceOf(IllegalStateException.class);
    finish(runner);
    assertThat(runner.observation().cycles()).isZero();
    runner.beginCycle();
    finish(runner);
    var first = runner.observation();
    assertThat(first.initialUses()).containsEntry("prepare", Fraction.ONE);
    assertThat(first.cycleUses()).containsEntry("exalt", Fraction.ONE).containsEntry("annul", HALF);
    assertThat(first.exits().get(Exit.GOAL)).containsEntry("goal", HALF);
    runner.beginCycle();
    finish(runner);
    assertThat(runner.observation().exits().get(Exit.GOAL))
        .containsEntry("goal", Fraction.of(3, 4));
    assertThat(first.exits().get(Exit.GOAL)).containsEntry("goal", HALF);
  }

  @Test
  void independentHistoryEnumerationMatchesChangingMarkovPoolAndMergedArrivals() {
    // Enumerate histories without state caching/merging. Unlike a geometric model, q changes at B.
    Transition<String, String> transition =
        (state, action) ->
            switch (state) {
              case "A" -> kernel("A", "B");
              case "B" -> kernel("A", "goal", "goal");
              default -> throw new AssertionError(state);
            };
    var runner =
        new ExactMethodRunner<>(
            "A",
            new Template<String, String>("changing-pool", List.of(), List.of(s -> "roll")),
            transition,
            ExactMethodRunnerTest::classify);
    for (int cycles = 1; cycles <= 8; cycles++) {
      runner.beginCycle();
      finish(runner);
      var expected = enumerate("A", cycles, transition);
      var actual = runner.observation();
      assertThat(actual.mass()).isEqualTo(Fraction.ONE);
      assertThat(actual.exits().get(Exit.GOAL).getOrDefault("goal", Fraction.ZERO))
          .isEqualTo(expected);
    }
  }

  private static Fraction enumerate(
      String state, int remaining, Transition<String, String> transition) {
    if (state.equals("goal")) return Fraction.ONE;
    if (remaining == 0) return Fraction.ZERO;
    var sum = Fraction.ZERO;
    for (var outcome : transition.apply(state, "roll").outcomes())
      sum =
          sum.add(
              outcome
                  .probability()
                  .multiply(enumerate(outcome.state(), remaining - 1, transition)));
    return sum;
  }

  @Test
  void unknownAndUnavailableAreDisjointAndUnavailableDoesNotConsumeCurrency() {
    var runner =
        new ExactMethodRunner<>(
            "root",
            new Template<String, String>(
                "branches", List.of(), List.of(s -> "roll", Function.identity())),
            (state, action) ->
                state.equals("root")
                    ? new Kernel<>(List.of(new Outcome<>("blocked", HALF)), HALF, false)
                    : new Kernel<>(List.of(), Fraction.ZERO, true),
            ExactMethodRunnerTest::classify);
    runner.beginCycle();
    finish(runner);
    var result = runner.observation();
    assertThat(result.exits().get(Exit.UNRESOLVED)).containsEntry("root", HALF);
    assertThat(result.exits().get(Exit.DEAD)).containsEntry("blocked", HALF);
    assertThat(result.cycleUses()).containsOnlyKeys("roll");
    assertThat(result.mass()).isEqualTo(Fraction.ONE);
  }

  @Test
  void invalidKernelAndFailedEvaluationDoNotAdvanceCursor() {
    assertThatThrownBy(() -> new Kernel<>(List.of(new Outcome<>("x", HALF)), Fraction.ZERO, false))
        .isInstanceOf(IllegalArgumentException.class);
    var calls = new int[] {0};
    var runner =
        new ExactMethodRunner<>(
            "root",
            new Template<String, String>("retry", List.of(), List.of(s -> "roll")),
            (state, action) -> {
              if (calls[0]++ == 0) throw new IllegalStateException("interrupted");
              return kernel("goal");
            },
            ExactMethodRunnerTest::classify);
    runner.beginCycle();
    assertThatThrownBy(runner::step).isInstanceOf(IllegalStateException.class);
    finish(runner);
    assertThat(runner.observation().exits().get(Exit.GOAL)).containsEntry("goal", Fraction.ONE);
    assertThat(runner.observation().cycleUses()).containsEntry("roll", Fraction.ONE);
  }
}
