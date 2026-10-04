package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReviewedBowsTest {
  @ParameterizedTest
  @ValueSource(strings = {"warmonger", "guardian", "gemini", "fanatic", "obliterator"})
  void completePoolRetainsLegacyDefinitionsAndDistinctImplicitRanges(String key) {
    var c = ItemCatalogLoader.loadTopBase(key);
    var legacy = ItemCatalogLoader.loadBow();
    assertThat(c.base().id()).isEqualTo(ReviewedBows.BASES.get(key));
    assertThat(c.metadata().snapshotId()).isNotEqualTo(legacy.metadata().snapshotId());
    for (var d : legacy.modifiers().values()) assertThat(c.find(d.id())).contains(d);
    assertThat(
            c.modifiers().values().stream()
                .filter(d -> d.layer() == ModifierDefinition.Layer.EXPLICIT && d.weight() > 0))
        .hasSize(140);
    var implicit =
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
    var root =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            implicit,
            List.of(),
            Set.of());
    assertThat(new ItemStateValidator(c).validate(root)).isEmpty();
    if (key.equals("guardian")) {
      var stat = c.find(c.base().implicitModifierId()).orElseThrow().stats().getFirst();
      assertThat(stat.id()).isEqualTo("local_additional_attack_chain_chance_%");
      assertThat(stat.min()).isEqualTo(25);
      assertThat(stat.max()).isEqualTo(35);
    }
    if (key.equals("fanatic"))
      assertThat(implicit.getFirst().values())
          .containsEntry("local_weapon_implicit_hidden_added_minimum_chaos_damage", 28L)
          .containsEntry("local_weapon_implicit_hidden_added_maximum_chaos_damage", 64L);
    if (key.equals("obliterator"))
      assertThat(implicit.getFirst().values()).containsEntry("projectile_attack_range_+%", -50L);
    if (key.equals("warmonger")) assertThat(implicit).isEmpty();
    var glinting = c.find("crude-bow:prefix:glinting").orElseThrow();
    var honed = c.find("crude-bow:prefix:honed").orElseThrow();
    var conflict =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            implicit,
            List.of(instance(glinting), instance(honed)),
            Set.of());
    assertThat(new ItemStateValidator(c).validate(conflict))
        .anyMatch(v -> v.code() == ItemStateValidator.Code.CONFLICTING_MODIFIERS);
    var low =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            1,
            ItemState.Rarity.MAGIC,
            implicit,
            List.of(instance(honed)),
            Set.of());
    assertThat(new ItemStateValidator(c).validate(low)).isEmpty();
    assertThat(new ItemStateValidator(c).validateForGeneration(low))
        .anyMatch(v -> v.code() == ItemStateValidator.Code.ITEM_LEVEL_TOO_LOW);
  }

  private static ModifierInstance instance(ModifierDefinition d) {
    return new ModifierInstance(
        d.id(),
        d.stats().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    ModifierDefinition.StatRange::id, ModifierDefinition.StatRange::min)));
  }
}
