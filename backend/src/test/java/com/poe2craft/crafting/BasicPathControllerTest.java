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
  void solarRenewalHttpReturnsExactFractionsAndQualityEvidence() throws Exception {
    var item = SolarAmulet.initial(catalog);
    var eligible =
        catalog.modifiers().values().stream()
            .filter(
                m ->
                    m.layer() == ModifierDefinition.Layer.EXPLICIT
                        && m.weight() > 0
                        && m.requiredItemLevel() <= item.itemLevel()
                        && m.stats().size() == 1)
            .toList();
    var root = eligible.getFirst();
    var stat = root.stats().getFirst();
    var target = eligible.stream().filter(m -> !m.id().equals(root.id())).findFirst().orElseThrow();
    var rare =
        new ItemState(
            item.snapshotId(),
            item.baseItemId(),
            item.itemLevel(),
            ItemState.Rarity.RARE,
            item.implicits(),
            List.of(new ModifierInstance(root.id(), Map.of(stat.id(), stat.min()))),
            item.conditions(),
            item.augmentSockets(),
            new CatalystQuality(CatalystQuality.Type.FLESH, 20));
    var request =
        new BasicPathService.Request(
            new BasicCurrencyState(rare, service.provenance()),
            new BasicPathService.Policy(
                List.of(WorkbenchCurrency.CHAOS), BasicPathService.PolicyMode.REPEAT_CYCLE),
            new BasicPathService.Target(null, Set.of(target.id())),
            Set.of(),
            List.of(100L, 300L, 500L));
    mvc.perform(
            post("/api/v1/crafting/basic-paths/first-hit")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.renewalProof.proofVersion")
                .value("single-explicit-plain-chaos-full-empty-v1"))
        .andExpect(jsonPath("$.renewalProof.probability.numerator").isString())
        .andExpect(jsonPath("$.renewalProof.probability.denominator").isString())
        .andExpect(jsonPath("$.renewalProof.emptyState.item.catalystQuality.amount").value(20))
        .andExpect(jsonPath("$.points[2].status").value("COMPLETE"))
        .andExpect(jsonPath("$.points[2].unresolved.numerator").value("0"));
  }

  @Test
  void strictScalarMatrixAndExactDecimalObservationStrings() throws Exception {
    var state = new BasicCurrencyState(SolarAmulet.initial(catalog), service.provenance());
    var request =
        new BasicPathService.Request(
            state,
            new BasicPathService.Policy(
                List.of(WorkbenchCurrency.TRANSMUTATION), BasicPathService.PolicyMode.REPEAT_CYCLE),
            new BasicPathService.Target(state, Set.of()),
            Set.of(),
            List.of(100L));
    for (int variant = 0; variant < 9; variant++) {
      com.fasterxml.jackson.databind.node.ObjectNode body = mapper.valueToTree(request);
      var item = (com.fasterxml.jackson.databind.node.ObjectNode) body.get("start").get("item");
      var policy = (com.fasterxml.jackson.databind.node.ObjectNode) body.get("policy");
      switch (variant) {
        case 0 ->
            ((com.fasterxml.jackson.databind.node.ArrayNode) policy.get("actions"))
                .set(0, mapper.valueToTree(0));
        case 1 -> policy.put("mode", 0);
        case 2 -> item.put("rarity", 0);
        case 3 -> item.put("itemLevel", 82.9);
        case 4 -> item.put("itemLevel", "82");
        case 5 -> {
          var values =
              (com.fasterxml.jackson.databind.node.ObjectNode)
                  item.get("implicits").get(0).get("values");
          var field = values.fieldNames().next();
          values.put(field, values.get(field).longValue() + .9);
        }
        case 6 -> body.set("observations", mapper.valueToTree(List.of("100.9")));
        case 7 -> body.set("observations", mapper.valueToTree(List.of(true)));
        default -> body.set("observations", mapper.valueToTree(List.of("9223372036854775808")));
      }
      mvc.perform(
              post("/api/v1/crafting/basic-paths/first-hit")
                  .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(mapper.writeValueAsBytes(body)))
          .andExpect(status().isUnprocessableEntity());
    }
    com.fasterxml.jackson.databind.node.ObjectNode body = mapper.valueToTree(request);
    body.set("observations", mapper.valueToTree(List.of("0", "100", "9223372036854775807")));
    mvc.perform(
            post("/api/v1/crafting/basic-paths/first-hit")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(body)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.points[2].attempts").value("9223372036854775807"));
  }

  @Test
  void rejectsLossyObservationsAndNumericEnumsThroughHttp() throws Exception {
    var state = new BasicCurrencyState(SolarAmulet.initial(catalog), service.provenance());
    var request =
        new BasicPathService.Request(
            state,
            new BasicPathService.Policy(
                List.of(WorkbenchCurrency.TRANSMUTATION), BasicPathService.PolicyMode.REPEAT_CYCLE),
            new BasicPathService.Target(state, Set.of()),
            Set.of(),
            List.of(100L));
    var body = mapper.valueToTree(request);
    ((com.fasterxml.jackson.databind.node.ArrayNode) body.get("observations"))
        .set(0, mapper.valueToTree(100.9));
    mvc.perform(
            post("/api/v1/crafting/basic-paths/first-hit")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(body)))
        .andExpect(status().isUnprocessableEntity());
  }

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
