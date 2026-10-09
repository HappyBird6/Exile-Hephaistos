package com.poe2craft.crafting.domain.goalfilter;

import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import java.math.BigDecimal;
import java.util.*;

/** Structural validation also covers disabled content; support warnings do not erase it. */
public final class GoalValidator {
  public record Capabilities(Capability evaluation, Capability probability) {}

  public record Validation(
      int version, boolean valid, List<Issue> issues, Capabilities capabilities) {}

  public Validation validate(
      Context context, GoalFilter goal, GoalCatalog catalog, Map<String, GoalCatalog.Stat> known) {
    var issues = new ArrayList<Issue>();
    Capability evaluation = Capability.SUPPORTED;
    if (goal == null)
      return result(
          List.of(Issue.error("INVALID_STRUCTURE", "/goal", "Goal is required.")),
          Capability.UNSUPPORTED);
    if (!Integer.valueOf(1).equals(goal.version()))
      issues.add(
          Issue.error("UNSUPPORTED_VERSION", "/goal/version", "Only version 1 is supported."));
    if (!catalog.catalogVersion().equals(goal.catalogVersion()))
      issues.add(
          Issue.error(
              "CATALOG_VERSION_MISMATCH",
              "/goal/catalogVersion",
              "Refresh the catalog before submitting this goal."));
    var general = goal.general();
    if (general == null)
      issues.add(
          Issue.error("INVALID_STRUCTURE", "/goal/general", "General filters are required."));
    else {
      if (general.baseItemId() == null || general.baseItemId().isBlank())
        issues.add(
            Issue.error(
                "INVALID_STRUCTURE", "/goal/general/baseItemId", "Base identity is required."));
      if (!Objects.equals(context.baseItemId(), general.baseItemId()))
        issues.add(
            Issue.error(
                "CONTEXT_BASE_MISMATCH",
                "/goal/general/baseItemId",
                "Goal and request base must match."));
      range(general.itemLevel(), "/goal/general/itemLevel", issues);
      if (general.itemLevel() != null
          && (invalidLevel(general.itemLevel().min()) || invalidLevel(general.itemLevel().max())))
        issues.add(
            Issue.error(
                "INVALID_ITEM_LEVEL_RANGE",
                "/goal/general/itemLevel",
                "Item level bounds must be integers between 1 and 100."));
      if (general.rarities() == null
          || general.rarities().isEmpty()
          || general.rarities().stream().anyMatch(Objects::isNull)
          || new HashSet<>(general.rarities()).size() != general.rarities().size())
        issues.add(
            Issue.error(
                "INVALID_RARITIES",
                "/goal/general/rarities",
                "Provide distinct supported rarity values."));
    }
    if (goal.groups() == null)
      issues.add(Issue.error("INVALID_STRUCTURE", "/goal/groups", "Groups are required."));
    else {
      var groupIds = new HashSet<String>();
      var entryIds = new HashSet<String>();
      int active = 0;
      for (int i = 0; i < goal.groups().size(); i++) {
        String path = "/goal/groups/" + i;
        var group = goal.groups().get(i);
        if (group == null) {
          issues.add(Issue.error("INVALID_STRUCTURE", path, "Group is required."));
          continue;
        }
        identity(group.id(), groupIds, path + "/id", issues);
        if (group.disabled() == null || group.type() == null)
          issues.add(
              Issue.error("INVALID_STRUCTURE", path, "Group type and disabled flag are required."));
        boolean enabled = Boolean.FALSE.equals(group.disabled());
        if (enabled) active++;
        boolean weighted = group.type() == Type.WEIGHTED_V1 || group.type() == Type.WEIGHTED_V2;
        if (weighted || group.type() == Type.COUNT) range(group.range(), path + "/range", issues);
        else if (group.range() != null)
          issues.add(
              Issue.error("UNEXPECTED_RANGE", path + "/range", "This group has no group range."));
        if (group.entries() == null) {
          issues.add(Issue.error("INVALID_STRUCTURE", path + "/entries", "Entries are required."));
          continue;
        }
        var statIds = new HashSet<String>();
        int activeRows = 0;
        for (int j = 0; j < group.entries().size(); j++) {
          var entry = group.entries().get(j);
          String row = path + "/entries/" + j;
          if (entry == null) {
            issues.add(Issue.error("INVALID_STRUCTURE", row, "Entry is required."));
            continue;
          }
          identity(entry.id(), entryIds, row + "/id", issues);
          if (entry.statId() == null
              || entry.statId().isBlank()
              || entry.unit() == null
              || entry.unit().isBlank())
            issues.add(
                Issue.error(
                    "INVALID_STRUCTURE",
                    row,
                    "Stat identity and unit are required, including for disabled entries."));
          if (entry.disabled() == null)
            issues.add(
                Issue.error("INVALID_STRUCTURE", row + "/disabled", "Disabled flag is required."));
          boolean enabledRow = enabled && Boolean.FALSE.equals(entry.disabled());
          if (Boolean.FALSE.equals(entry.disabled())) activeRows++;
          if (!statIds.add(entry.statId()))
            issues.add(
                Issue.error(
                    "DUPLICATE_STAT", row + "/statId", "A stat may appear only once per group."));
          range(entry.range(), row + "/range", issues);
          if (weighted && entry.weight() == null)
            issues.add(
                Issue.error(
                    "MISSING_WEIGHT",
                    row + "/weight",
                    "Weighted entries require a finite weight."));
          if (!weighted && entry.weight() != null)
            issues.add(
                Issue.error(
                    "UNEXPECTED_WEIGHT",
                    row + "/weight",
                    "Weights are only allowed in weighted groups."));
          var stat = entry.statId() == null ? null : known.get(entry.statId());
          if (stat == null)
            issues.add(
                enabledRow
                    ? Issue.error("UNKNOWN_STAT", row + "/statId", "Unknown stat identity.")
                    : Issue.warning(
                        "UNKNOWN_STAT", row + "/statId", "Disabled stat identity is unknown."));
          else {
            if (!stat.unit().equals(entry.unit()))
              issues.add(
                  Issue.error(
                      "UNIT_MISMATCH", row + "/unit", "Entry unit must match the catalog."));
            if (!stat.eligible())
              issues.add(
                  Issue.warning(
                      "INELIGIBLE_STAT",
                      row + "/statId",
                      "No new generation candidate is eligible; existing values remain evaluable."));
            if (stat.support().evaluation() != Capability.SUPPORTED) {
              issues.add(
                  Issue.warning(
                      stat.support().reasonCode(),
                      row + "/statId",
                      "Stat unit or effect needs reviewed evidence."));
              if (enabledRow) evaluation = stat.support().evaluation();
            }
          }
        }
        if (enabled && activeRows == 0)
          issues.add(
              Issue.error(
                  "EMPTY_GROUP", path + "/entries", "An active group requires an active entry."));
        if (group.type() == Type.COUNT
            && group.range() != null
            && (invalidCountMinimum(group.range().min())
                || invalidCount(group.range().max(), activeRows)))
          issues.add(
              Issue.error(
                  "INVALID_COUNT_RANGE",
                  path + "/range",
                  "Count bounds must be nonnegative integers within the active entry count."));
      }
      if (active == 0)
        issues.add(Issue.error("EMPTY_GOAL", "/goal/groups", "At least one group must be active."));
    }
    if (!catalog.issues().isEmpty()) {
      issues.addAll(catalog.issues());
      evaluation = Capability.UNSUPPORTED;
    }
    return result(issues, evaluation);
  }

