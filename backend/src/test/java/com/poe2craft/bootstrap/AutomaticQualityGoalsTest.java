package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.Exit;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomaticQualityGoalsTest {
  final GoalDirectedPlannerScenariosTest fixture = new GoalDirectedPlannerScenariosTest();
  final ItemState root = fixture.item(ItemState.Rarity.RARE, 82, List.of(fixture.life()));

  @Test
  void quality20NeedsWorkButNoBreachAndRoundTripsTheExactCursor() {
    var goal = fixture.goal(root, Map.of(GoalDirectedPlannerScenariosTest.LIFE, 1L));
    var kernel = fixture.kernel(root, goal).withQualityGoal(20);
    try (var graph =
        new AutomaticMethodGraph(
            root,
            GoalDirectedPlannerScenariosTest.policy,
            kernel,
            fixture.evaluation(root, goal),
            CertifiedInterval.Precision.standard(),
            64,
            128,
            20000,
            5000)) {
      assertThat(graph.nodes().getFirst().progress().goal()).isEqualTo(GoalFilter.Status.NO_MATCH);
      for (int i = 0; i < 1000 && !graph.finished() && !graph.resourceLimited(); i++) graph.step();
      graph.publishProgress();
      assertThat(graph.nodes())
          .anyMatch(
              n -> n.abstractQuality() == 20 && n.progress().goal() == GoalFilter.Status.MATCH);
      assertThat(graph.methods())
          .noneMatch(
              m ->
                  m.template().initial().stream()
                      .anyMatch(a -> a.currency() == WorkbenchCurrency.ESSENCE_BREACH));
      assertThat(graph.methods())
          .anyMatch(
              m ->
                  m.template().cycle().getFirst().purpose()
                      == AutomaticCurrencyPolicy.Purpose.QUALITY_BASE_CHECKPOINT);
      try (var restored =
          AutomaticMethodGraph.restore(
              graph.checkpoint("quality20"),
              "quality20",
              GoalDirectedPlannerScenariosTest.policy,
              kernel,
              fixture.evaluation(root, goal))) {
        assertThat(restored.nodes()).isEqualTo(graph.nodes());
        assertThat(restored.methods()).isEqualTo(graph.methods());
      }
    }
  }

  @Test
  void qualityAbove20RequiresCapThenWorkAndCapMayRemain() {
    var goal = fixture.goal(root, Map.of(GoalDirectedPlannerScenariosTest.LIFE, 1L));
    var kernel = fixture.kernel(root, goal).withQualityGoal(30);
    var breach =
        new AutomaticCraftingAction(
            WorkbenchCurrency.ESSENCE_BREACH, Set.of(), AutomaticCurrencyPolicy.Purpose.ORDINARY);
    var work =
        new AutomaticCraftingAction(
            WorkbenchCurrency.ESSENCE_BREACH,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT);
    assertThat(kernel.expand(root, work, 5000).status())
        .isEqualTo(AutomaticCraftingKernel.Status.UNAVAILABLE);
    var law = kernel.expand(root, breach, 5000);
    assertThat(law.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
    var cap = law.outcomes().getFirst().item();
    assertThat(AutomaticCraftingKernel.hasQualityCap(cap)).isTrue();
    assertThat(cap.catalystQuality()).isNull();
    assertThat(cap.explicits()).doesNotContain(fixture.life());
    try (var execution =
        new AutomaticMethodExecution(
            cap,
            new AutomaticMethodExecution.Template("quality40", List.of(), List.of(work)),
            kernel,
            (item, quality) -> quality >= 30 ? Optional.of(Exit.GOAL) : Optional.empty(),
            CertifiedInterval.Precision.standard(),
            5000,
            AutomaticMethodExecution.PreparationSuccess.STOP_ON_FIRST_HIT,
            false,
            0)) {
      execution.beginCycle();
      while (!execution.atBoundary()) execution.step(64);
      assertThat(execution.observation().exits().get(Exit.GOAL).keySet())
          .allMatch(
              s ->
                  s.abstractQuality() == 40
                      && s.quality40()
                      && AutomaticCraftingKernel.hasQualityCap(s.item()));
      assertThat(AutomaticMethodExecution.materialUses(execution.observation().cycleUses()))
          .containsOnlyKeys("WORK:QUALITY_40");
    }
  }

  @Test
  void alreadyKnownQuality40WithTheCapDoesNotInventAnotherQualityWorkCost() {
    var explicits = new ArrayList<>(root.explicits());
    explicits.add(
        new ModifierInstance(QualityLimitRules.BREACH_ID, Map.of(QualityLimitRules.STAT_ID, 20L)));
    var typed =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            root.itemLevel(),
            root.rarity(),
            root.implicits(),
            explicits,
            root.conditions(),
            root.augmentSockets(),
            new CatalystQuality(CatalystQuality.Type.FLESH, 40));
    var goal = fixture.goal(root, Map.of(GoalDirectedPlannerScenariosTest.LIFE, 1L));
    try (var graph =
        new AutomaticMethodGraph(
            typed,
            GoalDirectedPlannerScenariosTest.policy,
            fixture.kernel(root, goal).withQualityGoal(30),
            item -> new AutomaticMethodGraph.Progress(GoalFilter.Status.MATCH, List.of()),
            CertifiedInterval.Precision.standard(),
            64,
            128,
            20000,
            5000)) {
      assertThat(graph.nodes().getFirst().progress().goal()).isEqualTo(GoalFilter.Status.MATCH);
      assertThat(graph.finished()).isTrue();
      assertThat(graph.methods()).isEmpty();
    }
  }

  @Test
  void qualityPrerequisiteCanReplaceAnAlreadyMatchedAffixAndReacquireItAfterWork() {
    var goal = fixture.goal(root, Map.of(GoalDirectedPlannerScenariosTest.LIFE, 1L));
    var kernel = fixture.kernel(root, goal).withQualityGoal(30);
    try (var graph =
        new AutomaticMethodGraph(
            root,
            GoalDirectedPlannerScenariosTest.policy,
            kernel,
            fixture.evaluation(root, goal),
            CertifiedInterval.Precision.standard(),
            64,
            128,
            20000,
            5000)) {
      fixture.exhaust(graph);
      assertThat(graph.finished())
          .as(
              "%s; goals=%s; methods=%s",
              graph.resources(),
              graph.nodes().stream()
                  .filter(n -> n.progress().goal() == GoalFilter.Status.MATCH)
                  .count(),
              graph.methods().stream().map(m -> m.source() + ":" + m.template().cycle()).toList())
          .isTrue();
      assertThat(graph.nodes())
          .anyMatch(
              n ->
                  n.abstractQuality() == 40
                      && n.progress().goal() == GoalFilter.Status.MATCH
                      && n.item().explicits().stream()
                          .anyMatch(
                              m -> m.values().containsKey(GoalDirectedPlannerScenariosTest.LIFE)));
      var completed = graph.nodes().stream().filter(n -> n.abstractQuality() == 40).toList();
      assertThat(graph.methods())
          .noneMatch(
              m ->
                  completed.stream().anyMatch(n -> n.id() == m.source())
                      && m.template().cycle().getFirst().purpose()
                          == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT);
    }
  }

  @Test
  void knownQualityAloneDoesNotRequireAnUnimplementedAffixScalingProjection() {
    var typed =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            root.itemLevel(),
            root.rarity(),
            root.implicits(),
            root.explicits(),
            root.conditions(),
            root.augmentSockets(),
            new CatalystQuality(CatalystQuality.Type.FLESH, 20));
    var context = new GoalFilter.Context(root.snapshotId(), root.baseItemId(), root.itemLevel());
    var catalog = GoalDirectedPlannerScenariosTest.goals.catalog(context);
    for (int minimum : List.of(20, 21)) {
      var goal =
          new GoalFilter(
              1,
              catalog.catalogVersion(),
              new GoalFilter.General(
                  root.baseItemId(),
                  new GoalFilter.Range(null, null),
                  List.of(ItemState.Rarity.RARE),
                  minimum),
              List.of());
      assertThat(GoalDirectedPlannerScenariosTest.goals.evaluate(typed, goal).status())
          .isEqualTo(minimum == 20 ? GoalFilter.Status.MATCH : GoalFilter.Status.NO_MATCH);
    }
    var affixGoal = fixture.goal(root, Map.of(GoalDirectedPlannerScenariosTest.LIFE, 1L));
    assertThat(GoalDirectedPlannerScenariosTest.goals.evaluate(typed, affixGoal).status())
        .isEqualTo(GoalFilter.Status.UNSUPPORTED);
  }

  @Test
  void oldGoalJsonHasNoNewRequiredFieldAndNewQualityIsValidatedAndPreserved() throws Exception {
    var json = new ObjectMapper();
    var old = fixture.goal(root, Map.of(GoalDirectedPlannerScenariosTest.LIFE, 1L));
    var text = json.writeValueAsString(old);
    assertThat(text).doesNotContain("\"quality\"");
    assertThat(json.readValue(text, GoalFilter.class)).isEqualTo(old);
    var general = old.general();
    for (int quality : List.of(0, 20, 21, 30, 40)) {
      var goal =
          new GoalFilter(
              old.version(),
              old.catalogVersion(),
              new GoalFilter.General(
                  general.baseItemId(), general.itemLevel(), general.rarities(), quality),
              old.groups());
      assertThat(
              GoalDirectedPlannerScenariosTest.goals
                  .validate(new GoalFilter.Context(root.snapshotId(), root.baseItemId(), 82), goal)
                  .valid())
          .isTrue();
      assertThat(json.readValue(json.writeValueAsString(goal), GoalFilter.class)).isEqualTo(goal);
    }
    var invalid =
        new GoalFilter(
            old.version(),
            old.catalogVersion(),
            new GoalFilter.General(
                general.baseItemId(), general.itemLevel(), general.rarities(), 41),
            old.groups());
    assertThatThrownBy(
            () ->
                GoalDirectedPlannerScenariosTest.goals.validate(
                    new GoalFilter.Context(root.snapshotId(), root.baseItemId(), 82), invalid))
        .hasMessageContaining("Invalid goal");
  }
}
