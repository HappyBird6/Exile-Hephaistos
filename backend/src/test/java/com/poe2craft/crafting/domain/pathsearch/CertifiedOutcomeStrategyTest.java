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
    assertThat(stalled.result("A").status()).isEqualTo("PENDING");
    assertThat(stalled.result("A").fixedPointPending()).isTrue();
    assertThat(stalled.result("A").transitionArithmetic()).isFalse();
  }
}
