package com.poe2craft.craftingsupport.goal.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 목표 필터의 카탈로그 조회·검증·평가 요청을 처리한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code itemCatalog}: 아이템·카탈로그 ({@code ItemCatalog}).
 *   <li>{@code index}: 색인 ({@code GoalCatalogIndex}).
 *   <li>{@code mvc}: MVC검증기 ({@code MockMvc}).
 *   <li>{@code root}: 시작점; 초기값 {@code "/api/v1/crafting/support/goal-filters"} ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code numericStringsAreNotCoercedIntoAstNumbers()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *       사용하는 예외: {@code Exception}.
 *   <li>{@code knownOmenIsAValidUnsupportedCapabilityAndServiceFailureIsSanitized()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code goal(version, stat)}: 목표 처리에서 {@code itemCatalog.base}, {@code
 *       BigDecimal.valueOf}을(를) 사용한다. 입력: {@code version}(버전), {@code stat}(수치). 반환: {@code
 *       GoalFilter}.
 *   <li>{@code catalogValidateEvaluateAndPartialRecommendationUseContractWireShape()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code staleUnknownAndMalformedRequestsAreSanitizedProblemDetails()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code spoofedObservationsAndFractionalRollsAreNotAcceptedOrTruncated()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
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
      new GoalCatalogIndex(List.of(itemCatalog), GoalDefinitionsLoader.load());
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

  /** 직접 목표와 과거 Total이 함께 있으면 세 API 모두 활성 Total 행을 가리켜 거절한다. */
  @Test
  void retiredTotalBlocksTheWholeRequestAndDisabledTotalsRemainReadable() throws Exception {
    var item = SolarAmulet.initial(itemCatalog);
    var context = new Context(item.snapshotId(), item.baseItemId(), item.itemLevel());
    var current =
        goal(index.version(), GoalCatalogIndex.id("explicit", "base_cold_damage_resistance_%"));
    var old =
        goal(index.version(), GoalCatalogIndex.id("pseudo", "total_cold_resistance"))
            .groups()
            .getFirst()
            .entries()
            .getFirst();
    for (boolean disabled : List.of(false, true)) {
      var saved = new Entry("saved", old.statId(), old.unit(), old.range(), old.weight(), disabled);
      var group =
          new Group(
              "g",
              Type.AND,
              false,
              null,
              List.of(current.groups().getFirst().entries().getFirst(), saved));
      var mixed = new GoalFilter(1, current.catalogVersion(), current.general(), List.of(group));
      for (String path : List.of("validate", "evaluate", "recommend")) {
        var payload =
            switch (path) {
              case "validate" -> Map.of("context", context, "goal", mixed);
              case "evaluate" -> Map.of("item", item, "goal", mixed);
              default ->
                  Map.of(
                      "item",
                      item,
                      "goal",
                      mixed,
                      "activeOmens",
                      Set.of(),
                      "limits",
                      new GoalFilterService.Limits(100, 100, 100));
            };
        var call =
            mvc.perform(
                post(root + "/" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(payload)));
        if (disabled) call.andExpect(status().isOk());
        else
          call.andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.issues[0].code").value("RETIRED_TOTAL_GOAL"))
              .andExpect(jsonPath("$.issues[0].path").value("/goal/groups/0/entries/1/statId"));
      }
    }
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
        .andExpect(jsonPath("$.groupTypes.length()").value(6))
        .andExpect(jsonPath("$.stats[?(@.kind == 'PSEUDO')]").isEmpty());
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
                        new com.poe2craft.craftingsupport.goal.api.dto.RecommendRequest(
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
