package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.Function;

/** Engine facts, not the reference planner's selected candidates. No retrieval or network here. */
public final class StrategyEvidence {
  public record ActionFact(
      String id,
      AutomaticCraftingAction action,
      String status,
      String reason,
      boolean advances,
      boolean protectedRemoval,
      boolean guaranteedProtectedRemoval) {}

  public record PreparationFact(
      String initial,
      String cycle,
      boolean ready,
      boolean advances,
      boolean consumesDummyAndAdvances,
      boolean tierPreparationAdvances) {}

  public record AdditionFact(
      String plain,
      String other,
      boolean equalPlans,
      boolean complete,
      boolean bothCertainlyFinishGoal,
      boolean otherDominatesGoodItems,
      Fraction plainAdvanceProbability,
      Fraction otherAdvanceProbability) {}

  public record Context(
      ItemState item,
      AutomaticMethodGraph.Progress progress,
      boolean initialStage,
      boolean nodeComplete,
      boolean acquiredMutableTarget,
      long mutableAffixes,
      boolean initialChaosOnly,
      int qualityGoal,
      int workedQuality,
      boolean hasQualityCap,
      boolean baseQualitySupported) {}

  public record Facts(
      Context context,
      List<ActionFact> actions,
      List<PreparationFact> preparations,
      List<AdditionFact> additions) {}

  public static final class LimitReached extends RuntimeException {}

