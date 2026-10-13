package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.item.*;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;

class DeferredMixturesTest {
  @Test
  void slicedMethodCompactionKeepsFirstHitAndActualCurrencyUse() {
    var d = spirit();
    var first = item(List.of(value(d, 47)));
    var band =
        new DeferredItem(
            first.representative(),
            Map.of(d.id(), new DeferredItem.Band(d.stats().getFirst().id(), 47, 48)));
    var input = Map.of(first, Fraction.of(1, 3), band, Fraction.of(2, 3));
    var cursor = mixtures.start(input);
    assertThatThrownBy(cursor::result).isInstanceOf(IllegalStateException.class);
    cursor.step(1);
    assertThat(cursor.complete()).isFalse();
    var runner =
        new ExactMethodRunner<DeferredItem, WorkbenchCurrency>(
            new DeferredItem(root),
            new ExactMethodRunner.Template<>(
                "first-hit-mixture",
                List.of(),
                List.of(s -> WorkbenchCurrency.EXALTED, s -> WorkbenchCurrency.ANNULMENT)),
            (state, action) -> {
              assertThat(action).isEqualTo(WorkbenchCurrency.EXALTED);
              return new ExactMethodRunner.Kernel<>(
                  input.entrySet().stream()
                      .map(e -> new ExactMethodRunner.Outcome<>(e.getKey(), e.getValue()))
                      .toList(),
                  Fraction.ZERO,
                  false);
            },
            state ->
                state.item().explicits().isEmpty()
                    ? Optional.empty()
                    : Optional.of(ExactMethodRunner.Exit.GOAL),
            mixtures::start);
    runner.beginCycle();
    for (int n = 0; n < 100 && !runner.atBoundary(); n++) runner.step(1);
    assertThat(runner.atBoundary()).isTrue();
    var result = runner.observation();
    assertThat(result.cycles()).isEqualTo(1);
    assertThat(result.mass()).isEqualTo(Fraction.ONE);
    assertThat(result.cycleUses())
        .containsExactlyEntriesOf(Map.of(WorkbenchCurrency.EXALTED, Fraction.ONE));
    assertThat(result.exits().get(ExactMethodRunner.Exit.GOAL)).hasSize(1);
    assertThat(restore(result.exits().get(ExactMethodRunner.Exit.GOAL))).isEqualTo(restore(input));
  }

  final DeferredProductionTest fixture = new DeferredProductionTest();
  final BasicCurrencyState root = fixture.root();
  final RollPartition partition =
      fixture.fixture.goals.rollPartition(root.item(), fixture.goal(47));
  final DeferredMixtures mixtures =
      new DeferredMixtures(
          new BasicTransitionClasses(
              fixture.fixture.catalog,
              fixture.primitive,
              partition,
              fixture.fixture.goals.compileEvaluation(root.item(), fixture.goal(47))));

  DeferredItem item(List<ModifierInstance> explicits) {
    var i = root.item();
    return new DeferredItem(
        new BasicCurrencyState(
            new ItemState(
                i.snapshotId(),
                i.baseItemId(),
                i.itemLevel(),
                i.rarity(),
                i.implicits(),
                explicits,
                i.conditions()),
            root.provenance()));
  }

  ModifierDefinition spirit() {
    return fixture.fixture.catalog.modifiers().values().stream()
        .filter(
            d ->
                d.stats().size() == 1
                    && d.stats().getFirst().id().equals("base_spirit_from_equipment")
                    && d.stats().getFirst().min() <= 47
                    && d.stats().getFirst().max() >= 48)
        .findFirst()
        .orElseThrow();
  }

  ModifierInstance value(ModifierDefinition d, long v) {
    return new ModifierInstance(d.id(), Map.of(d.stats().getFirst().id(), v));
  }

  Map<BasicCurrencyState, Fraction> restore(Map<DeferredItem, Fraction> distribution) {
    var result = new HashMap<BasicCurrencyState, Fraction>();
    distribution.forEach(
        (state, mass) -> {
          for (var n = BigInteger.ZERO;
              n.compareTo(state.cardinality()) < 0;
              n = n.add(BigInteger.ONE))
            result.merge(
                state.concrete(n), mass.multiply(state.concreteProbability(n)), Fraction::add);
        });
    return result;
  }

