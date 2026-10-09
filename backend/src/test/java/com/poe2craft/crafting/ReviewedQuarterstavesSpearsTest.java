package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.AugmentSocketRules;
import com.poe2craft.crafting.domain.QualityLimitRules;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReviewedQuarterstavesSpearsTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "aegis-quarterstaff",
        "bolting-quarterstaff",
        "dreaming-quarterstaff",
        "grand-spear",
        "flying-spear",
        "akoyan-spear"
      })
  void completeIndependentPoolsAndCanonicalImplicits(String key) {
    var catalog = ItemCatalogLoader.loadTopBase(key);
    assertThat(catalog.base().id()).isEqualTo(ReviewedQuarterstavesSpears.BASES.get(key));
    assertThat(catalog.modifiers().values().stream().filter(d -> d.weight() > 0))
        .hasSize(key.endsWith("quarterstaff") ? 158 : 162);
    assertThat(catalog.modifiers().values())
        .noneMatch(
            d -> d.stats().stream().anyMatch(s -> s.id().equals("base_number_of_crossbow_bolts")));
    var implicits = new ArrayList<ModifierInstance>();
    if (catalog.base().hasImplicit()) {
      var implicit = catalog.find(catalog.base().implicitModifierId()).orElseThrow();
      var values = new HashMap<String, Long>();
      implicit.stats().forEach(s -> values.put(s.id(), s.max()));
      implicits.add(new ModifierInstance(implicit.id(), values));
      if (key.equals("aegis-quarterstaff"))
        assertThat(values).containsEntry("additional_block_%", 18L);
      if (key.equals("bolting-quarterstaff"))
        assertThat(values)
            .containsEntry("local_weapon_implicit_hidden_added_maximum_lightning_damage", 100L)
            .containsEntry("local_weapon_implicit_hidden_added_minimum_lightning_damage", 1L);
      if (key.endsWith("spear"))
        assertThat(values).containsEntry("local_display_grants_spear_throw_skill", 1L);
      if (key.equals("flying-spear"))
        assertThat(values).containsEntry("local_projectile_speed_+%", 35L);
    } else assertThat(key).isEqualTo("dreaming-quarterstaff");
    var state =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            1,
            ItemState.Rarity.NORMAL,
            implicits,
            List.of(),
            Set.of());
    assertThat(new ItemStateValidator(catalog).validate(state)).isEmpty();
    assertThat(QualityLimitRules.describe(state, catalog).maximumQuality()).isEqualTo(20);
    assertThat(CatalystQuality.supportedBase(state.baseItemId())).isFalse();
    assertThat(AugmentSocketRules.refusal(state)).isNotEmpty();
  }
}
