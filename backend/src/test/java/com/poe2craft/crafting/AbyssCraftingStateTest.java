package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.ModifierDefinition.AffixType;
import java.util.*;
import org.junit.jupiter.api.Test;

class AbyssCraftingStateTest {
  final HysteriaEssenceTest fixture = new HysteriaEssenceTest();
  final ItemCatalog catalog = fixture.catalog;

  AbyssCraftingState start(String... ids) {
    return AbyssCraftingState.from(fixture.rare(82, "", ids), catalog);
  }

  ModifierInstance modifier(String id) {
    return fixture.rare(82, "", id).explicits().getFirst();
  }

  @Test
  void unrevealedCountsAsSlotAndRemovalTargetWithoutInventedStats() {
    var before = start("amulet:prefix:hale", "amulet:prefix:adept-s");
    var state = before.addUnrevealed("abyss", AffixType.PREFIX, catalog);
    assertThat(state.count(AffixType.PREFIX)).isEqualTo(3);
    assertThat(state.knownItem()).isEqualTo(before.knownItem());
    assertThat(state.removalCandidates(AffixType.PREFIX)).hasSize(3);
    assertThat(state.removalCandidates(AffixType.SUFFIX)).isEmpty();
    assertThat(state.remove("abyss", null, catalog).slots()).isEqualTo(before.slots());
    assertThatThrownBy(() -> state.addUnrevealed("second", AffixType.PREFIX, catalog))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void observedChoiceReworksTheSameSlotAtSixWithoutChangingOtherRolls() {
    var state =
        start(
                "amulet:prefix:hale",
                "amulet:prefix:adept-s",
                "amulet:suffix:of-the-brute",
                "amulet:suffix:of-the-penguin",
                "amulet:suffix:of-the-salamander")
            .addUnrevealed("abyss", AffixType.PREFIX, catalog);
    var candidate = modifier("amulet:prefix:azure");
    var observed = state.observe("abyss", List.of(candidate, candidate, candidate), catalog);
    var chosen = observed.choose(1, catalog);
    assertThat(chosen.slots()).hasSize(6);
    assertThat(chosen.slots().subList(0, 5)).isEqualTo(state.slots().subList(0, 5));
    assertThat(chosen.slots().getLast().modifier()).isEqualTo(candidate);
    assertThat(chosen.offer()).isNull();
    assertThat(
            chosen
                .observe("abyss", List.of(candidate, candidate, candidate), catalog)
                .choose(0, catalog))
        .isEqualTo(chosen);
  }

  @Test
  void invalidOffersAreRejectedRatherThanSilentlyPruned() {
    var state = start("amulet:prefix:hale").addUnrevealed("abyss", AffixType.PREFIX, catalog);
    var valid = modifier("amulet:prefix:azure");
    var conflict = modifier("amulet:prefix:hale");
    var wrongSide = modifier("amulet:suffix:of-the-brute");
    assertThatThrownBy(() -> state.observe("abyss", List.of(valid, valid), catalog))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> state.observe("abyss", List.of(valid, valid, conflict), catalog))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> state.observe("abyss", List.of(valid, valid, wrongSide), catalog))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> state.choose(0, catalog)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> state.observe("explicit-0", List.of(valid, valid, valid), catalog))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void fracturedOptionsStayOutsideDummyRemovalPool() {
    var concrete =
        fixture.rare(82, "amulet:prefix:hale", "amulet:prefix:hale", "amulet:prefix:adept-s");
    var state =
        AbyssCraftingState.from(concrete, catalog)
            .addUnrevealed("abyss", AffixType.PREFIX, catalog);
    assertThat(state.removalCandidates(null)).hasSize(2);
    assertThatThrownBy(() -> state.remove("explicit-1", null, catalog))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
