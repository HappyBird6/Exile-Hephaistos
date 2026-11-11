package com.poe2craft.workbench.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.bootstrap.CatalogConfiguration;
import com.poe2craft.bootstrap.http.CraftingErrors;
import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import com.poe2craft.workbench.application.ModifierReferenceService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkbenchReferenceController.class)
@Import({CatalogConfiguration.class, CraftingErrors.class, ModifierReferenceService.class})
class WorkbenchReferenceControllerTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CatalogCraftingService crafting;
  @Autowired ModifierReferenceService references;
  @Autowired ItemCatalog catalog;

  private JsonNode request(String path, Object body) throws Exception {
    return json.readTree(
        mvc.perform(
                post("/api/v1/crafting/workbench/" + path)
                    .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(body)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  private ItemState rare() {
    var root = SolarAmulet.initial(catalog);
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        root.itemLevel(),
        ItemState.Rarity.RARE,
        root.implicits(),
        List.of(),
        Set.of());
  }

  @Test
  void rebasingPreservesFractureCraftedQualityInfusionAndUnrevealedSlots() throws Exception {
    var root = rare();
    var state =
        new ItemState(
                root.snapshotId(),
                root.baseItemId(),
                100,
                root.rarity(),
                root.implicits(),
                List.of(
                    new ModifierInstance(
                        QualityLimitRules.BREACH_ID,
                        Map.of(QualityLimitRules.STAT_ID, 20L),
                        false,
                        true),
                    new ModifierInstance(
                        "amulet:suffix:of-the-overseer",
                        Map.of("minion_skill_gem_level_+", 3L),
                        true)),
                Set.of(),
                null,
                new CatalystQuality(CatalystQuality.Type.NECROTIC, 50))
            .withQualityInfused(true);
    var abyss =
        AbyssCraftingState.from(state, crafting.abyssCatalog(state))
            .addUnrevealed(
                "pending-reveal",
                ModifierDefinition.AffixType.PREFIX,
                crafting.abyssCatalog(state));
    var input = (ObjectNode) json.valueToTree(abyss.storedItem());
    input.put("snapshotId", "previous-season");
    var before = input.deepCopy();
    var response =
        request(
            "session-compatibility", Map.of("state", input, "compatibilityId", "previous-rule"));
    assertThat(response.path("compatible").asBoolean()).as(response.toString()).isTrue();
    assertThat(response.path("sameRules").asBoolean()).isFalse();
    assertThat(input).isEqualTo(before);
    before.put("snapshotId", response.path("state").path("snapshotId").asText());
    assertThat(response.path("state")).isEqualTo(json.readTree(before.toString()));
    assertThat(response.path("abyss").path("slots").toString()).contains("pending-reveal");
  }

  @Test
  void rebasingPreservesInstalledRunesAndEquipmentInfusion() throws Exception {
    var f = new StockyCraftingFixture();
    var base = f.root;
    var state =
        new ItemState(
                base.snapshotId(),
                base.baseItemId(),
                82,
                base.rarity(),
                base.implicits(),
                base.explicits(),
                Set.of(),
                2)
            .withEquipmentQuality(30)
            .withQualityInfused(true);
    state = SocketAugments.install(state, 0, "Perfect_Desert_Rune");
    var input = (ObjectNode) json.valueToTree(state);
    input.put("snapshotId", "previous-season");
    var response = request("session-compatibility", Map.of("state", input));
    assertThat(response.path("compatible").asBoolean()).as(response.toString()).isTrue();
    var expected = input.deepCopy();
    expected.put("snapshotId", response.path("state").path("snapshotId").asText());
    assertThat(response.path("state")).isEqualTo(json.readTree(expected.toString()));
  }

  @Test
  void oldSnapshotCanBecomeANewCheckpointWithoutChangingRollsOrInput() throws Exception {
    var state = (ObjectNode) json.valueToTree(rare());
    state.put("snapshotId", "old-snapshot");
    var before = state.deepCopy();
    var response =
        request(
            "session-compatibility",
            Map.of(
                "state", state, "compatibilityId", crafting.simulator(rare()).compatibilityId()));
    assertThat(response.path("compatible").asBoolean()).isTrue();
    assertThat(response.path("sameRules").asBoolean()).isTrue();
    assertThat(state).isEqualTo(before);
    state.put("snapshotId", catalog.metadata().snapshotId());
    assertThat(response.get("state")).isEqualTo(json.readTree(json.writeValueAsBytes(state)));
  }

  @Test
  void rejectsUnknownFieldsAndDoesNotClampUnknownOrOutOfRangeModifiers() throws Exception {
    var original = (ObjectNode) json.valueToTree(rare());
    var unknown = original.deepCopy().put("futureProperty", 10);
    assertThat(
            request("session-compatibility", Map.of("state", unknown))
                .path("compatible")
                .asBoolean())
        .isFalse();
    for (var modifier :
        List.of(
            new ModifierInstance("unknown-id", Map.of("life", 10L)),
            new ModifierInstance("amulet:prefix:hale", Map.of("base_maximum_life", 999999L)))) {
      var invalid = original.deepCopy();
      invalid.set("explicits", json.valueToTree(List.of(modifier)));
      assertThat(
              request("session-compatibility", Map.of("state", invalid))
                  .path("compatible")
                  .asBoolean())
          .isFalse();
      assertThat(invalid.get("explicits")).isEqualTo(json.valueToTree(List.of(modifier)));
    }
  }

  @Test
  void legacyCheckpointIsValidatedWithoutClaimingSameRules() throws Exception {
    var response = request("session-compatibility", Map.of("state", rare()));
    assertThat(response.path("compatible").asBoolean()).isTrue();
    assertThat(response.path("sameRules").asBoolean()).isFalse();
  }

  @Test
  void preservesHiddenSlotsAndOffersAndRejectsInconsistentAbyss() throws Exception {
    var abyss =
        AbyssCraftingState.from(rare(), crafting.abyssCatalog(rare()))
            .addUnrevealed(
                "hidden", ModifierDefinition.AffixType.PREFIX, crafting.abyssCatalog(rare()));
    var revealCatalog = crafting.abyssCatalog(rare());
    var offered =
        abyss.eligibleRevealIds("hidden", revealCatalog).stream()
            .limit(3)
            .map(
                id -> {
                  var definition = revealCatalog.find(id).orElseThrow();
                  var rolls = new HashMap<String, Long>();
                  definition.stats().forEach(stat -> rolls.put(stat.id(), stat.min()));
                  return new ModifierInstance(id, rolls);
                })
            .toList();
    abyss =
        new AbyssCraftingState(
            abyss.item(), abyss.slots(), new AbyssCraftingState.Offer("hidden", offered));
    var state = (ObjectNode) json.valueToTree(abyss.storedItem());
    var saved = (ObjectNode) json.valueToTree(abyss);
    state.put("snapshotId", "past");
    ((ObjectNode) saved.get("item")).put("snapshotId", "past");
    var response = request("session-compatibility", Map.of("state", state, "abyss", saved));
    assertThat(response.path("compatible").asBoolean()).isTrue();
    assertThat(response.get("abyss").get("slots")).isEqualTo(saved.get("slots"));
    assertThat(response.get("abyss").get("offer"))
        .isEqualTo(json.readTree(json.writeValueAsBytes(saved.get("offer"))));
    ((ObjectNode) saved.get("item")).put("snapshotId", "different");
    assertThat(
            request("session-compatibility", Map.of("state", state, "abyss", saved))
                .path("compatible")
                .asBoolean())
        .isFalse();
  }

  @Test
  void poolUsesExactExecutionCandidatesAndKeepsExcludedDefinitions() {
    var root = rare();
    var d = catalog.find("amulet:prefix:hale").orElseThrow();
    var values = new HashMap<String, Long>();
    d.stats().forEach(s -> values.put(s.id(), s.min()));
    var state =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            20,
            root.rarity(),
            root.implicits(),
            List.of(new ModifierInstance(d.id(), values)),
            Set.of());
    var entries = references.describe(state, true).entries();
    assertThat(
            entries.stream()
                .filter(e -> e.kind().equals("ordinary") && e.eligible())
                .map(ModifierReferenceService.Entry::modifierId))
        .containsExactlyInAnyOrderElementsOf(
            crafting.simulator(state).pool(state, WorkbenchCurrency.EXALTED, null).stream()
                .map(ModifierDefinition::id)
                .toList());
    assertThat(
            entries.stream()
                .filter(e -> e.modifierId().equals(d.id()) && e.kind().equals("ordinary"))
                .findFirst()
                .orElseThrow()
                .reasons())
        .contains("FAMILY_OCCUPIED");
    assertThat(entries).anyMatch(e -> e.reasons().contains("ITEM_LEVEL"));
    assertThat(references.describe(state, false).entries())
        .allMatch(ModifierReferenceService.Entry::eligible);
  }

  @Test
  void hiddenSlotsUseExecutionAndRevealCandidates() {
    var current = crafting.abyssCatalog(rare());
    var abyss =
        AbyssCraftingState.from(rare(), current)
            .addUnrevealed("hidden", ModifierDefinition.AffixType.PREFIX, current);
    var entries = references.describe(abyss.storedItem(), true).entries();
    var expected =
        new AbyssProbabilityComparison(current).pool(abyss, WorkbenchCurrency.EXALTED, null);
    assertThat(
            entries.stream()
                .filter(e -> e.kind().equals("ordinary") && e.eligible())
                .map(ModifierReferenceService.Entry::modifierId))
        .containsExactlyInAnyOrderElementsOf(
            expected.stream().map(ModifierDefinition::id).toList());
    var reveal = Set.copyOf(abyss.eligibleRevealIds("hidden", current));
    assertThat(entries.stream().filter(e -> e.kind().equals("desecrated") && e.eligible()))
        .allMatch(e -> reveal.contains(e.modifierId()));
  }

  @Test
  void perItemKeyIgnoresCosmeticsAndSnapshotButDetectsMechanicalChanges() {
    var original = crafting.abyssCatalog(rare());
    var simulator = crafting.simulator(rare());
    var first = ItemRulesetIdentity.of(original, simulator, Set.of());
    var m = original.metadata();
    var metadata =
        new ItemCatalog.Metadata(
            "new-snapshot",
            "new-date",
            m.sourceUrl(),
            m.weightPolicy(),
            m.rawSha256(),
            m.detailsSha256(),
            m.prefixCount(),
            m.suffixCount(),
            m.prefixWeight(),
            m.suffixWeight());
    var b = original.base();
    var base =
        new ItemCatalog.BaseItem(
            b.id(),
            "Different display name",
            b.sourceUrl(),
            b.implicitModifierId(),
            b.magicPrefixes(),
            b.magicSuffixes(),
            b.rarePrefixes(),
            b.rareSuffixes());
    var cosmetic =
        original.modifiers().values().stream()
            .map(
                d ->
                    new ModifierDefinition(
                        d.id(),
                        "Translated",
                        d.layer(),
                        d.affixType(),
                        d.familyIds(),
                        d.requiredItemLevel(),
                        d.weight(),
                        d.tier(),
                        "Translated text",
                        d.stats(),
                        d.tags(),
                        d.sourceUrl()))
            .toList();
    assertThat(
            ItemRulesetIdentity.of(new ItemCatalog(metadata, base, cosmetic), simulator, Set.of()))
        .isEqualTo(first);
    var smaller =
        new ItemCatalog.BaseItem(
            b.id(),
            b.name(),
            b.sourceUrl(),
            b.implicitModifierId(),
            b.magicPrefixes(),
            b.magicSuffixes(),
            2,
            b.rareSuffixes());
    assertThat(
            ItemRulesetIdentity.of(
                new ItemCatalog(metadata, smaller, cosmetic), simulator, Set.of()))
        .isNotEqualTo(first);
  }

  @Test
  void fullHiddenSideExplainsCapacityAndDoesNotBlockRevealIntoReservedSlots() {
    var current = crafting.abyssCatalog(rare());
    var abyss = AbyssCraftingState.from(rare(), current);
    for (int i = 0; i < 3; i++)
      abyss = abyss.addUnrevealed("hidden-" + i, ModifierDefinition.AffixType.PREFIX, current);
    var entries = references.describe(abyss.storedItem(), true).entries();
    assertThat(
            entries.stream()
                .filter(
                    e ->
                        e.kind().equals("ordinary")
                            && current.find(e.modifierId()).orElseThrow().affixType()
                                == ModifierDefinition.AffixType.PREFIX))
        .allMatch(e -> !e.eligible() && e.reasons().contains("AFFIX_FULL"));
    assertThat(entries.stream().filter(e -> e.kind().equals("desecrated")))
        .anyMatch(ModifierReferenceService.Entry::eligible);
  }

  @Test
  void everyReviewedCatalogUsesTheSameOrdinaryCandidatesAsItsSimulator() {
    for (var current : crafting.reviewedCatalogs()) {
      var implicits = new ArrayList<ModifierInstance>();
      if (current.base().hasImplicit()) {
        var d = current.find(current.base().implicitModifierId()).orElseThrow();
        var values = new HashMap<String, Long>();
        d.stats().forEach(stat -> values.put(stat.id(), stat.max()));
        implicits.add(new ModifierInstance(d.id(), values));
      }
      for (int level : List.of(1, 82)) {
        var state =
            new ItemState(
                current.metadata().snapshotId(),
                current.base().id(),
                level,
                ItemState.Rarity.RARE,
                implicits,
                List.of(),
                Set.of());
        var actual = references.describe(state, true).entries();
        assertThat(actual).allMatch(e -> e.eligible() == e.reasons().isEmpty());
        assertThat(
                actual.stream()
                    .filter(e -> e.kind().equals("ordinary") && e.eligible())
                    .map(ModifierReferenceService.Entry::modifierId))
            .as(current.base().id() + " level " + level)
            .containsExactlyInAnyOrderElementsOf(
                crafting.simulator(state).pool(state, WorkbenchCurrency.EXALTED, null).stream()
                    .map(ModifierDefinition::id)
                    .toList());
      }
    }
  }

  @Test
  void referenceEndpointsStillRequireCurrentTransportIdentity() throws Exception {
    for (String path : List.of("modifier-pools", "session-compatibility"))
      mvc.perform(
              post("/api/v1/crafting/workbench/" + path)
                  .header(RulesetBoundary.HEADER, "old-rules")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(json.writeValueAsBytes(Map.of("state", rare()))))
          .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void candidateReferencePreservesCatalystQualityWithKnownAndHiddenSlots() throws Exception {
    var plain = rare();
    var quality =
        crafting.apply(plain, WorkbenchCurrency.CATALYST_FLESH, Set.of(), new Random(23)).state();
    for (boolean hidden : List.of(false, true)) {
      var state =
          hidden
              ? quality.withDesecrated(
                  List.of(
                      new ItemState.DesecratedSlot(
                          "hidden", ModifierDefinition.AffixType.SUFFIX, null)))
              : quality;
      var before = json.valueToTree(state);
      var response = request("modifier-pools", Map.of("state", state, "dynamic", true));
      assertThat(response.path("entries").size()).isPositive();
      assertThat(json.<JsonNode>valueToTree(state)).isEqualTo(before);
      assertThat(state.catalystQuality()).isEqualTo(quality.catalystQuality());
    }
  }
}
