package com.poe2craft.craftingsupport.search.application;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.search.application.MethodSearchEngine.*;
import com.poe2craft.craftingsupport.search.domain.DeferredItem;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.*;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 로컬·제작방법·추천 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code fixture}: 검증사례 ({@code MethodSearchCalculationTest}).
 *   <li>{@code item}: 아이템 ({@code DeferredItem}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code source(expanded)}: 출처 처리에서 {@code StateView.cell}, {@code
 *       MethodSearchCalculation.StateView}을(를) 사용한다. 입력: {@code expanded}(확장된). 반환: {@code
 *       StateView}.
 *   <li>{@code method(id, success, unknown, ready)}: 제작방법 처리에서 {@code Exit.values}, {@code
 *       exits.put}을(를) 사용한다. 입력: {@code id}(식별자), {@code success}(성공), {@code unknown}(미확정), {@code
 *       ready}(준비완료). 반환: {@code MethodView}.
 *   <li>{@code hundredCycleRecommendationDoesNotWaitForLaterObservationsAndKeepsTies()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code missingMethodOrCompetitiveUnresolvedMassPreventsCertification()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class LocalMethodRecommendationTest {
  final MethodSearchCalculationTest fixture = new MethodSearchCalculationTest();
  final DeferredItem item = new DeferredItem(fixture.root());

  StateView source(boolean expanded) {
    return StateView.cell(
        new MethodSearchCalculation.StateView(
            "root", item, 0, fixture.progress(item.item()), expanded, false));
  }

  MethodView method(String id, int success, int unknown, boolean ready) {
    var exits = new EnumMap<Exit, Map<String, Fraction>>(Exit.class);
    for (var kind : Exit.values()) exits.put(kind, Map.of());
    if (success > 0) exits.put(Exit.GOAL, Map.of("root", Fraction.of(success, 10)));
    if (unknown > 0) exits.put(Exit.UNRESOLVED, Map.of("root", Fraction.of(unknown, 10)));
    var point =
        new Observation<String, WorkbenchCurrency>(
            100,
            exits,
            Map.of("root", Fraction.of(10 - success - unknown, 10)),
            Map.of(),
            Map.of());
    assertThat(point.mass()).isEqualTo(Fraction.ONE);
    return new MethodView(
        id,
        "root",
        List.of(),
        List.of(WorkbenchCurrency.EXALTED),
        "PARTIAL",
        null,
        ready ? Map.of(100, point) : Map.of());
  }

  @Test
  void hundredCycleRecommendationDoesNotWaitForLaterObservationsAndKeepsTies() {
    var methods =
        List.of(
            method("best-a", 8, 0, true),
            method("best-b", 8, 0, true),
            method("other", 3, 0, true));
    assertThat(
            MethodSearchService.localRecommendations(
                List.of(source(true)), Map.of("root", methods)))
        .containsExactly(new MethodSearchProtocol.Local("root", 100, List.of("best-a", "best-b")));
    assertThat(
            MethodSearchService.localRecommendations(
                List.of(source(false)), Map.of("root", methods)))
        .isEmpty();
  }

  @Test
  void missingMethodOrCompetitiveUnresolvedMassPreventsCertification() {
    assertThat(
            MethodSearchService.localRecommendations(
                List.of(source(true)),
                Map.of(
                    "root", List.of(method("known", 8, 0, true), method("pending", 0, 0, false)))))
        .isEmpty();
    assertThat(
            MethodSearchService.localRecommendations(
                List.of(source(true)),
                Map.of(
                    "root", List.of(method("known", 8, 0, true), method("unknown", 3, 6, true)))))
        .isEmpty();
  }
}
