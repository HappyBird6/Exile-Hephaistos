package com.poe2craft.crafting.application.goalfilter;

import com.poe2craft.crafting.domain.WorkbenchOmen;
import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import com.poe2craft.item.ItemState;
import com.poe2craft.item.ItemStateValidator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Coordinates validation and exact observations; numeric probability remains honestly gated. */
public final class GoalFilterService {
  private final GoalCatalogIndex index;
  private final GoalValidator validator = new GoalValidator();
  private final GoalEvaluator evaluator = new GoalEvaluator();
  private final ItemStatProjection projection = new ItemStatProjection();

  public GoalFilterService(GoalCatalogIndex index) {
    this.index = index;
  }

  public GoalCatalog catalog(Context context) {
    requireContext(context);
    return index.catalog(context);
  }

  public GoalValidator.Validation validate(Context context, GoalFilter goal) {
    requireContext(context);
    var result =
        validator.validate(context, goal, index.catalog(context), index.knownStats(context));
    if (!result.valid()) throw new InvalidGoal(result.issues());
    return result;
  }

  public GoalEvaluator.Evaluation evaluate(ItemState item, GoalFilter goal) {
    if (item == null)
      throw new InvalidGoal(
          List.of(Issue.error("INVALID_ITEM", "/item", "ItemState is required.")));
    var context = new Context(item.snapshotId(), item.baseItemId(), item.itemLevel());
    var validated = validate(context, goal);
    var issues = new ArrayList<>(validated.issues());
    var itemCatalog = index.itemCatalog(context).orElse(null);
    boolean supported = itemCatalog != null;
    if (itemCatalog != null) {
      var violations = new ItemStateValidator(itemCatalog).validate(item);
      for (var violation : violations) {
        if (violation.code() == ItemStateValidator.Code.UNSUPPORTED_STATE) {
          supported = false;
          issues.add(
              Issue.warning(
                  "UNSUPPORTED_ITEM_EFFECT",
                  "/item",
                  "Special conditions or quality effects need a reviewed numeric projection."));
        } else issues.add(Issue.error(violation.code().name(), "/item", violation.message()));
      }
    }
    // Quality can change displayed values. Never assume its source values are final display values.
    if (item.catalystQuality() != null) {
      supported = false;
      issues.add(
          Issue.warning(
              "QUALITY_PROJECTION_NOT_IMPLEMENTED",
              "/item/catalystQuality",
              "Quality scaling is not implemented by this numeric filter."));
    }
    if (issues.stream().anyMatch(i -> i.severity() == Severity.ERROR))
      throw new InvalidGoal(issues);
    var observations = projection.project(item, index.knownStats(context), supported);
    for (int i = 0; i < goal.groups().size(); i++) {
      var group = goal.groups().get(i);
      if (group.disabled()) continue;
      for (int j = 0; j < group.entries().size(); j++) {
        var entry = group.entries().get(j);
        if (entry.disabled()) continue;
        var observation = observations.get(entry.statId());
        if (observation != null && observation.support() != Capability.SUPPORTED)
          issues.add(
              Issue.warning(
                  observation.support() == Capability.UNSUPPORTED
                      ? "UNSUPPORTED_STAT_VALUE"
                      : "UNKNOWN_STAT_VALUE",
                  "/goal/groups/" + i + "/entries/" + j,
                  "The stat value cannot be established from this item."));
      }
    }
    var general = goal.general();
    var generalStatus =
        general.itemLevel().contains(BigDecimal.valueOf(item.itemLevel()))
                && general.rarities().contains(item.rarity())
            ? Status.MATCH
            : Status.NO_MATCH;
    var result = evaluator.evaluate(goal, generalStatus, observations, issues);
    if (supported) return result;
    return new GoalEvaluator.Evaluation(
        result.version(),
        result.catalogVersion(),
        GoalEvaluator.and(List.of(result.status(), Status.UNSUPPORTED)),
        result.generalStatus(),
        result.groups(),
        result.issues());
  }

  public record Limits(Integer maxStates, Integer maxEdges, Integer maxMillis) {}

  public record Probability(
      String status, String reasonCode, String modelVersion, String ledgerVersion) {}

  public record Recommendation(
      int version,
      String catalogVersion,
      Status evaluation,
      Probability probability,
      List<Object> comparisons,
      boolean rankingCertified,
      int comparedSequences,
      Integer totalSequences) {}

  public Recommendation recommend(
      ItemState item, GoalFilter goal, Set<String> activeOmens, Limits limits) {
    if (activeOmens == null)
      throw new InvalidGoal(
          List.of(Issue.error("INVALID_OMENS", "/activeOmens", "An omen set is required.")));
    if (limits == null
        || limits.maxStates() == null
        || limits.maxEdges() == null
        || limits.maxMillis() == null
        || limits.maxStates() < 1
        || limits.maxEdges() < 1
        || limits.maxMillis() < 1)
      throw new InvalidGoal(
          List.of(
              Issue.error(
                  "INVALID_LIMITS", "/limits", "Positive exploration limits are required.")));
    for (var omen : activeOmens) {
      try {
        WorkbenchOmen.fromId(omen);
      } catch (IllegalArgumentException error) {
        throw new InvalidGoal(
            List.of(
                Issue.error("UNKNOWN_OMEN", "/activeOmens", "Provide known Workbench omen IDs.")));
      }
    }
    var evaluation = evaluate(item, goal);
    return new Recommendation(
        1,
        goal.catalogVersion(),
        evaluation.status(),
        new Probability("UNSUPPORTED", "NUMERIC_DISTRIBUTION_NOT_IMPLEMENTED", null, null),
        List.of(),
        false,
        0,
        null);
  }

  private static void requireContext(Context context) {
    var issues = GoalValidator.contextIssues(context);
    if (!issues.isEmpty()) throw new InvalidGoal(issues);
  }

  public static final class InvalidGoal extends IllegalArgumentException {
    private final List<Issue> issues;

    public InvalidGoal(List<Issue> issues) {
      super("Invalid goal filter request");
      this.issues = List.copyOf(issues);
    }

    public List<Issue> issues() {
      return issues;
    }

    public boolean staleCatalog() {
      return issues.stream().anyMatch(i -> i.code().equals("CATALOG_VERSION_MISMATCH"));
    }
  }
}
