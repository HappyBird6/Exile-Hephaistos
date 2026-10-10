package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.crafting.presentation.RulesetBoundary;
import com.poe2craft.crafting.presentation.pathsearch.*;
import jakarta.servlet.http.Cookie;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PathSearchHttpTest {
  @Test
  void strictInputRulesetAndDeletedReferencesKeepOriginalInput() throws Exception {
    var fixture = new PathSearchServiceTest();
    var json = fixture.json;
    var identity = fixture.identity;
    try (var service = fixture.service(1)) {
      var mvc =
          MockMvcBuilders.standaloneSetup(new PathSearchController(service, json))
              .setControllerAdvice(new PathSearchErrors())
              .addFilters(new RulesetBoundary(json))
              .build();
      var request = fixture.request(service);
      var body = (ObjectNode) json.valueToTree(request);
      var endpoint = "/api/v1/crafting/path-searches";
      var source =
          json.readTree(
              java.nio.file.Path.of("../contracts/crafting-paths-v1/solar-source-fixture.json")
                  .toFile());
      var originalSnapshot = body.deepCopy();
      ((ObjectNode) originalSnapshot.path("start")).set("item", source.get("startItem"));
      mvc.perform(
              post(endpoint)
                  .header(RulesetBoundary.HEADER, identity)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(originalSnapshot.toString()))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.code").value("INVALID_ITEM"));
      mvc.perform(post(endpoint).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.code").value("RULESET_IDENTITY_REQUIRED"))
          .andExpect(jsonPath("$.detail").doesNotExist());
      var invalid = new ArrayList<ObjectNode>();
      invalid.add(body.deepCopy().put("policy", "CHAOS"));
      var numeric = body.deepCopy();
      numeric.putArray("observations").add(100);
      invalid.add(numeric);
      var missing = body.deepCopy();
      missing.remove("activeOmens");
      invalid.add(missing);
      var floating = body.deepCopy();
      ((ObjectNode) floating.path("start").path("item")).put("itemLevel", 82.5);
      invalid.add(floating);
      var string = body.deepCopy();
      ((ObjectNode) string.path("start").path("item")).put("itemLevel", "82");
      invalid.add(string);
      var unsafe = body.deepCopy();
      ((ObjectNode) unsafe.path("start").path("item").path("explicits").get(0).path("values"))
          .put("base_cold_damage_resistance_%", 9007199254740992L);
      invalid.add(unsafe);
      for (var payload : invalid)
        mvc.perform(
                post(endpoint)
                    .header(RulesetBoundary.HEADER, identity)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload.toString()))
            .andExpect(status().isUnprocessableEntity());
      mvc.perform(
              post(endpoint)
                  .header(RulesetBoundary.HEADER, identity)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body.toString().replace("\"version\":1", "\"version\":1,\"version\":1")))
          .andExpect(status().isUnprocessableEntity());
      var deleted = body.deepCopy();
      ((ObjectNode) deleted.path("goal").path("groups").get(0).path("entries").get(0))
          .put("statId", "deleted-stat");
      mvc.perform(
              post(endpoint)
                  .header(RulesetBoundary.HEADER, identity)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(deleted.toString()))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.code").value("UNKNOWN_STAT"));
      var stale = body.deepCopy();
      ((ObjectNode) stale.get("goal")).put("catalogVersion", "stale");
      mvc.perform(
              post(endpoint)
                  .header(RulesetBoundary.HEADER, identity)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(stale.toString()))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("CATALOG_VERSION_MISMATCH"));
      var accepted =
          mvc.perform(
                  post(endpoint)
                      .header(RulesetBoundary.HEADER, identity)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(body.toString()))
              .andExpect(status().isAccepted())
              .andReturn()
              .getResponse();
      assertThat(accepted.getHeader("Set-Cookie")).contains("HttpOnly", "SameSite=Strict");
      String cookie = accepted.getHeader("Set-Cookie").split("[=;]")[1];
      String id = json.readTree(accepted.getContentAsString()).get("jobId").asText();
      mvc.perform(get(endpoint + "/" + id).cookie(new Cookie("crafting_path_client", cookie)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("QUEUED"));
      mvc.perform(get(endpoint + "/" + id)).andExpect(status().isGone());
      assertThat((com.fasterxml.jackson.databind.JsonNode) json.valueToTree(request))
          .isEqualTo(body);
    }
  }
}
