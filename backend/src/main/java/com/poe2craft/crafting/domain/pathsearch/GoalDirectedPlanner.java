package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.item.ItemState;
import java.util.*;
import java.util.function.Function;

/**
 * Finite milestone planner. A method acquires a missing predicate, or prepares that acquisition.
 */
public final class GoalDirectedPlanner {
  public record Choice(
      List<AutomaticCraftingAction> initial, List<AutomaticCraftingAction> cycle) {}

  private final AutomaticCraftingKernel kernel;
  private final Function<ItemState, AutomaticMethodGraph.Progress> evaluate;
  private final List<AutomaticCraftingAction> inventory;

  public GoalDirectedPlanner(
      AutomaticCraftingKernel kernel,
      Function<ItemState, AutomaticMethodGraph.Progress> evaluate,
      List<AutomaticCraftingAction> inventory) {
    this.kernel = kernel;
    this.evaluate = evaluate;
    this.inventory = List.copyOf(inventory);
  }

  public static long matched(AutomaticMethodGraph.Progress progress) {
    return progress.entries().stream().filter(s -> s == Status.MATCH).count();
  }

  private boolean advances(ItemState source, ItemState target) {
    var before = evaluate.apply(source);
    var after = evaluate.apply(target);
    return after.goal() == Status.MATCH
        || (preserves(before, target) && matched(after) > matched(before));
  }

  private boolean preserves(AutomaticMethodGraph.Progress before, ItemState target) {
    var after = evaluate.apply(target);
    if (before.entries().size() != after.entries().size()) return false;
    for (int i = 0; i < before.entries().size(); i++)
      if (before.entries().get(i) == Status.MATCH && after.entries().get(i) != Status.MATCH)
        return false;
    return true;
  }

  private boolean protectedRemoval(ItemState source, AutomaticCraftingPlan plan) {
    var before = evaluate.apply(source);
    return plan.status() == AutomaticCraftingPlan.Status.READY
        && plan.branches().stream().anyMatch(b -> preserves(before, b.remaining()));
  }

  private boolean guaranteedProtectedRemoval(ItemState source, AutomaticCraftingPlan plan) {
    var before = evaluate.apply(source);
    return plan.status() == AutomaticCraftingPlan.Status.READY
        && !plan.branches().isEmpty()
        && plan.branches().stream().allMatch(b -> preserves(before, b.remaining()));
  }

