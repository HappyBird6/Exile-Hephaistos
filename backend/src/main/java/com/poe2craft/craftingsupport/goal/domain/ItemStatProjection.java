package com.poe2craft.craftingsupport.goal.domain;

import com.poe2craft.craftingsupport.goal.domain.GoalEvaluator.Observation;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Capability;
import com.poe2craft.shared.item.CatalystQualityDisplay;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.ModifierMagnitudes;
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
  /** 실제 기폭제·주얼 효과로 반올림한 뒤 장비 배율을 적용한다. 미검증 수치는 비워 목표와 카드 모두에서 구분한다. */
  public static Map<String, BigDecimal> qualityValues(
      ItemState item, ModifierDefinition definition, ModifierInstance modifier) {
    var projected = CatalystQualityDisplay.projectStages(modifier, definition, item);
    if (projected.status().equals("UNREVIEWED_NUMERIC_SEMANTICS")) return Map.of();
    var factor =
        ModifierMagnitudes.factor(
            item, definition, definition.layer() == ModifierDefinition.Layer.IMPLICIT);
    var numbers = new TreeMap<String, BigDecimal>();
    projected
        .displayedValues()
        .forEach((id, value) -> numbers.put(id, BigDecimal.valueOf(value).multiply(factor)));
    return Map.copyOf(numbers);
  }

  public Map<String, Observation> project(
      ItemState item,
      Map<String, GoalCatalog.Stat> stats,
      boolean fullySupported,
      ItemCatalog catalog) {
    var result = new TreeMap<String, Observation>();
    var typed = item.catalystQuality() != null;
    var projected = typed ? new HashMap<String, Map<String, BigDecimal>>() : null;
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
      boolean supported = true;
      for (var modifier : modifiers) {
        var value = modifier.values().get(mapping.sourceStatId());
        if (value != null && seen.add(modifier.modifierId())) {
          present = true;
          var definition = catalog.modifiers().get(modifier.modifierId());
          if (projected != null) {
            var numbers =
                projected.computeIfAbsent(
                    modifier.modifierId(), id -> qualityValues(item, definition, modifier));
            var number = numbers.get(mapping.sourceStatId());
            if (number == null) {
              supported = false;
              break;
            }
            sum = sum.add(number);
          } else {
            sum =
                sum.add(
                    ModifierMagnitudes.value(
                        item,
                        definition,
                        modifier,
                        mapping.sourceStatId(),
                        mapping.layer().equals("IMPLICIT")));
          }
        }
      }
      result.put(
          stat.statId(),
          !supported
              ? Observation.unsupported()
              : present
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
