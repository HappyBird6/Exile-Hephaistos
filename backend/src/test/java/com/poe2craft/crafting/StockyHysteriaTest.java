package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class StockyHysteriaTest {
  final ItemCatalog catalog = stocky();

  static ItemCatalog stocky() {
    try (var data =
            StockyHysteriaTest.class.getResourceAsStream("/catalog/stocky-mitts/catalog.json");
        var raw =
            StockyHysteriaTest.class.getResourceAsStream("/catalog/stocky-mitts/base.raw.json");
        var details =
            StockyHysteriaTest.class.getResourceAsStream("/catalog/stocky-mitts/details.raw.json");
        var special =
            StockyHysteriaTest.class.getResourceAsStream(
                "/catalog/stocky-mitts/abyss-essence.catalog.json");
        var specialRaw =
            StockyHysteriaTest.class.getResourceAsStream(
                "/catalog/stocky-mitts/abyss-essence.raw.json");
        var horror =
            StockyHysteriaTest.class.getResourceAsStream(
                "/catalog/stocky-mitts/horror-essence.catalog.json");
        var horrorRaw =
            StockyHysteriaTest.class.getResourceAsStream(
                "/catalog/stocky-mitts/horror-essence.raw.json");
        var perfect =
            StockyHysteriaTest.class.getResourceAsStream(
                "/catalog/stocky-mitts/perfect-grounding-opulence.catalog.json");
        var perfectRaw =
            StockyHysteriaTest.class.getResourceAsStream(
                "/catalog/stocky-mitts/perfect-grounding-opulence.raw.json");
        var prismatic =
            StockyHysteriaTest.class.getResourceAsStream(
                "/catalog/stocky-mitts/prismatic-alloy.catalog.json");
        var scalar =
            StockyHysteriaTest.class.getResourceAsStream(
                "/catalog/stocky-mitts/scalar-alloys.catalog.json");
        var scalarRaw =
            StockyHysteriaTest.class.getResourceAsStream(
                "/catalog/stocky-mitts/scalar-alloys.raw.json");
        var reviewed =
            StockyHysteriaTest.class.getResourceAsStream(
                "/catalog/stocky-mitts/reviewed-alloys.catalog.json");
        var reviewedRaw =
            StockyHysteriaTest.class.getResourceAsStream(
                "/catalog/stocky-mitts/reviewed-alloys.raw.json");
        var prismaticRaw =
            StockyHysteriaTest.class.getResourceAsStream(
                "/catalog/stocky-mitts/prismatic-alloy.raw.json")) {
      return ItemCatalogLoader.addSpecial(
          ItemCatalogLoader.addSpecial(
              ItemCatalogLoader.addSpecial(
                  ItemCatalogLoader.addSpecial(
                      ItemCatalogLoader.addSpecial(
                          ItemCatalogLoader.loadWithSpecial(
                              data, raw, details, special, specialRaw),
                          horror,
                          horrorRaw),
                      perfect,
                      perfectRaw),
                  prismatic,
                  prismaticRaw),
              scalar,
              scalarRaw),
          reviewed,
          reviewedRaw);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  final WorkbenchSimulator simulator =
      new WorkbenchSimulator(
          catalog,
          new CraftingEngine(catalog),
          catalog.modifiers().values().stream()
              .filter(d -> d.stats().size() > 1)
              .map(ModifierDefinition::id)
              .collect(java.util.stream.Collectors.toSet()),
          StockyEssenceTargets.VERIFIED,
          StockyEssenceTargets.REPLACEMENTS);
  final ItemState root =
      new ItemState(
          catalog.metadata().snapshotId(),
          com.poe2craft.crafting.application.WorkbenchService.STOCKY_BASE_ID,
          82,
          ItemState.Rarity.NORMAL,
          List.of(),
          List.of(),
          Set.of());
  final WorkbenchCurrency action = WorkbenchCurrency.ESSENCE_HYSTERIA;
  final String target = "stocky-mitts:suffix:of-fury";

  ItemState rare(int level, String locked, String... ids) {
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        level,
        ItemState.Rarity.RARE,
        root.implicits(),
        Arrays.stream(ids)
            .map(
                id -> {
                  var stat = catalog.find(id).orElseThrow().stats().getFirst();
                  return new ModifierInstance(id, Map.of(stat.id(), stat.min()), id.equals(locked));
                })
            .toList(),
        root.conditions());
  }

  @Test
  void removesOnlyUnlockedInstancesAndPreservesEveryOtherRollWithExactEvidence() {
    var definition = catalog.find(target).orElseThrow();
    assertThat(definition.affixType()).isEqualTo(ModifierDefinition.AffixType.SUFFIX);
    assertThat(definition.stats().getFirst().min()).isEqualTo(25);
    assertThat(definition.stats().getFirst().max()).isEqualTo(29);
    for (var before :
        List.of(
            rare(45, "", "stocky-mitts:prefix:sanguine", "stocky-mitts:suffix:of-the-brute"),
            rare(
                82,
                "stocky-mitts:prefix:sanguine",
                "stocky-mitts:prefix:sanguine",
                "stocky-mitts:prefix:hunter-s",
                "stocky-mitts:suffix:of-the-brute",
                "stocky-mitts:suffix:of-the-penguin"),
            rare(
                82,
                "stocky-mitts:prefix:sanguine",
                "stocky-mitts:prefix:sanguine",
                "stocky-mitts:suffix:of-the-brute",
                "stocky-mitts:suffix:of-the-penguin",
                "stocky-mitts:suffix:of-the-salamander"))) {
      var eligible = before.explicits().stream().filter(m -> !m.fractured()).toList();
      var reached = new HashSet<String>();
      for (int seed = 0; seed < 100; seed++) {
        var result =
            simulator.apply(
                before,
                action,
                Set.of(WorkbenchOmen.BLESSED.id(), WorkbenchOmen.WHITTLING.id()),
                new Random(seed * 104729L));
        assertThat(result.applied()).isTrue();
        assertThat(result.state().explicits()).hasSize(before.explicits().size());
        assertThat(result.state().implicits()).isEqualTo(before.implicits());
        assertThat(result.events()).hasSize(2);
        var removed = result.events().getFirst();
        assertThat(removed.kind()).isEqualTo("REMOVE");
        assertThat(removed.selectionProbability()).isEqualTo(1.0 / eligible.size());
        assertThat(eligible.stream().map(ModifierInstance::modifierId))
            .contains(removed.modifierId());
        for (var old : before.explicits())
          if (!old.modifierId().equals(removed.modifierId()))
            assertThat(result.state().explicits()).contains(old);
        var added = result.events().getLast();
        assertThat(added.kind()).isEqualTo("ADD");
        assertThat(added.modifierId()).isEqualTo(target);
        assertThat(added.selectionProbability()).isEqualTo(1);
        assertThat(result.assumptions().getFirst().id()).isEqualTo("uniform-removal-v1");
        assertThat(result.assumptions().getFirst().candidates())
            .containsExactlyElementsOf(
                eligible.stream().map(ModifierInstance::modifierId).toList());
        assertThat(result.assumptions().getLast().n()).isEqualTo(5);
        assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
        assertThat(result.consumedOmens()).isEmpty();
        assertThat(result.remainingOmens())
            .containsExactlyInAnyOrder(WorkbenchOmen.BLESSED.id(), WorkbenchOmen.WHITTLING.id());
        reached.add(removed.modifierId());
      }
      assertThat(reached)
          .containsExactlyInAnyOrderElementsOf(
              eligible.stream().map(ModifierInstance::modifierId).toList());
    }
  }

  @Test
  void blocksAnyUnverifiedRemovalBranchWithoutFilteringOrMutatingTheItem() {
    var magic =
        simulator.apply(root, WorkbenchCurrency.TRANSMUTATION, Set.of(), new Random(1)).state();
    for (var before :
        List.of(
            root,
            magic,
            rare(82, ""),
            rare(44, "", "stocky-mitts:prefix:sanguine"),
            rare(82, target, target),
            rare(
                82,
                "",
                "stocky-mitts:prefix:sanguine",
                "stocky-mitts:suffix:of-the-brute",
                "stocky-mitts:suffix:of-the-penguin",
                "stocky-mitts:suffix:of-the-salamander"),
            rare(82, "", "stocky-mitts:prefix:sanguine", target))) {
      var result = simulator.apply(before, action, Set.of(), new Random(1));
      assertThat(result.applied()).isFalse();
      assertThat(result.state()).isEqualTo(before);
      assertThat(result.events()).isEmpty();
      assertThat(result.assumptions()).isEmpty();
    }
  }

  @Test
  void canReplaceTheSoleUnlockedCriticalBonusInstanceWithoutCreatingADuplicateFamily() {
    var result = simulator.apply(rare(82, "", target), action, Set.of(), new Random(1));
    assertThat(result.applied()).isTrue();
    assertThat(result.state().explicits()).hasSize(1);
    assertThat(result.events().getFirst().modifierId()).isEqualTo(target);
    assertThat(result.events().getFirst().selectionProbability()).isEqualTo(1);
    assertThat(result.events().getLast().modifierId()).isEqualTo(target);
  }

  @Test
  void crystallisationRestrictsRemovalAndConsumesOnlyTheMatchingOmen() {
    var before =
        rare(
            82,
            "stocky-mitts:prefix:sanguine",
            "stocky-mitts:prefix:sanguine",
            "stocky-mitts:prefix:hunter-s",
            "stocky-mitts:suffix:of-the-brute",
            "stocky-mitts:suffix:of-the-penguin");
    for (var omen :
        List.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION, WorkbenchOmen.DEXTRAL_CRYSTALLISATION)) {
      for (int seed = 0; seed < 40; seed++) {
        var result =
            simulator.apply(
                before,
                action,
                Set.of(omen.id(), WorkbenchOmen.BLESSED.id()),
                new Random(seed * 104729L));
        assertThat(result.applied()).isTrue();
        assertThat(catalog.find(result.events().getFirst().modifierId()).orElseThrow().affixType())
            .isEqualTo(omen.affix());
        assertThat(result.consumedOmens()).containsExactly(omen.id());
        assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
        assertThat(result.state().explicits())
            .contains(
                before.explicits().stream()
                    .filter(ModifierInstance::fractured)
                    .findFirst()
                    .orElseThrow());
      }
    }
    // Restriction makes the complete removal set a single overlapping target: replacement is safe.
    var overlap = rare(82, "", "stocky-mitts:prefix:sanguine", target);
    assertThat(
            simulator
                .apply(
                    overlap,
                    action,
                    Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()),
                    new Random(1))
                .applied())
        .isTrue();
    assertThat(simulator.apply(overlap, action, Set.of(), new Random(1)).applied()).isFalse();
    for (var omens :
        List.of(
            Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id()),
            Set.of(
                WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id(),
                WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()))) {
      var blocked = simulator.apply(rare(82, "", target), action, omens, new Random(1));
      assertThat(blocked.applied()).isFalse();
      assertThat(blocked.events()).isEmpty();
      assertThat(blocked.consumedOmens()).isEmpty();
      assertThat(blocked.remainingOmens()).containsExactlyInAnyOrderElementsOf(omens);
    }
    var lockedSide =
        rare(
            82,
            "stocky-mitts:prefix:sanguine",
            "stocky-mitts:prefix:sanguine",
            "stocky-mitts:suffix:of-the-brute");
    var denied =
        simulator.apply(
            lockedSide,
            action,
            Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id()),
            new Random(1));
    assertThat(denied.applied()).isFalse();
    assertThat(denied.state()).isEqualTo(lockedSide);
  }

  @Test
  void primaryHysteriaTargetHasExactCurrentSourceStats() throws Exception {
    try (var source = getClass().getResourceAsStream("/catalog/stocky-mitts/hysteria-proof.json")) {
      var proof = new com.fasterxml.jackson.databind.ObjectMapper().readTree(source);
      assertThat(proof.get("row").get("Code").asText()).isEqualTo("CriticalMultiplier4");
      assertThat(proof.get("row").get("Level").asInt()).isEqualTo(45);
      assertThat(proof.get("row").get("Removes").asBoolean()).isTrue();
      assertThat(proof.get("text").asText())
          .contains("base critical strike multiplier +", "CriticalStrikeMultiplier", "of Fury");
      var definition = catalog.find(target).orElseThrow();
      assertThat(definition.stats())
          .containsExactly(
              new ModifierDefinition.StatRange("base_critical_strike_multiplier_+", 25, 29));
      assertThat(catalog.modifiers()).hasSize(194);
    }
  }
}
