package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class BowPerfectEssenceTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadBow();
  final WorkbenchSimulator simulator =
      new WorkbenchSimulator(
          catalog,
          new CraftingEngine(catalog),
          catalog.modifiers().values().stream()
              .filter(d -> d.stats().size() > 1)
              .map(ModifierDefinition::id)
              .collect(java.util.stream.Collectors.toSet()),
          BowEssenceTargets.VERIFIED);

  static Stream<WorkbenchCurrency> actions() {
    return Stream.of(
        WorkbenchCurrency.PERFECT_ESSENCE_ABRASION,
        WorkbenchCurrency.PERFECT_ESSENCE_FLAMES,
        WorkbenchCurrency.PERFECT_ESSENCE_ICE,
        WorkbenchCurrency.PERFECT_ESSENCE_ELECTRICITY,
        WorkbenchCurrency.PERFECT_ESSENCE_BATTLE,
        WorkbenchCurrency.PERFECT_ESSENCE_HASTE);
  }

  ItemState rare(int level, String locked, String... ids) {
    return new ItemState(
        catalog.metadata().snapshotId(),
        BowEssenceTargets.BASE_ID,
        level,
        ItemState.Rarity.RARE,
        List.of(),
        Arrays.stream(ids)
            .map(
                id -> {
                  var d = catalog.find(id).orElseThrow();
                  var values = new LinkedHashMap<String, Long>();
                  d.stats().forEach(s -> values.put(s.id(), s.min()));
                  return new ModifierInstance(id, values, id.equals(locked));
                })
            .toList(),
        Set.of());
  }

  @ParameterizedTest
  @MethodSource("actions")
  void sourcedFixedResultAndUniformRemovalPreserveOtherValues(WorkbenchCurrency action) {
    var d = catalog.find(action.replacementModifiers().getFirst()).orElseThrow();
    assertThat(d.weight()).isZero();
    assertThat(d.requiredItemLevel()).isEqualTo(72);
    var range = d.stats().getFirst();
    var before = rare(72, "", "crude-bow:prefix:focused", "crude-bow:suffix:of-ease");
    var seen = new HashSet<Long>();
    var removed = new HashSet<String>();
    for (int i = 0; i < 150; i++) {
      var r = simulator.apply(before, action, Set.of(), new Random(i * 104729L));
      assertThat(r.applied()).isTrue();
      assertThat(r.events()).hasSize(2);
      assertThat(r.events().getFirst().selectionProbability()).isEqualTo(.5);
      assertThat(r.events().getLast().selectionProbability()).isEqualTo(1);
      assertThat(r.events().getLast().modifierId()).isEqualTo(d.id());
      seen.add(r.events().getLast().values().get(range.id()));
      removed.add(r.events().getFirst().modifierId());
      assertThat(r.assumptions())
          .extracting(WorkbenchSimulator.Assumption::id)
          .containsExactlyElementsOf(
              range.min() == range.max()
                  ? List.of("uniform-removal-v1")
                  : List.of("uniform-removal-v1", "assumed-source-integer-roll-v1"));
      if (range.min() != range.max())
        assertThat(r.assumptions().getLast().sourceUrl()).isEqualTo(d.sourceUrl());
      if (range.min() != range.max())
        assertThat(r.assumptions().getLast().reason()).contains("UNVERIFIED");
      before.explicits().stream()
          .filter(m -> !m.modifierId().equals(r.events().getFirst().modifierId()))
          .forEach(m -> assertThat(r.state().explicits()).contains(m));
      assertThat(new ItemStateValidator(catalog).validate(r.state())).isEmpty();
    }
    assertThat(removed).hasSize(2);
    assertThat(seen).hasSize((int) (range.max() - range.min() + 1));
  }

  @ParameterizedTest
  @MethodSource("actions")
  void refusalConditionsAndCrystallisationAreAtomic(WorkbenchCurrency action) {
    var target = action.replacementModifiers().getFirst();
    for (var before :
        List.of(
            rare(71, "", "crude-bow:prefix:focused"),
            rare(57, "", "crude-bow:prefix:focused"),
            rare(82, target, target),
            rare(82, "", target, "crude-bow:prefix:focused"))) {
      var r = simulator.apply(before, action, Set.of(), new Random(17));
      assertThat(r.applied()).isFalse();
      assertThat(r.state()).isEqualTo(before);
      assertThat(r.events()).isEmpty();
    }
    var before =
        rare(
            82,
            "crude-bow:prefix:focused",
            "crude-bow:prefix:focused",
            "crude-bow:prefix:burnished",
            "crude-bow:suffix:of-ease");
    for (var omen :
        List.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION, WorkbenchOmen.DEXTRAL_CRYSTALLISATION)) {
      var r =
          simulator.apply(
              before, action, Set.of(omen.id(), WorkbenchOmen.BLESSED.id()), new Random(17));
      assertThat(r.applied()).isTrue();
      assertThat(catalog.find(r.events().getFirst().modifierId()).orElseThrow().affixType())
          .isEqualTo(omen.affix());
      assertThat(r.events().getFirst().selectionProbability()).isEqualTo(1);
      assertThat(r.consumedOmens()).containsExactly(omen.id());
      assertThat(r.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
      assertThat(r.state().explicits())
          .contains(
              before.explicits().stream()
                  .filter(ModifierInstance::fractured)
                  .findFirst()
                  .orElseThrow());
    }
    var r =
        simulator.apply(
            before,
            action,
            Set.of(
                WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id(),
                WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()),
            new Random(17));
    assertThat(r.applied()).isFalse();
    assertThat(r.state()).isEqualTo(before);
    assertThat(r.consumedOmens()).isEmpty();
  }

  @Test
  void sharedDamageFamilyDoesNotPruneInvalidBranchesAndOldFilmStillLoads() {
    var physical = WorkbenchCurrency.PERFECT_ESSENCE_ABRASION.replacementModifiers().getFirst();
    var before = rare(82, "", physical, "crude-bow:suffix:of-ease");
    var refused =
        simulator.apply(before, WorkbenchCurrency.PERFECT_ESSENCE_FLAMES, Set.of(), new Random(17));
    assertThat(refused.applied()).isFalse();
    var applied =
        simulator.apply(
            before,
            WorkbenchCurrency.PERFECT_ESSENCE_FLAMES,
            Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id()),
            new Random(17));
    assertThat(applied.applied()).isTrue();
    assertThat(catalog.compatibleSnapshotIds())
        .contains("poe2db-crude-bow-normal-2026-10-03-417390c5ebe4");
    var legacy =
        new ItemState(
            catalog.metadata().snapshotId(),
            before.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            rare(82, "", "crude-bow:prefix:focused").explicits(),
            Set.of());
    assertThat(
            simulator
                .apply(legacy, WorkbenchCurrency.PERFECT_ESSENCE_HASTE, Set.of(), new Random(17))
                .applied())
        .isTrue();
    for (int i = 0; i < 100; i++) {
      var normal =
          new ItemState(
              catalog.metadata().snapshotId(),
              BowEssenceTargets.BASE_ID,
              82,
              ItemState.Rarity.NORMAL,
              List.of(),
              List.of(),
              Set.of());
      var natural = simulator.apply(normal, WorkbenchCurrency.ALCHEMY, Set.of(), new Random(i));
      assertThat(natural.applied()).isTrue();
      assertThat(natural.state().explicits())
          .allSatisfy(
              m -> assertThat(catalog.find(m.modifierId()).orElseThrow().weight()).isPositive());
    }
  }
}
