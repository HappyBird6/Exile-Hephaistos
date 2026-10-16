package com.poe2craft.crafting.domain;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.RollPartition;
import com.poe2craft.item.*;
import java.math.BigInteger;
import java.util.*;

/**
 * Finite primitive laws under declared scalar roll assumptions, never sampled game probabilities.
 */
public final class AutomaticCraftingKernel {
  public static final String VERSION = "automatic-scalar-kernel-v4-roll-cells";

  public record Identity(
      String kernelVersion,
      String policyVersion,
      String catalogDigest,
      String family,
      String rollPartition,
      WorkbenchSimulator.AutomaticIdentity model) {}

  public Identity identity() {
    return identity;
  }

  public enum Status {
    COMPLETE,
    PARTIAL,
    UNAVAILABLE,
    UNSUPPORTED,
    POLICY_REJECTED
  }

  public record Outcome(ItemState item, Fraction probability) {}

  public record Result(
      Status status,
      String reason,
      List<Outcome> outcomes,
      Fraction boundary,
      Fraction unresolved,
      List<WorkbenchSimulator.Assumption> assumptions) {
    public Result {
      outcomes = List.copyOf(outcomes);
      assumptions = List.copyOf(assumptions);
      var total = boundary.add(unresolved);
      for (var outcome : outcomes) total = total.add(outcome.probability());
      if (!total.equals(Fraction.ONE))
        throw new IllegalArgumentException("Primitive mass must equal one");
    }
  }

  private final AutomaticCurrencyPolicy policy;
  private final ItemCatalog catalog;
  private final WorkbenchSimulator simulator;
  private final String family;
  private final Identity identity;
  private final RollPartition partition;

  public record RollBand(String stat, long min, long max) {}

  public Map<String, RollBand> rollBands(ItemState item) {
    var result = new LinkedHashMap<String, RollBand>();
    for (var modifier : item.explicits()) {
      var definition = catalog.modifiers().get(modifier.modifierId());
      if (definition == null || definition.stats().size() != 1) continue;
      var stat = definition.stats().getFirst().id();
      var value = modifier.values().get(stat);
      if (value == null) continue;
      long end = partition.end(definition, value);
      if (end > value) result.put(modifier.modifierId(), new RollBand(stat, value, end));
    }
    return Map.copyOf(result);
  }

  public AutomaticCraftingKernel withRollPartition(RollPartition partition, String objective) {
    return new AutomaticCraftingKernel(
        policy, catalog, simulator, family, partition, RollPartition.VERSION + ":" + objective);
  }

  public AutomaticCraftingKernel(
      AutomaticCurrencyPolicy policy,
      ItemCatalog catalog,
      WorkbenchSimulator simulator,
      String family) {
    this(policy, catalog, simulator, family, RollPartition.exact(), "exact");
  }

  private AutomaticCraftingKernel(
      AutomaticCurrencyPolicy policy,
      ItemCatalog catalog,
      WorkbenchSimulator simulator,
      String family,
      RollPartition partition,
      String partitionIdentity) {
    this.policy = Objects.requireNonNull(policy);
    this.catalog = Objects.requireNonNull(catalog);
    this.simulator = Objects.requireNonNull(simulator);
    this.family = Objects.requireNonNull(family);
    this.partition = Objects.requireNonNull(partition);
    identity =
        new Identity(
            VERSION,
            policy.document().policyVersion(),
            BasicCurrencyState.catalogDigest(catalog),
            family,
            partitionIdentity,
            simulator.automaticIdentity());
  }

