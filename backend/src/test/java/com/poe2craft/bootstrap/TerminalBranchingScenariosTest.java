package com.poe2craft.bootstrap;

import static com.poe2craft.bootstrap.GoalDirectedPlannerScenariosTest.*;
import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.goal.domain.ExplicitGoalScope;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.search.application.MethodSearchProtocol;
import com.poe2craft.craftingsupport.search.application.MethodSearchService;
import com.poe2craft.craftingsupport.search.application.PathSearchProtocol;
import com.poe2craft.craftingsupport.search.domain.AutomaticMethodGraph;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Real-policy branching regression: six-affix scope with unchanged resource constants. */
class TerminalBranchingScenariosTest {
  final GoalDirectedPlannerScenariosTest f = new GoalDirectedPlannerScenariosTest();

  @Test
  void fourGoalsKeepPartialEssenceStatesWithoutInventingFinishingExalt() {
    var root = f.item(ItemState.Rarity.MAGIC, 82, List.of(f.life()));
    var goal =
        f.goal(root, Map.of(LIFE, 1L, CHAOS, 1L, COLD, 1L, "base_fire_damage_resistance_%", 1L));
    try (var graph = f.graph(root, goal)) {
      f.exhaust(graph);
      assertThat(graph.hasGoal()).isFalse();
      assertThat(graph.nodes()).anyMatch(n -> n.item().explicits().size() == 2);
      assertThat(graph.methods())
          .noneMatch(
              m ->
                  m.template().initial().isEmpty()
                      && m.template().cycle().getFirst().currency() == WorkbenchCurrency.EXALTED);
      assertThat(graph.executionCount()).isEqualTo(graph.methods().size());
      assertThat(graph.nodes()).allMatch(n -> n.item().explicitSlotCount() <= 6);
      if (graph.resourceLimited() || graph.hasPendingObservation())
        assertThat(graph.complete()).isFalse();
      try (var restored =
          AutomaticMethodGraph.restore(
              graph.checkpoint(goal.toString()),
              goal.toString(),
              policy,
              f.kernel(root, goal),
              f.evaluation(root, goal))) {
        assertThat(restored.nodes()).isEqualTo(graph.nodes());
        assertThat(restored.methods()).isEqualTo(graph.methods());
      }
    }
  }

  @Test
  void fourSuffixGoalDoesNotBecomeCompleteSuccessByCompression() {
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
      assertThat(graph.nodes()).allMatch(n -> n.item().explicits().size() <= 6);
      if (graph.resourceLimited()) assertThat(graph.complete()).isFalse();
    }
  }

  @org.junit.jupiter.api.io.TempDir java.nio.file.Path directory;

  @Test
  void sevenGoalScopeProofRejectsBeforeCreatingAProductionJob() throws Exception {
    var root = f.item(ItemState.Rarity.MAGIC, 82, List.of(f.life()));
    var goal =
        f.goal(
            root,
            Map.of(
                LIFE,
                1L,
                "base_maximum_mana",
                1L,
                "base_maximum_energy_shield",
                1L,
                CHAOS,
                1L,
                COLD,
                1L,
                "base_fire_damage_resistance_%",
                1L,
                "minion_skill_gem_level_+",
                3L));
    assertThat(goals.minimumExplicitModifiers(root, goal)).isEqualTo(7);
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    try (var legacy =
            new MethodSearchService(workbench.reviewedCatalogs(), goals, "scope-test", json);
        var service =
            new CraftingSupportConfiguration()
                .automaticSearchService(workbench, goals, legacy, json, directory.toString())) {
      var request =
          new MethodSearchProtocol.Create(
              2,
              "seven-scope",
              new BasicCurrencyState(root, legacy.provenance(root.baseItemId())),
              goal,
              List.of());
      assertThatThrownBy(() -> service.create("scope", request))
          .isInstanceOf(PathSearchProtocol.Rejected.class)
          .hasMessage("GOAL_EXCEEDS_SIX_EXPLICIT_SCOPE");
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
    var display = goals.catalog(new GoalFilter.Context(root.snapshotId(), root.baseItemId(), 82));
    var source = catalog.modifiers().get(f.life().modifierId());
    var allStats =
        List.of(LIFE, CHAOS, COLD, "base_fire_damage_resistance_%", "minion_skill_gem_level_+")
            .stream()
            .map(id -> new ModifierDefinition.StatRange(id, 1, 10))
            .toList();
    var multi =
        new ModifierDefinition(
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
        new GoalCatalog(
            display.version(),
            display.catalogVersion(),
            display.context(),
            display.groupTypes(),
            display.stats(),
            display.issues(),
            Map.of(multi.id(), multi));
    assertThat(ExplicitGoalScope.minimumModifiers(goal, catalogWithMulti)).isEqualTo(1);
    var entries = Collections.nCopies(5, goal.groups().getFirst().entries().getFirst());
    var duplicated =
        new GoalFilter(
            goal.version(),
            goal.catalogVersion(),
            goal.general(),
            List.of(new GoalFilter.Group("duplicates", GoalFilter.Type.AND, false, null, entries)));
    assertThat(ExplicitGoalScope.minimumModifiers(duplicated, display)).isEqualTo(1);
    var count =
        new GoalFilter(
            goal.version(),
            goal.catalogVersion(),
            goal.general(),
            List.of(
                new GoalFilter.Group(
                    "count",
                    GoalFilter.Type.COUNT,
                    false,
                    new GoalFilter.Range(java.math.BigDecimal.ONE, null),
                    goal.groups().getFirst().entries())));
    assertThat(ExplicitGoalScope.minimumModifiers(count, display)).isZero();
  }
}
