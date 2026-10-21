package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.*;

/** Diagnostic of the existing product resume ceilings, not a replacement for completion tests. */
@Timeout(180)
class AutomaticPreparationBudgetTest {
  @Test
  void tierAndRarityPreparationAtTheUnchangedProductMaximum() {
    var fixture = new GoalDirectedPlannerScenariosTest();
    var minion =
        new ModifierInstance("amulet:suffix:of-the-despot", Map.of("minion_skill_gem_level_+", 2L));
    var starts =
        List.of(
            fixture.lowLifeAndMinionTwo(),
            fixture.item(ItemState.Rarity.MAGIC, 82, List.of(minion)));
    for (int scenario = 0; scenario < starts.size(); scenario++) {
      var root = starts.get(scenario);
      var goal =
          fixture.goal(
              root,
              scenario == 0
                  ? Map.of(
                      GoalDirectedPlannerScenariosTest.LIFE, 1L, "minion_skill_gem_level_+", 3L)
                  : Map.of("minion_skill_gem_level_+", 3L));
      try (var graph = fixture.graph(root, goal)) {
        int steps = 0, resumes = 0;
        while (!graph.finished() && steps < 500000) {
          if (graph.resourceLimited()) {
            System.out.println(
                "BUDGET_STAGE|scenario="
                    + (scenario + 6)
                    + "|resumes="
                    + resumes
                    + "|resources="
                    + graph.resources());
            if (!graph.canResume()) break;
            var saved = graph.checkpoint("budget-observation");
            // Identical doubling and ceilings to AutomaticSearchService.RESUME.
            graph.increaseLimits(
                Math.min(128, saved.states() * 2),
                Math.min(256, saved.methods() * 2),
                Math.min(50000, saved.retained() * 2),
                Math.min(10000, saved.primitive() * 2));
            resumes++;
          }
          graph.step();
          if (++steps % 100 == 0) graph.publishProgress();
        }
        graph.publishProgress();
        System.out.println(
            "BUDGET_FINAL|scenario="
                + (scenario + 6)
                + "|steps="
                + steps
                + "|resumes="
                + resumes
                + "|finished="
                + graph.finished()
                + "|complete="
                + graph.complete()
                + "|canResume="
                + graph.canResume()
                + "|limited="
                + graph.resourceLimited()
                + "|resources="
                + graph.resources()
                + "|goals="
                + graph.nodes().stream()
                    .filter(n -> n.progress().goal() == GoalFilter.Status.MATCH)
                    .count());
        assertThat(steps)
            .as("diagnostic must reach completion or a real product ceiling")
            .isLessThan(500000);
        assertThat(graph.finished() || (graph.resourceLimited() && !graph.canResume())).isTrue();
        for (var method : graph.methods())
          for (var point : method.points().values())
            assertThat(point.mass().contains(ExactNumericDistribution.Fraction.ONE)).isTrue();
      }
    }
  }
}
