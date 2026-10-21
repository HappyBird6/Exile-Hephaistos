package com.poe2craft.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.Exit;
import com.poe2craft.crafting.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomaticMethodExecutionTest {
  @Test
  void ineligiblePreparedSideStopsWithoutChargingChaos() {
    var tiny = new BasicCurrencyTransitionsTest();
    var kernel =
        new AutomaticCraftingKernel(
            com.poe2craft.crafting.HistoricalAutomaticPolicy.beforeDeferrals(),
            tiny.catalog,
            new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog)),
            "amulets");
    var prep =
        new AutomaticCraftingAction(
            WorkbenchCurrency.EXALTED,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL);
    var chaos =
        new AutomaticCraftingAction(
            WorkbenchCurrency.CHAOS,
            Set.of(WorkbenchOmen.SINISTRAL_ERASURE),
            AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
    var empty = tiny.item(ItemState.Rarity.RARE, List.of());
    try (var run =
        new AutomaticMethodExecution(
            empty,
            new AutomaticMethodExecution.Template("ineligible-side", List.of(prep), List.of(chaos)),
            kernel,
            item ->
                item.explicits().stream().anyMatch(m -> m.modifierId().equals("a"))
                    ? Optional.of(Exit.GOAL)
                    : Optional.empty(),
            CertifiedInterval.Precision.standard(),
            100,
            AutomaticMethodExecution.PreparationSuccess.STOP_ON_FIRST_HIT)) {
      while (!run.atBoundary()) run.step(100);
      if (!run.absorbed()) {
        run.beginCycle();
        while (!run.atBoundary()) run.step(100);
      }
      var point = run.observation();
      assertThat(run.absorbed()).isTrue();
      var hit =
          point.exits().get(Exit.GOAL).values().stream()
              .reduce(CertifiedInterval.zero(64), CertifiedInterval::add);
      var dead =
          point.exits().get(Exit.DEAD).values().stream()
              .reduce(CertifiedInterval.zero(64), CertifiedInterval::add);
      assertThat(hit.contains(Fraction.of(1, 4))).isTrue();
      assertThat(dead.contains(Fraction.of(3, 4))).isTrue();
      assertThat(point.cycleUses()).isEmpty();
      assertThat(
              AutomaticMethodExecution.materialUses(point.initialUses())
                  .get("EXALTED")
                  .contains(Fraction.ONE))
          .isTrue();
      assertThat(point.mass().contains(Fraction.ONE)).isTrue();
    }
  }

  @Test
  void emptyRarePreparationThenChaosHasExactRenewalCostsAndRestores() {
    var tiny = new BasicCurrencyTransitionsTest();
    var kernel =
        new AutomaticCraftingKernel(
            AutomaticCurrencyPolicyLoader.load(),
            tiny.catalog,
            new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog)),
            "amulets");
    var prep =
        new AutomaticCraftingAction(
            WorkbenchCurrency.EXALTED,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL);
    var chaos =
        new AutomaticCraftingAction(
            WorkbenchCurrency.CHAOS, Set.of(), AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
    var empty = tiny.item(ItemState.Rarity.RARE, List.of());
    assertThat(kernel.plan(empty, chaos).status())
        .isEqualTo(AutomaticCraftingPlan.Status.UNAVAILABLE);
    java.util.function.Function<ItemState, Optional<Exit>> classify =
        item ->
            item.explicits().stream().anyMatch(m -> m.modifierId().equals("a"))
                ? Optional.of(Exit.GOAL)
                : Optional.empty();
    try (var run =
        new AutomaticMethodExecution(
            empty,
            new AutomaticMethodExecution.Template("prepared-chaos", List.of(prep), List.of(chaos)),
            kernel,
            classify,
            CertifiedInterval.Precision.standard(),
            100,
            AutomaticMethodExecution.PreparationSuccess.STOP_ON_FIRST_HIT)) {
      while (!run.atBoundary()) run.step(100);
      var miss = Fraction.of(3, 4);
      var uses = Fraction.ZERO;
      for (int n = 0; n <= 3; n++) {
        var point = run.observation();
        var hit =
            point.exits().get(Exit.GOAL).values().stream()
                .reduce(CertifiedInterval.zero(64), CertifiedInterval::add);
        assertThat(hit.contains(miss.complement())).as("first Exalted plus %s Chaos", n).isTrue();
        assertThat(
                AutomaticMethodExecution.materialUses(point.initialUses())
                    .get("EXALTED")
                    .contains(Fraction.ONE))
            .isTrue();
        if (n > 0)
          assertThat(
                  AutomaticMethodExecution.materialUses(point.cycleUses())
                      .get("CHAOS")
                      .contains(uses))
              .isTrue();
        assertThat(point.mass().contains(Fraction.ONE)).isTrue();
        assertThat(point.exits().get(Exit.LOSS)).isEmpty();
        try (var restored =
            AutomaticMethodExecution.restore(run.checkpoint("a"), kernel, "a", classify)) {
          assertThat(restored.observation()).isEqualTo(point);
        }
        uses = uses.add(miss);
        miss = miss.multiply(Fraction.of(3, 4));
        run.beginCycle();
        while (!run.atBoundary()) run.step(100);
      }
    }
  }

  @Test
  void emptyRarePreparationThenDextralChaosHasExactRenewalCostsAndRestores() {
    var tiny = new BasicCurrencyTransitionsTest();
    var kernel =
        new AutomaticCraftingKernel(
            com.poe2craft.crafting.HistoricalAutomaticPolicy.beforeDeferrals(),
            tiny.catalog,
            new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog)),
            "amulets");
    var prep =
        new AutomaticCraftingAction(
            WorkbenchCurrency.EXALTED,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL);
    var chaos =
        new AutomaticCraftingAction(
            WorkbenchCurrency.CHAOS,
            Set.of(WorkbenchOmen.DEXTRAL_ERASURE),
            AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
    var empty = tiny.item(ItemState.Rarity.RARE, List.of());
    assertThat(kernel.plan(empty, chaos).status())
        .isEqualTo(AutomaticCraftingPlan.Status.UNAVAILABLE);
    java.util.function.Function<ItemState, Optional<Exit>> classify =
        item ->
            item.explicits().stream().anyMatch(m -> m.modifierId().equals("a"))
                ? Optional.of(Exit.GOAL)
                : Optional.empty();
    try (var run =
        new AutomaticMethodExecution(
            empty,
            new AutomaticMethodExecution.Template("prepared-chaos", List.of(prep), List.of(chaos)),
            kernel,
            classify,
            CertifiedInterval.Precision.standard(),
            100,
            AutomaticMethodExecution.PreparationSuccess.STOP_ON_FIRST_HIT)) {
      while (!run.atBoundary()) run.step(100);
      var miss = Fraction.of(3, 4);
      var uses = Fraction.ZERO;
      for (int n = 0; n <= 3; n++) {
        var point = run.observation();
        var hit =
            point.exits().get(Exit.GOAL).values().stream()
                .reduce(CertifiedInterval.zero(64), CertifiedInterval::add);
        assertThat(hit.contains(miss.complement())).as("first Exalted plus %s Chaos", n).isTrue();
        assertThat(
                AutomaticMethodExecution.materialUses(point.initialUses())
                    .get("EXALTED")
                    .contains(Fraction.ONE))
            .isTrue();
        if (n > 0)
          assertThat(
                  AutomaticMethodExecution.materialUses(point.cycleUses())
                      .get("CHAOS")
                      .contains(uses))
              .isTrue();
        assertThat(point.mass().contains(Fraction.ONE)).isTrue();
        assertThat(point.exits().get(Exit.LOSS)).isEmpty();
        try (var restored =
            AutomaticMethodExecution.restore(run.checkpoint("a"), kernel, "a", classify)) {
          assertThat(restored.observation()).isEqualTo(point);
        }
        uses = uses.add(miss);
        miss = miss.multiply(Fraction.of(3, 4));
        run.beginCycle();
        while (!run.atBoundary()) run.step(100);
      }
    }
  }

  @Test
  void pendingPreparationDecisionRetainsSuccessMassAsUnresolved() {
    var tiny = new BasicCurrencyTransitionsTest();
    var kernel =
        new AutomaticCraftingKernel(
            AutomaticCurrencyPolicyLoader.load(),
            tiny.catalog,
            new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog)),
            "amulets");
    var addition =
        new AutomaticCraftingAction(
            WorkbenchCurrency.EXALTED,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL);
    var replacement =
        new AutomaticCraftingAction(
            WorkbenchCurrency.PERFECT_ESSENCE_MIND,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.ORDINARY);
    try (var execution =
        new AutomaticMethodExecution(
            tiny.item(ItemState.Rarity.RARE, List.of()),
            new AutomaticMethodExecution.Template(
                "pending", List.of(addition), List.of(replacement)),
            kernel,
            item -> item.explicits().isEmpty() ? Optional.empty() : Optional.of(Exit.GOAL),
            CertifiedInterval.Precision.standard(),
            1)) {
      execution.step(1);
      assertThat(execution.resourceLimited()).isTrue();
      assertThat(execution.checkpoint("pending-goal").rows())
          .noneMatch(row -> row.kind() == CertifiedMethodRunner.CursorKind.INITIAL_USE);
      execution.increasePrimitiveBudget(100);
      while (!execution.atBoundary()) execution.step(1);
      var point = execution.observation();
      assertThat(point.exits().get(Exit.GOAL)).isEmpty();
      assertThat(
              point.exits().get(Exit.UNRESOLVED).values().stream()
                  .reduce(CertifiedInterval.zero(64), CertifiedInterval::add)
                  .contains(Fraction.ONE))
          .isTrue();
      assertThat(execution.unresolvedReasons()).contains("PREPARATION_FIRST_HIT_POLICY_UNDECIDED");
      assertThat(point.cycleUses()).isEmpty();
      assertThat(
              AutomaticMethodExecution.materialUses(point.initialUses())
                  .get("EXALTED")
                  .contains(Fraction.ONE))
          .isTrue();
    }
  }

  @Test
  void partialPrimitiveRestoresThroughJsonWithoutRechargingCurrency() throws Exception {
    var tiny = new BasicCurrencyTransitionsTest();
    var policy = AutomaticCurrencyPolicyLoader.load();
    var simulator = new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog));
    var kernel = new AutomaticCraftingKernel(policy, tiny.catalog, simulator, "amulets");
    var addition =
        new AutomaticCraftingAction(
            WorkbenchCurrency.EXALTED,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL);
    var replacement =
        new AutomaticCraftingAction(
            WorkbenchCurrency.PERFECT_ESSENCE_MIND,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.ORDINARY);
    var template =
        new AutomaticMethodExecution.Template(
            "restore-preparation", List.of(addition), List.of(replacement));
    java.util.function.Function<ItemState, Optional<Exit>> classify =
        item ->
            item.explicits().stream().anyMatch(m -> m.modifierId().equals("a"))
                ? Optional.of(Exit.GOAL)
                : Optional.empty();
    try (var original =
        new AutomaticMethodExecution(
            tiny.item(ItemState.Rarity.RARE, List.of()),
            template,
            kernel,
            classify,
            CertifiedInterval.Precision.standard(),
            100)) {
      original.step(1);
      var json = new com.fasterxml.jackson.databind.ObjectMapper();
      var saved =
          json.readValue(
              json.writeValueAsBytes(original.checkpoint("goal-a-v1")),
              AutomaticMethodExecution.Checkpoint.class);
      try (var restored = AutomaticMethodExecution.restore(saved, kernel, "goal-a-v1", classify)) {
        for (var execution : List.of(original, restored)) {
          while (!execution.atBoundary()) execution.step(1);
          execution.beginCycle();
          while (!execution.atBoundary()) execution.step(1);
        }
        assertThat(restored.observation()).isEqualTo(original.observation());
        assertThat(restored.unresolvedReasons()).isEqualTo(original.unresolvedReasons());
      }
      var changed = new AutomaticCraftingKernel(policy, tiny.catalog, simulator, "gloves");
      assertThatThrownBy(
              () -> AutomaticMethodExecution.restore(saved, kernel, "different-goal", classify))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("recalculation");
      assertThatThrownBy(
              () -> AutomaticMethodExecution.restore(saved, changed, "goal-a-v1", classify))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("recalculation");
    }
  }

  @Test
  void firstHitAfterPreparationSkipsReplacementAndCountsOnlyConsumedOmens() {
    var tiny = new BasicCurrencyTransitionsTest();
    var target = tiny.definition("c", ModifierDefinition.AffixType.PREFIX, 7, 7, 0);
    var catalog = tiny.catalog(List.of(tiny.implicit, tiny.a, tiny.b, target));
    var simulator =
        new WorkbenchSimulator(
            catalog,
            new CraftingEngine(catalog),
            Set.of(),
            Map.of(),
            Map.of(WorkbenchCurrency.PERFECT_ESSENCE_MIND, List.of("c")));
    var kernel =
        new AutomaticCraftingKernel(
            AutomaticCurrencyPolicyLoader.load(), catalog, simulator, "amulets");
    var addition =
        new AutomaticCraftingAction(
            WorkbenchCurrency.EXALTED,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL);
    var replace =
        new AutomaticCraftingAction(
            WorkbenchCurrency.PERFECT_ESSENCE_MIND,
            Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION),
            AutomaticCurrencyPolicy.Purpose.ORDINARY);
    var template =
        new AutomaticMethodExecution.Template(
            "temporary-then-fixed", List.of(addition), List.of(replace));
    try (var execution =
        new AutomaticMethodExecution(
            tiny.item(ItemState.Rarity.RARE, List.of()),
            template,
            kernel,
            item ->
                item.explicits().stream().anyMatch(m -> Set.of("a", "c").contains(m.modifierId()))
                    ? Optional.of(Exit.GOAL)
                    : Optional.empty(),
            CertifiedInterval.Precision.standard(),
            100,
            AutomaticMethodExecution.PreparationSuccess.STOP_ON_FIRST_HIT)) {
      while (!execution.atBoundary()) execution.step(1);
      var initial = execution.observation();
      var hit =
          initial.exits().get(Exit.GOAL).values().stream()
              .reduce(CertifiedInterval.zero(64), CertifiedInterval::add);
      assertThat(hit.contains(Fraction.of(1, 4))).isTrue();
      execution.beginCycle();
      while (!execution.atBoundary()) execution.step(1);
      assertThat(execution.absorbed()).isTrue();
      var point = execution.observation();
      for (var cycles : List.of(100, 300, 500)) {
        var observed = point.atLaterAbsorbedBoundary(cycles);
        assertThat(observed.mass().contains(Fraction.ONE)).isTrue();
        var initialUses = AutomaticMethodExecution.materialUses(observed.initialUses());
        var cycleUses = AutomaticMethodExecution.materialUses(observed.cycleUses());
        assertThat(initialUses.get("EXALTED").contains(Fraction.ONE)).isTrue();
        assertThat(cycleUses.get("PERFECT_ESSENCE_MIND").contains(Fraction.of(3, 4))).isTrue();
        assertThat(cycleUses.get("OMEN:DEXTRAL_CRYSTALLISATION").contains(Fraction.of(3, 4)))
            .isTrue();
      }
      assertThat(execution.unresolvedReasons()).isEmpty();
    }
    assertThatThrownBy(
            () ->
                new AutomaticMethodExecution.Template(
                    "standalone-random-goal", List.of(), List.of(addition)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
