package com.poe2craft.item.testparser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.bootstrap.AdminCredentials;
import com.poe2craft.bootstrap.SecurityConfiguration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ItemTextController.class)
@Import({
  ItemTextService.class,
  ItemTextErrors.class,
  SecurityConfiguration.class,
  AdminCredentials.class
})
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
  void exactByteLimitIsAcceptedAndOnlyExactPostRouteIsPublic() throws Exception {
    String prefix = "Item Class: Synthetic\nRarity: Normal\nBase\n--------\n";
    String text = prefix + "a".repeat(ItemTextService.MAX_TEXT_BYTES - prefix.length());
    mvc.perform(
            post("/api/v1/items/parse")
                .contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("text", text))))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/items/parse")).andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/items/other")).andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/admin/crawl-runs")).andExpect(status().isForbidden());
  }
}
