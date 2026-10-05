package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.QualityLimitRules;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReviewedBeltsTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "linen-belt",
        "wide-belt",
        "long-belt",
        "plate-belt",
        "ornate-belt",
        "mail-belt",
        "double-belt",
        "heavy-belt",
        "utility-belt",
        "fine-belt",
        "invoking-belt",
        "sinew-belt",
        "forking-belt"
      })
  void sourceIdentityRangesCompletePoolAndClassRestrictions(String key) {
    var c = ItemCatalogLoader.loadTopBase(key);
    var legacy = ItemCatalogLoader.loadBelt();
    assertThat(c.base().id()).isEqualTo(ReviewedBelts.BASES.get(key));
    assertThat(c.base().id()).doesNotContain("Unique");
    assertThat(c.metadata().snapshotId()).isNotEqualTo(legacy.metadata().snapshotId());
    for (var d : legacy.modifiers().values()) assertThat(c.find(d.id())).contains(d);
    assertThat(c.modifiers().values().stream().filter(d -> d.weight() > 0)).hasSize(135);
    var d = c.find(c.base().implicitModifierId()).orElseThrow();
    var values =
        d.stats().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    ModifierDefinition.StatRange::id, ModifierDefinition.StatRange::max));
    var state =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(new ModifierInstance(d.id(), values)),
            List.of(),
            Set.of());
    assertThat(new ItemStateValidator(c).validate(state)).isEmpty();
    assertThat(QualityLimitRules.describe(state, c)).isNull();
    assertThat(CatalystQuality.supportedBase(c.base().id())).isFalse();
    var withCatalyst =
        new ItemState(
            state.snapshotId(),
            state.baseItemId(),
            82,
            state.rarity(),
            state.implicits(),
            List.of(),
            Set.of(),
            null,
            new CatalystQuality(CatalystQuality.Type.FLESH, 20));
    assertThat(new ItemStateValidator(c).validate(withCatalyst)).isNotEmpty();
    if (Set.of("invoking-belt", "sinew-belt", "forking-belt").contains(key)) {
      assertThat(d.stats()).contains(new ModifierDefinition.StatRange("local_charm_slots", 1, 1));
      assertThat(d.familyIds()).contains("AdditionalCharm");
    } else assertThat(d.stats()).noneMatch(s -> s.id().equals("local_charm_slots"));
    if (key.equals("fine-belt")) {
      assertThat(d.stats())
          .containsExactly(
              new ModifierDefinition.StatRange(
                  "generate_x_charges_for_any_flask_per_minute", 10, 10));
      assertThat(d.text()).contains("0.17");
    }
    if (Set.of("ornate-belt", "mail-belt").contains(key))
      assertThat(d.stats().getFirst().min()).isEqualTo(-15);
    assertThat(c.modifiers().values())
        .noneMatch(m -> m.id().contains("alloy") || m.id().equals(QualityLimitRules.BREACH_ID));
  }
}
