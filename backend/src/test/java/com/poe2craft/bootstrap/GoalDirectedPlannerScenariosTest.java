package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.infrastructure.BundledGoalCatalogs;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.craftingsupport.search.application.AutomaticKernelFactory;
import com.poe2craft.craftingsupport.search.domain.AutomaticCraftingKernel;
import com.poe2craft.craftingsupport.search.domain.AutomaticMethodExecution;
import com.poe2craft.craftingsupport.search.domain.AutomaticMethodGraph;
import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner;
import com.poe2craft.craftingsupport.search.domain.GoalDirectedPlanner;
import com.poe2craft.craftingsupport.search.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingAction;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingPlan;
import com.poe2craft.shared.crafting.domain.AutomaticCurrencyPolicy;
import com.poe2craft.shared.crafting.domain.HistoricalAutomaticPolicy;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.probability.Fraction;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;

/** Real catalog and the complete production policy, never a reduced currency test inventory. */
@Timeout(90)
class GoalDirectedPlannerScenariosTest {
  static final CatalogConfiguration cfg = new CatalogConfiguration();
  static final ItemCatalog catalog = cfg.itemCatalog(cfg.workbenchDefinitions());
  static final CatalogCraftingService workbench =
      cfg.workbenchService(catalog, cfg.workbenchSimulator(catalog, cfg.craftingEngine(catalog)));
  static final GoalFilterService goals =
      new GoalFilterService(
          new GoalCatalogIndex(BundledGoalCatalogs.load(workbench), GoalDefinitionsLoader.load()));
  static final AutomaticCurrencyPolicy policy = AutomaticCurrencyPolicyLoader.load();
  static final String LIFE = "base_maximum_life",
      CHAOS = "base_chaos_damage_resistance_%",
      COLD = "base_cold_damage_resistance_%";

  ItemState item(ItemState.Rarity rarity, int level, List<ModifierInstance> explicit) {
    return new ItemState(
        catalog.metadata().snapshotId(),
        catalog.base().id(),
        level,
        rarity,
        List.of(
            new ModifierInstance(
                "solar-amulet:implicit:spirit", Map.of("base_spirit_from_equipment", 15L))),
        explicit,
        Set.of(),
        null);
  }

  ModifierInstance life() {
    return new ModifierInstance("amulet:prefix:athlete-s", Map.of(LIFE, 120L));
  }

  GoalFilter goal(ItemState root, Map<String, Long> requirements) {
    var context = new GoalFilter.Context(root.snapshotId(), root.baseItemId(), root.itemLevel());
    var display = goals.catalog(context);
    var entries =
        requirements.entrySet().stream()
            .map(
                e -> {
                  var stat =
                      display.stats().stream()
                          .filter(
                              s ->
                                  s.sourceStatIds().equals(List.of(e.getKey()))
                                      && s.kind() == GoalCatalog.Kind.EXPLICIT)
                          .findFirst()
                          .orElseThrow();
                  return new GoalFilter.Entry(
                      e.getKey(),
                      stat.statId(),
                      stat.unit(),
                      new GoalFilter.Range(BigDecimal.valueOf(e.getValue()), null),
                      null,
                      false);
                })
            .toList();
    return new GoalFilter(
        1,
        display.catalogVersion(),
        new GoalFilter.General(
            root.baseItemId(), new GoalFilter.Range(null, null), List.of(ItemState.Rarity.RARE)),
        List.of(new GoalFilter.Group("goals", GoalFilter.Type.AND, false, null, entries)));
  }

  Function<ItemState, AutomaticMethodGraph.Progress> evaluation(ItemState root, GoalFilter goal) {
    var compiled = goals.compileEvaluation(root, goal);
    return state -> {
      var result = compiled.apply(state);
      return new AutomaticMethodGraph.Progress(
          result.status(),
          result.groups().stream()
              .flatMap(g -> g.entries().stream())
              .map(e -> e.status())
              .toList());
    };
  }

  AutomaticCraftingKernel kernel(ItemState root, GoalFilter goal) {
    return new AutomaticKernelFactory(workbench)
        .create(root, policy)
        .withRollPartition(goals.rollPartition(root, goal), goal.toString());
  }

