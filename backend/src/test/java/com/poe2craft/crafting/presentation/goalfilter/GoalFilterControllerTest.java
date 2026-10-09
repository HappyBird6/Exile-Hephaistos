package com.poe2craft.crafting.presentation.goalfilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.goalfilter.GoalFilterService;
import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GoalFilterControllerTest {
  @Test
  void numericStringsAreNotCoercedIntoAstNumbers() throws Exception {
    var item = SolarAmulet.initial(itemCatalog);
    var body =
        json.valueToTree(
            Map.of(
                "item",
                item,
                "goal",
                goal(
                    index.version(),
                    GoalCatalogIndex.id("explicit", "base_cold_damage_resistance_%"))));
    ((com.fasterxml.jackson.databind.node.ObjectNode)
            body.path("goal").path("groups").get(0).path("entries").get(0).path("range"))
        .put("min", "1");
    mvc.perform(
            post(root + "/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void knownOmenIsAValidUnsupportedCapabilityAndServiceFailureIsSanitized() throws Exception {
    var item = SolarAmulet.initial(itemCatalog);
    var goal =
        goal(index.version(), GoalCatalogIndex.id("explicit", "base_cold_damage_resistance_%"));
    mvc.perform(
            post(root + "/recommend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "item",
                            item,
                            "goal",
                            goal,
                            "activeOmens",
                            List.of("Omen_of_Sinistral_Exaltation"),
                            "limits",
                            Map.of("maxStates", 100, "maxEdges", 100, "maxMillis", 100)))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.probability.status").value("UNSUPPORTED"));
    var failed = org.mockito.Mockito.mock(GoalFilterService.class);
    org.mockito.Mockito.when(failed.catalog(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new IllegalStateException("private SQL and item contents"));
    var isolated =
        MockMvcBuilders.standaloneSetup(new GoalFilterController(failed, json))
            .setControllerAdvice(new GoalFilterErrors())
            .build();
    isolated
        .perform(
            get(root + "/catalog")
                .param("snapshotId", item.snapshotId())
                .param("baseItemId", item.baseItemId())
                .param("itemLevel", "82"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.detail").value("The goal filter service is unavailable."));
  }

  final ObjectMapper json = new ObjectMapper();
  final ItemCatalog itemCatalog = ItemCatalogLoader.loadDefault();
  final GoalCatalogIndex index =
      new GoalCatalogIndex(
          List.of(itemCatalog),
          com.poe2craft.crafting.infrastructure.goalfilter.GoalDefinitionsLoader.load());
  final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new GoalFilterController(new GoalFilterService(index), json))
          .setControllerAdvice(new GoalFilterErrors())
          .build();
  final String root = "/api/v1/crafting/support/goal-filters";

  GoalFilter goal(String version, String stat) {
    return new GoalFilter(
        1,
        version,
        new General(
            itemCatalog.base().id(),
            new Range(BigDecimal.ONE, BigDecimal.valueOf(100)),
            List.of(ItemState.Rarity.NORMAL)),
        List.of(
            new Group(
                "g",
                Type.AND,
                false,
                null,
                List.of(
                    new Entry(
                        "r", stat, "percent", new Range(BigDecimal.ONE, null), null, false)))));
  }

  @Test
  void catalogValidateEvaluateAndPartialRecommendationUseContractWireShape() throws Exception {
    var item = SolarAmulet.initial(itemCatalog);
    var goal =
        goal(index.version(), GoalCatalogIndex.id("explicit", "base_cold_damage_resistance_%"));
    mvc.perform(
            get(root + "/catalog")
                .param("snapshotId", item.snapshotId())
                .param("baseItemId", item.baseItemId())
                .param("itemLevel", "82"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.groupTypes.length()").value(6));
    var context = new Context(item.snapshotId(), item.baseItemId(), item.itemLevel());
    mvc.perform(
            post(root + "/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("context", context, "goal", goal))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true))
        .andExpect(jsonPath("$.capabilities.evaluation").value("SUPPORTED"))
        .andExpect(jsonPath("$.capabilities.probability").value("SUPPORTED"));
    mvc.perform(
            post(root + "/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("item", item, "goal", goal))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("NO_MATCH"))
        .andExpect(jsonPath("$.groups[0].entries[0].presence").value("ABSENT"))
        .andExpect(jsonPath("$.groups[0].entries[0].value").isEmpty());
    mvc.perform(
            post(root + "/recommend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        new GoalFilterController.RecommendRequest(
                            item, goal, Set.of(), new GoalFilterService.Limits(100, 100, 100)))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.probability.status").value("PARTIAL"))
        .andExpect(jsonPath("$.probability.reasonCode").value("BUDGET_EXHAUSTED"))
        .andExpect(jsonPath("$.comparisons").isNotEmpty())
        .andExpect(jsonPath("$.rankingCertified").value(false))
        .andExpect(jsonPath("$.totalSequences").isNumber());
  }

  @Test
  void staleUnknownAndMalformedRequestsAreSanitizedProblemDetails() throws Exception {
    var item = SolarAmulet.initial(itemCatalog);
    mvc.perform(
            post(root + "/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(Map.of("item", item, "goal", goal("old", "missing")))))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("CATALOG_VERSION_MISMATCH"));
    mvc.perform(
            post(root + "/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of("item", item, "goal", goal(index.version(), "missing")))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.issues[0].code").value("UNKNOWN_STAT"));
    mvc.perform(
            post(root + "/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{broken secret-input"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("The goal filter request could not be read."));
    var body =
        json.valueToTree(
            Map.of(
                "item",
                item,
                "goal",
                goal(
                    index.version(),
                    GoalCatalogIndex.id("explicit", "base_cold_damage_resistance_%"))));
    ((com.fasterxml.jackson.databind.node.ObjectNode) body.path("goal").path("groups").get(0))
        .put("type", "BAD_ENUM");
    mvc.perform(
            post(root + "/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void spoofedObservationsAndFractionalRollsAreNotAcceptedOrTruncated() throws Exception {
    var item = SolarAmulet.initial(itemCatalog);
    var goal =
        goal(index.version(), GoalCatalogIndex.id("explicit", "base_cold_damage_resistance_%"));
    mvc.perform(
            post(root + "/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "item",
                            item,
                            "goal",
                            goal,
                            "observedStats",
                            Map.of(goal.groups().getFirst().entries().getFirst().statId(), 100)))))
        .andExpect(status().isBadRequest());
    var body = json.valueToTree(Map.of("item", item, "goal", goal));
    ((com.fasterxml.jackson.databind.node.ObjectNode) body.path("item")).put("itemLevel", 82.5);
    mvc.perform(
            post(root + "/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest());
    ((com.fasterxml.jackson.databind.node.ObjectNode) body.path("item")).put("itemLevel", 0);
    mvc.perform(
            post(root + "/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isUnprocessableEntity());
  }
}
