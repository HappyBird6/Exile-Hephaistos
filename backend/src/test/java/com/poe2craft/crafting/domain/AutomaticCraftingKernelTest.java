package com.poe2craft.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomaticCraftingKernelTest {
  final AutomaticCurrencyPolicy policy = AutomaticCurrencyPolicyLoader.load();
  final BasicCurrencyTransitionsTest tiny = new BasicCurrencyTransitionsTest();

  @Test
  void goalRollCellsReconstructTheIndependentScalarOracleWithoutMergingModifierTiers() {
    var root = tiny.item(ItemState.Rarity.RARE, List.of());
    var known =
        new com.poe2craft.crafting.domain.goalfilter.GoalCatalog.Stat(
            "a",
            "a",
            "flat",
            com.poe2craft.crafting.domain.goalfilter.GoalCatalog.Kind.EXPLICIT,
            new com.poe2craft.crafting.domain.goalfilter.GoalCatalog.Support(
                com.poe2craft.crafting.domain.goalfilter.GoalFilter.Capability.SUPPORTED,
                com.poe2craft.crafting.domain.goalfilter.GoalFilter.Capability.SUPPORTED,
                null),
            true,
            null,
            List.of("a"),
            List.of(),
            List.of());
    var goal =
        new com.poe2craft.crafting.domain.goalfilter.GoalFilter(
            1,
            "test",
            new com.poe2craft.crafting.domain.goalfilter.GoalFilter.General(
                root.baseItemId(),
                new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Range(null, null),
                List.of(ItemState.Rarity.RARE)),
            List.of(
                new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Group(
                    "g",
                    com.poe2craft.crafting.domain.goalfilter.GoalFilter.Type.AND,
                    false,
                    null,
                    List.of(
                        new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Entry(
                            "e",
                            "a",
                            "flat",
                            new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Range(
                                java.math.BigDecimal.valueOf(2), null),
                            null,
                            false)))));
    var exact =
        new AutomaticCraftingKernel(
            policy,
            tiny.catalog,
            new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog)),
            "amulets");
    var grouped =
        exact.withRollPartition(
            com.poe2craft.crafting.domain.pathsearch.RollPartition.compile(
                tiny.catalog, Map.of("a", known), goal, root),
            goal.toString());
    var action =
        new AutomaticCraftingAction(
            WorkbenchCurrency.EXALTED,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL);
    var oracle = exact.expand(root, action, 100);
    var cells = grouped.expand(root, action, 100);
    assertThat(cells.outcomes()).hasSize(3);
    var rebuilt = new LinkedHashMap<ItemState, Fraction>();
    for (var cell : cells.outcomes()) {
      var bands = grouped.rollBands(cell.item());
      if (bands.isEmpty()) rebuilt.put(cell.item(), cell.probability());
      else {
        var modifier = cell.item().explicits().getFirst();
        var band = bands.get(modifier.modifierId());
        for (long v = band.min(); v <= band.max(); v++)
          rebuilt.put(
              tiny.item(ItemState.Rarity.RARE, List.of(tiny.mod(modifier.modifierId(), v))),
              cell.probability().multiply(Fraction.of(1, band.max() - band.min() + 1)));
      }
    }
    assertThat(rebuilt).hasSize(oracle.outcomes().size());
    for (var outcome : oracle.outcomes())
      assertThat(rebuilt.get(outcome.item())).isEqualTo(outcome.probability());
    assertThat(grouped.identity()).isNotEqualTo(exact.identity());
  }

  @Test
  void unverifiedJointCandidateKeepsItsWeightWithoutHidingVerifiedScalarOutcomes() {
    var joint =
        new ModifierDefinition(
            "joint",
            "joint",
            ModifierDefinition.Layer.EXPLICIT,
            ModifierDefinition.AffixType.PREFIX,
            Set.of("joint"),
            1,
            4,
            1,
            "joint",
            List.of(
                new ModifierDefinition.StatRange("x", 1, 2),
                new ModifierDefinition.StatRange("y", 1, 2)),
            Set.of(),
            "https://example.test/joint");
    var catalog = tiny.catalog(List.of(tiny.implicit, tiny.a, tiny.b, joint));
    var kernel =
        new AutomaticCraftingKernel(
            policy,
            catalog,
            new WorkbenchSimulator(catalog, new CraftingEngine(catalog)),
            "amulets");
    var result =
        kernel.expand(
            tiny.item(ItemState.Rarity.RARE, List.of()),
            new AutomaticCraftingAction(
                WorkbenchCurrency.EXALTED,
                Set.of(),
                AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL),
            100);
    assertThat(result.status()).isEqualTo(AutomaticCraftingKernel.Status.PARTIAL);
    assertThat(result.reason()).isEqualTo("JOINT_ROLL_MODEL_NOT_IMPLEMENTED");
    assertThat(result.unresolved()).isEqualTo(Fraction.of(1, 2));
    assertThat(result.outcomes())
        .extracting(AutomaticCraftingKernel.Outcome::probability)
        .containsExactlyInAnyOrder(
            Fraction.of(1, 16), Fraction.of(1, 16), Fraction.of(3, 16), Fraction.of(3, 16));
  }

  @Test
  void optedInJointRollsKeepCorrelationTickWeightsAndUnresolvedBudgetMass() {
    var joint =
        new ModifierDefinition(
            "joint",
            "joint",
            ModifierDefinition.Layer.EXPLICIT,
            ModifierDefinition.AffixType.PREFIX,
            Set.of("joint"),
            1,
            4,
            1,
            "joint",
            List.of(
                new ModifierDefinition.StatRange("x", 1, 2),
                new ModifierDefinition.StatRange("y", 1, 2)),
            Set.of(),
            "https://example.test/joint");
    var catalog = tiny.catalog(List.of(tiny.implicit, tiny.a, tiny.b, joint));
    var kernel =
        new AutomaticCraftingKernel(
            policy,
            catalog,
            new WorkbenchSimulator(catalog, new CraftingEngine(catalog), Set.of("joint")),
            "amulets");
    var root = tiny.item(ItemState.Rarity.RARE, List.of());
    var action =
        new AutomaticCraftingAction(
            WorkbenchCurrency.EXALTED,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL);
    var result = kernel.expand(root, action, 100);
    assertThat(result.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
    assertThat(result.unresolved()).isEqualTo(Fraction.ZERO);
    var tuples = new LinkedHashMap<Map<String, Long>, Fraction>();
    result
        .outcomes()
        .forEach(
            o -> {
              var m = o.item().explicits().getFirst();
              if (m.modifierId().equals("joint")) tuples.put(m.values(), o.probability());
            });
    assertThat(tuples)
        .containsOnly(
            entry(Map.of("x", 1L, "y", 1L), Fraction.of(5000, 20002)),
            entry(Map.of("x", 2L, "y", 2L), Fraction.of(5001, 20002)));
    assertThat(result.assumptions())
        .anySatisfy(
            a -> {
              assertThat(a.id()).isEqualTo(CoupledStatRollModel.ID);
              assertThat(a.reason()).contains("Unverified user conjecture");
              assertThat(a.ratioTick()).isNull();
            });
    assertThat(kernel.representatives(kernel.plan(root, action)))
        .anySatisfy(
            i -> assertThat(i.explicits().getFirst().values()).isEqualTo(Map.of("x", 2L, "y", 2L)));
    var partial = kernel.expand(root, action, 5);
    assertThat(partial.status()).isEqualTo(AutomaticCraftingKernel.Status.PARTIAL);
    assertThat(partial.reason()).isEqualTo("OUTCOME_BUDGET");
    assertThat(
            partial.outcomes().stream()
                .map(AutomaticCraftingKernel.Outcome::probability)
                .reduce(Fraction.ZERO, Fraction::add)
                .add(partial.unresolved()))
        .isEqualTo(Fraction.ONE);
    assertThat(partial.unresolved().numerator()).isPositive();
  }

  @Test
  void scalarOraclePreservesNonGoalCandidatesAndBudgetRemainder() {
    var kernel =
        new AutomaticCraftingKernel(
            policy,
            tiny.catalog,
            new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog)),
            "amulets");
    var action =
        new AutomaticCraftingAction(
            WorkbenchCurrency.EXALTED,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL);
    var item = tiny.item(ItemState.Rarity.RARE, List.of());
    var result = kernel.expand(item, action, 100);
    assertThat(result.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
    assertThat(result.outcomes())
        .extracting(AutomaticCraftingKernel.Outcome::probability)
        .containsExactlyInAnyOrder(
            Fraction.of(1, 8), Fraction.of(1, 8), Fraction.of(3, 8), Fraction.of(3, 8));
    var partial = kernel.expand(item, action, 1);
    assertThat(partial.status()).isEqualTo(AutomaticCraftingKernel.Status.PARTIAL);
    assertThat(partial.outcomes()).hasSize(1);
    assertThat(partial.outcomes().getFirst().probability().add(partial.unresolved()))
        .isEqualTo(Fraction.ONE);
    assertThat(partial.outcomes().getFirst().probability().numerator())
        .isLessThan(partial.outcomes().getFirst().probability().denominator());
  }

  @Test
  void replacementHasUniformRemovalAndConsumedOmenChangesOnlyRemovalSide() {
    var target = tiny.definition("c", ModifierDefinition.AffixType.PREFIX, 7, 7, 99);
    var catalog = tiny.catalog(List.of(tiny.implicit, tiny.a, tiny.b, target));
    var simulator =
        new WorkbenchSimulator(
            catalog,
            new CraftingEngine(catalog),
            Set.of(),
            Map.of(),
            Map.of(WorkbenchCurrency.PERFECT_ESSENCE_MIND, List.of("c")));
    var kernel = new AutomaticCraftingKernel(policy, catalog, simulator, "amulets");
    var item = tiny.item(ItemState.Rarity.RARE, List.of(tiny.mod("a", 1), tiny.mod("b", 3)));
    var plain =
        new AutomaticCraftingAction(
            WorkbenchCurrency.PERFECT_ESSENCE_MIND,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.ORDINARY);
    var result = kernel.expand(item, plain, 100);
    assertThat(result.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
    assertThat(result.outcomes())
        .hasSize(2)
        .allMatch(o -> o.probability().equals(Fraction.of(1, 2)));
    var prefix =
        kernel.expand(
            item,
            new AutomaticCraftingAction(
                plain.currency(), Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION), plain.purpose()),
            100);
    assertThat(prefix.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
    assertThat(prefix.outcomes()).hasSize(1);
    assertThat(prefix.outcomes().getFirst().item().explicits())
        .extracting(ModifierInstance::modifierId)
        .containsExactly("b", "c");
    assertThat(prefix.outcomes().getFirst().probability()).isEqualTo(Fraction.ONE);
  }

  @Test
  void fifthExplicitRetainsBoundaryMassAndReplacementCanStillRunAtFour() {
    var c = tiny.definition("c", ModifierDefinition.AffixType.PREFIX, 1, 1, 1);
    var d = tiny.definition("d", ModifierDefinition.AffixType.SUFFIX, 1, 1, 1);
    var e = tiny.definition("e", ModifierDefinition.AffixType.PREFIX, 1, 1, 1);
    var catalog = tiny.catalog(List.of(tiny.implicit, tiny.a, tiny.b, c, d, e));
    var simulator =
        new WorkbenchSimulator(
            catalog,
            new CraftingEngine(catalog),
            Set.of(),
            Map.of(),
            Map.of(WorkbenchCurrency.PERFECT_ESSENCE_MIND, List.of("e")));
    var kernel = new AutomaticCraftingKernel(policy, catalog, simulator, "amulets");
    var item =
        tiny.item(
            ItemState.Rarity.RARE,
            List.of(tiny.mod("a", 1), tiny.mod("b", 3), tiny.mod("c", 1), tiny.mod("d", 1)));
    var result =
        kernel.expand(
            item,
            new AutomaticCraftingAction(
                WorkbenchCurrency.EXALTED,
                Set.of(),
                AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL),
            1);
    assertThat(result.boundary()).isEqualTo(Fraction.ONE);
    assertThat(result.unresolved()).isEqualTo(Fraction.ZERO);
    assertThat(result.outcomes()).isEmpty();
    var replacement =
        kernel.expand(
            item,
            new AutomaticCraftingAction(
                WorkbenchCurrency.PERFECT_ESSENCE_MIND,
                Set.of(),
                AutomaticCurrencyPolicy.Purpose.ORDINARY),
            100);
    assertThat(replacement.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
    assertThat(replacement.outcomes()).hasSize(4).allMatch(o -> o.item().explicits().size() == 4);
  }

  @Test
  void everyApprovedMaterialIsRepresentedAndAbyssRemainsExplicitlyUnsupported() {
    var candidates = AutomaticCraftingAction.candidates(policy);
    var ids = new HashSet<String>();
    for (var candidate : candidates) {
      ids.add(candidate.currency().name());
      candidate.omens().forEach(o -> ids.add("OMEN:" + o.name()));
    }
    assertThat(ids)
        .containsExactlyInAnyOrderElementsOf(
            policy.candidates().stream().map(AutomaticCurrencyPolicy.Entry::id).toList());
    var kernel =
        new AutomaticCraftingKernel(
            policy,
            tiny.catalog,
            new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog)),
            "amulets");
    for (var candidate : candidates) {
      var result = kernel.expand(tiny.item(ItemState.Rarity.RARE, List.of()), candidate, 1);
      assertThat(result).isNotNull();
      if (candidate.currency() == WorkbenchCurrency.ESSENCE_ABYSS)
        assertThat(result.status()).isEqualTo(AutomaticCraftingKernel.Status.UNSUPPORTED);
    }
  }
}
