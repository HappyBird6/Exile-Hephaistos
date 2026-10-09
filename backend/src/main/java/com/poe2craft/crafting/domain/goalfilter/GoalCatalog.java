package com.poe2craft.crafting.domain.goalfilter;

import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import java.math.BigDecimal;
import java.util.List;

/** Display units and source scales are explicit; production IDs are local, never GGG IDs. */
public record GoalCatalog(
    int version,
    String catalogVersion,
    Context context,
    List<GroupSupport> groupTypes,
    List<Stat> stats,
    List<Issue> issues) {
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
