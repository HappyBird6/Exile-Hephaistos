package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class LegacyFiveTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final WorkbenchSimulator simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));

  ItemState state(ItemState.Rarity rarity, List<ModifierInstance> mods) {
    var initial = SolarAmulet.initial(catalog);
    return new ItemState(
        initial.snapshotId(),
        initial.baseItemId(),
        82,
        rarity,
        initial.implicits(),
        mods,
        Set.of());
  }

  @Test
  void alchemyGuaranteesMaximumSideAndUsesPublishedConditionalWeights() {
    for (var omen : List.of(WorkbenchOmen.SINISTRAL_ALCHEMY, WorkbenchOmen.DEXTRAL_ALCHEMY)) {
      for (int seed = 0; seed < 12; seed++) {
        var before = state(ItemState.Rarity.NORMAL, List.of());
        var result =
            simulator.apply(
                before,
                WorkbenchCurrency.ALCHEMY,
                Set.of(omen.id(), WorkbenchOmen.WHITTLING.id()),
                new Random(seed));
        assertThat(result.applied()).isTrue();
        assertThat(result.state().explicits()).hasSize(4);
        assertThat(
                result.state().explicits().stream()
                    .filter(
                        m -> catalog.find(m.modifierId()).orElseThrow().affixType() == omen.affix())
                    .count())
            .isEqualTo(3);
        assertThat(result.consumedOmens()).containsExactly(omen.id());
        assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.WHITTLING.id());
        var intermediate = new ArrayList<ModifierInstance>();
        for (int i = 0; i < 4; i++) {
          var side =
              i < 3
                  ? omen
                  : omen == WorkbenchOmen.SINISTRAL_ALCHEMY
                      ? WorkbenchOmen.DEXTRAL_ALCHEMY
                      : WorkbenchOmen.SINISTRAL_ALCHEMY;
          var pool =
              simulator.pool(
                  state(ItemState.Rarity.RARE, intermediate), WorkbenchCurrency.ALCHEMY, side);
          var event = result.events().get(i);
          var d = catalog.find(event.modifierId()).orElseThrow();
          assertThat(event.selectionProbability())
              .isEqualTo(
                  (double) d.weight() / pool.stream().mapToLong(ModifierDefinition::weight).sum());
          intermediate.add(
              result.state().explicits().stream()
                  .filter(m -> m.modifierId().equals(event.modifierId()))
                  .findFirst()
                  .orElseThrow());
        }
      }
    }
  }

  @Test
  void coronationAddsOnlyRequestedSideAndRetainsExistingRolls() {
    for (var omen : List.of(WorkbenchOmen.SINISTRAL_CORONATION, WorkbenchOmen.DEXTRAL_CORONATION)) {
      var plain =
          simulator
              .apply(
                  state(ItemState.Rarity.NORMAL, List.of()),
                  WorkbenchCurrency.TRANSMUTATION,
                  Set.of(),
                  new Random(4))
              .state();
      var locked = plain.explicits();
      var before = state(ItemState.Rarity.MAGIC, locked);
      var result =
          simulator.apply(before, WorkbenchCurrency.REGAL, Set.of(omen.id()), new Random(9));
      assertThat(result.applied()).isTrue();
      assertThat(result.state().rarity()).isEqualTo(ItemState.Rarity.RARE);
      assertThat(result.state().explicits()).containsAll(locked).hasSize(2);
      assertThat(catalog.find(result.events().getFirst().modifierId()).orElseThrow().affixType())
          .isEqualTo(omen.affix());
      var tiered =
          simulator.apply(
              before, WorkbenchCurrency.GREATER_REGAL, Set.of(omen.id()), new Random(9));
      assertThat(tiered.applied()).isTrue();
      assertThat(catalog.find(tiered.events().getFirst().modifierId()).orElseThrow().affixType())
          .isEqualTo(omen.affix());
    }
  }

  @Test
  void greaterAnnulmentRemovesTwoDistinctUnlockedInstancesAndRefusesOneBeforeDrawing() {
    var rare =
        simulator
            .apply(
                state(ItemState.Rarity.NORMAL, List.of()),
                WorkbenchCurrency.ALCHEMY,
                Set.of(),
                new Random(7))
            .state();
    var mods = new ArrayList<>(rare.explicits());
    var first = mods.getFirst();
    mods.set(0, new ModifierInstance(first.modifierId(), first.values(), true));
    var before = state(ItemState.Rarity.RARE, mods);
    var result =
        simulator.apply(
            before,
            WorkbenchCurrency.ANNULMENT,
            Set.of(WorkbenchOmen.GREATER_ANNULMENT.id()),
            new Random(9));
    assertThat(result.applied()).isTrue();
    assertThat(result.state().explicits()).hasSize(2).contains(mods.getFirst());
    assertThat(result.events()).hasSize(2);
    assertThat(
            result.events().stream().map(WorkbenchSimulator.Event::modifierId).distinct().count())
        .isEqualTo(2);
    assertThat(result.events().get(0).selectionProbability()).isEqualTo(1.0 / 3);
    assertThat(result.events().get(1).selectionProbability()).isEqualTo(1.0 / 2);
    var one = state(ItemState.Rarity.MAGIC, List.of(first));
    assertAtomicRefusal(
        one,
        simulator.apply(
            one,
            WorkbenchCurrency.ANNULMENT,
            Set.of(WorkbenchOmen.GREATER_ANNULMENT.id()),
            noRandom()),
        Set.of(WorkbenchOmen.GREATER_ANNULMENT.id()));
    var pair =
        Set.of(
            WorkbenchOmen.GREATER_ANNULMENT.id(),
            WorkbenchOmen.DEXTRAL_ANNULMENT.id(),
            WorkbenchOmen.SINISTRAL_ANNULMENT.id());
    assertAtomicRefusal(
        before, simulator.apply(before, WorkbenchCurrency.ANNULMENT, pair, noRandom()), pair);
  }

  @Test
  void conflictingAlchemyOmensAndFracturedAlchemyRefuseAtomically() {
    var before = state(ItemState.Rarity.NORMAL, List.of());
    var pair = Set.of(WorkbenchOmen.SINISTRAL_ALCHEMY.id(), WorkbenchOmen.DEXTRAL_ALCHEMY.id());
    assertAtomicRefusal(
        before, simulator.apply(before, WorkbenchCurrency.ALCHEMY, pair, noRandom()), pair);
    var magic =
        simulator.apply(before, WorkbenchCurrency.TRANSMUTATION, Set.of(), new Random(1)).state();
    var locked =
        magic.explicits().stream()
            .map(m -> new ModifierInstance(m.modifierId(), m.values(), true))
            .toList();
    var fracture = state(ItemState.Rarity.MAGIC, locked);
    assertThatThrownBy(
            () ->
                simulator.apply(
                    fracture,
                    WorkbenchCurrency.ALCHEMY,
                    Set.of(WorkbenchOmen.SINISTRAL_ALCHEMY.id()),
                    noRandom()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  Random noRandom() {
    return new Random() {
      @Override
      public long nextLong(long bound) {
        throw new AssertionError("Refusal drew randomness");
      }

      @Override
      public int nextInt(int bound) {
        throw new AssertionError("Refusal drew randomness");
      }
    };
  }

  void assertAtomicRefusal(ItemState before, WorkbenchSimulator.Result result, Set<String> omens) {
    assertThat(result.applied()).isFalse();
    assertThat(result.state()).isEqualTo(before);
    assertThat(result.events()).isEmpty();
    assertThat(result.assumptions()).isEmpty();
    assertThat(result.consumedOmens()).isEmpty();
    assertThat(result.remainingOmens()).containsExactlyInAnyOrderElementsOf(omens);
  }
}
