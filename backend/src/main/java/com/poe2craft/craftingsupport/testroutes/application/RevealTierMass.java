package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.craftingsupport.testroutes.application.RecipeTransitions.RevealGroup;
import com.poe2craft.shared.probability.Fraction;
import java.util.HashMap;
import java.util.Map;

/** 훼손 티어별 등장 비율을 모를 때 한 공개 선택의 조건부 질량을 보존하고, 모든 적격 티어가 동의한 값만 확정한다. */
final class RevealTierMass {
  /** 일반 분기의 빈 질량과 확정·미확정 상수는 경로 전체에서 재사용한다. */
  private static final RevealTierMass ZERO = new RevealTierMass(Fraction.ZERO, Map.of());

  private static final RevealTierMass ONE = new RevealTierMass(Fraction.ONE, Map.of());
  private static final RevealTierMass UNKNOWN = new RevealTierMass(null, Map.of());

  /** 티어와 무관하게 알려진 질량이다. 일반 전이까지 미확정이면 null이다. */
  private final Fraction direct;

  /** 각 공개 기회에서 해당 티어가 선택되었다는 조건 아래의 질량이다. 관측되지 않은 적격 티어의 질량은 0이다. */
  private final Map<RevealGroup, Map<String, Fraction>> draws;

  /** 구성 후 수정하지 않는 공개별·티어별 질량과 일반 질량을 보관한다. */
  private RevealTierMass(Fraction direct, Map<RevealGroup, Map<String, Fraction>> draws) {
    this.direct = direct;
    this.draws = draws;
  }

  /** 기존 작업대 분기를 그대로 보존한다. 분포를 모르는 일반 전이는 null이다. */
  static RevealTierMass known(Fraction mass) {
    if (mass == null) return UNKNOWN;
    if (mass == Fraction.ZERO) return ZERO;
    if (mass == Fraction.ONE) return ONE;
    return new RevealTierMass(mass, Map.of());
  }

  /** 배타적인 상태 결과의 질량을 더한다. 서로 다른 공개 기회의 티어 조건은 독립적으로 보존한다. */
  RevealTierMass plus(RevealTierMass other) {
    if (this == UNKNOWN || other == UNKNOWN) return UNKNOWN;
    if (this == ZERO) return other;
    if (other == ZERO) return this;
    var combined = direct.add(other.direct);
    if (draws.isEmpty()) return new RevealTierMass(combined, other.draws);
    if (other.draws.isEmpty()) return new RevealTierMass(combined, draws);
    var merged = new HashMap<>(draws);
    other.draws.forEach(
        (draw, tiers) ->
            merged.merge(
                draw,
                tiers,
                (left, right) -> {
                  var sums = new HashMap<>(left);
                  right.forEach((tier, mass) -> sums.merge(tier, mass, Fraction::add));
                  return sums;
                }));
    return new RevealTierMass(combined, merged);
  }

  /** 일반 작업대 전이의 확률을 모든 티어 조건에 적용한다. */
  RevealTierMass times(Fraction weight) {
    if (Fraction.ZERO.equals(weight)) return known(Fraction.ZERO);
    if (direct == null || weight == null) return known(null);
    if (Fraction.ONE.equals(weight)) return this;
    if (draws.isEmpty()) return known(direct.multiply(weight));
    var scaled = new HashMap<RevealGroup, Map<String, Fraction>>();
    draws.forEach(
        (draw, tiers) -> {
          var masses = new HashMap<String, Fraction>();
          tiers.forEach((tier, mass) -> masses.put(tier, mass.multiply(weight)));
          scaled.put(draw, masses);
        });
    return new RevealTierMass(direct.multiply(weight), scaled);
  }

  /** 새로운 공개의 티어 조건과 그 티어 안에서의 수치 굴림·뼈 분기 확률을 기록한다. 이전 공개에 의존하는 두 번째 선택은 미확정이다. */
  RevealTierMass select(RevealGroup group, String tier, Fraction branchRollMass) {
    if (!group.tiers().contains(tier)) throw new IllegalArgumentException("Ineligible reveal tier");
    if (group.tiers().size() == 1) return times(branchRollMass);
    if (Fraction.ZERO.equals(branchRollMass)) return known(Fraction.ZERO);
    var parent = exact();
    if (parent == null || branchRollMass == null) return known(null);
    if (Fraction.ZERO.equals(parent)) return known(Fraction.ZERO);
    return new RevealTierMass(
        Fraction.ZERO, Map.of(group, Map.of(tier, parent.multiply(branchRollMass))));
  }

  /** 모든 적격 티어의 기여분이 같을 때에만 공개 기회마다 해당 값을 한 번 더해 실제 확률을 복원한다. */
  Fraction exact() {
    if (direct == null) return null;
    var sum = direct;
    for (var entry : draws.entrySet()) {
      var masses = entry.getValue();
      Fraction common = null;
      for (String tier : entry.getKey().tiers()) {
        var mass = masses.getOrDefault(tier, Fraction.ZERO);
        if (common != null && !common.equals(mass)) return null;
        common = mass;
      }
      sum = sum.add(common);
    }
    return sum;
  }
}
