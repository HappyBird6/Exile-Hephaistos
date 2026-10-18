package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
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

  public List<Choice> choices(ItemState source) {
    var progress = evaluate.apply(source);
    if (progress.goal() == Status.UNKNOWN
        || progress.goal() == Status.UNSUPPORTED
        || progress.entries().stream()
            .anyMatch(s -> s == Status.UNKNOWN || s == Status.UNSUPPORTED)) return List.of();
    if (progress.goal() == Status.MATCH && AutomaticCraftingKernel.hasQualityCap(source))
      return inventory.stream()
          .filter(a -> a.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT)
          .map(a -> new Choice(List.of(), List.of(a)))
          .toList();
    var result = new LinkedHashSet<Choice>();
    var prerequisites = new ArrayList<AutomaticCraftingAction>();
    for (var action : inventory) {
      if (action.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT) {
        if (AutomaticCraftingKernel.hasQualityCap(source))
          result.add(new Choice(List.of(), List.of(action)));
        continue;
      }
      var plan = kernel.plan(source, action);
      if (plan.status() != AutomaticCraftingPlan.Status.READY) continue;
      if (action.purpose() == AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL
          || action.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION) {
        if (action.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION
            && source.explicits().size() < 2) continue;
        prerequisites.add(action);
      } else if (kernel.representatives(plan).stream().anyMatch(t -> advances(source, t))) {
        result.add(new Choice(List.of(), List.of(action)));
      }
    }
    // Preparations are admitted only with an eligible, goal-acquiring successor, never all pairs.
    for (var preparation : prerequisites) {
      // If this same addition already acquires a missing goal, let its checkpoint own
      // the next step. Hiding that acquisition inside a prep pair duplicates the graph
      // and admits unrelated filler states before the useful target is selected.
      if (preparation.purpose() != AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION
          && result.stream()
              .anyMatch(
                  c ->
                      c.initial().isEmpty()
                          && c.cycle().getFirst().currency() == preparation.currency()
                          && c.cycle().getFirst().purpose()
                              == AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION)) continue;
      var prepared = kernel.representatives(kernel.plan(source, preparation));
      for (var acquisition : inventory) {
        if (acquisition.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT) continue;
        boolean tierPreparation =
            preparation.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION;
        if (tierPreparation
            && (acquisition.currency() != WorkbenchCurrency.CHAOS
                || !acquisition.omens().contains(WorkbenchOmen.WHITTLING)
                || protectedRemoval(source, kernel.plan(source, acquisition)))) continue;
        if (acquisition.purpose() != AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION
            && acquisition.currency().replacementModifiers().isEmpty()) continue;
        boolean useful = false;
        for (var state : prepared) {
          if (tierPreparation
              && (!preserves(progress, state)
                  || matched(evaluate.apply(state)) != matched(progress))) continue;
          var next = kernel.plan(state, acquisition);
          if (next.status() == AutomaticCraftingPlan.Status.READY
              && (!tierPreparation || protectedRemoval(source, next))
              && kernel.representatives(next).stream().anyMatch(t -> advances(source, t))) {
            useful = true;
            break;
          }
        }
        if (useful) result.add(new Choice(List.of(preparation), List.of(acquisition)));
      }
    }
    return List.copyOf(result);
  }
}
