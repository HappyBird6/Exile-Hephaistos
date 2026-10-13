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
    verify(false, false);
  }

  @Test
  void weightedTierCellsMatchEveryCompletedMethodAndFullConditionalLaw() throws Exception {
    verify(true, false);
  }

  @Test
  void jointFamilyCellsMatchEveryCompletedMethodAndConcreteConditionalLaw() throws Exception {
    verify(false, true);
  }

  private void verify(boolean weightedTier, boolean orbit) throws Exception {
    verify(weightedTier, orbit, false);
  }

  @Test
  void internalMixturesRestoreFiniteGraphAndEveryConcreteMethodLaw() throws Exception {
    verify(true, false, true);
  }

  @Test
  void internalOrbitMixturesMatchEveryConcreteMethodAndRestoreOriginalGraphCells()
      throws Exception {
    verify(false, true, true);
  }

  private void verify(boolean weightedTier, boolean orbit, boolean mixture) throws Exception {
    verify(weightedTier, orbit, mixture, 100000);
  }

  @Test
  void boundedPrimitiveCachePreservesMixedDependenciesAndEveryConditionalMethodLaw()
      throws Exception {
    verify(true, false, true, 32);
  }

  private void verify(boolean weightedTier, boolean orbit, boolean mixture, int cacheLimit)
      throws Exception {
    var fixture = new MethodSearchCalculationTest();
    var definitions = new ArrayList<>(fixture.catalog.modifiers().values());
    if (weightedTier) {
      var original = fixture.catalog.modifiers().get("amulet:prefix:adept-s");
      definitions.add(
          new ModifierDefinition(
              "weighted-alternative",
              original.name(),
              original.layer(),
              original.affixType(),
              original.familyIds(),
              mixture ? 50 : 1,
              3,
              2,
              original.text(),
              List.of(new ModifierDefinition.StatRange(fixture.fixture.stat, 3, 4)),
              original.tags(),
              original.sourceUrl()));
    }
    if (orbit) {
      var original = fixture.catalog.modifiers().get("amulet:suffix:of-the-seal");
      for (var family : List.of("anonymous-x", "anonymous-y")) {
        definitions.add(
            new ModifierDefinition(
                family,
                family,
                original.layer(),
                original.affixType(),
                Set.of(family),
                1,
                1,
                1,
                original.text(),
                List.of(new ModifierDefinition.StatRange(family, 1, 1)),
                Set.of(),
                original.sourceUrl()));
        if (mixture)
          definitions.add(
              new ModifierDefinition(
                  family + "-high",
                  family,
                  original.layer(),
                  original.affixType(),
                  Set.of(family),
                  50,
                  3,
                  2,
                  original.text(),
                  List.of(new ModifierDefinition.StatRange(family, 10, 10)),
                  Set.of(),
                  original.sourceUrl()));
      }
    }
    var metadata = fixture.catalog.metadata();
    if (weightedTier || orbit)
      metadata =
          new ItemCatalog.Metadata(
              metadata.snapshotId(),
              metadata.retrievedAt(),
              metadata.sourceUrl(),
              metadata.weightPolicy(),
              metadata.rawSha256(),
              metadata.detailsSha256(),
              metadata.prefixCount() + (weightedTier ? 1 : 0),
              metadata.suffixCount() + (orbit ? mixture ? 4 : 2 : 0),
              metadata.prefixWeight() + (weightedTier ? 3 : 0),
              metadata.suffixWeight() + (orbit ? mixture ? 8 : 2 : 0));
    var catalog = new ItemCatalog(metadata, fixture.catalog.base(), definitions);
    var transitions = new BasicCurrencyTransitions(catalog, "method-search-fixture");
    var root = new BasicCurrencyState(fixture.root().item(), transitions.provenance());
    var index =
        new GoalCatalogIndex(
            List.of(catalog), fixture.fixture.definitions(), "method-search-fixture");
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
    var partition = RollPartition.compile(catalog, known, goal, root.item());
    var evaluator =
        new com.poe2craft.crafting.application.goalfilter.GoalFilterService(index)
            .compileEvaluation(root.item(), goal);
    var full = new MethodSearchCalculation(root, evaluator, transitions, Set.of(), 100000);
    var deferred =
        mixture
            ? new MethodSearchCalculation(
                root,
                evaluator,
                transitions,
                Set.of(),
                cacheLimit,
                partition,
                new DeferredMixtures(
                    new BasicTransitionClasses(catalog, transitions, partition, evaluator)))
            : new MethodSearchCalculation(
                root, evaluator, transitions, Set.of(), cacheLimit, partition);
    // Four independent families exercise 1,477 concrete method instances, including
    // slow 500-cycle tails. This is an oracle work guard, not a crafting budget.
    // The additional low/high orbit categories grow the independent oracle from 24 to 54
    // concrete states and 2,849 methods with nonstationary 500-cycle tails. Keep the same
    // complete-law assertions; only this larger fixture receives a larger execution guard.
    int workGuard = mixture && orbit ? 8000000 : 2000000;
    for (int i = 0; i < workGuard && (!full.complete() || !deferred.complete()); i++) {
      full.step();
      deferred.step();
    }
    assertThat(full.complete())
        .withFailMessage(
            "Full oracle: %s operations=%s states=%s memory=%s",
            full.cacheMetrics(), full.operations(), full.states().size(), full.memoryLimited())
        .isTrue();
    assertThat(deferred.complete())
        .withFailMessage(
            "Deferred oracle: %s operations=%s states=%s memory=%s",
            deferred.cacheMetrics(),
            deferred.operations(),
            deferred.states().size(),
            deferred.memoryLimited())
        .isTrue();
    assertThat(deferred.states()).hasSizeLessThan(full.states().size());
    if (cacheLimit < 100000) {
      assertThat(deferred.primitiveKernelEvictions()).isPositive();
      assertThat(deferred.cacheMetrics().outcomes()).isLessThan(cacheLimit);
    }
    if (weightedTier && !mixture)
      assertThat(deferred.states()).anyMatch(s -> !s.state().tiers().isEmpty());
    if (mixture) {
      assertThat(deferred.mixtureMetrics().mergedCells()).isPositive();
      if (orbit) {
        assertThat(deferred.mixtureMetrics().mergedOrbitCells()).isPositive();
        assertThat(deferred.derivedKernelEvictions()).isPositive();
        assertThat(deferred.cacheMetrics().outcomes()).isLessThan(100000);
      }
      if (weightedTier) {
        assertThat(deferred.mixtureMetrics().generatedLaws()).isPositive();
        assertThat(deferred.states()).hasSize(6).allMatch(s -> s.state().tiers().isEmpty());
      }
      assertThat(deferred.states())
          .allMatch(s -> s.state().orbits().values().stream().noneMatch(OrbitCell::mixed));
    }
    if (orbit) assertThat(deferred.states()).anyMatch(s -> !s.state().orbits().isEmpty());
    var sources = new HashMap<String, DeferredItem>();
    deferred.states().forEach(s -> sources.put(s.id(), s.state()));
    for (var method : deferred.methods()) {
      var source = sources.get(method.source());
      var concreteMethods = new ArrayList<MethodSearchCalculation.MethodView>();
      var sourceWeights = new ArrayList<Fraction>();
      for (var i = BigInteger.ZERO;
          i.compareTo(source.cardinality()) < 0;
          i = i.add(BigInteger.ONE)) {
        var id = source.concrete(i).canonicalKey();
        sourceWeights.add(source.concreteProbability(i));
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
          for (int indexOfSource = 0; indexOfSource < concreteMethods.size(); indexOfSource++) {
            var weight = sourceWeights.get(indexOfSource);
            restore(concreteMethods.get(indexOfSource).points().get(cycles).exits().get(exit))
                .forEach(
                    (state, mass) -> expected.merge(state, mass.multiply(weight), Fraction::add));
          }
          assertThat(restore(observed.exits().get(exit))).isEqualTo(expected);
        }
        var expectedActive = new HashMap<BasicCurrencyState, Fraction>();
        var initial = new HashMap<WorkbenchCurrency, Fraction>();
        var currency = new HashMap<WorkbenchCurrency, Fraction>();
        for (int indexOfSource = 0; indexOfSource < concreteMethods.size(); indexOfSource++) {
          var concrete = concreteMethods.get(indexOfSource);
          var weight = sourceWeights.get(indexOfSource);
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
          for (var i = BigInteger.ZERO;
              i.compareTo(cell.cardinality()) < 0;
              i = i.add(BigInteger.ONE))
            result.merge(
                cell.concrete(i), mass.multiply(cell.concreteProbability(i)), Fraction::add);
        });
    return result;
  }
}