  private void preferAddition(
      ItemState source, LinkedHashSet<Choice> choices, WorkbenchCurrency currency) {
    var direct =
        choices.stream()
            .filter(
                c ->
                    c.initial().isEmpty()
                        && c.cycle().size() == 1
                        && c.cycle().getFirst().currency() == currency)
            .toList();
    var plain =
        direct.stream()
            .filter(c -> c.cycle().getFirst().omens().isEmpty())
            .findFirst()
            .orElse(null);
    if (plain == null) return;
    var laws = new HashMap<Choice, AutomaticCraftingKernel.Result>();
    var basePlan = kernel.plan(source, plain.cycle().getFirst());
    for (var omen : direct) {
      if (omen == plain) continue;
      var plan = kernel.plan(source, omen.cycle().getFirst());
      if (basePlan.branches().equals(plan.branches())
          && basePlan.uniformCandidates() == plan.uniformCandidates()) {
        choices.remove(omen);
        continue;
      }
      var ordinary =
          laws.computeIfAbsent(plain, c -> kernel.expand(source, c.cycle().getFirst(), 5000));
      var improved =
          laws.computeIfAbsent(omen, c -> kernel.expand(source, c.cycle().getFirst(), 5000));
      if (ordinary.status() != AutomaticCraftingKernel.Status.COMPLETE
          || improved.status() != AutomaticCraftingKernel.Status.COMPLETE) continue;
      // Once both complete laws finish the whole objective with certainty, the extra
      // omen cannot improve this goal or a successor search. Their junk-affix laws
      // need not be identical; this is a goal/cost dominance, not item equivalence.
      if (ordinary.outcomes().stream()
              .allMatch(o -> evaluate.apply(o.item()).goal() == Status.MATCH)
          && improved.outcomes().stream()
              .allMatch(o -> evaluate.apply(o.item()).goal() == Status.MATCH)) {
        choices.remove(omen);
        continue;
      }
      var good = ordinary.outcomes().stream().filter(o -> advances(source, o.item())).toList();
      if (good.isEmpty()) continue;
      var byItem =
          new HashMap<
              ItemState,
              com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction>();
      improved.outcomes().forEach(o -> byItem.put(o.item(), o.probability()));
      boolean dominates =
          good.stream()
              .allMatch(
                  o -> {
                    var probability = byItem.get(o.item());
                    return probability != null
                        && probability
                                .numerator()
                                .multiply(o.probability().denominator())
                                .compareTo(
                                    o.probability().numerator().multiply(probability.denominator()))
                            >= 0;
                  });
      if (dominates) {
        choices.remove(plain);
        break;
      }
    }
    var remaining = direct.stream().filter(choices::contains).toList();
    var scores = new HashMap<Choice, Fraction>();
    for (var choice : remaining) {
      var law =
          laws.computeIfAbsent(choice, c -> kernel.expand(source, c.cycle().getFirst(), 5000));
      if (law.status() != AutomaticCraftingKernel.Status.COMPLETE) return;
      scores.put(
          choice,
          law.outcomes().stream()
              .filter(o -> advances(source, o.item()))
              .map(AutomaticCraftingKernel.Outcome::probability)
              .reduce(Fraction.ZERO, Fraction::add));
    }
    var ranked =
        remaining.stream()
            .sorted(
                (a, b) ->
                    scores
                        .get(b)
                        .numerator()
                        .multiply(scores.get(a).denominator())
                        .compareTo(scores.get(a).numerator().multiply(scores.get(b).denominator())))
            .iterator();
    var ordered = choices.stream().map(c -> scores.containsKey(c) ? ranked.next() : c).toList();
    choices.clear();
    choices.addAll(ordered);
  }

