package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class StockyHorrorTest {
  final StockyHysteriaTest f = new StockyHysteriaTest();
  final String target = "stocky-mitts:suffix:essence-socketed-augment-effect";
  final WorkbenchCurrency action = WorkbenchCurrency.ESSENCE_HORROR;

  @Test
  void fixedPercentLocalUnscalableSourceRemainsSeparateFromEveryOrdinaryWeight() throws Exception {
    var d = f.catalog.find(target).orElseThrow();
    assertThat(d.text()).isEqualTo("60% increased effect of Socketed Augment Items");
    assertThat(d.familyIds()).containsExactly("SoulCore");
    assertThat(d.affixType()).isEqualTo(ModifierDefinition.AffixType.SUFFIX);
    assertThat(d.requiredItemLevel()).isEqualTo(1);
    assertThat(d.weight()).isZero();
    assertThat(d.stats())
        .containsExactly(
            new ModifierDefinition.StatRange("local_socketed_items_effect_+%", 60, 60));
    try (var raw =
        getClass().getResourceAsStream("/catalog/stocky-mitts/horror-essence.raw.json")) {
      var source = new com.fasterxml.jackson.databind.ObjectMapper().readTree(raw);
      assertThat(source.path("row").path("Code").asText())
          .isEqualTo("EssenceLocalRuneAndSoulCoreEffect1");
      assertThat(source.path("html").asText()).contains("Unscalable Value");
      assertThat(source.path("parsed").path("stats").get(0).path("locality").asText())
          .isEqualTo("Local");
    }
    assertThat(f.catalog.modifiers().values().stream().filter(m -> m.weight() > 0)).hasSize(182);
    assertThat(f.catalog.metadata().prefixWeight()).isEqualTo(63700);
    assertThat(f.catalog.metadata().suffixWeight()).isEqualTo(84500);
    assertThat(f.catalog.metadata().snapshotId())
        .startsWith("stocky-special-")
        .hasSizeLessThanOrEqualTo(120);
    assertThat(f.catalog.compatibleSnapshotIds()).hasSize(5);
  }

  @Test
  void everyUnlockedRemovalRemainsReachableAtLevelOneWithoutNumericOrChoiceAssumption() {
    var before = f.rare(1, "", "stocky-mitts:prefix:sanguine", "stocky-mitts:suffix:of-the-brute");
    var reached = new HashSet<String>();
    for (int i = 0; i < 80; i++) {
      var r =
          f.simulator.apply(
              before, action, Set.of(WorkbenchOmen.BLESSED.id()), new Random(i * 104729L));
      assertThat(r.applied()).isTrue();
      var removed = r.events().getFirst();
      var added = r.events().getLast();
      reached.add(removed.modifierId());
      assertThat(removed.selectionProbability()).isEqualTo(.5);
      assertThat(added.modifierId()).isEqualTo(target);
      assertThat(added.values()).containsExactly(entry("local_socketed_items_effect_+%", 60L));
      assertThat(added.selectionProbability()).isEqualTo(1);
      assertThat(r.assumptions())
          .extracting(WorkbenchSimulator.Assumption::id)
          .containsExactly("uniform-removal-v1");
      assertThat(r.consumedOmens()).isEmpty();
      assertThat(r.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
      before.explicits().stream()
          .filter(m -> !m.modifierId().equals(removed.modifierId()))
          .forEach(m -> assertThat(r.state().explicits()).contains(m));
    }
    assertThat(reached)
        .containsExactlyInAnyOrder(
            "stocky-mitts:prefix:sanguine", "stocky-mitts:suffix:of-the-brute");
  }

  @Test
  void crystallisationRestrictsRemovalAndPreservesOtherOmensAndFractures() {
    var before =
        f.rare(
            82,
            "stocky-mitts:prefix:sanguine",
            "stocky-mitts:prefix:sanguine",
            "stocky-mitts:prefix:hunter-s",
            "stocky-mitts:suffix:of-the-brute");
    for (var omen :
        List.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION, WorkbenchOmen.DEXTRAL_CRYSTALLISATION)) {
      var r =
          f.simulator.apply(
              before, action, Set.of(omen.id(), WorkbenchOmen.BLESSED.id()), new Random(1));
      assertThat(r.applied()).isTrue();
      assertThat(f.catalog.find(r.events().getFirst().modifierId()).orElseThrow().affixType())
          .isEqualTo(omen.affix());
      assertThat(r.state().explicits())
          .contains(
              before.explicits().stream()
                  .filter(ModifierInstance::fractured)
                  .findFirst()
                  .orElseThrow());
      assertThat(r.consumedOmens()).containsExactly(omen.id());
      assertThat(r.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
    }
    var r =
        f.simulator.apply(
            before,
            action,
            Set.of(
                WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id(),
                WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()),
            new Random());
    assertThat(r.applied()).isFalse();
    assertThat(r.state()).isEqualTo(before);
    assertThat(r.consumedOmens()).isEmpty();
  }

  @Test
  void
      survivingFamilyFullSuffixAndLockedTargetBlockWithoutPruningButSoleUnlockedTargetCanReplace() {
    var full =
        f.rare(
            82,
            "",
            "stocky-mitts:prefix:sanguine",
            "stocky-mitts:suffix:of-the-brute",
            "stocky-mitts:suffix:of-the-penguin",
            "stocky-mitts:suffix:of-the-salamander");
    for (var before :
        List.of(
            full,
            f.rare(82, "", target, "stocky-mitts:prefix:sanguine"),
            f.rare(82, target, target),
            f.root)) {
      var r = f.simulator.apply(before, action, Set.of(), new Random());
      assertThat(r.applied()).isFalse();
      assertThat(r.state()).isEqualTo(before);
      assertThat(r.events()).isEmpty();
    }
    var r = f.simulator.apply(f.rare(1, "", target), action, Set.of(), new Random());
    assertThat(r.applied()).isTrue();
    assertThat(r.events().getFirst().modifierId()).isEqualTo(target);
    assertThat(r.events().getLast().modifierId()).isEqualTo(target);
  }

  @Test
  void fracturedHorrorCannotBeRemovedAndDivineKeepsSixtyWithoutRollingIt() {
    var before =
        f.rare(
            82,
            "",
            target,
            "stocky-mitts:prefix:sanguine",
            "stocky-mitts:prefix:hunter-s",
            "stocky-mitts:suffix:of-the-brute");
    ItemState locked = null;
    for (int i = 0; i < 80; i++) {
      var candidate =
          f.simulator
              .apply(before, WorkbenchCurrency.FRACTURING, Set.of(), new Random(i * 104729L))
              .state();
      if (candidate.explicits().stream()
          .anyMatch(m -> m.modifierId().equals(target) && m.fractured())) {
        locked = candidate;
        break;
      }
    }
    assertThat(locked).isNotNull();
    var fixed =
        locked.explicits().stream().filter(ModifierInstance::fractured).findFirst().orElseThrow();
    for (var currency :
        List.of(WorkbenchCurrency.DIVINE, WorkbenchCurrency.CHAOS, WorkbenchCurrency.ANNULMENT)) {
      var r = f.simulator.apply(locked, currency, Set.of(), new Random(3));
      assertThat(r.state().explicits()).contains(fixed);
      assertThat(r.events().stream().filter(e -> e.kind().equals("REMOVE")))
          .noneMatch(e -> e.modifierId().equals(target));
    }
  }

  @Test
  void additiveCatalogRetainsEveryOldDefinitionAndBothPriorIdentitiesAndRejectsTamperedProof()
      throws Exception {
    try (var data = getClass().getResourceAsStream("/catalog/stocky-mitts/catalog.json");
        var raw = getClass().getResourceAsStream("/catalog/stocky-mitts/base.raw.json");
        var details = getClass().getResourceAsStream("/catalog/stocky-mitts/details.raw.json");
        var abyss =
            getClass().getResourceAsStream("/catalog/stocky-mitts/abyss-essence.catalog.json");
        var abyssRaw =
            getClass().getResourceAsStream("/catalog/stocky-mitts/abyss-essence.raw.json")) {
      var ordinary = ItemCatalogLoader.load(data, raw, details);
      var prior = ItemCatalogLoader.addSpecial(ordinary, abyss, abyssRaw);
      prior
          .modifiers()
          .forEach((id, definition) -> assertThat(f.catalog.find(id)).contains(definition));
      assertThat(f.catalog.base()).isEqualTo(prior.base());
      assertThat(f.catalog.compatibleSnapshotIds())
          .contains(ordinary.metadata().snapshotId(), prior.metadata().snapshotId());
      try (var horror =
          getClass().getResourceAsStream("/catalog/stocky-mitts/horror-essence.catalog.json")) {
        assertThatThrownBy(
                () ->
                    ItemCatalogLoader.addSpecial(
                        prior, horror, new java.io.ByteArrayInputStream(new byte[0])))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("checksum");
      }
    }
  }

  @Test
  void solarDoesNotBorrowGloveTarget() {
    var solar = ItemCatalogLoader.loadDefault();
    var simulator = new WorkbenchSimulator(solar, new CraftingEngine(solar));
    var preset =
        new com.poe2craft.crafting.application.WorkbenchService(solar, simulator, f.catalog)
            .initial("solar", 82)
            .state();
    var root =
        new ItemState(
            preset.snapshotId(),
            preset.baseItemId(),
            82,
            ItemState.Rarity.NORMAL,
            preset.implicits(),
            List.of(),
            Set.of());
    assertThat(simulator.apply(root, action, Set.of(), new Random()).applied()).isFalse();
    assertThat(simulator.actions(root, Set.of())).noneMatch(a -> a.action() == action);
  }
}
