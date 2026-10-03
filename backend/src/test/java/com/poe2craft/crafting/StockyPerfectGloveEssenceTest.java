package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class StockyPerfectGloveEssenceTest {
  final StockyHysteriaTest f = new StockyHysteriaTest();
  final List<WorkbenchCurrency> actions =
      List.of(
          WorkbenchCurrency.PERFECT_ESSENCE_GROUNDING, WorkbenchCurrency.PERFECT_ESSENCE_OPULENCE);

  String target(WorkbenchCurrency action) {
    return action.replacementModifiers().getFirst();
  }

  @Test
  void exactPrimaryTargetsRetainRawLevelVersusEffectiveRequirementAndNoOrdinaryWeight()
      throws Exception {
    try (var raw =
        getClass()
            .getResourceAsStream("/catalog/stocky-mitts/perfect-grounding-opulence.raw.json")) {
      var proof = new com.fasterxml.jackson.databind.ObjectMapper().readTree(raw);
      assertThat(proof.size()).isEqualTo(2);
      for (int i = 0; i < 2; i++) {
        var d = f.catalog.find(target(actions.get(i))).orElseThrow();
        assertThat(d.affixType()).isEqualTo(ModifierDefinition.AffixType.SUFFIX);
        assertThat(d.requiredItemLevel()).isEqualTo(72);
        assertThat(d.weight()).isZero();
        assertThat(d.familyIds())
            .containsExactly(i == 0 ? "LightningDamageTakenRecoupedAsLife" : "EssenceGoldDropped");
        assertThat(d.stats())
            .containsExactly(
                new ModifierDefinition.StatRange(
                    i == 0
                        ? "lightning_damage_taken_goes_to_life_over_4_seconds_%"
                        : "gold_+%_from_enemies",
                    i == 0 ? 26 : 10,
                    i == 0 ? 30 : 15));
        assertThat(proof.get(i).path("row").path("Code").asText())
            .isEqualTo(i == 0 ? "EssenceLightningRecoupLife1" : "EssenceGoldDropped1");
        assertThat(proof.get(i).path("row").path("Level").asInt()).isEqualTo(72);
        assertThat(proof.get(i).path("row").path("reqlvl").asInt()).isEqualTo(57);
        assertThat(proof.get(i).path("row").path("IsPerfect").asText()).isEqualTo("1");
        assertThat(proof.get(i).path("row").path("Removes").asBoolean()).isTrue();
        assertThat(proof.get(i).path("parsed").path("stats").get(0).path("locality").asText())
            .isEqualTo("Global");
      }
    }
    assertThat(f.catalog.modifiers()).hasSize(187);
    assertThat(f.catalog.modifiers().values().stream().filter(d -> d.weight() > 0)).hasSize(182);
    assertThat(f.catalog.metadata().prefixWeight()).isEqualTo(63700);
    assertThat(f.catalog.metadata().suffixWeight()).isEqualTo(84500);
  }

  @Test
  void everyRemovalAndModelledIntegerIsReachableWithSeparateUnverifiedNumericLedger() {
    var before = f.rare(72, "", "stocky-mitts:prefix:sanguine", "stocky-mitts:suffix:of-the-brute");
    for (var action : actions) {
      var d = f.catalog.find(target(action)).orElseThrow();
      var range = d.stats().getFirst();
      var seenValues = new HashSet<Long>();
      var removed = new HashSet<String>();
      for (int i = 0; i < 200; i++) {
        var r =
            f.simulator.apply(
                before, action, Set.of(WorkbenchOmen.BLESSED.id()), new Random(i * 104729L));
        assertThat(r.applied()).isTrue();
        assertThat(r.events()).hasSize(2);
        assertThat(r.events().getFirst().selectionProbability()).isEqualTo(.5);
        assertThat(r.events().getLast().modifierId()).isEqualTo(target(action));
        assertThat(r.events().getLast().selectionProbability()).isEqualTo(1);
        var value = r.events().getLast().values().get(range.id());
        assertThat(value).isBetween(range.min(), range.max());
        seenValues.add(value);
        removed.add(r.events().getFirst().modifierId());
        assertThat(r.assumptions())
            .extracting(WorkbenchSimulator.Assumption::id)
            .containsExactly("uniform-removal-v1", "assumed-source-integer-roll-v1");
        var numeric = r.assumptions().getLast();
        assertThat(numeric.n()).isEqualTo(range.max() - range.min() + 1);
        assertThat(numeric.min()).isEqualTo(range.min());
        assertThat(numeric.max()).isEqualTo(range.max());
        assertThat(numeric.reason())
            .contains("UNVERIFIED", "not an established game outcome domain");
        assertThat(r.consumedOmens()).isEmpty();
        assertThat(r.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
        assertThat(new ItemStateValidator(f.catalog).validate(r.state())).isEmpty();
        before.explicits().stream()
            .filter(m -> !m.modifierId().equals(r.events().getFirst().modifierId()))
            .forEach(m -> assertThat(r.state().explicits()).contains(m));
      }
      assertThat(removed).hasSize(2);
      assertThat(seenValues).hasSize((int) (range.max() - range.min() + 1));
    }
  }

  @Test
  void unverifiedLowerLevelsFullSuffixSurvivingFamilyAndLocksBlockWithoutPruning() {
    for (var action : actions) {
      var target = target(action);
      for (var before :
          List.of(
              f.root,
              f.rare(71, "", "stocky-mitts:prefix:sanguine"),
              f.rare(57, "", "stocky-mitts:prefix:sanguine"),
              f.rare(82, target, target),
              f.rare(82, "", target, "stocky-mitts:prefix:sanguine"),
              f.rare(
                  82,
                  "",
                  "stocky-mitts:prefix:sanguine",
                  "stocky-mitts:suffix:of-the-brute",
                  "stocky-mitts:suffix:of-the-penguin",
                  "stocky-mitts:suffix:of-the-salamander"))) {
        var r = f.simulator.apply(before, action, Set.of(), new Random());
        assertThat(r.applied()).isFalse();
        assertThat(r.state()).isEqualTo(before);
        assertThat(r.events()).isEmpty();
        assertThat(r.assumptions()).isEmpty();
      }
      assertThat(
              f.simulator.apply(f.rare(72, "", target), action, Set.of(), new Random()).applied())
          .isTrue();
    }
  }

  @Test
  void crystallisationConsumesOnlyItsMatchingRemovalSideAndPairConflictKeepsState() {
    var before =
        f.rare(
            82,
            "stocky-mitts:prefix:sanguine",
            "stocky-mitts:prefix:sanguine",
            "stocky-mitts:prefix:hunter-s",
            "stocky-mitts:suffix:of-the-brute");
    for (var action : actions) {
      for (var omen :
          List.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION, WorkbenchOmen.DEXTRAL_CRYSTALLISATION)) {
        var r =
            f.simulator.apply(
                before, action, Set.of(omen.id(), WorkbenchOmen.BLESSED.id()), new Random());
        assertThat(r.applied()).isTrue();
        assertThat(f.catalog.find(r.events().getFirst().modifierId()).orElseThrow().affixType())
            .isEqualTo(omen.affix());
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
  }

  @Test
  void bothEffectsCanCoexistAndEachFractureSurvivesDivineChaosAnnul() {
    for (var action : actions) {
      var target = target(action);
      var other = target(actions.get(action == actions.getFirst() ? 1 : 0));
      var before =
          f.rare(
              82,
              "",
              target,
              other,
              "stocky-mitts:prefix:sanguine",
              "stocky-mitts:prefix:hunter-s");
      assertThat(new ItemStateValidator(f.catalog).validate(before)).isEmpty();
      ItemState locked = null;
      for (int i = 0; i < 80; i++) {
        var state =
            f.simulator
                .apply(before, WorkbenchCurrency.FRACTURING, Set.of(), new Random(i * 104729L))
                .state();
        if (state.explicits().stream()
            .anyMatch(m -> m.modifierId().equals(target) && m.fractured())) {
          locked = state;
          break;
        }
      }
      assertThat(locked).isNotNull();
      var fixed =
          locked.explicits().stream().filter(ModifierInstance::fractured).findFirst().orElseThrow();
      for (var currency :
          List.of(WorkbenchCurrency.DIVINE, WorkbenchCurrency.CHAOS, WorkbenchCurrency.ANNULMENT)) {
        var r = f.simulator.apply(locked, currency, Set.of(), new Random(3));
        assertThat(r.applied()).isTrue();
        assertThat(r.state().explicits()).contains(fixed);
        if (currency == WorkbenchCurrency.CHAOS)
          assertThat(f.catalog.find(r.events().getLast().modifierId()).orElseThrow().weight())
              .isPositive();
      }
    }
  }

  @Test
  void newCatalogPreservesExactHorrorProofBytesDefinitionAndPriorIdentity() throws Exception {
    try (var data = getClass().getResourceAsStream("/catalog/stocky-mitts/catalog.json");
        var raw = getClass().getResourceAsStream("/catalog/stocky-mitts/base.raw.json");
        var details = getClass().getResourceAsStream("/catalog/stocky-mitts/details.raw.json");
        var abyss =
            getClass().getResourceAsStream("/catalog/stocky-mitts/abyss-essence.catalog.json");
        var abyssRaw =
            getClass().getResourceAsStream("/catalog/stocky-mitts/abyss-essence.raw.json");
        var horror =
            getClass().getResourceAsStream("/catalog/stocky-mitts/horror-essence.catalog.json");
        var horrorRaw =
            getClass().getResourceAsStream("/catalog/stocky-mitts/horror-essence.raw.json")) {
      var bytes = horrorRaw.readAllBytes();
      assertThat(
              java.util.HexFormat.of()
                  .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)))
          .isEqualTo("871c235ca9af82a1b4caccdfdcfe27ad28ba57c2946f35417dc270eb68e1d50f");
      var prior =
          ItemCatalogLoader.addSpecial(
              ItemCatalogLoader.loadWithSpecial(data, raw, details, abyss, abyssRaw),
              horror,
              new java.io.ByteArrayInputStream(bytes));
      assertThat(prior.modifiers()).hasSize(185);
      assertThat(prior.metadata().snapshotId())
          .isEqualTo(
              "stocky-special-4b2a7d6b0d70eecbbcf6ad4c01a5206dfde6c68fd065db5e0de72bdbce03de07");
      prior.modifiers().forEach((id, d) -> assertThat(f.catalog.find(id)).contains(d));
      var identities = new ArrayList<>(prior.compatibleSnapshotIds());
      identities.add(prior.metadata().snapshotId());
      assertThat(f.catalog.compatibleSnapshotIds()).containsExactlyElementsOf(identities);
      assertThat(f.catalog.metadata().snapshotId())
          .startsWith("stocky-special-")
          .hasSizeLessThanOrEqualTo(120);
      try (var extension =
          getClass()
              .getResourceAsStream(
                  "/catalog/stocky-mitts/perfect-grounding-opulence.catalog.json")) {
        assertThatThrownBy(
                () ->
                    ItemCatalogLoader.addSpecial(
                        prior, extension, new java.io.ByteArrayInputStream(new byte[0])))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("checksum");
      }
    }
  }

  @Test
  void solarNeitherListsNorAppliesTheseGloveTargets() {
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
    for (var action : actions) {
      assertThat(simulator.apply(root, action, Set.of(), new Random()).applied()).isFalse();
      assertThat(simulator.actions(root, Set.of())).noneMatch(a -> a.action() == action);
    }
  }
}
