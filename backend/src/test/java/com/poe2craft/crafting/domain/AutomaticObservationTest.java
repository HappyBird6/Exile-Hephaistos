package com.poe2craft.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomaticObservationTest {
  @Test
  void singleExaltedKeepsFirstPassProbabilityAndOneUseAtEveryHorizon() {
    var tiny = new BasicCurrencyTransitionsTest();
    var policy = AutomaticCurrencyPolicyLoader.load();
    var kernel =
        new AutomaticCraftingKernel(
            policy,
            tiny.catalog,
            new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog)),
            "amulets");
    try (var graph =
        new AutomaticMethodGraph(
            tiny.item(ItemState.Rarity.RARE, List.of()),
            policy,
            kernel,
            item ->
                new AutomaticMethodGraph.Progress(
                    item.explicits().isEmpty() ? Status.NO_MATCH : Status.MATCH,
                    List.of(item.explicits().isEmpty() ? Status.NO_MATCH : Status.MATCH)),
            CertifiedInterval.Precision.standard(),
            64,
            256,
            50000,
            10000)) {
      for (int i = 0; i < 30000 && !graph.finished() && !graph.resourceLimited(); i++) graph.step();
      var method =
          graph.methods().stream()
              .filter(
                  m ->
                      m.source() == 0
                          && m.template().initial().isEmpty()
                          && m.template().cycle().size() == 1
                          && m.template().cycle().getFirst().currency() == WorkbenchCurrency.EXALTED
                          && m.template().cycle().getFirst().omens().isEmpty())
              .findFirst()
              .orElseThrow();
      assertThat(method.executionMode()).isEqualTo("SINGLE_PASS");
      assertThat(method.completeObservations()).contains(1, 100, 300, 500);
      var first = method.points().get(1);
      for (int count : List.of(100, 300, 500)) {
        assertThat(method.points().get(count).exits()).isEqualTo(first.exits());
        assertThat(method.points().get(count).cycleUses()).isEqualTo(first.cycleUses());
      }
      assertThat(
              AutomaticMethodExecution.materialUses(first.cycleUses())
                  .get("EXALTED")
                  .contains(Fraction.ONE))
          .isTrue();
      assertThat(first.initialUses()).isEmpty();
    }
  }

  @Test
  void repeatableChaosRetainsSeparateFirstPassAndFiniteHorizonObservations() {
    var tiny = new BasicCurrencyTransitionsTest();
    var policy = AutomaticCurrencyPolicyLoader.load();
    var kernel =
        new AutomaticCraftingKernel(
            policy,
            tiny.catalog,
            new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog)),
            "amulets");
    var root =
        tiny.item(ItemState.Rarity.RARE, List.of(new ModifierInstance("a", Map.of("a", 1L))));
    try (var graph =
        new AutomaticMethodGraph(
            root,
            policy,
            kernel,
            item -> {
              boolean hit = item.explicits().stream().anyMatch(m -> m.modifierId().equals("b"));
              return new AutomaticMethodGraph.Progress(
                  hit ? Status.MATCH : Status.NO_MATCH,
                  List.of(hit ? Status.MATCH : Status.NO_MATCH));
            },
            CertifiedInterval.Precision.standard(),
            64,
            256,
            50000,
            10000)) {
      for (int i = 0; i < 30000 && !graph.finished() && !graph.resourceLimited(); i++) graph.step();
      var method =
          graph.methods().stream()
              .filter(
                  m ->
                      m.source() == 0
                          && m.template().initial().isEmpty()
                          && m.template().cycle().getFirst().currency() == WorkbenchCurrency.CHAOS
                          && m.template().cycle().getFirst().omens().isEmpty())
              .findFirst()
              .orElseThrow();
      assertThat(method.executionMode()).isEqualTo("REPEATABLE");
      assertThat(method.points()).containsKeys(1, 100).doesNotContainKeys(300, 500);
      assertThat(method.points().get(1).active()).isNotEmpty();
      assertThat(method.points().get(1).initialUses()).isEmpty();
    }
  }
}
