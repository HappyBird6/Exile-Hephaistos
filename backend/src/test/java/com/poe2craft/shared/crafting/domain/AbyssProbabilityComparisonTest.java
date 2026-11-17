package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 다음 옵션 추가 확률의 민감도를 비교한다. 훼손 후보 공개 확률은 계산하지 않는다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code fixture}: 검증사례 ({@code HysteriaEssenceTest}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code comparison}: 비교 ({@code AbyssProbabilityComparison}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code fullPrefixSideExcludesPrefixesEvenThoughUnrevealedHasNoCatalogId()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code conditionalChanceUsesAnActualEligibleHighestWeightBlockerAndRecomputedPool()}: 이름에
 *       명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code fullItemDoesNotClaimZeroChanceForAnUnavailableAddition()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 * </ul>
 */
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
