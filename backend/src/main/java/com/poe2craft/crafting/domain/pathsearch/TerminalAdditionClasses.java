package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.Exit;
import com.poe2craft.item.*;
import java.util.*;
import java.util.function.Function;

/**
 * A deliberately bounded proof for an absorbing, addition-only suffix of a plan. Every eligible
 * action and every scalar outcome is compared, including its exact mass. Concrete incoming items
 * remain separate transport inputs; no representative is averaged.
 */
public final class TerminalAdditionClasses {
  public record Row(ItemState addition, Exit exit, Fraction probability) {}

  public record Law(AutomaticCraftingAction action, List<Row> outcomes) {}

  public record Key(
      ItemState context,
      List<AutomaticCraftingKernel.RetainedModifierRole> roles,
      boolean quality40,
      AutomaticMethodGraph.Progress progress,
      List<Law> laws) {}

  private TerminalAdditionClasses() {}

  public static Optional<Key> prove(
      ItemState item,
      boolean quality40,
      AutomaticMethodGraph.Progress progress,
      List<GoalDirectedPlanner.Choice> choices,
      AutomaticCraftingKernel kernel,
      Function<ItemState, Optional<Exit>> classify,
      long outcomeBudget) {
    return prove(
        item, quality40, progress, choices, kernel, classify, outcomeBudget, ignored -> {});
  }

  public static Optional<Key> prove(
      ItemState item,
      boolean quality40,
      AutomaticMethodGraph.Progress progress,
      List<GoalDirectedPlanner.Choice> choices,
      AutomaticCraftingKernel kernel,
      Function<ItemState, Optional<Exit>> classify,
      long outcomeBudget,
      java.util.function.LongConsumer recordWork) {
    if (choices.isEmpty() || AutomaticCraftingKernel.hasQualityCap(item)) return Optional.empty();
    // This is only a proof-work cap. An unproved state keeps its ordinary exact execution.
    long remainingProofRows = Math.min(outcomeBudget, 512);
    var laws = new ArrayList<Law>();
    for (var choice : choices) {
      if (!choice.initial().isEmpty() || choice.cycle().size() != 1) return Optional.empty();
      var action = choice.cycle().getFirst();
      if (action.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT
          || action.purpose() == AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL
          || action.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION)
        return Optional.empty();
      var plan = kernel.plan(item, action);
      if (plan.status() != AutomaticCraftingPlan.Status.READY
          || plan.branches().stream()
              .anyMatch(b -> !b.remaining().explicits().equals(item.explicits())))
        return Optional.empty();
      if (remainingProofRows <= 0) return Optional.empty();
      var result = kernel.expand(item, action, remainingProofRows);
      recordWork.accept(result.outcomes().size());
      remainingProofRows -= result.outcomes().size();
      if (result.status() != AutomaticCraftingKernel.Status.COMPLETE) return Optional.empty();
      var rows = new ArrayList<Row>();
      var loss = Fraction.ZERO;
      for (var outcome : result.outcomes()) {
        if (AutomaticCraftingKernel.hasQualityCap(outcome.item())) return Optional.empty();
        var added = new ArrayList<>(outcome.item().explicits());
        for (var existing : item.explicits()) if (!added.remove(existing)) return Optional.empty();
        if (added.size() != 1) return Optional.empty();
        var exit = classify.apply(outcome.item()).orElse(Exit.LOSS);
        if (exit != Exit.GOAL && exit != Exit.LOSS) return Optional.empty();
        if (exit == Exit.LOSS) loss = loss.add(outcome.probability());
        else rows.add(new Row(withExplicits(outcome.item(), added), exit, outcome.probability()));
      }
      // Failed items are absorbed and never continue; only their exact total mass is observable.
      if (!loss.equals(Fraction.ZERO)) rows.add(new Row(null, Exit.LOSS, loss));
      laws.add(new Law(action, List.copyOf(rows)));
    }
    return Optional.of(
        new Key(
            withExplicits(item, List.of()),
            kernel.retainedModifierRoles(item),
            quality40,
            progress,
            List.copyOf(laws)));
  }

  /** Rebuild the exact member's result, preserving its own tier and scalar values. */
  public static ItemState transport(ItemState representative, ItemState actual, ItemState result) {
    var additions = new ArrayList<>(result.explicits());
    for (var existing : representative.explicits())
      if (!additions.remove(existing))
        throw new IllegalArgumentException("Not an addition-only result");
    additions.addAll(actual.explicits());
    return withExplicits(result, additions);
  }

  private static ItemState withExplicits(ItemState item, List<ModifierInstance> modifiers) {
    return new ItemState(
        item.snapshotId(),
        item.baseItemId(),
        item.itemLevel(),
        item.rarity(),
        item.implicits(),
        modifiers,
        item.conditions(),
        item.augmentSockets(),
        item.catalystQuality());
  }
}
