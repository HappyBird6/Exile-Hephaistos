package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class StockyReviewedAlloyTest {
  final StockyHysteriaTest f = new StockyHysteriaTest();

  record Spec(
      WorkbenchCurrency action,
      String target,
      String stat,
      int min,
      int max,
      int level,
      String code,
      int index,
      ModifierDefinition.AffixType affix) {}

  static Stream<Spec> materials() {
    return Stream.of(
        new Spec(
            WorkbenchCurrency.ADAPTIVE_ALLOY,
            "stocky-mitts:suffix:alloy-attack-speed-missing-ward",
            "attack_speed_+%_while_missing_ward",
            10,
            15,
            25,
            "AlloyAttackSpeedIfMissingWardRecently1",
            0,
            ModifierDefinition.AffixType.SUFFIX),
        new Spec(
            WorkbenchCurrency.SWIFT_ALLOY,
            "stocky-mitts:suffix:alloy-cast-speed",
            "base_cast_speed_+%",
            9,
            12,
            45,
            "AlloyCastSpeedGloves1",
            1,
            ModifierDefinition.AffixType.SUFFIX),
        new Spec(
            WorkbenchCurrency.SOVEREIGN_ALLOY,
            "stocky-mitts:prefix:alloy-local-runic-ward",
            "local_ward_+%",
            24,
            30,
            25,
            "AlloyLocalWardIncreasePercent1",
            2,
            ModifierDefinition.AffixType.PREFIX));
  }

  @ParameterizedTest
  @MethodSource("materials")
  void definitionMatchesExactPrimaryRowAndPreservesPool(Spec s) throws Exception {
    var d = f.catalog.find(s.target()).orElseThrow();
    assertThat(d.affixType()).isEqualTo(s.affix());
    assertThat(d.requiredItemLevel()).isEqualTo(s.level());
    assertThat(d.weight()).isZero();
    assertThat(d.stats())
        .containsExactly(new ModifierDefinition.StatRange(s.stat(), s.min(), s.max()));
    try (var raw =
        getClass().getResourceAsStream("/catalog/stocky-mitts/reviewed-alloys.raw.json")) {
      var proof = new com.fasterxml.jackson.databind.ObjectMapper().readTree(raw).get(s.index());
      assertThat(proof.path("row").path("Code").asText()).isEqualTo(s.code());
      assertThat(proof.path("row").path("IsAlloy").asBoolean()).isTrue();
      assertThat(proof.path("row").path("Removes").asBoolean()).isTrue();
      assertThat(proof.path("row").path("Level").asInt()).isEqualTo(s.level());
      var families = new HashSet<String>();
      proof.path("row").path("ModFamilyList").forEach(v -> families.add(v.asText()));
      assertThat(d.familyIds()).containsExactlyInAnyOrderElementsOf(families);
      assertThat(proof.path("parsed").path("stats").get(0).path("locality").asText())
          .isEqualTo(s.action() == WorkbenchCurrency.SOVEREIGN_ALLOY ? "Local" : "Global");
    }
    assertThat(f.catalog.modifiers()).hasSize(194);
    assertThat(f.catalog.modifiers().values().stream().filter(m -> m.weight() > 0)).hasSize(182);
    assertThat(f.catalog.metadata().prefixWeight()).isEqualTo(63700);
    assertThat(f.catalog.metadata().suffixWeight()).isEqualTo(84500);
    assertThat(f.catalog.compatibleSnapshotIds()).hasSize(6);
  }

  @ParameterizedTest
  @MethodSource("materials")
  void completeRemovalNumericModelAndUnrelatedOmensPreserveStateConditions(Spec s) {
    var before =
        f.rare(s.level(), "", "stocky-mitts:prefix:sanguine", "stocky-mitts:suffix:of-the-brute");
    var omens =
        Set.of(
            WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id(),
            WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id(),
            WorkbenchOmen.BLESSED.id());
    var values = new HashSet<Long>();
    var removed = new HashSet<String>();
    for (int i = 0; i < 200; i++) {
      var r = f.simulator.apply(before, s.action(), omens, new Random(i * 104729L));
      assertThat(r.applied()).isTrue();
      assertThat(r.consumedOmens()).isEmpty();
      assertThat(r.remainingOmens()).containsExactlyInAnyOrderElementsOf(omens);
      assertThat(r.state().conditions()).isEqualTo(before.conditions());
      assertThat(r.events().getFirst().selectionProbability()).isEqualTo(.5);
      assertThat(r.events().getLast().modifierId()).isEqualTo(s.target());
      assertThat(r.events().getLast().selectionProbability()).isEqualTo(1);
      values.add(r.events().getLast().values().get(s.stat()));
      removed.add(r.events().getFirst().modifierId());
      assertThat(r.assumptions())
          .extracting(WorkbenchSimulator.Assumption::id)
          .containsExactly("uniform-removal-v1", "assumed-source-integer-roll-v1");
      assertThat(r.assumptions().getLast().n()).isEqualTo(s.max() - s.min() + 1);
      assertThat(r.assumptions().getLast().reason()).contains("UNVERIFIED");
      assertThat(new ItemStateValidator(f.catalog).validate(r.state())).isEmpty();
    }
    assertThat(values)
        .containsExactlyInAnyOrderElementsOf(
            java.util.stream.LongStream.rangeClosed(s.min(), s.max()).boxed().toList());
    assertThat(removed).hasSize(2);
  }

  @ParameterizedTest
  @MethodSource("materials")
  void lowLevelsCapacityFamilyLocksAndSolarRefuseSafely(Spec s) {
    var full =
        s.affix() == ModifierDefinition.AffixType.PREFIX
            ? f.rare(
                82,
                "",
                "stocky-mitts:prefix:sanguine",
                "stocky-mitts:prefix:hunter-s",
                "stocky-mitts:prefix:azure",
                "stocky-mitts:suffix:of-the-brute")
            : f.rare(
                82,
                "",
                "stocky-mitts:prefix:sanguine",
                "stocky-mitts:suffix:of-the-brute",
                "stocky-mitts:suffix:of-the-penguin",
                "stocky-mitts:suffix:of-the-salamander");
    for (var before :
        List.of(
            f.root,
            f.rare(s.level() - 1, "", "stocky-mitts:prefix:sanguine"),
            f.rare(82, s.target(), s.target()),
            f.rare(82, "", s.target(), "stocky-mitts:prefix:sanguine"),
            full)) {
      var r = f.simulator.apply(before, s.action(), Set.of(), new Random());
      assertThat(r.applied()).isFalse();
      assertThat(r.state()).isEqualTo(before);
      assertThat(r.events()).isEmpty();
      assertThat(r.consumedOmens()).isEmpty();
    }
    assertThat(
            f.simulator
                .apply(f.rare(s.level(), "", s.target()), s.action(), Set.of(), new Random())
                .applied())
        .isTrue();
    var solar = com.poe2craft.item.infrastructure.ItemCatalogLoader.loadDefault();
    assertThat(
            new WorkbenchSimulator(solar, new CraftingEngine(solar))
                .actions(
                    new ItemState(
                        solar.metadata().snapshotId(),
                        SolarAmulet.BASE_ID,
                        82,
                        ItemState.Rarity.NORMAL,
                        SolarAmulet.initial(solar, 82, 15).implicits(),
                        List.of(),
                        Set.of()),
                    Set.of()))
        .noneMatch(a -> a.action() == s.action());
  }

  @ParameterizedTest
  @MethodSource("materials")
  void fracturedSourceRollSurvivesDivineChaosAnnul(Spec s) {
    var before =
        f.rare(
            82,
            s.target(),
            s.target(),
            "stocky-mitts:prefix:sanguine",
            "stocky-mitts:suffix:of-the-brute",
            "stocky-mitts:suffix:of-the-penguin");
    var fixed =
        before.explicits().stream().filter(ModifierInstance::fractured).findFirst().orElseThrow();
    for (var action :
        List.of(WorkbenchCurrency.DIVINE, WorkbenchCurrency.CHAOS, WorkbenchCurrency.ANNULMENT)) {
      var r = f.simulator.apply(before, action, Set.of(), new Random(3));
      assertThat(r.applied()).isTrue();
      assertThat(r.state().explicits()).contains(fixed);
      if (action == WorkbenchCurrency.CHAOS)
        assertThat(f.catalog.find(r.events().getLast().modifierId()).orElseThrow().weight())
            .isPositive();
    }
  }

  @Test
  void swiftRejectsSurvivingEitherFamilyAndAllowsSoleConflictRemoval() {
    var swift = f.catalog.find("stocky-mitts:suffix:alloy-cast-speed").orElseThrow();
    assertThat(swift.familyIds())
        .containsExactlyInAnyOrder("IncreasedAttackSpeed", "IncreasedCastSpeed");
    assertThat(f.simulator.pool(f.rare(82, "", swift.id()), WorkbenchCurrency.EXALTED, null))
        .noneMatch(
            d ->
                d.familyIds().contains("IncreasedAttackSpeed")
                    || d.familyIds().contains("IncreasedCastSpeed"));
    for (var family : swift.familyIds()) {
      var source = f.catalog.find("stocky-mitts:suffix:of-skill").orElseThrow();
      var fixture =
          new ModifierDefinition(
              source.id(),
              source.name(),
              source.layer(),
              source.affixType(),
              Set.of(family),
              source.requiredItemLevel(),
              source.weight(),
              source.tier(),
              source.text(),
              source.stats(),
              source.tags(),
              source.sourceUrl());
      var definitions = new ArrayList<>(f.catalog.modifiers().values());
      definitions.replaceAll(d -> d.id().equals(fixture.id()) ? fixture : d);
      // Isolates each real target family without claiming an additional game modifier.
      var catalog =
          new ItemCatalog(
              f.catalog.metadata(),
              f.catalog.base(),
              definitions,
              f.catalog.compatibleSnapshotIds());
      var simulator =
          new WorkbenchSimulator(
              catalog,
              new CraftingEngine(catalog),
              catalog.modifiers().values().stream()
                  .filter(d -> d.stats().size() > 1)
                  .map(ModifierDefinition::id)
                  .collect(java.util.stream.Collectors.toSet()));
      var before = f.rare(82, "", "stocky-mitts:suffix:of-skill", "stocky-mitts:prefix:sanguine");
      var r = simulator.apply(before, WorkbenchCurrency.SWIFT_ALLOY, Set.of(), new Random());
      assertThat(r.applied()).isFalse();
      assertThat(r.state()).isEqualTo(before);
      assertThat(
              simulator
                  .apply(
                      f.rare(82, "", "stocky-mitts:suffix:of-skill"),
                      WorkbenchCurrency.SWIFT_ALLOY,
                      Set.of(),
                      new Random())
                  .applied())
          .isTrue();
    }
    assertThat(
            f.simulator
                .apply(
                    f.rare(
                        82,
                        "",
                        "stocky-mitts:suffix:alloy-attack-speed-missing-ward",
                        "stocky-mitts:prefix:sanguine"),
                    WorkbenchCurrency.SWIFT_ALLOY,
                    Set.of(),
                    new Random())
                .applied())
        .isFalse();
  }

  @Test
  void adaptiveKeepsConditionalTextWithoutInventingMissingWardState() {
    var d = f.catalog.find("stocky-mitts:suffix:alloy-attack-speed-missing-ward").orElseThrow();
    assertThat(d.text()).contains("while missing Runic Ward");
    assertThat(f.rare(25, "", "stocky-mitts:prefix:sanguine").conditions()).isEmpty();
    assertThat(
            f.simulator
                .apply(
                    f.rare(25, "", "stocky-mitts:prefix:sanguine"),
                    WorkbenchCurrency.ADAPTIVE_ALLOY,
                    Set.of(),
                    new Random())
                .applied())
        .isTrue();
  }

  @Test
  void localWardTargetsOnlyLocalSourceAndDoesNotInventAnImplicit() {
    var d = f.catalog.find("stocky-mitts:prefix:alloy-local-runic-ward").orElseThrow();
    assertThat(d.name()).isEqualTo("Verisium");
    assertThat(d.familyIds()).containsExactly("LocalRunicWardPercent");
    assertThat(
            f.simulator
                .apply(
                    f.rare(25, "", "stocky-mitts:suffix:of-the-brute"),
                    WorkbenchCurrency.SOVEREIGN_ALLOY,
                    Set.of(),
                    new Random())
                .state()
                .implicits())
        .isEmpty();
  }
}
