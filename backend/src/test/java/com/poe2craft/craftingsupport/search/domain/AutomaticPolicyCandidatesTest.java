package com.poe2craft.craftingsupport.search.domain;

import static com.poe2craft.craftingsupport.search.domain.AutomaticPolicyCandidates.*;
import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 결정적 정책 후보를 제한된 수로 만들고 성공 도달 가능성으로 비교한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code p(n, d)}: p 처리에서 {@code CertifiedInterval.enclose}, {@code Fraction.of}을(를) 사용한다.
 *       입력: {@code n}(n), {@code d}(d). 반환: {@code CertifiedInterval}.
 *   <li>{@code action(id, outcomes, cost)}: 행동의 입력에 따른 분기·결과를 처리한다. 입력: {@code id}(식별자), {@code
 *       outcomes}(결과목록), {@code cost}(비용). 반환: {@code Action}.
 *   <li>{@code evaluator(states)}: 평가기 처리에서 {@code graph.put}, {@code states.forEach}을(를) 사용한다. 입력:
 *       {@code states}(상태목록). 반환: {@code AutomaticPolicyCandidates}.
 *   <li>{@code entireBranchPolicyAggregatesTerminalMassAndReachWeightedMaterialUse()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code topThreeSurvivePruningAndMatchExhaustiveOneStatePolicies()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code accumulatedSmallEndingsAreReportedForTheEntirePolicy()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 *   <li>{@code unknownAndIterationTailStayUnresolvedWithNoInventedUsageUpperBound()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code overlappingCandidatesRemainAndNamesDoNotCreateNewPolicies()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 * </ul>
 */
class AutomaticPolicyCandidatesTest {
  static CertifiedInterval p(long n, long d) {
    return CertifiedInterval.enclose(Fraction.of(n, d), 64);
  }

  static Action action(String id, Map<Integer, CertifiedInterval> outcomes, long cost) {
    return new Action(id, outcomes, Map.of("orb", p(cost, 1)), true);
  }

  static AutomaticPolicyCandidates evaluator(Map<Integer, State> states) {
    var graph = new LinkedHashMap<Integer, CertifiedOutcomeStrategy.State<Integer>>();
    for (int terminal : List.of(FAILURE, GOAL, REBASE))
      graph.put(terminal, new CertifiedOutcomeStrategy.State<>(terminal == GOAL, false, List.of()));
    states.forEach(
        (id, s) ->
            graph.put(
                id,
                new CertifiedOutcomeStrategy.State<>(
                    s.goal(),
                    s.open(),
                    s.actions().stream()
                        .map(
                            a -> {
                              var law = new LinkedHashMap<>(a.outcomes());
                              var unknown = law.remove(UNRESOLVED);
                              return new CertifiedOutcomeStrategy.Choice<>(
                                  a.id(), law, unknown == null ? p(0, 1) : unknown);
                            })
                        .toList())));
    var strategy = new CertifiedOutcomeStrategy<>(graph, 64);
    for (int i = 0; i < 16; i++) strategy.refine();
    return new AutomaticPolicyCandidates(states, strategy, 64);
  }

  @Test
  void entireBranchPolicyAggregatesTerminalMassAndReachWeightedMaterialUse() {
    var states =
        Map.of(
            0,
            new State(false, false, List.of(action("branch", Map.of(1, p(1, 2), 2, p(1, 2)), 1))),
            1,
            new State(
                false,
                false,
                List.of(
                    action(
                        "finish-a", Map.of(GOAL, p(1, 2), REBASE, p(1, 4), FAILURE, p(1, 4)), 2))),
            2,
            new State(false, false, List.of(action("finish-b", Map.of(GOAL, p(1, 1)), 4))));
    var c = evaluator(states).top(3).getFirst();
    assertThat(c.decisions())
        .containsExactly(
            new Decision(0, "branch"), new Decision(1, "finish-a"), new Decision(2, "finish-b"));
    assertThat(c.success()).isEqualTo(p(3, 4));
    assertThat(c.rebase()).isEqualTo(p(1, 8));
    assertThat(c.failure()).isEqualTo(p(1, 8));
    assertThat(c.unresolved()).isEqualTo(p(0, 1));
    assertThat(c.uses()).containsEntry("orb", p(4, 1));
    assertThat(c.usageComplete()).isTrue();
  }

  @Test
  void topThreeSurvivePruningAndMatchExhaustiveOneStatePolicies() {
    var actions = new ArrayList<Action>();
    for (int i = 1; i <= 5; i++)
      actions.add(action("a" + i, Map.of(GOAL, p(i, 8), FAILURE, p(8 - i, 8)), 10 - i));
    var candidates = evaluator(Map.of(0, new State(false, false, actions))).top(3);
    assertThat(candidates)
        .extracting(c -> c.decisions().getFirst().method())
        .containsExactly("a5", "a4", "a3");
    assertThat(candidates)
        .extracting(Candidate::success)
        .containsExactly(p(5, 8), p(4, 8), p(3, 8));
    assertThat(candidates).extracting(Candidate::id).doesNotHaveDuplicates();
  }

  @Test
  void accumulatedSmallEndingsAreReportedForTheEntirePolicy() {
    var states = new LinkedHashMap<Integer, State>();
    for (int i = 0; i < 10; i++)
      states.put(
          i,
          new State(
              false,
              false,
              List.of(
                  action(
                      "a" + i, Map.of(REBASE, p(1, 128), i == 9 ? GOAL : i + 1, p(127, 128)), 1))));
    var c = evaluator(states).top(3).getFirst();
    var success = Fraction.ONE;
    for (int i = 0; i < 10; i++) success = success.multiply(Fraction.of(127, 128));
    assertThat(c.success().contains(success)).isTrue();
    assertThat(c.rebase().lowerUnits()).isGreaterThan(p(7, 100).upperUnits());
  }

  @Test
  void unknownAndIterationTailStayUnresolvedWithNoInventedUsageUpperBound() {
    var unknown =
        evaluator(
                Map.of(
                    0,
                    new State(
                        false, false, List.of(action("unknown", Map.of(UNRESOLVED, p(1, 1)), 1)))))
            .top(3)
            .getFirst();
    assertThat(unknown.rebase()).isEqualTo(p(0, 1));
    assertThat(unknown.unresolved()).isEqualTo(p(1, 1));
    assertThat(unknown.usageComplete()).isFalse();
    var loop =
        evaluator(
                Map.of(0, new State(false, false, List.of(action("loop", Map.of(0, p(1, 1)), 1)))))
            .top(3)
            .getFirst();
    assertThat(loop.failure())
        .isEqualTo(new CertifiedInterval(64, p(0, 1).lowerUnits(), p(1, 1).upperUnits()));
    assertThat(loop.unresolved()).isEqualTo(p(1, 1));
    assertThat(loop.usageComplete()).isFalse();
  }

  @Test
  void overlappingCandidatesRemainAndNamesDoNotCreateNewPolicies() {
    var a =
        new Action("a", Map.of(UNRESOLVED, p(1, 1)), Map.of("orb", p(1, 1)), false, "same-action");
    var alias = new Action("renamed-a", a.outcomes(), a.uses(), false, "same-action");
    var b = new Action("b", a.outcomes(), Map.of("other", p(1, 1)), false, "other-action");
    var result = evaluator(Map.of(0, new State(false, false, List.of(a, alias, b)))).top(3);
    assertThat(result).hasSize(2).allMatch(c -> c.rankStatus().equals("OVERLAPPING"));
  }
}
