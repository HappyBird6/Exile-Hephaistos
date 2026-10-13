package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.application.pathsearch.MethodSearchEngine.*;
import com.poe2craft.crafting.domain.WorkbenchCurrency;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.DeferredItem;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.util.*;
import org.junit.jupiter.api.Test;

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
