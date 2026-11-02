package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.bootstrap.CraftingConfiguration;
import com.poe2craft.crafting.domain.AbyssCraftingState;
import com.poe2craft.crafting.presentation.*;
import com.poe2craft.item.ModifierDefinition.AffixType;
import java.util.*;
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
class AbyssControllerTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  final HysteriaEssenceTest fixture = new HysteriaEssenceTest();

  @Test
  void directChoiceUsesEligiblePoolAndRestoresWithoutThreeCandidates() throws Exception {
    var catalog = com.poe2craft.item.ManualRevealCatalog.enrich(fixture.catalog);
    var hidden =
        com.poe2craft.crafting.domain.DesecrationCurrency.apply(
            fixture.rare(82, ""),
            "Preserved_Collarbone",
            AffixType.PREFIX,
            null,
            Set.of(),
            catalog);
    var slot = hidden.slots().getFirst().id();
    var eligibleResponse =
        mvc.perform(
                post("/api/v1/crafting/workbench/abyss/eligible")
                    .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(Map.of("state", hidden, "slotId", slot))))
            .andExpect(status().isOk())
            .andReturn();
    var ids = json.readTree(eligibleResponse.getResponse().getContentAsString());
    assertThat(ids.size()).isPositive();
    var manual =
        mvc.perform(
                get("/api/v1/crafting/workbench/initial")
                    .param("base", "solar")
                    .param("manualReveal", "true"))
            .andExpect(status().isOk())
            .andReturn();
    var extraId =
        catalog.modifiers().values().stream()
            .filter(d -> d.tags().contains("reveal_only"))
            .findFirst()
            .orElseThrow()
            .id();
    assertThat(
            json.readTree(manual.getResponse().getContentAsString()).path("modifiers").has(extraId))
        .isTrue();
    var ordinary =
        mvc.perform(get("/api/v1/crafting/workbench/initial").param("base", "solar"))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(
            json.readTree(ordinary.getResponse().getContentAsString())
                .path("modifiers")
                .has(extraId))
        .isFalse();
    var definition = catalog.find(ids.get(0).asText()).orElseThrow();
    var values = new HashMap<String, Long>();
    definition.stats().forEach(stat -> values.put(stat.id(), stat.min()));
    var body =
        json.valueToTree(
            Map.of(
                "state",
                hidden,
                "slotId",
                slot,
                "candidate",
                new com.poe2craft.item.ModifierInstance(definition.id(), values)));
    var response =
        mvc.perform(
                post("/api/v1/crafting/workbench/abyss/select")
                    .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.offer").isEmpty())
            .andExpect(jsonPath("$.slots[0].modifier.modifierId").value(definition.id()))
            .andReturn();
    var restored = json.readTree(response.getResponse().getContentAsString());
    mvc.perform(
            post("/api/v1/crafting/workbench/abyss/validate")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("state", restored))))
        .andExpect(status().isOk());
    ((ObjectNode) body.get("candidate")).put("fractured", true);
    mvc.perform(
            post("/api/v1/crafting/workbench/abyss/select")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body)))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void boneApplicationRequiresIdentityAndRestoresCurrencyConstraint() throws Exception {
    var body =
        json.valueToTree(
            Map.of(
                "state",
                fixture.rare(82, "", "amulet:prefix:hale"),
                "currency",
                "Ancient_Collarbone",
                "side",
                "SUFFIX",
                "activeOmens",
                List.of("Omen_of_Dextral_Necromancy")));
    mvc.perform(
            post("/api/v1/crafting/workbench/abyss/desecrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body)))
        .andExpect(status().isUnprocessableEntity());
    var response =
        mvc.perform(
                post("/api/v1/crafting/workbench/abyss/desecrate")
                    .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slots[1].minimumModifierLevel").value(40))
            .andExpect(jsonPath("$.slots[1].modifier").isEmpty())
            .andReturn();
    var next = json.readTree(response.getResponse().getContentAsString());
    mvc.perform(
            post("/api/v1/crafting/workbench/abyss/validate")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("state", next))))
        .andExpect(status().isOk());
  }

  ObjectNode request() {
    var state =
        AbyssCraftingState.from(fixture.rare(82, "", "amulet:prefix:hale"), fixture.catalog)
            .addUnrevealed("abyss", AffixType.SUFFIX, fixture.catalog);
    return json.valueToTree(Map.of("state", state));
  }

  @Test
  void restoresAnUnrevealedSlotWithoutLosingItsOccupancyAndNeverReturnsRevealProbability()
      throws Exception {
    var request = request();
    var restored =
        mvc.perform(
                post("/api/v1/crafting/workbench/abyss/validate")
                    .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slots[1].desecrated").value(true))
            .andReturn();
    request.set("state", json.readTree(restored.getResponse().getContentAsString()));
    request.put("slotId", "abyss");
    var candidate = fixture.rare(82, "", "amulet:suffix:of-the-brute").explicits().getFirst();
    request.set("candidates", json.valueToTree(List.of(candidate, candidate, candidate)));
    var observed =
        mvc.perform(
                post("/api/v1/crafting/workbench/abyss/observe")
                    .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.offer.candidates.length()").value(3))
            .andReturn();
    assertThat(observed.getResponse().getContentAsString())
        .doesNotContain("Probability", "probability");
  }

  @Test
  void unknownFieldsAndFractionalStatValuesAreRejectedBeforeConversion() throws Exception {
    var extra = request();
    ((ObjectNode) extra.path("state").path("slots").get(1)).put("inventedWeight", 10);
    var fractional = request();
    ((ObjectNode) fractional.path("state").path("slots").get(0).path("modifier").path("values"))
        .put("maximum_life", 1.5);
    for (var invalid : List.of(extra, fractional))
      mvc.perform(
              post("/api/v1/crafting/workbench/abyss/validate")
                  .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(json.writeValueAsBytes(invalid)))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void missingRulesetCannotExecuteNewEndpoints() throws Exception {
    mvc.perform(
            post("/api/v1/crafting/workbench/abyss/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(request())))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void commonSlotsImportWithoutFlatteningAndOrdinaryEndpointStillRefuses() throws Exception {
    var common =
        fixture
            .rare(82, "", "amulet:prefix:hale")
            .withDesecrated(
                List.of(
                    new com.poe2craft.item.ItemState.DesecratedSlot(
                        "hidden", AffixType.SUFFIX, null)));
    var body =
        json.writeValueAsBytes(
            Map.of("state", common, "action", "EXALTED", "activeOmens", List.of()));
    mvc.perform(
            post("/api/v1/crafting/workbench/abyss/import")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slots[1].id").value("hidden"))
        .andExpect(jsonPath("$.slots[1].desecrated").value(true));
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void importDoesNotDiscardDuplicateMarksForOneRevealedModifier() throws Exception {
    var state =
        fixture
            .rare(82, "", "amulet:prefix:hale")
            .withDesecrated(
                List.of(
                    new com.poe2craft.item.ItemState.DesecratedSlot(
                        "one", AffixType.PREFIX, "amulet:prefix:hale"),
                    new com.poe2craft.item.ItemState.DesecratedSlot(
                        "two", AffixType.PREFIX, "amulet:prefix:hale")));
    mvc.perform(
            post("/api/v1/crafting/workbench/abyss/import")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("state", state))))
        .andExpect(status().isUnprocessableEntity());
  }
}
