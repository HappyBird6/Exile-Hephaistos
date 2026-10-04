package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class AdditionTransitionsTest {
  private final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  private final AdditionTransitions transitions = new AdditionTransitions(catalog);
  private final WorkbenchSimulator sampler =
      new WorkbenchSimulator(catalog, new CraftingEngine(catalog));

  private ItemState empty(ItemState.Rarity rarity, int level) {
    var root = SolarAmulet.initial(catalog, level, 15);
    return new ItemState(
        root.snapshotId(), root.baseItemId(), level, rarity, root.implicits(), List.of(), Set.of());
  }

  private ItemState source(WorkbenchCurrency action, int level) {
    return empty(
        switch (action.baseAction()) {
          case TRANSMUTATION -> ItemState.Rarity.NORMAL;
          case AUGMENTATION, REGAL -> ItemState.Rarity.MAGIC;
          default -> ItemState.Rarity.RARE;
        },
        level);
  }

  @Test
  void allTwelveActionsHaveExactMassAndEverySamplerBoundaryMatchesTheTransition() {
    int count = 0;
    for (var action : WorkbenchCurrency.values()) {
      if (!AdditionRules.isAddition(action)) continue;
      count++;
      var state = source(action, 82);
      var result = transitions.transition(StateBucket.from(state), action, Set.of());
      assertThat(result.available()).isTrue();
      long total = result.outcomes().getFirst().totalWeight();
      assertThat(result.outcomes().stream().mapToLong(AdditionTransitions.Outcome::weight).sum())
          .isEqualTo(total);
      assertThat(
              result.outcomes().stream()
                  .mapToDouble(AdditionTransitions.Outcome::probability)
                  .sum())
          .isCloseTo(1, within(1e-12));
      long boundary = 0;
      for (var outcome : result.outcomes()) {
        final long draw = boundary;
        var rng =
            new Random(0) {
              private boolean first = true;

              @Override
              public long nextLong(long bound) {
                if (first) {
                  first = false;
                  assertThat(draw).isLessThan(bound);
                  return draw;
                }
                return 0;
              }
            };
        var sampled = sampler.apply(state, action, Set.of(), rng);
        assertThat(StateBucket.from(sampled.state())).isEqualTo(outcome.state());
        assertThat(sampled.events().getFirst().selectionProbability())
            .isEqualTo(outcome.probability());
        assertThat(outcome.state().modifierIds()).hasSize(1);
        boundary += outcome.weight();
      }
      assertThat(boundary).isEqualTo(total);
    }
    assertThat(count).isEqualTo(12);
  }

  @Test
  void ordinaryAdditionDistributionsMatchTheExistingExactEngine() {
    var old = new CraftingEngine(catalog);
    for (var action : WorkbenchCurrency.values()) {
      if (!AdditionRules.isAddition(action) || action.minimumModifierLevel() != 0) continue;
      var state = StateBucket.from(source(action, 82));
      var actual = transitions.transition(state, action, Set.of());
      var expected = old.transition(state, action.baseAction());
      var probabilities = new HashMap<StateBucket, Double>();
      expected.outcomes().forEach(o -> probabilities.put(o.state(), o.probability()));
      actual
          .outcomes()
          .forEach(o -> assertThat(o.probability()).isEqualTo(probabilities.get(o.state())));
      assertThat(actual.outcomes()).hasSize(expected.outcomes().size());
    }
  }

  @Test
  void minimumLevelExceptionAndBelowFloorAreSharedWithoutMutatingTheSource() {
    var action = WorkbenchCurrency.PERFECT_TRANSMUTATION;
    var state = StateBucket.from(source(action, 82));
    var result = transitions.transition(state, action, Set.of());
    assertThat(result.outcomes())
        .anyMatch(
            o -> {
              var d = catalog.find(o.addedModifierId()).orElseThrow();
              return d.familyIds().contains("BaseSpirit") && d.requiredItemLevel() == 54;
            });
    for (var a : WorkbenchCurrency.values()) {
      if (!AdditionRules.isAddition(a) || a.minimumModifierLevel() == 0) continue;
      var input = StateBucket.from(source(a, a.minimumModifierLevel() - 1));
      var blocked = transitions.transition(input, a, Set.of(WorkbenchOmen.BLESSED.id()));
      assertThat(blocked.available()).isFalse();
      assertThat(blocked.outcomes()).isEmpty();
      assertThat(blocked.source()).isEqualTo(input);
      assertThat(blocked.consumedOmens()).isEmpty();
      assertThat(blocked.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
    }
  }

  @Test
  void omenEligibilityConsumesOnlyMatchingOmenAndBlocksUnsupportedInteractions() {
    var state = StateBucket.from(empty(ItemState.Rarity.RARE, 82));
    for (var omen : List.of(WorkbenchOmen.SINISTRAL_EXALTATION, WorkbenchOmen.DEXTRAL_EXALTATION)) {
      var active = Set.of(omen.id(), WorkbenchOmen.BLESSED.id());
      var result = transitions.transition(state, WorkbenchCurrency.EXALTED, active);
      assertThat(result.available()).isTrue();
      assertThat(result.outcomes())
          .allMatch(
              o -> catalog.find(o.addedModifierId()).orElseThrow().affixType() == omen.affix());
      assertThat(result.consumedOmens()).containsExactly(omen.id());
      assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
      var tiered = transitions.transition(state, WorkbenchCurrency.PERFECT_EXALTED, active);
      assertThat(tiered.available()).isTrue();
      assertThat(tiered.outcomes())
          .allMatch(
              o -> catalog.find(o.addedModifierId()).orElseThrow().affixType() == omen.affix());
      assertThat(tiered.consumedOmens()).containsExactly(omen.id());
      assertThat(tiered.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
    }
    assertThat(
            transitions
                .transition(
                    state,
                    WorkbenchCurrency.EXALTED,
                    Set.of(
                        WorkbenchOmen.SINISTRAL_EXALTATION.id(),
                        WorkbenchOmen.DEXTRAL_EXALTATION.id()))
                .available())
        .isFalse();
  }

  @Test
  void slotExhaustionAndRecoveryActionsAreExplicitlyBlocked() {
    var concrete = empty(ItemState.Rarity.RARE, 82);
    var rng = new Random(3);
    for (int i = 0; i < 6; i++)
      concrete = sampler.apply(concrete, WorkbenchCurrency.EXALTED, Set.of(), rng).state();
    var full = StateBucket.from(concrete);
    for (var action : WorkbenchCurrency.values()) {
      var result = transitions.transition(full, action, Set.of());
      assertThat(result.available()).isFalse();
      assertThat(result.source()).isEqualTo(full);
      assertThat(result.outcomes()).isEmpty();
    }
    for (var action :
        List.of(WorkbenchCurrency.CHAOS, WorkbenchCurrency.ANNULMENT, WorkbenchCurrency.DIVINE)) {
      var result =
          transitions.transition(
              StateBucket.from(empty(ItemState.Rarity.RARE, 82)), action, Set.of());
      assertThat(result.reason()).contains("outside the finite addition");
    }
  }
}
