package com.poe2craft.crafting.domain.goalfilter;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class GoalGroupSemanticsTest {
  Entry row(String id) {
    return new Entry(
        id, id, "flat", new Range(BigDecimal.TEN, BigDecimal.valueOf(20)), null, false);
  }

  GoalEvaluator.Evaluation evaluate(
      Type type, Range range, Map<String, GoalEvaluator.Observation> observations) {
    var goal =
        new GoalFilter(
            1,
            "v1",
            null,
            List.of(new Group("g", type, false, range, List.of(row("a"), row("b")))));
    return new GoalEvaluator().evaluate(goal, Status.MATCH, observations, List.of());
  }

  @Test
  void countProvesBoundsWithUnknownOrUnsupportedRowsWithoutCountingFamilies() {
    var observations =
        Map.of(
            "a",
            GoalEvaluator.Observation.present(BigDecimal.TEN),
            "b",
            GoalEvaluator.Observation.unsupported());
    assertThat(
            evaluate(Type.COUNT, new Range(BigDecimal.ONE, BigDecimal.valueOf(2)), observations)
                .status())
        .isEqualTo(Status.MATCH);
    assertThat(
            evaluate(Type.COUNT, new Range(BigDecimal.ZERO, BigDecimal.ZERO), observations)
                .status())
        .isEqualTo(Status.NO_MATCH);
    assertThat(
            evaluate(
                    Type.COUNT,
                    new Range(BigDecimal.valueOf(2), BigDecimal.valueOf(2)),
                    observations)
                .status())
        .isEqualTo(Status.UNSUPPORTED);
  }

  @Test
  void definiteFalseDominatesUncertaintyAndUnsupportedDominatesUnknown() {
    assertThat(
            evaluate(
                    Type.AND,
                    null,
                    Map.of(
                        "a",
                        GoalEvaluator.Observation.absent(),
                        "b",
                        GoalEvaluator.Observation.unsupported()))
                .status())
        .isEqualTo(Status.NO_MATCH);
    assertThat(
            evaluate(
                    Type.NOT,
                    null,
                    Map.of(
                        "a",
                        GoalEvaluator.Observation.present(BigDecimal.TEN),
                        "b",
                        GoalEvaluator.Observation.unsupported()))
                .status())
        .isEqualTo(Status.NO_MATCH);
    assertThat(
            evaluate(
                    Type.AND,
                    null,
                    Map.of(
                        "a",
                        GoalEvaluator.Observation.unknown(),
                        "b",
                        GoalEvaluator.Observation.unsupported()))
                .status())
        .isEqualTo(Status.UNSUPPORTED);
  }

  @Test
  void groupsAreRootAndAndDisabledGroupDoesNotParticipate() {
    var enabled = new Group("active", Type.AND, false, null, List.of(row("a")));
    var disabled = new Group("disabled", Type.AND, true, null, List.of(row("b")));
    var goal = new GoalFilter(1, "v1", null, List.of(enabled, disabled));
    var result =
        new GoalEvaluator()
            .evaluate(
                goal,
                Status.MATCH,
                Map.of("a", GoalEvaluator.Observation.present(BigDecimal.TEN)),
                List.of());
    assertThat(result.status()).isEqualTo(Status.MATCH);
    assertThat(result.groups()).hasSize(1);
    assertThat(new GoalEvaluator().evaluate(goal, Status.NO_MATCH, Map.of(), List.of()).status())
        .isEqualTo(Status.NO_MATCH);
  }
}
