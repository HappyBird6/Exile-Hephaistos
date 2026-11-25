package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.craftingsupport.testroutes.application.RecipeTransitions.RevealGroup;
import com.poe2craft.shared.probability.Fraction;
import java.util.HashMap;
import java.util.Map;

/** 훼손 티어별 등장 비율을 모를 때 한 공개 선택의 조건부 질량을 보존하고, 모든 적격 티어가 동의한 값만 확정한다. */
final class RevealTierMass {
  /** 일반 분기의 빈 질량과 확정·미확정 상수는 경로 전체에서 재사용한다. */
  private static final RevealTierMass ZERO = new RevealTierMass(Fraction.ZERO, Map.of(), false);

  private static final RevealTierMass ONE = new RevealTierMass(Fraction.ONE, Map.of(), false);
  private static final RevealTierMass UNKNOWN = new RevealTierMass(Fraction.ZERO, Map.of(), true);

  /** 티어와 무관하게 확인한 질량이다. */
  private final Fraction direct;

  /** 각 공개 기회에서 해당 티어가 선택되었다는 조건 아래의 질량이다. 관측되지 않은 적격 티어의 질량은 0이다. */
  private final Map<RevealGroup, Map<String, Fraction>> draws;

  /** 알려진 질량 외에 상한을 확인하지 못한 기여분이 있으면 참이다. */
  private final boolean unknownExtra;

  /** 독립적으로 확인한 질량과 아직 알 수 없는 기여분의 유무를 보관한다. */
  private RevealTierMass(
      Fraction direct, Map<RevealGroup, Map<String, Fraction>> draws, boolean unknownExtra) {
    this.direct = direct;
    this.draws = draws;
    this.unknownExtra = unknownExtra;
  }

  /** 기존 작업대 분기를 그대로 보존한다. 분포를 모르는 일반 전이는 null이다. */
  static RevealTierMass known(Fraction mass) {
    if (mass == null) return UNKNOWN;
    if (mass == Fraction.ZERO) return ZERO;
    if (mass == Fraction.ONE) return ONE;
    return new RevealTierMass(mass, Map.of(), false);
  }

  /** 배타적인 상태 결과의 질량을 더한다. 서로 다른 공개 기회의 티어 조건은 독립적으로 보존한다. */
  RevealTierMass plus(RevealTierMass other) {
    if (this == ZERO) return other;
    if (other == ZERO) return this;
    var combined = direct.add(other.direct);
    boolean unresolved = unknownExtra || other.unknownExtra;
    if (draws.isEmpty()) return new RevealTierMass(combined, other.draws, unresolved);
    if (other.draws.isEmpty()) return new RevealTierMass(combined, draws, unresolved);
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
    return new RevealTierMass(combined, merged, unresolved);
  }

  /** 일반 작업대 전이의 확률을 모든 티어 조건에 적용한다. */
  RevealTierMass times(Fraction weight) {
    if (this == ZERO || Fraction.ZERO.equals(weight)) return ZERO;
    if (weight == null) return Fraction.ZERO.equals(maximum()) ? ZERO : UNKNOWN;
    if (Fraction.ONE.equals(weight)) return this;
    if (draws.isEmpty()) return new RevealTierMass(direct.multiply(weight), Map.of(), unknownExtra);
    var scaled = new HashMap<RevealGroup, Map<String, Fraction>>();
    draws.forEach(
        (draw, tiers) -> {
          var masses = new HashMap<String, Fraction>();
          tiers.forEach((tier, mass) -> masses.put(tier, mass.multiply(weight)));
          scaled.put(draw, masses);
        });
    return new RevealTierMass(direct.multiply(weight), scaled, unknownExtra);
  }

  /** 새로운 공개의 티어 조건과 그 티어 안에서의 수치 굴림·뼈 분기 확률을 기록한다. 이전 공개에 의존하는 두 번째 선택은 미확정이다. */
  RevealTierMass select(RevealGroup group, String tier, Fraction branchRollMass) {
    if (!group.tiers().contains(tier)) throw new IllegalArgumentException("Ineligible reveal tier");
    if (group.tiers().size() == 1) return times(branchRollMass);
    if (Fraction.ZERO.equals(branchRollMass)) return known(Fraction.ZERO);
    var parent = exact();
    if (branchRollMass == null) return times(null);
    if (Fraction.ZERO.equals(parent)) return known(Fraction.ZERO);
    var lower = parent == null ? minimum() : parent;
    return new RevealTierMass(
        Fraction.ZERO, Map.of(group, Map.of(tier, lower.multiply(branchRollMass))), parent == null);
  }

  /** 모든 적격 티어의 기여분이 같을 때에만 공개 기회마다 해당 값을 한 번 더해 실제 확률을 복원한다. */
  Fraction exact() {
    if (unknownExtra) return null;
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

  /** 공개 티어 가중치를 어떻게 선택하더라도 반드시 포함하는 성공 질량이다. */
  Fraction minimum() {
    return tierBound(false);
  }

  /** 가능한 공개 티어별 최대 기여분을 더한다. 추가 질량을 모르면 호출자가 직전 경로 상한을 사용한다. */
  Fraction maximum() {
    return unknownExtra ? null : tierBound(true);
  }

  /** 각 공개 기회에서 빠진 티어는 0이다. 같은 공개 조건의 기여분은 먼저 합친 후 범위를 계산한다. */
  private Fraction tierBound(boolean upper) {
    var sum = direct;
    for (var entry : draws.entrySet()) {
      Fraction extreme = null;
      for (String tier : entry.getKey().tiers()) {
        var mass = entry.getValue().getOrDefault(tier, Fraction.ZERO);
        if (extreme == null) {
          extreme = mass;
        } else {
          int comparison =
              mass.numerator()
                  .multiply(extreme.denominator())
                  .compareTo(extreme.numerator().multiply(mass.denominator()));
          if (upper ? comparison > 0 : comparison < 0) extreme = mass;
        }
      }
      sum = sum.add(extreme);
    }
    return sum;
  }
}
