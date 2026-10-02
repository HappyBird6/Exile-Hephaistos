package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PerfectInfiniteEssenceTest {
  final HysteriaEssenceTest fixture = new HysteriaEssenceTest();
  final WorkbenchSimulator simulator = fixture.simulator;
  final WorkbenchCurrency action = WorkbenchCurrency.PERFECT_ESSENCE_INFINITE;
  final List<String> targets = action.replacementEssenceModifiers();

  @Test
  void samplesExactSourcedResultsAndSideRestrictedUnlockedRemovalWithSeparateAssumptions() {
    var before =
        fixture.rare(
            72,
            "amulet:prefix:hale",
            "amulet:prefix:hale",
            "amulet:prefix:adept-s",
            "amulet:suffix:of-the-brute",
            "amulet:suffix:of-the-penguin");
    for (var omens :
        List.of(
            Set.of(WorkbenchOmen.BLESSED.id()),
            Set.of(WorkbenchOmen.BLESSED.id(), WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id()),
            Set.of(WorkbenchOmen.BLESSED.id(), WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()))) {
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
      var removals = new HashSet<String>();
      var additions = new HashSet<String>();
      for (int seed = 0; seed < 100; seed++) {
        var result = simulator.apply(before, action, omens, new Random(seed * 104729L));
        assertThat(result.applied()).isTrue();
        assertThat(result.state().explicits()).hasSize(before.explicits().size());
        assertThat(result.state().implicits()).isEqualTo(before.implicits());
        assertThat(result.events()).hasSize(2);
        var removed = result.events().getFirst();
        var added = result.events().getLast();
        assertThat(removed.kind()).isEqualTo("REMOVE");
        assertThat(removed.selectionProbability()).isEqualTo(1.0 / eligible.size());
        assertThat(eligible.stream().map(ModifierInstance::modifierId))
            .contains(removed.modifierId());
        assertThat(added.kind()).isEqualTo("ADD");
        assertThat(targets).contains(added.modifierId());
        assertThat(added.selectionProbability()).isEqualTo(1.0 / 3);
        assertThat(added.values().values()).allMatch(v -> v >= 7 && v <= 10);
        for (var old : before.explicits())
          if (!old.modifierId().equals(removed.modifierId()))
            assertThat(result.state().explicits()).contains(old);
        assertThat(result.assumptions().stream().map(WorkbenchSimulator.Assumption::id))
            .containsExactly(
                "uniform-removal-v1", "uniform-essence-choice-v1", "uniform-integer-roll-v1");
        assertThat(result.assumptions().get(0).n()).isEqualTo(eligible.size());
        assertThat(result.assumptions().get(0).candidates())
            .containsExactlyElementsOf(
                eligible.stream().map(ModifierInstance::modifierId).toList());
        assertThat(result.assumptions().get(1).candidates()).containsExactlyElementsOf(targets);
        assertThat(result.assumptions().get(1).sourceUrl())
            .isEqualTo(action.replacementEssenceSource());
        assertThat(result.assumptions().get(2).n()).isEqualTo(4);
        assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
        assertThat(result.consumedOmens()).hasSize(side == null ? 0 : 1);
        assertThat(new ItemStateValidator(fixture.catalog).validateForGeneration(result.state()))
            .isEmpty();
        removals.add(removed.modifierId());
        additions.add(added.modifierId());
      }
      assertThat(removals)
          .containsExactlyInAnyOrderElementsOf(
              eligible.stream().map(ModifierInstance::modifierId).toList());
      assertThat(additions).containsExactlyInAnyOrderElementsOf(targets);
    }
  }

  @Test
  void blocksUnknownOverlapOrSlotBranchesAndWrongRarityLevelLocksOrPairedOmensWithoutDrawing() {
    var pair =
        Set.of(
            WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id(),
            WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id());
    var ordinary = fixture.rare(82, "", "amulet:prefix:hale", "amulet:suffix:of-the-brute");
    for (var before :
        List.of(
            fixture.root,
            fixture.rare(82, ""),
            fixture.rare(71, "", "amulet:prefix:hale"),
            fixture.rare(82, "amulet:prefix:hale", "amulet:prefix:hale"),
            fixture.rare(82, "", "amulet:prefix:hale", targets.getFirst()),
            fixture.rare(82, targets.getFirst(), targets.getFirst()),
            fixture.rare(
                82,
                "",
                "amulet:prefix:hale",
                "amulet:suffix:of-the-brute",
                "amulet:suffix:of-the-penguin",
                "amulet:suffix:of-the-salamander"))) {
      var result =
          simulator.apply(
              before,
              action,
              Set.of(WorkbenchOmen.BLESSED.id()),
              new Random(1) {
                @Override
                public int nextInt(int bound) {
                  throw new AssertionError("Blocked action must not draw");
                }
              });
      assertThat(result.applied()).isFalse();
      assertThat(result.state()).isEqualTo(before);
      assertThat(result.events()).isEmpty();
      assertThat(result.assumptions()).isEmpty();
      assertThat(result.consumedOmens()).isEmpty();
    }
    assertThat(simulator.apply(ordinary, action, pair, new Random(1)).applied()).isFalse();
    assertThat(
            simulator
                .apply(
                    fixture.rare(82, "", "amulet:prefix:hale"),
                    action,
                    Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()),
                    new Random(1))
                .applied())
        .isFalse();
    var full =
        fixture.rare(
            82,
            "amulet:prefix:hale",
            "amulet:prefix:hale",
            "amulet:suffix:of-the-brute",
            "amulet:suffix:of-the-penguin",
            "amulet:suffix:of-the-salamander");
    assertThat(
            simulator
                .apply(
                    full, action, Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()), new Random(1))
                .applied())
        .isTrue();
  }

  @Test
  void canReplaceTheOnlyUnlockedPercentageSuffixAndPreservesUnrelatedOmens() {
    for (var target : targets) {
      var before = fixture.rare(82, "amulet:prefix:hale", "amulet:prefix:hale", target);
      for (int seed = 0; seed < 30; seed++) {
        var result =
            simulator.apply(
                before,
                action,
                Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id(), WorkbenchOmen.WHITTLING.id()),
                new Random(seed * 104729L));
        assertThat(result.applied()).isTrue();
        assertThat(result.events().getFirst().modifierId()).isEqualTo(target);
        assertThat(result.state().explicits()).hasSize(2);
        assertThat(result.state().explicits())
            .contains(
                before.explicits().stream()
                    .filter(ModifierInstance::fractured)
                    .findFirst()
                    .orElseThrow());
        assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.WHITTLING.id());
      }
    }
  }

  @Test
  void specialSuffixCanFractureAndRemainsUnchangedThroughDivineChaosAndAnnulment() {
    for (var target : targets) {
      var before =
          fixture.rare(
              82,
              "",
              "amulet:prefix:hale",
              "amulet:prefix:adept-s",
              "amulet:suffix:of-the-brute",
              target);
      ItemState locked = null;
      for (int seed = 0; seed < 100; seed++) {
        var result =
            simulator.apply(
                before, WorkbenchCurrency.FRACTURING, Set.of(), new Random(seed * 104729L));
        assertThat(result.applied()).isTrue();
        if (result.events().getFirst().modifierId().equals(target)) {
          locked = result.state();
          assertThat(result.events().getFirst().selectionProbability()).isEqualTo(0.25);
          break;
        }
      }
      assertThat(locked).isNotNull();
      var special =
          locked.explicits().stream().filter(ModifierInstance::fractured).findFirst().orElseThrow();
      for (var currency :
          List.of(WorkbenchCurrency.DIVINE, WorkbenchCurrency.CHAOS, WorkbenchCurrency.ANNULMENT)) {
        for (int seed = 0; seed < 30; seed++) {
          var result = simulator.apply(locked, currency, Set.of(), new Random(seed * 104729L));
          assertThat(result.applied()).isTrue();
          assertThat(result.state().explicits()).contains(special);
          assertThat(result.events())
              .noneMatch(e -> e.kind().equals("REMOVE") && e.modifierId().equals(target));
          assertThat(result.events().stream().filter(e -> e.kind().equals("ADD")))
              .allMatch(e -> fixture.catalog.find(e.modifierId()).orElseThrow().weight() > 0);
        }
      }
    }
  }
}
