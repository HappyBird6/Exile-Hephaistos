package com.poe2craft.crafting.application;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import java.util.*;

/** Forward first-hit DP for fixed finite-addition sequences; unresolved mass is never failure. */
public final class SupportRecommendations {
  private final ItemCatalog catalog;
  private final AdditionPoolCache pools;
  private final SupportGoals goals;
  private final Map<String, Integer> familyIndex = new HashMap<>();

  public SupportRecommendations(ItemCatalog catalog, AdditionPoolCache pools) {
    this.catalog = catalog;
    this.pools = pools;
    goals = new SupportGoals(catalog);
    int index = 0;
    for (var family : goals.families()) familyIndex.put(family.id(), index++);
  }

  public synchronized Report recommend(
      StateBucket root, SupportGoals.Goal goal, Set<String> activeOmens, Limits limits) {
    if (limits == null
        || limits.maxStates() < 1
        || limits.maxStates() > 200000
        || limits.maxEdges() < 1
        || limits.maxEdges() > 5000000
        || limits.maxMillis() < 10
        || limits.maxMillis() > 10000)
      throw new IllegalArgumentException("Invalid Support computation budget");
    var assessment = goals.assess(root, goal);
    if (!assessment.valid()) throw new IllegalArgumentException("Invalid Support goal");
    var omens = activeOmens == null ? Set.<String>of() : Set.copyOf(activeOmens);
    omens.forEach(WorkbenchOmen::fromId);
    rejectMultiAddition(omens);
    long start = System.nanoTime();
    var before = pools.stats();
    var budget = new Budget(limits, start);
    if (assessment.achieved() || !assessment.feasible())
      return report(assessment, List.of(), true, true, 0, 0, budget, before, start);
    var sequences = new ArrayList<List<WorkbenchCurrency>>();
    sequences(
        root.rarity(), root.modifierIds().size(), root.itemLevel(), new ArrayList<>(), sequences);
    var comparisons = new ArrayList<Comparison>();
    for (var sequence : sequences) {
      if (budget.globallyExhausted()) break;
      budget.localStateLimit =
          budget.states + Math.max(1, limits.maxStates() / Math.max(1, sequences.size()));
      comparisons.add(evaluate(root, goal, omens, sequence, budget));
    }
    comparisons.sort(
        Comparator.comparingDouble(Comparison::successLower)
            .reversed()
            .thenComparing(c -> c.sequence().toString()));
    var top = comparisons.stream().limit(5).toList();
    boolean complete =
        comparisons.size() == sequences.size()
            && comparisons.stream().allMatch(Comparison::complete);
    boolean certified = complete;
    return report(
        assessment,
        top,
        complete,
        certified,
        comparisons.size(),
        sequences.size(),
        budget,
        before,
        start);
  }

  private Report report(
      SupportGoals.Assessment assessment,
      List<Comparison> top,
      boolean complete,
      boolean certified,
      int compared,
      int total,
      Budget budget,
      AdditionPoolCache.Stats before,
      long start) {
    var after = pools.stats();
    return new Report(
        WorkbenchSimulator.RULE_VERSION,
        WorkbenchSimulator.LEDGER_VERSION,
        pools.namespace(),
        assessment,
        top,
        complete,
        certified,
        compared,
        total,
        budget.states,
        budget.edges,
        (System.nanoTime() - start) / 1e6,
        new AdditionPoolCache.Stats(
            after.memoryHits() - before.memoryHits(),
            after.persistedHits() - before.persistedHits(),
            after.computedPools() - before.computedPools()));
  }

  public Comparison evaluate(
      StateBucket root,
      SupportGoals.Goal goal,
      Set<String> omens,
      List<WorkbenchCurrency> sequence,
      Limits limits) {
    rejectMultiAddition(omens);
    var assessment = goals.assess(root, goal);
    if (!assessment.valid()
        || sequence == null
        || sequence.size() > 6
        || sequence.stream().anyMatch(a -> !AdditionRules.isAddition(a)))
      throw new IllegalArgumentException("Invalid finite addition sequence or goal");
    return evaluate(root, goal, omens, sequence, new Budget(limits, System.nanoTime()));
  }

  private void rejectMultiAddition(Set<String> omens) {
    if (omens != null && omens.contains(WorkbenchOmen.GREATER_EXALTATION.id()))
      throw new IllegalArgumentException(
          "Greater Exaltation is outside the finite addition model. Deactivate it for Support.");
  }

