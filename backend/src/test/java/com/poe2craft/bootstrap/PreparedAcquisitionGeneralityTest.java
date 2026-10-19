package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Timeout(120)
class PreparedAcquisitionGeneralityTest {
  @ParameterizedTest
  @CsvSource({
    "stocky,base_maximum_life,false",
    "belt,base_cold_damage_resistance_%,false",
    "ring,base_chaos_damage_resistance_%,false",
    "stocky,base_maximum_life,true",
    "belt,base_cold_damage_resistance_%,true",
    "ring,base_chaos_damage_resistance_%,true"
  })
  void differentFamiliesAndStatsUseTheSamePreparedReplacementLaw(
      String base, String stat, boolean highest) {
    var f = new GoalDirectedPlannerScenariosTest();
    var initial = f.workbench.initial(base, 82);
    assertThat(initial.modifiers().values())
        .anyMatch(m -> m.stats().stream().anyMatch(s -> s.id().equals(stat)));
    var s = initial.state();
    var root =
        new ItemState(
            s.snapshotId(),
            s.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            s.implicits(),
            List.of(),
            Set.of(),
            null);
    long threshold =
        highest
            ? initial.modifiers().values().stream()
                .flatMap(m -> m.stats().stream())
                .filter(v -> v.id().equals(stat))
                .mapToLong(v -> v.max())
                .max()
                .orElseThrow()
            : 1L;
    var goal = f.goal(root, Map.of(stat, threshold));
    var k = f.kernel(root, goal);
    var exact =
        f.workbench
            .automaticKernel(root, f.policy)
            .expand(root, f.action(WorkbenchCurrency.EXALTED), 5000);
    var partitioned = k.expand(root, f.action(WorkbenchCurrency.EXALTED), 5000);
    var evaluate = f.evaluation(root, goal);
    var exactHit =
        exact.outcomes().stream()
            .filter(o -> evaluate.apply(o.item()).goal() == GoalFilter.Status.MATCH)
            .map(AutomaticCraftingKernel.Outcome::probability)
            .reduce(ExactNumericDistribution.Fraction.ZERO, ExactNumericDistribution.Fraction::add);
    var partitionedHit =
        partitioned.outcomes().stream()
            .filter(o -> evaluate.apply(o.item()).goal() == GoalFilter.Status.MATCH)
            .map(AutomaticCraftingKernel.Outcome::probability)
            .reduce(ExactNumericDistribution.Fraction.ZERO, ExactNumericDistribution.Fraction::add);
    assertThat(partitionedHit).isEqualTo(exactHit);
    assertThat(partitioned.unresolved()).isEqualTo(exact.unresolved());
    assertThat(partitioned.boundary()).isEqualTo(exact.boundary());
    System.out.println(
        "ENUMERATION_ORACLE base="
            + base
            + " threshold="
            + threshold
            + " hit="
            + exactHit
            + " unresolved="
            + exact.unresolved());
    assertThat(k.plan(root, f.action(WorkbenchCurrency.CHAOS)).status())
        .isEqualTo(AutomaticCraftingPlan.Status.UNAVAILABLE);
    long started = System.nanoTime();
    try (var graph = f.graph(root, goal)) {
      f.exhaust(graph);
      System.out.println(
          "DIAGNOSTIC "
              + base
              + " finished="
              + graph.finished()
              + " limited="
              + graph.resourceLimited()
              + " nodes="
              + graph.nodes().size()
              + " methods="
              + graph.methods().size()
              + " ops="
              + graph.operations()
              + " retained="
              + graph.retainedStates());
      assertThat(graph.finished()).isTrue();
      assertThat(graph.resourceLimited()).isFalse();
      assertThat(graph.nodes()).anyMatch(n -> n.progress().goal() == GoalFilter.Status.MATCH);
      var prepared =
          graph.methods().stream()
              .filter(
                  m ->
                      !m.template().initial().isEmpty()
                          && m.template().cycle().getFirst().currency() == WorkbenchCurrency.CHAOS
                          && m.template().cycle().getFirst().omens().isEmpty())
              .toList();
      assertThat(prepared).isNotEmpty().allMatch(m -> m.executionMode().equals("REPEATABLE"));
      assertThat(prepared)
          .allSatisfy(
              m ->
                  assertThat(
                          m.points()
                              .get(100)
                              .mass()
                              .contains(ExactNumericDistribution.Fraction.ONE))
                      .isTrue());
      var plain =
          prepared.stream()
              .filter(m -> m.template().initial().getFirst().omens().isEmpty())
              .findFirst()
              .orElseThrow();
      var point = plain.points().get(100);
      var hit =
          point.exits().get(ExactMethodRunner.Exit.GOAL).values().stream()
              .reduce(CertifiedInterval.zero(64), CertifiedInterval::add);
      var unresolved =
          point.exits().get(ExactMethodRunner.Exit.UNRESOLVED).values().stream()
              .reduce(CertifiedInterval.zero(64), CertifiedInterval::add);
      System.out.println(
          "GENERALITY_PROBABILITY base="
              + base
              + " threshold="
              + threshold
              + " goal100="
              + hit.lower()
              + ".."
              + hit.upper()
              + " unresolved100="
              + unresolved.lower()
              + ".."
              + unresolved.upper());
      System.out.println(
          "GENERALITY base="
              + base
              + " stat="
              + stat
              + " threshold="
              + threshold
              + " finished="
              + graph.finished()
              + " complete="
              + graph.complete()
              + " nodes="
              + graph.nodes().size()
              + " methods="
              + graph.methods().size()
              + " operations="
              + graph.operations()
              + " millis="
              + (System.nanoTime() - started) / 1000000);
    }
    var impossible = f.goal(root, Map.of(stat, 99999L));
    try (var graph = f.graph(root, impossible)) {
      f.exhaust(graph);
      System.out.println(
          "DIAGNOSTIC "
              + base
              + " finished="
              + graph.finished()
              + " limited="
              + graph.resourceLimited()
              + " nodes="
              + graph.nodes().size()
              + " methods="
              + graph.methods().size()
              + " ops="
              + graph.operations()
              + " retained="
              + graph.retainedStates());
      assertThat(graph.finished()).isTrue();
      assertThat(graph.nodes()).noneMatch(n -> n.progress().goal() == GoalFilter.Status.MATCH);
    }
    var normal =
        new ItemState(
            s.snapshotId(),
            s.baseItemId(),
            82,
            ItemState.Rarity.NORMAL,
            s.implicits(),
            List.of(),
            Set.of(),
            null);
    assertThat(k.plan(normal, f.action(WorkbenchCurrency.EXALTED)).status())
        .isEqualTo(AutomaticCraftingPlan.Status.UNAVAILABLE);
  }
}
