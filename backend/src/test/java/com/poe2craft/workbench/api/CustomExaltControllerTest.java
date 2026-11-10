package com.poe2craft.workbench.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.poe2craft.bootstrap.CatalogConfiguration;
import com.poe2craft.bootstrap.http.CraftingErrors;
import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.text.ItemTextService;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({CustomExaltController.class, WorkbenchController.class})
@Import({CatalogConfiguration.class, CraftingErrors.class, ItemTextService.class})
class CustomExaltControllerTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CatalogCraftingService crafting;

  private ItemState rare(String base) {
    var b = crafting.initial(base, 100, true).state();
    return new ItemState(
        b.snapshotId(),
        b.baseItemId(),
        100,
        ItemState.Rarity.RARE,
        b.implicits(),
        List.of(),
        Set.of());
  }

  private JsonNode call(String path, Object body) throws Exception {
    return json.readTree(
        mvc.perform(
                post("/api/v1/crafting/workbench/" + path)
                    .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(body)))
            .andExpect(
                r ->
                    assertThat(r.getResponse().getStatus())
                        .withFailMessage(
                            "HTTP %s: %s", r.getResponse().getStatus(), r.getResolvedException())
                        .isEqualTo(200))
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  @Test
  void allExaltedTiersKeepNaturalWeightsAndOnlyForceTheChosenTier() throws Exception {
    var state = rare("solar");
    var catalog = crafting.abyssCatalog(state);
    for (String action : List.of("EXALTED", "GREATER_EXALTED", "PERFECT_EXALTED")) {
      var request = new LinkedHashMap<String, Object>();
      request.put("state", state);
      request.put("action", action);
      request.put("activeOmens", List.of());
      var choices = call("custom-exalt/choices", request);
      assertThat(choices.size()).isGreaterThan(1);
      double sum = 0;
      for (var choice : choices) sum += choice.path("probability").asDouble();
      assertThat(sum).isCloseTo(1, within(1e-10));
      var chosen = choices.get(0);
      request.put("modifierId", chosen.path("modifierId").asText());
      var applied = call("custom-exalt/apply", request);
      assertThat(applied.path("choice")).isEqualTo(chosen);
      assertThat(applied.path("result").path("action").asText()).isEqualTo(action);
      var item = json.treeToValue(applied.path("result").path("state"), ItemState.class);
      assertThat(item.explicits()).hasSize(1);
      var mod = item.explicits().getFirst();
      assertThat(mod.modifierId()).isEqualTo(chosen.path("modifierId").asText());
      for (var range : catalog.find(mod.modifierId()).orElseThrow().stats())
        assertThat(mod.values().get(range.id())).isBetween(range.min(), range.max());
      request.put("state", item);
      mvc.perform(
              post("/api/v1/crafting/workbench/custom-exalt/apply")
                  .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(json.writeValueAsBytes(request)))
          .andExpect(status().isUnprocessableEntity());
    }
  }

  @Test
  void customExaltPreservesHiddenDesecrationConstraints() throws Exception {
    var state =
        rare("solar")
            .withDesecrated(
                List.of(
                    new ItemState.DesecratedSlot(
                        "hidden", ModifierDefinition.AffixType.PREFIX, null, 40, "ALTERED")));
    var body = new LinkedHashMap<String, Object>();
    body.put("state", state);
    body.put("action", "EXALTED");
    body.put("activeOmens", List.of());
    var choices = call("custom-exalt/choices", body);
    body.put("modifierId", choices.get(0).path("modifierId").asText());
    var result = call("custom-exalt/apply", body);
    assertThat(result.path("result").path("state").path("desecrated"))
        .isEqualTo(json.valueToTree(state.desecrated()));
    assertThat(result.path("abyss").path("slots").get(0).path("minimumModifierLevel").asInt())
        .isEqualTo(40);
  }

  @Test
  void augmentInstallKeepsBothHiddenAndRevealedDesecratedOptions() throws Exception {
    var root = rare("body");
    var body = (ObjectNode) json.valueToTree(root);
    body.put("augmentSockets", 2);
    var slot =
        json.createObjectNode()
            .put("id", "hidden")
            .put("side", "PREFIX")
            .put("minimumModifierLevel", 40);
    slot.putNull("modifierId");
    body.set("desecrated", json.createArrayNode().add(slot));
    var installed =
        call(
            "augments/install",
            Map.of("state", body, "slotIndex", 0, "augmentId", "Perfect_Iron_Rune"));
    assertThat(installed.path("state").path("desecrated")).isEqualTo(body.path("desecrated"));
    assertThat(installed.path("state").path("installedAugments").get(0).path("augmentId").asText())
        .isEqualTo("Perfect_Iron_Rune");
    var d =
        crafting.initial("body", 100, true).modifiers().values().stream()
            .filter(
                m ->
                    m.layer() == ModifierDefinition.Layer.EXPLICIT
                        && m.affixType() == ModifierDefinition.AffixType.PREFIX
                        && m.requiredItemLevel() >= 40
                        && m.weight() > 0)
            .findFirst()
            .orElseThrow();
    var values = new HashMap<String, Long>();
    d.stats().forEach(s -> values.put(s.id(), s.min()));
    body.set("explicits", json.valueToTree(List.of(new ModifierInstance(d.id(), values))));
    slot.put("modifierId", d.id());
    slot.remove("revealPool");
    installed =
        call(
            "augments/install",
            Map.of("state", body, "slotIndex", 0, "augmentId", "Perfect_Iron_Rune"));
    assertThat(installed.path("state").path("desecrated")).isEqualTo(body.path("desecrated"));
    assertThat(installed.path("state").path("explicits"))
        .isEqualTo(json.readTree(body.path("explicits").toString()));
  }

  @Test
  void twoModifierOmenStillAddsTwoAndReferenceChanceIncludesEitherDraw() {
    var state = rare("solar");
    var simulator = crafting.simulator(state);
    var omens = Set.of("Omen_of_Greater_Exaltation");
    var choices = simulator.exaltChoices(state, WorkbenchCurrency.EXALTED, omens);
    assertThat(choices).isNotEmpty();
    assertThat(choices.stream().mapToDouble(WorkbenchSimulator.ExaltChoice::probability).sum())
        .isCloseTo(2, within(1e-9));
    var result =
        simulator.applyCustomExalt(
            state,
            WorkbenchCurrency.EXALTED,
            omens,
            new SplittableRandom(17),
            choices.getFirst().modifierId());
    assertThat(result.state().explicits()).hasSize(2);
    assertThat(result.state().explicits().getFirst().modifierId())
        .isEqualTo(choices.getFirst().modifierId());
  }
}
