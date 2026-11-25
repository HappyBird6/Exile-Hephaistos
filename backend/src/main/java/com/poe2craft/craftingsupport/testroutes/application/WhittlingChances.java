package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.shared.probability.Fraction;

/** 같은 시작 상태 분포에서 절사 한 번의 성공·복구 가능·실패를 서로 겹치지 않게 합산한다. */
final class WhittlingChances {
  /** 성공만 원래 경로를 이어가며 복구는 다음 재시도의 가능 여부만 나타낸다. */
  RevealTierMass success = RevealTierMass.known(Fraction.ZERO);

  RevealTierMass recoverable = RevealTierMass.known(Fraction.ZERO);
  RevealTierMass failure = RevealTierMass.known(Fraction.ZERO);
  boolean complete = true;

  /** 미검사 질량을 각 결과의 상한에 남기고, 이미 확정한 다른 두 결과는 제외한다. */
  RecipeRoutePlanner.OmenOutcomes measured(
      RecipeRoutePlanner.Chance hit,
      RecipeProbabilityRange live,
      Fraction rawLive,
      boolean allParents,
      RevealTierMass mainSuccess) {
    // 성공 경로의 수치 분포와 1회 옵션 ID 검사가 서로 다른 예산 지점에서 끝날 수 있다.
    var provenSuccess = mainSuccess.minimum();
    var measuredSuccess = success.minimum();
    if (RecipeProbabilityRange.min(provenSuccess, measuredSuccess).equals(provenSuccess))
      provenSuccess = measuredSuccess;
    var recovered =
        chance(
            recoverable,
            provenSuccess.add(failure.minimum()),
            live,
            rawLive,
            allParents && complete);
    var failed =
        chance(
            failure,
            provenSuccess.add(recoverable.minimum()),
            live,
            rawLive,
            allParents && complete);
    return new RecipeRoutePlanner.OmenOutcomes(hit, recovered, failed);
  }

  /** 관측된 실제 질량을 같은 단계에 도달한 부모 질량으로 나눈다. 부모나 일부 결과가 미상이면 범위를 반환한다. */
  private static RecipeRoutePlanner.Chance chance(
      RevealTierMass mass,
      Fraction other,
      RecipeProbabilityRange live,
      Fraction denominator,
      boolean complete) {
    var exact =
        complete
                && denominator != null
                && !Fraction.ZERO.equals(denominator)
                && mass.exact() != null
            ? mass.exact().divide(denominator)
            : null;
    var range = RecipeProbabilityRange.event(mass, complete, live.upper(), other).given(live);
    return RecipeRoutePlanner.Chance.measured(exact, range);
  }
}
