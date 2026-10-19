package com.poe2craft.crafting.domain.goalfilter;

import com.poe2craft.item.ModifierDefinition;
import java.util.*;

/** Conservative catalog proof, not a count of filter entries or a reachability claim. */
public final class ExplicitGoalScope {
  private ExplicitGoalScope() {}

  public static int minimumModifiers(GoalFilter goal, GoalCatalog catalog) {
    var groups = goal.groups().stream().filter(g -> !Boolean.TRUE.equals(g.disabled())).toList();
    if (groups.size() != 1 || groups.getFirst().type() != GoalFilter.Type.AND) return 0;
    var required = new HashSet<String>();
    for (var entry : groups.getFirst().entries()) {
      if (Boolean.TRUE.equals(entry.disabled())
          || entry.range() == null
          || entry.range().min() == null
          || entry.range().min().signum() <= 0) continue;
      var stat =
          catalog.stats().stream().filter(s -> s.statId().equals(entry.statId())).findFirst();
      if (stat.isEmpty() || stat.get().kind() != GoalCatalog.Kind.EXPLICIT) continue;
      GoalCatalogIndex.mapping(stat.get()).ifPresent(m -> required.add(m.sourceStatId()));
    }
    if (required.isEmpty() || catalog.sourceModifiers().isEmpty()) return 0;
    int coverage = 0;
    var available = new HashSet<String>();
    // Include every catalog modifier, even currently ineligible ones: overestimating coverage
    // only weakens this lower bound, so it cannot falsely reject a supported target.
    for (var definition : catalog.sourceModifiers().values()) {
      if (definition.layer() != ModifierDefinition.Layer.EXPLICIT) continue;
      var covered = new HashSet<String>();
      for (var stat : definition.stats()) if (required.contains(stat.id())) covered.add(stat.id());
      available.addAll(covered);
      coverage = Math.max(coverage, covered.size());
    }
    if (coverage == 0 || !available.containsAll(required)) return 0;
    return (required.size() + coverage - 1) / coverage;
  }
}
