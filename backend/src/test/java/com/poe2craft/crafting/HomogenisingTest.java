package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class HomogenisingTest {
  final String exalt = WorkbenchOmen.HOMOGENISING_EXALTATION.id();
  final String regal = WorkbenchOmen.HOMOGENISING_CORONATION.id();
  final String greater = WorkbenchOmen.GREATER_EXALTATION.id();

  ModifierDefinition mod(
      String id, ModifierDefinition.AffixType side, int weight, Set<String> tags) {
    return new ModifierDefinition(
        id,
        id,
        ModifierDefinition.Layer.EXPLICIT,
        side,
        Set.of(id),
        1,
        weight,
        1,
        id,
        List.of(new ModifierDefinition.StatRange(id, 1, 1)),
        tags,
        "https://example.test/" + id);
  }

  ItemCatalog catalog(boolean matching) {
    var source = ItemCatalogLoader.loadDefault();
    var p = ModifierDefinition.AffixType.PREFIX;
    var s = ModifierDefinition.AffixType.SUFFIX;
    var defs =
        List.of(
            mod("seed", p, 1, Set.of("life")),
            mod("bridge", s, 2, matching ? Set.of("life", "damage") : Set.of("damage")),
            mod("same", p, 3, matching ? Set.of("life") : Set.of("damage")),
            mod("new-tag-only", s, 9999, Set.of("damage")),
            source.find(source.base().implicitModifierId()).orElseThrow());
    var m = source.metadata();
    return new ItemCatalog(
        new ItemCatalog.Metadata(
            m.snapshotId(),
            m.retrievedAt(),
            m.sourceUrl(),
            m.weightPolicy(),
            m.rawSha256(),
            m.detailsSha256(),
            2,
            2,
            4,
            10001),
        source.base(),
        defs);
  }

  ItemState state(ItemCatalog c, ItemState.Rarity rarity, List<ModifierInstance> mods) {
    var root = SolarAmulet.initial(c);
    return new ItemState(
        root.snapshotId(), root.baseItemId(), 82, rarity, root.implicits(), mods, Set.of());
  }

  @Test
  void singleTagUnionUsesOrdinaryWeightsAndConsumesOnlyMatchingTrigger() {
    var c = catalog(true);
    var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
    for (var rarity : List.of(ItemState.Rarity.MAGIC, ItemState.Rarity.RARE)) {
      var action =
          rarity == ItemState.Rarity.MAGIC ? WorkbenchCurrency.REGAL : WorkbenchCurrency.EXALTED;
      var omen = rarity == ItemState.Rarity.MAGIC ? regal : exalt;
      var other = rarity == ItemState.Rarity.MAGIC ? exalt : regal;
      var before = state(c, rarity, List.of(new ModifierInstance("seed", Map.of("seed", 1L))));
      for (int seed = 0; seed < 20; seed++) {
        var result = sim.apply(before, action, Set.of(omen, other), new Random(seed));
        assertThat(result.applied()).isTrue();
        assertThat(result.consumedOmens()).containsExactly(omen);
        assertThat(result.remainingOmens()).containsExactly(other);
        assertThat(result.state().explicits()).containsAll(before.explicits());
        assertThat(result.state().implicits()).isEqualTo(before.implicits());
        var event = result.events().getFirst();
        assertThat(event.modifierId()).isIn("bridge", "same");
        assertThat(event.selectionProbability())
            .isEqualTo(c.find(event.modifierId()).orElseThrow().weight() / 5.0);
      }
      var plan = new AdditionRules(c).plan(StateBucket.from(before), action, Set.of(omen));
      assertThat(plan.candidates())
          .extracting(ModifierDefinition::id)
          .containsExactly("bridge", "same");
    }
  }

  @Test
  void doubleAdditionNeverExpandsTagsAndRetainsFracturedOriginal() {
    var c = catalog(true);
    var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
    var before =
        state(
            c,
            ItemState.Rarity.RARE,
            List.of(new ModifierInstance("seed", Map.of("seed", 1L), true)));
    var result =
        sim.apply(
            before,
            WorkbenchCurrency.EXALTED,
            Set.of(exalt, greater),
            new Random() {
              @Override
              public long nextLong(long bound) {
                return 0;
              }
            });
    assertThat(result.applied()).isTrue();
    assertThat(result.events())
        .extracting(WorkbenchSimulator.Event::modifierId)
        .containsExactly("bridge", "same");
    assertThat(result.events().getLast().selectionProbability()).isEqualTo(1);
    assertThat(result.consumedOmens()).containsExactlyInAnyOrder(exalt, greater);
    assertThat(result.state().explicits()).contains(before.explicits().getFirst());
    assertThat(
            new AdditionRules(c)
                .plan(StateBucket.from(before), WorkbenchCurrency.EXALTED, Set.of(exalt, greater))
                .available())
        .isFalse();
  }

  @Test
  void noTagsFallsBackButNoMatchingCandidateRefusesWithoutConsumingOrDrawing() {
    var c = catalog(false);
    var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
    for (var rarity : List.of(ItemState.Rarity.MAGIC, ItemState.Rarity.RARE)) {
      var action =
          rarity == ItemState.Rarity.MAGIC ? WorkbenchCurrency.REGAL : WorkbenchCurrency.EXALTED;
      var omen = rarity == ItemState.Rarity.MAGIC ? regal : exalt;
      var empty = state(c, rarity, List.of());
      var ordinary = sim.apply(empty, action, Set.of(), new Random(12));
      var fallback = sim.apply(empty, action, Set.of(omen), new Random(12));
      assertThat(fallback.state()).isEqualTo(ordinary.state());
      assertThat(fallback.events()).isEqualTo(ordinary.events());
      assertThat(fallback.consumedOmens()).containsExactly(omen);
      var before = state(c, rarity, List.of(new ModifierInstance("seed", Map.of("seed", 1L))));
      var failed =
          sim.apply(
              before,
              action,
              Set.of(omen),
              new Random() {
                @Override
                public long nextLong(long bound) {
                  throw new AssertionError("Must not draw");
                }
              });
      assertThat(failed.applied()).isFalse();
      assertThat(failed.state()).isSameAs(before);
      assertThat(failed.consumedOmens()).isEmpty();
      assertThat(failed.events()).isEmpty();
    }
  }

  @Test
  void tieredDoubleAdditionWorksButUnknownCombinationAndSecondBranchRefuse() {
    var c = catalog(true);
    var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
    var before =
        state(c, ItemState.Rarity.RARE, List.of(new ModifierInstance("seed", Map.of("seed", 1L))));
    for (var action : List.of(WorkbenchCurrency.GREATER_EXALTED, WorkbenchCurrency.PERFECT_EXALTED))
      assertThat(sim.apply(before, action, Set.of(exalt, greater), new Random()).applied())
          .isTrue();
    for (var other :
        List.of(WorkbenchOmen.SINISTRAL_EXALTATION.id(), WorkbenchOmen.DEXTRAL_EXALTATION.id()))
      assertThat(
              sim.apply(before, WorkbenchCurrency.EXALTED, Set.of(exalt, other), new Random())
                  .applied())
          .isFalse();
    var oneLeft =
        state(
            c,
            ItemState.Rarity.RARE,
            List.of(
                new ModifierInstance("seed", Map.of("seed", 1L)),
                new ModifierInstance("same", Map.of("same", 1L))));
    assertThat(
            sim.apply(oneLeft, WorkbenchCurrency.EXALTED, Set.of(exalt, greater), new Random())
                .applied())
        .isFalse();
  }
}
