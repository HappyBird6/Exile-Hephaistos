package com.poe2craft.craftingsupport.recommendation.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.bootstrap.CatalogConfiguration;
import com.poe2craft.bootstrap.http.CraftingErrors;
import com.poe2craft.craftingsupport.recommendation.domain.SupportGoals;
import com.poe2craft.shared.crafting.domain.StateBucket;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Keep the real HTTP filters; mock only their external Redis dependency.
/**
 * 제작 목표의 현재 달성 여부와 지원 범위를 평가한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code mvc}: MVC검증기 ({@code MockMvc}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code publishesCatalogFamiliesAndAssessesActualRoot()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code malformedStateUsesProblemDetailsInsteadOfAZeroProbability()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
@org.springframework.test.context.bean.override.mockito.MockitoBean(
    types = org.springframework.data.redis.core.StringRedisTemplate.class)
@WebMvcTest(SupportAssessmentController.class)
@Import({CatalogConfiguration.class, CraftingErrors.class})
class SupportAssessmentControllerTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired ItemCatalog catalog;

  @Test
  void publishesCatalogFamiliesAndAssessesActualRoot() throws Exception {
    mvc.perform(get("/api/v1/crafting/support/families"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(30));
    var body =
        Map.of(
            "state",
            StateBucket.from(SolarAmulet.initial(catalog)),
            "goal",
            new SupportGoals.Goal(
                List.of(new SupportGoals.Condition("IncreasedLife", 2)), List.of(), 0));
    mvc.perform(
            post("/api/v1/crafting/support/assess")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("READY"))
        .andExpect(jsonPath("$.achieved").value(false));
  }

  @Test
  void malformedStateUsesProblemDetailsInsteadOfAZeroProbability() throws Exception {
    mvc.perform(
            post("/api/v1/crafting/support/assess")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("INVALID_CRAFTING_REQUEST"));
  }
}
