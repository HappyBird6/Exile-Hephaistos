package com.poe2craft.item;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class CatalystQualityTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final ItemState root = SolarAmulet.initial(catalog);

  ItemState state(CatalystQuality.Type type, int amount, List<ModifierInstance> modifiers) {
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        82,
        ItemState.Rarity.RARE,
        root.implicits(),
        modifiers,
        Set.of(),
        null,
        new CatalystQuality(type, amount));
  }

  @Test
  void originalRollsStayInSourceBoundsWhileDerivedIntegerDisplayTruncates() {
    var item =
        state(
            CatalystQuality.Type.FLESH,
            20,
            List.of(
                new ModifierInstance("amulet:prefix:healthy", Map.of("base_maximum_life", 29L))));
    var copy = item.explicits();
    var result =
        CatalystQualityDisplay.describe(item, catalog).stream()
            .filter(m -> m.modifierId().equals("amulet:prefix:healthy"))
            .findFirst()
            .orElseThrow();
    assertThat(result.originalValues()).containsEntry("base_maximum_life", 29L);
    assertThat(result.displayedValues()).containsEntry("base_maximum_life", 34L);
    assertThat(result.status()).isEqualTo("SCALED_INTEGER");
    assertThat(item.explicits()).isEqualTo(copy);
    assertThat(new ItemStateValidator(catalog).validate(item)).isEmpty();
    var different = state(CatalystQuality.Type.NEURAL, 20, copy);
    assertThat(
            CatalystQualityDisplay.describe(different, catalog).stream()
                .filter(m -> m.modifierId().equals("amulet:prefix:healthy"))
                .findFirst()
                .orElseThrow()
                .status())
        .isEqualTo("NO_MATCH");
  }

  @Test
  void singleActiveTypeTagsUseAnyMatchOnceAndNeverInventMissingTags() {
    assertThat(CatalystQuality.Type.values()).hasSize(13);
    assertThat(CatalystQuality.Type.CARAPACE.matches(Set.of("defences", "armour", "energyshield")))
        .isTrue();
    for (var type : CatalystQuality.Type.values()) assertThat(type.matches(Set.of())).isFalse();
    assertThatThrownBy(() -> new CatalystQuality(null, 20))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new CatalystQuality(CatalystQuality.Type.FLESH, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void breachCapIsUnscalableAndAboveDefaultNeedsTheActualRetainedBreach() {
    var breach =
        new ModifierInstance(
            "amulet:prefix:essence-maximum-quality", Map.of("local_maximum_quality_+", 20L));
    var item = state(CatalystQuality.Type.FLESH, 40, List.of(breach));
    assertThat(new ItemStateValidator(catalog).validate(item)).isEmpty();
    assertThat(QualityLimitRules.describe(item, catalog).maximumQuality()).isEqualTo(40);
    assertThat(
            CatalystQualityDisplay.describe(item, catalog).stream()
                .filter(m -> m.modifierId().equals(breach.modifierId()))
                .findFirst()
                .orElseThrow()
                .status())
        .isEqualTo("UNSCALABLE");
    assertThat(
            new ItemStateValidator(catalog)
                .validate(state(CatalystQuality.Type.FLESH, 21, List.of())))
        .isNotEmpty();
  }

  @Test
  void refusalPreservesQualityOriginalsOmensAndDoesNotDrawRandomnessOrProjectLossily() {
    var item = state(CatalystQuality.Type.FLESH, 20, List.of());
    var sim = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var random =
        new Random() {
          @Override
          public long nextLong(long bound) {
            throw new AssertionError("Must not roll");
          }
        };
    var result =
        sim.apply(item, WorkbenchCurrency.EXALTED, Set.of("Omen_of_Sinistral_Exaltation"), random);
    assertThat(result.applied()).isFalse();
    assertThat(result.state()).isSameAs(item);
    assertThat(result.events()).isEmpty();
    assertThat(result.consumedOmens()).isEmpty();
    assertThat(result.remainingOmens()).containsExactly("Omen_of_Sinistral_Exaltation");
    assertThat(sim.actions(item, Set.of())).allMatch(a -> !a.available());
    assertThatThrownBy(() -> StateBucket.from(item)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ringIntegerImplicitCanBeDisplayedWhileBeltCapStaysUnknown() {
    var ring = ItemCatalogLoader.loadRing();
    var item =
        new ItemState(
            ring.metadata().snapshotId(),
            ring.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(
                new ModifierInstance(
                    "iron-ring:implicit:added-physical-damage-to-attacks",
                    Map.of(
                        "attack_minimum_added_physical_damage",
                        1L,
                        "attack_maximum_added_physical_damage",
                        4L))),
            List.of(),
            Set.of(),
            null,
            new CatalystQuality(CatalystQuality.Type.REAVER, 20));
    assertThat(QualityLimitRules.describe(item, ring).maximumQuality()).isEqualTo(20);
    assertThat(CatalystQualityDisplay.describe(item, ring).getFirst().status())
        .isEqualTo("SCALED_INTEGER");
    var belt = ItemCatalogLoader.loadBelt();
    var plain =
        new ItemState(
            belt.metadata().snapshotId(),
            belt.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(),
            List.of(),
            Set.of());
    assertThat(QualityLimitRules.describe(plain, belt)).isNull();
    var withQuality =
        new ItemState(
            plain.snapshotId(),
            plain.baseItemId(),
            82,
            plain.rarity(),
            plain.implicits(),
            plain.explicits(),
            Set.of(),
            null,
            item.catalystQuality());
    assertThat(new ItemStateValidator(belt).validate(withQuality)).isNotEmpty();
  }
}
