package com.poe2craft.crafting.domain.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.OutcomeStrategy.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OutcomeStrategyTest {
  @Test
  void eventualTiesPreserveProperAlternativesWithoutCallingStallingAnOptimalPolicy() {
    var solver =
        new OutcomeStrategy<>(
            Map.of(
                "root",
                state(
                    choice("stall", "root"),
                    choice("fast", "goal"),
                    choice("slow", "root", "goal")),
                "goal",
                new State<String>(true, true, List.of())));
    for (int i = 0; i < 10; i++) {
      solver.refine();
      solver.refineAlmostSure();
    }
    assertThat(solver.certifiedAlternatives("root"))
        .extracting(Recommendation::methodId)
        .containsExactlyInAnyOrder("fast", "slow");
    // Finite one-method observations still distinguish these policies (1 versus 1-2^-N).
    var slowOnly =
        new OutcomeStrategy<>(
            Map.of(
                "root",
                state(choice("slow", "root", "goal")),
                "goal",
                new State<String>(true, true, List.of())));
    for (int n = 1; n <= 500; n++) {
      slowOnly.refine();
      if (Set.of(100, 300, 500).contains(n)) {
        var denominator = java.math.BigInteger.ONE.shiftLeft(n);
        assertThat(slowOnly.bounds().get("root").lower())
            .isEqualTo(new Fraction(denominator.subtract(java.math.BigInteger.ONE), denominator));
        assertThat(slowOnly.bounds().get("root").exact()).isFalse();
      }
    }
  }

  private static Choice<String> choice(String id, String... targets) {
    var mass = new LinkedHashMap<String, Fraction>();
    for (var target : targets) mass.merge(target, Fraction.of(1, targets.length), Fraction::add);
    return new Choice<>(id, mass, Fraction.ZERO);
  }

  @SafeVarargs
  private static State<String> state(Choice<String>... choices) {
    return new State<>(false, true, List.of(choices));
  }

  @Test
  void diamondChoosesByOutcomeAndPreservesParallelAlternativeMethods() {
    var graph =
        Map.of(
            "root",
            state(choice("direct", "goal", "dead"), choice("branch", "A", "B")),
            "A",
            state(choice("finish-A", "goal"), choice("other-A", "dead")),
            "B",
            state(choice("other-B", "dead"), choice("finish-B", "goal")),
            "goal",
            new State<String>(true, true, List.of()),
            "dead",
            state());
    var solver = new OutcomeStrategy<>(graph);
    assertThat(solver.recommendation("root").orElseThrow().methodId()).isEqualTo("direct");
    assertThat(solver.recommendation("root").orElseThrow().certified()).isFalse();
    solver.refine();
    solver.refine();
    var best = solver.recommendation("root").orElseThrow();
    assertThat(best.methodId()).isEqualTo("branch");
    assertThat(best.value()).isEqualTo(new Bounds(Fraction.ONE, Fraction.ONE));
    assertThat(best.certified()).isTrue();
    assertThat(solver.recommendation("A").orElseThrow().methodId()).isEqualTo("finish-A");
    assertThat(solver.recommendation("B").orElseThrow().methodId()).isEqualTo("finish-B");
  }

  @Test
  void externalCyclesProduceBoundsAndClosedNonGoalCyclesAreZero() {
    var solver =
        new OutcomeStrategy<>(
            Map.of(
                "retry",
                state(choice("recover", "retry", "goal")),
                "goal",
                new State<String>(true, true, List.of()),
                "closed",
                state(choice("loop", "closed"))));
    for (int i = 0; i < 3; i++) solver.refine();
    assertThat(solver.bounds().get("retry")).isEqualTo(new Bounds(Fraction.of(7, 8), Fraction.ONE));
    assertThat(solver.bounds().get("closed")).isEqualTo(new Bounds(Fraction.ZERO, Fraction.ZERO));
    assertThat(solver.bounds().get("retry").exact()).isFalse();
  }

  @Test
  void equalValueCyclesDoNotDisplaceTheActionThatEstablishedSuccess() {
    var solver =
        new OutcomeStrategy<>(
            Map.of(
                "A",
                    state(
                        choice("stall-A", "A"), choice("cycle-A", "B"), choice("finish-A", "goal")),
                "B",
                    state(
                        choice("stall-B", "B"), choice("cycle-B", "A"), choice("finish-B", "goal")),
                "goal", new State<String>(true, true, List.of())));
    for (int i = 0; i < 4; i++) solver.refine();
    assertThat(solver.bounds().get("A")).isEqualTo(new Bounds(Fraction.ONE, Fraction.ONE));
    assertThat(solver.recommendation("A").orElseThrow().methodId()).isEqualTo("finish-A");
    assertThat(solver.recommendation("B").orElseThrow().methodId()).isEqualTo("finish-B");
  }

  @Test
  void exactPolicyCursorSolvesRecoveryCyclesAndRetainsAProperWitness() {
    // Independent equations: A = B/2 + 1/4, B = A/2 + 1/2; hence A=2/3, B=5/6.
    // A's direct alternative is 3/5, while stalling forever has least reachability zero.
    var solver =
        new OutcomeStrategy<>(
            Map.of(
                "A",
                state(
                    choice("stall", "A"),
                    choice("direct", "goal", "goal", "goal", "dead", "dead"),
                    choice("recover", "B", "B", "goal", "dead")),
                "B",
                state(choice("return", "A", "goal")),
                "goal",
                new State<String>(true, true, List.of()),
                "dead",
                state()));
    for (int i = 0; i < 100; i++) solver.refineExactPolicy();
    assertThat(solver.bounds().get("A"))
        .isEqualTo(new Bounds(Fraction.of(2, 3), Fraction.of(2, 3)));
    assertThat(solver.bounds().get("B"))
        .isEqualTo(new Bounds(Fraction.of(5, 6), Fraction.of(5, 6)));
    assertThat(solver.recommendation("A").orElseThrow().methodId()).isEqualTo("recover");
    assertThat(solver.recommendation("A").orElseThrow().certified()).isTrue();
    solver.refine();
    assertThat(solver.recommendation("A").orElseThrow().methodId()).isEqualTo("recover");
  }

  @Test
  void exactPolicyCursorNeverCertifiesMissingActionsOrUnknownMass() {
    var solver =
        new OutcomeStrategy<>(
            Map.of(
                "open",
                new State<String>(false, false, List.of(choice("known", "goal", "dead"))),
                "unknown",
                state(
                    new Choice<>("unknown", Map.of("goal", Fraction.of(1, 2)), Fraction.of(1, 2))),
                "goal",
                new State<String>(true, true, List.of()),
                "dead",
                state()));
    for (int i = 0; i < 100; i++) {
      solver.refine();
      solver.refineExactPolicy();
    }
    assertThat(solver.bounds().get("open").exact()).isFalse();
    assertThat(solver.bounds().get("unknown").exact()).isFalse();
  }

  @Test
  void incompleteActionEnumerationPreventsFinalRecommendation() {
    var solver =
        new OutcomeStrategy<>(
            Map.of(
                "root",
                new State<>(false, false, List.of(choice("known", "goal", "dead"))),
                "goal",
                new State<String>(true, true, List.of()),
                "dead",
                state()));
    solver.refine();
    assertThat(solver.bounds().get("root")).isEqualTo(new Bounds(Fraction.of(1, 2), Fraction.ONE));
    assertThat(solver.recommendation("root").orElseThrow().certified()).isFalse();
  }

  @Test
  void largeRecoveryCycleHasExactSuccessAndAProperOutcomeResponsivePolicy() {
    var graph = new LinkedHashMap<String, State<String>>();
    graph.put("goal", new State<>(true, true, List.of()));
    for (int i = 0; i < 100; i++)
      graph.put(
          "s" + i,
          state(
              choice("stall-" + i, "s" + i),
              choice("advance-" + i, "s" + ((i + 1) % 100), i == 99 ? "goal" : "s" + i)));
    var solver = new OutcomeStrategy<>(graph);
    for (int i = 0; i < 100; i++) solver.refineAlmostSure();
    for (int i = 0; i < 100; i++) {
      assertThat(solver.bounds().get("s" + i)).isEqualTo(new Bounds(Fraction.ONE, Fraction.ONE));
      var recommended = solver.recommendation("s" + i).orElseThrow();
      assertThat(recommended.methodId()).isEqualTo("advance-" + i);
      assertThat(recommended.certified()).isTrue();
    }
    solver.refine();
    assertThat(solver.recommendation("s0").orElseThrow().methodId()).isEqualTo("advance-0");
  }

  @Test
  void qualitativeProofEliminatesRiskyPredecessorsAndDoesNotPromoteUnknownGraphs() {
    var graph = new LinkedHashMap<String, State<String>>();
    graph.put("goal", new State<>(true, true, List.of()));
    graph.put("dead", state(choice("closed", "dead")));
    for (int i = 0; i < 100; i++)
      graph.put(
          "s" + i,
          state(
              choice("stall", "s" + i), choice("risky", i == 99 ? "goal" : "s" + (i + 1), "dead")));
    var solver = new OutcomeStrategy<>(graph);
    for (int i = 0; i < 2000; i++) solver.refineAlmostSure();
    assertThat(solver.bounds().get("s0").exact()).isFalse();
    assertThat(solver.bounds().get("dead")).isEqualTo(new Bounds(Fraction.ZERO, Fraction.ZERO));
    for (boolean unknown : List.of(false, true)) {
      var incomplete =
          new OutcomeStrategy<>(
              Map.of(
                  "goal", new State<String>(true, true, List.of()),
                  "s",
                      new State<>(
                          false,
                          unknown,
                          List.of(
                              unknown
                                  ? new Choice<>(
                                      "unknown",
                                      Map.of("goal", Fraction.of(1, 2)),
                                      Fraction.of(1, 2))
                                  : choice("known", "s", "goal")))));
      for (int i = 0; i < 100; i++) incomplete.refineAlmostSure();
      assertThat(incomplete.bounds().get("s")).isEqualTo(new Bounds(Fraction.ZERO, Fraction.ONE));
    }
  }
}
