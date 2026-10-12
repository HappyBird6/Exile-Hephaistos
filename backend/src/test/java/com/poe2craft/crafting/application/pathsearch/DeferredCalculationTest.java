package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import com.poe2craft.item.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class DeferredCalculationTest {
  @Test
  void completedProductionSearchMatchesConcreteOracleForEveryMethodAndObservation()
      throws Exception {
    var fixture = new MethodSearchCalculationTest();
    var index =
        new GoalCatalogIndex(
            List.of(fixture.catalog), fixture.fixture.definitions(), "method-search-fixture");
    var known =
        index.knownStats(
            new Context(fixture.root().item().snapshotId(), fixture.catalog.base().id(), 82));
    var bId =
        GoalCatalogIndex.id(
            "explicit",
            fixture.catalog.modifiers().get("amulet:suffix:of-the-seal").stats().getFirst().id());
    var goal =
        new GoalFilter(
            1,
            index.version(),
            new General(
                fixture.catalog.base().id(), new Range(null, null), List.of(ItemState.Rarity.RARE)),
            List.of(
                new Group(
                    "goal",
                    Type.AND,
                    false,
                    null,
                    List.of(
                        new Entry(
                            "a",
                            GoalCatalogIndex.id("explicit", fixture.fixture.stat),
                            "flat",
                            new Range(BigDecimal.ONE, null),
                            null,
                            false),
                        new Entry(
                            "b",
                            bId,
                            known.get(bId).unit(),
                            new Range(BigDecimal.valueOf(6), null),
                            null,
                            false)))));
    var partition = RollPartition.compile(fixture.catalog, known, goal, fixture.root().item());
    var full =
        new MethodSearchCalculation(
            fixture.root(), fixture::progress, fixture.transitions, Set.of(), 100000, true);
    var deferred =
        new MethodSearchCalculation(
            fixture.root(),
            fixture::progress,
            fixture.transitions,
            Set.of(),
            100000,
            true,
            512,
            partition);
    for (int i = 0; i < 500000 && (!full.complete() || !deferred.complete()); i++) {
      full.step();
      deferred.step();
    }
    assertThat(full.complete()).isTrue();
    assertThat(deferred.complete()).isTrue();
    assertThat(deferred.states()).hasSizeLessThan(full.states().size());
    var sources = new HashMap<String, DeferredItem>();
    deferred.states().forEach(s -> sources.put(s.id(), s.state()));
    for (var method : deferred.methods()) {
      var source = sources.get(method.source());
      var weight = new Fraction(BigInteger.ONE, source.cardinality());
      var concreteMethods = new ArrayList<MethodSearchCalculation.MethodView>();
      for (var i = BigInteger.ZERO;
          i.compareTo(source.cardinality()) < 0;
          i = i.add(BigInteger.ONE)) {
        var id = source.concrete(i).canonicalKey();
        concreteMethods.add(
            full.methods().stream()
                .filter(
                    m ->
                        m.source().equals(id)
                            && m.initialActions().equals(method.initialActions())
                            && m.actions().equals(method.actions()))
                .findFirst()
                .orElseThrow());
      }
      for (int cycles : MethodSearchCalculation.OBSERVATIONS) {
        var observed = method.points().get(cycles);
        for (var exit : Exit.values()) {
          var expected = new HashMap<BasicCurrencyState, Fraction>();
          concreteMethods.forEach(
              m ->
                  restore(m.points().get(cycles).exits().get(exit))
                      .forEach(
                          (state, mass) ->
                              expected.merge(state, mass.multiply(weight), Fraction::add)));
          assertThat(restore(observed.exits().get(exit))).isEqualTo(expected);
        }
        var expectedActive = new HashMap<BasicCurrencyState, Fraction>();
        var initial = new HashMap<WorkbenchCurrency, Fraction>();
        var currency = new HashMap<WorkbenchCurrency, Fraction>();
        for (var concrete : concreteMethods) {
          var point = concrete.points().get(cycles);
          restore(point.active())
              .forEach(
                  (state, mass) ->
                      expectedActive.merge(state, mass.multiply(weight), Fraction::add));
          point
              .initialUses()
              .forEach(
                  (action, mass) -> initial.merge(action, mass.multiply(weight), Fraction::add));
          point
              .cycleUses()
              .forEach(
                  (action, mass) -> currency.merge(action, mass.multiply(weight), Fraction::add));
        }
        assertThat(restore(observed.active())).isEqualTo(expectedActive);
        assertThat(observed.initialUses()).isEqualTo(initial);
        assertThat(observed.cycleUses()).isEqualTo(currency);
      }
    }
    for (int cycles : MethodSearchCalculation.OBSERVATIONS) {
      var a = full.strategy(cycles);
      var b = deferred.strategy(cycles);
      for (int i = 0; i < 10; i++) {
        a.refine();
        b.refine();
      }
      for (var source : sources.values())
        for (var i = BigInteger.ZERO;
            i.compareTo(source.cardinality()) < 0;
            i = i.add(BigInteger.ONE))
          assertThat(a.bounds().get(new DeferredItem(source.concrete(i))))
              .isEqualTo(b.bounds().get(source));
    }
  }

  private static Map<BasicCurrencyState, Fraction> restore(Map<DeferredItem, Fraction> law) {
    var result = new HashMap<BasicCurrencyState, Fraction>();
    law.forEach(
        (cell, mass) -> {
          var probability =
              new Fraction(mass.numerator(), mass.denominator().multiply(cell.cardinality()));
          for (var i = BigInteger.ZERO;
              i.compareTo(cell.cardinality()) < 0;
              i = i.add(BigInteger.ONE)) result.merge(cell.concrete(i), probability, Fraction::add);
        });
    return result;
  }
}
