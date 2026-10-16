package com.poe2craft.crafting.domain;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.item.*;
import java.math.BigInteger;
import java.util.*;

/**
 * Finite primitive laws under declared scalar roll assumptions, never sampled game probabilities.
 */
public final class AutomaticCraftingKernel {
  public static final String VERSION = "automatic-scalar-kernel-v1";

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

  public AutomaticCraftingKernel(
      AutomaticCurrencyPolicy policy,
      ItemCatalog catalog,
      WorkbenchSimulator simulator,
      String family) {
    this.policy = Objects.requireNonNull(policy);
    this.catalog = Objects.requireNonNull(catalog);
    this.simulator = Objects.requireNonNull(simulator);
    this.family = Objects.requireNonNull(family);
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
    // Preflight the entire law; unsupported joint rolls never disappear from a denominator.
    for (var branch : plan.branches()) {
      if (branch.candidates().isEmpty()) return blocked(Status.UNSUPPORTED, "EMPTY_CANDIDATE_LAW");
      for (var candidate : branch.candidates())
        if (candidate.stats().size() != 1)
          return blocked(Status.UNSUPPORTED, "JOINT_ROLL_MODEL_NOT_IMPLEMENTED");
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
        var range = candidate.stats().getFirst();
        var size =
            BigInteger.valueOf(range.max())
                .subtract(BigInteger.valueOf(range.min()))
                .add(BigInteger.ONE);
        var selection = branchMass.multiply(Fraction.of(uniform ? 1 : candidate.weight(), total));
        var roll = simulator.singleStatRollAssumption(candidate);
        if (roll != null) assumptions.add(roll);
        if (!policy.withinExplorationBoundary(branch.remaining().explicits().size() + 1)) {
          boundary = boundary.add(selection);
          emitted = emitted.add(selection);
          continue;
        }
        var mass = selection.multiply(new Fraction(BigInteger.ONE, size));
        for (long value = range.min(); ; value++) {
          if (count >= outcomeBudget)
            return result(outcomes, boundary, emitted, assumptions, "OUTCOME_BUDGET");
          var explicits = new ArrayList<>(branch.remaining().explicits());
          explicits.add(new ModifierInstance(candidate.id(), Map.of(range.id(), value)));
          var item = simulator.finishBasicCurrency(branch.remaining(), explicits);
          outcomes.merge(item, mass, Fraction::add);
          emitted = emitted.add(mass);
          count++;
          if (value == range.max()) break;
        }
      }
    }
    return result(
        outcomes,
        boundary,
        emitted,
        assumptions,
        boundary.equals(Fraction.ZERO) ? "" : "EXPLICIT_MODIFIER_BOUNDARY");
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
}
