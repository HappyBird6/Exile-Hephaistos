package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.*;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import org.junit.jupiter.api.Test;

class EssenceReplacementRulesTest {
  final StockyCraftingFixture f = new StockyCraftingFixture();

  private ItemState full(boolean fractured) {
    var state = f.rare(82, "", "stocky-mitts:prefix:sanguine");
    while (state.explicits().size() < 6) {
      var r =
          f.simulator.apply(
              state,
              WorkbenchCurrency.EXALTED,
              Set.of(),
              new Random(11 + state.explicits().size()));
      assertThat(r.applied()).isTrue();
      state = r.state();
    }
    if (!fractured) return state;
    var mods = new ArrayList<>(state.explicits());
    var first = mods.getFirst();
    mods.set(0, new ModifierInstance(first.modifierId(), first.values(), true));
    return AbyssCraftingState.withExplicits(state, mods);
  }

  @Test
  void targetIsUniformWhileFracturedExclusionsMakeConditionalRemovalCountsDifferent() {
    var state = full(true);
    var targets =
        f.simulator.referenceEssenceTargets().get("ESSENCE_ABYSS").stream()
            .map(id -> f.simulator.catalog().find(id).orElseThrow())
            .toList();
    var plan =
        EssenceReplacementRules.plan(
            AbyssCraftingState.from(state, f.simulator.catalog()),
            f.simulator.catalog(),
            targets,
            null);
    assertThat(plan.available()).as(plan.reason()).isTrue();
    assertThat(plan.outcomes()).hasSize(2);
    var total = Fraction.ZERO;
    for (var outcome : plan.outcomes()) {
      assertThat(outcome.removals())
          .allSatisfy(
              r -> {
                assertThat(r.side()).isEqualTo(outcome.target().affixType());
                assertThat(r.modifier().fractured()).isFalse();
              });
      int expected = outcome.target().affixType() == ModifierDefinition.AffixType.PREFIX ? 2 : 3;
      assertThat(outcome.removals()).hasSize(expected);
      var mass = Fraction.of(1, plan.outcomes().size() * outcome.removals().size());
      for (var ignored : outcome.removals()) total = total.add(mass);
    }
    assertThat(total).isEqualTo(Fraction.ONE);
    var seen = new HashSet<ModifierDefinition.AffixType>();
    for (int seed = 0; seed < 100; seed++) {
      var result =
          f.simulator.apply(
              state, WorkbenchCurrency.ESSENCE_ABYSS, Set.of(), new Random(seed * 104729L));
      assertThat(result.applied()).as(result.reason()).isTrue();
      var add = f.simulator.catalog().find(result.events().getLast().modifierId()).orElseThrow();
      var remove =
          f.simulator.catalog().find(result.events().getFirst().modifierId()).orElseThrow();
      assertThat(add.affixType()).isEqualTo(remove.affixType());
      seen.add(add.affixType());
      assertThat(result.events().getLast().selectionProbability()).isEqualTo(.5);
      assertThat(result.events().getFirst().selectionProbability())
          .isEqualTo(add.affixType() == ModifierDefinition.AffixType.PREFIX ? .5 : 1.0 / 3);
      assertThat(result.state().explicits()).contains(state.explicits().getFirst());
    }
    assertThat(seen).hasSize(2);
  }

  @Test
  void hiddenSlotsAndAutomaticSearchUseTheSameSideRestrictionAndProbability() {
    var full = full(false);
    var action = WorkbenchCurrency.PERFECT_ESSENCE_OPULENCE;
    var removed = full.explicits().getFirst();
    var known =
        AbyssCraftingState.withExplicits(
            full, full.explicits().stream().filter(m -> m != removed).toList());
    var hidden =
        AbyssCraftingState.from(known, f.simulator.catalog())
            .addUnrevealed("hidden", ModifierDefinition.AffixType.PREFIX, f.simulator.catalog());
    var plan = f.simulator.abyssPlan(hidden, action, Set.of());
    assertThat(plan.available()).as(plan.reason()).isTrue();
    assertThat(plan.branches()).hasSize(3);
    assertThat(plan.branches())
        .allSatisfy(
            b -> {
              assertThat(b.remaining().slots()).anyMatch(s -> s.id().equals("hidden"));
              assertThat(b.probabilityDenominator()).isEqualTo(3);
            });
    var applied = f.simulator.applyAbyss(hidden, action, Set.of(), new Random(2));
    assertThat(applied.slots())
        .hasSize(6)
        .anyMatch(s -> s.id().equals("hidden") && s.modifier() == null);
    var automatic =
        f.simulator.automaticPlan(
            full,
            new AutomaticCraftingAction(
                action, Set.of(), AutomaticCurrencyPolicy.Purpose.ORDINARY));
    assertThat(automatic.status()).isEqualTo(AutomaticCraftingPlan.Status.READY);
    assertThat(automatic.branches())
        .hasSize(3)
        .allSatisfy(
            b -> {
              assertThat(b.probabilityDenominator()).isEqualTo(3);
              assertThat(b.remaining().explicits()).hasSize(5);
            });
    var blocked =
        f.simulator.apply(
            full, action, Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id()), new Random(1));
    assertThat(blocked.applied()).isFalse();
    assertThat(blocked.state()).isEqualTo(full);
    assertThat(blocked.remainingOmens())
        .containsExactly(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id());
  }
}