  private Comparison evaluate(
      StateBucket root,
      SupportGoals.Goal goal,
      Set<String> omens,
      List<WorkbenchCurrency> sequence,
      Budget budget) {
    if (matched(root, goal))
      return new Comparison(
          sequence, 1, 1, 0, 0, true, List.of(new Step(0, null, 1, 0, 0, List.of())));
    Map<Key, Mass> current = new LinkedHashMap<>();
    current.put(key(root, goal, omens), new Mass(root, omens, 1));
    double success = 0, failure = 0;
    var steps = new ArrayList<Step>();
    for (int step = 0; step < sequence.size(); step++) {
      var action = sequence.get(step);
      Map<Key, Mass> next = new LinkedHashMap<>();
      var sources = new ArrayList<>(current.values());
      double stepSuccess = 0, stepFailure = 0;
      var blockedReasons = new TreeSet<String>();
      for (int index = 0; index < sources.size(); index++) {
        if (budget.exhausted()) {
          double unresolved = next.values().stream().mapToDouble(Mass::probability).sum();
          for (int i = index; i < sources.size(); i++) unresolved += sources.get(i).probability();
          steps.add(
              new Step(
                  step + 1,
                  action,
                  stepSuccess,
                  stepFailure,
                  unresolved,
                  List.copyOf(blockedReasons)));
          return comparison(sequence, success, failure, unresolved, false, steps);
        }
        var source = sources.get(index);
        budget.states++;
        var pool = pools.get(source.state(), action, source.omens());
        if (!pool.available()) {
          failure += source.probability();
          stepFailure += source.probability();
          blockedReasons.add(pool.reason());
          continue;
        }
        // Expand a pool atomically (<=209 channels), then check the budget before the next source.
        budget.edges += pool.channels().size();
        var remaining = Set.copyOf(pool.remainingOmens());
        for (var channel : pool.channels()) {
          var ids = new ArrayList<>(source.state().modifierIds());
          ids.add(channel.modifierId());
          var target = source.state().with(pool.rarity(), ids);
          double probability = source.probability() * channel.probability();
          if (matched(target, goal)) {
            success += probability;
            stepSuccess += probability;
          } else {
            var key = key(target, goal, remaining);
            var prior = next.get(key);
            next.put(
                key,
                new Mass(
                    prior == null ? target : prior.state(),
                    remaining,
                    probability + (prior == null ? 0 : prior.probability())));
          }
        }
      }
      double unresolved = next.values().stream().mapToDouble(Mass::probability).sum();
      steps.add(
          new Step(
              step + 1, action, stepSuccess, stepFailure, unresolved, List.copyOf(blockedReasons)));
      current = next;
      if (current.isEmpty()) break;
    }
    failure += current.values().stream().mapToDouble(Mass::probability).sum();
    return comparison(sequence, success, failure, 0, true, steps);
  }

  private Comparison comparison(
      List<WorkbenchCurrency> sequence,
      double success,
      double failure,
      double unresolved,
      boolean complete,
      List<Step> steps) {
    if (Math.abs(success + failure + unresolved - 1) > 1e-8)
      throw new IllegalStateException("Support probability mass mismatch");
    return new Comparison(
        sequence, success, Math.min(1, success + unresolved), failure, unresolved, complete, steps);
  }

  private Key key(StateBucket state, SupportGoals.Goal goal, Set<String> omens) {
    long occupied = 0, qualified = 0;
    var thresholds = thresholds(goal);
    for (var id : state.modifierIds()) {
      var d = catalog.find(id).orElseThrow();
      var family = d.familyIds().iterator().next();
      long bit = 1L << familyIndex.get(family);
      occupied |= bit;
      if (thresholds.containsKey(family) && d.tier() <= thresholds.get(family)) qualified |= bit;
    }
    return new Key(occupied, qualified, state.rarity(), omens);
  }

  private Map<String, Integer> thresholds(SupportGoals.Goal goal) {
    var thresholds = new HashMap<String, Integer>();
    goal.required().forEach(c -> thresholds.put(c.family(), c.minimumTier()));
    goal.candidates().forEach(c -> thresholds.put(c.family(), c.minimumTier()));
    return thresholds;
  }

