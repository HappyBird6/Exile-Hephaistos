package com.poe2craft.crafting.domain;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.AutomaticTierPartition;
import com.poe2craft.crafting.domain.pathsearch.RollPartition;
import com.poe2craft.crafting.domain.pathsearch.TierCell;
import com.poe2craft.crafting.domain.pathsearch.TierPartition;
import com.poe2craft.item.*;
import java.math.BigInteger;
import java.util.*;

/**
 * Finite primitive laws under declared scalar/coupled assumptions, never sampled game
 * probabilities.
 */
public final class AutomaticCraftingKernel {
  public static final String VERSION = "automatic-finite-kernel-v7-reviewed-source-units";

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

  public record Outcome(
      ItemState item, Fraction probability, Map<String, TierCell> tiers, Set<String> retained) {
    public Outcome {
      tiers = Map.copyOf(tiers);
      retained = Set.copyOf(retained);
    }

    public Outcome(ItemState item, Fraction probability, Map<String, TierCell> tiers) {
      this(
          item,
          probability,
          tiers,
          item.explicits().stream()
              .map(ModifierInstance::modifierId)
              .collect(java.util.stream.Collectors.toSet()));
    }

    public Outcome(ItemState item, Fraction probability) {
      this(item, probability, Map.of());
    }
  }

  private record TierState(
      ItemState item, Map<String, TierCell> tiers, Set<String> retained, boolean lineage) {
    TierState {
      tiers = Map.copyOf(tiers);
    }
  }

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
  private int qualityGoal;
  private final boolean tierGroupingSafe;

  public record RetainedModifierRole(
      ModifierDefinition.AffixType side, Set<String> families, boolean fractured) {}

  /** Tier-independent identity used only after a caller proves every future transition. */
  public List<RetainedModifierRole> retainedModifierRoles(ItemState item) {
    return item.explicits().stream()
        .map(
            m -> {
              var definition = Objects.requireNonNull(catalog.modifiers().get(m.modifierId()));
              return new RetainedModifierRole(
                  definition.affixType(), Set.copyOf(definition.familyIds()), m.fractured());
            })
        .sorted(
            Comparator.comparing((RetainedModifierRole r) -> r.side().name())
                .thenComparing(r -> new TreeSet<>(r.families()).toString())
                .thenComparing(RetainedModifierRole::fractured))
        .toList();
  }

  private record Role(Object attribute, Set<String> tags, Object region) {}

  private record Behavior(ItemState context, List<Role> roles) {}

  public int modifierTier(String id) {
    return Objects.requireNonNull(catalog.modifiers().get(id)).tier();
  }

  public Object attributeKey(ModifierInstance modifier) {
    var definition = Objects.requireNonNull(catalog.modifiers().get(modifier.modifierId()));
    return List.of(
        definition.affixType(),
        new TreeSet<>(definition.familyIds()),
        new TreeSet<>(modifier.values().keySet()),
        modifier.fractured());
  }

  public Object behaviorKey(ItemState item) {
    if (!tierGroupingSafe) return item;
    var roles =
        item.explicits().stream()
            .map(
                m -> {
                  var definition = Objects.requireNonNull(catalog.modifiers().get(m.modifierId()));
                  var regions = new TreeMap<String, Object>();
                  m.values()
                      .forEach(
                          (stat, value) ->
                              regions.put(stat, partition.automaticRegion(stat, value)));
                  return new Role(attributeKey(m), definition.tags(), regions);
                })
            .sorted(Comparator.comparing(Object::toString))
            .toList();
    var context =
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
    return new Behavior(context, roles);
  }

  public Object attributeContext(ItemState item) {
    var roles =
        item.explicits().stream()
            .map(this::attributeKey)
            .sorted(Comparator.comparing(Object::toString))
            .toList();
    var context =
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
    return List.of(context, roles);
  }

  public AutomaticCraftingKernel withQualityGoal(int quality) {
    if (quality < 0 || quality > 40) throw new IllegalArgumentException("Invalid quality goal");
    var configured =
        new AutomaticCraftingKernel(
            policy, catalog, simulator, family, partition, identity.rollPartition());
    configured.qualityGoal = quality;
    return configured;
  }

  public int qualityGoal() {
    return qualityGoal;
  }

  public int workedQuality(ItemState item, int abstractQuality) {
    return Math.max(
        abstractQuality, item.catalystQuality() == null ? 0 : item.catalystQuality().amount());
  }

  public boolean baseQualitySupported(ItemState item) {
    return com.poe2craft.item.CatalystQuality.supportedBase(item.baseItemId());
  }

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

