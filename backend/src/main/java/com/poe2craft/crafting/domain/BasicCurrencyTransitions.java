package com.poe2craft.crafting.domain;

import com.poe2craft.crafting.domain.BasicCurrencyState.Provenance;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.item.*;
import java.math.BigInteger;
import java.util.*;

/** Exhaustive finite distribution under declared assumptions, never verified game roll odds. */
public final class BasicCurrencyTransitions {
  public static final String MODEL_VERSION = "solar-basic-transition-v1";
  public static final String INTERPRETATION = "DECLARED_MODEL_NOT_VERIFIED_GAME_PROBABILITY";
  private final ItemCatalog catalog;
  private final WorkbenchSimulator simulator;
  private final Provenance provenance;

  public BasicCurrencyTransitions(ItemCatalog catalog, String rulesetIdentity) {
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    if (rulesetIdentity == null || rulesetIdentity.isBlank())
      throw new IllegalArgumentException("Explicit ruleset identity is required");
    simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var m = catalog.metadata();
    provenance =
        new Provenance(
            rulesetIdentity,
            BasicCurrencyState.catalogDigest(catalog),
            MODEL_VERSION,
            simulator.ruleVersion(),
            simulator.ledgerVersion(),
            m.weightPolicy(),
            m.sourceUrl(),
            m.retrievedAt(),
            m.rawSha256(),
            m.detailsSha256());
  }

  public static List<WorkbenchCurrency> supportedActions() {
    return BasicCurrencyPlan.ACTIONS;
  }

  public Provenance provenance() {
    return provenance;
  }

  public record ChaosRenewal(
      BasicCurrencyState emptyState,
      String targetModifierId,
      Fraction probability,
      List<String> eligibleModifierIds,
      String proofVersion) {}

  /**
   * Narrow renewal proof for this sealed model. Every supported roll creates exactly one unlocked
   * explicit. Removal then restores the identical full empty state, so the next pool is identical.
   * finishBasicCurrency copies nonexplicit fields; the current PRESERVE_EXISTING policy is identity
   * for every value, not an interpolation inferred from sampled endpoints.
   */
  public Optional<ChaosRenewal> proveSingleExplicitChaos(BasicCurrencyState source, String target) {
    if (source == null || !provenance.equals(source.provenance()))
      throw new IllegalArgumentException("Current transition provenance required");
    var item = source.item();
    if (item.rarity() != ItemState.Rarity.RARE
        || item.explicits().size() != 1
        || item.explicits().getFirst().fractured()
        || !item.conditions().isEmpty()
        || QualityCapChangePolicy.DEFAULT != QualityCapChangePolicy.PRESERVE_EXISTING)
      return Optional.empty();
    // The normal exhaustive transition preflights ALL candidates, including joint/oversize rolls.
    // A one-outcome budget limits emission only; it never prunes the support check or denominator.
    var supported = expand(source, WorkbenchCurrency.CHAOS, Set.of(), 1);
    if (supported.status() == Status.UNSUPPORTED || supported.status() == Status.UNAVAILABLE)
      return Optional.empty();
    var plan = simulator.basicCurrencyPlan(item, WorkbenchCurrency.CHAOS);
    if (plan.branches().size() != 1) return Optional.empty();
    var branch = plan.branches().getFirst();
    var expectedEmpty =
        new ItemState(
            item.snapshotId(),
            item.baseItemId(),
            item.itemLevel(),
            item.rarity(),
            item.implicits(),
            List.of(),
            item.conditions(),
            item.augmentSockets(),
            item.catalystQuality());
    if (!branch.remaining().equals(expectedEmpty)) return Optional.empty();
    long total = 0, selected = 0;
    for (var candidate : branch.candidates()) {
      var range = candidate.stats().getFirst();
      for (long value : new long[] {range.min(), range.max()}) {
        var instance = new ModifierInstance(candidate.id(), Map.of(range.id(), value));
        var finished = simulator.finishBasicCurrency(expectedEmpty, List.of(instance));
        var expected =
            new ItemState(
                item.snapshotId(),
                item.baseItemId(),
                item.itemLevel(),
                item.rarity(),
                item.implicits(),
                List.of(instance),
                item.conditions(),
                item.augmentSockets(),
                item.catalystQuality());
        if (!finished.equals(expected)) return Optional.empty();
        var next = simulator.basicCurrencyPlan(finished, WorkbenchCurrency.CHAOS);
        if (!next.available()
            || next.branches().size() != 1
            || !next.branches().getFirst().remaining().equals(expectedEmpty)
            || !next.branches().getFirst().candidates().equals(branch.candidates()))
          return Optional.empty();
      }
      total = Math.addExact(total, candidate.weight());
      if (candidate.id().equals(target)) selected = Math.addExact(selected, candidate.weight());
    }
    return Optional.of(
        new ChaosRenewal(
            new BasicCurrencyState(expectedEmpty, provenance),
            target,
            Fraction.of(selected, total),
            branch.candidates().stream().map(ModifierDefinition::id).toList(),
            "single-explicit-plain-chaos-full-empty-v1"));
  }

