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
  private final boolean reviewedCatalog;

  public static BasicCurrencyTransitions forMethodSearch(
      ItemCatalog catalog, String rulesetIdentity) {
    return new BasicCurrencyTransitions(
        catalog, rulesetIdentity, !SolarAmulet.BASE_ID.equals(catalog.base().id()));
  }

  public BasicCurrencyTransitions(ItemCatalog catalog, String rulesetIdentity) {
    this(catalog, rulesetIdentity, false);
  }

  private BasicCurrencyTransitions(
      ItemCatalog catalog, String rulesetIdentity, boolean reviewedCatalog) {
    this.reviewedCatalog = reviewedCatalog;
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    if (rulesetIdentity == null || rulesetIdentity.isBlank())
      throw new IllegalArgumentException("Explicit ruleset identity is required");
    simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var m = catalog.metadata();
    provenance =
        new Provenance(
            rulesetIdentity,
            BasicCurrencyState.catalogDigest(catalog),
            reviewedCatalog ? "reviewed-basic-transition-v2" : MODEL_VERSION,
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

  /**
   * The basic rules never downgrade rare items; all other starts keep the conservative full set.
   */
  public static List<WorkbenchCurrency> reachableActions(ItemState root) {
    return root.rarity() == ItemState.Rarity.RARE
        ? supportedActions().stream()
            .filter(
                action ->
                    WorkbenchSimulator.basicRarityAllows(
                        ItemState.Rarity.RARE, action.baseAction()))
            .toList()
        : supportedActions();
  }

  public Provenance provenance() {
    return provenance;
  }

  private boolean uniformSelection() {
    return Set.of(
            "UNVERIFIED_GAME_WEIGHTS_EXPLICIT_UNIFORM_ELIGIBLE_CANDIDATES",
            "USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS")
        .contains(catalog.metadata().weightPolicy());
  }

  private int selectionWeight(ModifierDefinition candidate) {
    return uniformSelection() ? 1 : candidate.weight();
  }

  private List<WorkbenchSimulator.Assumption> selectionAssumptions(
      List<ModifierDefinition> candidates) {
    if (!uniformSelection()) return simulator.basicSelectionAssumptions(candidates);
    return List.of(
        new WorkbenchSimulator.Assumption(
            "explicit-uniform-modifier-tier-v1",
            "eligible individual modifier tier",
            candidates.size(),
            candidates.stream().map(ModifierDefinition::id).toList(),
            null,
            null,
            catalog.metadata().sourceUrl(),
            "Catalog declares unavailable game weights: equal 1/n after eligibility, without family deduplication or removal of non-goal candidates. Numeric rolls are a separate assumption."));
  }

  /** Equal keys prove the complete next-item law, not equivalence of the input items. */
  public record KernelIdentity(
      Provenance provenance, WorkbenchCurrency action, List<KernelBranch> branches) {
    public KernelIdentity {
      branches = List.copyOf(branches);
    }
  }

  public record KernelBranch(ItemState remaining, List<ModifierDefinition> candidates) {
    public KernelBranch {
      candidates = List.copyOf(candidates);
    }
  }

  /** Full-state primitive stream. Preflight checks all candidates before any mass is emitted. */
  public PrimitiveCursor primitiveCursor(
      BasicCurrencyState source, WorkbenchCurrency action, Set<String> activeOmens) {
    return new PrimitiveCursor(source, action, activeOmens);
  }

  public final class PrimitiveCursor {
    private final WorkbenchCurrency action;
    private final Result preflight;
    private final BasicCurrencyPlan plan;
    private final List<Long> weights;
    private int branchIndex;
    private int candidateIndex;
    private long value;
    private Fraction emitted = Fraction.ZERO;
    private BigInteger remainingOutcomes = BigInteger.ZERO;

    private PrimitiveCursor(
        BasicCurrencyState source, WorkbenchCurrency action, Set<String> activeOmens) {
      this.action = action;
      preflight = expand(source, action, activeOmens, 1);
      if (blocked()) {
        plan = null;
        weights = List.of();
        return;
      }
      plan = simulator.basicCurrencyPlan(source.item(), action);
      var totals = new ArrayList<Long>();
      for (var branch : plan.branches()) {
        long total = 0;
        if (action == WorkbenchCurrency.ANNULMENT)
          remainingOutcomes = remainingOutcomes.add(BigInteger.ONE);
        for (var candidate : branch.candidates()) {
          total = Math.addExact(total, selectionWeight(candidate));
          remainingOutcomes = remainingOutcomes.add(rangeSize(candidate.stats().getFirst()));
        }
        totals.add(total);
      }
      weights = List.copyOf(totals);
      startRange();
    }

    public boolean blocked() {
      return preflight.status() == Status.UNSUPPORTED || preflight.status() == Status.UNAVAILABLE;
    }

    public Optional<KernelIdentity> identity() {
      if (blocked()) return Optional.empty();
      // Removal is uniform over these branches, including duplicate remaining items. Removed
      // values no longer affect the next item. All retained fields, candidate tiers/weights/rolls,
      // action and model provenance remain in record equality; no hash-only comparison is used.
      return Optional.of(
          new KernelIdentity(
              provenance,
              action,
              plan.branches().stream()
                  .map(branch -> new KernelBranch(branch.remaining(), branch.candidates()))
                  .toList()));
    }

    public BigInteger remainingOutcomes() {
      return remainingOutcomes;
    }

    /** Read-only indexed projection uses the same validated pool, weights and roll model. */
    public Fraction candidateBandProbability(int branch, int candidate, long first, long last) {
      var definition = indexedCandidate(branch, candidate, first, last);
      return new Fraction(
          BigInteger.valueOf(selectionWeight(definition))
              .multiply(
                  BigInteger.valueOf(last).subtract(BigInteger.valueOf(first)).add(BigInteger.ONE)),
          BigInteger.valueOf(weights.get(branch))
              .multiply(rangeSize(definition.stats().getFirst()))
              .multiply(BigInteger.valueOf(plan.branches().size())));
    }

    public BasicCurrencyState candidateWitness(int branch, int candidate, long value) {
      var definition = indexedCandidate(branch, candidate, value, value);
      var remaining = plan.branches().get(branch).remaining();
      var explicits = new ArrayList<>(remaining.explicits());
      explicits.add(
          new ModifierInstance(definition.id(), Map.of(definition.stats().getFirst().id(), value)));
      return new BasicCurrencyState(
          simulator.finishBasicCurrency(remaining, explicits), provenance);
    }

    private ModifierDefinition indexedCandidate(int branch, int candidate, long first, long last) {
      if (blocked() || action == WorkbenchCurrency.ANNULMENT)
        throw new IllegalStateException("No indexed addition pool");
      var definition = plan.branches().get(branch).candidates().get(candidate);
      var range = definition.stats().getFirst();
      if (first < range.min() || last > range.max() || first > last)
        throw new IllegalArgumentException("Invalid indexed roll range");
      return definition;
    }

    public Status status() {
      return blocked() ? preflight.status() : complete() ? Status.COMPLETE : Status.PARTIAL;
    }

    public String reason() {
      return blocked() ? preflight.reason() : "";
    }

    public List<WorkbenchSimulator.Assumption> assumptions() {
      if (blocked()) return preflight.assumptions();
      var result = new LinkedHashSet<WorkbenchSimulator.Assumption>();
      if (action.baseAction() == CraftingAction.ANNULMENT
          || action.baseAction() == CraftingAction.CHAOS)
        result.add(simulator.basicRemovalAssumption(plan.removals(), action));
      for (var branch : plan.branches()) {
        result.addAll(selectionAssumptions(branch.candidates()));
        for (var candidate : branch.candidates()) {
          var roll = simulator.singleStatRollAssumption(candidate);
          if (roll != null) result.add(roll);
        }
      }
      return List.copyOf(result);
    }

    /**
     * Necessary shared-rule conditions only. Candidate conflicts are still checked by the next
     * kernel.
     */
    public boolean mayApplyAfter(WorkbenchCurrency next) {
      if (next == null || !supportedActions().contains(next))
        throw new IllegalArgumentException("Basic currency required");
      if (blocked()) return true;
      if (BasicJewel.supportedCrafting(catalog.base().id())) return true;
      for (var branch : plan.branches()) {
        var rarity = branch.remaining().rarity();
        if (!WorkbenchSimulator.basicRarityAllows(rarity, next.baseAction())
            || branch.remaining().itemLevel() < next.minimumModifierLevel()) continue;
        int count =
            branch.remaining().explicits().size() + (action == WorkbenchCurrency.ANNULMENT ? 0 : 1);
        if (next.baseAction() == CraftingAction.CHAOS
            || next.baseAction() == CraftingAction.ANNULMENT) {
          if (count > 0) return true;
        } else {
          var targetRarity =
              next.baseAction() == CraftingAction.REGAL ? ItemState.Rarity.RARE : rarity;
          int capacity =
              targetRarity == ItemState.Rarity.RARE
                  ? catalog.base().rarePrefixes() + catalog.base().rareSuffixes()
                  : catalog.base().magicPrefixes() + catalog.base().magicSuffixes();
          if (count < capacity) return true;
        }
      }
      return false;
    }

    public boolean complete() {
      return !blocked() && branchIndex == plan.branches().size();
    }

    public Fraction unresolved() {
      return new Fraction(
          emitted.denominator().subtract(emitted.numerator()), emitted.denominator());
    }

    /**
     * Exactly one modifier-tier/roll outcome per call; resume retains branch, candidate and roll.
     */
    public Outcome next() {
      return nextGrouped((candidate, first) -> first).outcome();
    }

    public GroupedOutcome nextGrouped(
        java.util.function.ToLongBiFunction<ModifierDefinition, Long> end) {
      if (blocked() || complete()) throw new IllegalStateException("Cursor is not active");
      var branch = plan.branches().get(branchIndex);
      int emittedBranch = branchIndex;
      var removalMass = Fraction.of(1, plan.branches().size());
      Outcome result;
      String added = null;
      String stat = null;
      long first = value, last = value;
      if (action == WorkbenchCurrency.ANNULMENT) {
        result = new Outcome(new BasicCurrencyState(branch.remaining(), provenance), removalMass);
        branchIndex++;
      } else {
        var candidate = branch.candidates().get(candidateIndex);
        var range = candidate.stats().getFirst();
        added = candidate.id();
        stat = range.id();
        last = end.applyAsLong(candidate, value);
        if (last < value || last > range.max())
          throw new IllegalArgumentException("Invalid roll cell");
        var mass =
            removalMass.multiply(
                new Fraction(
                    BigInteger.valueOf(selectionWeight(candidate))
                        .multiply(
                            BigInteger.valueOf(last)
                                .subtract(BigInteger.valueOf(value))
                                .add(BigInteger.ONE)),
                    BigInteger.valueOf(weights.get(branchIndex)).multiply(rangeSize(range))));
        var explicits = new ArrayList<>(branch.remaining().explicits());
        explicits.add(new ModifierInstance(candidate.id(), Map.of(range.id(), value)));
        result =
            new Outcome(
                new BasicCurrencyState(
                    simulator.finishBasicCurrency(branch.remaining(), explicits), provenance),
                mass);
        if (last == range.max()) {
          candidateIndex++;
          if (candidateIndex == branch.candidates().size()) {
            branchIndex++;
            candidateIndex = 0;
          }
          startRange();
        } else value = last + 1;
      }
      emitted = emitted.add(result.probability());
      remainingOutcomes =
          remainingOutcomes.subtract(
              action == WorkbenchCurrency.ANNULMENT
                  ? BigInteger.ONE
                  : BigInteger.valueOf(last)
                      .subtract(BigInteger.valueOf(first))
                      .add(BigInteger.ONE));
      if (complete() && !emitted.equals(Fraction.ONE))
        throw new IllegalStateException("Primitive mass not conserved");
      return new GroupedOutcome(result, added, stat, first, last, emittedBranch);
    }

    private void startRange() {
      if (action != WorkbenchCurrency.ANNULMENT && branchIndex < plan.branches().size())
        value =
            plan.branches()
                .get(branchIndex)
                .candidates()
                .get(candidateIndex)
                .stats()
                .getFirst()
                .min();
    }
  }

  public record GroupedOutcome(
      Outcome outcome, String addedModifier, String stat, long min, long max, int branch) {}

  /**
   * Streaming proof for the fixed one-explicit policies. The cursor retains the exact elementary
   * roll position; no truncated transition is promoted to a complete kernel or cached as one.
   */
  public RenewalCursor renewalCursor(BasicCurrencyState source, List<WorkbenchCurrency> actions) {
    return new RenewalCursor(source, actions);
  }

  public final class RenewalCursor {
    private final BasicCurrencyState source;
    private final List<WorkbenchCurrency> actions;
    private BasicCurrencyPlan plan;
    private ItemState empty;
    private int candidateIndex;
    private long value;
    private long totalWeight;
    private String blocked;
    private boolean unavailable;

    private RenewalCursor(BasicCurrencyState source, List<WorkbenchCurrency> actions) {
      this.source = source;
      this.actions = List.copyOf(actions);
      boolean chaos =
          actions.size() == 1 && actions.getFirst().baseAction() == CraftingAction.CHAOS;
      boolean pair =
          actions.size() == 2
              && actions.getFirst() == WorkbenchCurrency.ANNULMENT
              && actions.getLast().baseAction() == CraftingAction.EXALTED;
      if (!chaos && !pair) throw new IllegalArgumentException("Fixed renewal policy required");
      var item = source.item();
      if (!provenance.equals(source.provenance()))
        throw new IllegalArgumentException("Current provenance required");
      if (!catalog.base().id().equals(SolarAmulet.BASE_ID)
          || item.rarity() != ItemState.Rarity.RARE
          || item.explicits().size() != 1
          || item.explicits().getFirst().fractured()
          || item.catalystQuality() != null
          || !item.conditions().isEmpty()
          || QualityCapChangePolicy.DEFAULT != QualityCapChangePolicy.PRESERVE_EXISTING) {
        blocked = "RENEWAL_SCOPE_NOT_SUPPORTED";
        return;
      }
      var removal = simulator.basicCurrencyPlan(item, actions.getFirst());
      if (!removal.available()) {
        unavailable = true;
        blocked = removal.reason();
        return;
      }
      if (removal.branches().size() != 1) {
        blocked = "RENEWAL_NOT_PROVEN";
        return;
      }
      empty = removal.branches().getFirst().remaining();
      var expected =
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
      if (!empty.equals(expected)) {
        blocked = "RENEWAL_NOT_PROVEN";
        return;
      }
      var input = pair ? new BasicCurrencyState(empty, provenance) : source;
      var preflight = expand(input, actions.getLast(), Set.of(), 1);
      if (preflight.status() == Status.UNSUPPORTED || preflight.status() == Status.UNAVAILABLE) {
        blocked = preflight.reason();
        unavailable = preflight.status() == Status.UNAVAILABLE;
        return;
      }
      plan = simulator.basicCurrencyPlan(input.item(), actions.getLast());
      if (plan.branches().size() != 1 || !plan.branches().getFirst().remaining().equals(empty)) {
        blocked = "RENEWAL_NOT_PROVEN";
        return;
      }
      for (var candidate : plan.branches().getFirst().candidates())
        totalWeight = Math.addExact(totalWeight, selectionWeight(candidate));
      value = plan.branches().getFirst().candidates().getFirst().stats().getFirst().min();
    }

    public String blocked() {
      return blocked;
    }

    public boolean unavailable() {
      return unavailable;
    }

    public BasicCurrencyState emptyState() {
      return empty == null ? null : new BasicCurrencyState(empty, provenance);
    }

    public boolean complete() {
      return blocked == null && candidateIndex == plan.branches().getFirst().candidates().size();
    }

    /**
     * Checks the return kernel for every positive roll, including unrelated and failing outcomes.
     */
    public Outcome next() {
      if (blocked != null || complete()) throw new IllegalStateException("Cursor is not active");
      var candidates = plan.branches().getFirst().candidates();
      var candidate = candidates.get(candidateIndex);
      var range = candidate.stats().getFirst();
      var instance = new ModifierInstance(candidate.id(), Map.of(range.id(), value));
      var item = simulator.finishBasicCurrency(empty, List.of(instance));
      var next = simulator.basicCurrencyPlan(item, actions.getFirst());
      if (!next.available()
          || next.branches().size() != 1
          || !next.branches().getFirst().remaining().equals(empty)) {
        blocked = "RENEWAL_NOT_PROVEN";
        return null;
      }
      var returning =
          actions.size() == 1 ? next : simulator.basicCurrencyPlan(empty, actions.getLast());
      if (!returning.available()
          || returning.branches().size() != 1
          || !returning.branches().getFirst().candidates().equals(candidates)) {
        blocked = "RENEWAL_NOT_PROVEN";
        return null;
      }
      var mass =
          new Fraction(
              BigInteger.valueOf(selectionWeight(candidate)),
              BigInteger.valueOf(totalWeight).multiply(rangeSize(range)));
      if (value == range.max()) {
        candidateIndex++;
        if (candidateIndex < candidates.size())
          value = candidates.get(candidateIndex).stats().getFirst().min();
      } else value++;
      return new Outcome(new BasicCurrencyState(item, provenance), mass);
    }
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
      total = Math.addExact(total, selectionWeight(candidate));
      if (candidate.id().equals(target))
        selected = Math.addExact(selected, selectionWeight(candidate));
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
    if (!reviewedCatalog && !SolarAmulet.BASE_ID.equals(catalog.base().id()))
      return blocked(source, action, Status.UNSUPPORTED, "NUMERIC_BASE_NOT_IMPLEMENTED");
    if (!"POE2DB_AS_PUBLISHED".equals(catalog.metadata().weightPolicy())
        && !(reviewedCatalog && uniformSelection()))
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
      assumptions.addAll(selectionAssumptions(branch.candidates()));
      long total = 0;
      for (var candidate : branch.candidates())
        total = Math.addExact(total, selectionWeight(candidate));
      for (var candidate : branch.candidates()) {
        var range = candidate.stats().getFirst();
        var mass =
            removalMass.multiply(
                new Fraction(
                    BigInteger.valueOf(selectionWeight(candidate)),
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
