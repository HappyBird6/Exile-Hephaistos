package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomaticNormalStartTest {
  @Test
  void normalSolarFindsRareLifeWithoutAlchemy() {
    var fixture = new GoalDirectedPlannerScenariosTest();
    var root = fixture.item(ItemState.Rarity.NORMAL, 82, List.of());
    var goal = fixture.goal(root, Map.of(GoalDirectedPlannerScenariosTest.LIFE, 1L));
    try (var graph = fixture.graph(root, goal)) {
      fixture.exhaust(graph);
      assertThat(graph.nodes())
          .anyMatch(
              n ->
                  n.item().rarity() == ItemState.Rarity.RARE
                      && n.progress().goal() == GoalFilter.Status.MATCH);
      assertThat(graph.methods())
          .anyMatch(
              m ->
                  m.source() == 0
                      && m.template().initial().stream()
                          .anyMatch(a -> a.currency() == WorkbenchCurrency.TRANSMUTATION));
      assertThat(graph.methods())
          .noneMatch(
              m ->
                  m.template().initial().stream()
                          .anyMatch(a -> a.currency() == WorkbenchCurrency.ALCHEMY)
                      || m.template().cycle().stream()
                          .anyMatch(a -> a.currency() == WorkbenchCurrency.ALCHEMY));
      System.out.println(
          "NORMAL_START|finished="
              + graph.finished()
              + "|resources="
              + graph.resources()
              + "|goalNodes="
              + graph.nodes().stream()
                  .filter(n -> n.progress().goal() == GoalFilter.Status.MATCH)
                  .count());
    }
  }
}
