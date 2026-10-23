package com.poe2craft.bootstrap;

import static com.poe2craft.bootstrap.GoalDirectedPlannerScenariosTest.*;
import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.item.ItemState;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Real-policy branching regression: unchanged explicit cap and resource constants. */
class TerminalBranchingScenariosTest {
  final GoalDirectedPlannerScenariosTest f = new GoalDirectedPlannerScenariosTest();

  @Test
  void fourIndependentGoalsPreserveAllConcreteTiersWithinTheOriginalBudget() {
    var root = f.item(ItemState.Rarity.MAGIC, 82, List.of(f.life()));
    var goal =
        f.goal(root, Map.of(LIFE, 1L, CHAOS, 1L, COLD, 1L, "base_fire_damage_resistance_%", 1L));
    var started = System.nanoTime();
    try (var graph = f.graph(root, goal)) {
      f.exhaust(graph);
      System.out.println(
          "BRANCHING_FOUR|finished="
              + graph.finished()
              + "|complete="
              + graph.complete()
              + "|resources="
              + graph.resources()
              + "|operations="
              + graph.operations()
              + "|ms="
              + (System.nanoTime() - started) / 1000000);
      assertThat(graph.finished()).isTrue();
      assertThat(graph.complete()).isTrue();
      assertThat(graph.hasGoal()).isTrue();
      assertThat(graph.computationStates()).isLessThan(64);
      // The old graph shared executions but retained duplicate per-tier methods.
      // The joint graph stores the class once, with exact conditional source links.
      assertThat(graph.executionCount()).isEqualTo(graph.methods().size());
      var terminal =
          graph.nodes().stream()
              .filter(
                  n ->
                      n.progress().goal()
                          == com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status.MATCH)
              .toList();
      assertThat(terminal).hasSize(1);
      assertThat(terminal.getFirst().joint()).isNotEmpty();
      assertThat(terminal.getFirst().attributes().values()).anyMatch(options -> options.size() > 1);
      assertThat(graph.nodes()).allMatch(n -> n.item().explicits().size() <= 4);
    }
  }

  @Test
  void unsupportedFiveExplicitGoalDoesNotBecomeCompleteSuccessByCompression() {
    var root = f.item(ItemState.Rarity.MAGIC, 82, List.of(f.life()));
    var goal =
        f.goal(
            root,
            Map.of(
                LIFE,
                1L,
                CHAOS,
                1L,
                COLD,
                1L,
                "base_fire_damage_resistance_%",
                1L,
                "minion_skill_gem_level_+",
                3L));
    try (var graph = f.graph(root, goal)) {
      for (int resume = 0; resume < 4; resume++) {
        f.exhaust(graph);
        if (!graph.resourceLimited() || !graph.canResume()) break;
        var c = graph.checkpoint("five-goal");
        graph.increaseLimits(
            Math.min(128, c.states() * 2),
            Math.min(256, c.methods() * 2),
            Math.min(50000, c.retained() * 2),
            Math.min(10000, c.primitive() * 2));
      }
      System.out.println(
          "BRANCHING_FIVE|finished="
              + graph.finished()
              + "|complete="
              + graph.complete()
              + "|hasGoal="
              + graph.hasGoal()
              + "|resources="
              + graph.resources());
      assertThat(graph.hasGoal()).isFalse();
      assertThat(graph.nodes()).allMatch(n -> n.item().explicits().size() <= 4);
      if (graph.resourceLimited()) assertThat(graph.complete()).isFalse();
    }
  }

  @org.junit.jupiter.api.io.TempDir java.nio.file.Path directory;

