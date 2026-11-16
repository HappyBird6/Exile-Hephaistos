package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.search.domain.*;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.ItemState;
import java.util.*;
import org.junit.jupiter.api.Test;

class RagRealCatalogTest {
  @Test
  void actualAmuletGoalBuildsBoundedFactsWithoutChangingReferenceCandidates() throws Exception {
    var fixture = new GoalDirectedPlannerScenariosTest();
    var limits = BackendSettings.defaults().supportRag();
    for (var root :
        List.of(
            fixture.item(ItemState.Rarity.NORMAL, 82, List.of()),
            fixture.item(ItemState.Rarity.RARE, 82, List.of()),
            fixture.item(ItemState.Rarity.RARE, 82, List.of(fixture.life())))) {
      var goal = fixture.goal(root, Map.of("minion_skill_gem_level_+", 3L));
      var kernel = fixture.kernel(root, goal);
      var evaluate = fixture.evaluation(root, goal);
      var inventory = AutomaticCraftingAction.candidates(GoalDirectedPlannerScenariosTest.policy);
      var planner = new GoalDirectedPlanner(kernel, evaluate, inventory);
      var before = planner.choices(root, true);
      var facts =
          new StrategyEvidence(
                  kernel,
                  evaluate,
                  inventory,
                  limits.factOperations(),
                  System.nanoTime() + limits.computeTimeout().toNanos())
              .build(root, true, 0, inventory);
      assertThat(facts.actions()).isNotEmpty();
      var json = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
      var documents =
          com.poe2craft.craftingsupport.rag.infrastructure.BundledStrategyKnowledge.load(json)
              .select(
                  com.poe2craft.craftingsupport.rag.application.StrategyKnowledge.Mode.ALL, "", 3)
              .documents();
      assertThat(json.writeValueAsBytes(Map.of("facts", facts, "documents", documents)).length)
          .isLessThan(limits.maxPromptBytes() - 10000);
      assertThat(planner.choices(root, true)).isEqualTo(before);
    }
  }
}
