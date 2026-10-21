package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.item.*;
import java.util.*;
import java.util.stream.*;
import org.junit.jupiter.api.*;

class IndependentPlannerReviewTest extends GoalDirectedPlannerScenariosTest {
  record Scenario(String id, ItemState start, Map<String, Long> target) {}

  @TestFactory
  Stream<DynamicTest> completedProductScenarios() {
    var cold = new ModifierInstance("amulet:suffix:of-the-seal", Map.of(COLD, 6L));
    var chaos = new ModifierInstance("amulet:suffix:of-expulsion", Map.of(CHAOS, 16L));
    var minion =
        new ModifierInstance("amulet:suffix:of-the-despot", Map.of("minion_skill_gem_level_+", 2L));
    return List.of(
            new Scenario(
                "01-original-two",
                item(ItemState.Rarity.MAGIC, 82, List.of(life())),
                Map.of(LIFE, 1L, CHAOS, 1L)),
            new Scenario(
                "02-original-three",
                item(ItemState.Rarity.MAGIC, 82, List.of(life())),
                Map.of(LIFE, 1L, CHAOS, 1L, COLD, 1L)),
            new Scenario(
                "03-minion-three",
                item(ItemState.Rarity.RARE, 82, List.of(life())),
                Map.of("minion_skill_gem_level_+", 3L)),
            new Scenario(
                "04-empty-strength-percent",
                item(ItemState.Rarity.RARE, 82, List.of()),
                Map.of("strength_+%", 7L)),
            new Scenario(
                "05-whittling-preserve-life",
                item(ItemState.Rarity.RARE, 82, List.of(life(), cold)),
                Map.of(LIFE, 120L, CHAOS, 1L)),
            new Scenario(
                "06-tier-preparation",
                lowLifeAndMinionTwo(),
                Map.of(LIFE, 1L, "minion_skill_gem_level_+", 3L)),
            new Scenario(
                "07-rarity-preparation",
                item(ItemState.Rarity.MAGIC, 82, List.of(minion)),
                Map.of("minion_skill_gem_level_+", 3L)),
            new Scenario(
                "08-exalted-third",
                item(ItemState.Rarity.RARE, 82, List.of(life(), chaos)),
                Map.of(LIFE, 120L, CHAOS, 16L, COLD, 1L)),
            new Scenario(
                "09-rarity-only",
                item(ItemState.Rarity.MAGIC, 82, List.of(life())),
                Map.of(LIFE, 120L)),
            new Scenario(
                "10-cold-start",
                item(ItemState.Rarity.MAGIC, 82, List.of(cold)),
                Map.of(COLD, 6L, CHAOS, 1L)),
            new Scenario(
                "11-chaos-start",
                item(ItemState.Rarity.MAGIC, 82, List.of(chaos)),
                Map.of(CHAOS, 16L, LIFE, 1L)),
            new Scenario(
                "12-fractured-preservation",
                item(
                    ItemState.Rarity.RARE,
                    82,
                    List.of(new ModifierInstance(life().modifierId(), life().values(), true))),
                Map.of(LIFE, 120L, CHAOS, 1L)))
        .stream()
        .map(
            s ->
                DynamicTest.dynamicTest(
                    s.id(),
                    () -> {
                      long started = System.nanoTime();
                      var target = goal(s.start(), s.target());
                      try (var graph = graph(s.start(), target)) {
                        exhaust(graph);
                        var goalCount =
                            graph.nodes().stream()
                                .filter(n -> n.progress().goal() == GoalFilter.Status.MATCH)
                                .count();
                        System.out.println(
                            "REVIEW_SCENARIO|"
                                + s.id()
                                + "|finished="
                                + graph.finished()
                                + "|complete="
                                + graph.complete()
                                + "|limited="
                                + graph.resourceLimited()
                                + "|goals="
                                + goalCount
                                + "|nodes="
                                + graph.nodes().size()
                                + "|methods="
                                + graph.methods().size()
                                + "|computationStates="
                                + graph.computationStates()
                                + "|executions="
                                + graph.executionCount()
                                + "|proofRows="
                                + graph.proofRows()
                                + "|proofMs="
                                + graph.proofNanos() / 1000000
                                + "|operations="
                                + graph.operations()
                                + "|retained="
                                + graph.retainedStates()
                                + "|ms="
                                + (System.nanoTime() - started) / 1000000);
                        for (var method : graph.methods())
                          if (method.source() == 0 && method.points().containsKey(100)) {
                            var point = method.points().get(100);
                            var p = CertifiedInterval.zero(64);
                            for (var mass : point.exits().get(ExactMethodRunner.Exit.GOAL).values())
                              p = p.add(mass);
                            System.out.println(
                                "REVIEW_METHOD|"
                                    + s.id()
                                    + "|"
                                    + method.template()
                                    + "|goal100="
                                    + p.lower()
                                    + ".."
                                    + p.upper());
                            assertThat(point.mass().contains(ExactNumericDistribution.Fraction.ONE))
                                .isTrue();
                          }
                        var strategy = graph.strategy(100);
                        for (int iteration = 0; iteration < 128; iteration++) strategy.refine();
                        var reachability = strategy.result(0);
                        System.out.println(
                            "REVIEW_STRATEGY|"
                                + s.id()
                                + "|lower="
                                + reachability.value().lower()
                                + "|upper="
                                + reachability.value().upper()
                                + "|status="
                                + reachability.status());
                        assertThat(graph.finished())
                            .as(s.id() + " must exhaust supported plans")
                            .isTrue();
                        assertThat(goalCount)
                            .as(s.id() + " must reach unchanged goal")
                            .isPositive();
                      }
                    }));
  }

  @Test
  void tiedRemovalRiskStillAllowsProtectiveTierPreparation() {
    var root =
        item(
            ItemState.Rarity.RARE,
            82,
            List.of(
                new ModifierInstance("amulet:prefix:hale", Map.of(LIFE, 10L)),
                new ModifierInstance("amulet:suffix:of-the-seal", Map.of(COLD, 6L))));
    var target = goal(root, Map.of(LIFE, 1L, CHAOS, 1L));
    var k = historicalKernel(root, target);
    var direct =
        k.plan(
            root,
            action(
                com.poe2craft.crafting.domain.WorkbenchCurrency.CHAOS,
                com.poe2craft.crafting.domain.WorkbenchOmen.WHITTLING));
    assertThat(direct.branches())
        .anyMatch(
            b -> b.remaining().explicits().stream().noneMatch(m -> m.values().containsKey(LIFE)));
    assertThat(direct.branches())
        .anyMatch(
            b -> b.remaining().explicits().stream().anyMatch(m -> m.values().containsKey(LIFE)));
    var choices =
        new GoalDirectedPlanner(
                k,
                evaluation(root, target),
                com.poe2craft.crafting.domain.AutomaticCraftingAction.candidates(
                    com.poe2craft.crafting.HistoricalAutomaticPolicy.beforeDeferrals()))
            .choices(root);
    assertThat(choices)
        .anyMatch(
            c ->
                c.initial().stream()
                        .anyMatch(
                            a ->
                                a.purpose()
                                    == com.poe2craft.crafting.domain.AutomaticCurrencyPolicy.Purpose
                                        .TARGET_PREPARATION)
                    && c.cycle()
                        .getFirst()
                        .omens()
                        .equals(Set.of(com.poe2craft.crafting.domain.WorkbenchOmen.WHITTLING)));
  }
}
