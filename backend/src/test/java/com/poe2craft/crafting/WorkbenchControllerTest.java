package com.poe2craft.crafting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.bootstrap.CraftingConfiguration;
import com.poe2craft.crafting.presentation.CraftingErrors;
import com.poe2craft.crafting.presentation.WorkbenchController;
import com.poe2craft.item.ItemCatalog;
import com.poe2craft.item.SolarAmulet;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkbenchController.class)
@Import({
  CraftingConfiguration.class,
  CraftingErrors.class,
  com.poe2craft.item.testparser.ItemTextService.class
})
class WorkbenchControllerTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired ItemCatalog catalog;
  @Autowired com.poe2craft.crafting.application.WorkbenchService service;

  @Test
  void legacyHomogenisingWorksAcrossNineBasesAndPreservesUnmatchedOmen() throws Exception {
    for (String base :
        java.util.List.of(
            "solar", "stocky", "bow", "wand", "body", "sceptre", "belt", "helmet", "ring")) {
      var initial = service.initial(base, 82);
      var bucket = initial.state();
      var normal =
          new com.poe2craft.item.ItemState(
              bucket.snapshotId(),
              bucket.baseItemId(),
              bucket.itemLevel(),
              bucket.rarity(),
              bucket.implicits(),
              java.util.List.of(),
              bucket.conditions());
      var seed =
          initial.modifiers().values().stream()
              .filter(
                  d ->
                      d.layer() == com.poe2craft.item.ModifierDefinition.Layer.EXPLICIT
                          && d.weight() > 0
                          && d.requiredItemLevel() <= 82)
              .filter(
                  d ->
                      initial.modifiers().values().stream()
                          .anyMatch(
                              other ->
                                  other.layer()
                                          == com.poe2craft.item.ModifierDefinition.Layer.EXPLICIT
                                      && other.weight() > 0
                                      && other.requiredItemLevel() <= 82
                                      && java.util.Collections.disjoint(
                                          d.familyIds(), other.familyIds())
                                      && !java.util.Collections.disjoint(d.tags(), other.tags())))
              .findFirst()
              .orElseThrow();
      var values = new java.util.HashMap<String, Long>();
      seed.stats().forEach(stat -> values.put(stat.id(), stat.min()));
      var magic =
          new com.poe2craft.item.ItemState(
              normal.snapshotId(),
              normal.baseItemId(),
              82,
              com.poe2craft.item.ItemState.Rarity.MAGIC,
              normal.implicits(),
              java.util.List.of(new com.poe2craft.item.ModifierInstance(seed.id(), values)),
              normal.conditions());
      var omens =
          java.util.Set.of("Omen_of_Homogenising_Coronation", "Omen_of_Homogenising_Exaltation");
      mvc.perform(
              post("/api/v1/crafting/workbench/apply")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      mapper.writeValueAsBytes(
                          Map.of("state", magic, "action", "REGAL", "activeOmens", omens))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.applied").value(true))
          .andExpect(jsonPath("$.state.rarity").value("RARE"))
          .andExpect(jsonPath("$.consumedOmens[0]").value("Omen_of_Homogenising_Coronation"))
          .andExpect(jsonPath("$.remainingOmens[0]").value("Omen_of_Homogenising_Exaltation"));
      mvc.perform(
              post("/api/v1/crafting/workbench/apply")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      mapper.writeValueAsBytes(
                          Map.of("state", magic, "action", "GREATER_REGAL", "activeOmens", omens))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.applied").value(false))
          .andExpect(jsonPath("$.consumedOmens").isEmpty());
    }
  }

  @Test
  void typedQualityInspectionPreservesRollsAndRejectsLossyOrCoercedProperties() throws Exception {
    var state =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            mapper.valueToTree(SolarAmulet.initial(catalog));
    state.put("rarity", "RARE");
    state.set(
        "explicits",
        mapper.readTree(
            "[{\"modifierId\":\"amulet:prefix:healthy\",\"values\":{\"base_maximum_life\":29},\"fractured\":true}]"));
    state.set("catalystQuality", mapper.readTree("{\"type\":\"FLESH\",\"amount\":20}"));
    mvc.perform(
            post("/api/v1/crafting/workbench/quality-display")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(Map.of("state", state))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state.catalystQuality.type").value("FLESH"))
        .andExpect(jsonPath("$.state.explicits[0].values.base_maximum_life").value(29))
        .andExpect(jsonPath("$.state.explicits[0].fractured").value(true))
        .andExpect(jsonPath("$.modifiers[1].displayedValues.base_maximum_life").value(34));
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(Map.of("state", state, "action", "DIVINE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applied").value(false))
        .andExpect(jsonPath("$.state.catalystQuality.amount").value(20))
        .andExpect(jsonPath("$.state.explicits[0].values.base_maximum_life").value(29));
    for (String bad :
        java.util.List.of(
            "{\"type\":\"FLESH\",\"amount\":20.5}",
            "{\"type\":\"FLESH\",\"amount\":\"20\"}",
            "{\"type\":\"FLESH\",\"amount\":20,\"extra\":1}")) {
      state.set("catalystQuality", mapper.readTree(bad));
      mvc.perform(
              post("/api/v1/crafting/workbench/quality-display")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(mapper.writeValueAsBytes(Map.of("state", state))))
          .andExpect(status().isUnprocessableEntity());
    }
  }

  @Test
  void legacyJsonDefaultsToUnlockedAndFractureRoundTripsWithoutLosingLockedValues()
      throws Exception {
    var simulator =
        new com.poe2craft.crafting.domain.WorkbenchSimulator(
            catalog, new com.poe2craft.crafting.domain.CraftingEngine(catalog));
    var rare =
        simulator
            .apply(
                SolarAmulet.initial(catalog),
                com.poe2craft.crafting.domain.WorkbenchCurrency.ALCHEMY,
                java.util.Set.of(),
                new java.util.Random(42))
            .state();
    var legacy = mapper.valueToTree(rare);
    for (String layer : java.util.List.of("implicits", "explicits"))
      legacy
          .get(layer)
          .forEach(m -> ((com.fasterxml.jackson.databind.node.ObjectNode) m).remove("fractured"));
    var response =
        mvc.perform(
                post("/api/v1/crafting/workbench/apply")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        mapper.writeValueAsBytes(Map.of("state", legacy, "action", "FRACTURING"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applied").value(true))
            .andExpect(jsonPath("$.events[0].kind").value("FRACTURE"))
            .andExpect(jsonPath("$.ledgerVersion").value("solar-uniform-assumptions-v10"))
            .andReturn();
    var state =
        mapper.treeToValue(
            mapper.readTree(response.getResponse().getContentAsString()).get("state"),
            com.poe2craft.item.ItemState.class);
    var locked =
        state.explicits().stream().filter(com.poe2craft.item.ModifierInstance::fractured).toList();
    org.assertj.core.api.Assertions.assertThat(locked).hasSize(1);
    var divine =
        mvc.perform(
                post("/api/v1/crafting/workbench/apply")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(Map.of("state", state, "action", "DIVINE"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applied").value(true))
            .andReturn();
    var rerolled =
        mapper.treeToValue(
            mapper.readTree(divine.getResponse().getContentAsString()).get("state"),
            com.poe2craft.item.ItemState.class);
    org.assertj.core.api.Assertions.assertThat(rerolled.explicits()).contains(locked.getFirst());
  }

  @Test
  void registrySeparatesInventoryFromImplementedRulesAndPublishesTheLedger() throws Exception {
    mvc.perform(get("/api/v1/crafting/workbench/registry"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value("equipment-crafting-registry-v2"))
        .andExpect(jsonPath("$.inventoryComplete").value(false))
        .andExpect(jsonPath("$.entries.length()").value(220))
        .andExpect(jsonPath("$.assumptionLedger.length()").value(4));
  }

  @Test
  void applyReturnsConcreteRollsAndAssumptionsWithVersionedEvidence() throws Exception {
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsBytes(
                        Map.of("state", SolarAmulet.initial(catalog), "action", "TRANSMUTATION"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applied").value(true))
        .andExpect(jsonPath("$.state.rarity").value("MAGIC"))
        .andExpect(jsonPath("$.state.explicits.length()").value(1))
        .andExpect(jsonPath("$.events[0].kind").value("ADD"))
        .andExpect(
            jsonPath("$.ruleVersion")
                .value("solar-workbench-abyss-essence-v16-homogenising-legacy-v1"));
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.stackTrace").doesNotExist());
  }

  @Test
  void mappingRetainsUnsupportedEvidenceAndOnlyEnablesValidatedSolarStates() throws Exception {
    String text =
        "Item Class: Amulets\nRarity: Rare\nExample\nSolar Amulet\n--------\nItem Level: 82\n--------\n+15 to Spirit (implicit)\n--------\n+17 to maximum Life";
    mvc.perform(
            post("/api/v1/crafting/workbench/map-text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(Map.of("text", text))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mapped").value(true))
        .andExpect(jsonPath("$.state.rarity").value("RARE"))
        .andExpect(jsonPath("$.state.explicits.length()").value(1));
    mvc.perform(
            post("/api/v1/crafting/workbench/map-text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsBytes(Map.of("text", text + "\nGrants Skill: Unknown"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mapped").value(false))
        .andExpect(jsonPath("$.state").isEmpty())
        .andExpect(jsonPath("$.issues").isNotEmpty());
    mvc.perform(
            post("/api/v1/crafting/workbench/actions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsBytes(
                        Map.of(
                            "state",
                            SolarAmulet.initial(catalog),
                            "activeOmens",
                            java.util.List.of()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(49));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"quality", "qualityType", "socketCount", "sockets", "augments"})
  void rejectsUnsupportedPropertiesBeforeApplyAndAvailability(String field) throws Exception {
    var state =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            mapper.valueToTree(SolarAmulet.initial(catalog));
    state.put(field, 1);
    for (String route : java.util.List.of("apply", "actions"))
      mvc.perform(
              post("/api/v1/crafting/workbench/" + route)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      mapper.writeValueAsBytes(Map.of("state", state, "action", "TRANSMUTATION"))))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.code").value("UNSUPPORTED_ITEM_PROPERTIES"));
  }

  @Test
  void rejectsUnknownModifierPropertiesButPreservesLegacyDerivedMetadata() throws Exception {
    var state =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            mapper.valueToTree(SolarAmulet.initial(catalog));
    ((com.fasterxml.jackson.databind.node.ObjectNode) state.path("implicits").get(0))
        .put("socketBound", true);
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsBytes(Map.of("state", state, "action", "TRANSMUTATION"))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_ITEM_PROPERTIES"));
    ((com.fasterxml.jackson.databind.node.ObjectNode) state.path("implicits").get(0))
        .remove("socketBound");
    state.putArray("modifierIds");
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsBytes(Map.of("state", state, "action", "TRANSMUTATION"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applied").value(true));
  }

  @Test
  void ordinarySocketCountIsExplicitOnFreshBaseAndLegacyUnknownIsNotZero() throws Exception {
    mvc.perform(
            get("/api/v1/crafting/workbench/initial")
                .param("base", "stocky")
                .param("itemLevel", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.augmentSockets").value(0));
    var f = new StockyHysteriaTest();
    var state = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.valueToTree(f.root);
    state.remove("augmentSockets");
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(Map.of("state", state, "action", "ARTIFICER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applied").value(false));
    state.put("augmentSockets", 0);
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(Map.of("state", state, "action", "ARTIFICER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applied").value(true))
        .andExpect(jsonPath("$.state.augmentSockets").value(1));
  }

  @Test
  void numericSocketCountMustNotBeCoercedOrAcceptExceptionalState() throws Exception {
    var f = new StockyHysteriaTest();
    for (var value :
        java.util.List.<com.fasterxml.jackson.databind.JsonNode>of(
            mapper.valueToTree(1.5),
            mapper.valueToTree("1"),
            mapper.valueToTree(-1),
            mapper.valueToTree(2),
            mapper.valueToTree(2147483648L))) {
      var state = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.valueToTree(f.root);
      state.set("augmentSockets", value);
      mvc.perform(
              post("/api/v1/crafting/workbench/apply")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(mapper.writeValueAsBytes(Map.of("state", state, "action", "ARTIFICER"))))
          .andExpect(status().isUnprocessableEntity());
    }
  }
}
