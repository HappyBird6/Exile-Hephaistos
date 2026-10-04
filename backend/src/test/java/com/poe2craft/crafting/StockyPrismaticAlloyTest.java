package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class StockyPrismaticAlloyTest {
  final StockyHysteriaTest f = new StockyHysteriaTest();
  final WorkbenchCurrency action = WorkbenchCurrency.PRISMATIC_ALLOY;
  final String target = "stocky-mitts:prefix:alloy-elemental-penetration";

  @Test
  void sourcedAlloyHasSingleZeroWeightPrefixAndKeepsOrdinaryPool() throws Exception {
    var d = f.catalog.find(target).orElseThrow();
    assertThat(d.affixType()).isEqualTo(ModifierDefinition.AffixType.PREFIX);
    assertThat(d.requiredItemLevel()).isEqualTo(45);
    assertThat(d.weight()).isZero();
    assertThat(d.familyIds()).containsExactly("ElementalPenetration");
    assertThat(d.stats())
        .containsExactly(
            new ModifierDefinition.StatRange("reduce_enemy_elemental_resistance_%", 9, 15));
    assertThat(action.isAlloy()).isTrue();
    assertThat(action.replacementEssenceModifiers()).isEmpty();
    try (var raw =
        getClass().getResourceAsStream("/catalog/stocky-mitts/prismatic-alloy.raw.json")) {
      var proof = new com.fasterxml.jackson.databind.ObjectMapper().readTree(raw);
      assertThat(proof.path("row").path("Code").asText()).isEqualTo("AlloyElementalPenetration1");
      assertThat(proof.path("row").path("IsAlloy").asBoolean()).isTrue();
      assertThat(proof.path("row").path("reqlvl").asInt()).isEqualTo(36);
    }
    assertThat(f.catalog.modifiers()).hasSize(194);
    assertThat(f.catalog.modifiers().values().stream().filter(m -> m.weight() > 0)).hasSize(182);
    assertThat(f.catalog.metadata().prefixWeight()).isEqualTo(63700);
    assertThat(f.catalog.metadata().suffixWeight()).isEqualTo(84500);
    assertThat(f.catalog.compatibleSnapshotIds()).hasSize(6);
  }

  @Test
  void unrelatedCrystallisationIncludingPairRemainsUnconsumedAndAllRemovalOutcomesReachable() {
    var before = f.rare(45, "", "stocky-mitts:prefix:sanguine", "stocky-mitts:suffix:of-the-brute");
    var omens =
        Set.of(
            WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id(),
            WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id(),
            WorkbenchOmen.BLESSED.id());
    var values = new HashSet<Long>();
    var removed = new HashSet<String>();
    for (int i = 0; i < 200; i++) {
      var r = f.simulator.apply(before, action, omens, new Random(i * 104729L));
      assertThat(r.applied()).isTrue();
      assertThat(r.consumedOmens()).isEmpty();
      assertThat(r.remainingOmens()).containsExactlyInAnyOrderElementsOf(omens);
      assertThat(r.events()).hasSize(2);
      assertThat(r.events().getFirst().selectionProbability()).isEqualTo(.5);
      assertThat(r.events().getLast().selectionProbability()).isEqualTo(1);
      assertThat(r.events().getLast().modifierId()).isEqualTo(target);
      values.add(r.events().getLast().values().get("reduce_enemy_elemental_resistance_%"));
      removed.add(r.events().getFirst().modifierId());
      assertThat(r.assumptions())
          .extracting(WorkbenchSimulator.Assumption::id)
          .containsExactly("uniform-removal-v1", "assumed-source-integer-roll-v1");
      assertThat(r.assumptions().getLast().n()).isEqualTo(7);
      assertThat(r.assumptions().getLast().reason()).contains("UNVERIFIED");
      assertThat(new ItemStateValidator(f.catalog).validate(r.state())).isEmpty();
    }
    assertThat(values).containsExactlyInAnyOrder(9L, 10L, 11L, 12L, 13L, 14L, 15L);
    assertThat(removed).hasSize(2);
  }

  @Test
  void lowerLevelLocksFamilyAndAnyImpossiblePrefixBranchBlockWithoutPruning() {
    for (var before :
        List.of(
            f.root,
            f.rare(44, "", "stocky-mitts:prefix:sanguine"),
            f.rare(36, "", "stocky-mitts:prefix:sanguine"),
            f.rare(82, target, target),
            f.rare(82, "", target, "stocky-mitts:suffix:of-the-brute"),
            f.rare(
                82,
                "",
                "stocky-mitts:prefix:sanguine",
                "stocky-mitts:prefix:hunter-s",
                "stocky-mitts:prefix:azure",
                "stocky-mitts:suffix:of-the-brute"))) {
      var r = f.simulator.apply(before, action, Set.of(), new Random());
      assertThat(r.applied()).isFalse();
      assertThat(r.state()).isEqualTo(before);
      assertThat(r.events()).isEmpty();
    }
    assertThat(f.simulator.apply(f.rare(45, "", target), action, Set.of(), new Random()).applied())
        .isTrue();
  }

  @Test
  void fracturedTargetSurvivesDivineChaosAndAnnul() {
    var before =
        f.rare(
            82,
            target,
            target,
            "stocky-mitts:prefix:sanguine",
            "stocky-mitts:suffix:of-the-brute",
            "stocky-mitts:suffix:of-the-penguin");
    var fixed =
        before.explicits().stream().filter(ModifierInstance::fractured).findFirst().orElseThrow();
    for (var currency :
        List.of(WorkbenchCurrency.DIVINE, WorkbenchCurrency.CHAOS, WorkbenchCurrency.ANNULMENT)) {
      var r = f.simulator.apply(before, currency, Set.of(), new Random(3));
      assertThat(r.applied()).isTrue();
      assertThat(r.state().explicits()).contains(fixed);
      if (currency == WorkbenchCurrency.CHAOS)
        assertThat(f.catalog.find(r.events().getLast().modifierId()).orElseThrow().weight())
            .isPositive();
    }
  }
}
