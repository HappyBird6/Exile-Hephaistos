package com.poe2craft.crafting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.bootstrap.CraftingConfiguration;
import com.poe2craft.crafting.application.BasicPathService;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.presentation.*;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BasicPathController.class)
@Import({CraftingConfiguration.class, CraftingErrors.class})
class BasicPathControllerTest {
  @Test
  void rejectsUnsupportedRequestFieldsInsteadOfDiscardingThem() throws Exception {
    var state = new BasicCurrencyState(SolarAmulet.initial(catalog), service.provenance());
    var request =
        new BasicPathService.Request(
            state,
            new BasicPathService.Policy(
                List.of(WorkbenchCurrency.TRANSMUTATION), BasicPathService.PolicyMode.SINGLE_PASS),
            new BasicPathService.Target(state, Set.of()),
            Set.of(),
            List.of(1L));
    var body = mapper.valueToTree(request);
    ((com.fasterxml.jackson.databind.node.ObjectNode) body.get("start").get("item"))
        .put("unsupportedProperty", "preserve-me");
    mvc.perform(
            post("/api/v1/crafting/basic-paths/first-hit")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(body)))
        .andExpect(status().isUnprocessableEntity());
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired ItemCatalog catalog;
  @Autowired BasicPathService service;

  @Test
  void rejectsOldSeasonAndMissingRulesetBeforeCalculation() throws Exception {
    for (String endpoint : List.of("first-hit", "recovery")) {
      mvc.perform(
              post("/api/v1/crafting/basic-paths/" + endpoint)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.code").value("RULESET_IDENTITY_REQUIRED"));
      mvc.perform(
              post("/api/v1/crafting/basic-paths/" + endpoint)
                  .header(RulesetBoundary.HEADER, "old-season")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.code").value("RULESET_IDENTITY_MISMATCH"));
    }
  }

  @Test
  void explicitCheckpointAlreadyReachedProducesLosslessFractionsAndSeparateRecovery()
      throws Exception {
    var state = new BasicCurrencyState(SolarAmulet.initial(catalog), service.provenance());
    var request =
        new BasicPathService.Request(
            state,
            new BasicPathService.Policy(
                List.of(WorkbenchCurrency.TRANSMUTATION), BasicPathService.PolicyMode.REPEAT_CYCLE),
            new BasicPathService.Target(state, Set.of()),
            Set.of(),
            List.of(0L, 100L, 300L, 500L));
    mvc.perform(get("/api/v1/crafting/basic-paths/provenance"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rulesetIdentity").value(RulesetBoundary.identity()));
    mvc.perform(
            post("/api/v1/crafting/basic-paths/recovery")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.purpose").value("CONDITIONAL_RECOVERY"))
        .andExpect(jsonPath("$.points[3].lower.numerator").value("1"))
        .andExpect(jsonPath("$.points[3].unresolved.numerator").value("0"))
        .andExpect(jsonPath("$.recoveryIncludedInMain").value(false));
  }
}
