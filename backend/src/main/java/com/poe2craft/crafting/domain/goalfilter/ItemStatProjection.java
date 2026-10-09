package com.poe2craft.crafting.domain.goalfilter;

import com.poe2craft.crafting.domain.goalfilter.GoalEvaluator.Observation;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Capability;
import com.poe2craft.item.ItemState;
import java.math.BigDecimal;
import java.util.*;

/** Exact source/modifier/layer sums for validated items. No formatted text or Bucket input. */
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
          sum =
              sum.add(
                  BigDecimal.valueOf(value)
                      .multiply(BigDecimal.valueOf(mapping.numerator()))
                      .divide(BigDecimal.valueOf(mapping.denominator())));
        }
      }
      result.put(stat.statId(), present ? Observation.present(sum) : Observation.absent());
    }
    for (var stat : stats.values())
      if (stat.kind() == GoalCatalog.Kind.PSEUDO)
        result.put(stat.statId(), GoalEvaluator.pseudo(stat.contributions(), result));
    return Collections.unmodifiableMap(result);
  }
}
