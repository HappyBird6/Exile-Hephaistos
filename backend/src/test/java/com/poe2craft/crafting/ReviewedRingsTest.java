package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.QualityLimitRules;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReviewedRingsTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "kinetic",
        "vitalic",
        "mnemonic",
        "pearl",
        "amethyst",
        "prismatic",
        "ruby-ring",
        "two-stone-fire-cold"
      })
  void distinctSourceImplicitCompletePoolAndCatalystProjection(String key) {
    var c = ItemCatalogLoader.loadTopBase(key);
    var legacy = ItemCatalogLoader.loadRing();
    assertThat(c.base().id()).isEqualTo(ReviewedRings.BASES.get(key));
    assertThat(c.metadata().snapshotId()).isNotEqualTo(legacy.metadata().snapshotId());
    for (var d : legacy.modifiers().values())
      if (d.layer() == ModifierDefinition.Layer.EXPLICIT) assertThat(c.find(d.id())).contains(d);
    assertThat(c.modifiers().values().stream().filter(d -> d.weight() > 0)).hasSize(203);
    assertThat(c.find("iron-ring:implicit:added-physical-damage-to-attacks")).isEmpty();
    var d = c.find(c.base().implicitModifierId()).orElseThrow();
    var values =
        d.stats().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    ModifierDefinition.StatRange::id, ModifierDefinition.StatRange::max));
    var implicit = new ModifierInstance(d.id(), values);
    for (var type : CatalystQuality.Type.values()) {
      var state =
          new ItemState(
              c.metadata().snapshotId(),
              c.base().id(),
              82,
              ItemState.Rarity.NORMAL,
              List.of(implicit),
              List.of(),
              Set.of(),
              null,
              new CatalystQuality(type, 20));
      assertThat(new ItemStateValidator(c).validate(state)).isEmpty();
      assertThat(QualityLimitRules.describe(state, c).maximumQuality()).isEqualTo(20);
      var p = CatalystQualityDisplay.describe(state, c).getFirst();
      assertThat(p.originalValues()).isEqualTo(values);
      assertThat(p.status()).isEqualTo(type.matches(d.tags()) ? "SCALED_INTEGER" : "NO_MATCH");
      for (var s : d.stats())
        assertThat(p.displayedValues().get(s.id()))
            .isEqualTo(
                type.matches(d.tags())
                    ? QualityRoundingPolicy.roundRatio(
                        java.math.BigInteger.valueOf(s.max() * 120),
                        java.math.BigInteger.valueOf(100))
                    : s.max());
    }
    var wrong =
        new ModifierInstance(
            legacy.base().implicitModifierId(),
            Map.of(
                "attack_minimum_added_physical_damage",
                1L,
                "attack_maximum_added_physical_damage",
                4L));
    var wrongState =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(wrong),
            List.of(),
            Set.of());
    assertThat(new ItemStateValidator(c).validate(wrongState)).isNotEmpty();
    var breach = c.find(QualityLimitRules.BREACH_ID).orElseThrow();
    var b = new ModifierInstance(breach.id(), Map.of(QualityLimitRules.STAT_ID, 20L));
    var high =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(implicit),
            List.of(b),
            Set.of(),
            null,
            new CatalystQuality(CatalystQuality.Type.REAVER, 40));
    assertThat(QualityLimitRules.describe(high, c).maximumQuality()).isEqualTo(40);
    var overflow =
        new ItemState(
            high.snapshotId(),
            high.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            high.implicits(),
            List.of(),
            Set.of(),
            null,
            high.catalystQuality());
    assertThat(new ItemStateValidator(c).validate(overflow)).isEmpty();
    assertThat(QualityLimitRules.describe(overflow, c).maximumQuality()).isEqualTo(20);
  }
}
