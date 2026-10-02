package com.poe2craft.crafting.domain;

import com.poe2craft.item.ItemCatalog;
import java.util.*;

/** Exhaustive modifier-ID distribution. Numeric roll outcomes are marginalized, never sampled. */
public final class AdditionTransitions {
  private final AdditionRules rules;

  public AdditionTransitions(ItemCatalog catalog) {
    rules = new AdditionRules(catalog);
  }

  public Result transition(StateBucket state, WorkbenchCurrency action, Set<String> activeOmens) {
    var plan = rules.plan(state, action, activeOmens);
    if (!plan.available())
      return new Result(
          WorkbenchSimulator.RULE_VERSION,
          WorkbenchSimulator.LEDGER_VERSION,
          state,
          action,
          false,
          plan.reason(),
          List.of(),
          List.of(),
          plan.remainingOmens());
    long total = 0;
    for (var candidate : plan.candidates()) total = Math.addExact(total, candidate.weight());
    final long denominator = total;
    var outcomes =
        plan.candidates().stream()
            .map(
                candidate -> {
                  var ids = new ArrayList<>(state.modifierIds());
                  ids.add(candidate.id());
                  return new Outcome(
                      plan.upgraded().with(plan.upgraded().rarity(), ids),
                      candidate.id(),
                      candidate.weight(),
                      denominator);
                })
            .toList();
    return new Result(
        WorkbenchSimulator.RULE_VERSION,
        WorkbenchSimulator.LEDGER_VERSION,
        state,
        action,
        true,
        "",
        outcomes,
        plan.consumedOmens(),
        plan.remainingOmens());
  }

  /** Integer numerator/denominator preserve the exact per-edge published-weight ratio. */
  public record Outcome(StateBucket state, String addedModifierId, long weight, long totalWeight) {
    public double probability() {
      return (double) weight / totalWeight;
    }
  }

  public record Result(
      String ruleVersion,
      String ledgerVersion,
      StateBucket source,
      WorkbenchCurrency action,
      boolean available,
      String reason,
      List<Outcome> outcomes,
      List<String> consumedOmens,
      List<String> remainingOmens) {
    public Result {
      outcomes = List.copyOf(outcomes);
      consumedOmens = List.copyOf(consumedOmens);
      remainingOmens = List.copyOf(remainingOmens);
    }
  }
}
