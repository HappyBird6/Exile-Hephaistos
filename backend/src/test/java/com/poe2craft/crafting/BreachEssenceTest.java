package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class BreachEssenceTest {
  final HysteriaEssenceTest fixture = new HysteriaEssenceTest();
  final WorkbenchSimulator simulator = fixture.simulator;
  final WorkbenchCurrency action = WorkbenchCurrency.ESSENCE_BREACH;
  final String target = "amulet:prefix:essence-maximum-quality";

  @Test
  void sourcedLevelOneFixedResultNeedsNoChoiceOrNumericProbabilityAndRejectsSpecialConditions() {
    var before = fixture.rare(1, "", "amulet:prefix:hale");
    var result = simulator.apply(before, action, Set.of(), new Random(1));
    assertThat(result.applied()).isTrue();
    assertThat(result.events().getLast().values())
        .containsExactly(entry("local_maximum_quality_+", 20L));
    assertThat(result.events().getLast().selectionProbability()).isEqualTo(1);
    assertThat(result.assumptions()).hasSize(1);
    var corrupted =
        new ItemState(
            before.snapshotId(),
            before.baseItemId(),
            1,
            before.rarity(),
            before.implicits(),
            before.explicits(),
            Set.of(ItemState.Condition.CORRUPTED));
    assertThatThrownBy(() -> simulator.apply(corrupted, action, Set.of(), new Random(1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported concrete item state");
  }

  @Test
  void addsExactFixedMaximumQualityAndSamplesEveryUnlockedSideCandidateWithoutInventedRolls() {
    var before =
        fixture.rare(
            72,
            "amulet:prefix:hale",
            "amulet:prefix:hale",
            "amulet:prefix:adept-s",
            "amulet:suffix:of-the-brute",
            "amulet:suffix:of-the-penguin");
    for (var omen :
        Arrays.asList(
            null, WorkbenchOmen.SINISTRAL_CRYSTALLISATION, WorkbenchOmen.DEXTRAL_CRYSTALLISATION)) {
      var ids = new HashSet<>(Set.of(WorkbenchOmen.BLESSED.id()));
      if (omen != null) ids.add(omen.id());
      var side =
          omen == null
              ? null
              : omen == WorkbenchOmen.SINISTRAL_CRYSTALLISATION
                  ? ModifierDefinition.AffixType.PREFIX
                  : ModifierDefinition.AffixType.SUFFIX;
      var eligible =
          before.explicits().stream()
              .filter(m -> !m.fractured())
              .filter(
                  m ->
                      side == null
                          || fixture.catalog.find(m.modifierId()).orElseThrow().affixType() == side)
              .toList();
      var seen = new HashSet<String>();
      var rolls = new HashSet<Long>();
      for (int seed = 0; seed < 100; seed++) {
        var result = simulator.apply(before, action, ids, new Random(seed * 104729L));
        assertThat(result.applied()).isTrue();
        assertThat(result.events()).hasSize(2);
        var removed = result.events().getFirst();
        var added = result.events().getLast();
        assertThat(removed.kind()).isEqualTo("REMOVE");
        assertThat(removed.selectionProbability()).isEqualTo(1.0 / eligible.size());
        assertThat(eligible.stream().map(ModifierInstance::modifierId))
            .contains(removed.modifierId());
        assertThat(added.kind()).isEqualTo("ADD");
        assertThat(added.modifierId()).isEqualTo(target);
        assertThat(added.selectionProbability()).isEqualTo(1);
        assertThat(added.values()).hasSize(1).containsKey("local_maximum_quality_+");
        assertThat(added.values().values()).allMatch(v -> v == 20);
        assertThat(result.assumptions().stream().map(WorkbenchSimulator.Assumption::id))
            .containsExactly("uniform-removal-v1");
        assertThat(result.assumptions().getFirst().n()).isEqualTo(eligible.size());
        assertThat(result.assumptions().getFirst().candidates())
            .containsExactlyElementsOf(
                eligible.stream().map(ModifierInstance::modifierId).toList());
        assertThat(result.state().implicits()).isEqualTo(before.implicits());
        for (var old : before.explicits())
          if (!old.modifierId().equals(removed.modifierId()))
            assertThat(result.state().explicits()).contains(old);
        assertThat(result.state().explicits()).hasSize(before.explicits().size());
        assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
        assertThat(result.consumedOmens()).hasSize(omen == null ? 0 : 1);
        assertThat(new ItemStateValidator(fixture.catalog).validateForGeneration(result.state()))
            .isEmpty();
        seen.add(removed.modifierId());
        rolls.addAll(added.values().values());
      }
      assertThat(seen)
          .containsExactlyInAnyOrderElementsOf(
              eligible.stream().map(ModifierInstance::modifierId).toList());
      assertThat(rolls).containsExactly(20L);
    }
  }

  @Test
  void blocksWrongRarityLockedOverlapAndIncompletePrefixBranchesWithoutDrawing() {
    var full =
        fixture.rare(
            82,
            "",
            "amulet:prefix:hale",
            "amulet:prefix:adept-s",
            "amulet:prefix:azure",
            "amulet:suffix:of-the-brute");
    var magic =
        new ItemState(
            fixture.root.snapshotId(),
            fixture.root.baseItemId(),
            82,
            ItemState.Rarity.MAGIC,
            fixture.root.implicits(),
            List.of(),
            fixture.root.conditions());
    for (var before :
        List.of(
            fixture.root,
            magic,
            fixture.rare(82, ""),
            fixture.rare(82, target, target),
            fixture.rare(82, "", target, "amulet:suffix:of-the-brute"),
            full)) {
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
    assertThat(
            simulator
                .apply(
                    full,
                    action,
                    Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id()),
                    new Random(1))
                .applied())
        .isTrue();
    assertThat(
            simulator
                .apply(
                    full, action, Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()), new Random(1))
                .applied())
        .isFalse();
    assertThat(
            simulator
                .apply(
                    full,
                    action,
                    Set.of(
                        WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id(),
                        WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id()),
                    new Random(1))
                .applied())
        .isFalse();
    assertThat(
            simulator
                .apply(
                    fixture.rare(82, "", "amulet:prefix:hale"),
                    action,
                    Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()),
                    new Random(1))
                .applied())
        .isFalse();
  }

  @Test
  void replacingSoleUnlockedMaximumQualityWorksAndFracturedOtherEssenceResultIsPreserved() {
    var other = "amulet:suffix:essence-percent-strength";
    var before = fixture.rare(82, other, target, other);
    var result =
        simulator.apply(
            before,
            action,
            Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id(), WorkbenchOmen.WHITTLING.id()),
            new Random(5));
    assertThat(result.applied()).isTrue();
    assertThat(result.events().getFirst().modifierId()).isEqualTo(target);
    assertThat(result.state().explicits()).contains(before.explicits().getLast());
    assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.WHITTLING.id());
  }

  @Test
  void maximumQualityPrefixCanFractureAndIsPreservedByDivineChaosAnnulmentAndOtherPerfectEssence() {
    var before =
        fixture.rare(
            82,
            "",
            target,
            "amulet:prefix:hale",
            "amulet:suffix:of-the-brute",
            "amulet:suffix:of-the-penguin");
    ItemState locked = null;
    for (int seed = 0; seed < 100; seed++) {
      var result =
          simulator.apply(
              before, WorkbenchCurrency.FRACTURING, Set.of(), new Random(seed * 104729L));
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
        List.of(
            WorkbenchCurrency.DIVINE,
            WorkbenchCurrency.CHAOS,
            WorkbenchCurrency.ANNULMENT,
            WorkbenchCurrency.PERFECT_ESSENCE_INFINITE))
      for (int seed = 0; seed < 30; seed++) {
        var result = simulator.apply(locked, currency, Set.of(), new Random(seed * 104729L));
        assertThat(result.applied()).isTrue();
        assertThat(result.state().explicits()).contains(special);
        assertThat(result.events())
            .noneMatch(e -> e.kind().equals("REMOVE") && e.modifierId().equals(target));
        if (currency == WorkbenchCurrency.CHAOS)
          assertThat(result.events().stream().filter(e -> e.kind().equals("ADD")))
              .allMatch(e -> fixture.catalog.find(e.modifierId()).orElseThrow().weight() > 0);
      }
    assertThat(simulator.apply(locked, action, Set.of(), new Random(1)).applied()).isFalse();
  }
}
