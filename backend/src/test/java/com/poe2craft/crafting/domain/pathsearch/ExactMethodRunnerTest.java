package com.poe2craft.crafting.domain.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ExactMethodRunnerTest {
  private static final Fraction HALF = Fraction.of(1, 2);

  private static Compaction<String> terminalExpansion(Map<String, Fraction> input) {
    var mass = input.values().iterator().next();
    return new Compaction<>() {
      int remaining = 2;

      public boolean complete() {
        return remaining == 0;
      }

      public void step(int maxInputs) {
        remaining = Math.max(0, remaining - maxInputs);
      }

      public Map<String, Fraction> result() {
        if (!complete()) throw new IllegalStateException("Partial terminal law");
        return Map.of(
            "goal-a", mass.multiply(Fraction.of(1, 3)), "goal-b", mass.multiply(Fraction.of(2, 3)));
      }
    };
  }

  @Test
  void terminalLawIsExpandedOnceWithoutRecurrencyCostsOrCumulativeRecompaction() {
    var normalizations = new java.util.concurrent.atomic.AtomicInteger();
    var runner =
        new ExactMethodRunner<String, String>(
            "root",
            new Template<>("first-hit", List.of(), List.of(s -> "add", s -> "remove")),
            (state, action) -> {
              assertThat(action).isEqualTo("add");
              return kernel("goal-bundle");
            },
            state -> state.startsWith("goal") ? Optional.of(Exit.GOAL) : Optional.empty(),
            values -> {
              throw new AssertionError("Absorbed results must not be recompacted");
            },
            values -> {
              normalizations.incrementAndGet();
              return terminalExpansion(values);
            });
    runner.beginCycle();
    runner.step(1);
    assertThat(runner.atBoundary()).isFalse();
    assertThatThrownBy(runner::observation).isInstanceOf(IllegalStateException.class);
    while (!runner.atBoundary()) runner.step(1);
    var point = runner.observation();
    assertThat(point.exits().get(Exit.GOAL))
        .isEqualTo(Map.of("goal-a", Fraction.of(1, 3), "goal-b", Fraction.of(2, 3)));
    assertThat(point.cycleUses()).isEqualTo(Map.of("add", Fraction.ONE));
    assertThat(point.cycles()).isEqualTo(1);
    runner.beginCycle();
    finish(runner);
    assertThat(normalizations.get()).isEqualTo(1);
    assertThat(runner.observation().exits()).isEqualTo(point.exits());
    assertThat(runner.observation().cycleUses()).isEqualTo(point.cycleUses());
  }

  @Test
  void alreadyTerminalRootNormalizationConsumesNoCycleOrCurrency() {
    var runner =
        new ExactMethodRunner<String, String>(
            "goal-bundle",
            new Template<>("terminal-root", List.of(s -> "prepare"), List.of(s -> "add")),
            (state, action) -> {
              throw new AssertionError("Already successful root must not act");
            },
            state -> Optional.of(Exit.GOAL),
            null,
            ExactMethodRunnerTest::terminalExpansion);
    finish(runner);
    var point = runner.observation();
    assertThat(point.cycles()).isZero();
    assertThat(point.mass()).isEqualTo(Fraction.ONE);
    assertThat(point.initialUses()).isEmpty();
    assertThat(point.cycleUses()).isEmpty();
  }

  @Test
  void publishedObservationRejectsMassLostByTerminalNormalization() {
    var runner =
        new ExactMethodRunner<String, String>(
            "root",
            new Template<>("terminal-guard", List.of(), List.of(s -> "draw")),
            (state, action) -> kernel("goal"),
            ExactMethodRunnerTest::classify,
            null,
            values ->
                new Compaction<>() {
                  public boolean complete() {
                    return true;
                  }

                  public void step(int maxInputs) {}

                  public Map<String, Fraction> result() {
                    return Map.of("goal", HALF);
                  }
                });
    runner.beginCycle();
    finish(runner);
    assertThat(runner.boundarySnapshot().mass()).isEqualTo(HALF);
    assertThatThrownBy(runner::observation)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Method mass not conserved");
  }

  @Test
  void publishedObservationRejectsMassLostByFaultyCompaction() {
    var runner =
        new ExactMethodRunner<String, String>(
            "root",
            new Template<>("guard", List.of(), List.of(s -> "draw")),
            (state, action) -> kernel("a", "b"),
            state -> Optional.empty(),
            values ->
                new Compaction<>() {
                  public boolean complete() {
                    return true;
                  }

                  public void step(int maxInputs) {}

                  public Map<String, Fraction> result() {
                    return Map.of("a", HALF);
                  }
                });
    runner.beginCycle();
    finish(runner);
    assertThat(runner.boundarySnapshot().mass()).isEqualTo(HALF);
    assertThatThrownBy(runner::observation)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Method mass not conserved");
  }

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
  void equivalentPhaseInputsShareWorkWithoutAveragingEntryLawsOrUnknownExits() {
    var shared =
        new Kernel<String>(
            List.of(
                new Outcome<>("goal", HALF),
                new Outcome<>("x", Fraction.of(1, 8)),
                new Outcome<>("y", Fraction.of(1, 8))),
            Fraction.of(1, 4),
            false);
    var runners = new ArrayList<ExactMethodRunner<String, String>>();
    for (boolean reverse : List.of(false, true))
      for (boolean reuse : List.of(true, false)) {
        runners.add(
            new ExactMethodRunner<>(
                "root",
                new Template<>(
                    "entry-" + reverse + reuse, List.of(s -> "prepare"), List.of(s -> "try")),
                (state, action) ->
                    action.equals("prepare")
                        ? new Kernel<>(
                            List.of(
                                new Outcome<>("x", Fraction.of(reverse ? 3 : 1, 4)),
                                new Outcome<>("y", Fraction.of(reverse ? 1 : 3, 4))),
                            Fraction.ZERO,
                            false)
                        : reuse
                            ? shared
                            : new Kernel<>(shared.outcomes(), shared.unresolved(), false),
                ExactMethodRunnerTest::classify));
      }
    runners.forEach(ExactMethodRunnerTest::finish);
    for (int cycle = 1; cycle <= 100; cycle++) {
      for (var runner : runners) {
        runner.beginCycle();
        finish(runner);
      }
      assertThat(runners.get(0).observation()).isEqualTo(runners.get(1).observation());
      assertThat(runners.get(2).observation()).isEqualTo(runners.get(3).observation());
      if (cycle == 1) {
        // Independent one-step enumeration: unknown mass is one quarter of each input.
        assertThat(runners.get(0).observation().exits().get(Exit.UNRESOLVED))
            .isEqualTo(Map.of("x", Fraction.of(1, 16), "y", Fraction.of(3, 16)));
        assertThat(runners.get(2).observation().exits().get(Exit.UNRESOLVED))
            .isEqualTo(Map.of("x", Fraction.of(3, 16), "y", Fraction.of(1, 16)));
      }
    }
    assertThat(runners.get(0).groupedInputs()).isPositive();
    assertThat(runners.get(1).groupedInputs()).isZero();
    assertThat(runners.get(0).observation()).isNotEqualTo(runners.get(2).observation());
  }

  @Test
  void equalKernelsDoNotMergeDifferentActionsOrUnavailableInputStates() {
    var shared = kernel("goal");
    var runner =
        new ExactMethodRunner<>(
            "root",
            new Template<String, String>(
                "different-actions", List.of(s -> "prepare"), List.of(s -> s)),
            (state, action) -> action.equals("prepare") ? kernel("x", "y") : shared,
            ExactMethodRunnerTest::classify);
    finish(runner);
    runner.beginCycle();
    finish(runner);
    assertThat(runner.groupedInputs()).isZero();
    assertThat(runner.observation().cycleUses()).isEqualTo(Map.of("x", HALF, "y", HALF));

    var unavailable = new Kernel<String>(List.of(), Fraction.ZERO, true);
    var blocked =
        new ExactMethodRunner<>(
            "root",
            new Template<String, String>(
                "blocked-inputs", List.of(s -> "prepare"), List.of(s -> "blocked")),
            (state, action) -> action.equals("prepare") ? kernel("x", "y") : unavailable,
            ExactMethodRunnerTest::classify);
    finish(blocked);
    blocked.beginCycle();
    finish(blocked);
    assertThat(blocked.groupedInputs()).isZero();
    assertThat(blocked.observation().exits().get(Exit.DEAD))
        .isEqualTo(Map.of("x", HALF, "y", HALF));
    assertThat(blocked.observation().cycleUses()).isEmpty();
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

  @Test
  void outcomeSlicesResumeWithoutDuplicateCostMassOrPostHitRemoval() {
    var calls = new int[] {0};
    var fail = new boolean[] {true};
    var runner =
        new ExactMethodRunner<>(
            "root",
            new Template<String, String>("sliced", List.of(), List.of(s -> "roll", s -> "remove")),
            (state, action) -> {
              calls[0]++;
              return action.equals("roll") ? kernel("goal", "miss") : kernel("root");
            },
            state -> {
              if (state.equals("miss") && fail[0]) {
                fail[0] = false;
                throw new IllegalStateException("interrupted projection");
              }
              return classify(state);
            });
    runner.beginCycle();
    runner.step(1);
    assertThat(runner.atBoundary()).isFalse();
    assertThatThrownBy(runner::observation).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> runner.step(1)).isInstanceOf(IllegalStateException.class);
    finish(runner);
    var result = runner.observation();
    assertThat(calls[0]).isEqualTo(2);
    assertThat(result.mass()).isEqualTo(Fraction.ONE);
    assertThat(result.exits().get(Exit.GOAL)).containsEntry("goal", HALF);
    assertThat(result.active()).containsEntry("root", HALF);
    assertThat(result.cycleUses())
        .containsEntry("roll", Fraction.ONE)
        .containsEntry("remove", HALF);
  }

  @Test
  void laterAbsorptionCannotRewriteAnEarlierObservationOrChargeUnusedCycles() {
    var runner =
        new ExactMethodRunner<>(
            0,
            new Template<Integer, String>("long-chain", List.of(), List.of(s -> "advance")),
            (state, action) ->
                new Kernel<>(List.of(new Outcome<>(state + 1, Fraction.ONE)), Fraction.ZERO, false),
            state -> state == 150 ? Optional.of(Exit.GOAL) : Optional.empty());
    Observation<Integer, String> hundred = null;
    for (int n = 1; n <= 150; n++) {
      runner.beginCycle();
      finish(runner);
      if (n == 100) hundred = runner.observation();
    }
    assertThat(hundred.active()).containsEntry(100, Fraction.ONE);
    var absorbed = runner.observation();
    assertThatThrownBy(() -> absorbed.atLaterAbsorbedBoundary(100))
        .isInstanceOf(IllegalArgumentException.class);
    var threeHundred = absorbed.atLaterAbsorbedBoundary(300);
    assertThat(threeHundred.cycles()).isEqualTo(300);
    assertThat(threeHundred.cycleUses()).containsEntry("advance", Fraction.of(150, 1));
    assertThat(threeHundred.exits().get(Exit.GOAL)).containsEntry(150, Fraction.ONE);
  }
}
