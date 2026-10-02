package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class WorkbenchSimulatorTest {
  private final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  private final CraftingEngine engine = new CraftingEngine(catalog);
  private final WorkbenchSimulator simulator = new WorkbenchSimulator(catalog, engine);

  @Test
  void alchemyReplacesMagicAffixesWithFourWeightedRareAffixesAndPreservesImplicit() {
    var root = SolarAmulet.initial(catalog);
    var magic = simulator.apply(root, CraftingAction.TRANSMUTATION, new Random(3)).state();
    for (int seed = 0; seed < 100; seed++) {
      for (var input : List.of(root, magic)) {
        var result =
            simulator.apply(input, WorkbenchCurrency.ALCHEMY, java.util.Set.of(), new Random(seed));
        assertThat(result.applied()).isTrue();
        assertThat(result.state().rarity()).isEqualTo(ItemState.Rarity.RARE);
        assertThat(result.state().explicits()).hasSize(4);
        assertThat(result.state().implicits()).isEqualTo(input.implicits());
        assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
        assertThat(result.events().stream().filter(e -> e.kind().equals("ADD"))).hasSize(4);
        assertThat(result.events().stream().filter(e -> e.kind().equals("REMOVE")))
            .hasSize(input.explicits().size());
        var partial = new java.util.ArrayList<ModifierInstance>();
        for (var event : result.events()) {
          if (!event.kind().equals("ADD")) continue;
          var empty =
              new ItemState(
                  root.snapshotId(),
                  root.baseItemId(),
                  root.itemLevel(),
                  ItemState.Rarity.RARE,
                  root.implicits(),
                  partial,
                  root.conditions());
          var pool = simulator.pool(empty, WorkbenchCurrency.ALCHEMY, null);
          var selected = catalog.find(event.modifierId()).orElseThrow();
          assertThat(event.selectionProbability())
              .isEqualTo(
                  (double) selected.weight()
                      / pool.stream().mapToLong(ModifierDefinition::weight).sum());
          partial.add(new ModifierInstance(event.modifierId(), event.values()));
        }
        var blocked =
            simulator.apply(
                result.state(), WorkbenchCurrency.ALCHEMY, java.util.Set.of(), new Random(seed));
        assertThat(blocked.applied()).isFalse();
        assertThat(blocked.state()).isEqualTo(result.state());
      }
    }
    assertThat(root.explicits()).isEmpty();
    assertThat(magic.explicits()).hasSize(1);
  }

  @Test
  void allSixActionsChangeConcreteStateWithoutMutatingInputOrUnchangedRolls() {
    var root = SolarAmulet.initial(catalog);
    var state = root;
    for (var action :
        List.of(
            CraftingAction.TRANSMUTATION,
            CraftingAction.AUGMENTATION,
            CraftingAction.REGAL,
            CraftingAction.EXALTED,
            CraftingAction.CHAOS,
            CraftingAction.ANNULMENT)) {
      var before = state;
      var result = simulator.apply(state, action, new Random(42));
      assertThat(result.applied()).isTrue();
      state = result.state();
      assertThat(new ItemStateValidator(catalog).validate(state)).isEmpty();
      assertThat(state.implicits()).isEqualTo(root.implicits());
      for (var instance : before.explicits()) {
        boolean removed =
            result.events().stream()
                .anyMatch(
                    e -> e.kind().equals("REMOVE") && e.modifierId().equals(instance.modifierId()));
        if (!removed) assertThat(state.explicits()).contains(instance);
      }
      for (var instance : state.explicits()) {
        var range = catalog.find(instance.modifierId()).orElseThrow().stats().getFirst();
        assertThat(instance.values().get(range.id())).isBetween(range.min(), range.max());
      }
    }
    assertThat(root.rarity()).isEqualTo(ItemState.Rarity.NORMAL);
    assertThat(root.explicits()).isEmpty();
    assertThat(state.explicits()).hasSize(3);
  }

  @Test
  void chaosReaddingTheSameModifierRerollsItsValuesAndReportsBothEvents() {
    var root = SolarAmulet.initial(catalog);
    var definition =
        catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.layer() == ModifierDefinition.Layer.EXPLICIT
                        && d.stats().getFirst().max() > d.stats().getFirst().min())
            .findFirst()
            .orElseThrow();
    var range = definition.stats().getFirst();
    var old = new ModifierInstance(definition.id(), Map.of(range.id(), range.min()));
    var rare =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            root.itemLevel(),
            ItemState.Rarity.RARE,
            root.implicits(),
            List.of(old),
            root.conditions());
    var emptyRare =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            root.itemLevel(),
            ItemState.Rarity.RARE,
            root.implicits(),
            List.of(),
            root.conditions());
    var pool = new ModifierPoolResolver(catalog).resolve(StateBucket.from(emptyRare));
    long offset = 0;
    for (var candidate : pool.candidates()) {
      if (candidate.id().equals(definition.id())) break;
      offset += candidate.weight();
    }
    final long target = offset;
    var random =
        new Random() {
          @Override
          public int nextInt(int bound) {
            return 0;
          }

          @Override
          public long nextLong(long bound) {
            return bound == pool.totalWeight() ? target : bound - 1;
          }
        };
    var result = simulator.apply(rare, CraftingAction.CHAOS, random);
    assertThat(result.state().explicits())
        .containsExactly(new ModifierInstance(definition.id(), Map.of(range.id(), range.max())));
    assertThat(rare.explicits()).containsExactly(old);
    assertThat(result.events())
        .extracting(WorkbenchSimulator.Event::kind)
        .containsExactly("REMOVE", "ADD");
    assertThat(result.assumptions())
        .extracting(WorkbenchSimulator.Assumption::id)
        .containsExactly("uniform-removal-v1", "uniform-integer-roll-v1");
    assertThat(result.assumptions().getLast().n()).isEqualTo(range.max() - range.min() + 1);
  }

  @Test
  void unavailableAndInvalidInputsCannotConsumeOrChangeAnItem() {
    var root = SolarAmulet.initial(catalog);
    var result = simulator.apply(root, CraftingAction.EXALTED, new Random(1));
    assertThat(result.applied()).isFalse();
    assertThat(result.state()).isEqualTo(root);
    assertThat(result.events()).isEmpty();
    assertThat(result.assumptions()).isEmpty();
    assertThatThrownBy(() -> simulator.apply(null, CraftingAction.CHAOS, new Random()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void bundledRollDomainsAreSingleStatsAndOnlyVerifiedEssenceResultsHaveZeroSpawnWeight() {
    assertThat(catalog.modifiers().values())
        .allSatisfy(
            d -> {
              assertThat(d.stats()).hasSize(1);
              if (WorkbenchCurrency.PERFECT_ESSENCE_INFINITE
                      .replacementEssenceModifiers()
                      .contains(d.id())
                  || WorkbenchCurrency.PERFECT_ESSENCE_ENHANCEMENT
                      .replacementEssenceModifiers()
                      .contains(d.id())
                  || WorkbenchCurrency.ESSENCE_BREACH
                      .replacementEssenceModifiers()
                      .contains(d.id())) assertThat(d.weight()).isZero();
              else if (d.layer() == ModifierDefinition.Layer.EXPLICIT)
                assertThat(d.weight()).isPositive();
            });
  }
}
