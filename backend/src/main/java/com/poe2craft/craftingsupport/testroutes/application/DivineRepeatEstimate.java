package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.configuration.PublicContracts;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.probability.Fraction;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.function.Predicate;

/** 각 시작 아이템의 고정된 티어에서 디바인을 최대 1,000회 사용했을 때의 적중률과 평균 소모량을 합산한다. */
final class DivineRepeatEstimate {
  /** 한 베이스에서 시도하는 반복 제작의 한도다. */
  static final int MAX_USES = PublicContracts.REPEAT.maximumUses();

  /** 모든 재굴림을 검증한 시작 상태들의 정확한 도달 질량이다. */
  private Fraction covered = Fraction.ZERO;

  /** 상태별 한도 내 적중률과 평균 사용량을 그 상태의 도달 질량으로 가중한 합이다. */
  private BigDecimal successes = BigDecimal.ZERO;

  private BigDecimal uses = BigDecimal.ZERO;

  /** 도달한 상태의 재굴림 분포가 미확정이면 기대값도 미확정이다. */
  private boolean known = true;

  /** 한 시작 상태의 모든 결과로 1회 적중률 p를 구한다. N회 내 적중은 1-(1-p)^N, 평균 소모는 그 값을 p로 나눈 값이다. */
  void add(Fraction parentMass, RecipeTransitions.Law law, Predicate<ItemState> matchesGoal) {
    if (!known || Fraction.ZERO.equals(parentMass)) return;
    if (parentMass == null || !law.complete()) {
      known = false;
      return;
    }
    Fraction total = Fraction.ZERO;
    Fraction hits = Fraction.ZERO;
    for (var outcome : law.outcomes()) {
      if (outcome.probability() == null) {
        known = false;
        return;
      }
      total = total.add(outcome.probability());
      if (matchesGoal.test(outcome.state())) hits = hits.add(outcome.probability());
    }
    if (!Fraction.ONE.equals(total)) {
      known = false;
      return;
    }
    double p = decimal(hits).doubleValue();
    // 작은 적중률도 1에서 차감하는 과정에서 0이 되지 않게 계산한다. 적중 불가능하면 같은 아이템에서 한도를 소진한다.
    double chance = p == 1 ? 1 : -Math.expm1(MAX_USES * Math.log1p(-p));
    double draws = p == 0 ? MAX_USES : Math.min(MAX_USES, chance / p);
    var weight = decimal(parentMass);
    successes = successes.add(weight.multiply(BigDecimal.valueOf(chance)));
    uses = uses.add(weight.multiply(BigDecimal.valueOf(draws)));
    covered = covered.add(parentMass);
  }

  /** 필요한 시작 상태의 분포를 모두 확인했을 때 최대 1,000회 안에 목표에 도달하는 확률을 반환한다. */
  Double successWithinLimit(Fraction liveParentMass, boolean complete) {
    return mean(successes, liveParentMass, complete);
  }

  /** 실패로 한도를 소진한 경우까지 포함한 평균 사용량을 반환한다. */
  Double expectedUses(Fraction liveParentMass, boolean complete) {
    return mean(uses, liveParentMass, complete);
  }

  /** 빠진 시작 상태가 있으면 null을 반환한다. 완전한 경우 도달 질량으로 조건부 평균을 구한다. */
  private Double mean(BigDecimal weighted, Fraction liveParentMass, boolean complete) {
    return known && complete && !Fraction.ZERO.equals(covered) && covered.equals(liveParentMass)
        ? weighted.divide(decimal(covered), MathContext.DECIMAL128).doubleValue()
        : null;
  }

  /** 상태의 정확한 질량을 가중 평균에 필요한 정밀도의 소수로 변환한다. */
  private static BigDecimal decimal(Fraction mass) {
    return new BigDecimal(mass.numerator())
        .divide(new BigDecimal(mass.denominator()), MathContext.DECIMAL128);
  }
}
