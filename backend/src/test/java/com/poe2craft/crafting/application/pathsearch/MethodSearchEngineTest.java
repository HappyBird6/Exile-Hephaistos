package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.DeferredItem;
import java.util.*;
import org.junit.jupiter.api.Test;

class MethodSearchEngineTest {
  @Test
  void identityViewsPreserveEveryLawStrategyBoundAndEarlierSnapshot() {
    var fixture = new MethodSearchCalculationTest();
    var calculation =
        new MethodSearchCalculation(
            fixture.root(), fixture::progress, fixture.transitions, Set.of(), 100000, true);
    var engine = new DeferredMethodSearchEngine(calculation);
    var earlier = engine.states();
    assertThat(engine.methods()).isEmpty();
    assertThat(engine.rootId()).isEqualTo(fixture.root().canonicalKey());
    for (int n = 0; n < 500000 && !engine.complete(); n++) engine.step();
    assertThat(engine.complete()).isTrue();
    assertThat(earlier).hasSize(1);
    assertThat(earlier.getFirst().expanded()).isFalse();
    assertThat(engine.states()).isSameAs(engine.states());
    assertThat(engine.methods()).isSameAs(engine.methods());
    var states = new HashMap<String, MethodSearchEngine.StateView>();
    engine.states().forEach(state -> states.put(state.id(), state));
    calculation
        .states()
        .forEach(
            state -> {
              var actual = states.get(state.id());
              assertThat(actual.representative()).isEqualTo(state.state());
              assertThat(actual.distributionCell()).contains(state.state());
              assertThat(actual.progress()).isEqualTo(state.progress());
              assertThat(actual.depth()).isEqualTo(state.depth());
              assertThat(actual.expanded()).isEqualTo(state.expanded());
              assertThat(actual.complete()).isEqualTo(state.complete());
            });
    var methods = new HashMap<String, MethodSearchEngine.MethodView>();
    engine.methods().forEach(method -> methods.put(method.id(), method));
    calculation
        .methods()
        .forEach(
            method -> {
              var actual = methods.get(method.id());
              assertThat(actual.source()).isEqualTo(method.source());
              assertThat(actual.initialActions()).isEqualTo(method.initialActions());
              assertThat(actual.actions()).isEqualTo(method.actions());
              assertThat(actual.status()).isEqualTo(method.status());
              assertThat(actual.reason()).isEqualTo(method.reason());
              method
                  .points()
                  .forEach(
                      (cycles, point) -> {
                        var converted = actual.points().get(cycles);
                        assertThat(converted.cycles()).isEqualTo(point.cycles());
                        assertThat(converted.mass()).isEqualTo(Fraction.ONE);
                        assertThat(converted.active()).isEqualTo(identities(point.active()));
                        point
                            .exits()
                            .forEach(
                                (exit, law) ->
                                    assertThat(converted.exits().get(exit))
                                        .isEqualTo(identities(law)));
                        assertThat(converted.initialUses()).isEqualTo(point.initialUses());
                        assertThat(converted.cycleUses()).isEqualTo(point.cycleUses());
                      });
            });
    for (int cycles : MethodSearchCalculation.OBSERVATIONS) {
      var original = calculation.strategy(cycles);
      var converted = engine.strategy(cycles);
      for (int step = 0; step < 5; step++) {
        original.refine();
        converted.refine();
        original
            .bounds()
            .forEach(
                (state, bounds) ->
                    assertThat(converted.bounds().get(state.canonicalKey())).isEqualTo(bounds));
      }
    }
  }

  private static Map<String, Fraction> identities(Map<DeferredItem, Fraction> law) {
    var result = new HashMap<String, Fraction>();
    law.forEach((state, mass) -> result.put(state.canonicalKey(), mass));
    return result;
  }
}