  public Result expand(ItemState state, AutomaticCraftingAction execution, long outcomeBudget) {
    if (outcomeBudget < 1) throw new IllegalArgumentException("Positive outcome budget required");
    var context =
        new AutomaticCurrencyPolicy.Context(family, execution.purpose(), execution.currency());
    if (policy.evaluate(execution.currency().name(), context)
            != AutomaticCurrencyPolicy.Verdict.PERMITTED
        || execution.omens().stream()
            .anyMatch(
                o ->
                    policy.evaluate("OMEN:" + o.name(), context)
                        != AutomaticCurrencyPolicy.Verdict.PERMITTED))
      return blocked(Status.POLICY_REJECTED, "AUTOMATIC_POLICY_REJECTED");
    if (!policy.withinExplorationBoundary(state.explicits().size()))
      return new Result(
          Status.PARTIAL,
          "EXPLICIT_MODIFIER_BOUNDARY",
          List.of(),
          Fraction.ONE,
          Fraction.ZERO,
          List.of());
    if (execution.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT) {
      if (execution.currency() != WorkbenchCurrency.ESSENCE_BREACH || !execution.omens().isEmpty())
        return blocked(Status.POLICY_REJECTED, "INVALID_QUALITY_WORK");
      if (!hasQualityCap(state))
        return blocked(Status.UNAVAILABLE, "QUALITY_40_REQUIRES_MAXIMUM_QUALITY_MODIFIER");
      var limit = QualityLimitRules.describe(state, catalog);
      if (limit == null || limit.maximumQuality() < 40)
        return blocked(Status.UNSUPPORTED, "QUALITY_40_CAP_NOT_VERIFIED");
      return new Result(
          Status.COMPLETE,
          "",
          List.of(new Outcome(state, Fraction.ONE)),
          Fraction.ZERO,
          Fraction.ZERO,
          List.of(
              new WorkbenchSimulator.Assumption(
                  "abstract-quality-work-v1",
                  "quality checkpoint",
                  1,
                  List.of(),
                  null,
                  null,
                  "https://poe2db.tw/us/Essence_of_the_Breach",
                  "User-approved abstract work to 40 quality; catalyst type, count and numeric scaling are not inferred.")));
    }
    var plan = simulator.automaticPlan(state, execution);
    if (plan.status() != AutomaticCraftingPlan.Status.READY)
      return blocked(
          plan.status() == AutomaticCraftingPlan.Status.UNAVAILABLE
              ? Status.UNAVAILABLE
              : Status.UNSUPPORTED,
          plan.reason());
    boolean uniform =
        plan.uniformCandidates()
            || Set.of(
                    "UNVERIFIED_GAME_WEIGHTS_EXPLICIT_UNIFORM_ELIGIBLE_CANDIDATES",
                    "USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS")
                .contains(catalog.metadata().weightPolicy());
    if (!uniform && !catalog.metadata().weightPolicy().equals("POE2DB_AS_PUBLISHED"))
      return blocked(Status.UNSUPPORTED, "SELECTION_WEIGHT_MODEL_NOT_IMPLEMENTED");
    // Unsupported rolls retain their selection mass in the same eligible denominator.
    String unsupportedRoll = "";
    for (var branch : plan.branches()) {
      if (branch.candidates().isEmpty()) return blocked(Status.UNSUPPORTED, "EMPTY_CANDIDATE_LAW");
      for (var candidate : branch.candidates()) {
        if (candidate.stats().size() != 1) {
          unsupportedRoll = "JOINT_ROLL_MODEL_NOT_IMPLEMENTED";
          continue;
        }
        var range = candidate.stats().getFirst();
        if (BigInteger.valueOf(range.max())
                .subtract(BigInteger.valueOf(range.min()))
                .add(BigInteger.ONE)
                .compareTo(BigInteger.valueOf(Long.MAX_VALUE))
            > 0) unsupportedRoll = "ROLL_DOMAIN_NOT_IMPLEMENTED";
      }
    }
    if (plan.branches().isEmpty()) return blocked(Status.UNAVAILABLE, "NO_REMOVAL_CANDIDATE");
    var outcomes = new LinkedHashMap<ItemState, Fraction>();
    var assumptions = new LinkedHashSet<WorkbenchSimulator.Assumption>();
    var boundary = Fraction.ZERO;
    var emitted = Fraction.ZERO;
    long count = 0;
    var branchMass = Fraction.of(1, plan.branches().size());
    if (plan.branches().size() > 1)
      assumptions.add(
          new WorkbenchSimulator.Assumption(
              "uniform-removal-v1",
              "eligible explicit instance",
              plan.branches().size(),
              List.of(),
              null,
              null,
              plan.sourceUrl(),
              "Uniform over every eligible removal, after the consumed omen restriction; no published removal weights."));
    for (var branch : plan.branches()) {
      long total = 0;
      for (var candidate : branch.candidates())
        total = Math.addExact(total, uniform ? 1 : candidate.weight());
      if (total <= 0) return blocked(Status.UNSUPPORTED, "INVALID_SELECTION_WEIGHT");
      if (uniform)
        assumptions.add(
            new WorkbenchSimulator.Assumption(
                "uniform-individual-candidate-v1",
                "eligible individual modifier tier",
                branch.candidates().size(),
                branch.candidates().stream().map(ModifierDefinition::id).toList(),
                null,
                null,
                plan.sourceUrl(),
                plan.uniformCandidates()
                    ? "Fixed sourced outcomes use 1/n: ordinary spawn weight is not a fixed-material selection weight."
                    : "Catalog explicitly declares uniform eligible modifier-tier candidates; game weights are unavailable."));
      for (var candidate : branch.candidates()) {
        if (candidate.stats().size() != 1) continue;
        var range = candidate.stats().getFirst();
        var size =
            BigInteger.valueOf(range.max())
                .subtract(BigInteger.valueOf(range.min()))
                .add(BigInteger.ONE);
        if (size.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) continue;
        var selection = branchMass.multiply(Fraction.of(uniform ? 1 : candidate.weight(), total));
        var roll = simulator.singleStatRollAssumption(candidate);
        if (roll != null) assumptions.add(roll);
        if (!policy.withinExplorationBoundary(branch.remaining().explicits().size() + 1)) {
          boundary = boundary.add(selection);
          emitted = emitted.add(selection);
          continue;
        }
        var mass = selection.multiply(new Fraction(BigInteger.ONE, size));
        for (long value = range.min(); ; ) {
          if (count >= outcomeBudget)
            return result(outcomes, boundary, emitted, assumptions, "OUTCOME_BUDGET");
          var explicits = new ArrayList<>(branch.remaining().explicits());
          explicits.add(new ModifierInstance(candidate.id(), Map.of(range.id(), value)));
          var item = simulator.finishBasicCurrency(branch.remaining(), explicits);
          long end = partition.end(candidate, value);
          var cellMass =
              mass.multiply(
                  new Fraction(
                      BigInteger.valueOf(end)
                          .subtract(BigInteger.valueOf(value))
                          .add(BigInteger.ONE),
                      BigInteger.ONE));
          outcomes.merge(item, cellMass, Fraction::add);
          emitted = emitted.add(cellMass);
          count++;
          if (end == range.max()) break;
          value = end + 1;
        }
      }
    }
    return result(
        outcomes,
        boundary,
        emitted,
        assumptions,
        !unsupportedRoll.isEmpty()
            ? unsupportedRoll
            : boundary.equals(Fraction.ZERO) ? "" : "EXPLICIT_MODIFIER_BOUNDARY");
  }

  private static Result result(
      Map<ItemState, Fraction> outcomes,
      Fraction boundary,
      Fraction emitted,
      Set<WorkbenchSimulator.Assumption> assumptions,
      String reason) {
    var unresolved =
        new Fraction(emitted.denominator().subtract(emitted.numerator()), emitted.denominator());
    return new Result(
        unresolved.equals(Fraction.ZERO) && boundary.equals(Fraction.ZERO)
            ? Status.COMPLETE
            : Status.PARTIAL,
        reason,
        outcomes.entrySet().stream().map(e -> new Outcome(e.getKey(), e.getValue())).toList(),
        boundary,
        unresolved,
        List.copyOf(assumptions));
  }

  private static Result blocked(Status status, String reason) {
    return new Result(status, reason, List.of(), Fraction.ZERO, Fraction.ONE, List.of());
  }

  public static boolean hasQualityCap(ItemState item) {
    return item.explicits().stream()
        .anyMatch(
            m ->
                m.modifierId().equals(QualityLimitRules.BREACH_ID)
                    && Long.valueOf(20).equals(m.values().get(QualityLimitRules.STAT_ID)));
  }
}