  public static List<Issue> contextIssues(Context context) {
    if (context == null
        || context.snapshotId() == null
        || context.snapshotId().isBlank()
        || context.baseItemId() == null
        || context.baseItemId().isBlank()
        || context.itemLevel() == null
        || context.itemLevel() < 1
        || context.itemLevel() > 100)
      return List.of(
          Issue.error(
              "INVALID_CONTEXT",
              "/context",
              "Snapshot, base and item level between 1 and 100 are required."));
    return List.of();
  }

  private static Validation result(List<Issue> issues, Capability evaluation) {
    return new Validation(
        1,
        issues.stream().noneMatch(i -> i.severity() == Severity.ERROR),
        List.copyOf(issues),
        new Capabilities(evaluation, Capability.UNSUPPORTED));
  }

  private static void identity(String value, Set<String> ids, String path, List<Issue> issues) {
    if (value == null || value.isBlank())
      issues.add(Issue.error("INVALID_ID", path, "A nonblank identity is required."));
    else if (!ids.add(value))
      issues.add(Issue.error("DUPLICATE_ID", path, "Identity appears more than once."));
  }

  private static void range(Range range, String path, List<Issue> issues) {
    if (range == null)
      issues.add(Issue.error("INVALID_RANGE", path, "A range object is required."));
    else if (range.min() != null && range.max() != null && range.min().compareTo(range.max()) > 0)
      issues.add(Issue.error("INVALID_RANGE", path, "Minimum cannot exceed maximum."));
  }

  private static boolean integer(BigDecimal value) {
    return value.stripTrailingZeros().scale() <= 0;
  }

  private static boolean invalidLevel(BigDecimal value) {
    return value != null
        && (!integer(value)
            || value.compareTo(BigDecimal.ONE) < 0
            || value.compareTo(BigDecimal.valueOf(100)) > 0);
  }

  private static boolean invalidCount(BigDecimal value, int max) {
    return value != null
        && (!integer(value) || value.signum() < 0 || value.compareTo(BigDecimal.valueOf(max)) > 0);
  }

  private static boolean invalidCountMinimum(BigDecimal value) {
    return value != null && (!integer(value) || value.signum() < 0);
  }
}
