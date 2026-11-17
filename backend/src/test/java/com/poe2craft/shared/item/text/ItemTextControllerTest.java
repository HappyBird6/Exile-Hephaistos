package com.poe2craft.shared.item.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

// Keep the real HTTP filters; mock only their external Redis dependency.
/**
 * 아이템 복사 텍스트를 표시용 구조로 분석하는 HTTP API다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code mvc}: MVC검증기 ({@code MockMvc}).
 *   <li>{@code mapper}: 입력·출력 표현을 변환하는 구성요소 ({@code ObjectMapper}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code exposesStructuredModifierMetadataAndRejectsKoreanHeaders()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code anonymousParsePreservesTextWithoutCreatingSession(locale)}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다. 입력: {@code locale}(언어코드). 사용하는 예외: {@code Exception}.
 *   <li>{@code rejectsInvalidText(body)}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 입력: {@code
 *       body}(본문). 사용하는 예외: {@code Exception}.
 *   <li>{@code rejectsMalformedJson(body)}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 입력: {@code
 *       body}(본문). 사용하는 예외: {@code Exception}.
 *   <li>{@code byteLimitCountsMultibyteKorean()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외:
 *       {@code Exception}.
 *   <li>{@code exactByteLimitIsAcceptedAndUnknownRoutesAreAbsent()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
@org.springframework.test.context.bean.override.mockito.MockitoBean(
    types = org.springframework.data.redis.core.StringRedisTemplate.class)
@WebMvcTest(ItemTextController.class)
@Import({ItemTextService.class, ItemTextErrors.class})
class ItemTextControllerTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;

  @Test
  void exposesStructuredModifierMetadataAndRejectsKoreanHeaders() throws Exception {
    String text =
        "Rarity: Rare\nSynthetic\nSynthetic Base\n--------\nItem Level: 42\n--------\n{ Prefix Modifier \"Synthetic\" (Tier: 1) — Test }\n+7 to Synthetic Value";
    mvc.perform(
            post("/api/v1/items/parse")
                .contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("text", text))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.modifiers[0].type").value("EXPLICIT"))
        .andExpect(jsonPath("$.modifiers[0].affix").value("PREFIX"))
        .andExpect(jsonPath("$.modifiers[0].tier").value(1))
        .andExpect(jsonPath("$.modifiers[0].metadata.number").value(7));
    mvc.perform(
            post("/api/v1/items/parse")
                .contentType("application/json")
                .content(
                    mapper.writeValueAsString(Map.of("text", "아이템 종류: 테스트\n아이템 희귀도: 일반\n테스트"))))
        .andExpect(status().isUnprocessableEntity());
  }

  @ParameterizedTest
  @ValueSource(strings = {"en"})
  void anonymousParsePreservesTextWithoutCreatingSession(String locale) throws Exception {
    String text =
        locale.equals("en")
            ? "Item Class: Synthetic\nRarity: Normal\nSynthetic Base\n--------\nItem Level: 12\nunknown"
            : "아이템 종류: 가상\n아이템 희귀도: 일반\n가상 베이스\n--------\n아이템 레벨: 12\n미해석";
    var result =
        mvc.perform(
                post("/api/v1/items/parse")
                    .contentType("application/json")
                    .content(mapper.writeValueAsString(Map.of("text", text))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locale").value(locale))
            .andExpect(jsonPath("$.itemLevel").value(12))
            .andExpect(jsonPath("$.text.originalText").value(text))
            .andExpect(jsonPath("$.unparsedLines.length()").value(1))
            .andExpect(jsonPath("$.warnings[1].code").value("CATALOG_VALIDATION_REQUIRED"))
            .andReturn();
    assertThat(result.getRequest().getSession(false)).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"text\":null}", "{\"text\":\"\"}", "{\"text\":\"invalid\"}"})
  void rejectsInvalidText(String body) throws Exception {
    mvc.perform(post("/api/v1/items/parse").contentType("application/json").content(body))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("INVALID_ITEM_TEXT"))
        .andExpect(jsonPath("$.traceId").isString())
        .andExpect(jsonPath("$.instance").value("/api/v1/items/parse"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{",
        "[]",
        "null",
        "{\"text\":{}}",
        "{\"text\":123}",
        "{\"text\":true}",
        "{\"text\":[]}"
      })
  void rejectsMalformedJson(String body) throws Exception {
    mvc.perform(post("/api/v1/items/parse").contentType("application/json").content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
  }

  @Test
  void byteLimitCountsMultibyteKorean() throws Exception {
    mvc.perform(
            post("/api/v1/items/parse")
                .contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("text", "가".repeat(5462)))))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.code").value("ITEM_TEXT_TOO_LARGE"));
  }

  @Test
  void exactByteLimitIsAcceptedAndUnknownRoutesAreAbsent() throws Exception {
    String prefix = "Item Class: Synthetic\nRarity: Normal\nBase\n--------\n";
    String text = prefix + "a".repeat(ItemTextService.MAX_TEXT_BYTES - prefix.length());
    mvc.perform(
            post("/api/v1/items/parse")
                .contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("text", text))))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/items/parse")).andExpect(status().isMethodNotAllowed());
    mvc.perform(post("/api/v1/items/other")).andExpect(status().isNotFound());
    mvc.perform(post("/api/v1/admin/crawl-runs")).andExpect(status().isNotFound());
  }
}
