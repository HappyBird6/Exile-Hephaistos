package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.goal.api.GoalFilterController;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.rag.api.RagComparisonController;
import com.poe2craft.craftingsupport.rag.application.RagComparisonService;
import com.poe2craft.craftingsupport.rag.application.StrategyKnowledge;
import com.poe2craft.craftingsupport.rag.application.StrategyModel;
import com.poe2craft.craftingsupport.rag.infrastructure.BundledStrategyKnowledge;
import com.poe2craft.craftingsupport.search.application.MethodSearchProtocol;
import com.poe2craft.craftingsupport.search.application.MethodSearchService;
import com.poe2craft.craftingsupport.search.domain.*;
import com.poe2craft.craftingsupport.search.infrastructure.BundledSearchRuntimeFactory;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.ItemState;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RagRealCatalogTest {
  @Test
  void browserShapedNormalSolar82ProjectileThreePassesValidationAndPromptEndpoints()
      throws Exception {
    var fixture = new GoalDirectedPlannerScenariosTest();
    var json = new ObjectMapper().findAndRegisterModules();
    var root = fixture.item(ItemState.Rarity.NORMAL, 82, List.of());
    var goal = fixture.goal(root, Map.of("projectile_skill_gem_level_+", 3L));
    var workbench = GoalDirectedPlannerScenariosTest.workbench;
    var goals = GoalDirectedPlannerScenariosTest.goals;
    var calls = new AtomicInteger();
    try (var legacy =
        new MethodSearchService(workbench.reviewedCatalogs(), goals, "rag-api-test", json)) {
      var input =
          new RagComparisonService.Input(
              new MethodSearchProtocol.Create(
                  2,
                  UUID.randomUUID().toString(),
                  new BasicCurrencyState(root, legacy.provenance(root.baseItemId())),
                  goal,
                  List.of()),
              true,
              0,
              StrategyKnowledge.Mode.RETRIEVED,
              "ko");
      var provider =
          new StrategyModel() {
            public boolean configured() {
              return true;
            }

            public Result propose(
                StrategyKnowledge.Selection docs,
                StrategyEvidence.Facts facts,
                GoalFilter g,
                String locale) {
              calls.incrementAndGet();
              return new Result("INSUFFICIENT_EVIDENCE", List.of(), 1, 1);
            }
          };
      var limits = (ObjectNode) json.valueToTree(BackendSettings.defaults().supportRag());
      limits.put("enabled", true);
      var service =
          new RagComparisonService(
              new BundledSearchRuntimeFactory(
                  workbench, goals, legacy, json, GoalDirectedPlannerScenariosTest.policy),
              GoalDirectedPlannerScenariosTest.policy,
              BundledStrategyKnowledge.load(json),
              provider,
              json.treeToValue(limits, BackendSettings.SupportRag.class),
              json,
              Clock.systemUTC());
      var mvc =
          MockMvcBuilders.standaloneSetup(
                  new RagComparisonController(service, json), new GoalFilterController(goals, json))
              .build();
      var body = (ObjectNode) json.valueToTree(input);
      var item = (ObjectNode) body.path("request").path("start").path("item");
      item.remove(
          List.of(
              "qualityInfused",
              "installedAugments",
              "equipmentQuality",
              "sanctification",
              "observations"));
      item.path("implicits").forEach(m -> ((ObjectNode) m).remove("crafted"));
      var context = new GoalFilter.Context(root.snapshotId(), root.baseItemId(), 82);
      mvc.perform(
              post("/api/v1/crafting/support/goal-filters/validate")
                  .contentType("application/json")
                  .content(json.writeValueAsString(Map.of("context", context, "goal", goal))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.valid").value(true));
      var result =
          mvc.perform(
                  post("/api/support-test/rag/prompt")
                      .contentType("application/json")
                      .content(json.writeValueAsString(body)))
              .andExpect(status().isOk())
              .andExpect(header().string("Cache-Control", "no-store"))
              .andReturn();
      assertThat(calls).hasValue(0);
      var preview = json.readTree(result.getResponse().getContentAsString());
      var promptBody = json.readTree(preview.path("prompt").path("user").asText());
      assertThat(promptBody.path("goal")).isEqualTo(json.readTree(json.writeValueAsString(goal)));
      assertThat(promptBody.path("documents")).isNotEmpty();
      mvc.perform(
              post("/api/support-test/rag/generate")
                  .contentType("application/json")
                  .content(
                      json.writeValueAsString(
                          Map.of("promptId", preview.path("promptId").asText()))))
          .andExpect(status().isOk());
      assertThat(calls).hasValue(1);
    }
  }

  @Test
  void actualAmuletGoalBuildsBoundedFactsWithoutChangingReferenceCandidates() throws Exception {
    var fixture = new GoalDirectedPlannerScenariosTest();
    var limits = BackendSettings.defaults().supportRag();
    for (var root :
        List.of(
            fixture.item(ItemState.Rarity.NORMAL, 82, List.of()),
            fixture.item(ItemState.Rarity.RARE, 82, List.of()),
            fixture.item(ItemState.Rarity.RARE, 82, List.of(fixture.life())))) {
      for (var stat : List.of("minion_skill_gem_level_+", "projectile_skill_gem_level_+")) {
        var goal = fixture.goal(root, Map.of(stat, 3L));
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
        var json = new ObjectMapper().findAndRegisterModules();
        var documents =
            BundledStrategyKnowledge.load(json)
                .select(StrategyKnowledge.Mode.ALL, "", 3)
                .documents();
        assertThat(json.writeValueAsBytes(Map.of("facts", facts, "documents", documents)).length)
            .isLessThan(limits.maxPromptBytes() - 10000);
        assertThat(planner.choices(root, true)).isEqualTo(before);
      }
    }
  }
}
