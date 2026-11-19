package com.poe2craft.craftingsupport.goal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 목표·그룹·Semantics 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code row(id)}: 행 처리에서 {@code BigDecimal.valueOf}을(를) 사용한다. 입력: {@code id}(식별자). 반환:
 *       {@code Entry}.
 *   <li>{@code evaluate(type, range, observations)}: 입력 조건이나 정책을 평가해 결과를 구한다. 입력: {@code type}(타입),
 *       {@code range}(범위), {@code observations}(관측값목록). 반환: {@code GoalEvaluator.Evaluation}.
 *   <li>{@code countProvesBoundsWithUnknownOrUnsupportedRowsWithoutCountingFamilies()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code definiteFalseDominatesUncertaintyAndUnsupportedDominatesUnknown()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code groupsAreRootAndAndDisabledGroupDoesNotParticipate()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 * </ul>
 */
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
            evaluate(Type.COUNT, new Range(BigDecimal.ZERO, BigDecimal.ONE.negate()), observations)
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

  /** 표시용 경계는 행 개수에서 자르지 않고, 극단적인 지수의 상대·소수 경계도 필요한 정수로만 변환한다. */
  @Test
  void resolvesCountBoundsWithoutExpandingLargePowersOfTen() {
    var tiny = new BigDecimal("1E-1000000");
    var huge = new BigDecimal("1E+1000000");
    assertThat(new Range(new BigDecimal("0.5"), BigDecimal.valueOf(5)).forCount(1))
        .isEqualTo(new Range(BigDecimal.ONE, BigDecimal.valueOf(5)));
    assertThat(new Range(tiny, huge).forCount(3)).isEqualTo(new Range(BigDecimal.ZERO, huge));
    assertThat(new Range(huge.negate(), huge.negate()).forCount(3))
        .isEqualTo(new Range(BigDecimal.ONE, BigDecimal.ZERO));
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
