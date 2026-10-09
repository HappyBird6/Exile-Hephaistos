package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReviewedWandsTest {
  @Test
  void allNineSkillFamiliesHaveCompleteTagFilteredPoolsAndStableModifierIdentities() {
    var old = ItemCatalogLoader.loadWand();
    for (var key : ReviewedWands.BASES.keySet()) {
      var c = ItemCatalogLoader.loadTopBase(key);
      assertThat(c.base().id()).isEqualTo(ReviewedWands.BASES.get(key));
      assertThat(c.base().hasImplicit()).isFalse();
      int expected =
          switch (key) {
            case "bone", "offering", "primordial" -> 118;
            case "volatile", "galvanic" -> 123;
            default -> 185;
          };
      assertThat(c.modifiers().values().stream().filter(d -> d.weight() > 0)).hasSize(expected);
      for (var d : c.modifiers().values()) assertThat(d).isEqualTo(old.modifiers().get(d.id()));
      var root =
          new ItemState(
              c.metadata().snapshotId(),
              c.base().id(),
              82,
              ItemState.Rarity.NORMAL,
              List.of(),
              List.of(),
              Set.of());
      assertThat(QualityLimitRules.describe(root, c).maximumQuality()).isEqualTo(20);
      var coupled =
          c.modifiers().values().stream()
              .filter(d -> d.stats().size() > 1)
              .map(ModifierDefinition::id)
              .collect(java.util.stream.Collectors.toSet());
      var simulator =
          new WorkbenchSimulator(
              c,
              new CraftingEngine(c),
              coupled,
              WandEssenceTargets.VERIFIED,
              Map.of(
                  WorkbenchCurrency.PERFECT_ESSENCE_SORCERY,
                      List.of("attuned-wand:suffix:essence-spell-skill-level"),
                  WorkbenchCurrency.PERFECT_ESSENCE_ALACRITY,
                      List.of("attuned-wand:suffix:essence-mana-cost-efficiency")));
      assertThat(
              simulator
                  .apply(root, WorkbenchCurrency.TRANSMUTATION, Set.of(), new Random(19))
                  .applied())
          .isTrue();
      var magic =
          new ItemState(
              root.snapshotId(),
              root.baseItemId(),
              82,
              ItemState.Rarity.MAGIC,
              List.of(),
              List.of(),
              Set.of());
      for (var action : WandEssenceTargets.VERIFIED.keySet()) {
        var result = simulator.apply(magic, action, Set.of(), new Random(19));
        assertThat(result.applied()).isTrue();
        assertThat(result.state().explicits())
            .anyMatch(m -> WandEssenceTargets.VERIFIED.get(action).contains(m.modifierId()));
      }
      for (var action :
          List.of(
              WorkbenchCurrency.ESSENCE_COMMAND,
              WorkbenchCurrency.ESSENCE_BODY,
              WorkbenchCurrency.ARTIFICER)) {
        var result = simulator.apply(magic, action, Set.of(), new Random(19));
        assertThat(result.applied()).isFalse();
        assertThat(result.state()).isEqualTo(magic);
      }
    }
  }
}
