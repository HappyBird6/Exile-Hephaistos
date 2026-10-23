package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomaticTierLawTest {
  final GoalDirectedPlannerScenariosTest fixture = new GoalDirectedPlannerScenariosTest();

  @Test
  void knownPointBoundariesDoNotSplitOtherwiseIdenticalFutureBehavior() {
    var root = fixture.lowLifeAndMinionTwo();
    var goal =
        fixture.goal(
            root,
            Map.of(GoalDirectedPlannerScenariosTest.LIFE, 1L, "minion_skill_gem_level_+", 3L));
    var kernel = fixture.kernel(root, goal);
    var life =
        fixture.catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.stats().size() == 1
                        && d.stats().getFirst().id().equals(GoalDirectedPlannerScenariosTest.LIFE)
                        && d.stats().getFirst().min() > 10)
            .findFirst()
            .orElseThrow();
    var otherLife =
        fixture.item(
            ItemState.Rarity.RARE,
            82,
            List.of(
                new ModifierInstance(
                    life.id(),
                    Map.of(GoalDirectedPlannerScenariosTest.LIFE, life.stats().getFirst().min())),
                root.explicits().get(1)));
    assertThat(kernel.behaviorKey(root)).isEqualTo(kernel.behaviorKey(otherLife));
    var minion =
        fixture.catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.stats().size() == 1
                        && d.stats().getFirst().id().equals("minion_skill_gem_level_+")
                        && d.stats().getFirst().min() == 3)
            .findFirst()
            .orElseThrow();
    var attained =
        fixture.item(
            ItemState.Rarity.RARE,
            82,
            List.of(
                root.explicits().get(0),
                new ModifierInstance(minion.id(), Map.of("minion_skill_gem_level_+", 3L))));
    assertThat(kernel.behaviorKey(root)).isNotEqualTo(kernel.behaviorKey(attained));
    // Scalar interval encoding still distinguishes the original known point.
    assertThat(kernel.rollBands(root)).doesNotContainKey("amulet:prefix:hale");
  }

  Map<ItemState, Fraction> flatten(List<AutomaticCraftingKernel.Outcome> law) {
    var result = new LinkedHashMap<ItemState, Fraction>();
    for (var outcome : law) {
      var states = new LinkedHashMap<ItemState, Fraction>();
      states.put(outcome.item(), outcome.probability());
      for (var entry : outcome.tiers().entrySet()) {
        var next = new LinkedHashMap<ItemState, Fraction>();
        for (var state : states.entrySet())
          for (var v : entry.getValue().variants()) {
            var mods =
                state.getKey().explicits().stream()
                    .map(
                        m ->
                            m.modifierId().equals(entry.getKey())
                                ? new ModifierInstance(
                                    v.modifierId(), Map.of(v.stat(), v.min()), m.fractured())
                                : m)
                    .toList();
            var i = state.getKey();
            var item =
                new ItemState(
                    i.snapshotId(),
                    i.baseItemId(),
                    i.itemLevel(),
                    i.rarity(),
                    i.implicits(),
                    mods,
                    i.conditions(),
                    i.augmentSockets(),
                    i.catalystQuality());
            next.merge(item, state.getValue().multiply(v.probability()), Fraction::add);
          }
        states = next;
      }
      states.forEach((i, p) -> result.merge(i, p, Fraction::add));
    }
    return result;
  }

  @Test
  void groupedTierLawReconstructsEveryScalarWeightAndGoalBoundary() {
    var root = fixture.item(ItemState.Rarity.RARE, 82, List.of());
    for (long minimum : List.of(1L, 100L, 120L)) {
      var goal = fixture.goal(root, Map.of(GoalDirectedPlannerScenariosTest.LIFE, minimum));
      var kernel = fixture.kernel(root, goal);
      var action = fixture.action(WorkbenchCurrency.EXALTED);
      var scalar = kernel.expand(root, action, 5000);
      var grouped = kernel.expandDeferred(root, Map.of(), action, 5000);
      assertThat(grouped.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
      assertThat(grouped.outcomes().size()).isLessThan(scalar.outcomes().size());
      assertThat(flatten(grouped.outcomes())).isEqualTo(flatten(scalar.outcomes()));
      var evaluate = fixture.evaluation(root, goal);
      for (var outcome : grouped.outcomes())
        for (var item :
            flatten(
                    List.of(
                        new AutomaticCraftingKernel.Outcome(
                            outcome.item(), Fraction.ONE, outcome.tiers())))
                .keySet())
          assertThat(evaluate.apply(item)).isEqualTo(evaluate.apply(outcome.item()));
      System.out.println(
          "TIER_LAW|min="
              + minimum
              + "|scalar="
              + scalar.outcomes().size()
              + "|grouped="
              + grouped.outcomes().size());
    }
  }

  @Test
  void additionAndRemovalPreserveTheFullJointLawNotProductsOfUnrelatedMarginals() {
    var root = fixture.item(ItemState.Rarity.RARE, 82, List.of());
    var goal = fixture.goal(root, Map.of(GoalDirectedPlannerScenariosTest.LIFE, 100L));
    var kernel = fixture.kernel(root, goal);
    var first =
        kernel
            .expandDeferred(root, Map.of(), fixture.action(WorkbenchCurrency.EXALTED), 5000)
            .outcomes()
            .stream()
            .filter(o -> !o.tiers().isEmpty())
            .findFirst()
            .orElseThrow();
    for (var action :
        List.of(
            fixture.action(WorkbenchCurrency.EXALTED), fixture.action(WorkbenchCurrency.CHAOS))) {
      var expected = new LinkedHashMap<ItemState, Fraction>();
      for (var source :
          flatten(
                  List.of(
                      new AutomaticCraftingKernel.Outcome(
                          first.item(), Fraction.ONE, first.tiers())))
              .entrySet()) {
        var law = kernel.expand(source.getKey(), action, 5000);
        assertThat(law.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
        flatten(law.outcomes())
            .forEach(
                (item, p) -> expected.merge(item, p.multiply(source.getValue()), Fraction::add));
      }
      var grouped = kernel.expandDeferred(first.item(), first.tiers(), action, 5000);
      assertThat(flatten(grouped.outcomes())).isEqualTo(expected);
    }
  }

  @Test
  void graphAndCheckpointKeepTierLawsInStateIdentity() {
    var root = fixture.item(ItemState.Rarity.NORMAL, 82, List.of());
    var goal = fixture.goal(root, Map.of(GoalDirectedPlannerScenariosTest.LIFE, 1L));
    try (var graph = fixture.graph(root, goal)) {
      fixture.exhaust(graph);
      assertThat(graph.finished()).isTrue();
      assertThat(graph.nodes()).anyMatch(n -> !n.tiers().isEmpty());
      var saved = graph.checkpoint(goal.toString());
      assertThat(saved.nodes()).anyMatch(n -> !n.tiers().isEmpty());
      try (var restored =
          AutomaticMethodGraph.restore(
              saved,
              goal.toString(),
              fixture.policy,
              fixture.kernel(root, goal),
              fixture.evaluation(root, goal))) {
        assertThat(restored.nodes()).isEqualTo(graph.nodes());
        assertThat(restored.methods()).isEqualTo(graph.methods());
      }
      System.out.println(
          "TIER_GRAPH|nodes="
              + graph.nodes().size()
              + "|laws="
              + graph.nodes().stream().filter(n -> !n.tiers().isEmpty()).count());
    }
  }
}
