package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.item.ItemState;
import java.util.*;
import org.junit.jupiter.api.Test;

class CertifiedPreparationSharingTest {
  @Test
  void realLowLevelSolarDoesNotShareAnUnavailableHigherCurrency() {
    var fixture = new DeferredProductionTest();
    var item = fixture.root().item();
    var root =
        new BasicCurrencyState(
            new ItemState(
                item.snapshotId(),
                item.baseItemId(),
                1,
                item.rarity(),
                item.implicits(),
                List.of(),
                item.conditions()),
            fixture.root().provenance());
    var classes =
        new BasicTransitionClasses(
            fixture.fixture.catalog,
            fixture.primitive,
            fixture.fixture.goals.rollPartition(root.item(), fixture.goal(47)),
            fixture.fixture.goals.compileEvaluation(root.item(), fixture.goal(47)));
    var index = new BasicClassIndex(classes);
    int source = index.intern(root);
    var store = new IndexedKernelStore(index, 20, 10000);
    var normal = store.request(source, WorkbenchCurrency.EXALTED);
    var greater = store.request(source, WorkbenchCurrency.GREATER_EXALTED);
    while (!normal.complete()) normal.step();
    while (!greater.complete()) greater.step();
    assertThat(normal.result().unavailable()).isFalse();
    assertThat(greater.result().unavailable()).isTrue();
    assertThatThrownBy(
            () ->
                new IndexedCertifiedMethodRunner.InitialLaw(normal)
                    .equivalentTo(new IndexedCertifiedMethodRunner.InitialLaw(greater)))
        .isInstanceOf(IllegalArgumentException.class);
    int otherSource = normal.result().outcomes().getFirst().state();
    var chaos = store.request(otherSource, WorkbenchCurrency.CHAOS);
    while (!chaos.complete()) chaos.step();
    var crossProof =
        new IndexedCertifiedMethodRunner.InitialLaw(normal)
            .equivalentTo(new IndexedCertifiedMethodRunner.InitialLaw(chaos));
    var runner =
        new IndexedCertifiedMethodRunner(
            store,
            source,
            new ExactMethodRunner.Template<>(
                "cross-origin",
                List.of(s -> WorkbenchCurrency.EXALTED),
                List.of(s -> WorkbenchCurrency.ANNULMENT)),
            id -> Optional.empty(),
            CertifiedInterval.Precision.standard());
    while (!runner.atBoundary()) runner.step();
    while (!runner.absorbed()) {
      runner.beginCycle();
      while (!runner.atBoundary()) runner.step();
    }
    var rebound = runner.snapshot().atLaterAbsorbedBoundary(100).withEquivalentInitial(crossProof);
    assertThat(rebound.observation().initialUses()).containsOnlyKeys(WorkbenchCurrency.CHAOS);
    var actualInput = index.witness(otherSource);
    assertThat(actualInput.item().explicits()).hasSize(1);
    var law =
        rebound.condition(
            ConditionalMethodLaw.root(index, Map.of(actualInput, Fraction.ONE)),
            Optional.of(ExactMethodRunner.Exit.DEAD),
            source);
    var expansion = law.expansion();
    for (int n = 0; n < 100000 && !expansion.complete(); n++) expansion.step();
    assertThat(expansion.complete()).isTrue();
    assertThat(expansion.result()).containsExactlyEntriesOf(Map.of(root, Fraction.ONE));
    assertThat(expansion.probability()).isEqualTo(Fraction.ONE);
  }

  @Test
  void equivalentPreparationKeepsCurrencyIdentityAndConcreteConditionalReplay() {
    var fixture = new MethodSearchCalculationTest();
    var root = fixture.root();
    var classes =
        new BasicTransitionClasses(
            fixture.catalog,
            fixture.transitions,
            RollPartition.exact(),
            item ->
                new com.poe2craft.crafting.domain.goalfilter.GoalEvaluator.Evaluation(
                    1,
                    "fixture",
                    com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status.NO_MATCH,
                    com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status.MATCH,
                    List.of(),
                    List.of()));
    var index = new BasicClassIndex(classes);
    int source = index.intern(root);
    var store = new IndexedKernelStore(index, 20, 10000);
    var first = store.request(source, WorkbenchCurrency.EXALTED);
    var second = store.request(source, WorkbenchCurrency.GREATER_EXALTED);
    while (!first.complete()) first.step();
    while (!second.complete()) second.step();
    var a = new IndexedCertifiedMethodRunner.InitialLaw(first);
    var b = new IndexedCertifiedMethodRunner.InitialLaw(second);
    var proof = a.equivalentTo(b);
    var different = store.request(source, WorkbenchCurrency.ANNULMENT);
    while (!different.complete()) different.step();
    assertThatThrownBy(() -> a.equivalentTo(new IndexedCertifiedMethodRunner.InitialLaw(different)))
        .isInstanceOf(IllegalArgumentException.class);
    var runner =
        new IndexedCertifiedMethodRunner(
            store,
            source,
            new ExactMethodRunner.Template<>(
                "prepare",
                List.of(s -> WorkbenchCurrency.EXALTED),
                List.of(s -> WorkbenchCurrency.ANNULMENT)),
            id -> id == source ? Optional.empty() : Optional.of(ExactMethodRunner.Exit.CHECKPOINT),
            CertifiedInterval.Precision.standard());
    while (!runner.atBoundary()) runner.step();
    var original = runner.snapshot().atLaterAbsorbedBoundary(100);
    var rebound = original.withEquivalentInitial(proof);
    assertThat(rebound.observation().exits()).isSameAs(original.observation().exits());
    assertThat(rebound.observation().initialUses())
        .containsOnlyKeys(WorkbenchCurrency.GREATER_EXALTED);
    assertThat(
            rebound
                .observation()
                .initialUses()
                .get(WorkbenchCurrency.GREATER_EXALTED)
                .contains(Fraction.ONE))
        .isTrue();
    assertThat(rebound.observation().cycleUses()).isEmpty();
    assertThatThrownBy(() -> original.withEquivalentInitial(b.equivalentTo(a)))
        .isInstanceOf(IllegalArgumentException.class);
    var actual = new HashMap<BasicCurrencyState, Fraction>();
    for (int target :
        rebound.observation().exits().get(ExactMethodRunner.Exit.CHECKPOINT).keySet()) {
      var law =
          rebound.condition(
              ConditionalMethodLaw.root(index, Map.of(root, Fraction.ONE)),
              Optional.of(ExactMethodRunner.Exit.CHECKPOINT),
              target);
      var expansion = law.expansion();
      for (int n = 0; n < 100000 && !expansion.complete(); n++) expansion.step();
      assertThat(expansion.complete()).isTrue();
      expansion
          .result()
          .forEach(
              (state, probability) ->
                  actual.merge(
                      state, probability.multiply(expansion.probability()), Fraction::add));
    }
    var raw = new HashMap<BasicCurrencyState, Fraction>();
    var cursor =
        fixture.transitions.primitiveCursor(root, WorkbenchCurrency.GREATER_EXALTED, Set.of());
    while (!cursor.complete()) {
      var outcome = cursor.next();
      raw.merge(outcome.state(), outcome.probability(), Fraction::add);
    }
    assertThat(actual).isEqualTo(raw);
  }
}
