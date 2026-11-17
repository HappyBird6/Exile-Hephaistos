package com.poe2craft.workbench.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.bootstrap.CatalogConfiguration;
import com.poe2craft.bootstrap.http.*;
import com.poe2craft.shared.crafting.api.VersionedCatalogController;
import com.poe2craft.shared.ids.*;
import com.poe2craft.shared.item.text.ItemTextService;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 실제 MVC 변환기와 HTTP 캐시 경계에서 긴 ID·짧은 ID 클라이언트가 같은 제작을 하는지 검사한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code mvc}: MVC검증기 ({@code MockMvc}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code ids}: 식별자목록 ({@code GameIdMappings}).
 *   <li>{@code codec}: 저장·전송 표현의 변환기 ({@code GameIdCodec}).
 * </ul>
 *
 * <p>rejectsMalformedOrMismatchedCompactBodies는 손상된 JSON·중복 키·다른 사전의 압축 본문이 400으로 거절되는지 검사한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code compactRequestCraftsAndLegacyRequestStaysReadable()}: 실제 MVC 경계에서 짧은 ID로 제작하고 원문 응답과
 *       같은 상태로 복원되는지 검증한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code representationSpecificEtagAndVaryPreventCrossFormatCacheReuse()}: 짧은 ID와 원문 응답이 서로
 *       다른 ETag를 사용해 잘못된 304 응답을 막는지 검증한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
@org.springframework.test.context.bean.override.mockito.MockitoBean(
    types = org.springframework.data.redis.core.StringRedisTemplate.class)
@WebMvcTest({WorkbenchController.class, VersionedCatalogController.class})
@Import({
  CatalogConfiguration.class,
  CraftingErrors.class,
  ItemTextService.class,
  GameIdTransport.class,
  com.poe2craft.shared.crafting.infrastructure.CatalogReferences.class
})
class GameIdTransportHttpTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  private final GameIdMappings ids = GameIdMappings.active();
  private final GameIdCodec codec = new GameIdCodec(ids);

  @Test
  void compactRequestCraftsAndLegacyRequestStaysReadable() throws Exception {
    String url = "/api/v1/crafting/workbench/initial?base=stocky";
    var plain =
        json.readTree(
            mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    var compact =
        json.readTree(
            mvc.perform(get(url).header(GameIdMappings.HEADER, ids.identity()))
                .andExpect(status().isOk())
                .andExpect(header().string(GameIdMappings.HEADER, ids.identity()))
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(codec.decode(compact)).isEqualTo(plain);
    var state = plain.get("state").deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) state).putArray("explicits");
    var body =
        json.valueToTree(
            Map.of("state", state, "action", "TRANSMUTATION", "activeOmens", java.util.List.of()));
    var result =
        mvc.perform(
                post("/api/v1/crafting/workbench/apply")
                    .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                    .header(GameIdMappings.HEADER, ids.identity())
                    .header(GameIdTransport.BODY_HEADER, "compact")
                    .contentType("application/json")
                    .content(json.writeValueAsBytes(codec.encode(body))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(codec.decode(json.readTree(result.getContentAsString())).path("applied").asBoolean())
        .isTrue();
  }

  @Test
  void representationSpecificEtagAndVaryPreventCrossFormatCacheReuse() throws Exception {
    String url = "/api/v1/crafting/catalogs/" + RulesetBoundary.identity() + "/stocky";
    var legacy = mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse();
    var compact =
        mvc.perform(get(url).header(GameIdMappings.HEADER, ids.identity()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(compact.getHeader("ETag")).isNotEqualTo(legacy.getHeader("ETag"));
    assertThat(compact.getHeaders("Vary")).anyMatch(v -> v.contains(GameIdMappings.HEADER));
    mvc.perform(
            get(url)
                .header(GameIdMappings.HEADER, ids.identity())
                .header("If-None-Match", compact.getHeader("ETag")))
        .andExpect(status().isNotModified());
    mvc.perform(
            get(url)
                .header(GameIdMappings.HEADER, ids.identity())
                .header("If-None-Match", legacy.getHeader("ETag")))
        .andExpect(status().isOk());
  }

  @Test
  void rejectsMalformedOrMismatchedCompactBodies() throws Exception {
    for (String body : java.util.List.of("{", "{\"action\":\"c0001\",\"action\":\"c0002\"}"))
      mvc.perform(
              post("/api/v1/crafting/workbench/apply")
                  .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                  .header(GameIdMappings.HEADER, ids.identity())
                  .header(GameIdTransport.BODY_HEADER, "compact")
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .header(GameIdMappings.HEADER, "different")
                .header(GameIdTransport.BODY_HEADER, "compact")
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest());
  }
}
