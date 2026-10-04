package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class SapphireCatalystTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadSapphire();
  final WorkbenchSimulator sim = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));

  ItemState item(ItemState.Rarity rarity, List<ModifierInstance> affixes) {
    return new ItemState(
        catalog.metadata().snapshotId(),
        SapphireJewel.BASE_ID,
        82,
        rarity,
        List.of(),
        affixes,
        Set.of());
  }

  @Test
  void allThirteenRefinedTypesApplyRepeatReplaceAndPreserveOriginalRolls() {
    var suffix =
        new ModifierInstance(SapphireJewel.CAST_SPEED_ID, Map.of("display_cast_speed_percent", 3L));
    for (var rarity : List.of(ItemState.Rarity.MAGIC, ItemState.Rarity.RARE)) {
      var before = item(rarity, List.of(suffix));
      for (var type : CatalystQuality.Type.values()) {
        var action = WorkbenchCurrency.valueOf("REFINED_CATALYST_" + type);
        var applied =
            sim.apply(before, action, Set.of("Omen_of_Sinistral_Exaltation"), new Random(1));
        assertThat(applied.applied()).isTrue();
        assertThat(applied.state().catalystQuality()).isEqualTo(new CatalystQuality(type, 20));
        assertThat(applied.state().explicits()).isEqualTo(before.explicits());
        assertThat(applied.consumedOmens()).isEmpty();
        assertThat(applied.events()).isEmpty();
        assertThat(sim.apply(applied.state(), action, Set.of(), new Random(1)).state())
            .isEqualTo(applied.state());
        var projection = CatalystQualityDisplay.describe(applied.state(), catalog).getFirst();
        boolean matches =
            type == CatalystQuality.Type.SIBILANT || type == CatalystQuality.Type.SKITTERING;
        assertThat(projection.status()).isEqualTo(matches ? "SCALED_INTEGER" : "NO_MATCH");
        assertThat(projection.originalValues()).isEqualTo(suffix.values());
        assertThat(projection.displayedValues())
            .containsEntry("display_cast_speed_percent", matches ? 4L : 3L);
        before = applied.state();
      }
    }
  }

  @Test
  void emptyJewelHasNoInventedMatchingAffixAndOrdinaryCatalystsRemainUnavailable() {
    var root = item(ItemState.Rarity.MAGIC, List.of());
    assertThat(catalog.modifiers().values().stream().filter(d -> d.weight() > 0)).hasSize(58);
    for (var a : WorkbenchCurrency.values()) {
      if (a.catalystType() == null) continue;
      var result = sim.apply(root, a, Set.of(), new Random(1));
      assertThat(result.applied()).isEqualTo(a.refinedCatalyst());
      assertThat(result.state().explicits()).isEmpty();
    }
    assertThat(CatalystQualityDisplay.describe(root, catalog)).isEmpty();
    var ring = ItemCatalogLoader.loadRing();
    var ringRoot =
        new ItemState(
            ring.metadata().snapshotId(),
            ring.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(
                new ModifierInstance(
                    ring.base().implicitModifierId(),
                    Map.of(
                        "attack_minimum_added_physical_damage",
                        1L,
                        "attack_maximum_added_physical_damage",
                        4L))),
            List.of(),
            Set.of());
    var ringSim = new WorkbenchSimulator(ring, new CraftingEngine(ring));
    for (var type : CatalystQuality.Type.values())
      assertThat(
              ringSim
                  .apply(
                      ringRoot,
                      WorkbenchCurrency.valueOf("REFINED_CATALYST_" + type),
                      Set.of(),
                      new Random(1))
                  .applied())
          .isFalse();
  }

  @Test
  void uniqueSpecialAndAboveCapStatesAreNotSupported() {
    assertThat(new ItemStateValidator(catalog).validate(item(ItemState.Rarity.NORMAL, List.of())))
        .isEmpty();
    for (var rarity : List.of(ItemState.Rarity.UNIQUE))
      assertThat(new ItemStateValidator(catalog).validate(item(rarity, List.of()))).isNotEmpty();
    var root = item(ItemState.Rarity.MAGIC, List.of());
    var overcap =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            root.rarity(),
            List.of(),
            List.of(),
            Set.of(),
            null,
            new CatalystQuality(CatalystQuality.Type.FLESH, 21));
    assertThatThrownBy(
            () ->
                sim.apply(
                    overcap, WorkbenchCurrency.REFINED_CATALYST_FLESH, Set.of(), new Random(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(QualityLimitRules.describe(root, catalog).maximumQuality()).isEqualTo(20);
  }
}
