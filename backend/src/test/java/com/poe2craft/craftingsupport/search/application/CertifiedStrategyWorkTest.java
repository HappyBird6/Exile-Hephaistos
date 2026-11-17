package com.poe2craft.craftingsupport.search.application;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.CertifiedOutcomeStrategy;
import com.poe2craft.craftingsupport.search.domain.CertifiedRecommendations;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 유한 전략의 계산 구간을 나눠 이어서 실행한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code p(n, d)}: p 처리에서 {@code CertifiedInterval.enclose}, {@code Fraction.of}을(를) 사용한다.
 *       입력: {@code n}(n), {@code d}(d). 반환: {@code CertifiedInterval}.
 *   <li>{@code choice(id, outcomes)}: 선택의 입력에 따른 분기·결과를 처리한다. 입력: {@code id}(식별자), {@code
 *       outcomes}(결과목록). 반환: {@code CertifiedOutcomeStrategy.Choice<String>}.
 *   <li>{@code finalAdaptiveChoicesDifferFromTheImmediateChoiceAtAllHorizons()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code unknownExitPausesWithItsEnclosureInsteadOfBecomingFailure()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code cyclicNonDyadicTruthStaysInsideEveryPublishedInterval()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 * </ul>
 */
class CertifiedStrategyWorkTest {
  private static CertifiedInterval p(long n, long d) {
    return CertifiedInterval.enclose(Fraction.of(n, d), 64);
  }

  private static CertifiedOutcomeStrategy.Choice<String> choice(
      String id, Map<String, CertifiedInterval> outcomes) {
    return new CertifiedOutcomeStrategy.Choice<>(id, outcomes, p(0, 1));
  }

  @Test
  void finalAdaptiveChoicesDifferFromTheImmediateChoiceAtAllHorizons() {
    var graph = new LinkedHashMap<String, CertifiedOutcomeStrategy.State<String>>();
    graph.put(
        "root",
        new CertifiedOutcomeStrategy.State<>(
            false,
            false,
            List.of(
                choice("direct", Map.of("goal", p(1, 2), "dead", p(1, 2))),
                choice("branch", Map.of("A", p(1, 2), "B", p(1, 2))))));
    graph.put(
        "A",
        new CertifiedOutcomeStrategy.State<>(
            false,
            false,
            List.of(
                choice("finish-A", Map.of("goal", p(1, 1))),
                choice("fail-A", Map.of("dead", p(1, 1))))));
    graph.put(
        "B",
        new CertifiedOutcomeStrategy.State<>(
            false,
            false,
            List.of(
                choice("fail-B", Map.of("dead", p(1, 1))),
                choice("finish-B", Map.of("goal", p(1, 1))))));
    graph.put("goal", new CertifiedOutcomeStrategy.State<>(true, false, List.of()));
    graph.put("dead", new CertifiedOutcomeStrategy.State<>(false, false, List.of()));
    var local =
        CertifiedRecommendations.local(
            List.of(
                new CertifiedRecommendations.Choice("direct", p(1, 2), p(0, 1)),
                new CertifiedRecommendations.Choice("branch", p(0, 1), p(0, 1))),
            true);
    assertThat(local.methodIds()).containsExactly("direct");
    var work =
        new CertifiedStrategyWork(
            List.copyOf(graph.keySet()), 2, cycles -> new CertifiedOutcomeStrategy<>(graph, 64));
    for (int i = 0; i < 20 && !work.complete(); i++) work.step();
    assertThat(work.complete()).isTrue();
    assertThat(work.rows()).hasSize(15);
    for (var row : work.rows()) {
      var truth = row.stateId().equals("dead") ? Fraction.ZERO : Fraction.ONE;
      assertThat(row.result().value().contains(truth)).isTrue();
      if (row.stateId().equals("root"))
        assertThat(row.result().methodIds()).containsExactly("branch");
      if (row.stateId().equals("A"))
        assertThat(row.result().methodIds()).containsExactly("finish-A");
      if (row.stateId().equals("B"))
        assertThat(row.result().methodIds()).containsExactly("finish-B");
    }
  }

  @Test
  void unknownExitPausesWithItsEnclosureInsteadOfBecomingFailure() {
    var graph =
        Map.of(
            "root",
            new CertifiedOutcomeStrategy.State<String>(
                false,
                false,
                List.of(
                    new CertifiedOutcomeStrategy.Choice<String>("unknown", Map.of(), p(1, 1)))));
    var work =
        new CertifiedStrategyWork(
            List.of("root"), 2, cycles -> new CertifiedOutcomeStrategy<>(graph, 64));
    for (int i = 0; i < 5; i++) work.step();
    assertThat(work.complete()).isFalse();
    assertThat(work.pauseReason()).isEqualTo("STRATEGY_UNRESOLVED");
    assertThat(work.rows().getFirst().result().value().lower()).isEqualTo(Fraction.ZERO);
    assertThat(work.rows().getFirst().result().value().upper()).isEqualTo(Fraction.ONE);
    assertThat(work.rows().getFirst().result().methodIds()).isEmpty();
  }

  @Test
  void cyclicNonDyadicTruthStaysInsideEveryPublishedInterval() {
    var graph =
        Map.of(
            "root",
                new CertifiedOutcomeStrategy.State<String>(
                    false,
                    false,
                    List.of(
                        choice(
                            "retry", Map.of("root", p(1, 2), "goal", p(1, 6), "dead", p(1, 3))))),
            "goal", new CertifiedOutcomeStrategy.State<String>(true, false, List.of()),
            "dead", new CertifiedOutcomeStrategy.State<String>(false, false, List.of()));
    var work =
        new CertifiedStrategyWork(
            List.copyOf(graph.keySet()), 2, cycles -> new CertifiedOutcomeStrategy<>(graph, 64));
    for (int i = 0; i < 300 && !work.complete(); i++) {
      work.step();
      work.rows().stream()
          .filter(r -> r.stateId().equals("root"))
          .forEach(r -> assertThat(r.result().value().contains(Fraction.of(1, 3))).isTrue());
    }
    assertThat(work.complete()).isTrue();
    assertThat(work.rows())
        .filteredOn(r -> r.stateId().equals("root"))
        .hasSize(3)
        .allMatch(r -> r.result().value().confirmedPercent(2).orElseThrow().equals("33.33"));
  }
}
