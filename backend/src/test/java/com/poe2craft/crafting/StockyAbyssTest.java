package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class StockyAbyssTest {
  final StockyHysteriaTest fixture = new StockyHysteriaTest();
  final WorkbenchSimulator simulator = fixture.simulator;
  final WorkbenchCurrency action = WorkbenchCurrency.ESSENCE_ABYSS;
  final List<String> targets = StockyEssenceTargets.REPLACEMENTS.get(action);

  @Test
  void bothSourcedAffixesAndEveryUnlockedRemovalRemainReachableWithSeparateAssumptions() {
    var before =
        fixture.rare(
            82,
            "stocky-mitts:prefix:sanguine",
            "stocky-mitts:prefix:sanguine",
            "stocky-mitts:suffix:of-the-brute");
    for (var omens :
        List.of(
            Set.of(WorkbenchOmen.BLESSED.id()),
            Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id()),
            Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()))) {
      var side =
          omens.contains(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id())
              ? ModifierDefinition.AffixType.PREFIX
              : omens.contains(WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id())
                  ? ModifierDefinition.AffixType.SUFFIX
                  : null;
      var eligible =
          before.explicits().stream()
              .filter(m -> !m.fractured())
              .filter(
                  m ->
                      side == null
                          || fixture.catalog.find(m.modifierId()).orElseThrow().affixType() == side)
              .toList();
      if (eligible.isEmpty()) {
        assertThat(simulator.apply(before, action, omens, new Random()).applied()).isFalse();
        continue;
      }
      var seen = new HashSet<String>();
      for (int seed = 0; seed < 100; seed++) {
        var result = simulator.apply(before, action, omens, new Random(seed * 104729L));
        assertThat(result.applied()).isTrue();
        assertThat(result.state().implicits()).isEqualTo(before.implicits());
        assertThat(result.state().explicits())
            .hasSize(2)
            .contains(
                before.explicits().stream()
                    .filter(ModifierInstance::fractured)
                    .findFirst()
                    .orElseThrow());
        assertThat(result.events()).hasSize(2);
        assertThat(result.events().getFirst().modifierId())
            .isEqualTo("stocky-mitts:suffix:of-the-brute");
        var added = result.events().getLast();
        assertThat(targets).contains(added.modifierId());
        assertThat(added.values()).containsExactly(entry("essence_abyss_guaranteed_pick", 1L));
        assertThat(added.selectionProbability()).isEqualTo(.5);
        seen.add(added.modifierId());
        assertThat(result.assumptions())
            .extracting(WorkbenchSimulator.Assumption::id)
            .containsExactly("uniform-removal-v1", "uniform-essence-choice-v1");
        assertThat(result.assumptions().getLast().n()).isEqualTo(2);
        assertThat(result.assumptions().getLast().candidates()).containsExactlyElementsOf(targets);
        assertThat(result.consumedOmens())
            .containsExactlyElementsOf(side == null ? Set.of() : omens);
        assertThat(result.remainingOmens())
            .containsExactlyElementsOf(side == null ? omens : Set.of());
      }
      assertThat(seen).containsExactlyInAnyOrderElementsOf(targets);
    }
  }

  @Test
  void fullAffixAndSurvivingSharedFamilyBlockRatherThanPruneOutcomesOrRemovalBranches() {
    var full =
        fixture.rare(
            82,
            "",
            "stocky-mitts:prefix:sanguine",
            "stocky-mitts:prefix:hunter-s",
            "stocky-mitts:prefix:azure",
            "stocky-mitts:suffix:of-the-brute");
    var overlap = fixture.rare(82, "", targets.getFirst(), "stocky-mitts:suffix:of-the-brute");
    var locked = fixture.rare(82, targets.getFirst(), targets.getFirst());
    for (var before : List.of(full, overlap, locked, fixture.root)) {
      var result =
          simulator.apply(
              before,
              action,
              Set.of(),
              new Random() {
                @Override
                public int nextInt(int bound) {
                  throw new AssertionError("blocked input must not draw");
                }
              });
      assertThat(result.applied()).isFalse();
      assertThat(result.state()).isEqualTo(before);
      assertThat(result.events()).isEmpty();
    }
    assertThat(
            simulator
                .apply(
                    full,
                    action,
                    Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id()),
                    new Random())
                .applied())
        .isTrue();
    assertThat(
            simulator
                .apply(
                    full, action, Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()), new Random())
                .applied())
        .isFalse();
  }

  @Test
  void soleUnlockedMarkCanBeReplacedWithoutDuplicateFamilyAndUnrelatedOmensSurvive() {
    for (var target : targets) {
      var before =
          fixture.rare(82, "stocky-mitts:prefix:sanguine", "stocky-mitts:prefix:sanguine", target);
      var result =
          simulator.apply(before, action, Set.of(WorkbenchOmen.BLESSED.id()), new Random(4));
      assertThat(result.applied()).isTrue();
      assertThat(result.state().explicits())
          .hasSize(2)
          .contains(
              before.explicits().stream()
                  .filter(ModifierInstance::fractured)
                  .findFirst()
                  .orElseThrow());
      assertThat(result.events().getFirst().modifierId()).isEqualTo(target);
      assertThat(
              result.state().explicits().stream()
                  .filter(m -> targets.contains(m.modifierId()))
                  .count())
          .isEqualTo(1);
      assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
    }
  }

  @Test
  void pairedCrystallisationIsRejectedBeforeDrawing() {
    var before = fixture.rare(82, "", "stocky-mitts:prefix:sanguine");
    var result =
        simulator.apply(
            before,
            action,
            Set.of(
                WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id(),
                WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()),
            new Random());
    assertThat(result.applied()).isFalse();
    assertThat(result.state()).isEqualTo(before);
  }

  @Test
  void eitherMarkCanFractureAndSurvivesDivineChaosAnnulmentWithoutEnteringOrdinaryPools() {
    for (var target : targets) {
      var before =
          fixture.rare(
              82,
              "",
              target,
              "stocky-mitts:prefix:sanguine",
              "stocky-mitts:suffix:of-the-brute",
              "stocky-mitts:suffix:of-the-penguin");
      var fracture =
          simulator.apply(
              before,
              WorkbenchCurrency.FRACTURING,
              Set.of(),
              new Random() {
                @Override
                public int nextInt(int bound) {
                  return before
                      .explicits()
                      .indexOf(
                          before.explicits().stream()
                              .filter(m -> m.modifierId().equals(target))
                              .findFirst()
                              .orElseThrow());
                }
              });
      assertThat(fracture.applied()).isTrue();
      assertThat(fracture.events().getFirst().modifierId()).isEqualTo(target);
      var mark =
          fracture.state().explicits().stream()
              .filter(m -> m.modifierId().equals(target))
              .findFirst()
              .orElseThrow();
      assertThat(mark.fractured()).isTrue();
      var current = fracture.state();
      for (var next :
          List.of(WorkbenchCurrency.DIVINE, WorkbenchCurrency.CHAOS, WorkbenchCurrency.ANNULMENT)) {
        var result = simulator.apply(current, next, Set.of(), new Random(12));
        assertThat(result.applied()).isTrue();
        assertThat(result.state().explicits()).contains(mark);
        if (next == WorkbenchCurrency.CHAOS)
          assertThat(
                  fixture
                      .catalog
                      .find(result.events().getLast().modifierId())
                      .orElseThrow()
                      .weight())
              .isPositive();
        current = result.state();
      }
    }
  }
}
