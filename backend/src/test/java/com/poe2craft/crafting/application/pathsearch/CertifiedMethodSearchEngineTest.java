package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.WorkbenchCurrency;
import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.item.ItemState;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CertifiedMethodSearchEngineTest {
  record Identity(
      BasicTransitionClasses.Key source,
      List<WorkbenchCurrency> initial,
      List<WorkbenchCurrency> cycle) {}

  @Test
  void completeSmallMapContainsEveryExactLawAndPreservesMethodDepthAndAlternatives(
      @TempDir java.nio.file.Path directory) throws Exception {
    var fixture = new MethodSearchCalculationTest();
    Function<ItemState, GoalEvaluator.Evaluation> evaluate =
        item -> {
          var p = fixture.progress(item);
          var entries = new ArrayList<GoalEvaluator.EntryResult>();
          for (int i = 0; i < p.entries().size(); i++)
            entries.add(
                new GoalEvaluator.EntryResult(
                    "e" + i, p.entries().get(i), GoalFilter.Presence.UNKNOWN, null));
          return new GoalEvaluator.Evaluation(
              1,
              "fixture",
              p.goal(),
              GoalFilter.Status.MATCH,
              List.of(new GoalEvaluator.GroupResult("g", p.goal(), null, null, entries)),
              List.of());
        };
    var classes =
        new BasicTransitionClasses(
            fixture.catalog, fixture.transitions, RollPartition.exact(), evaluate);
    var exact = new IndexedMethodSearchEngine(fixture.root(), classes, evaluate, 1000000);
    var bounded =
        new CertifiedMethodSearchEngine(
            fixture.root(),
            classes,
            evaluate,
            1000000,
            new CertifiedInterval.Precision(32, 2),
            new TemporaryLawPages(directory, 0));
    int unchangedGraphSamples = 0;
    boolean earlyGoal = false;
    for (int step = 0; step < 5000000 && (!exact.complete() || !bounded.complete()); step++) {
      if (!exact.complete()) exact.step();
      if (!bounded.complete()) {
        if (step % 100 == 0) {
          long revision = bounded.graphRevision();
          var statesBefore = bounded.states();
          var methodsBefore = bounded.methods();
          bounded.step();
          if (revision == bounded.graphRevision()) {
            unchangedGraphSamples++;
            assertThat(bounded.states()).isEqualTo(statesBefore);
            assertThat(bounded.methods()).isEqualTo(methodsBefore);
          }
        } else bounded.step();
      }
      if (step % 100 == 0 && !bounded.rootDirectComplete()) {
        var earlyStates = bounded.states();
        var earlyDepths = new HashMap<String, Integer>();
        earlyStates.forEach(s -> earlyDepths.put(s.id(), s.depth()));
        assertThat(bounded.methods()).allMatch(m -> earlyDepths.get(m.source()) <= 1);
        earlyGoal |=
            earlyStates.stream().anyMatch(s -> s.progress().goal() == GoalFilter.Status.MATCH);
        assertThat(bounded.complete()).isFalse();
      }
    }
    assertThat(exact.complete()).isTrue();
    assertThat(unchangedGraphSamples).isPositive();
    assertThat(earlyGoal)
        .as("A checkpoint continuation reaches the goal before all root methods finish")
        .isTrue();
    assertThat(bounded.complete())
        .withFailMessage("work=%s retained=%s", bounded.operations(), bounded.retainedEntries())
        .isTrue();
    assertThat(bounded.storage().residentPackedBytes()).isZero();
    assertThat(bounded.storage().diskBytes()).isPositive();
    assertThat(bounded.storageLimited()).isFalse();
    var exactStates = keys(exact.states(), classes);
    var exactProgress = new HashMap<String, MethodSearchCalculation.Progress>();
    exact.states().forEach(s -> exactProgress.put(s.id(), s.progress()));
    var boundedStates = keys(bounded.states(), classes);
    assertThat(new HashSet<>(boundedStates.values()))
        .isEqualTo(new HashSet<>(exactStates.values()));
    var depths = new HashMap<BasicTransitionClasses.Key, Integer>();
    exact.states().forEach(s -> depths.put(exactStates.get(s.id()), s.depth()));
    bounded
        .states()
        .forEach(s -> assertThat(s.depth()).isEqualTo(depths.get(boundedStates.get(s.id()))));
    var methods = new HashMap<Identity, MethodSearchEngine.MethodView>();
    exact
        .methods()
        .forEach(
            m ->
                methods.put(
                    new Identity(exactStates.get(m.source()), m.initialActions(), m.actions()), m));
    assertThat(bounded.methods()).hasSize(methods.size()).hasSizeGreaterThan(6);
    long decodedBeforeSummaries = bounded.storage().decodedPages();
    bounded.methods().forEach(m -> CertifiedMethodProtocol.method(m, bounded.precision()));
    var locals =
        CertifiedMethodProtocol.locals(bounded.states(), bounded.methods(), bounded.precision());
    assertThat(
            locals.stream()
                .filter(
                    local ->
                        local.stateId().equals(bounded.rootId())
                            && local.cycles() == 100
                            && local.target() != null)
                .map(CertifiedMethodProtocol.Local::target)
                .distinct()
                .count())
        .isGreaterThanOrEqualTo(2);
    assertThat(bounded.storage().decodedPages()).isEqualTo(decodedBeforeSummaries);
    var absorbed =
        bounded.methods().stream()
            .filter(
                m ->
                    m.initialActions().isEmpty()
                        && m.actions().equals(List.of(WorkbenchCurrency.EXALTED)))
            .findFirst()
            .orElseThrow();
    assertThat(absorbed.points().get(100).active()).isEmpty();
    for (var kind : ExactMethodRunner.Exit.values()) {
      var first = absorbed.points().get(100).exits().get(kind);
      assertThat(absorbed.points().get(300).exits().get(kind)).isSameAs(first);
      assertThat(absorbed.points().get(500).exits().get(kind)).isSameAs(first);
      assertThatThrownBy(() -> first.put("class:invalid", CertifiedInterval.one(32)))
          .isInstanceOf(UnsupportedOperationException.class);
      first.forEach((key, value) -> assertThat(first.get(key)).isSameAs(value));
      assertThat(first.get("class:+1")).isNull();
    }
    assertThat(bounded.retention().savedAccounting())
        .isEqualTo(bounded.retention().uniqueSavedEntries());
    assertThat(bounded.retention().sharedMethods()).isPositive();
    for (int cycles : List.of(100, 300, 500)) {
      var oracle = exact.strategy(cycles);
      var strategy = bounded.strategy(cycles);
      for (int i = 0; i < 100; i++) {
        oracle.refine();
        oracle.refineExactPolicy();
        oracle.refineAlmostSure();
        strategy.refine();
      }
      var exactValues = new HashMap<BasicTransitionClasses.Key, Fraction>();
      oracle
          .bounds()
          .forEach(
              (id, value) -> {
                assertThat(value.exact()).isTrue();
                exactValues.put(exactStates.get(id), value.lower());
              });
      boundedStates.forEach(
          (id, key) ->
              assertThat(strategy.result(id).value().contains(exactValues.get(key))).isTrue());
      assertThat(strategy.result(bounded.rootId()).status()).isEqualTo("PROVEN_ONE");
      assertThat(strategy.result(bounded.rootId()).methodIds()).isNotEmpty();
    }
    for (var method : bounded.methods()) {
      var reference =
          methods.get(
              new Identity(
                  boundedStates.get(method.source()), method.initialActions(), method.actions()));
      assertThat(reference).isNotNull();
      assertThat(method.points().keySet()).containsExactlyInAnyOrder(100, 300, 500);
      method
          .points()
          .forEach(
              (cycles, point) -> {
                var expected = reference.points().get(cycles);
                var checkpointTotals = new HashMap<MethodSearchCalculation.Progress, Fraction>();
                for (var kind :
                    List.of(
                        ExactMethodRunner.Exit.GOAL,
                        ExactMethodRunner.Exit.CHECKPOINT,
                        ExactMethodRunner.Exit.LOSS))
                  expected
                      .exits()
                      .get(kind)
                      .forEach(
                          (target, mass) ->
                              checkpointTotals.merge(
                                  exactProgress.get(target), mass, Fraction::add));
                var actualCheckpoints = method.checkpoints().get(cycles);
                assertThat(actualCheckpoints.keySet()).isEqualTo(checkpointTotals.keySet());
                checkpointTotals.forEach(
                    (target, mass) ->
                        assertThat(actualCheckpoints.get(target).contains(mass)).isTrue());
                assertThat(point.mass().contains(Fraction.ONE)).isTrue();
                expected
                    .exits()
                    .forEach(
                        (kind, law) ->
                            verify(law, exactStates, point.exits().get(kind), boundedStates));
                verify(expected.active(), exactStates, point.active(), boundedStates);
                expected
                    .initialUses()
                    .forEach(
                        (action, mass) ->
                            assertThat(point.initialUses().get(action).contains(mass)).isTrue());
                expected
                    .cycleUses()
                    .forEach(
                        (action, mass) ->
                            assertThat(point.cycleUses().get(action).contains(mass)).isTrue());
              });
    }
    bounded.close();
    try (var files = java.nio.file.Files.list(directory)) {
      assertThat(files.toList()).isEmpty();
    }
  }

  private static Map<String, BasicTransitionClasses.Key> keys(
      List<MethodSearchEngine.StateView> states, BasicTransitionClasses classes) {
    var result = new HashMap<String, BasicTransitionClasses.Key>();
    states.forEach(s -> result.put(s.id(), classes.key(s.representative().representative())));
    return result;
  }

  private static void verify(
      Map<String, Fraction> expected,
      Map<String, BasicTransitionClasses.Key> exactStates,
      Map<String, CertifiedInterval> actual,
      Map<String, BasicTransitionClasses.Key> boundedStates) {
    var byKey = new HashMap<BasicTransitionClasses.Key, CertifiedInterval>();
    actual.forEach((id, mass) -> byKey.put(boundedStates.get(id), mass));
    assertThat(byKey.keySet())
        .isEqualTo(
            expected.keySet().stream()
                .map(exactStates::get)
                .collect(java.util.stream.Collectors.toSet()));
    expected.forEach(
        (id, mass) -> assertThat(byKey.get(exactStates.get(id)).contains(mass)).isTrue());
  }
}
