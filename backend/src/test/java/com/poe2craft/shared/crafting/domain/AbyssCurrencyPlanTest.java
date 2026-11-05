package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import java.util.*;
import org.junit.jupiter.api.Test;

class AbyssCurrencyPlanTest {
  final HysteriaEssenceTest fixture = new HysteriaEssenceTest();

  @Test
  void dummySharesEssenceRemovalRiskAndCanBeRemovedWithoutLosingTheUsefulAffix() {
    var start =
        AbyssCraftingState.from(fixture.rare(82, "", "amulet:prefix:hale"), fixture.catalog);
    var state = start.addUnrevealed("abyss", AffixType.SUFFIX, fixture.catalog);
    var plan = fixture.simulator.abyssPlan(state, WorkbenchCurrency.ESSENCE_HYSTERIA, Set.of());
    assertThat(plan.available()).isTrue();
    assertThat(plan.branches()).hasSize(2).allMatch(b -> b.removalProbability() == .5);
    var dummy =
        plan.branches().stream()
            .filter(b -> b.removedSlotId().equals("abyss"))
            .findFirst()
            .orElseThrow();
    assertThat(dummy.remaining().knownItem()).isEqualTo(start.knownItem());
    assertThat(dummy.candidates())
        .extracting(ModifierDefinition::id)
        .containsExactly(fixture.target);
    var outcomes = new HashSet<String>();
    for (int seed = 0; seed < 100; seed++) {
      var result =
          fixture.simulator.applyAbyss(
              state, WorkbenchCurrency.ESSENCE_HYSTERIA, Set.of(), new Random(seed * 104729L));
      assertThat(result.slots()).hasSize(2);
      result.validate(fixture.catalog);
      outcomes.add(
          result.slots().stream().anyMatch(s -> s.modifier() == null)
              ? "kept-dummy"
              : "removed-dummy");
    }
    assertThat(outcomes).containsExactlyInAnyOrder("kept-dummy", "removed-dummy");
  }

  @Test
  void removalSideRestrictionsIncludeOnlyTheMatchingDummy() {
    var state =
        AbyssCraftingState.from(fixture.rare(82, "", "amulet:prefix:hale"), fixture.catalog)
            .addUnrevealed("abyss", AffixType.SUFFIX, fixture.catalog);
    var plan =
        fixture.simulator.abyssPlan(
            state,
            WorkbenchCurrency.ESSENCE_HYSTERIA,
            Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()));
    assertThat(plan.branches()).hasSize(1);
    assertThat(plan.branches().getFirst().removedSlotId()).isEqualTo("abyss");
    assertThat(plan.branches().getFirst().removalProbability()).isEqualTo(1);
  }

  @Test
  void twoUsefulPrefixesPlusDummyForceFollowingAdditionOntoSuffixes() {
    var state =
        AbyssCraftingState.from(
                fixture.rare(82, "", "amulet:prefix:hale", "amulet:prefix:adept-s"),
                fixture.catalog)
            .addUnrevealed("abyss", AffixType.PREFIX, fixture.catalog);
    var result =
        fixture.simulator.applyAbyss(state, WorkbenchCurrency.EXALTED, Set.of(), new Random(1));
    assertThat(result.slots()).containsAll(state.slots());
    assertThat(result.count(AffixType.PREFIX)).isEqualTo(3);
    assertThat(result.count(AffixType.SUFFIX)).isEqualTo(1);
  }
}