  @Test
  void weightedOverlappingIntervalsRestoreExactLawAndAllFollowingActions() {
    var d = spirit();
    var first = item(List.of(value(d, 47)));
    var band =
        new DeferredItem(
            first.representative(),
            Map.of(d.id(), new DeferredItem.Band(d.stats().getFirst().id(), 47, 48)));
    var input = Map.of(first, Fraction.of(1, 3), band, Fraction.of(2, 3));
    var mixed = mixtures.compact(input);
    assertThat(mixed).hasSize(1);
    assertThat(mixtures.expand(mixed)).isEqualTo(input);
    var nested =
        mixtures.compact(
            Map.of(mixed.keySet().iterator().next(), Fraction.of(1, 2), band, Fraction.of(1, 2)));
    assertThat(mixtures.expand(nested))
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(first, Fraction.of(1, 6), band, Fraction.of(5, 6)));
    assertThat(restore(mixed)).isEqualTo(restore(input));
    assertThat(restore(mixed).values())
        .containsExactlyInAnyOrder(Fraction.of(2, 3), Fraction.of(1, 3));
    var transitions = new DeferredTransitions(fixture.primitive, partition);
    for (var action : BasicCurrencyTransitions.supportedActions()) {
      var expected = new HashMap<BasicCurrencyState, Fraction>();
      for (var entry : restore(input).entrySet()) {
        var raw = fixture.primitive.primitiveCursor(entry.getKey(), action, Set.of());
        if (raw.blocked()) continue;
        while (!raw.complete()) {
          var outcome = raw.next();
          expected.merge(
              outcome.state(), entry.getValue().multiply(outcome.probability()), Fraction::add);
        }
      }
      var actual = new HashMap<DeferredItem, Fraction>();
      for (var entry : mixed.entrySet()) {
        var cursor = transitions.primitiveCursor(entry.getKey(), action, Set.of());
        if (cursor.blocked()) continue;
        while (!cursor.complete()) {
          var outcome = cursor.next();
          actual.merge(
              outcome.state(), entry.getValue().multiply(outcome.probability()), Fraction::add);
        }
      }
      assertThat(restore(actual)).as("%s", action).isEqualTo(expected);
    }
  }

  @Test
  void goalThresholdsAndCorrelatedOtherModifiersRemainSeparate() {
    var d = spirit();
    var low =
        fixture.fixture.catalog.modifiers().values().stream()
            .filter(
                v ->
                    v.stats().size() == 1
                        && v.stats().getFirst().id().equals(d.stats().getFirst().id())
                        && v.stats().getFirst().min() < 47)
            .findFirst()
            .orElseThrow();
    var thresholds =
        Map.of(
            item(List.of(value(low, low.stats().getFirst().min()))),
            Fraction.of(1, 2),
            item(List.of(value(d, 47))),
            Fraction.of(1, 2));
    assertThat(mixtures.compact(thresholds)).hasSize(2);
    var projectile =
        fixture.fixture.catalog.modifiers().values().stream()
            .filter(
                v ->
                    v.stats().size() == 1
                        && v.stats().getFirst().id().equals("projectile_skill_gem_level_+"))
            .findFirst()
            .orElseThrow();
    var projectileValue = value(projectile, projectile.stats().getFirst().min());
    var correlated =
        Map.of(
            item(List.of(value(d, 47), projectileValue)),
            Fraction.of(1, 2),
            item(List.of(value(d, 48))),
            Fraction.of(1, 2));
    assertThat(mixtures.compact(correlated)).hasSize(2);
    assertThat(restore(mixtures.compact(correlated))).isEqualTo(restore(correlated));
    var projectile2 =
        fixture.fixture.catalog.modifiers().values().stream()
            .filter(
                v ->
                    v.stats().size() == 1
                        && v.stats().getFirst().id().equals("projectile_skill_gem_level_+")
                        && v.stats().getFirst().min() != projectile.stats().getFirst().min())
            .findFirst()
            .orElseThrow();
    var bothPresent =
        Map.of(
            item(List.of(value(d, 47), projectileValue)),
            Fraction.of(1, 2),
            item(List.of(value(d, 48), value(projectile2, projectile2.stats().getFirst().min()))),
            Fraction.of(1, 2));
    assertThat(mixtures.compact(bothPresent)).hasSize(2);
    assertThat(restore(mixtures.compact(bothPresent))).isEqualTo(restore(bothPresent));
  }
}
