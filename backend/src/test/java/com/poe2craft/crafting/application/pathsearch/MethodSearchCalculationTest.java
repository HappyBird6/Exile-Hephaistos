package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class MethodSearchCalculationTest {
  final PathSearchDataTest fixture = new PathSearchDataTest();
  final ItemCatalog catalog = fixture.catalog();
  final BasicCurrencyTransitions transitions =
      new BasicCurrencyTransitions(catalog, "method-search-fixture");

  BasicCurrencyState root() {
    var item = SolarAmulet.initial(catalog);
    return new BasicCurrencyState(
        new ItemState(
            item.snapshotId(),
            item.baseItemId(),
            item.itemLevel(),
            ItemState.Rarity.RARE,
            item.implicits(),
            List.of(),
            Set.of()),
        transitions.provenance());
  }

  MethodSearchCalculation.Progress progress(ItemState item) {
    boolean a =
        item.explicits().stream().anyMatch(m -> m.modifierId().equals("amulet:prefix:adept-s"));
    boolean b =
        item.explicits().stream().anyMatch(m -> m.modifierId().equals("amulet:suffix:of-the-seal"));
    return new MethodSearchCalculation.Progress(
        a && b ? Status.MATCH : Status.NO_MATCH,
        List.of(a ? Status.MATCH : Status.NO_MATCH, b ? Status.MATCH : Status.NO_MATCH));
  }

  @Test
  void emptyRareDiscoversDiamondAndAlternativeMethodsWithoutMergingFullRollStates() {
    var search =
        new MethodSearchCalculation(root(), this::progress, transitions, Set.of(), 100000, true);
    for (int i = 0;
        i < 250000 && search.states().stream().noneMatch(s -> s.progress().goal() == Status.MATCH);
        i++) search.step();
    assertThat(search.states())
        .anyMatch(
            s ->
                s.depth() == 1
                    && s.progress().entries().equals(List.of(Status.MATCH, Status.NO_MATCH)));
    assertThat(search.states())
        .anyMatch(
            s ->
                s.depth() == 1
                    && s.progress().entries().equals(List.of(Status.NO_MATCH, Status.MATCH)));
    assertThat(search.states())
        .anyMatch(s -> s.depth() == 2 && s.progress().goal() == Status.MATCH);
    assertThat(
            search.states().stream()
                .filter(
                    s ->
                        s.depth() == 1
                            && s.progress()
                                .entries()
                                .equals(List.of(Status.MATCH, Status.NO_MATCH))))
        .hasSize(2);
    var rootMethods =
        search.methods().stream().filter(m -> m.source().equals(root().canonicalKey())).toList();
    assertThat(rootMethods.size()).isGreaterThan(6);
    assertThat(rootMethods)
        .anyMatch(
            m ->
                m.actions()
                    .equals(List.of(WorkbenchCurrency.EXALTED, WorkbenchCurrency.ANNULMENT)));
    assertThat(rootMethods).anyMatch(m -> m.actions().equals(List.of(WorkbenchCurrency.EXALTED)));
    for (var method : search.methods())
      for (var point : method.points().values()) assertThat(point.mass()).isEqualTo(Fraction.ONE);
    // The graph remains partial while other discovered methods or later layers still have work.
    assertThat(search.complete()).isFalse();
  }

  @Test
  void memoryBudgetIsNotReportedAsCompleteAndUnsupportedIsNotZeroProbability() {
    var search =
        new MethodSearchCalculation(root(), this::progress, transitions, Set.of(), 1, true);
    for (int i = 0; i < 100 && !search.memoryLimited(); i++) search.step();
    assertThat(search.memoryLimited()).isTrue();
    assertThat(search.complete()).isFalse();
    assertThat(search.methods()).anyMatch(m -> m.status().equals("PARTIAL"));
  }

  @Test
  void finiteTinySearchCompletesOnlyAfterEveryStateAndMethodAndChoosesByOutcome() {
    var search =
        new MethodSearchCalculation(root(), this::progress, transitions, Set.of(), 100000, true);
    for (int i = 0; i < 500000 && !search.complete(); i++) search.step();
    assertThat(search.complete()).isTrue();
    assertThat(search.cacheMetrics().liveRunners()).isZero();
    assertThat(search.cacheMetrics().sharedLookups()).isPositive();
    assertThat(search.cacheMetrics().distinctKernels())
        .isLessThan(search.cacheMetrics().sourceActions());
    assertThat(search.states()).allMatch(MethodSearchCalculation.StateView::complete);
    assertThat(search.methods())
        .allSatisfy(m -> assertThat(m.points()).containsOnlyKeys(100, 300, 500));
    var strategy = search.strategy(100);
    for (int i = 0; i < 5; i++) strategy.refine();
    assertThat(strategy.bounds().get(root()).lower()).isEqualTo(Fraction.ONE);
    assertThat(strategy.recommendation(root()).orElseThrow().certified()).isTrue();
  }
}
