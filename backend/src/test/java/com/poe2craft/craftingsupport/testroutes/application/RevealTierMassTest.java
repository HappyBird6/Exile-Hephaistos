package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.craftingsupport.testroutes.application.RecipeTransitions.RevealGroup;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.probability.Fraction;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 미관측 티어 선택의 각 조건부 굴림에서 원래 뼈 확률을 복원하고, 다른 공개·비대칭 결과는 구분한다. */
class RevealTierMassTest {
  /** 아이템 레벨로 서로 다른 부모를 구분하고 각 부모에서 같은 두 티어를 공개할 수 있다. */
  private static RevealGroup group(int level) {
    var item =
        new ItemState(
            "snapshot", "base", level, ItemState.Rarity.RARE, List.of(), List.of(), Set.of());
    return new RevealGroup(item, Set.of("T1", "T2"));
  }

  /** 1/2로 열린 뼈에서 첫 티어 두 수치와 둘째 티어 한 수치가 전부 성공하면 최종 질량은 1/2이다. */
  @Test
  void agreesAcrossTiersWithoutAssumingHowOftenEachTierAppears() {
    var parent = RevealTierMass.known(Fraction.of(1, 2));
    var firstRoll = parent.select(group(82), "T1", Fraction.of(1, 3));
    var secondRoll = parent.select(group(82), "T1", Fraction.of(2, 3));
    var otherTier = parent.select(group(82), "T2", Fraction.ONE);
    assertThat(firstRoll.exact()).isNull();
    assertThat(firstRoll.plus(otherTier).exact()).isNull();
    assertThat(firstRoll.plus(secondRoll).plus(otherTier).exact()).isEqualTo(Fraction.of(1, 2));
    assertThat(
            firstRoll
                .plus(secondRoll)
                .times(Fraction.of(1, 4))
                .plus(otherTier.times(Fraction.of(1, 4)))
                .plus(RevealTierMass.known(Fraction.of(1, 8)))
                .exact())
        .isEqualTo(Fraction.of(1, 4));
  }

  /** 다른 부모에서 가져온 티어를 합쳐 불확실성을 지우거나, 미해결 공개 위에 새 티어 분포를 가정할 수 없다. */
  @Test
  void independentOrNestedSelectionsDoNotCancel() {
    var certain = RevealTierMass.known(Fraction.ONE);
    var first = certain.select(group(82), "T1", Fraction.ONE);
    var right = certain.select(group(81), "T2", Fraction.ONE);
    assertThat(first.plus(right).exact()).isNull();
    var nested = first.select(group(80), "T1", Fraction.ONE);
    assertThat(nested.plus(certain.select(group(80), "T2", Fraction.ONE)).exact()).isNull();
    assertThat(first.times(Fraction.ZERO).exact()).isEqualTo(Fraction.ZERO);
    assertThat(first.plus(RevealTierMass.known(null)).exact()).isNull();
  }
}
