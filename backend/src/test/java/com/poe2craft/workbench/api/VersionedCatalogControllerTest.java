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
