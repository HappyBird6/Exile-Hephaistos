package com.poe2craft.crafting.domain.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.OutcomeStrategy.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OutcomeStrategyTest {
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
}