  @Test
  void fiveGoalScopeProofRejectsBeforeCreatingAProductionJob() throws Exception {
    var root = f.item(ItemState.Rarity.MAGIC, 82, List.of(f.life()));
    var goal =
        f.goal(
            root,
            Map.of(
                LIFE,
                1L,
                CHAOS,
                1L,
                COLD,
                1L,
                "base_fire_damage_resistance_%",
                1L,
                "minion_skill_gem_level_+",
                3L));
    assertThat(goals.minimumExplicitModifiers(root, goal)).isEqualTo(5);
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    try (var legacy =
            new com.poe2craft.crafting.application.pathsearch.MethodSearchService(
                workbench.reviewedCatalogs(), goals, "scope-test", json);
        var service =
            new SupportConfiguration()
                .automaticSearchService(workbench, goals, legacy, json, directory.toString())) {
      var request =
          new com.poe2craft.crafting.application.pathsearch.MethodSearchProtocol.Create(
              2,
              "five-scope",
              new com.poe2craft.crafting.domain.BasicCurrencyState(
                  root, legacy.provenance(root.baseItemId())),
              goal,
              List.of());
      assertThatThrownBy(() -> service.create("scope", request))
          .isInstanceOf(
              com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.Rejected.class)
          .hasMessage("GOAL_EXCEEDS_FOUR_EXPLICIT_SCOPE");
      try (var paths = java.nio.file.Files.walk(directory)) {
        assertThat(paths.filter(java.nio.file.Files::isRegularFile).count()).isZero();
      }
    }
  }

  @Test
  void scopeProofAccountsForMultiStatModifiersAndDuplicateConditions() {
    var root = f.item(ItemState.Rarity.MAGIC, 82, List.of(f.life()));
    var goal =
        f.goal(
            root,
            Map.of(
                LIFE,
                1L,
                CHAOS,
                1L,
                COLD,
                1L,
                "base_fire_damage_resistance_%",
                1L,
                "minion_skill_gem_level_+",
                3L));
    var display =
        goals.catalog(
            new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Context(
                root.snapshotId(), root.baseItemId(), 82));
    var source = catalog.modifiers().get(f.life().modifierId());
    var allStats =
        List.of(LIFE, CHAOS, COLD, "base_fire_damage_resistance_%", "minion_skill_gem_level_+")
            .stream()
            .map(id -> new com.poe2craft.item.ModifierDefinition.StatRange(id, 1, 10))
            .toList();
    var multi =
        new com.poe2craft.item.ModifierDefinition(
            source.id(),
            source.name(),
            source.layer(),
            source.affixType(),
            source.familyIds(),
            source.requiredItemLevel(),
            source.weight(),
            source.tier(),
            source.text(),
            allStats,
            source.tags(),
            source.sourceUrl());
    var catalogWithMulti =
        new com.poe2craft.crafting.domain.goalfilter.GoalCatalog(
            display.version(),
            display.catalogVersion(),
            display.context(),
            display.groupTypes(),
            display.stats(),
            display.issues(),
            Map.of(multi.id(), multi));
    assertThat(
            com.poe2craft.crafting.domain.goalfilter.ExplicitGoalScope.minimumModifiers(
                goal, catalogWithMulti))
        .isEqualTo(1);
    var entries = Collections.nCopies(5, goal.groups().getFirst().entries().getFirst());
    var duplicated =
        new com.poe2craft.crafting.domain.goalfilter.GoalFilter(
            goal.version(),
            goal.catalogVersion(),
            goal.general(),
            List.of(
                new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Group(
                    "duplicates",
                    com.poe2craft.crafting.domain.goalfilter.GoalFilter.Type.AND,
                    false,
                    null,
                    entries)));
    assertThat(
            com.poe2craft.crafting.domain.goalfilter.ExplicitGoalScope.minimumModifiers(
                duplicated, display))
        .isEqualTo(1);
    var count =
        new com.poe2craft.crafting.domain.goalfilter.GoalFilter(
            goal.version(),
            goal.catalogVersion(),
            goal.general(),
            List.of(
                new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Group(
                    "count",
                    com.poe2craft.crafting.domain.goalfilter.GoalFilter.Type.COUNT,
                    false,
                    new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Range(
                        java.math.BigDecimal.ONE, null),
                    goal.groups().getFirst().entries())));
    assertThat(
            com.poe2craft.crafting.domain.goalfilter.ExplicitGoalScope.minimumModifiers(
                count, display))
        .isZero();
  }
}
