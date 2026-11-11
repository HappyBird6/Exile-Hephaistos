package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
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
        List.of(WorkbenchCurrency.GREATER_EXALTED, WorkbenchCurrency.PERFECT_EXALTED)) {
      var result = simulator.apply(four, currency, Set.of(omen), new Random(1));
      assertThat(result.applied()).isTrue();
      assertThat(result.events()).hasSize(2);
      assertThat(result.state().explicits()).hasSize(6).containsAll(four.explicits());
      var intermediate = four;
      for (var e : result.events()) {
        var pool = simulator.pool(intermediate, currency, null);
        var chosen = catalog.find(e.modifierId()).orElseThrow();
        assertThat(pool).contains(chosen);
        assertThat(e.selectionProbability())
            .isEqualTo(
                (double) chosen.weight()
                    / pool.stream().mapToLong(ModifierDefinition::weight).sum());
        var mods = new ArrayList<>(intermediate.explicits());
        mods.add(new ModifierInstance(e.modifierId(), e.values()));
        intermediate =
            new ItemState(
                four.snapshotId(),
                four.baseItemId(),
                four.itemLevel(),
                four.rarity(),
                four.implicits(),
                mods,
                four.conditions());
      }
      assertThat(result.assumptions())
          .anyMatch(
              a ->
                  a.id().equals("tiered-omen-composition-v1") && a.reason().contains("UNVERIFIED"));
    }
    var plan =
        new AdditionRules(catalog)
            .plan(StateBucket.from(four), WorkbenchCurrency.EXALTED, Set.of(omen));
    assertThat(plan.available()).isFalse();
    assertThat(plan.reason()).contains("finite addition model");
  }

  @Test
  void directionalGreaterUsesOnlyItsSideAndRetainsGreaterWithOneSlot() {
    for (var directional :
        List.of(WorkbenchOmen.SINISTRAL_EXALTATION, WorkbenchOmen.DEXTRAL_EXALTATION)) {
      for (int occupied : List.of(1, 2, 3)) {
        var state =
            new ItemState(
                root.snapshotId(),
                root.baseItemId(),
                root.itemLevel(),
                ItemState.Rarity.RARE,
                root.implicits(),
                List.of(),
                root.conditions());
        for (int i = 0; i < occupied; i++)
          state =
              simulator
                  .apply(state, WorkbenchCurrency.EXALTED, Set.of(directional.id()), new Random(i))
                  .state();
        for (var currency :
            List.of(
                WorkbenchCurrency.EXALTED,
                WorkbenchCurrency.GREATER_EXALTED,
                WorkbenchCurrency.PERFECT_EXALTED)) {
          var omens = Set.of(omen, directional.id());
          var choices = simulator.exaltChoices(state, currency, omens);
          var result = simulator.apply(state, currency, omens, new Random(5));
          assertThat(result.applied()).isEqualTo(occupied < 3);
          assertThat(result.state().explicits()).containsAll(state.explicits());
          if (occupied == 3) {
            assertThat(choices).isEmpty();
            assertThat(result.consumedOmens()).isEmpty();
            continue;
          }
          assertThat(result.events())
              .hasSize(3 - occupied)
              .allMatch(
                  e ->
                      catalog.find(e.modifierId()).orElseThrow().affixType()
                          == directional.affix());
          assertThat(result.consumedOmens()).contains(directional.id());
          assertThat(result.consumedOmens().contains(omen)).isEqualTo(occupied == 1);
          assertThat(result.remainingOmens().contains(omen)).isEqualTo(occupied == 2);
          assertThat(choices)
              .allMatch(
                  c ->
                      catalog.find(c.modifierId()).orElseThrow().affixType()
                          == directional.affix());
          assertThat(
                  choices.stream().mapToDouble(WorkbenchSimulator.ExaltChoice::probability).sum())
              .isCloseTo(3 - occupied, within(1e-9));
          var custom =
              simulator.applyCustomExalt(
                  state, currency, omens, new Random(5), choices.getFirst().modifierId());
          assertThat(custom.applied()).isTrue();
          assertThat(custom.events()).hasSize(3 - occupied);
          assertThat(custom.consumedOmens()).containsExactlyElementsOf(result.consumedOmens());
        }
      }
    }
  }
}
