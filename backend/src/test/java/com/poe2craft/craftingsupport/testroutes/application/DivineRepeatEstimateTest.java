package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.shared.probability.Fraction;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 디바인 반복의 혼합 기대값과 도달 불가·미검증 분포 처리를 검사한다. */
class DivineRepeatEstimateTest {
  /** 두 결과를 구분하는 예시 아이템이다. */
  private final ItemState hit = SolarAmulet.initial(ItemCatalogLoader.loadDefault(), 82, 12);

  /** 목표 미달로 구별할 예시 아이템이다. */
  private final ItemState miss = SolarAmulet.initial(ItemCatalogLoader.loadDefault(), 82, 13);

  /** 성공·실패 분포를 제공한다. */
  private RecipeTransitions.Law law(Fraction p) {
    return new RecipeTransitions.Law(
        List.of(
            new RecipeTransitions.Outcome(hit, p, Set.of()),
            new RecipeTransitions.Outcome(miss, p.complement(), Set.of())),
        true,
        Set.of());
  }

  /** 두 티어의 성공률이 1/4와 1/2이면 동등하게 도달했을 때 평균은 (4+2)/2다. */
  @Test
  void averagesTheRepeatCostOfEachStartingTier() {
    var estimate = new DivineRepeatEstimate();
    var quarter = Fraction.of(1, 4);
    estimate.add(quarter, law(quarter), hit::equals);
    estimate.add(quarter, law(Fraction.of(1, 2)), hit::equals);
    assertThat(estimate.expectedUses(Fraction.of(1, 2), true)).isEqualTo(3.0);
    assertThat(estimate.expectedUses(Fraction.ONE, true)).isNull();
    assertThat(estimate.expectedUses(Fraction.of(1, 2), false)).isNull();
  }

  /** 성공할 수 없는 티어에서도 같은 아이템에서 한도만큼 소모한다. */
  @Test
  void unreachableStateSpendsItsOwnLimitOnTheSameItem() {
    var estimate = new DivineRepeatEstimate();
    estimate.add(Fraction.of(1, 2), law(Fraction.ONE), hit::equals);
    estimate.add(Fraction.of(1, 2), law(Fraction.ZERO), hit::equals);
    assertThat(estimate.expectedUses(Fraction.ONE, true)).isEqualTo(500.5);
    assertThat(estimate.successWithinLimit(Fraction.ONE, true)).isEqualTo(0.5);
  }
}
