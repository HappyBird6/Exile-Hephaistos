package com.poe2craft.workbench.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.bootstrap.CatalogConfiguration;
import com.poe2craft.bootstrap.http.CraftingErrors;
import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.StockyCraftingFixture;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.text.ItemTextService;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkbenchController.class)
@Import({
  CatalogConfiguration.class,
  CraftingErrors.class,
  ItemTextService.class,
  com.poe2craft.workbench.application.ModifierReferenceService.class
})
class WorkbenchControllerTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired ItemCatalog catalog;
  @Autowired CatalogCraftingService service;

  @Test
  void preparesTextOnceWithEquivalentMappingAndPreservesEditRules() throws Exception {
    String text =
        "Item Class: Amulets\nRarity: Normal\nSolar Amulet\n--------\nItem Level: 82\n--------\n+15 to Spirit (implicit)";
    var legacy =
        mapper.readTree(
            mvc.perform(
                    post("/api/v1/crafting/workbench/map-text")
                        .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(Map.of("text", text))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    var prepared =
        mapper.readTree(
            mvc.perform(
                    post("/api/v1/crafting/workbench/prepare-text")
                        .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(Map.of("text", text))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    org.assertj.core.api.Assertions.assertThat(prepared.path("mapping")).isEqualTo(legacy);
    org.assertj.core.api.Assertions.assertThat(
            prepared.path("parsed").path("text").path("originalText").asText())
        .isEqualTo(text);
    var body = Map.of("text", text, "state", prepared.path("mapping").path("state"));
    var edit =
        mapper.readTree(
            mvc.perform(
                    post("/api/v1/crafting/workbench/edit-text")
                        .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    var combined =
        mapper.readTree(
            mvc.perform(
                    post("/api/v1/crafting/workbench/prepare-text")
                        .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    org.assertj.core.api.Assertions.assertThat(combined.path("mapping")).isEqualTo(edit);
    mvc.perform(
            post("/api/v1/crafting/workbench/prepare-text")
                .header("X-Crafting-Ruleset", "obsolete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(body)))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void hiddenAbyssSlotsAlsoReceivePoolUpdatesWithCurrency() throws Exception {
    var root = SolarAmulet.initial(catalog);
    var rare =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            root.itemLevel(),
            ItemState.Rarity.RARE,
            root.implicits(),
            java.util.List.of(),
            java.util.Set.of());
    var abyss =
        com.poe2craft.shared.crafting.domain.AbyssCraftingState.from(
                rare, service.abyssCatalog(rare))
            .addUnrevealed(
                "hidden", ModifierDefinition.AffixType.PREFIX, service.abyssCatalog(rare));
    mvc.perform(
            post("/api/v1/crafting/workbench/abyss/currency")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .header("X-Modifier-Pool-Revision", "unknown")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsBytes(
                        Map.of(
                            "state",
                            abyss,
                            "action",
                            "EXALTED",
                            "activeOmens",
                            java.util.List.of()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applied").value(true))
        .andExpect(jsonPath("$.modifierPool.reset").value(true))
        .andExpect(jsonPath("$.modifierPool.entries").isNotEmpty());
  }

  @Test
  void optedInApplyIncludesPoolAndLegacyClientsKeepOriginalEnvelope() throws Exception {
    var initial = SolarAmulet.initial(catalog);
    var result =
        mvc.perform(
                post("/api/v1/crafting/workbench/apply")
                    .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                    .header("X-Modifier-Pool-Revision", "unknown")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        mapper.writeValueAsBytes(
                            Map.of("state", initial, "action", "TRANSMUTATION"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applied").value(true))
            .andExpect(jsonPath("$.rulesetIdentity").value(RulesetBoundary.identity()))
            .andExpect(jsonPath("$.modifierPool.reset").value(true))
            .andExpect(jsonPath("$.modifierPool.entries").isNotEmpty())
            .andReturn();
    var body = mapper.readTree(result.getResponse().getContentAsString());
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .header(
                    "X-Modifier-Pool-Revision", body.path("modifierPool").path("revision").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsBytes(
                        Map.of("state", body.get("state"), "action", "DIVINE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.modifierPool.reset").value(false))
        .andExpect(jsonPath("$.modifierPool.entries").isEmpty());
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsBytes(Map.of("state", initial, "action", "TRANSMUTATION"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.modifierPool").doesNotExist());
  }

  @Test
  void importedObservationsSurviveJsonRestorationAndManualCraftsWhileUnresolvedAugmentsStayOnHold()
      throws Exception {
    for (String source :
        java.util.List.of(
            "Item Class: Body Armours\nRarity: Normal\nSoldier Cuirass\n--------\nSockets: S S S S\nItem Level: 82",
            "Item Class: Sceptres\nRarity: Normal\nOmen Sceptre\n--------\nGrants Skill: Level 17 Malice\nItem Level: 82",
            "Item Class: Body Armours\nRarity: Normal\nSoldier Cuirass\n--------\nSockets: S S\nItem Level: 82\n--------\n20% increased Armour (rune)")) {
      var mapped =
          mapper.readTree(
              mvc.perform(
                      post("/api/v1/crafting/workbench/map-text")
                          .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(mapper.writeValueAsBytes(Map.of("text", source))))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.mapped").value(true))
                  .andReturn()
                  .getResponse()
                  .getContentAsString());
      var state = mapped.get("state");
      var observations = state.get("observations").deepCopy();
      for (String action : java.util.List.of("TRANSMUTATION", "AUGMENTATION", "REGAL")) {
        var result =
            mapper.readTree(
                mvc.perform(
                        post("/api/v1/crafting/workbench/apply")
                            .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                mapper.writeValueAsBytes(Map.of("state", state, "action", action))))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString());
        org.assertj.core.api.Assertions.assertThat(result.get("state").get("observations"))
            .isEqualTo(observations);
        if (source.contains("(rune)")) {
          org.assertj.core.api.Assertions.assertThat(result.path("applied").asBoolean()).isFalse();
          org.assertj.core.api.Assertions.assertThat(result.get("state")).isEqualTo(state);
        } else
          org.assertj.core.api.Assertions.assertThat(result.path("applied").asBoolean()).isTrue();
        state = mapper.readTree(mapper.writeValueAsBytes(result.get("state")));
      }
    }
  }

  @Test
  void rulesetBoundaryPreservesReadsAndRejectsOlderAndMismatchedClients() throws Exception {
    mvc.perform(get("/api/v1/crafting/workbench/initial"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rulesetIdentity").value(RulesetBoundary.identity()));
    var state = SolarAmulet.initial(catalog);
    for (String endpoint : java.util.List.of("apply", "actions", "quality-display", "map-text")) {
      var body =
          mapper.writeValueAsBytes(Map.of("state", state, "action", "TRANSMUTATION", "text", ""));
      mvc.perform(
              post("/api/v1/crafting/workbench/" + endpoint)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.code").value("RULESET_IDENTITY_REQUIRED"));
      mvc.perform(
              post("/api/v1/crafting/workbench/" + endpoint)
                  .header("X-Crafting-Ruleset", "past-season")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.code").value("RULESET_IDENTITY_MISMATCH"));
    }
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsBytes(Map.of("state", state, "action", "TRANSMUTATION"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applied").value(true))
        .andExpect(jsonPath("$.rulesetIdentity").value(RulesetBoundary.identity()));
  }

  @Test
  void legacyHomogenisingWorksAcrossNineBasesAndPreservesUnmatchedOmen() throws Exception {
    for (String base :
        java.util.List.of(
            "solar", "stocky", "bow", "wand", "body", "sceptre", "belt", "helmet", "ring")) {
      var initial = service.initial(base, 82);
      var bucket = initial.state();
      var normal =
          new ItemState(
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
                      d.layer() == ModifierDefinition.Layer.EXPLICIT
                          && d.weight() > 0
                          && d.requiredItemLevel() <= 82)
              .filter(
                  d ->
                      initial.modifiers().values().stream()
                          .anyMatch(
                              other ->
                                  other.layer() == ModifierDefinition.Layer.EXPLICIT
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
          new ItemState(
              normal.snapshotId(),
              normal.baseItemId(),
              82,
              ItemState.Rarity.MAGIC,
              normal.implicits(),
              java.util.List.of(new ModifierInstance(seed.id(), values)),
              normal.conditions());
      var omens =
          java.util.Set.of("Omen_of_Homogenising_Coronation", "Omen_of_Homogenising_Exaltation");
      mvc.perform(
              post("/api/v1/crafting/workbench/apply")
                  .header("X-Crafting-Ruleset", RulesetBoundary.identity())
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
                  .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      mapper.writeValueAsBytes(
                          Map.of("state", magic, "action", "GREATER_REGAL", "activeOmens", omens))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.applied").value(true))
          .andExpect(jsonPath("$.consumedOmens[0]").value("Omen_of_Homogenising_Coronation"))
          .andExpect(jsonPath("$.remainingOmens[0]").value("Omen_of_Homogenising_Exaltation"));
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
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(Map.of("state", state))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state.catalystQuality.type").value("FLESH"))
        .andExpect(jsonPath("$.state.explicits[0].values.base_maximum_life").value(29))
        .andExpect(jsonPath("$.state.explicits[0].fractured").value(true))
        .andExpect(jsonPath("$.modifiers[1].displayedValues.base_maximum_life").value(35));
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
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
                  .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(mapper.writeValueAsBytes(Map.of("state", state))))
          .andExpect(status().isUnprocessableEntity());
    }
  }

  @Test
  void legacyJsonDefaultsToUnlockedAndFractureRoundTripsWithoutLosingLockedValues()
      throws Exception {
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var rare =
        simulator
            .apply(
                SolarAmulet.initial(catalog),
                WorkbenchCurrency.ALCHEMY,
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
                    .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        mapper.writeValueAsBytes(Map.of("state", legacy, "action", "FRACTURING"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applied").value(true))
            .andExpect(jsonPath("$.events[0].kind").value("FRACTURE"))
            .andExpect(jsonPath("$.ledgerVersion").value("solar-uniform-assumptions-v14"))
            .andReturn();
    var state =
        mapper.treeToValue(
            mapper.readTree(response.getResponse().getContentAsString()).get("state"),
            ItemState.class);
    var locked = state.explicits().stream().filter(ModifierInstance::fractured).toList();
    org.assertj.core.api.Assertions.assertThat(locked).hasSize(1);
    var divine =
        mvc.perform(
                post("/api/v1/crafting/workbench/apply")
                    .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(Map.of("state", state, "action", "DIVINE"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applied").value(true))
            .andReturn();
    var rerolled =
        mapper.treeToValue(
            mapper.readTree(divine.getResponse().getContentAsString()).get("state"),
            ItemState.class);
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
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
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
                .value(
                    "equipment-currency-v1-solar-workbench-abyss-essence-v22-homogenising-legacy-v1-legacy-five-v1-omen-composition-v2-fracture-whittling-v2-catalyst-max-v1-refined-sapphire-v1-quality-cap-preserve-v2-sanctification-unfractured-rounded-v3-user-selected-equipment-quality-maximum-v2-quality-infuser-once-plus-ten-v1"));
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
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
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(Map.of("text", text))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mapped").value(true))
        .andExpect(jsonPath("$.state.rarity").value("RARE"))
        .andExpect(jsonPath("$.state.explicits.length()").value(1));
    mvc.perform(
            post("/api/v1/crafting/workbench/map-text")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsBytes(Map.of("text", text + "\nGrants Skill: Unknown"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mapped").value(false))
        .andExpect(jsonPath("$.state").isEmpty())
        .andExpect(jsonPath("$.issues").isNotEmpty());
    mvc.perform(
            post("/api/v1/crafting/workbench/actions")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsBytes(
                        Map.of(
                            "state",
                            SolarAmulet.initial(catalog),
                            "activeOmens",
                            java.util.List.of()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(82));
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
                  .header("X-Crafting-Ruleset", RulesetBoundary.identity())
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
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
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
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
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
    var f = new StockyCraftingFixture();
    var state = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.valueToTree(f.root);
    state.remove("augmentSockets");
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(Map.of("state", state, "action", "ARTIFICER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applied").value(false));
    state.put("augmentSockets", 0);
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(Map.of("state", state, "action", "ARTIFICER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applied").value(true))
        .andExpect(jsonPath("$.state.augmentSockets").value(1));
  }

  @Test
  void numericSocketCountMustNotBeCoercedOrAcceptExceptionalState() throws Exception {
    var f = new StockyCraftingFixture();
    for (var value :
        java.util.List.<com.fasterxml.jackson.databind.JsonNode>of(
            mapper.valueToTree(1.5),
            mapper.valueToTree("1"),
            mapper.valueToTree(-1),
            mapper.valueToTree(3),
            mapper.valueToTree(2147483648L))) {
      var state = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.valueToTree(f.root);
      state.set("augmentSockets", value);
      mvc.perform(
              post("/api/v1/crafting/workbench/apply")
                  .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(mapper.writeValueAsBytes(Map.of("state", state, "action", "ARTIFICER"))))
          .andExpect(status().isUnprocessableEntity());
    }
  }

  @Test
  void importedEquipmentQualitySurvivesManualCurrencyAndJsonRestore() throws Exception {
    var text =
        "Item Class: Body Armours\nRarity: Normal\nSoldier Cuirass\n--------\nQuality: +20%\nItem Level: 82";
    var mapped =
        mvc.perform(
                post("/api/v1/crafting/workbench/map-text")
                    .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(Map.of("text", text))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mapped").value(true))
            .andReturn();
    var state = mapper.readTree(mapped.getResponse().getContentAsString()).get("state");
    for (String action : java.util.List.of("TRANSMUTATION", "AUGMENTATION", "REGAL")) {
      var result =
          mvc.perform(
                  post("/api/v1/crafting/workbench/apply")
                      .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          mapper.writeValueAsBytes(
                              Map.of(
                                  "state",
                                  state,
                                  "action",
                                  action,
                                  "activeOmens",
                                  java.util.List.of()))))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.applied").value(true))
              .andExpect(jsonPath("$.state.equipmentQuality").value(20))
              .andReturn();
      state = mapper.readTree(result.getResponse().getContentAsString()).get("state");
    }
  }
}
