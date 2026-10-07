package com.poe2craft.crafting.domain.goalfilter;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.item.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.function.Function;

/** Finite open-loop addition sequences, with first success absorbed before the next action. */
public final class NumericAdditionSearch {
  public record ExactMass(
      Fraction success,
      Fraction failure,
      Fraction unresolved,
      Fraction terminalFailure,
      Fraction illegalActionFailure) {}

  public record Comparison(
      List<String> sequence,
      double successLower,
      double successUpper,
      double failureProbability,
      double unresolvedProbability,
      boolean complete,
      ExactMass exactMass) {}

  public record Result(List<Comparison> comparisons, boolean complete, int totalSequences) {}

  public static final class Budget {
    private final int maxStates, maxEdges;
    private final long started = System.nanoTime(), duration;
    private int states, edges;

    public Budget(int maxStates, int maxEdges, int maxMillis) {
      this.maxStates = maxStates;
      this.maxEdges = maxEdges;
      duration = (long) maxMillis * 1_000_000;
    }

    public boolean exhausted() {
      return states >= maxStates || edges >= maxEdges || timedOut();
    }

    private boolean timedOut() {
      return System.nanoTime() - started >= duration;
    }

    boolean state() {
      if (states >= maxStates || timedOut()) return false;
      states++;
      return true;
    }

    boolean edge() {
      if (edges >= maxEdges || timedOut()) return false;
      edges++;
      return true;
    }
  }

  public Result compare(
      ItemCatalog catalog, ItemState root, Function<ItemState, Status> evaluate, Budget budget) {
    var sequences = sequences(root);
    var kernel = new NumericAdditionKernel(catalog);
    var comparisons = new ArrayList<Comparison>();
    for (var sequence : sequences) {
      if (budget.exhausted()) break;
      comparisons.add(firstHit(root, sequence, kernel, evaluate, budget));
    }
    boolean complete =
        comparisons.size() == sequences.size()
            && comparisons.stream().allMatch(Comparison::complete);
    comparisons.sort((a, b) -> compareFractions(b.exactMass().success(), a.exactMass().success()));
    return new Result(List.copyOf(comparisons), complete, sequences.size());
  }

  public Comparison firstHit(
      ItemState root,
      List<WorkbenchCurrency> sequence,
      NumericAdditionKernel kernel,
      Function<ItemState, Status> evaluate,
      Budget budget) {
    var frontier = new LinkedHashMap<ItemState, Fraction>();
    frontier.put(root, Fraction.ONE);
    var success = Fraction.ZERO;
    var terminal = Fraction.ZERO;
    var illegal = Fraction.ZERO;
    var unresolved = Fraction.ZERO;
    for (int depth = 0; depth <= sequence.size(); depth++) {
      var next = new LinkedHashMap<ItemState, Fraction>();
      for (var entry : frontier.entrySet()) {
        var mass = entry.getValue();
        if (!budget.state()) {
          unresolved = unresolved.add(mass);
          continue;
        }
        var status = evaluate.apply(entry.getKey());
        if (status == Status.MATCH) {
          success = success.add(mass);
          continue;
        }
        if (status != Status.NO_MATCH) {
          unresolved = unresolved.add(mass);
          continue;
        }
        if (depth == sequence.size()) {
          terminal = terminal.add(mass);
          continue;
        }
        var expanded = kernel.expand(entry.getKey(), sequence.get(depth), budget::edge);
        if (!expanded.available()) {
          illegal = illegal.add(mass);
          continue;
        }
        unresolved = unresolved.add(mass.multiply(expanded.unresolved()));
        for (var outcome : expanded.outcomes())
          next.merge(outcome.item(), mass.multiply(outcome.probability()), Fraction::add);
      }
      frontier = next;
    }
    var failure = terminal.add(illegal);
    if (!success.add(failure).add(unresolved).equals(Fraction.ONE))
      throw new IllegalStateException("Search mass must equal one");
    return new Comparison(
        sequence.stream().map(Enum::name).toList(),
        decimal(success),
        decimal(success.add(unresolved)),
        decimal(failure),
        decimal(unresolved),
        unresolved.equals(Fraction.ZERO),
        new ExactMass(success, failure, unresolved, terminal, illegal));
  }

  private static int compareFractions(Fraction a, Fraction b) {
    return a.numerator()
        .multiply(b.denominator())
        .compareTo(b.numerator().multiply(a.denominator()));
  }

  private static double decimal(Fraction f) {
    return new BigDecimal(f.numerator())
        .divide(new BigDecimal(f.denominator()), MathContext.DECIMAL128)
        .doubleValue();
  }

  /** Every prefix (including Stop) is a policy; no removal, recovery, or repeat cycles. */
  public static List<List<WorkbenchCurrency>> sequences(ItemState root) {
    var result = new ArrayList<List<WorkbenchCurrency>>();
    append(result, List.of(), root.rarity(), root.explicits().size(), root.itemLevel());
    result.sort(Comparator.comparingInt(List::size));
    return List.copyOf(result);
  }

  private static void append(
      List<List<WorkbenchCurrency>> result,
      List<WorkbenchCurrency> prefix,
      ItemState.Rarity rarity,
      int count,
      int level) {
    result.add(List.copyOf(prefix));
    if (count >= 6) return;
    for (var action : WorkbenchCurrency.values()) {
      if (!AdditionRules.isAddition(action) || action.minimumModifierLevel() > level) continue;
      boolean legal =
          switch (action.baseAction()) {
            case TRANSMUTATION -> rarity == ItemState.Rarity.NORMAL;
            case AUGMENTATION -> rarity == ItemState.Rarity.MAGIC && count < 2;
            case REGAL -> rarity == ItemState.Rarity.MAGIC;
            case EXALTED -> rarity == ItemState.Rarity.RARE;
            default -> false;
          };
      if (!legal) continue;
      var next = new ArrayList<>(prefix);
      next.add(action);
      var upgraded =
          action.baseAction() == CraftingAction.TRANSMUTATION
              ? ItemState.Rarity.MAGIC
              : action.baseAction() == CraftingAction.REGAL ? ItemState.Rarity.RARE : rarity;
      append(result, next, upgraded, count + 1, level);
    }
  }
}
