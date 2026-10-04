package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class BodyEssenceTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final WorkbenchSimulator simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  final ItemState root = SolarAmulet.initial(catalog);
  final List<WorkbenchCurrency> actions =
      List.of(
          WorkbenchCurrency.LESSER_ESSENCE_BODY,
          WorkbenchCurrency.ESSENCE_BODY,
          WorkbenchCurrency.GREATER_ESSENCE_BODY);

  ItemState magic(String... ids) {
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        root.itemLevel(),
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
  void eachTierAddsItsSingleGuaranteedLifeRangeAndPreservesMagicModifiers() {
    long[][] ranges = {{20, 29}, {70, 84}, {85, 99}};
    for (int tier = 0; tier < actions.size(); tier++) {
      var action = actions.get(tier);
      var definition = catalog.find(action.fixedModifierId()).orElseThrow();
      assertThat(definition.stats().getFirst().min()).isEqualTo(ranges[tier][0]);
      assertThat(definition.stats().getFirst().max()).isEqualTo(ranges[tier][1]);
      for (var before :
          List.of(
              magic("amulet:suffix:of-the-brute"),
              magic("amulet:suffix:of-the-brute", "amulet:prefix:adept-s")))
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
          assertThat(result.events().getFirst().kind()).isEqualTo("ADD");
          assertThat(result.events().getFirst().modifierId()).isEqualTo(definition.id());
          assertThat(result.events().getFirst().selectionProbability()).isEqualTo(1);
          assertThat(result.assumptions().getFirst().n())
              .isEqualTo(ranges[tier][1] - ranges[tier][0] + 1);
          assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
          assertThat(result.consumedOmens()).isEmpty();
          assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
        }
    }
  }

  @Test
  void wrongRarityOverlapAndUnverifiedLowLevelScopePreserveState() {
    var rare = simulator.apply(root, WorkbenchCurrency.ALCHEMY, Set.of(), new Random(4)).state();
    var low =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            1,
            ItemState.Rarity.MAGIC,
            root.implicits(),
            magic("amulet:suffix:of-the-brute").explicits(),
            root.conditions());
    for (var action : actions)
      for (var before : List.of(root, rare, low, magic("amulet:prefix:hale"))) {
        var result = simulator.apply(before, action, Set.of(), new Random(1));
        assertThat(result.applied()).isFalse();
        assertThat(result.state()).isEqualTo(before);
        assertThat(result.events()).isEmpty();
      }
  }
}
