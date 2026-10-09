package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.AugmentSocketRules;
import com.poe2craft.crafting.domain.QualityLimitRules;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReviewedQuiversTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "visceral-quiver",
        "volant-quiver",
        "penetrating-quiver",
        "primed-quiver",
        "serrated-quiver",
        "toxic-quiver",
        "blunt-quiver",
        "two-point-quiver",
        "sacral-quiver",
        "fire-quiver",
        "broadhead-quiver"
      })
  void independentClassCompletePoolAndRestrictions(String key) {
    var catalog = ItemCatalogLoader.loadTopBase(key);
    assertThat(catalog.base().id()).isEqualTo(ReviewedQuivers.BASES.get(key)).contains("/Quivers/");
    assertThat(catalog.modifiers().values().stream().filter(d -> d.weight() > 0)).hasSize(100);
    assertThat(
            catalog.modifiers().values().stream()
                .filter(d -> d.layer() == ModifierDefinition.Layer.IMPLICIT))
        .hasSize(1);
    assertThat(catalog.modifiers().values())
        .noneMatch(d -> d.stats().stream().anyMatch(s -> s.id().equals("local_attack_speed_+%")));
    assertThat(catalog.modifiers().values())
        .anyMatch(
            d ->
                d.stats().stream()
                    .anyMatch(
                        s -> s.id().equals("attack_speed_+%") && s.min() == 5 && s.max() == 7));
    var implicit = catalog.find(catalog.base().implicitModifierId()).orElseThrow();
    var values = new HashMap<String, Long>();
    implicit.stats().forEach(s -> values.put(s.id(), s.max()));
    var state =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            1,
            ItemState.Rarity.NORMAL,
            List.of(new ModifierInstance(implicit.id(), values)),
            List.of(),
            Set.of());
    assertThat(new ItemStateValidator(catalog).validate(state)).isEmpty();
    assertThat(QualityLimitRules.describe(state, catalog)).isNull();
    assertThat(CatalystQuality.supportedBase(state.baseItemId())).isFalse();
    assertThat(AugmentSocketRules.refusal(state)).isNotEmpty();
    if (key.equals("penetrating-quiver"))
      assertThat(values).containsEntry("base_chance_to_pierce_%", 100L);
  }
}
