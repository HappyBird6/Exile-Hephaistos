package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.ModifierDefinition.AffixType;
import java.util.*;
import org.junit.jupiter.api.Test;

class AbyssProbabilityComparisonTest {
  final HysteriaEssenceTest fixture = new HysteriaEssenceTest();
  final ItemCatalog catalog = fixture.catalog;
  final AbyssProbabilityComparison comparison = new AbyssProbabilityComparison(catalog);

  @Test
  void fullPrefixSideExcludesPrefixesEvenThoughUnrevealedHasNoCatalogId() {
    var state =
        AbyssCraftingState.from(
                fixture.rare(82, "", "amulet:prefix:hale", "amulet:prefix:adept-s"), catalog)
            .addUnrevealed("abyss", AffixType.PREFIX, catalog);
    assertThat(comparison.pool(state, WorkbenchCurrency.EXALTED, null))
        .isNotEmpty()
        .allMatch(d -> d.affixType() == AffixType.SUFFIX);
  }

  @Test
  void conditionalChanceUsesAnActualEligibleHighestWeightBlockerAndRecomputedPool() {
    var state =
        AbyssCraftingState.from(fixture.rare(82, "", "amulet:prefix:hale"), catalog)
            .addUnrevealed("abyss", AffixType.PREFIX, catalog);
    var targets = Set.of("amulet:suffix:of-the-brute");
    var result =
        comparison.compare(state, "abyss", WorkbenchCurrency.EXALTED, null, targets).orElseThrow();
    var blocker = catalog.find(result.blockerModifierId()).orElseThrow();
    assertThat(blocker.affixType()).isEqualTo(AffixType.PREFIX);
    assertThat(result.blockerWeight()).isEqualTo(blocker.weight());
    var baseline = comparison.pool(state, WorkbenchCurrency.EXALTED, null);
    var blocked =
        baseline.stream()
            .filter(d -> !Collections.disjoint(d.familyIds(), blocker.familyIds()))
            .mapToLong(ModifierDefinition::weight)
            .sum();
    assertThat(result.baseline().totalWeight())
        .isEqualTo(baseline.stream().mapToLong(ModifierDefinition::weight).sum());
    assertThat(result.afterBlocking().totalWeight())
        .isEqualTo(result.baseline().totalWeight() - blocked);
    assertThat(result.afterBlocking().targetWeight()).isEqualTo(result.baseline().targetWeight());
    assertThat(result.afterBlocking().percent()).isGreaterThan(result.baseline().percent());
    assertThat(state.slots().getLast().modifier()).isNull();
  }

  @Test
  void fullItemDoesNotClaimZeroChanceForAnUnavailableAddition() {
    var state =
        AbyssCraftingState.from(
                fixture.rare(
                    82,
                    "",
                    "amulet:prefix:hale",
                    "amulet:prefix:adept-s",
                    "amulet:suffix:of-the-brute",
                    "amulet:suffix:of-the-penguin",
                    "amulet:suffix:of-the-salamander"),
                catalog)
            .addUnrevealed("abyss", AffixType.PREFIX, catalog);
    assertThat(
            comparison.compare(
                state, "abyss", WorkbenchCurrency.EXALTED, null, Set.of("amulet:prefix:azure")))
        .isEmpty();
  }
}
