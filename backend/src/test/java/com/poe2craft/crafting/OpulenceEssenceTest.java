package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpulenceEssenceTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final WorkbenchSimulator simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  final ItemState root = SolarAmulet.initial(catalog);
  final List<WorkbenchCurrency> actions =
      List.of(
          WorkbenchCurrency.LESSER_ESSENCE_OPULENCE,
          WorkbenchCurrency.ESSENCE_OPULENCE,
          WorkbenchCurrency.GREATER_ESSENCE_OPULENCE);

  ItemState magic(int level, String... ids) {
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        level,
        ItemState.Rarity.MAGIC,
        root.implicits(),
        Arrays.stream(ids)
            .map(
                id -> {
                  var stat = catalog.find(id).orElseThrow().stats().getFirst();
                  return new ModifierInstance(id, Map.of(stat.id(), stat.min()));
                })
            .toList(),
        root.conditions());
  }

  @Test
  void fixedRaritySuffixesPreserveExistingModifiersIncludingSeparateRarityPrefix() {
    long[][] ranges = {{6, 10}, {11, 14}, {15, 18}};
    for (int index = 0; index < actions.size(); index++) {
      var action = actions.get(index);
      var definition = catalog.find(action.fixedModifierId()).orElseThrow();
      assertThat(definition.affixType()).isEqualTo(ModifierDefinition.AffixType.SUFFIX);
      assertThat(definition.stats().getFirst().min()).isEqualTo(ranges[index][0]);
      assertThat(definition.stats().getFirst().max()).isEqualTo(ranges[index][1]);
      for (var before :
          List.of(
              magic(82, "amulet:suffix:of-the-brute"),
              magic(82, "amulet:suffix:of-the-brute", "amulet:prefix:magpie-s")))
        for (int seed = 0; seed < 100; seed++) {
          var result =
              simulator.apply(
                  before, action, Set.of(WorkbenchOmen.BLESSED.id()), new Random(seed * 104729L));
          assertThat(result.applied()).isTrue();
          assertThat(result.state().rarity()).isEqualTo(ItemState.Rarity.RARE);
          assertThat(result.state().explicits())
              .hasSize(before.explicits().size() + 1)
              .containsAll(before.explicits());
          assertThat(result.state().implicits()).isEqualTo(before.implicits());
          assertThat(result.events()).hasSize(1);
          assertThat(result.events().getFirst().modifierId()).isEqualTo(definition.id());
          assertThat(result.events().getFirst().selectionProbability()).isEqualTo(1);
          assertThat(result.assumptions().getFirst().n())
              .isEqualTo(ranges[index][1] - ranges[index][0] + 1);
          assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
          assertThat(result.consumedOmens()).isEmpty();
          assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
        }
    }
  }

  @Test
  void catalogLevelBoundaryIsSupportedButLowerLevelsAndFamilyOverlapAreNotGuessed() {
    for (var action : actions) {
      var definition = catalog.find(action.fixedModifierId()).orElseThrow();
      var boundary = magic(definition.requiredItemLevel(), "amulet:suffix:of-the-brute");
      assertThat(simulator.apply(boundary, action, Set.of(), new Random(1)).applied()).isTrue();
      var below = magic(definition.requiredItemLevel() - 1, "amulet:suffix:of-the-brute");
      var overlap = magic(82, definition.id());
      var rare = simulator.apply(boundary, action, Set.of(), new Random(1)).state();
      for (var before : List.of(root, below, overlap, rare)) {
        var result = simulator.apply(before, action, Set.of(), new Random(1));
        assertThat(result.applied()).isFalse();
        assertThat(result.state()).isEqualTo(before);
        assertThat(result.events()).isEmpty();
      }
    }
  }
}
