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
  void originalRollsStayInSourceBoundsWhileDerivedIntegerDisplayRoundsProvisionally() {
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
    assertThat(result.displayedValues()).containsEntry("base_maximum_life", 35L);
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
  void catalystSetsMaximumReplacesTypeAndPreservesOriginalsAndOmensWithoutDrawing() {
    var item =
        state(
            CatalystQuality.Type.FLESH,
            7,
            List.of(
                new ModifierInstance("amulet:prefix:healthy", Map.of("base_maximum_life", 29L))));
    var sim = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var random =
        new Random() {
          @Override
          public long nextLong(long bound) {
            throw new AssertionError("Must not roll");
          }

          @Override
          public int nextInt(int bound) {
            throw new AssertionError("Must not roll");
          }
        };
    for (var type : CatalystQuality.Type.values()) {
      var result =
          sim.apply(
              item,
              WorkbenchCurrency.valueOf("CATALYST_" + type),
              Set.of("Omen_of_Sinistral_Exaltation"),
              random);
      assertThat(result.applied()).isTrue();
      assertThat(result.state().catalystQuality()).isEqualTo(new CatalystQuality(type, 20));
      assertThat(result.state().explicits()).isEqualTo(item.explicits());
      assertThat(result.events()).isEmpty();
      assertThat(result.consumedOmens()).isEmpty();
      assertThat(result.remainingOmens()).containsExactly("Omen_of_Sinistral_Exaltation");
      var repeated =
          sim.apply(
              result.state(), WorkbenchCurrency.valueOf("CATALYST_" + type), Set.of(), random);
      assertThat(repeated.state()).isEqualTo(result.state());
      assertThat(
              sim.apply(
                      item, WorkbenchCurrency.valueOf("REFINED_CATALYST_" + type), Set.of(), random)
                  .applied())
          .isFalse();
    }
    assertThatThrownBy(() -> StateBucket.from(item)).isInstanceOf(IllegalArgumentException.class);
    var added = sim.apply(item, WorkbenchCurrency.EXALTED, Set.of(), new Random(1));
    assertThat(added.applied()).isTrue();
    assertThat(added.state().catalystQuality()).isEqualTo(item.catalystQuality());
    assertThat(added.state().explicits()).containsAll(item.explicits());
  }

  @Test
  void provisionalRoundingUsesHalfUpForSignedRatios() {
    var denominator = java.math.BigInteger.valueOf(100);
    assertThat(QualityRoundingPolicy.roundRatio(java.math.BigInteger.valueOf(3480), denominator))
        .isEqualTo(35);
    assertThat(QualityRoundingPolicy.roundRatio(java.math.BigInteger.valueOf(150), denominator))
        .isEqualTo(2);
    assertThat(QualityRoundingPolicy.roundRatio(java.math.BigInteger.valueOf(-150), denominator))
        .isEqualTo(-2);
    assertThat(QualityRoundingPolicy.roundRatio(java.math.BigInteger.valueOf(4060), denominator))
        .isEqualTo(41);
  }

  @Test
  void maximumCapRemovalClampsUnderDeclaredSimulatorPolicy() {
    var breach =
        new ModifierInstance(QualityLimitRules.BREACH_ID, Map.of(QualityLimitRules.STAT_ID, 20L));
    var item = state(CatalystQuality.Type.FLESH, 20, List.of(breach));
    var sim = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var result = sim.apply(item, WorkbenchCurrency.CATALYST_NEURAL, Set.of(), new Random(1));
    assertThat(result.state().catalystQuality().amount()).isEqualTo(40);
    assertThat(result.state().explicits()).containsExactly(breach);
    assertThat(
            sim.apply(result.state(), WorkbenchCurrency.ANNULMENT, Set.of(), new Random(1))
                .applied())
        .isTrue();
    assertThat(sim.apply(item, WorkbenchCurrency.ANNULMENT, Set.of(), new Random(1)).applied())
        .isTrue();
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