  private boolean matched(StateBucket state, SupportGoals.Goal goal) {
    var tiers = new HashMap<String, Integer>();
    for (var id : state.modifierIds()) {
      var d = catalog.find(id).orElseThrow();
      tiers.put(d.familyIds().iterator().next(), d.tier());
    }
    return goal.required().stream()
            .allMatch(c -> tiers.getOrDefault(c.family(), Integer.MAX_VALUE) <= c.minimumTier())
        && goal.candidates().stream()
                .filter(c -> tiers.getOrDefault(c.family(), Integer.MAX_VALUE) <= c.minimumTier())
                .count()
            >= goal.candidateCount();
  }

  private void sequences(
      ItemState.Rarity rarity,
      int count,
      int level,
      List<WorkbenchCurrency> prefix,
      List<List<WorkbenchCurrency>> results) {
    if (count >= 6) {
      results.add(List.copyOf(prefix));
      return;
    }
    for (var action :
        List.of(
            WorkbenchCurrency.PERFECT_TRANSMUTATION,
            WorkbenchCurrency.GREATER_TRANSMUTATION,
            WorkbenchCurrency.TRANSMUTATION,
            WorkbenchCurrency.PERFECT_AUGMENTATION,
            WorkbenchCurrency.GREATER_AUGMENTATION,
            WorkbenchCurrency.AUGMENTATION,
            WorkbenchCurrency.PERFECT_REGAL,
            WorkbenchCurrency.GREATER_REGAL,
            WorkbenchCurrency.REGAL,
            WorkbenchCurrency.PERFECT_EXALTED,
            WorkbenchCurrency.GREATER_EXALTED,
            WorkbenchCurrency.EXALTED)) {
      if (level < action.minimumModifierLevel()) continue;
      boolean eligible =
          switch (action.baseAction()) {
            case TRANSMUTATION -> rarity == ItemState.Rarity.NORMAL;
            case AUGMENTATION -> rarity == ItemState.Rarity.MAGIC && count < 2;
            case REGAL -> rarity == ItemState.Rarity.MAGIC;
            case EXALTED -> rarity == ItemState.Rarity.RARE;
            default -> false;
          };
      if (!eligible) continue;
      var nextRarity =
          action.baseAction() == CraftingAction.TRANSMUTATION
              ? ItemState.Rarity.MAGIC
              : action.baseAction() == CraftingAction.REGAL ? ItemState.Rarity.RARE : rarity;
      prefix.add(action);
      sequences(nextRarity, count + 1, level, prefix, results);
      prefix.removeLast();
    }
  }

  private record Key(long occupied, long qualified, ItemState.Rarity rarity, Set<String> omens) {}

  private record Mass(StateBucket state, Set<String> omens, double probability) {}

  private static final class Budget {
    final Limits limits;
    final long start;
    int states, edges;
    int localStateLimit = Integer.MAX_VALUE;

    Budget(Limits limits, long start) {
      this.limits = limits;
      this.start = start;
    }

    boolean globallyExhausted() {
      return states >= limits.maxStates()
          || edges >= limits.maxEdges()
          || System.nanoTime() - start >= limits.maxMillis() * 1000000L
          || Thread.currentThread().isInterrupted();
    }

    boolean exhausted() {
      return states >= localStateLimit || globallyExhausted();
    }
  }

  public record Limits(int maxStates, int maxEdges, int maxMillis) {}

  public record Step(
      int step,
      WorkbenchCurrency action,
      double firstHitProbability,
      double blockedProbability,
      double continuingOrUnresolvedProbability,
      List<String> blockedReasons) {
    public Step {
      blockedReasons = List.copyOf(blockedReasons);
    }
  }

  public record Comparison(
      List<WorkbenchCurrency> sequence,
      double successLower,
      double successUpper,
      double failureProbability,
      double unresolvedProbability,
      boolean complete,
      List<Step> steps) {
    public Comparison {
      sequence = List.copyOf(sequence);
      steps = List.copyOf(steps);
    }
  }

  public record Report(
      String ruleVersion,
      String ledgerVersion,
      String transitionNamespace,
      SupportGoals.Assessment assessment,
      List<Comparison> comparisons,
      boolean complete,
      boolean rankingCertified,
      int comparedSequences,
      int totalSequences,
      int expandedStates,
      int expandedEdges,
      double elapsedMillis,
      AdditionPoolCache.Stats cache) {
    public Report {
      comparisons = List.copyOf(comparisons);
    }
  }
}