  /** Explicit provenance supplied by the caller is checked; never stamp an old item as current. */
  public Result expand(
      BasicCurrencyState source,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      long maxElementaryOutcomes) {
    if (source == null || action == null || activeOmens == null || maxElementaryOutcomes < 1)
      throw new IllegalArgumentException(
          "State, action, explicit omen set and positive limit required");
    if (!provenance.equals(source.provenance()))
      throw new IllegalArgumentException(
          "Ruleset, catalog or transition model provenance mismatch");
    if (!supportedActions().contains(action))
      return blocked(source, action, Status.UNSUPPORTED, "ACTION_NOT_IMPLEMENTED");
    if (!activeOmens.isEmpty())
      return blocked(source, action, Status.UNSUPPORTED, "OMEN_MODEL_NOT_IMPLEMENTED");
    if (!SolarAmulet.BASE_ID.equals(catalog.base().id()))
      return blocked(source, action, Status.UNSUPPORTED, "NUMERIC_BASE_NOT_IMPLEMENTED");
    if (!"POE2DB_AS_PUBLISHED".equals(catalog.metadata().weightPolicy()))
      return blocked(source, action, Status.UNSUPPORTED, "SELECTION_WEIGHT_MODEL_NOT_IMPLEMENTED");
    if (!source.item().conditions().isEmpty() || source.item().rarity() == ItemState.Rarity.UNIQUE)
      return blocked(source, action, Status.UNSUPPORTED, "SPECIAL_ITEM_STATE_NOT_IMPLEMENTED");
    var plan = simulator.basicCurrencyPlan(source.item(), action);
    if (!plan.available()) return blocked(source, action, Status.UNAVAILABLE, plan.reason());
    // Preflight every positive-probability branch before emitting anything. No denominator pruning.
    for (var branch : plan.branches()) {
      for (var candidate : branch.candidates()) {
        if (candidate.stats().size() != 1)
          return blocked(source, action, Status.UNSUPPORTED, "JOINT_ROLL_MODEL_NOT_IMPLEMENTED");
        var range = candidate.stats().getFirst();
        // The shared Workbench model uses signed-long inclusive range sizes. Do not invent a wider
        // model.
        if (rangeSize(range).compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0)
          return blocked(source, action, Status.UNSUPPORTED, "ROLL_DOMAIN_NOT_IMPLEMENTED");
      }
    }
    var outcomes = new LinkedHashMap<BasicCurrencyState, Fraction>();
    var assumptions = new LinkedHashSet<WorkbenchSimulator.Assumption>();
    var emitted = Fraction.ZERO;
    long expanded = 0;
    var removalMass = Fraction.of(1, plan.branches().size());
    if (action.baseAction() == CraftingAction.ANNULMENT
        || action.baseAction() == CraftingAction.CHAOS)
      assumptions.add(simulator.basicRemovalAssumption(plan.removals(), action));
    for (var branch : plan.branches()) {
      if (action == WorkbenchCurrency.ANNULMENT) {
        if (expanded >= maxElementaryOutcomes)
          return result(source, action, outcomes, emitted, assumptions, expanded);
        var target = new BasicCurrencyState(branch.remaining(), provenance);
        outcomes.merge(target, removalMass, Fraction::add);
        emitted = emitted.add(removalMass);
        expanded++;
        continue;
      }
      assumptions.addAll(simulator.basicSelectionAssumptions(branch.candidates()));
      long total = 0;
      for (var candidate : branch.candidates()) total = Math.addExact(total, candidate.weight());
      for (var candidate : branch.candidates()) {
        var range = candidate.stats().getFirst();
        var mass =
            removalMass.multiply(
                new Fraction(
                    BigInteger.valueOf(candidate.weight()),
                    BigInteger.valueOf(total).multiply(rangeSize(range))));
        var rollAssumption = simulator.singleStatRollAssumption(candidate);
        if (rollAssumption != null) assumptions.add(rollAssumption);
        for (long value = range.min(); ; value++) {
          if (expanded >= maxElementaryOutcomes)
            return result(source, action, outcomes, emitted, assumptions, expanded);
          var explicits = new ArrayList<>(branch.remaining().explicits());
          explicits.add(new ModifierInstance(candidate.id(), Map.of(range.id(), value)));
          var item = simulator.finishBasicCurrency(branch.remaining(), explicits);
          outcomes.merge(new BasicCurrencyState(item, provenance), mass, Fraction::add);
          emitted = emitted.add(mass);
          expanded++;
          if (value == range.max()) break;
        }
      }
    }
    if (!emitted.equals(Fraction.ONE))
      throw new IllegalStateException("Invalid basic transition mass");
    return result(source, action, outcomes, emitted, assumptions, expanded);
  }

