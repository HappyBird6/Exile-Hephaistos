package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class InfiniteEssenceTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final WorkbenchSimulator simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  final ItemState root = SolarAmulet.initial(catalog);
  final List<WorkbenchCurrency> actions =
      List.of(
          WorkbenchCurrency.LESSER_ESSENCE_INFINITE,
          WorkbenchCurrency.ESSENCE_INFINITE,
          WorkbenchCurrency.GREATER_ESSENCE_INFINITE);

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
  void samplesExactlyThreeSourcedAttributeOutcomesWithExplicitUniformAssumption() {
    long[][] ranges = {{9, 12}, {17, 20}, {25, 27}};
    for (int tier = 0; tier < actions.size(); tier++) {
      var action = actions.get(tier);
      for (var id : action.essenceModifierIds()) {
        var definition = catalog.find(id).orElseThrow();
        assertThat(definition.affixType()).isEqualTo(ModifierDefinition.AffixType.SUFFIX);
        assertThat(definition.stats().getFirst().min()).isEqualTo(ranges[tier][0]);
        assertThat(definition.stats().getFirst().max()).isEqualTo(ranges[tier][1]);
      }
      var reached = new HashSet<String>();
      for (var before :
          List.of(
              magic(82, "amulet:prefix:hale"),
              magic(82, "amulet:prefix:hale", "amulet:suffix:of-the-lost")))
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
          var event = result.events().getFirst();
          assertThat(action.essenceModifierIds()).contains(event.modifierId());
          assertThat(event.selectionProbability()).isEqualTo(1.0 / 3);
          assertThat(result.assumptions().getFirst().id()).isEqualTo("uniform-essence-choice-v1");
          assertThat(result.assumptions().getFirst().n()).isEqualTo(3);
          assertThat(result.assumptions().getFirst().candidates())
              .containsExactlyElementsOf(action.essenceModifierIds());
          assertThat(result.assumptions().getFirst().sourceUrl())
              .isEqualTo(action.essenceChoiceSource());
          assertThat(result.assumptions().get(1).n())
              .isEqualTo(ranges[tier][1] - ranges[tier][0] + 1);
          assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
          assertThat(result.consumedOmens()).isEmpty();
          assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
          reached.add(event.modifierId());
        }
      assertThat(reached).containsExactlyInAnyOrderElementsOf(action.essenceModifierIds());
    }
  }

  @Test
  void attributeOverlapNeverSilentlyShrinksTheChoiceSetAndLowLevelScopeIsBlocked() {
    for (var action : actions) {
      int level =
          catalog.find(action.essenceModifierIds().getFirst()).orElseThrow().requiredItemLevel();
      assertThat(
              simulator
                  .apply(magic(level, "amulet:prefix:hale"), action, Set.of(), new Random(1))
                  .applied())
          .isTrue();
      var rare =
          simulator.apply(magic(82, "amulet:prefix:hale"), action, Set.of(), new Random(2)).state();
      for (var before :
          List.of(
              root,
              rare,
              magic(level - 1, "amulet:prefix:hale"),
              magic(82, "amulet:suffix:of-the-brute"))) {
        var result = simulator.apply(before, action, Set.of(), new Random(1));
        assertThat(result.applied()).isFalse();
        assertThat(result.state()).isEqualTo(before);
        assertThat(result.events()).isEmpty();
        assertThat(result.assumptions()).isEmpty();
      }
    }
  }
}