  AutomaticCraftingAction action(WorkbenchCurrency currency, WorkbenchOmen... omens) {
    return new AutomaticCraftingAction(
        currency, Set.of(omens), AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
  }

  AutomaticCraftingKernel historicalKernel(ItemState root, GoalFilter goal) {
    return new AutomaticKernelFactory(workbench)
        .create(root, HistoricalAutomaticPolicy.beforeDeferrals())
        .withRollPartition(goals.rollPartition(root, goal), goal.toString());
  }

  AutomaticMethodGraph graph(ItemState root, GoalFilter goal) {
    return new AutomaticMethodGraph(
        root,
        policy,
        kernel(root, goal),
        evaluation(root, goal),
        CertifiedInterval.Precision.standard(),
        64,
        128,
        20000,
        5000);
  }

  void exhaust(AutomaticMethodGraph graph) {
    for (int i = 0; i < 120000 && !graph.finished() && !graph.resourceLimited(); i++) {
      graph.step();
      if (i % 100 == 0) graph.publishProgress();
    }
    graph.publishProgress();
  }

  @Test
  void laterSingleAffixCannotReenterInitialChaosStage() {
    var root = item(ItemState.Rarity.RARE, 82, List.of(life()));
    var goal = goal(root, Map.of("minion_skill_gem_level_+", 3L));
    var planner =
        new GoalDirectedPlanner(
            kernel(root, goal), evaluation(root, goal), AutomaticCraftingAction.candidates(policy));
    assertThat(planner.choices(root, true))
        .anyMatch(c -> c.cycle().stream().anyMatch(a -> a.currency() == WorkbenchCurrency.CHAOS));
    assertThat(planner.choices(root, false))
        .noneMatch(c -> c.cycle().stream().anyMatch(a -> a.currency() == WorkbenchCurrency.CHAOS));
    var acquiredGoal = goal(root, Map.of(LIFE, 1L, "minion_skill_gem_level_+", 3L));
    assertThat(
            new GoalDirectedPlanner(
                    kernel(root, acquiredGoal),
                    evaluation(root, acquiredGoal),
                    AutomaticCraftingAction.candidates(policy))
                .choices(root))
        .noneMatch(c -> c.cycle().stream().anyMatch(a -> a.currency() == WorkbenchCurrency.CHAOS));
  }

  @Test
  void chaosCannotAcquireFirstTargetFromMultipleExplicitAffixes() {
    var root = lowLifeAndMinionTwo();
    var goal = goal(root, Map.of("minion_skill_gem_level_+", 3L));
    var choices =
        new GoalDirectedPlanner(
                kernel(root, goal),
                evaluation(root, goal),
                AutomaticCraftingAction.candidates(policy))
            .choices(root);
    assertThat(choices)
        .noneMatch(c -> c.cycle().stream().anyMatch(a -> a.currency() == WorkbenchCurrency.CHAOS));
  }

  @Test
  void chaosKernelRejectsMultipleExplicitAffixes() {
    var root = lowLifeAndMinionTwo();
    var goal = goal(root, Map.of("minion_skill_gem_level_+", 3L));
    assertThat(kernel(root, goal).plan(root, action(WorkbenchCurrency.CHAOS)).status())
        .isNotEqualTo(AutomaticCraftingPlan.Status.READY);
  }

  @Test
  void magicLifeToRareLifeChaosHasDirectCertainEssence() {
    var root = item(ItemState.Rarity.MAGIC, 82, List.of(life()));
    var goal = goal(root, Map.of(LIFE, 1L, CHAOS, 1L));
    var law =
        kernel(root, goal)
            .expand(
                root,
                new AutomaticCraftingAction(
                    WorkbenchCurrency.GREATER_ESSENCE_RUIN,
                    Set.of(),
                    AutomaticCurrencyPolicy.Purpose.ORDINARY),
                5000);
    assertThat(law.unresolved()).isEqualTo(Fraction.ZERO);
    assertThat(law.outcomes())
        .allMatch(o -> evaluation(root, goal).apply(o.item()).goal() == GoalFilter.Status.MATCH);
    try (var graph = graph(root, goal)) {
      exhaust(graph);
      assertThat(graph.nodes()).anyMatch(n -> n.progress().goal() == GoalFilter.Status.MATCH);
      assertThat(graph.finished()).isTrue();
    }
  }

  @Test
  void thirdColdGoalKeepsPartialEssenceRoutesWithoutFinishingExalted() {
    var root = item(ItemState.Rarity.MAGIC, 82, List.of(life()));
    var goal = goal(root, Map.of(LIFE, 1L, CHAOS, 1L, COLD, 1L));
    try (var graph = graph(root, goal)) {
      exhaust(graph);
      assertThat(graph.nodes()).anyMatch(n -> n.item().explicits().size() == 2);
      assertThat(graph.nodes()).noneMatch(n -> n.progress().goal() == GoalFilter.Status.MATCH);
      assertThat(
              graph.methods().stream()
                  .filter(m -> m.source() == 0)
                  .flatMap(m -> m.template().cycle().stream())
                  .map(AutomaticCraftingAction::currency))
          .contains(
              WorkbenchCurrency.GREATER_ESSENCE_RUIN, WorkbenchCurrency.GREATER_ESSENCE_THAWING);
      assertThat(graph.methods())
          .noneMatch(
              m ->
                  m.template().initial().isEmpty()
                      && m.template().cycle().getFirst().currency() == WorkbenchCurrency.EXALTED);
      if (graph.resourceLimited()) assertThat(graph.complete()).isFalse();
    }
  }

  @Test
  void emptyMagicPreparesRareBeforeChaosAndFracturedAffixIsNotAFreeRetry() {
    var magic = item(ItemState.Rarity.MAGIC, 82, List.of());
    var goal = goal(magic, Map.of("minion_skill_gem_level_+", 3L));
    var planner =
        new GoalDirectedPlanner(
            kernel(magic, goal),
            evaluation(magic, goal),
            AutomaticCraftingAction.candidates(policy));
    assertThat(planner.choices(magic))
        .anyMatch(
            c ->
                c.initial().size() == 1
                    && c.initial().getFirst().currency() == WorkbenchCurrency.REGAL
                    && c.cycle().getFirst().currency() == WorkbenchCurrency.CHAOS);
    var upgrade = kernel(magic, goal).expand(magic, action(WorkbenchCurrency.REGAL), 5000);
    assertThat(upgrade.outcomes())
        .isNotEmpty()
        .allMatch(
            o -> o.item().rarity() == ItemState.Rarity.RARE && o.item().explicits().size() == 1);
    var fractured =
        item(
            ItemState.Rarity.RARE,
            82,
            List.of(new ModifierInstance(life().modifierId(), life().values(), true)));
    var fracturedGoal = goal(fractured, Map.of("minion_skill_gem_level_+", 3L));
    var k = kernel(fractured, fracturedGoal);
    assertThat(k.plan(fractured, action(WorkbenchCurrency.CHAOS)).status())
        .isEqualTo(AutomaticCraftingPlan.Status.UNAVAILABLE);
    assertThat(k.expand(fractured, action(WorkbenchCurrency.EXALTED), 5000).outcomes())
        .allMatch(o -> o.item().explicits().contains(fractured.explicits().getFirst()));
    assertThat(
            new GoalDirectedPlanner(
                    k,
                    evaluation(fractured, fracturedGoal),
                    AutomaticCraftingAction.candidates(policy))
                .choices(fractured))
        .noneMatch(
            c ->
                c.initial().isEmpty()
                    && c.cycle().getFirst().currency() == WorkbenchCurrency.CHAOS);
  }

  @Test
  void emptyRareMinionGoalKeepsPreparedChaosAndCompletes() {
    var root = item(ItemState.Rarity.RARE, 82, List.of());
    var goal = goal(root, Map.of("minion_skill_gem_level_+", 3L));
    assertThat(kernel(root, goal).plan(root, action(WorkbenchCurrency.CHAOS)).status())
        .isEqualTo(AutomaticCraftingPlan.Status.UNAVAILABLE);
    try (var graph = graph(root, goal)) {
      exhaust(graph);
      System.out.println(
          "EMPTY_DIAGNOSTIC finished="
              + graph.finished()
              + " limited="
              + graph.resourceLimited()
              + " retained="
              + graph.retainedStates()
              + " nodes="
              + graph.nodes().size()
              + " methods="
              + graph.methods().size()
              + " ops="
              + graph.operations());
      graph
          .methods()
          .forEach(
              m ->
                  System.out.println(
                      "EMPTY_METHOD "
                          + m.id()
                          + " "
                          + m.executionMode()
                          + " "
                          + m.finished()
                          + " "
                          + m.points().keySet()));
      assertThat(graph.finished()).isTrue();
      assertThat(graph.resourceLimited()).isFalse();
      var prepared =
          graph.methods().stream()
              .filter(
                  m ->
                      m.source() == 0
                          && !m.template().initial().isEmpty()
                          && m.template().cycle().getFirst().currency() == WorkbenchCurrency.CHAOS
                          && m.template().cycle().getFirst().omens().isEmpty())
              .toList();
      assertThat(prepared).isNotEmpty();
      assertThat(prepared).allMatch(m -> m.executionMode().equals("REPEATABLE"));
      assertThat(prepared)
          .allSatisfy(
              m -> {
                assertThat(m.points().get(100).exits().get(ExactMethodRunner.Exit.GOAL))
                    .isNotEmpty();
                assertThat(m.points().get(100).mass().contains(Fraction.ONE)).isTrue();
                System.out.println(
                    "EMPTY_RARE_CHAOS "
                        + m.template()
                        + " GOAL="
                        + m.points().get(100).exits().get(ExactMethodRunner.Exit.GOAL)
                        + " USES="
                        + m.points().get(100).cycleUses());
              });
    }
  }

  @Test
  void minionLevelThreeIsAnIntentionalProbabilisticAcquisition() {
    var stat =
        catalog.modifiers().values().stream()
            .flatMap(m -> m.stats().stream())
            .filter(s -> s.id().equals("minion_skill_gem_level_+") && s.max() == 3)
            .findFirst()
            .orElseThrow();
    var root = item(ItemState.Rarity.RARE, 82, List.of(life()));
    var goal = goal(root, Map.of(stat.id(), 3L));
    var choices =
        new GoalDirectedPlanner(
                kernel(root, goal),
                evaluation(root, goal),
                AutomaticCraftingAction.candidates(policy))
            .choices(root);
    assertThat(choices).anyMatch(c -> c.cycle().contains(action(WorkbenchCurrency.CHAOS)));
    var law = kernel(root, goal).expand(root, action(WorkbenchCurrency.CHAOS), 5000);
    assertThat(law.outcomes())
        .anyMatch(o -> evaluation(root, goal).apply(o.item()).goal() == GoalFilter.Status.MATCH);
    assertThat(law.outcomes())
        .anyMatch(o -> evaluation(root, goal).apply(o.item()).goal() != GoalFilter.Status.MATCH);
  }

  ItemState lowLifeAndMinionTwo() {
    return item(
        ItemState.Rarity.RARE,
        82,
        List.of(
            new ModifierInstance("amulet:prefix:hale", Map.of(LIFE, 10L)),
            new ModifierInstance(
                "amulet:suffix:of-the-despot", Map.of("minion_skill_gem_level_+", 2L))));
  }

  @Test
  void highTierPreparationIsPlannedBeforeWhittlingWithoutImmediateGoalGain() {
    var root = lowLifeAndMinionTwo();
    var goal = goal(root, Map.of(LIFE, 1L, "minion_skill_gem_level_+", 3L));
    var k = historicalKernel(root, goal);
    var evaluate = evaluation(root, goal);
    var direct = k.plan(root, action(WorkbenchCurrency.CHAOS, WorkbenchOmen.WHITTLING));
    assertThat(direct.branches())
        .allMatch(
            b -> b.remaining().explicits().stream().noneMatch(m -> m.values().containsKey(LIFE)));
    var choices =
        new GoalDirectedPlanner(
                k,
                evaluate,
                AutomaticCraftingAction.candidates(HistoricalAutomaticPolicy.beforeDeferrals()))
            .choices(root);
    assertThat(choices)
        .anyMatch(
            c ->
                c.initial().stream()
                        .anyMatch(
                            a -> a.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION)
                    && c.cycle().getFirst().omens().contains(WorkbenchOmen.WHITTLING));
  }

  @Test
  void magicSingleAffixCannotPrepareTwoAffixChaos() {
    var root =
        item(
            ItemState.Rarity.MAGIC,
            82,
            List.of(
                new ModifierInstance(
                    "amulet:suffix:of-the-despot", Map.of("minion_skill_gem_level_+", 2L))));
    var goal = goal(root, Map.of("minion_skill_gem_level_+", 3L));
    var choices =
        new GoalDirectedPlanner(
                kernel(root, goal),
                evaluation(root, goal),
                AutomaticCraftingAction.candidates(policy))
            .choices(root);
    assertThat(choices)
        .noneMatch(
            c ->
                c.cycle().stream()
                    .anyMatch(action -> action.currency() == WorkbenchCurrency.CHAOS));
  }

  @Test
  void highTierPreparationStopsFailedPrerequisitesAndConservesAllMass() {
    var root = lowLifeAndMinionTwo();
    var goal = goal(root, Map.of(LIFE, 1L, "minion_skill_gem_level_+", 3L));
    var k = historicalKernel(root, goal);
    var evaluate = evaluation(root, goal);
    var before = evaluate.apply(root);
    var choice =
        new GoalDirectedPlanner(
                k,
                evaluate,
                AutomaticCraftingAction.candidates(HistoricalAutomaticPolicy.beforeDeferrals()))
            .choices(root).stream()
                .filter(
                    c ->
                        c.initial().stream()
                            .anyMatch(
                                a ->
                                    a.purpose()
                                        == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION))
                .findFirst()
                .orElseThrow();
    Function<ItemState, Optional<ExactMethodRunner.Exit>> classify =
        state -> {
          var now = evaluate.apply(state);
          if (now.goal() == GoalFilter.Status.MATCH)
            return Optional.of(ExactMethodRunner.Exit.GOAL);
          for (int i = 0; i < before.entries().size(); i++)
            if (before.entries().get(i) == GoalFilter.Status.MATCH
                && now.entries().get(i) != GoalFilter.Status.MATCH)
              return Optional.of(ExactMethodRunner.Exit.LOSS);
          return Optional.empty();
        };
    try (var execution =
        new AutomaticMethodExecution(
            root,
            new AutomaticMethodExecution.Template(
                "tier-preparation", choice.initial(), choice.cycle()),
            k,
            classify,
            CertifiedInterval.Precision.standard(),
            5000,
            AutomaticMethodExecution.PreparationSuccess.STOP_ON_FIRST_HIT)) {
      while (!execution.atBoundary()) execution.step(64);
      if (!execution.absorbed()) {
        execution.beginCycle();
        while (!execution.atBoundary()) execution.step(64);
      }
      var point = execution.observation();
      assertThat(execution.absorbed()).isTrue();
      assertThat(point.mass().contains(Fraction.ONE)).isTrue();
      assertThat(point.exits().get(ExactMethodRunner.Exit.GOAL)).isNotEmpty();
      assertThat(point.exits().get(ExactMethodRunner.Exit.DEAD)).isNotEmpty();
      assertThat(execution.retainedStates()).isLessThan(2000);
      assertThat(point.initialUses().values()).allMatch(v -> v.contains(Fraction.ONE));
      assertThat(point.cycleUses().values())
          .allMatch(v -> v.upperUnits().compareTo(CertifiedInterval.one(64).upperUnits()) < 0);
    }
  }

  @Test
  void absorbingFailureCompactionPreservesProbabilityAndChargedMaterials() {
    var root = item(ItemState.Rarity.RARE, 82, List.of());
    var goal = goal(root, Map.of(CHAOS, 1L));
    var k = kernel(root, goal);
    var evaluate = evaluation(root, goal);
    Function<ItemState, Optional<ExactMethodRunner.Exit>> classify =
        state ->
            state.equals(root)
                ? Optional.empty()
                : Optional.of(
                    evaluate.apply(state).goal() == GoalFilter.Status.MATCH
                        ? ExactMethodRunner.Exit.GOAL
                        : ExactMethodRunner.Exit.LOSS);
    var template =
        new AutomaticMethodExecution.Template(
            "absorption", List.of(), List.of(action(WorkbenchCurrency.EXALTED)));
    try (var full =
            new AutomaticMethodExecution(
                root, template, k, classify, CertifiedInterval.Precision.standard(), 5000);
        var compact =
            new AutomaticMethodExecution(
                root,
                template,
                k,
                classify,
                CertifiedInterval.Precision.standard(),
                5000,
                AutomaticMethodExecution.PreparationSuccess.STOP_ON_FIRST_HIT)) {
      for (var execution : List.of(full, compact)) {
        execution.beginCycle();
        while (!execution.atBoundary()) execution.step(64);
      }
      var a = full.observation();
      var b = compact.observation();
      var lossA = CertifiedInterval.zero(64);
      for (var value : a.exits().get(ExactMethodRunner.Exit.LOSS).values())
        lossA = lossA.add(value);
      var lossB = CertifiedInterval.zero(64);
      for (var value : b.exits().get(ExactMethodRunner.Exit.LOSS).values())
        lossB = lossB.add(value);
      assertThat(a.exits().get(ExactMethodRunner.Exit.LOSS).size()).isGreaterThan(1);
      assertThat(b.exits().get(ExactMethodRunner.Exit.LOSS)).hasSize(1);
      assertThat(lossA).isEqualTo(lossB);
      assertThat(a.cycleUses()).isEqualTo(b.cycleUses());
      assertThat(a.exits().get(ExactMethodRunner.Exit.GOAL))
          .isEqualTo(b.exits().get(ExactMethodRunner.Exit.GOAL));
    }
  }

  @Test
  void emptyRareNeedsPreparedAffixBeforeReplacementEssence() {
    var root = item(ItemState.Rarity.RARE, 82, List.of());
    var goal = goal(root, Map.of("strength_+%", 7L));
    var choices =
        new GoalDirectedPlanner(
                kernel(root, goal),
                evaluation(root, goal),
                AutomaticCraftingAction.candidates(policy))
            .choices(root);
    assertThat(choices)
        .anyMatch(
            c ->
                !c.initial().isEmpty()
                    && !c.cycle().getFirst().currency().replacementModifiers().isEmpty());
  }

  @Test
  void lowerLevelDoesNotInventGreaterEssenceEligibility() {
    var root = item(ItemState.Rarity.MAGIC, 1, List.of());
    var goal = goal(root, Map.of(CHAOS, 1L));
    var law =
        kernel(root, goal)
            .expand(
                root,
                new AutomaticCraftingAction(
                    WorkbenchCurrency.GREATER_ESSENCE_RUIN,
                    Set.of(),
                    AutomaticCurrencyPolicy.Purpose.ORDINARY),
                5000);
    assertThat(law.outcomes()).isEmpty();
    assertThat(law.unresolved()).isEqualTo(Fraction.ONE);
  }

  @Test
  void whittlingPreparationUsesActualCatalogModifierLevels() {
    var weak = catalog.find("amulet:suffix:of-the-seal").orElseThrow();
    assertThat(weak.requiredItemLevel()).isEqualTo(1);
    var root =
        item(
            ItemState.Rarity.RARE,
            82,
            List.of(
                life(),
                new ModifierInstance(
                    weak.id(),
                    Map.of(weak.stats().getFirst().id(), weak.stats().getFirst().min()))));
    var goal = goal(root, Map.of(LIFE, 1L, CHAOS, 1L));
    var plan =
        historicalKernel(root, goal)
            .plan(root, action(WorkbenchCurrency.CHAOS, WorkbenchOmen.WHITTLING));
    assertThat(plan.status()).isEqualTo(AutomaticCraftingPlan.Status.READY);
    assertThat(plan.branches()).allMatch(b -> b.remaining().explicits().contains(life()));
  }

  @Test
  void unrestrictedChaosKeepsLossRiskInTheLaw() {
    var root =
        item(
            ItemState.Rarity.RARE,
            82,
            List.of(
                life(), new ModifierInstance("amulet:suffix:of-expulsion", Map.of(CHAOS, 16L))));
    var goal = goal(root, Map.of(LIFE, 1L, CHAOS, 1L, COLD, 1L));
    var law = historicalKernel(root, goal).expand(root, action(WorkbenchCurrency.CHAOS), 5000);
    assertThat(law.outcomes()).anyMatch(o -> !o.item().explicits().contains(life()));
    var sum = law.unresolved().add(law.boundary());
    for (var o : law.outcomes()) sum = sum.add(o.probability());
    assertThat(sum).isEqualTo(Fraction.ONE);
  }

  @Test
  void fourToFiveToSixPreservesProbabilityMassAndStopsAtCapacity() {
    var root = item(ItemState.Rarity.RARE, 82, List.of(life()));
    var goal = goal(root, Map.of(CHAOS, 1L));
    var k = kernel(root, goal);
    var current = root;
    for (int count = 2; count <= 6; count++) {
      // Isolated addition-phase capacity/mass law, not an automatic finishing route.
      var side =
          current.explicits().stream()
                      .filter(
                          m ->
                              catalog.modifiers().get(m.modifierId()).affixType()
                                  == ModifierDefinition.AffixType.PREFIX)
                      .count()
                  < 3
              ? WorkbenchOmen.SINISTRAL_EXALTATION
              : WorkbenchOmen.DEXTRAL_EXALTATION;
      var law =
          k.expand(
              current,
              new AutomaticCraftingAction(
                  WorkbenchCurrency.EXALTED,
                  Set.of(side),
                  AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL),
              5000);
      var mass = law.boundary().add(law.unresolved());
      for (var outcome : law.outcomes()) mass = mass.add(outcome.probability());
      assertThat(mass).isEqualTo(Fraction.ONE);
      assertThat(law.boundary()).isEqualTo(Fraction.ZERO);
      final int expected = count;
      assertThat(law.outcomes())
          .isNotEmpty()
          .allMatch(o -> o.item().explicits().size() == expected);
      current = law.outcomes().getFirst().item();
    }
    assertThat(
            k.expand(
                    current,
                    new AutomaticCraftingAction(
                        WorkbenchCurrency.EXALTED,
                        Set.of(WorkbenchOmen.SINISTRAL_EXALTATION),
                        AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL),
                    5000)
                .outcomes())
        .isEmpty();
  }

  @Test
  void impossibleNumericGoalExhaustsScopedPlansWithoutInventedHit() {
    var root = item(ItemState.Rarity.RARE, 82, List.of(life()));
    var goal = goal(root, Map.of(CHAOS, 999L));
    try (var graph = graph(root, goal)) {
      exhaust(graph);
      assertThat(graph.finished()).isTrue();
      assertThat(graph.nodes()).noneMatch(n -> n.progress().goal() == GoalFilter.Status.MATCH);
    }
  }

  @Test
  void fracturedWhittlingRemainsUnverifiedRatherThanRenormalized() {
    var root =
        item(
            ItemState.Rarity.RARE,
            82,
            List.of(new ModifierInstance(life().modifierId(), life().values(), true)));
    var goal = goal(root, Map.of(CHAOS, 1L));
    var law =
        historicalKernel(root, goal)
            .expand(root, action(WorkbenchCurrency.CHAOS, WorkbenchOmen.WHITTLING), 5000);
    assertThat(law.status()).isEqualTo(AutomaticCraftingKernel.Status.UNSUPPORTED);
    assertThat(law.unresolved()).isEqualTo(Fraction.ONE);
  }

  @Test
  void newGoalPurposeDoesNotEnableOtherExcludedCurrencies() {
    var context =
        new AutomaticCurrencyPolicy.Context(
            "amulets", AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION, WorkbenchCurrency.CHAOS);
    assertThat(policy.evaluate("CHAOS", context))
        .isEqualTo(AutomaticCurrencyPolicy.Verdict.PERMITTED);
    assertThat(policy.evaluate("ANNULMENT", context))
        .isEqualTo(AutomaticCurrencyPolicy.Verdict.EXCLUDED);
    assertThat(policy.evaluate("GREATER_CHAOS", context))
        .isEqualTo(AutomaticCurrencyPolicy.Verdict.EXCLUDED);
  }

  @Test
  void probabilityBudgetNeverRenormalizesRetainedOutcomes() {
    var root = item(ItemState.Rarity.RARE, 82, List.of());
    var goal = goal(root, Map.of(CHAOS, 1L));
    var law = kernel(root, goal).expand(root, action(WorkbenchCurrency.EXALTED), 1);
    assertThat(law.reason()).isEqualTo("OUTCOME_BUDGET");
    var sum = law.unresolved().add(law.boundary());
    for (var o : law.outcomes()) sum = sum.add(o.probability());
    assertThat(sum).isEqualTo(Fraction.ONE);
    assertThat(law.unresolved()).isNotEqualTo(Fraction.ZERO);
  }

  @Test
  void singleAffixChaosRetriesMatchFiniteSeriesIncludingUnresolvedAbsorption() {
    var stat =
        catalog.modifiers().values().stream()
            .flatMap(m -> m.stats().stream())
            .filter(s -> s.id().equals("minion_skill_gem_level_+") && s.max() == 3)
            .findFirst()
            .orElseThrow();
    var root = item(ItemState.Rarity.RARE, 82, List.of(life()));
    var goal = goal(root, Map.of(stat.id(), 3L));
    var k = kernel(root, goal);
    var evaluate = evaluation(root, goal);
    var law = k.expand(root, action(WorkbenchCurrency.CHAOS), 5000);
    var p = Fraction.ZERO;
    var retry = Fraction.ZERO;
    for (var outcome : law.outcomes()) {
      if (evaluate.apply(outcome.item()).goal() == GoalFilter.Status.MATCH)
        p = p.add(outcome.probability());
      else retry = retry.add(outcome.probability());
    }
    var expected = Fraction.ZERO;
    var survival = Fraction.ONE;
    for (int i = 0; i < 100; i++) {
      expected = expected.add(survival.multiply(p));
      survival = survival.multiply(retry);
    }
    try (var execution =
        new AutomaticMethodExecution(
            root,
            new AutomaticMethodExecution.Template(
                "stationary", List.of(), List.of(action(WorkbenchCurrency.CHAOS))),
            k,
            state ->
                Optional.ofNullable(
                    evaluate.apply(state).goal() == GoalFilter.Status.MATCH
                        ? ExactMethodRunner.Exit.GOAL
                        : null),
            CertifiedInterval.Precision.standard(),
            5000)) {
      for (int i = 0; i < 100; i++) {
        execution.beginCycle();
        while (!execution.atBoundary()) execution.step(64);
      }
      var probability = CertifiedInterval.zero(64);
      for (var mass : execution.observation().exits().get(ExactMethodRunner.Exit.GOAL).values())
        probability = probability.add(mass);
      assertThat(probability.contains(expected)).isTrue();
      assertThat(execution.observation().mass().contains(Fraction.ONE)).isTrue();
    }
    // The production graph supplies its own progress classifier. Verify that its
    // terminal no-progress classification also reaches the proven retry law.
    try (var graph = graph(root, goal)) {
      exhaust(graph);
      var method =
          graph.methods().stream()
              .filter(
                  m ->
                      m.source() == 0
                          && m.template().initial().isEmpty()
                          && m.template().cycle().equals(List.of(action(WorkbenchCurrency.CHAOS))))
              .findFirst()
              .orElseThrow();
      assertThat(method.completeObservations()).contains(100);
      var probability = CertifiedInterval.zero(64);
      for (var mass : method.points().get(100).exits().get(ExactMethodRunner.Exit.GOAL).values())
        probability = probability.add(mass);
      assertThat(probability.contains(expected)).isTrue();
    }
  }
}