  private static BigInteger rangeSize(ModifierDefinition.StatRange range) {
    return BigInteger.valueOf(range.max())
        .subtract(BigInteger.valueOf(range.min()))
        .add(BigInteger.ONE);
  }

  private static Result result(
      BasicCurrencyState source,
      WorkbenchCurrency action,
      Map<BasicCurrencyState, Fraction> outcomes,
      Fraction emitted,
      Set<WorkbenchSimulator.Assumption> assumptions,
      long expanded) {
    var unresolved =
        new Fraction(emitted.denominator().subtract(emitted.numerator()), emitted.denominator());
    boolean complete = unresolved.equals(Fraction.ZERO);
    return new Result(
        complete ? Status.COMPLETE : Status.PARTIAL,
        complete ? "" : "ELEMENTARY_OUTCOME_LIMIT",
        source,
        action,
        outcomes.entrySet().stream().map(e -> new Outcome(e.getKey(), e.getValue())).toList(),
        unresolved,
        List.copyOf(assumptions),
        INTERPRETATION,
        expanded);
  }

  private static Result blocked(
      BasicCurrencyState source, WorkbenchCurrency action, Status status, String reason) {
    return new Result(
        status, reason, source, action, List.of(), Fraction.ONE, List.of(), INTERPRETATION, 0);
  }

  public enum Status {
    COMPLETE,
    PARTIAL,
    UNAVAILABLE,
    UNSUPPORTED
  }

  public record Outcome(BasicCurrencyState state, Fraction probability) {}

  public record Result(
      Status status,
      String reason,
      BasicCurrencyState source,
      WorkbenchCurrency action,
      List<Outcome> outcomes,
      Fraction unresolved,
      List<WorkbenchSimulator.Assumption> assumptions,
      String interpretation,
      long elementaryOutcomes) {
    public Result {
      outcomes = List.copyOf(outcomes);
      assumptions = List.copyOf(assumptions);
    }
  }
}
