package com.poe2craft.craftingsupport.goal.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
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
        MockMvcBuilders.standaloneSetup(new GoalFilterController(failed, json, workbench))
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
  final CraftingOperations workbench = org.mockito.Mockito.mock(CraftingOperations.class);
  final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(
              new GoalFilterController(new GoalFilterService(index), json, workbench))
          .setControllerAdvice(new GoalFilterErrors())
          .build();
  final String root = "/api/v1/crafting/support/goal-filters";

  /** 실제 카탈로그 목표로 미리보기 HTTP 응답에 카드와 고정 옵션 획득처·정의가 함께 들어가는지 검증한다. */
  @Test
  void previewReturnsCardsWithSourcesAndDefinitions() throws Exception {
    var state = SolarAmulet.initial(itemCatalog);
    var simulator = new WorkbenchSimulator(itemCatalog, new CraftingEngine(itemCatalog));
    org.mockito.Mockito.when(workbench.simulatorForBase(state.baseItemId())).thenReturn(simulator);
    var context = new Context(state.snapshotId(), state.baseItemId(), state.itemLevel());
    var initialGoal =
        goal(index.version(), GoalCatalogIndex.id("explicit", "base_cold_damage_resistance_%"));
    var requestGoal =
        new GoalFilter(
            1,
            index.version(),
            new General(
                state.baseItemId(),
                initialGoal.general().itemLevel(),
                List.of(ItemState.Rarity.RARE)),
            List.of(
                new Group(
                    "count",
                    Type.COUNT,
                    false,
                    new Range(new BigDecimal("0.5"), BigDecimal.ZERO),
                    initialGoal.groups().getFirst().entries())));
    var implicit = itemCatalog.base().implicitModifierId();
    mvc.perform(
            post(root + "/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("context", context, "goal", requestGoal))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resolvedCountRanges.count.min").value(1))
        .andExpect(jsonPath("$.resolvedCountRanges.count.max").isEmpty())
        .andExpect(jsonPath("$.cards[0].item.rarity").value("RARE"))
        .andExpect(jsonPath("$.cards[0].tierVariants[0].rarity").value("RARE"))
        .andExpect(jsonPath("$.cards[0].tierVariants[1].explicits[0].modifierId").isString())
        .andExpect(jsonPath("$.cards[0].optionTiers['" + implicit + "'][0]").value(implicit))
        .andExpect(jsonPath("$.cards[0].sources['" + implicit + "'][0].kind").value("IMPLICIT"))
        .andExpect(jsonPath("$.modifiers['" + implicit + "'].id").value(implicit));
    var withQuality =
        new GoalFilter(
            1,
            index.version(),
            new General(
                state.baseItemId(),
                requestGoal.general().itemLevel(),
                List.of(ItemState.Rarity.RARE),
                40),
            List.of(
                new Group(
                    "count",
                    Type.COUNT,
                    false,
                    new Range(new BigDecimal("0.5"), BigDecimal.valueOf(5)),
                    initialGoal.groups().getFirst().entries())));
    mvc.perform(
            post(root + "/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("context", context, "goal", withQuality))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resolvedCountRanges.count.min").value(1))
        .andExpect(jsonPath("$.resolvedCountRanges.count.max").value(5))
        .andExpect(jsonPath("$.cards").isEmpty())
        .andExpect(jsonPath("$.reasonCode").value("QUALITY_PROJECTION_NOT_IMPLEMENTED"));
  }

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
