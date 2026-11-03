package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class QualityPlusOneTest {
  @Test
  void whetstoneUsesExactlyOnePointForEverySupportedRarityAndStopsAtTwenty() {
    var catalog = ItemCatalogLoader.loadBow();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    for (var rarity :
        List.of(ItemState.Rarity.NORMAL, ItemState.Rarity.MAGIC, ItemState.Rarity.RARE)) {
      for (int quality : List.of(0, 19, 20, 30)) {
        var state =
            new ItemState(
                    catalog.metadata().snapshotId(),
                    catalog.base().id(),
                    82,
                    rarity,
                    List.of(),
                    List.of(),
                    Set.of())
                .withEquipmentQuality(quality);
        var result =
            simulator.apply(
                state,
                WorkbenchCurrency.BLACKSMITHS_WHETSTONE,
                Set.of("Omen_of_Sanctification"),
                new Random(1));
        assertThat(result.applied()).isEqualTo(quality < 20);
        assertThat(result.state())
            .isEqualTo(quality < 20 ? state.withEquipmentQuality(quality + 1) : state);
        assertThat(result.consumedOmens()).isEmpty();
        assertThat(result.remainingOmens()).containsExactly("Omen_of_Sanctification");
        assertThat(result.events()).isEmpty();
        assertThat(result.assumptions()).hasSize(quality < 20 ? 1 : 0);
        assertThat(
                simulator
                    .apply(state, WorkbenchCurrency.ARMOURERS_SCRAP, Set.of(), new Random(1))
                    .applied())
            .isFalse();
      }
      var unknown =
          new ItemState(
              catalog.metadata().snapshotId(),
              catalog.base().id(),
              82,
              rarity,
              List.of(),
              List.of(),
              Set.of());
      assertThat(
              simulator
                  .apply(unknown, WorkbenchCurrency.BLACKSMITHS_WHETSTONE, Set.of(), new Random(1))
                  .applied())
          .isFalse();
    }
  }

  @Test
  void qualityPreservesUnrevealedFracturedObservedStateAndAllPendingOmens() {
    var fixture = new StockyHysteriaTest();
    var definition =
        fixture.catalog.modifiers().values().stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX && d.weight() > 0)
            .findFirst()
            .orElseThrow();
    var values = new HashMap<String, Long>();
    definition.stats().forEach(s -> values.put(s.id(), s.min()));
    var state =
        new ItemState(
            fixture.root.snapshotId(),
            fixture.root.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            List.of(new ModifierInstance(definition.id(), values, true)),
            Set.of(),
            0,
            null,
            List.of(
                new ItemState.DesecratedSlot(
                    "unrevealed", ModifierDefinition.AffixType.SUFFIX, null)),
            7,
            new ItemObservations(0, List.of("Preserved installed Augment effect"), List.of()));
    var omens = Set.of("Omen_of_Sanctification", "Omen_of_Whittling");
    var result =
        fixture.simulator.apply(state, WorkbenchCurrency.ARMOURERS_SCRAP, omens, new Random(1));
    assertThat(result.applied()).isTrue();
    assertThat(result.state()).isEqualTo(state.withEquipmentQuality(8));
    assertThat(result.consumedOmens()).isEmpty();
    assertThat(result.remainingOmens()).containsExactlyInAnyOrderElementsOf(omens);
    var sanctified =
        state.withSanctification(QualitySanctification.roll(state, fixture.catalog, new Random(1)));
    var blocked =
        fixture.simulator.apply(
            sanctified, WorkbenchCurrency.ARMOURERS_SCRAP, omens, new Random(1));
    assertThat(blocked.applied()).isFalse();
    assertThat(blocked.state()).isEqualTo(sanctified);
    assertThat(blocked.consumedOmens()).isEmpty();
  }

  @Test
  void whetstoneRejectsCasterWeaponsAndJewellery() {
    for (var id :
        List.of(
            "Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3",
            "Metadata/Items/Weapons/TwoHandWeapons/Staves/FourStaff1",
            "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1",
            "Metadata/Items/Amulets/FourAmulet9")) {
      var state =
          new ItemState("fixture", id, 82, ItemState.Rarity.NORMAL, List.of(), List.of(), Set.of())
              .withEquipmentQuality(0);
      assertThat(QualityCurrencyRules.refusal(state, WorkbenchCurrency.BLACKSMITHS_WHETSTONE))
          .isNotEmpty();
    }
  }
}
