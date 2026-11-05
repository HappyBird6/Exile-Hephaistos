package com.poe2craft.craftingsupport.goal.domain;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.Function;

/**
 * Exact finite oracle for externally proven joint outcomes, not a production game roll model. Full
 * ItemState keys retain numeric rolls, family identities, rarity, layer and quality. Joint tuples
 * are indivisible: no product-of-marginals or occupied/qualified merging.
 */
public final class ExactNumericDistribution {
  private ExactNumericDistribution() {}

  public record Outcome(ItemState item, Fraction probability) {}

  public record Bounds(Fraction success, Fraction failure, Fraction unresolved, boolean complete) {
    public Fraction upper() {
      return success.add(unresolved);
    }
  }

  /** Budgets produce unresolved mass only inside a validated normalized finite kernel. */
  public static Bounds firstHit(
      ItemState root,
      List<Function<ItemState, List<Outcome>>> steps,
      Function<ItemState, Status> evaluation,
      int maxEvaluations) {
    if (maxEvaluations < 1)
      throw new IllegalArgumentException("Positive evaluation budget required");
    var frontier = new LinkedHashMap<ItemState, Fraction>();
    frontier.put(root, Fraction.ONE);
    var success = Fraction.ZERO;
    var failure = Fraction.ZERO;
    var unresolved = Fraction.ZERO;
    int evaluated = 0;
    for (int depth = 0; depth <= steps.size(); depth++) {
      var next = new LinkedHashMap<ItemState, Fraction>();
      for (var entry : frontier.entrySet()) {
        if (evaluated++ >= maxEvaluations) {
          unresolved = unresolved.add(entry.getValue());
          continue;
        }
        var status = evaluation.apply(entry.getKey());
        if (status == Status.MATCH) {
          success = success.add(entry.getValue());
          continue;
        }
        if (status == Status.UNKNOWN || status == Status.UNSUPPORTED) {
          unresolved = unresolved.add(entry.getValue());
          continue;
        }
        if (depth == steps.size()) {
          failure = failure.add(entry.getValue());
          continue;
        }
        var outcomes = steps.get(depth).apply(entry.getKey());
        var total = Fraction.ZERO;
        for (var outcome : outcomes) total = total.add(outcome.probability());
        if (!total.equals(Fraction.ONE))
          throw new IllegalArgumentException("Kernel mass must equal one");
        for (var outcome : outcomes)
          next.merge(
              outcome.item(), entry.getValue().multiply(outcome.probability()), Fraction::add);
      }
      frontier = next;
    }
    return new Bounds(success, failure, unresolved, unresolved.equals(Fraction.ZERO));
  }
}
