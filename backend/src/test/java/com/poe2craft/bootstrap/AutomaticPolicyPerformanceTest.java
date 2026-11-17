package com.poe2craft.bootstrap;

import com.poe2craft.shared.item.ItemState;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "AUTOMATIC_BENCHMARK", matches = "true")
class AutomaticPolicyPerformanceTest {
  @Test
  void measuredCatalogGraph() {
    for (int run = 0; run < 3; run++) {
      var f = new GoalDirectedPlannerScenariosTest();
      var root = f.item(ItemState.Rarity.NORMAL, 82, List.of());
      var goal = f.goal(root, Map.of(GoalDirectedPlannerScenariosTest.LIFE, 1L));
      try (var graph = f.graph(root, goal)) {
        for (int i = 0; i < 2000 && !graph.finished() && !graph.resourceLimited(); i++)
          graph.step();
        graph.publishProgress();
        var strategy = graph.strategy(100);
        graph.refineStrategy(strategy, 16);
        graph.policyCandidates(3);
        System.out.println(
            "CATALOG run=" + run + " operations=" + graph.operations() + " " + graph.metrics());
      }
    }
  }
}
