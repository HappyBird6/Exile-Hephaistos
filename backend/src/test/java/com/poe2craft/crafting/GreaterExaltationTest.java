package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class GreaterExaltationTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final WorkbenchSimulator simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  final ItemState root = SolarAmulet.initial(catalog);
  final String omen = WorkbenchOmen.GREATER_EXALTATION.id();

  ItemState rare() {
    return simulator.apply(root, WorkbenchCurrency.ALCHEMY, Set.of(), new Random(42)).state();
  }

  @Test
  void addsTwoConditionalWeightedModifiersConsumesOnceAndPreservesFracture() {
    var before =
        simulator.apply(rare(), WorkbenchCurrency.FRACTURING, Set.of(), new Random(1)).state();
    for (int seed = 0; seed < 100; seed++) {
      var result =
          simulator.apply(
              before,
              WorkbenchCurrency.EXALTED,
              Set.of(omen, WorkbenchOmen.BLESSED.id()),
              new Random(seed * 104729L));
      assertThat(result.applied()).isTrue();
      assertThat(result.state().explicits()).hasSize(6).containsAll(before.explicits());
      assertThat(result.state().implicits()).isEqualTo(before.implicits());
      assertThat(result.events()).hasSize(2).allMatch(e -> e.kind().equals("ADD"));
      assertThat(result.consumedOmens()).containsExactly(omen);
      assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
      assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
      var intermediate = before;
      for (var event : result.events()) {
        var pool = simulator.pool(intermediate, WorkbenchCurrency.EXALTED, null);
        var chosen = catalog.find(event.modifierId()).orElseThrow();
        assertThat(pool).contains(chosen);
        assertThat(event.selectionProbability())
            .isEqualTo(
                (double) chosen.weight()
                    / pool.stream().mapToLong(ModifierDefinition::weight).sum());
        var mods = new ArrayList<>(intermediate.explicits());
        mods.add(new ModifierInstance(chosen.id(), event.values()));
        intermediate =
            new ItemState(
                before.snapshotId(),
                before.baseItemId(),
                before.itemLevel(),
                before.rarity(),
                before.implicits(),
                mods,
                before.conditions());
      }
      assertThat(intermediate).isEqualTo(result.state());
    }
    assertThat(before.explicits()).hasSize(4);
  }

  @Test
  void uncertainSlotsCurrenciesAndCombinationsBlockWithoutMutationOrConsumption() {
    var four = rare();
    var five = simulator.apply(four, CraftingAction.EXALTED, new Random(2)).state();
    var six = simulator.apply(five, CraftingAction.EXALTED, new Random(3)).state();
    for (var state : List.of(root, five, six)) {
      var result = simulator.apply(state, WorkbenchCurrency.EXALTED, Set.of(omen), new Random(1));
      assertThat(result.applied()).isFalse();
      assertThat(result.state()).isEqualTo(state);
      assertThat(result.events()).isEmpty();
      assertThat(result.consumedOmens()).isEmpty();
      assertThat(result.remainingOmens()).containsExactly(omen);
    }
    for (var currency :
        List.of(WorkbenchCurrency.GREATER_EXALTED, WorkbenchCurrency.PERFECT_EXALTED))
      assertThat(simulator.apply(four, currency, Set.of(omen), new Random(1)).applied()).isFalse();
    assertThat(
            simulator
                .apply(
                    four,
                    WorkbenchCurrency.EXALTED,
                    Set.of(omen, WorkbenchOmen.SINISTRAL_EXALTATION.id()),
                    new Random(1))
                .applied())
        .isFalse();
    var plan =
        new AdditionRules(catalog)
            .plan(StateBucket.from(four), WorkbenchCurrency.EXALTED, Set.of(omen));
    assertThat(plan.available()).isFalse();
    assertThat(plan.reason()).contains("finite addition model");
  }
}
