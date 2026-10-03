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
        .andExpect(jsonPath("$.ruleVersion").value("solar-workbench-abyss-essence-v16"));
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
}