  public List<Choice> choices(ItemState source) {
    var progress = evaluate.apply(source);
    if (progress.goal() == Status.UNKNOWN
        || progress.goal() == Status.UNSUPPORTED
        || progress.entries().stream()
            .anyMatch(s -> s == Status.UNKNOWN || s == Status.UNSUPPORTED)) return List.of();
    // This deterministic, untyped work leaves the item and all affix laws unchanged.
    // Finish it while the cap exists, before branching into affix acquisitions. Crafts
    // retaining the cap commute with this work; removing the cap beforehand is unsupported.
    if (kernel.qualityGoal() > 20
        && progress.entries().getLast() != Status.MATCH
        && AutomaticCraftingKernel.hasQualityCap(source))
      return inventory.stream()
          .filter(a -> a.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT)
          .map(a -> new Choice(List.of(), List.of(a)))
          .toList();
    if (progress.goal() == Status.MATCH && AutomaticCraftingKernel.hasQualityCap(source))
      return inventory.stream()
          .filter(a -> a.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT)
          .map(a -> new Choice(List.of(), List.of(a)))
          .toList();
    var result = new LinkedHashSet<Choice>();
    if (kernel.qualityGoal() > 0 && progress.entries().getLast() != Status.MATCH) {
      if (kernel.qualityGoal() <= 20 && kernel.baseQualitySupported(source))
        result.add(
            new Choice(
                List.of(),
                List.of(
                    new AutomaticCraftingAction(
                        WorkbenchCurrency.ESSENCE_BREACH,
                        Set.of(),
                        AutomaticCurrencyPolicy.Purpose.QUALITY_BASE_CHECKPOINT))));
    }
    var prerequisites = new ArrayList<AutomaticCraftingAction>();
    for (var action : inventory) {
      if (action.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT) {
        if (AutomaticCraftingKernel.hasQualityCap(source)
            && (kernel.qualityGoal() == 0 || progress.entries().getLast() != Status.MATCH))
          result.add(new Choice(List.of(), List.of(action)));
        continue;
      }
      var plan = kernel.plan(source, action);
      if (plan.status() != AutomaticCraftingPlan.Status.READY) continue;
      if (action.purpose() == AutomaticCurrencyPolicy.Purpose.RARITY_UPGRADE) {
        if (kernel.representatives(plan).stream()
            .anyMatch(t -> evaluate.apply(t).goal() == Status.MATCH))
          result.add(new Choice(List.of(), List.of(action)));
        prerequisites.add(action);
        continue;
      }
      if (action.purpose() == AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL
          || action.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION) {
        if (action.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION
            && source.explicits().size() < 2) continue;
        prerequisites.add(action);
      } else if ((action.currency() == WorkbenchCurrency.ESSENCE_BREACH
              && action.purpose() != AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT
              && kernel.qualityGoal() > 20
              && progress.entries().getLast() != Status.MATCH
              && !AutomaticCraftingKernel.hasQualityCap(source))
          || kernel.representatives(plan).stream().anyMatch(t -> advances(source, t))) {
        result.add(new Choice(List.of(), List.of(action)));
      }
    }
    // Preparations are admitted only with an eligible, goal-acquiring successor, never all pairs.
    for (var preparation : prerequisites) {
      var prepared = kernel.representatives(kernel.plan(source, preparation));
      for (var acquisition : inventory) {
        // This is an explicit material policy, not an equivalence of the three laws.
        if (acquisition.currency() == WorkbenchCurrency.CHAOS
            && Set.of(WorkbenchCurrency.EXALTED, WorkbenchCurrency.REGAL)
                .contains(preparation.currency())
            && !preparation.omens().isEmpty()) continue;
        if (acquisition.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT) continue;
        // A direct acquisition does not replace preparation when the follower is
        // unavailable at the source (for example Chaos on an empty rare item).
        if (preparation.purpose() != AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION
            && !(source.explicits().isEmpty() && acquisition.currency() == WorkbenchCurrency.CHAOS)
            && result.stream()
                .anyMatch(
                    c ->
                        c.initial().isEmpty()
                            && c.cycle().getFirst().currency() == preparation.currency()
                            && c.cycle().getFirst().purpose()
                                == AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION)) continue;
        boolean tierPreparation =
            preparation.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION;
        if (tierPreparation
            && (acquisition.currency() != WorkbenchCurrency.CHAOS
                || !acquisition.omens().contains(WorkbenchOmen.WHITTLING)
                || guaranteedProtectedRemoval(source, kernel.plan(source, acquisition)))) continue;
        if (acquisition.purpose() != AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION
            && acquisition.currency().replacementModifiers().isEmpty()) continue;
        boolean useful = false;
        var checkedSuccessors = new HashSet<AutomaticCraftingPlan>();
        for (var state : prepared) {
          if (tierPreparation
              && (!preserves(progress, state)
                  || matched(evaluate.apply(state)) != matched(progress))) continue;
          var next = kernel.plan(state, acquisition);
          if (next.status() == AutomaticCraftingPlan.Status.READY
              && (!tierPreparation || protectedRemoval(source, next))
              && checkedSuccessors.add(next)
              && kernel.representatives(next).stream().anyMatch(t -> advances(source, t))) {
            useful = true;
            break;
          }
        }
        if (useful) result.add(new Choice(List.of(preparation), List.of(acquisition)));
      }
    }
    preferAddition(source, result, WorkbenchCurrency.EXALTED);
    preferAddition(source, result, WorkbenchCurrency.REGAL);
    return List.copyOf(result);
  }
}
