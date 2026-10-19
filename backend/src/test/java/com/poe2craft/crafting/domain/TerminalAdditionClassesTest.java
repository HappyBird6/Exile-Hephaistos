package com.poe2craft.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.Exit;
import com.poe2craft.crafting.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.item.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class TerminalAdditionClassesTest {
  final BasicCurrencyTransitionsTest tiny = new BasicCurrencyTransitionsTest();
  final AutomaticCurrencyPolicy policy = AutomaticCurrencyPolicyLoader.load();
  final AutomaticCraftingKernel kernel =
      new AutomaticCraftingKernel(
          policy,
          tiny.catalog,
          new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog)),
          "amulets");
  final AutomaticCraftingAction exalt =
      new AutomaticCraftingAction(
          WorkbenchCurrency.EXALTED, Set.of(), AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
  final List<GoalDirectedPlanner.Choice> choices =
      List.of(new GoalDirectedPlanner.Choice(List.of(), List.of(exalt)));
  final AutomaticMethodGraph.Progress progress =
      new AutomaticMethodGraph.Progress(Status.NO_MATCH, List.of(Status.MATCH, Status.NO_MATCH));

  ItemState source(long value) {
    return tiny.item(ItemState.Rarity.RARE, List.of(tiny.mod("a", value)));
  }

  Optional<TerminalAdditionClasses.Key> prove(
      ItemState item, Function<ItemState, Optional<Exit>> classify) {
    return TerminalAdditionClasses.prove(item, false, progress, choices, kernel, classify, 100);
  }

  @Test
  void sharesOnlyAfterComparingEveryOutcomeAndTransportsExactIncomingValues() {
    Function<ItemState, Optional<Exit>> classify = item -> Optional.of(Exit.GOAL);
    var first = source(1);
    var second = source(2);
    assertThat(prove(first, classify)).isPresent().isEqualTo(prove(second, classify));
    var expected = kernel.expand(second, exalt, 100);
    var transported =
        kernel.expand(first, exalt, 100).outcomes().stream()
            .map(
                o ->
                    new AutomaticCraftingKernel.Outcome(
                        TerminalAdditionClasses.transport(first, second, o.item()),
                        o.probability()))
            .toList();
    assertThat(transported).containsExactlyElementsOf(expected.outcomes());
    assertThat(prove(first, classify).orElseThrow().laws().getFirst().outcomes()).hasSize(2);
  }

  @Test
  void distinguishesThresholdsAndRejectsIncompleteOrNonabsorbingLaws() {
    Function<ItemState, Optional<Exit>> sumGoal =
        item ->
            item.explicits().stream()
                        .flatMap(m -> m.values().values().stream())
                        .mapToLong(Long::longValue)
                        .sum()
                    >= 5
                ? Optional.of(Exit.GOAL)
                : Optional.empty();
    assertThat(prove(source(1), sumGoal)).isNotEqualTo(prove(source(2), sumGoal));
    assertThat(prove(source(1), item -> Optional.of(Exit.CHECKPOINT))).isEmpty();
    assertThat(
            TerminalAdditionClasses.prove(
                source(1), false, progress, choices, kernel, item -> Optional.of(Exit.GOAL), 1))
        .isEmpty();
  }

  @Test
  void rejectsRemovalIncludingLowestModifierLevelAndPreservesQualityIdentity() {
    var chaos =
        new AutomaticCraftingAction(
            WorkbenchCurrency.CHAOS,
            Set.of(WorkbenchOmen.WHITTLING),
            AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
    assertThat(
            TerminalAdditionClasses.prove(
                source(1),
                false,
                progress,
                List.of(new GoalDirectedPlanner.Choice(List.of(), List.of(chaos))),
                kernel,
                item -> Optional.of(Exit.GOAL),
                100))
        .isEmpty();
    var falseQuality = prove(source(1), item -> Optional.of(Exit.GOAL));
    var trueQuality =
        TerminalAdditionClasses.prove(
            source(1), true, progress, choices, kernel, item -> Optional.of(Exit.GOAL), 100);
    assertThat(falseQuality).isPresent().isNotEqualTo(trueQuality);
  }

  @Test
  void fullGraphSharesExecutionAndCheckpointRestoresConcreteOutcomeLaws() {
    Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
        item -> {
          boolean a = item.explicits().stream().anyMatch(m -> m.modifierId().equals("a"));
          boolean b = item.explicits().stream().anyMatch(m -> m.modifierId().equals("b"));
          return new AutomaticMethodGraph.Progress(
              a && b ? Status.MATCH : Status.NO_MATCH,
              List.of(a ? Status.MATCH : Status.NO_MATCH, b ? Status.MATCH : Status.NO_MATCH));
        };
    try (var graph =
        new AutomaticMethodGraph(
            tiny.item(ItemState.Rarity.RARE, List.of()),
            policy,
            kernel,
            evaluate,
            CertifiedInterval.Precision.standard(),
            64,
            256,
            50000,
            100)) {
      for (int i = 0; i < 20000 && !graph.finished() && !graph.resourceLimited(); i++) graph.step();
      graph.publishProgress();
      assertThat(graph.finished()).isTrue();
      assertThat(graph.executionCount()).isLessThan(graph.methods().size());
      try (var restored =
          AutomaticMethodGraph.restore(
              graph.checkpoint("same-goal"), "same-goal", policy, kernel, evaluate)) {
        assertThat(restored.nodes()).isEqualTo(graph.nodes());
        assertThat(restored.methods()).isEqualTo(graph.methods());
        assertThat(restored.executionCount()).isEqualTo(graph.executionCount());
        assertThat(restored.complete()).isEqualTo(graph.complete());
      }
      for (var method : graph.methods()) {
        if (method.template().initial().isEmpty()
            && method.template().cycle().equals(List.of(exalt))) {
          var source = graph.nodes().get(method.source()).item();
          var expected =
              kernel.expand(source, exalt, 100).outcomes().stream()
                  .filter(o -> evaluate.apply(o.item()).goal() == Status.MATCH)
                  .map(AutomaticCraftingKernel.Outcome::item)
                  .toList();
          var actual =
              method.points().get(1).exits().get(Exit.GOAL).keySet().stream()
                  .map(AutomaticMethodExecution.State::item)
                  .toList();
          assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        }
      }
    }
  }

  private Function<ItemState, AutomaticMethodGraph.Progress> both() {
    return item -> {
      boolean a = item.explicits().stream().anyMatch(m -> m.modifierId().equals("a"));
      boolean b = item.explicits().stream().anyMatch(m -> m.modifierId().equals("b"));
      return new AutomaticMethodGraph.Progress(
          a && b ? Status.MATCH : Status.NO_MATCH,
          List.of(a ? Status.MATCH : Status.NO_MATCH, b ? Status.MATCH : Status.NO_MATCH));
    };
  }

  private AutomaticCraftingKernel wideKernel() {
    var b = tiny.definition("b", ModifierDefinition.AffixType.SUFFIX, 3, 100, 3);
    var catalog = tiny.catalog(List.of(tiny.implicit, tiny.a, b));
    return new AutomaticCraftingKernel(
        policy, catalog, new WorkbenchSimulator(catalog, new CraftingEngine(catalog)), "amulets");
  }

  private void finish(AutomaticMethodGraph graph) {
    for (int i = 0; i < 20000 && !graph.finished() && !graph.resourceLimited(); i++) graph.step();
    graph.publishProgress();
  }

  @Test
  void restoresRunningRepresentativeAndAliasesWithoutDuplicatingExecution() {
    var k = wideKernel();
    try (var graph =
        new AutomaticMethodGraph(
            tiny.item(ItemState.Rarity.RARE, List.of()),
            policy,
            k,
            both(),
            CertifiedInterval.Precision.standard(),
            64,
            256,
            50000,
            1000)) {
      AutomaticMethodGraph.Checkpoint cursor = null;
      for (int i = 0; i < 20000 && !graph.finished(); i++) {
        graph.step();
        var c = graph.checkpoint("running");
        if (c.executions().stream()
            .anyMatch(
                m ->
                    m.owner() != null
                        && c.executions().stream()
                            .anyMatch(
                                owner -> owner.id().equals(m.owner()) && !owner.finished()))) {
          cursor = c;
          break;
        }
      }
      assertThat(cursor)
          .as("checkpoint must contain an unfinished representative and a concrete alias")
          .isNotNull();
      try (var restored = AutomaticMethodGraph.restore(cursor, "running", policy, k, both())) {
        assertThat(restored.methods()).isEqualTo(graph.methods());
        finish(graph);
        finish(restored);
        assertThat(restored.finished()).isTrue();
        assertThat(restored.nodes()).isEqualTo(graph.nodes());
        assertThat(restored.methods()).isEqualTo(graph.methods());
        assertThat(restored.executionCount()).isEqualTo(graph.executionCount());
      }
    }
  }

  @Test
  void restoresBudgetPauseThenEnlargesWithoutChangingAnyOutcome() {
    var k = wideKernel();
    try (var graph =
        new AutomaticMethodGraph(
            tiny.item(ItemState.Rarity.RARE, List.of()),
            policy,
            k,
            both(),
            CertifiedInterval.Precision.standard(),
            64,
            2,
            50000,
            1000)) {
      finish(graph);
      assertThat(graph.resourceLimited()).isTrue();
      try (var restored =
          AutomaticMethodGraph.restore(graph.checkpoint("paused"), "paused", policy, k, both())) {
        assertThat(restored.resourceLimited()).isTrue();
        assertThat(restored.methods()).isEqualTo(graph.methods());
        graph.increaseLimits(64, 256, 50000, 1000);
        restored.increaseLimits(64, 256, 50000, 1000);
        try (var enlarged =
            AutomaticMethodGraph.restore(
                restored.checkpoint("enlarged"), "enlarged", policy, k, both())) {
          finish(graph);
          finish(restored);
          finish(enlarged);
          assertThat(enlarged.finished()).isTrue();
          assertThat(restored.methods()).isEqualTo(graph.methods());
          assertThat(enlarged.methods()).isEqualTo(graph.methods());
          assertThat(enlarged.nodes()).isEqualTo(graph.nodes());
        }
      }
    }
  }
}