  /** Eligibility and prerequisite projection, before allocating a numeric probability law. */
  public AutomaticCraftingPlan plan(ItemState item, AutomaticCraftingAction action) {
    if (action.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_BASE_CHECKPOINT)
      return new AutomaticCraftingPlan(
          AutomaticCraftingPlan.Status.UNAVAILABLE, "ABSTRACT_QUALITY_WORK", List.of(), false, "");
    var context = new AutomaticCurrencyPolicy.Context(family, action.purpose(), action.currency());
    if (policy.evaluate(action.currency().name(), context)
            != AutomaticCurrencyPolicy.Verdict.PERMITTED
        || action.omens().stream()
            .anyMatch(
                o ->
                    policy.evaluate("OMEN:" + o.name(), context)
                        != AutomaticCurrencyPolicy.Verdict.PERMITTED))
      return new AutomaticCraftingPlan(
          AutomaticCraftingPlan.Status.UNAVAILABLE, "POLICY_REJECTED", List.of(), false, "");
    return simulator.automaticPlan(item, action);
  }

  /**
   * Every scalar goal cell or declared joint tuple is considered, without selection probability.
   */
  public List<ItemState> representatives(AutomaticCraftingPlan plan) {
    var result = new ArrayList<ItemState>();
    for (var branch : plan.branches())
      for (var candidate : branch.candidates()) {
        if (candidate.stats().size() != 1) {
          for (var cell : simulator.automaticJointRolls(candidate).orElse(List.of())) {
            var explicits = new ArrayList<>(branch.remaining().explicits());
            explicits.add(cell.modifier());
            if (policy.withinExplorationBoundary(explicits.size()))
              result.add(simulator.finishBasicCurrency(branch.remaining(), explicits));
          }
          continue;
        }
        var range = candidate.stats().getFirst();
        for (long value = range.min(); ; ) {
          var explicits = new ArrayList<>(branch.remaining().explicits());
          explicits.add(new ModifierInstance(candidate.id(), Map.of(range.id(), value)));
          if (policy.withinExplorationBoundary(explicits.size()))
            result.add(simulator.finishBasicCurrency(branch.remaining(), explicits));
          long end = partition.end(candidate, value);
          if (end >= range.max()) break;
          value = end + 1;
        }
      }
    return List.copyOf(result);
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
    tierGroupingSafe =
        AutomaticCraftingAction.candidates(policy).stream()
            .noneMatch(a -> a.omens().contains(WorkbenchOmen.WHITTLING));
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
    return expand(state, Map.of(), execution, outcomeBudget, false, false);
  }

  /** Keep each conditional tier law attached to its state, including through removal. */
  public Result expandDeferred(
      ItemState state,
      Map<String, TierCell> tiers,
      AutomaticCraftingAction execution,
      long outcomeBudget) {
    return expand(state, tiers, execution, outcomeBudget, tierGroupingSafe, true);
  }