  private final AutomaticCraftingKernel kernel;
  private final Function<ItemState, AutomaticMethodGraph.Progress> evaluate;
  private final GoalDirectedPlanner predicates;
  private final long deadline;
  private int remaining;
  private final Map<AutomaticCraftingPlan, List<ItemState>> representatives =
      new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(
            Map.Entry<AutomaticCraftingPlan, List<ItemState>> eldest) {
          return size() > 64;
        }
      };

  public StrategyEvidence(
      AutomaticCraftingKernel kernel,
      Function<ItemState, AutomaticMethodGraph.Progress> evaluate,
      List<AutomaticCraftingAction> inventory,
      int operations,
      long deadline) {
    this.kernel = kernel;
    this.evaluate = evaluate;
    this.predicates = new GoalDirectedPlanner(kernel, evaluate, inventory);
    this.remaining = operations;
    this.deadline = deadline;
  }

  private void charge() {
    if (--remaining < 0 || System.nanoTime() > deadline || Thread.currentThread().isInterrupted())
      throw new LimitReached();
  }

  private List<ItemState> representatives(AutomaticCraftingPlan plan) {
    charge();
    var result = representatives.get(plan);
    if (result == null) {
      result = kernel.representatives(plan);
      remaining -= result.size();
      charge();
      representatives.put(plan, result);
    }
    return result;
  }

  public Facts build(
      ItemState source,
      boolean initialStage,
      int abstractQuality,
      List<AutomaticCraftingAction> inventory) {
    var actions = new ArrayList<>(inventory);
    var basicQuality =
        new AutomaticCraftingAction(
            WorkbenchCurrency.ESSENCE_BREACH,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.QUALITY_BASE_CHECKPOINT);
    if (!actions.contains(basicQuality)) actions.add(basicQuality);
    actions.addAll(kernel.desecrationActions(source));
    var plans = new ArrayList<AutomaticCraftingPlan>();
    var facts = new ArrayList<ActionFact>();
    for (int i = 0; i < actions.size(); i++) {
      charge();
      var action = actions.get(i);
      var plan = kernel.plan(source, action);
      plans.add(plan);
      facts.add(
          new ActionFact(
              id(i),
              action,
              plan.status().name(),
              plan.reason(),
              representatives(plan).stream().anyMatch(t -> predicates.advances(source, t)),
              predicates.protectedRemoval(source, plan),
              predicates.guaranteedProtectedRemoval(source, plan)));
    }
    var pairs = new ArrayList<PreparationFact>();
    var before = evaluate.apply(source);
    for (int i = 0; i < inventory.size(); i++) {
      var purpose = actions.get(i).purpose();
      if (purpose != AutomaticCurrencyPolicy.Purpose.RARITY_UPGRADE
          && purpose != AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL
          && purpose != AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION) continue;
      if (plans.get(i).status() != AutomaticCraftingPlan.Status.READY) continue;
      for (int j = 0; j < inventory.size(); j++) {
        var acquisition = actions.get(j);
        if (acquisition.purpose() != AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION
            && acquisition.currency().replacementModifiers().isEmpty()) continue;
        boolean ready = false, advances = false, dummy = false, tier = false;
        for (var prepared : representatives(plans.get(i))) {
          charge();
          var next = kernel.plan(prepared, acquisition);
          if (next.status() != AutomaticCraftingPlan.Status.READY) continue;
          ready = true;
          boolean advance =
              representatives(next).stream().anyMatch(t -> predicates.advances(source, t));
          advances |= advance;
          if (advance) {
            dummy |= predicates.consumesPreparedAffix(source, prepared, next);
            tier |=
                predicates.preserves(before, prepared)
                    && GoalDirectedPlanner.matched(evaluate.apply(prepared))
                        == GoalDirectedPlanner.matched(before)
                    && predicates.protectedRemoval(source, next);
          }
          if (advances && dummy && tier) break;
        }
        if (ready) pairs.add(new PreparationFact(id(i), id(j), true, advances, dummy, tier));
      }
    }
    var additions = new ArrayList<AdditionFact>();
    for (var currency : List.of(WorkbenchCurrency.EXALTED, WorkbenchCurrency.REGAL)) {
      int plain = -1;
      for (int i = 0; i < inventory.size(); i++) {
        var a = actions.get(i);
        if (a.currency() == currency
            && a.omens().isEmpty()
            && a.purpose() == AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION) plain = i;
      }
      if (plain < 0 || plans.get(plain).status() != AutomaticCraftingPlan.Status.READY) continue;
      charge();
      var base =
          kernel.expand(source, actions.get(plain), GoalDirectedPlanner.ADDITION_OUTCOME_LIMIT);
      for (int i = 0; i < inventory.size(); i++) {
        var action = actions.get(i);
        if (action.currency() != currency
            || action.purpose() != AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION
            || plans.get(i).status() != AutomaticCraftingPlan.Status.READY) continue;
        charge();
        var other =
            i == plain
                ? base
                : kernel.expand(source, action, GoalDirectedPlanner.ADDITION_OUTCOME_LIMIT);
        boolean complete =
            base.status() == AutomaticCraftingKernel.Status.COMPLETE
                && other.status() == AutomaticCraftingKernel.Status.COMPLETE;
        var good =
            base.outcomes().stream().filter(o -> predicates.advances(source, o.item())).toList();
        var byItem = new HashMap<ItemState, Fraction>();
        other.outcomes().forEach(o -> byItem.put(o.item(), o.probability()));
        boolean dominates =
            !good.isEmpty()
                && good.stream()
                    .allMatch(
                        o -> {
                          var p = byItem.get(o.item());
                          return p != null
                              && p.numerator()
                                      .multiply(o.probability().denominator())
                                      .compareTo(
                                          o.probability().numerator().multiply(p.denominator()))
                                  >= 0;
                        });
        additions.add(
            new AdditionFact(
                id(plain),
                id(i),
                plans.get(plain).branches().equals(plans.get(i).branches())
                    && plans.get(plain).uniformCandidates() == plans.get(i).uniformCandidates(),
                complete,
                complete
                    && base.outcomes().stream()
                        .allMatch(
                            o ->
                                evaluate.apply(o.item()).goal()
                                    == com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status
                                        .MATCH)
                    && other.outcomes().stream()
                        .allMatch(
                            o ->
                                evaluate.apply(o.item()).goal()
                                    == com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status
                                        .MATCH),
                complete && dominates,
                complete ? score(source, base) : null,
                complete ? score(source, other) : null));
      }
    }
    charge();
    return new Facts(
        new Context(
            source,
            before,
            initialStage,
            before.goal() == com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status.MATCH
                && !(AutomaticCraftingKernel.hasQualityCap(source)
                    && kernel.workedQuality(source, abstractQuality) < 40),
            predicates.acquiredMutableTarget(source),
            AutomaticCraftingKernel.mutableAffixCount(source),
            kernel.initialChaosOnly(),
            kernel.qualityGoal(),
            kernel.workedQuality(source, abstractQuality),
            AutomaticCraftingKernel.hasQualityCap(source),
            kernel.baseQualitySupported(source)),
        List.copyOf(facts),
        List.copyOf(pairs),
        List.copyOf(additions));
  }

  private Fraction score(ItemState source, AutomaticCraftingKernel.Result result) {
    return result.outcomes().stream()
        .filter(o -> predicates.advances(source, o.item()))
        .map(AutomaticCraftingKernel.Outcome::probability)
        .reduce(Fraction.ZERO, Fraction::add);
  }

  private static String id(int index) {
    return "a" + index;
  }
}
