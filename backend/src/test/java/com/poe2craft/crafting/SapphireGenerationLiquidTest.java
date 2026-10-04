package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class SapphireGenerationLiquidTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadSapphire();
  final WorkbenchSimulator simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));

  ItemState item(ItemState.Rarity rarity, List<ModifierInstance> modifiers) {
    return new ItemState(
        catalog.metadata().snapshotId(),
        SapphireJewel.BASE_ID,
        82,
        rarity,
        List.of(),
        modifiers,
        Set.of(),
        null,
        new CatalystQuality(CatalystQuality.Type.SIBILANT, 20));
  }

  ModifierInstance minimum(String id) {
    var d = catalog.find(id).orElseThrow();
    var values = new HashMap<String, Long>();
    d.stats().forEach(s -> values.put(s.id(), s.min()));
    return new ModifierInstance(id, values);
  }

  @Test
  void exactOrdinaryPoolExcludesEveryCraftedAndSpecialSection() {
    var ordinary = catalog.modifiers().values().stream().filter(d -> d.weight() > 0).toList();
    assertThat(ordinary).hasSize(58);
    assertThat(ordinary.stream().filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX))
        .hasSize(23);
    assertThat(ordinary)
        .allSatisfy(
            d -> {
              assertThat(d.weight()).isEqualTo(1);
              assertThat(d.requiredItemLevel()).isEqualTo(1);
              assertThat(d.tags()).doesNotContain("crafted");
            });
    assertThat(catalog.metadata().weightPolicy())
        .isEqualTo("USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS");
    assertThat(catalog.modifiers().values().stream().filter(d -> d.tags().contains("crafted")))
        .hasSize(15);
  }

  @Test
  void allEighteenBasicCurrenciesHavePositivePathsIncludingTierException() {
    for (var action : WorkbenchCurrency.values()) {
      if (action.baseAction() == null
          && action != WorkbenchCurrency.ALCHEMY
          && action != WorkbenchCurrency.DIVINE) continue;
      var rarity =
          action.baseAction() == CraftingAction.TRANSMUTATION || action == WorkbenchCurrency.ALCHEMY
              ? ItemState.Rarity.NORMAL
              : action.baseAction() == CraftingAction.AUGMENTATION
                      || action.baseAction() == CraftingAction.REGAL
                  ? ItemState.Rarity.MAGIC
                  : ItemState.Rarity.RARE;
      var explicits =
          rarity == ItemState.Rarity.NORMAL
              ? List.<ModifierInstance>of()
              : List.of(minimum(SapphireJewel.CAST_SPEED_ID));
      var before = item(rarity, explicits);
      var result = simulator.apply(before, action, Set.of(), new Random(3));
      assertThat(result.applied()).as(action.name()).isTrue();
      assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
      assertThat(result.state().catalystQuality()).isEqualTo(before.catalystQuality());
      if (action == WorkbenchCurrency.ALCHEMY) assertThat(result.state().explicits()).hasSize(4);
      if (action.baseAction() != CraftingAction.ANNULMENT && action != WorkbenchCurrency.DIVINE)
        assertThat(result.assumptions())
            .extracting(WorkbenchSimulator.Assumption::id)
            .contains("sapphire-uniform-candidates-v1");
    }
  }

  @Test
  void magicAndRareFillOnlyTheirTwoOrFourSlotsAndFamilyOverlapsAreExcluded() {
    for (int seed = 0; seed < 100; seed++) {
      var normal = item(ItemState.Rarity.NORMAL, List.of());
      var magic =
          simulator
              .apply(normal, WorkbenchCurrency.TRANSMUTATION, Set.of(), new Random(seed))
              .state();
      magic =
          simulator
              .apply(magic, WorkbenchCurrency.AUGMENTATION, Set.of(), new Random(seed))
              .state();
      assertThat(magic.explicits()).hasSize(2);
      assertThat(
              simulator
                  .apply(magic, WorkbenchCurrency.AUGMENTATION, Set.of(), new Random(seed))
                  .applied())
          .isFalse();
      var rare =
          simulator.apply(magic, WorkbenchCurrency.REGAL, Set.of(), new Random(seed)).state();
      rare = simulator.apply(rare, WorkbenchCurrency.EXALTED, Set.of(), new Random(seed)).state();
      assertThat(rare.explicits()).hasSize(4);
      assertThat(new ItemStateValidator(catalog).validate(rare)).isEmpty();
      assertThat(
              simulator
                  .apply(rare, WorkbenchCurrency.EXALTED, Set.of(), new Random(seed))
                  .applied())
          .isFalse();
    }
    var bestial = item(ItemState.Rarity.RARE, List.of(minimum("sapphire:prefix:bestial")));
    assertThat(simulator.pool(bestial, WorkbenchCurrency.EXALTED, null))
        .extracting(ModifierDefinition::id)
        .doesNotContain("sapphire:prefix:overgrown");
  }

  @Test
  void allTenLiquidsReplaceLegalFullRareBranchesAndPersistCraftedThroughDivine() {
    var full =
        item(
            ItemState.Rarity.RARE,
            List.of(
                minimum("sapphire:prefix:shimmering"),
                minimum("sapphire:prefix:chilling"),
                minimum(SapphireJewel.CAST_SPEED_ID),
                minimum("sapphire:suffix:of-unmaking")));
    for (var action : WorkbenchCurrency.values()) {
      if (!action.isLiquid()) continue;
      var result =
          simulator.apply(full, action, Set.of("Omen_of_Sinistral_Crystallisation"), new Random(2));
      assertThat(result.applied()).as(action.name()).isTrue();
      assertThat(result.state().explicits()).hasSize(4);
      assertThat(result.consumedOmens()).isEmpty();
      assertThat(result.remainingOmens()).containsExactly("Omen_of_Sinistral_Crystallisation");
      assertThat(result.events())
          .extracting(WorkbenchSimulator.Event::kind)
          .containsExactly("REMOVE", "ADD");
      var target = result.events().getLast().modifierId();
      assertThat(action.replacementModifiers()).contains(target);
      assertThat(result.state().explicits())
          .extracting(ModifierInstance::modifierId)
          .contains(target);
      assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
      var repeat = simulator.apply(result.state(), action, Set.of(), new Random(2));
      assertThat(repeat.applied()).isFalse();
      assertThat(repeat.state()).isEqualTo(result.state());
      var divine =
          simulator.apply(result.state(), WorkbenchCurrency.DIVINE, Set.of(), new Random(4));
      assertThat(divine.state().explicits())
          .extracting(ModifierInstance::modifierId)
          .contains(target);
      assertThat(divine.state().catalystQuality()).isEqualTo(full.catalystQuality());
      var onlyCrafted = item(ItemState.Rarity.RARE, List.of(minimum(target)));
      assertThat(
              simulator
                  .apply(onlyCrafted, WorkbenchCurrency.ANNULMENT, Set.of(), new Random(4))
                  .state()
                  .explicits())
          .isEmpty();
      assertThat(
              simulator
                  .apply(
                      item(ItemState.Rarity.MAGIC, List.of(minimum(SapphireJewel.CAST_SPEED_ID))),
                      action,
                      Set.of(),
                      new Random(4))
                  .applied())
          .isFalse();
    }
  }
}
