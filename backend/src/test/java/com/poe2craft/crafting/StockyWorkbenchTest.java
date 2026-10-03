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
        var details = getClass().getResourceAsStream("/catalog/stocky-mitts/details.raw.json")) {
      return ItemCatalogLoader.load(data, raw, details);
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
      assertThat(rare.ruleVersion()).isEqualTo("stocky-workbench-source-model-v17");
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
            WorkbenchCurrency.ESSENCE_BODY,
            WorkbenchCurrency.RUNIC_ALLOY,
            WorkbenchCurrency.ESSENCE_ABYSS)) {
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
    assertThat(catalog.modifiers()).hasSize(182);
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
}