  private Result expand(
      ItemState state,
      Map<String, TierCell> retainedTiers,
      AutomaticCraftingAction execution,
      long outcomeBudget,
      boolean deferred,
      boolean lineage) {
    if (outcomeBudget < 1) throw new IllegalArgumentException("Positive outcome budget required");
    if (execution.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_BASE_CHECKPOINT) {
      if (execution.currency() != WorkbenchCurrency.ESSENCE_BREACH
          || !execution.omens().isEmpty()
          || !baseQualitySupported(state))
        return blocked(Status.UNAVAILABLE, "BASE_QUALITY_NOT_VERIFIED");
      return new Result(
          Status.COMPLETE,
          "",
          List.of(new Outcome(state, Fraction.ONE, retainedTiers)),
          Fraction.ZERO,
          Fraction.ZERO,
          List.of());
    }
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
          List.of(new Outcome(state, Fraction.ONE, retainedTiers)),
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
          if (simulator.automaticJointRolls(candidate).isEmpty())
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
    var outcomes = new LinkedHashMap<TierState, Fraction>();
    var assumptions = new LinkedHashSet<WorkbenchSimulator.Assumption>();
    if (execution.omens().contains(WorkbenchOmen.WHITTLING)) {
      var source =
          policy.document().entries().stream()
              .filter(e -> e.id().equals("OMEN:WHITTLING"))
              .findFirst()
              .orElseThrow();
      assumptions.add(
          new WorkbenchSimulator.Assumption(
              "declared-whittling-priority-v1",
              "eligible lowest modifier-level removal",
              plan.branches().size(),
              List.of(),
              null,
              null,
              source.sourceUrl(),
              "Lowest required modifier level after any affix-side restriction. Fractured combinations remain unsupported. Side+Whittling order is a declared composition, not independently established game odds."));
    }
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
      var emittedCells = new HashSet<TierCell>();
      var posterior =
          deferred
              ? new AutomaticTierPartition(partition)
                  .members(branch.candidates(), uniform, outcomeBudget)
              : Map.<TierPartition.BandKey, TierPartition.Member>of();
      var candidateIds =
          branch.candidates().stream()
              .map(ModifierDefinition::id)
              .collect(java.util.stream.Collectors.toSet());
      var retainedIds =
          branch.remaining().explicits().stream()
              .map(ModifierInstance::modifierId)
              .collect(java.util.stream.Collectors.toSet());
      var keptTiers = new HashMap<>(retainedTiers);
      keptTiers.keySet().retainAll(retainedIds);
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
        var selection = branchMass.multiply(Fraction.of(uniform ? 1 : candidate.weight(), total));
        if (candidate.stats().size() != 1) {
          var law = simulator.automaticJointRolls(candidate);
          if (law.isEmpty()) continue;
          if (candidate.stats().stream().anyMatch(s -> s.min() != s.max()))
            assumptions.add(CoupledStatRollModel.assumption(candidate, null));
          if (!policy.withinExplorationBoundary(branch.remaining().explicits().size() + 1)) {
            boundary = boundary.add(selection);
            emitted = emitted.add(selection);
            continue;
          }
          for (var cell : law.get()) {
            if (count >= outcomeBudget)
              return result(outcomes, boundary, emitted, assumptions, "OUTCOME_BUDGET");
            var explicits = new ArrayList<>(branch.remaining().explicits());
            explicits.add(cell.modifier());
            var item = simulator.finishBasicCurrency(branch.remaining(), explicits);
            var tierLaws = new HashMap<>(keptTiers);
            tierLaws.remove(candidate.id());
            var mass =
                selection.multiply(Fraction.of(cell.ticks(), CoupledStatRollModel.RESOLUTION + 1));
            outcomes.merge(
                new TierState(
                    item, tierLaws, lineage ? Set.copyOf(retainedIds) : Set.of(), lineage),
                mass,
                Fraction::add);
            emitted = emitted.add(mass);
            count++;
          }
          continue;
        }
        var range = candidate.stats().getFirst();
        var size =
            BigInteger.valueOf(range.max())
                .subtract(BigInteger.valueOf(range.min()))
                .add(BigInteger.ONE);
        if (size.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) continue;
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
          var tierLaws = new HashMap<>(keptTiers);
          tierLaws.remove(candidate.id()); // Removed and re-added means a fresh random draw.
          var member =
              deferred
                  ? Optional.ofNullable(
                      posterior.get(new TierPartition.BandKey(candidate.id(), value, end)))
                  : Optional.<com.poe2craft.crafting.domain.pathsearch.TierPartition.Member>empty();
          if (member.isPresent()
              && member.get().cell().variants().stream()
                  .allMatch(v -> candidateIds.contains(v.modifierId()))) {
            var tier = member.get();
            { // The posterior was built with this branch's actual selection model.
              if (!emittedCells.add(tier.cell())) {
                if (end == range.max()) break;
                value = end + 1;
                continue;
              }
              var representative = tier.cell().representative(false);
              explicits.removeLast();
              explicits.add(representative);
              item = simulator.finishBasicCurrency(branch.remaining(), explicits);
              tierLaws.put(representative.modifierId(), tier.cell());
              cellMass =
                  cellMass.multiply(
                      new Fraction(
                          tier.cellWeight()
                              .numerator()
                              .multiply(tier.intervalWeight().denominator()),
                          tier.cellWeight()
                              .denominator()
                              .multiply(tier.intervalWeight().numerator())));
            }
          }
          outcomes.merge(
              new TierState(item, tierLaws, lineage ? Set.copyOf(retainedIds) : Set.of(), lineage),
              cellMass,
              Fraction::add);
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
      Map<TierState, Fraction> outcomes,
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
        outcomes.entrySet().stream()
            .map(
                e ->
                    e.getKey().lineage()
                        ? new Outcome(
                            e.getKey().item(),
                            e.getValue(),
                            e.getKey().tiers(),
                            e.getKey().retained())
                        : new Outcome(e.getKey().item(), e.getValue(), e.getKey().tiers()))
            .toList(),
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
