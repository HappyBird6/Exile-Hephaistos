package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class FracturingTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final WorkbenchSimulator simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  final ItemState root = SolarAmulet.initial(catalog);

  ItemState rare() {
    return simulator.apply(root, WorkbenchCurrency.ALCHEMY, Set.of(), new Random(42)).state();
  }

  @Test
  void fracturesOneOfFourOrSixExplicitInstancesWithUniformLedgerWithoutChangingRolls() {
    var four = rare();
    var six =
        simulator
            .apply(
                simulator.apply(four, CraftingAction.EXALTED, new Random(3)).state(),
                CraftingAction.EXALTED,
                new Random(4))
            .state();
    for (var before : List.of(four, six)) {
      var selected = new HashSet<String>();
      for (int seed = 0; seed < 100; seed++) {
        var result =
            simulator.apply(
                before, WorkbenchCurrency.FRACTURING, Set.of(), new Random(seed * 104729L));
        assertThat(result.applied()).isTrue();
        assertThat(result.state().explicits().stream().filter(ModifierInstance::fractured))
            .hasSize(1);
        assertThat(result.state().implicits()).isEqualTo(before.implicits());
        assertThat(result.state().explicits().stream().map(ModifierInstance::modifierId))
            .containsExactlyElementsOf(
                before.explicits().stream().map(ModifierInstance::modifierId).toList());
        assertThat(result.state().explicits().stream().map(ModifierInstance::values))
            .containsExactlyElementsOf(
                before.explicits().stream().map(ModifierInstance::values).toList());
        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().selectionProbability())
            .isEqualTo(1.0 / before.explicits().size());
        assertThat(result.assumptions().getFirst().n()).isEqualTo(before.explicits().size());
        assertThat(result.assumptions().getFirst().candidates())
            .containsExactlyElementsOf(
                before.explicits().stream().map(ModifierInstance::modifierId).toList());
        assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
        selected.add(result.events().getFirst().modifierId());
        var blocked =
            simulator.apply(
                result.state(), WorkbenchCurrency.FRACTURING, Set.of(), new Random(seed * 104729L));
        assertThat(blocked.applied()).isFalse();
        assertThat(blocked.state()).isEqualTo(result.state());
        assertThat(blocked.events()).isEmpty();
      }
      assertThat(selected).hasSize(before.explicits().size());
    }
    assertThat(four.explicits()).noneMatch(ModifierInstance::fractured);
  }

  @Test
  void additionChaosAnnulmentAndDivinePreserveTheLockedInstanceAndItsValues() {
    var fractured =
        simulator.apply(rare(), WorkbenchCurrency.FRACTURING, Set.of(), new Random(1)).state();
    var locked =
        fractured.explicits().stream()
            .filter(ModifierInstance::fractured)
            .findFirst()
            .orElseThrow();
    for (int seed = 0; seed < 100; seed++) {
      for (var currency :
          List.of(
              WorkbenchCurrency.EXALTED,
              WorkbenchCurrency.GREATER_EXALTED,
              WorkbenchCurrency.PERFECT_EXALTED,
              WorkbenchCurrency.CHAOS,
              WorkbenchCurrency.GREATER_CHAOS,
              WorkbenchCurrency.PERFECT_CHAOS,
              WorkbenchCurrency.ANNULMENT,
              WorkbenchCurrency.DIVINE)) {
        var result = simulator.apply(fractured, currency, Set.of(), new Random(seed * 104729L));
        assertThat(result.applied()).isTrue();
        assertThat(result.state().explicits()).contains(locked);
        assertThat(result.events()).noneMatch(e -> e.modifierId().equals(locked.modifierId()));
      }
    }
    var state = fractured;
    while (state.explicits().size() > 1)
      state = simulator.apply(state, CraftingAction.ANNULMENT, new Random(4)).state();
    assertThat(state.explicits()).containsExactly(locked);
    var blocked = simulator.apply(state, CraftingAction.ANNULMENT, new Random(3));
    assertThat(blocked.applied()).isFalse();
    assertThat(blocked.state()).isEqualTo(state);
    assertThat(
            simulator
                .apply(
                    fractured, WorkbenchCurrency.CHAOS, Set.of("Omen_of_Whittling"), new Random(3))
                .applied())
        .isFalse();
  }

  @Test
  void rejectsWrongRarityOrTooFewModifiersAndUnsupportedFractureShapes() {
    var magic = simulator.apply(root, CraftingAction.TRANSMUTATION, new Random(4)).state();
    var lowRare = simulator.apply(magic, CraftingAction.REGAL, new Random(5)).state();
    for (var state : List.of(root, magic, lowRare))
      assertThat(
              simulator
                  .apply(state, WorkbenchCurrency.FRACTURING, Set.of(), new Random(3))
                  .applied())
          .isFalse();
    var rare = rare();
    var invalid =
        new ItemState(
            rare.snapshotId(),
            rare.baseItemId(),
            rare.itemLevel(),
            rare.rarity(),
            rare.implicits(),
            rare.explicits().stream()
                .map(m -> new ModifierInstance(m.modifierId(), m.values(), true))
                .toList(),
            rare.conditions());
    assertThat(new ItemStateValidator(catalog).validate(invalid)).isNotEmpty();
    assertThatThrownBy(
            () -> simulator.apply(invalid, WorkbenchCurrency.DIVINE, Set.of(), new Random(3)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
