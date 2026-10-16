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
