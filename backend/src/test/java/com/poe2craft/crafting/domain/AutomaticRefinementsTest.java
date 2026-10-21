package com.poe2craft.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.Exit;
import com.poe2craft.crafting.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomaticRefinementsTest {
  final BasicCurrencyTransitionsTest tiny = new BasicCurrencyTransitionsTest();
  final AutomaticCurrencyPolicy policy = AutomaticCurrencyPolicyLoader.load();

  AutomaticCraftingKernel kernel(ItemCatalog catalog) {
    return new AutomaticCraftingKernel(
        policy, catalog, new WorkbenchSimulator(catalog, new CraftingEngine(catalog)), "amulets");
  }

  @Test
  void currentPolicyDefersThreeOmensWithoutRemovingManualDefinitions() {
    for (var omen :
        List.of(
            WorkbenchOmen.SINISTRAL_ERASURE,
            WorkbenchOmen.DEXTRAL_ERASURE,
            WorkbenchOmen.WHITTLING)) {
      assertThat(
              policy.evaluate(
                  "OMEN:" + omen.name(),
                  new AutomaticCurrencyPolicy.Context(
                      "amulets",
                      AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION,
                      WorkbenchCurrency.CHAOS)))
          .isEqualTo(AutomaticCurrencyPolicy.Verdict.DEFERRED);
      assertThat(AutomaticCraftingAction.candidates(policy))
          .noneMatch(a -> a.omens().contains(omen));
      assertThat(omen.id()).isNotBlank();
    }
  }

  @Test
  void chaosFirstHitWithin100IncludesEveryFailureAndRestores() {
    var root = tiny.item(ItemState.Rarity.RARE, List.of(tiny.mod("b", 3)));
    var chaos =
        new AutomaticCraftingAction(
            WorkbenchCurrency.CHAOS, Set.of(), AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
    var kernel = kernel(tiny.catalog);
    java.util.function.Function<ItemState, Optional<Exit>> hit =
        item ->
            item.explicits().stream().anyMatch(m -> m.modifierId().equals("a"))
                ? Optional.of(Exit.GOAL)
                : Optional.empty();
    try (var execution =
        new AutomaticMethodExecution(
            root,
            new AutomaticMethodExecution.Template("100-chaos", List.of(), List.of(chaos)),
            kernel,
            hit,
            CertifiedInterval.Precision.standard(),
            100,
            AutomaticMethodExecution.PreparationSuccess.STOP_ON_FIRST_HIT)) {
      for (int cycle = 0; cycle < 100; cycle++) {
        execution.beginCycle();
        while (!execution.atBoundary()) execution.step(1);
      }
      var point = execution.observation();
      var expected = Fraction.of(3, 4).pow(100).complement();
      var mass =
          point.exits().get(Exit.GOAL).values().stream()
              .reduce(CertifiedInterval.zero(64), CertifiedInterval::add);
      assertThat(mass.contains(expected)).isTrue();
      assertThat(point.exits().get(Exit.LOSS)).isEmpty();
      assertThat(point.mass().contains(Fraction.ONE)).isTrue();
      try (var restored =
          AutomaticMethodExecution.restore(execution.checkpoint("a"), kernel, "a", hit)) {
        assertThat(restored.observation()).isEqualTo(point);
      }
    }
  }

  @Test
  void routeStrategyUsesTheSameFinite100ChaosProbabilityWithoutAnUnlimitedRetryLoop() {
    var root = tiny.item(ItemState.Rarity.RARE, List.of(tiny.mod("b", 3)));
    java.util.function.Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
        item -> {
          boolean hit =
              item.explicits().stream()
                  .anyMatch(
                      m ->
                          m.modifierId().equals("a")
                              && Long.valueOf(2).equals(m.values().get("a")));
          return new AutomaticMethodGraph.Progress(
              hit ? Status.MATCH : Status.NO_MATCH, List.of(hit ? Status.MATCH : Status.NO_MATCH));
        };
    try (var graph =
        new AutomaticMethodGraph(
            root,
            policy,
            kernel(tiny.catalog),
            evaluate,
            CertifiedInterval.Precision.standard(),
            64,
            128,
            20000,
            5000)) {
      for (int i = 0; i < 30000 && !graph.finished() && !graph.resourceLimited(); i++) graph.step();
      assertThat(graph.finished()).isTrue();
      var expected = Fraction.of(7, 8).pow(100).complement();
      var strategy = graph.strategy(100);
      for (int i = 0; i < 16; i++) strategy.refine();
      assertThat(strategy.result(0).value().contains(expected)).isTrue();
      assertThat(strategy.result(0).value().contains(Fraction.ONE)).isFalse();
    }
  }

  @Test
  void changingChaosFailuresUseTheirOwnLawFor100Attempts() {
    var definitions =
        List.of(
            tiny.implicit,
            tiny.definition("a", ModifierDefinition.AffixType.PREFIX, 1, 1, 1),
            tiny.definition("b", ModifierDefinition.AffixType.SUFFIX, 1, 1, 2),
            tiny.definition("c", ModifierDefinition.AffixType.PREFIX, 1, 1, 1),
            tiny.definition("d", ModifierDefinition.AffixType.SUFFIX, 1, 1, 10));
    var root = tiny.item(ItemState.Rarity.RARE, List.of(tiny.mod("a", 1), tiny.mod("b", 1)));
    var chaos =
        new AutomaticCraftingAction(
            WorkbenchCurrency.CHAOS, Set.of(), AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
    java.util.function.Function<ItemState, Optional<Exit>> classify =
        item ->
            item.explicits().stream().anyMatch(m -> m.modifierId().equals("c"))
                ? Optional.of(Exit.GOAL)
                : Optional.empty();
    // Independent four-modifier urn: remove either affix uniformly, then draw by weight
    // excluding the retained modifier. Failed pairs have different subsequent laws.
    var weights = Map.of("a", 1, "b", 2, "c", 1, "d", 10);
    Map<Set<String>, Fraction> active = Map.of(Set.of("a", "b"), Fraction.ONE);
    Fraction hit = Fraction.ZERO;
    for (int cycle = 0; cycle < 100; cycle++) {
      var next = new HashMap<Set<String>, Fraction>();
      for (var entry : active.entrySet()) {
        for (var retained : entry.getKey()) {
          int total = 14 - weights.get(retained);
          for (var draw : weights.entrySet()) {
            if (draw.getKey().equals(retained)) continue;
            var mass = entry.getValue().multiply(Fraction.of(draw.getValue(), 2L * total));
            if (draw.getKey().equals("c")) hit = hit.add(mass);
            else next.merge(Set.of(retained, draw.getKey()), mass, Fraction::add);
          }
        }
      }
      active = next;
    }
    var iid = Fraction.of(25, 312).complement().pow(100).complement();
    assertThat(hit).isNotEqualTo(iid);
    try (var execution =
        new AutomaticMethodExecution(
            root,
            new AutomaticMethodExecution.Template("changing-chaos", List.of(), List.of(chaos)),
            kernel(tiny.catalog(definitions)),
            classify,
            CertifiedInterval.Precision.standard(),
            100,
            AutomaticMethodExecution.PreparationSuccess.STOP_ON_FIRST_HIT)) {
      for (int cycle = 0; cycle < 100; cycle++) {
        execution.beginCycle();
        while (!execution.atBoundary()) execution.step(1);
      }
      var point = execution.observation();
      var mass =
          point.exits().get(Exit.GOAL).values().stream()
              .reduce(CertifiedInterval.zero(64), CertifiedInterval::add);
      assertThat(mass.contains(hit)).isTrue();
      assertThat(mass.contains(iid)).isFalse();
      assertThat(point.exits().get(Exit.LOSS)).isEmpty();
      assertThat(point.mass().contains(Fraction.ONE)).isTrue();
    }
  }

  @Test
  void incomparablePrefixAndSuffixGoalsKeepBothOmenRoutesAndOrdinaryLaw() {
    var root = tiny.item(ItemState.Rarity.RARE, List.of());
    java.util.function.Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
        item -> {
          var ids = item.explicits().stream().map(ModifierInstance::modifierId).toList();
          return new AutomaticMethodGraph.Progress(
              ids.containsAll(List.of("a", "b")) ? Status.MATCH : Status.NO_MATCH,
              List.of(
                  ids.contains("a") ? Status.MATCH : Status.NO_MATCH,
                  ids.contains("b") ? Status.MATCH : Status.NO_MATCH));
        };
    var choices =
        new GoalDirectedPlanner(
                kernel(tiny.catalog), evaluate, AutomaticCraftingAction.candidates(policy))
            .choices(root);
    var exalt =
        choices.stream()
            .filter(
                c ->
                    c.initial().isEmpty()
                        && c.cycle().getFirst().currency() == WorkbenchCurrency.EXALTED)
            .toList();
    assertThat(exalt).hasSize(3);
    assertThat(exalt.stream().map(c -> c.cycle().getFirst().omens()))
        .containsExactlyInAnyOrder(
            Set.of(),
            Set.of(WorkbenchOmen.SINISTRAL_EXALTATION),
            Set.of(WorkbenchOmen.DEXTRAL_EXALTATION));
  }

  @Test
  void exaltPrefersUsefulPrefixOmenAndKeepsProbabilities() {
    var root = tiny.item(ItemState.Rarity.RARE, List.of());
    java.util.function.Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
        item -> {
          boolean a = item.explicits().stream().anyMatch(m -> m.modifierId().equals("a"));
          return new AutomaticMethodGraph.Progress(
              a ? Status.MATCH : Status.NO_MATCH, List.of(a ? Status.MATCH : Status.NO_MATCH));
        };
    var kernel = kernel(tiny.catalog);
    var choices =
        new GoalDirectedPlanner(kernel, evaluate, AutomaticCraftingAction.candidates(policy))
            .choices(root);
    var exalt =
        choices.stream()
            .filter(
                c ->
                    c.initial().isEmpty()
                        && c.cycle().getFirst().currency() == WorkbenchCurrency.EXALTED)
            .toList();
    assertThat(exalt).hasSize(1);
    assertThat(exalt.getFirst().cycle().getFirst().omens())
        .containsExactly(WorkbenchOmen.SINISTRAL_EXALTATION);
    assertThat(kernel.expand(root, exalt.getFirst().cycle().getFirst(), 100).outcomes())
        .allMatch(o -> evaluate.apply(o.item()).goal() == Status.MATCH);
  }

  @Test
  void fullSuffixSlotsKeepPlainExaltAndDoNotDropTarget() {
    var definitions = new ArrayList<>(List.of(tiny.implicit, tiny.a, tiny.b));
    definitions.add(tiny.definition("c", ModifierDefinition.AffixType.SUFFIX, 1, 1, 1));
    definitions.add(tiny.definition("d", ModifierDefinition.AffixType.SUFFIX, 1, 1, 1));
    var catalog = tiny.catalog(definitions);
    var root =
        tiny.item(
            ItemState.Rarity.RARE, List.of(tiny.mod("b", 3), tiny.mod("c", 1), tiny.mod("d", 1)));
    var kernel = kernel(catalog);
    java.util.function.Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
        item -> {
          boolean a = item.explicits().stream().anyMatch(m -> m.modifierId().equals("a"));
          return new AutomaticMethodGraph.Progress(
              a ? Status.MATCH : Status.NO_MATCH, List.of(a ? Status.MATCH : Status.NO_MATCH));
        };
    var choices =
        new GoalDirectedPlanner(kernel, evaluate, AutomaticCraftingAction.candidates(policy))
            .choices(root);
    var exalt =
        choices.stream()
            .filter(
                c ->
                    c.initial().isEmpty()
                        && c.cycle().getFirst().currency() == WorkbenchCurrency.EXALTED)
            .toList();
    assertThat(exalt).hasSize(1);
    assertThat(exalt.getFirst().cycle().getFirst().omens()).isEmpty();
  }

  @Test
  void fullPrefixSlotsKeepPlainExaltAndDoNotDropSuffixTarget() {
    var definitions = new ArrayList<>(List.of(tiny.implicit, tiny.a, tiny.b));
    definitions.add(tiny.definition("c", ModifierDefinition.AffixType.PREFIX, 1, 1, 1));
    definitions.add(tiny.definition("d", ModifierDefinition.AffixType.PREFIX, 1, 1, 1));
    var root =
        tiny.item(
            ItemState.Rarity.RARE, List.of(tiny.mod("a", 1), tiny.mod("c", 1), tiny.mod("d", 1)));
    var kernel = kernel(tiny.catalog(definitions));
    java.util.function.Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
        item -> {
          boolean b = item.explicits().stream().anyMatch(m -> m.modifierId().equals("b"));
          return new AutomaticMethodGraph.Progress(
              b ? Status.MATCH : Status.NO_MATCH, List.of(b ? Status.MATCH : Status.NO_MATCH));
        };
    var choices =
        new GoalDirectedPlanner(kernel, evaluate, AutomaticCraftingAction.candidates(policy))
            .choices(root);
    var exalt =
        choices.stream()
            .filter(
                c ->
                    c.initial().isEmpty()
                        && c.cycle().getFirst().currency() == WorkbenchCurrency.EXALTED)
            .toList();
    assertThat(exalt).hasSize(1);
    assertThat(exalt.getFirst().cycle().getFirst().omens()).isEmpty();
  }
}
