package com.poe2craft.crafting.domain.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.CertifiedOutcomeStrategy.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CertifiedOutcomeStrategyTest {
  private static CertifiedInterval p(long n, long d) {
    return CertifiedInterval.enclose(Fraction.of(n, d), 64);
  }

  private static Choice<String> choice(String id, String... targets) {
    var law = new LinkedHashMap<String, CertifiedInterval>();
    for (var target : targets)
      law.merge(target, p(1, targets.length), (a, b) -> a.add(b).probability());
    return new Choice<>(id, law, p(0, 1));
  }

  @SafeVarargs
  private static State<String> state(Choice<String>... choices) {
    return new State<>(false, false, List.of(choices));
  }

  private static State<String> goal() {
    return new State<>(true, false, List.of());
  }

  @Test
  void diamondSwitchesActionsByOutcomeAndReversesLocalPreference() {
    var graph =
        Map.of(
            "root",
            state(choice("direct", "goal", "dead"), choice("branch", "A", "B")),
            "A",
            state(choice("finish-A", "goal"), choice("wrong-A", "dead")),
            "B",
            state(choice("wrong-B", "dead"), choice("finish-B", "goal")),
            "goal",
            goal(),
            "dead",
            state());
    var local =
        CertifiedRecommendations.local(
            List.of(
                new CertifiedRecommendations.Choice("direct", p(1, 2), p(0, 1)),
                new CertifiedRecommendations.Choice("branch", p(0, 1), p(0, 1))),
            true);
    assertThat(local.methodIds()).containsExactly("direct");
    var solver = new CertifiedOutcomeStrategy<>(graph, 64);
    assertThat(solver.result("root").status()).isEqualTo("PROVEN_ONE");
    assertThat(solver.result("root").methodIds()).containsExactly("branch");
    assertThat(solver.result("A").methodIds()).containsExactly("finish-A");
    assertThat(solver.result("B").methodIds()).containsExactly("finish-B");
  }

  @Test
  void recoveryCycleEnclosesIndependentExactEquationsWithoutAddingRecoveryToDirectSuccess() {
    // A = B/2 + 1/4; B = A/2 + 1/2 => A=2/3, B=5/6.
    var solver =
        new CertifiedOutcomeStrategy<>(
            Map.of(
                "A",
                state(
                    choice("direct", "goal", "goal", "goal", "dead", "dead"),
                    choice("recover", "B", "B", "goal", "dead")),
                "B",
                state(choice("return", "A", "goal")),
                "goal",
                goal(),
                "dead",
                state()),
            64);
    for (int i = 0; i < 150; i++) {
      solver.refine();
      assertThat(solver.result("A").value().contains(Fraction.of(2, 3))).isTrue();
      assertThat(solver.result("B").value().contains(Fraction.of(5, 6))).isTrue();
    }
    assertThat(solver.result("A").methodIds()).containsExactly("recover");
    assertThat(solver.result("A").status()).isEqualTo("ACTION_CERTIFIED");
    assertThat(solver.result("A").value().confirmedPercent(2)).contains("66.67");
    assertThat(solver.result("A").fixedPointPending()).isTrue();
  }

  @Test
  void supportProofAvoidsStallingAndUnknownOrMissingWorkIsSeparate() {
    var solver =
        new CertifiedOutcomeStrategy<>(
            Map.of(
                "root",
                state(choice("stall", "root"), choice("retry", "root", "goal")),
                "closed",
                state(choice("loop", "closed")),
                "goal",
                goal(),
                "open",
                new State<String>(false, true, List.of()),
                "unknown",
                state(new Choice<>("unknown", Map.of(), p(1, 1)))),
            64);
    assertThat(solver.result("root").methodIds()).containsExactly("retry");
    assertThat(solver.result("closed").status()).isEqualTo("PROVEN_ZERO");
    assertThat(solver.result("open").reachableOpen()).isTrue();
    assertThat(solver.result("open").transitionArithmetic()).isFalse();
    assertThat(solver.result("unknown").reachableUnresolved()).isTrue();
    assertThat(solver.result("unknown").value().contains(Fraction.ZERO)).isTrue();
    assertThat(solver.result("unknown").value().contains(Fraction.ONE)).isTrue();
    assertThat(solver.result("unknown").status()).isEqualTo("PENDING");
    assertThat(solver.result("unknown").methodIds()).isEmpty();
    var stalled =
        new CertifiedOutcomeStrategy<>(
            Map.of(
                "A",
                state(choice("stall", "A"), choice("risk", "goal", "dead")),
                "goal",
                goal(),
                "dead",
                state()),
            64);
    for (int n = 0; n < 100; n++) stalled.refine();
    assertThat(stalled.result("A").value().contains(Fraction.of(1, 2))).isTrue();
    assertThat(stalled.result("A").status()).isEqualTo("ACTION_CERTIFIED");
    assertThat(stalled.result("A").methodIds()).containsExactly("risk");
    assertThat(stalled.result("A").value()).isEqualTo(p(1, 2));
    assertThat(stalled.result("A").fixedPointPending()).isFalse();
    assertThat(stalled.result("A").transitionArithmetic()).isFalse();
  }

  @Test
  void communicatingRegionUsesConditionalExitAndAProperNavigationPolicy() {
    var solver =
        new CertifiedOutcomeStrategy<>(
            Map.of(
                "A",
                state(
                    choice("stall-A", "A"),
                    choice("navigate", "A", "B"),
                    choice("bad", "goal", "dead")),
                "B",
                state(choice("back", "A"), choice("exit", "A", "goal", "goal", "goal", "dead")),
                "goal",
                goal(),
                "dead",
                state()),
            64);
    for (int i = 0; i < 100; i++) solver.refine();
    assertThat(solver.result("A").value().contains(Fraction.of(3, 4))).isTrue();
    assertThat(solver.result("A").value().confirmedPercent(2)).contains("75.00");
    assertThat(solver.result("A").methodIds()).containsExactly("navigate");
    assertThat(solver.result("B").methodIds()).containsExactly("exit");
  }

  @Test
  void unknownExitAndUnprovenPositiveNavigationCannotProduceFalseCertification() {
    var uncertain =
        new CertifiedInterval(
            64, java.math.BigInteger.ZERO, java.math.BigInteger.ONE.shiftLeft(64));
    var solver =
        new CertifiedOutcomeStrategy<>(
            Map.of(
                "A",
                state(
                    choice("stall", "A"),
                    choice("risk", "goal", "dead"),
                    new Choice<>(
                        "uncertain-move", Map.of("A", uncertain, "B", uncertain), p(0, 1))),
                "B",
                state(choice("back", "A"), choice("win", "goal")),
                "goal",
                goal(),
                "dead",
                state()),
            64);
    for (int i = 0; i < 100; i++) solver.refine();
    assertThat(solver.result("A").value().contains(Fraction.of(1, 2))).isTrue();
    assertThat(solver.result("A").value().contains(Fraction.ONE)).isTrue();
    assertThat(solver.result("A").status()).isEqualTo("PENDING");
    assertThat(solver.result("A").methodIds()).isEmpty();
    var unknown =
        new CertifiedOutcomeStrategy<>(
            Map.of(
                "A",
                state(
                    choice("stall", "A"),
                    choice("risk", "goal", "dead"),
                    new Choice<>("unknown", Map.of(), p(1, 1))),
                "goal",
                goal(),
                "dead",
                state()),
            64);
    for (int i = 0; i < 100; i++) unknown.refine();
    assertThat(unknown.result("A").value().contains(Fraction.of(1, 2))).isTrue();
    assertThat(unknown.result("A").value().contains(Fraction.ONE)).isTrue();
    assertThat(unknown.result("A").status()).isEqualTo("PENDING");
    assertThat(unknown.result("A").reachableUnresolved()).isTrue();
  }

  @Test
  void communicatingCycleIsNotLimitedToTheSmallExactPolicyOracle() {
    var graph = new LinkedHashMap<String, State<String>>();
    for (int i = 0; i < 96; i++) graph.put("s" + i, state(choice("next", "s" + ((i + 1) % 96))));
    graph.put("s0", state(choice("next", "s1"), choice("exit", "goal", "dead")));
    graph.put("goal", goal());
    graph.put("dead", state());
    var solver = new CertifiedOutcomeStrategy<>(graph, 64);
    solver.refine();
    for (int i = 0; i < 96; i++) {
      assertThat(solver.result("s" + i).value()).isEqualTo(p(1, 2));
      assertThat(solver.result("s" + i).methodIds()).containsExactly(i == 0 ? "exit" : "next");
      assertThat(solver.result("s" + i).fixedPointPending()).isFalse();
    }
  }

  @Test
  void cyclicBoundsAndSelectedPoliciesAgreeWithIndependentRationalPolicyEvaluation() {
    var random = new Random(914071L);
    for (int example = 0; example < 80; example++) {
      var exact = new LinkedHashMap<Integer, OutcomeStrategy.State<Integer>>();
      var certified = new LinkedHashMap<Integer, State<Integer>>();
      for (int source = 0; source < 6; source++) {
        var choices = new ArrayList<OutcomeStrategy.Choice<Integer>>();
        choices.add(
            new OutcomeStrategy.Choice<>("stall", Map.of(source, Fraction.ONE), Fraction.ZERO));
        if (random.nextBoolean())
          choices.add(
              new OutcomeStrategy.Choice<>(
                  "move", Map.of(random.nextInt(6), Fraction.ONE), Fraction.ZERO));
        for (int action = 0; action < 2; action++) {
          var law = new LinkedHashMap<Integer, Fraction>();
          int outcomeCount = 3 + random.nextInt(4);
          for (int outcome = 0; outcome < outcomeCount; outcome++)
            law.merge(random.nextInt(8), Fraction.of(1, outcomeCount), Fraction::add);
          choices.add(new OutcomeStrategy.Choice<>("try-" + action, law, Fraction.ZERO));
        }
        exact.put(source, new OutcomeStrategy.State<>(false, true, choices));
        certified.put(
            source,
            new State<>(
                false,
                false,
                choices.stream()
                    .map(
                        c -> {
                          var law = new LinkedHashMap<Integer, CertifiedInterval>();
                          c.outcomes()
                              .forEach(
                                  (target, probability) ->
                                      law.put(target, CertifiedInterval.enclose(probability, 64)));
                          return new Choice<>(c.methodId(), law, p(0, 1));
                        })
                    .toList()));
      }
      exact.put(6, new OutcomeStrategy.State<>(true, true, List.of()));
      exact.put(7, new OutcomeStrategy.State<>(false, true, List.of()));
      certified.put(6, new State<>(true, false, List.of()));
      certified.put(7, new State<>(false, false, List.of()));
      var oracle = new FinitePolicySolver<>(exact);
      Optional<FinitePolicySolver.Solution<Integer>> solution = Optional.empty();
      for (int i = 0; i < 2000 && solution.isEmpty(); i++) solution = oracle.step();
      assertThat(solution).isPresent();
      var solver = new CertifiedOutcomeStrategy<>(certified, 64);
      for (int iteration = 0; iteration < 160; iteration++) {
        solver.refine();
        for (int source = 0; source < 8; source++)
          assertThat(
                  solver
                      .result(source)
                      .value()
                      .contains(solution.orElseThrow().values().get(source)))
              .as("example %s, iteration %s, source %s", example, iteration, source)
              .isTrue();
      }
      // Restrict only certified choices; independently evaluate the resulting policy.
      // This detects Bellman ties that would otherwise recommend perpetual stalling.
      var restricted = new LinkedHashMap<Integer, OutcomeStrategy.State<Integer>>();
      exact.forEach(
          (id, state) -> {
            var ids = solver.result(id).methodIds();
            restricted.put(
                id,
                new OutcomeStrategy.State<>(
                    state.goal(),
                    true,
                    ids.isEmpty()
                        ? state.choices()
                        : state.choices().stream()
                            .filter(c -> ids.contains(c.methodId()))
                            .toList()));
          });
      var witness = new FinitePolicySolver<>(restricted);
      Optional<FinitePolicySolver.Solution<Integer>> evaluated = Optional.empty();
      for (int i = 0; i < 2000 && evaluated.isEmpty(); i++) evaluated = witness.step();
      assertThat(evaluated).isPresent();
      assertThat(evaluated.orElseThrow().values()).isEqualTo(solution.orElseThrow().values());
    }
  }
}
