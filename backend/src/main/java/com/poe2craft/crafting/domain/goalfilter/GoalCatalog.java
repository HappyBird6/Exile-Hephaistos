package com.poe2craft.crafting.domain.goalfilter;

import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import com.poe2craft.item.ModifierDefinition;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Display units and source scales are explicit; production IDs are local, never GGG IDs. */
public record GoalCatalog(
    int version,
    String catalogVersion,
    Context context,
    List<GroupSupport> groupTypes,
    List<Stat> stats,
    List<Issue> issues,
    Map<String, ModifierDefinition> sourceModifiers) {
  public GoalCatalog(
      int version,
      String catalogVersion,
      Context context,
      List<GroupSupport> groupTypes,
      List<Stat> stats,
      List<Issue> issues) {
    this(version, catalogVersion, context, groupTypes, stats, issues, Map.of());
  }

  public record Support(Capability evaluation, Capability probability, String reasonCode) {}

  public record GroupSupport(
      Type type, Capability evaluation, Capability probability, String reasonCode) {}

  public record Contribution(String statId, BigDecimal coefficient) {}

  public record Stat(
      String statId,
      String label,
      String unit,
      Kind kind,
      Support support,
      boolean eligible,
      String eligibilityReason,
      List<String> sourceStatIds,
      List<Contribution> contributions,
      List<String> sourceUrls) {}

  public enum Kind {
    EXPLICIT,
    IMPLICIT,
    PSEUDO
  }

  /** Exact rational conversion from one raw stat, scoped to its modifier layer. */
  public record Mapping(String sourceStatId, String layer, long numerator, long denominator) {
    public Mapping {
      if (denominator <= 0)
        throw new IllegalArgumentException("Positive scale denominator required");
    }
  }
}
