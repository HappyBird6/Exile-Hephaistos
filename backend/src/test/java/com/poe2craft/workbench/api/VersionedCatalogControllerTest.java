package com.poe2craft.workbench.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.bootstrap.CatalogConfiguration;
import com.poe2craft.bootstrap.http.CraftingErrors;
import com.poe2craft.shared.crafting.api.VersionedCatalogController;
import com.poe2craft.shared.item.text.ItemTextService;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

// Keep the real HTTP filters; mock only their external Redis dependency.
/**
 * 규칙 버전별 불변 카탈로그를 응답하고 전송 형식별 HTTP 캐시를 구분한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code mvc}: MVC검증기 ({@code MockMvc}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code references}: 참조목록 ({@code
 *       com.poe2craft.shared.crafting.infrastructure.CatalogReferences}).
 *   <li>{@code crafting}: 제작 ({@code
 *       com.poe2craft.shared.crafting.application.CraftingOperations}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code identicalBasesShareReferencesAndAnotherReplicaCanResolveThem()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code sharedReferenceIsOptInSoExistingClientsKeepTheirExpectedBaseUrl()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code separateReferenceReconstructsTheOriginalEnvelopeIncludingManualReveal()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code obsoleteVersionCannotBeCachedAsCurrentData()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *       사용하는 예외: {@code Exception}.
 *   <li>{@code cachedReferenceCanBeRevalidatedWithoutRetransmittingDefinitions()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
@org.springframework.test.context.bean.override.mockito.MockitoBean(
    types = org.springframework.data.redis.core.StringRedisTemplate.class)
@WebMvcTest({WorkbenchController.class, VersionedCatalogController.class})
@Import({
  CatalogConfiguration.class,
  CraftingErrors.class,
  ItemTextService.class,
  com.poe2craft.shared.crafting.infrastructure.CatalogReferences.class
})
class VersionedCatalogControllerTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @Autowired com.poe2craft.shared.crafting.infrastructure.CatalogReferences references;
  @Autowired com.poe2craft.shared.crafting.application.CraftingOperations crafting;

  @Test
  void identicalBasesShareReferencesAndAnotherReplicaCanResolveThem() throws Exception {
    var hash = references.reference("hallowed", true);
    assertThat(references.reference("stoic", true)).isEqualTo(hash);
    assertThat(references.reference("solar", true)).isNotEqualTo(hash);
    var replica =
        new com.poe2craft.shared.crafting.infrastructure.CatalogReferences(crafting, json);
    assertThat(replica.find(hash)).isEqualTo(references.find(hash));
    assertThat(replica.find("invalid-hash")).isNull();
    String url = "/api/v1/crafting/catalogs/" + RulesetBoundary.identity() + "/shared/" + hash;
    String etag =
        mvc.perform(get(url))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader("ETag");
    mvc.perform(get(url).header("If-None-Match", etag)).andExpect(status().isNotModified());
    mvc.perform(get("/api/v1/crafting/catalogs/obsolete/shared/" + hash))
        .andExpect(status().isConflict());
  }

  @Test
  void sharedReferenceIsOptInSoExistingClientsKeepTheirExpectedBaseUrl() throws Exception {
    String initial = "/api/v1/crafting/workbench/initial?base=hallowed&manualReveal=true&catalog=";
    var legacy =
        json.readTree(
            mvc.perform(get(initial + "separate"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    var shared =
        json.readTree(
            mvc.perform(get(initial + "shared"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(legacy.path("catalogUrl").asText()).endsWith("/hallowed?manualReveal=true");
    assertThat(shared.path("catalogUrl").asText()).contains("/shared/");
    assertThat(shared.has("modifiers")).isFalse();
  }

  @Test
  void separateReferenceReconstructsTheOriginalEnvelopeIncludingManualReveal() throws Exception {
    for (boolean manual : new boolean[] {false, true}) {
      String url =
          "/api/v1/crafting/workbench/initial?base=solar&itemLevel=100&manualReveal=" + manual;
      var original =
          json.readTree(
              mvc.perform(get(url))
                  .andExpect(status().isOk())
                  .andReturn()
                  .getResponse()
                  .getContentAsString());
      var thin =
          (ObjectNode)
              json.readTree(
                  mvc.perform(get(url + "&catalog=separate"))
                      .andExpect(status().isOk())
                      .andExpect(header().string("Cache-Control", "no-cache"))
                      .andReturn()
                      .getResponse()
                      .getContentAsString());
      assertThat(thin.has("modifiers")).isFalse();
      var reference =
          json.readTree(
              mvc.perform(get(thin.path("catalogUrl").asText()))
                  .andExpect(status().isOk())
                  .andExpect(
                      header().string("Cache-Control", "max-age=31536000, public, immutable"))
                  .andReturn()
                  .getResponse()
                  .getContentAsString());
      assertThat(reference.get("rulesetIdentity")).isEqualTo(thin.get("rulesetIdentity"));
      thin.remove("catalogUrl");
      thin.set("modifiers", reference.get("modifiers"));
      assertThat(thin).isEqualTo(original);
    }
  }

  @Test
  void obsoleteVersionCannotBeCachedAsCurrentData() throws Exception {
    mvc.perform(get("/api/v1/crafting/catalogs/obsolete/solar"))
        .andExpect(status().isConflict())
        .andExpect(header().string("Cache-Control", "no-store"));
  }

  @Test
  void cachedReferenceCanBeRevalidatedWithoutRetransmittingDefinitions() throws Exception {
    String url = "/api/v1/crafting/catalogs/" + RulesetBoundary.identity() + "/solar";
    String etag =
        mvc.perform(get(url))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader("ETag");
    mvc.perform(get(url).header("If-None-Match", etag))
        .andExpect(status().isNotModified())
        .andExpect(content().string(""));
  }
}
