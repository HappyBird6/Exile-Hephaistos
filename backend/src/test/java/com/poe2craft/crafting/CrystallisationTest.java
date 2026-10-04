package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CrystallisationTest {
  final HysteriaEssenceTest fixture = new HysteriaEssenceTest();

  @Test
  void restrictsUnlockedRemovalBySideConsumesExactlyOneAndPreservesUnrelatedOmens() {
    var before =
        fixture.rare(
            82,
            "amulet:prefix:hale",
            "amulet:prefix:hale",
            "amulet:prefix:adept-s",
            "amulet:suffix:of-the-brute",
            "amulet:suffix:of-the-penguin");
    for (var omen :
        List.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION, WorkbenchOmen.DEXTRAL_CRYSTALLISATION)) {
      var candidates =
          before.explicits().stream()
              .filter(
                  m ->
                      !m.fractured()
                          && fixture.catalog.find(m.modifierId()).orElseThrow().affixType()
                              == omen.affix())
              .toList();
      var reached = new HashSet<String>();
      for (int seed = 0; seed < 100; seed++) {
        var result =
            fixture.simulator.apply(
                before,
                fixture.action,
                Set.of(omen.id(), WorkbenchOmen.BLESSED.id()),
                new Random(seed * 104729L));
        assertThat(result.applied()).isTrue();
        var removed = result.events().getFirst();
        assertThat(candidates.stream().map(ModifierInstance::modifierId))
            .contains(removed.modifierId());
        assertThat(removed.selectionProbability()).isEqualTo(1.0 / candidates.size());
        assertThat(result.assumptions().getFirst().candidates())
            .containsExactlyElementsOf(
                candidates.stream().map(ModifierInstance::modifierId).toList());
        assertThat(result.assumptions().getFirst().sourceUrl())
            .isEqualTo("https://poe2db.tw/us/" + omen.id());
        for (var old : before.explicits())
          if (!old.modifierId().equals(removed.modifierId()))
            assertThat(result.state().explicits()).contains(old);
        assertThat(result.consumedOmens()).containsExactly(omen.id());
        assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
        reached.add(removed.modifierId());
      }
      assertThat(reached)
          .containsExactlyInAnyOrderElementsOf(
              candidates.stream().map(ModifierInstance::modifierId).toList());
    }
    var normal =
        fixture.simulator.apply(
            fixture.root,
            WorkbenchCurrency.TRANSMUTATION,
            Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()),
            new Random(1));
    assertThat(normal.applied()).isTrue();
    assertThat(normal.consumedOmens()).isEmpty();
    assertThat(normal.remainingOmens()).containsExactly(WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id());
  }

  @Test
  void conflictsEmptySidesAndInvalidReplacementBranchesPreserveActivationAndState() {
    var both =
        Set.of(
            WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id(),
            WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id());
    var before = fixture.rare(82, "", "amulet:prefix:hale", "amulet:suffix:of-the-brute");
    var conflict = fixture.simulator.apply(before, fixture.action, both, new Random(1));
    assertThat(conflict.applied()).isFalse();
    assertThat(conflict.state()).isEqualTo(before);
    assertThat(conflict.remainingOmens()).containsExactlyInAnyOrderElementsOf(both);
    for (var blocked :
        List.of(
            fixture.rare(
                82,
                "amulet:suffix:of-the-brute",
                "amulet:prefix:hale",
                "amulet:suffix:of-the-brute"),
            fixture.rare(82, "", "amulet:prefix:hale"))) {
      var result =
          fixture.simulator.apply(
              blocked,
              fixture.action,
              Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()),
              new Random(1));
      assertThat(result.applied()).isFalse();
      assertThat(result.state()).isEqualTo(blocked);
      assertThat(result.consumedOmens()).isEmpty();
    }
    var full =
        fixture.rare(
            82,
            "",
            "amulet:prefix:hale",
            "amulet:suffix:of-the-brute",
            "amulet:suffix:of-the-penguin",
            "amulet:suffix:of-the-salamander");
    assertThat(
            fixture
                .simulator
                .apply(
                    full,
                    fixture.action,
                    Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id()),
                    new Random(1))
                .applied())
        .isFalse();
    assertThat(
            fixture
                .simulator
                .apply(
                    full,
                    fixture.action,
                    Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()),
                    new Random(1))
                .applied())
        .isTrue();
  }
}
