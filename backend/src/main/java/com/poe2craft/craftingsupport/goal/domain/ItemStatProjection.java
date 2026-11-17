package com.poe2craft.craftingsupport.goal.domain;

import com.poe2craft.craftingsupport.goal.domain.GoalEvaluator.Observation;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Capability;
import com.poe2craft.shared.item.ItemState;
import java.math.BigDecimal;
import java.util.*;

/**
 * 검증된 아이템의 옵션·계층·출처별 수치 합계를 계산한다.
 *
 * <p>기존 계약: Exact source/modifier/layer sums for validated items. No formatted text or Bucket
 * input.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code project(item, stats, fullySupported)}: 투영 처리에서 {@code stats.values}, {@code
 *       stat.kind}을(를) 사용한다. 입력: {@code item}(아이템), {@code stats}(수치목록), {@code
 *       fullySupported}(fully·지원). 반환: {@code Map<String, Observation>}.
 * </ul>
 */
public final class ItemStatProjection {
  public Map<String, Observation> project(
      ItemState item, Map<String, GoalCatalog.Stat> stats, boolean fullySupported) {
    var result = new TreeMap<String, Observation>();
    for (var stat : stats.values()) {
      if (stat.kind() == GoalCatalog.Kind.PSEUDO) continue;
      if (!fullySupported || stat.support().evaluation() != Capability.SUPPORTED) {
        result.put(stat.statId(), Observation.unsupported());
        continue;
      }
      var mapping = GoalCatalogIndex.mapping(stat).orElseThrow();
      var modifiers = mapping.layer().equals("IMPLICIT") ? item.implicits() : item.explicits();
      var seen = new HashSet<String>();
      var sum = BigDecimal.ZERO;
      boolean present = false;
      for (var modifier : modifiers) {
        var value = modifier.values().get(mapping.sourceStatId());
        if (value != null && seen.add(modifier.modifierId())) {
          present = true;
          sum = sum.add(BigDecimal.valueOf(value));
        }
      }
      result.put(
          stat.statId(),
          present
              ? Observation.presentNumber(
                  new GoalNumber(
                      sum.multiply(BigDecimal.valueOf(mapping.numerator())),
                      BigDecimal.valueOf(mapping.denominator())))
              : Observation.absent());
    }
    for (var stat : stats.values())
      if (stat.kind() == GoalCatalog.Kind.PSEUDO)
        result.put(stat.statId(), GoalEvaluator.pseudo(stat.contributions(), result));
    return Collections.unmodifiableMap(result);
  }
}
