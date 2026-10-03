package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.application.WorkbenchService;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class StockyWorkbenchTest {
  private ItemCatalog stocky() throws Exception {
    try (var data = getClass().getResourceAsStream("/catalog/stocky-mitts/catalog.json");
        var raw = getClass().getResourceAsStream("/catalog/stocky-mitts/base.raw.json");
        var details = getClass().getResourceAsStream("/catalog/stocky-mitts/details.raw.json");
        var special =
            getClass().getResourceAsStream("/catalog/stocky-mitts/abyss-essence.catalog.json");
        var specialRaw =
            getClass().getResourceAsStream("/catalog/stocky-mitts/abyss-essence.raw.json");
        var horror =
            getClass().getResourceAsStream("/catalog/stocky-mitts/horror-essence.catalog.json");
        var horrorRaw =
            getClass().getResourceAsStream("/catalog/stocky-mitts/horror-essence.raw.json");
        var perfect =
            getClass()
                .getResourceAsStream(
                    "/catalog/stocky-mitts/perfect-grounding-opulence.catalog.json");
        var perfectRaw =
            getClass()
                .getResourceAsStream("/catalog/stocky-mitts/perfect-grounding-opulence.raw.json");
        var prismatic =
            getClass().getResourceAsStream("/catalog/stocky-mitts/prismatic-alloy.catalog.json");
        var prismaticRaw =
            getClass().getResourceAsStream("/catalog/stocky-mitts/prismatic-alloy.raw.json")) {
      return ItemCatalogLoader.addSpecial(
          ItemCatalogLoader.addSpecial(
              ItemCatalogLoader.addSpecial(
                  ItemCatalogLoader.loadWithSpecial(data, raw, details, special, specialRaw),
                  horror,
                  horrorRaw),
              perfect,
              perfectRaw),
          prismatic,
          prismaticRaw);
    }
  }

  private WorkbenchService service() throws Exception {
    var solar = ItemCatalogLoader.loadDefault();
    return new WorkbenchService(
        solar, new WorkbenchSimulator(solar, new CraftingEngine(solar)), stocky());
  }

  private ItemState initial(WorkbenchService service) {
    var preset = service.initial("stocky", 82);
    return new ItemState(
        preset.state().snapshotId(),
        preset.state().baseItemId(),
        82,
        ItemState.Rarity.NORMAL,
        List.of(),
        List.of(),
        Set.of());
  }

  @Test
  void fullPoolSupportsOrdinaryCraftingFractureAndModelledRerolls() throws Exception {
    var service = service();
    var catalog = stocky();
    for (int seed = 0; seed < 40; seed++) {
      var random = new Random(seed);
      var rare = service.apply(initial(service), WorkbenchCurrency.ALCHEMY, Set.of(), random);
      assertThat(rare.applied()).isTrue();
      assertThat(rare.state().implicits()).isEmpty();
      assertThat(rare.state().explicits()).hasSize(4);
      assertThat(rare.ruleVersion()).isEqualTo("stocky-workbench-prismatic-v23");
      var locked =
          service.apply(rare.state(), WorkbenchCurrency.FRACTURING, Set.of(), random).state();
      var fracture =
          locked.explicits().stream().filter(ModifierInstance::fractured).findFirst().orElseThrow();
      for (var action :
          List.of(WorkbenchCurrency.DIVINE, WorkbenchCurrency.CHAOS, WorkbenchCurrency.ANNULMENT)) {
        var result = service.apply(locked, action, Set.of(), random);
        assertThat(result.state().explicits()).contains(fracture);
        assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
      }
      assertThat(
              service
                  .apply(locked, WorkbenchCurrency.DIVINE, Set.of("Omen_of_the_Blessed"), random)
                  .applied())
          .isFalse();
    }
  }

  @Test
  void sourcedEssencesHaveOneGuaranteedBaseSpecificResultAndSafeUnsupportedMaterials()
      throws Exception {
    var service = service();
    var root = initial(service);
    var magic =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            ItemState.Rarity.MAGIC,
            List.of(),
            List.of(),
            Set.of());
    for (var action :
        List.of(
            WorkbenchCurrency.LESSER_ESSENCE_ENHANCEMENT,
            WorkbenchCurrency.ESSENCE_ENHANCEMENT,
            WorkbenchCurrency.GREATER_ESSENCE_ENHANCEMENT,
            WorkbenchCurrency.GREATER_ESSENCE_BATTLE)) {
      var result = service.apply(magic, action, Set.of(), new Random(1));
      assertThat(result.applied()).isTrue();
      assertThat(result.state().rarity()).isEqualTo(ItemState.Rarity.RARE);
      assertThat(result.state().explicits())
          .extracting(ModifierInstance::modifierId)
          .containsExactly(action.fixedModifierId());
      assertThat(result.events()).hasSize(1);
    }
    for (var action :
        List.of(
            WorkbenchCurrency.ESSENCE_INFINITE,
            WorkbenchCurrency.RUNIC_ALLOY,
            WorkbenchCurrency.PERFECT_ESSENCE_ENHANCEMENT)) {
      var result = service.apply(magic, action, Set.of(), new Random(1));
      assertThat(result.applied()).isFalse();
      assertThat(result.state()).isEqualTo(magic);
      assertThat(result.events()).isEmpty();
    }
    assertThat(service.initial("solar", 82).modifiers()).hasSize(218);
    assertThatThrownBy(() -> service.initial("unknown", 82))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void omenAffixRestrictionAndConflictsPreserveTheFullCatalog() throws Exception {
    var service = service();
    var catalog = stocky();
    for (int seed = 0; seed < 20; seed++) {
      var magic =
          service.apply(
              new ItemState(
                  initial(service).snapshotId(),
                  WorkbenchService.STOCKY_BASE_ID,
                  82,
                  ItemState.Rarity.RARE,
                  List.of(),
                  List.of(),
                  Set.of()),
              WorkbenchCurrency.EXALTED,
              Set.of("Omen_of_Sinistral_Exaltation"),
              new Random(seed));
      assertThat(magic.applied()).isTrue();
      assertThat(magic.state().explicits())
          .allMatch(
              m ->
                  catalog.find(m.modifierId()).orElseThrow().affixType()
                      == ModifierDefinition.AffixType.PREFIX);
    }
    assertThat(catalog.modifiers()).hasSize(188);
    assertThat(
            service
                .apply(
                    initial(service),
                    WorkbenchCurrency.EXALTED,
                    Set.of("Omen_of_Sinistral_Exaltation", "Omen_of_Dextral_Exaltation"),
                    new Random(1))
                .applied())
        .isFalse();
  }

  @Test
  void additionalBasicEssencesMatchSourceAndPreserveBlockedInputs() throws Exception {
    var catalog = stocky();
    var service = service();
    var root = initial(service);
    try (var source =
        getClass().getResourceAsStream("/catalog/stocky-mitts/basic-essence-proof.json")) {
      var proofs = new com.fasterxml.jackson.databind.ObjectMapper().readTree(source);
      assertThat(proofs.size()).isEqualTo(21);
      assertThat(StockyEssenceTargets.VERIFIED).hasSize(21);
      for (var proof : proofs) {
        var action = WorkbenchCurrency.valueOf(proof.get("action").asText());
        var target = StockyEssenceTargets.VERIFIED.get(action).getFirst();
        var definition = catalog.find(target).orElseThrow();
        assertThat(target).isEqualTo(proof.get("definition").get("id").asText());
        assertThat(definition.requiredItemLevel())
            .isEqualTo(proof.get("sourceRow").get("Level").asInt());
        assertThat(definition.familyIds())
            .containsExactly(proof.get("sourceRow").get("ModFamilyList").get(0).asText());
        assertThat(proof.get("matched").asBoolean()).isTrue();
        for (var sourceStat : proof.get("parsed").get("stats")) {
          var stat =
              definition.stats().stream()
                  .filter(x -> x.id().equals(sourceStat.get("id").asText()))
                  .findFirst()
                  .orElseThrow();
          assertThat(stat.min()).isEqualTo(sourceStat.get("min").asLong());
          assertThat(stat.max()).isEqualTo(sourceStat.get("max").asLong());
        }
        var magic =
            new ItemState(
                root.snapshotId(),
                root.baseItemId(),
                82,
                ItemState.Rarity.MAGIC,
                List.of(),
                List.of(),
                Set.of());
        var result =
            service.apply(magic, action, Set.of("Omen_of_Sinistral_Exaltation"), new Random(1));
        assertThat(result.applied()).isTrue();
        assertThat(result.state().rarity()).isEqualTo(ItemState.Rarity.RARE);
        assertThat(result.state().explicits())
            .extracting(ModifierInstance::modifierId)
            .containsExactly(target);
        assertThat(result.state().implicits()).isEmpty();
        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().selectionProbability()).isEqualTo(1);
        assertThat(result.remainingOmens()).containsExactly("Omen_of_Sinistral_Exaltation");
        assertThat(result.consumedOmens()).isEmpty();
        var overlap =
            new ItemState(
                root.snapshotId(),
                root.baseItemId(),
                82,
                ItemState.Rarity.MAGIC,
                List.of(),
                result.state().explicits(),
                Set.of());
        var low =
            new ItemState(
                root.snapshotId(),
                root.baseItemId(),
                definition.requiredItemLevel() - 1,
                ItemState.Rarity.MAGIC,
                List.of(),
                List.of(),
                Set.of());
        for (var blocked : List.of(root, result.state(), overlap, low)) {
          var denied = service.apply(blocked, action, Set.of(), new Random(1));
          assertThat(denied.applied()).isFalse();
          assertThat(denied.state()).isEqualTo(blocked);
          assertThat(denied.events()).isEmpty();
        }
      }
    }
    assertThat(service.actions(root, Set.of())).hasSize(50);
    assertThat(
            service.actions(
                new ItemState(
                    service.initial("solar", 82).state().snapshotId(),
                    SolarAmulet.BASE_ID,
                    82,
                    ItemState.Rarity.NORMAL,
                    SolarAmulet.initial(ItemCatalogLoader.loadDefault(), 82, 15).implicits(),
                    List.of(),
                    Set.of()),
                Set.of()))
        .hasSize(49);
    assertThat(catalog.modifiers()).hasSize(188);
  }
}
