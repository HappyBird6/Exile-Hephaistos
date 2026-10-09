package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.AugmentSocketRules;
import com.poe2craft.crafting.domain.QualityLimitRules;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReviewedCrossbowsTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "siege-crossbow",
        "gemini-crossbow",
        "elegant-crossbow",
        "flexed-crossbow",
        "desolate-crossbow",
        "engraved-crossbow"
      })
  void distinctClassPoolImplicitsAndRestrictions(String key) {
    var c = ItemCatalogLoader.loadTopBase(key);
    assertThat(c.base().id()).isEqualTo(ReviewedCrossbows.BASES.get(key));
    assertThat(c.base().id()).contains("/Crossbows/").endsWith("Endgame");
    assertThat(c.modifiers().values().stream().filter(d -> d.weight() > 0)).hasSize(146);
    assertThat(
            c.modifiers().values().stream()
                .filter(d -> d.weight() == 0 && d.layer() == ModifierDefinition.Layer.EXPLICIT))
        .hasSize(8);
    assertThat(c.modifiers().values())
        .anyMatch(
            d ->
                d.stats()
                    .contains(
                        new ModifierDefinition.StatRange("base_number_of_crossbow_bolts", 2, 2)));
    assertThat(c.modifiers().values())
        .anyMatch(
            d ->
                d.stats()
                    .contains(
                        new ModifierDefinition.StatRange("base_number_of_crossbow_bolts", 1, 1)));
    var implicits =
        c.base().hasImplicit()
            ? List.of(
                new ModifierInstance(
                    c.base().implicitModifierId(),
                    c.find(c.base().implicitModifierId()).orElseThrow().stats().stream()
                        .collect(
                            java.util.stream.Collectors.toMap(
                                ModifierDefinition.StatRange::id,
                                ModifierDefinition.StatRange::max))))
            : List.<ModifierInstance>of();
    var state =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            implicits,
            List.of(),
            Set.of());
    assertThat(new ItemStateValidator(c).validate(state)).isEmpty();
    assertThat(QualityLimitRules.describe(state, c).maximumQuality()).isEqualTo(20);
    assertThat(CatalystQuality.supportedBase(c.base().id())).isFalse();
    assertThat(AugmentSocketRules.supportedState(state)).isTrue();
    assertThat(AugmentSocketRules.refusal(state)).isNotEmpty();
    var socketed =
        new ItemState(
            state.snapshotId(),
            state.baseItemId(),
            82,
            state.rarity(),
            state.implicits(),
            List.of(),
            Set.of(),
            1);
    assertThat(AugmentSocketRules.supportedState(socketed)).isFalse();
    assertThat(c.base().hasImplicit())
        .isEqualTo(!Set.of("desolate-crossbow", "engraved-crossbow").contains(key));
    if (key.equals("gemini-crossbow"))
      assertThat(implicits.getFirst().values()).containsEntry("base_number_of_crossbow_bolts", 1L);
    if (key.equals("siege-crossbow"))
      assertThat(implicits.getFirst().values())
          .containsEntry("grenade_skill_number_of_additional_projectiles", 1L);
    assertThat(c.modifiers().values())
        .noneMatch(d -> d.id().contains("alloy") || d.id().equals(QualityLimitRules.BREACH_ID));
  }
}
