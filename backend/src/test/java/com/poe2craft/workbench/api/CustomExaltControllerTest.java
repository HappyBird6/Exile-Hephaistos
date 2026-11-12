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

// Keep the real HTTP filters; mock only their external Redis dependency.
@org.springframework.test.context.bean.override.mockito.MockitoBean(
    types = org.springframework.data.redis.core.StringRedisTemplate.class)
@WebMvcTest({CustomExaltController.class, WorkbenchController.class})
@Import({
  CatalogConfiguration.class,
  CraftingErrors.class,
  ItemTextService.class,
  com.poe2craft.workbench.application.ModifierReferenceService.class
})
class CustomExaltControllerTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CatalogCraftingService crafting;
  @Autowired com.poe2craft.workbench.application.ModifierReferenceService references;

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

  private JsonNode callWithPool(String path, Object body, ItemState before) throws Exception {
    var previous = references.describe(before, true);
    var result =
        (ObjectNode)
            json.readTree(
                mvc.perform(
                        post("/api/v1/crafting/workbench/" + path)
                            .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                            .header("X-Modifier-Pool-Revision", previous.revision())
                            .header("X-Modifier-Pool-Baseline", previous.staticRevision())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsBytes(body)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString());
    var delta = result.remove("modifierPool");
    result.remove("rulesetIdentity");
    assertThat(delta).isNotNull();
    ItemState after =
        result.has("result")
            ? json.treeToValue(result.path("result").path("state"), ItemState.class)
            : result.has("state")
                ? json.treeToValue(result.path("state"), ItemState.class)
                : json.treeToValue(result, AbyssCraftingState.class).storedItem();
    var complete = references.describe(after, true);
    assertThat(delta.path("revision").asText()).isEqualTo(complete.revision());
    assertThat(delta.path("reset").asBoolean()).isFalse();
    var entries = new HashMap<String, JsonNode>();
    for (var entry : json.valueToTree(previous.entries()))
      entries.put(entry.path("kind").asText() + ":" + entry.path("modifierId").asText(), entry);
    delta.path("removed").forEach(e -> entries.remove(e.asText()));
    delta
        .path("entries")
        .forEach(
            e -> entries.put(e.path("kind").asText() + ":" + e.path("modifierId").asText(), e));
    var expected = new ArrayList<JsonNode>();
    json.valueToTree(complete.entries()).forEach(expected::add);
    assertThat(entries.values()).containsExactlyInAnyOrderElementsOf(expected);
    return result;
  }

  @Test
  void everyNewCraftTransportReconstructsTheAuthoritativePool() throws Exception {
    var state = rare("body");
    var socketState = (ObjectNode) json.valueToTree(state);
    socketState.put("augmentSockets", 1);
    var socketItem = json.treeToValue(socketState, ItemState.class);
    callWithPool(
        "augments/install",
        Map.of("state", socketItem, "slotIndex", 0, "augmentId", "Perfect_Iron_Rune"),
        socketItem);
    var hidden =
        callWithPool(
            "abyss/desecrate",
            Map.of(
                "state",
                state,
                "currency",
                "Preserved_Rib",
                "side",
                "PREFIX",
                "activeOmens",
                List.of()),
            state);
    var abyss = json.treeToValue(hidden, AbyssCraftingState.class);
    var id = abyss.slots().getFirst().id();
    var catalog = crafting.abyssCatalog(abyss.item());
    var definition = catalog.find(abyss.eligibleRevealIds(id, catalog).getFirst()).orElseThrow();
    var values = new HashMap<String, Long>();
    definition.stats().forEach(stat -> values.put(stat.id(), stat.min()));
    callWithPool(
        "abyss/select",
        Map.of(
            "state",
            abyss,
            "slotId",
            id,
            "candidate",
            new ModifierInstance(definition.id(), values)),
        abyss.storedItem());
    var choices =
        call(
            "custom-exalt/choices",
            Map.of("state", state, "action", "EXALTED", "activeOmens", List.of()));
    callWithPool(
        "custom-exalt/apply",
        Map.of(
            "state",
            state,
            "action",
            "EXALTED",
            "activeOmens",
            List.of(),
            "modifierId",
            choices.get(0).path("modifierId").asText()),
        state);
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

  @Test
  void hiddenAndRevealedExaltsShareQualityAndUnrelatedOmenSupportWithOrdinaryCrafting()
      throws Exception {
    for (boolean hidden : List.of(false, true))
      for (boolean quality : List.of(false, true))
        for (String action : List.of("EXALTED", "GREATER_EXALTED", "PERFECT_EXALTED")) {
          var root = rare("solar");
          if (quality)
            root =
                crafting
                    .apply(root, WorkbenchCurrency.CATALYST_FLESH, Set.of(), new Random(17))
                    .state();
          var catalog = crafting.abyssCatalog(root);
          var definition =
              catalog.modifiers().values().stream()
                  .filter(
                      d ->
                          d.layer() == ModifierDefinition.Layer.EXPLICIT
                              && d.weight() > 0
                              && d.affixType() == ModifierDefinition.AffixType.SUFFIX)
                  .findFirst()
                  .orElseThrow();
          var values = new HashMap<String, Long>();
          definition.stats().forEach(r -> values.put(r.id(), r.min()));
          var modifier = new ModifierInstance(definition.id(), values);
          var state =
              new ItemState(
                      root.snapshotId(),
                      root.baseItemId(),
                      root.itemLevel(),
                      root.rarity(),
                      root.implicits(),
                      hidden ? List.of() : List.of(modifier),
                      root.conditions(),
                      root.augmentSockets(),
                      root.catalystQuality())
                  .withDesecrated(
                      List.of(
                          new ItemState.DesecratedSlot(
                              "preserved",
                              definition.affixType(),
                              hidden ? null : definition.id())));
          var body = new LinkedHashMap<String, Object>();
          body.put("state", state);
          body.put("abyss", AbyssCraftingState.from(state, catalog));
          body.put("action", action);
          body.put("activeOmens", Set.of("Omen_of_Whittling"));
          var choices = call("custom-exalt/choices", body);
          assertThat(choices.size())
              .as("hidden=%s quality=%s action=%s", hidden, quality, action)
              .isPositive();
          body.put("modifierId", choices.get(0).path("modifierId").asText());
          var applied = call("custom-exalt/apply", body);
          assertThat(applied.path("result").path("state").path("desecrated"))
              .isEqualTo(json.valueToTree(state.desecrated()));
          assertThat(applied.path("result").path("remainingOmens"))
              .isEqualTo(json.readTree("[\"Omen_of_Whittling\"]"));
          assertThat(applied.path("result").path("state").path("catalystQuality"))
              .isEqualTo(json.valueToTree(state.catalystQuality()));
        }
  }
}
