package com.poe2craft.workbench.repeat;

import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * Sequential manual Chaos uses through the existing Workbench apply contract. Supply, for example,
 * {@code new ChaosRepeatRunner(workbenchService::apply)}. Matching syntax and display language
 * belong to the caller; this runner neither formats modifiers nor starts background jobs.
 */
public final class ChaosRepeatRunner {
  private final Apply apply;

  public ChaosRepeatRunner(Apply apply) {
    this.apply = Objects.requireNonNull(apply);
  }

  /**
   * The adapter must retain the existing atomic single-use semantics, including omen consumption.
   */
  @FunctionalInterface
  public interface Apply {
    WorkbenchSimulator.Result apply(
        ItemState state, WorkbenchCurrency action, Set<String> activeOmens, RandomGenerator random);
  }

  public Outcome run(
      ItemState state,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      int maximumUses,
      RandomGenerator random,
      Predicate<ItemState> matcher) {
    return run(state, action, activeOmens, maximumUses, random, matcher, () -> false);
  }

  /**
   * Records only completed uses. Matching begins after the first completed use, even when the input
   * already matches. Stop is observed between uses, including before the first; it cannot interrupt
   * apply or matching. A match on the last allowed use takes precedence over the limit.
   *
   * <p>The outcome is an internal factual result, not an API response or a decision to commit
   * partial work to the user's item. Runtime failures preserve completed work without retaining
   * exception messages or causes. Fatal JVM errors are not intercepted. Memory does not grow with
   * use count: only the last returned attempt, last applied result and counts by consumed omen are
   * retained.
   */
  public Outcome run(
      ItemState state,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      int maximumUses,
      RandomGenerator random,
      Predicate<ItemState> matcher,
      BooleanSupplier stopRequested) {
    if (state == null
        || action == null
        || activeOmens == null
        || random == null
        || matcher == null
        || stopRequested == null) throw new IllegalArgumentException("Repeat inputs are required");
    if (action != WorkbenchCurrency.CHAOS
        && action != WorkbenchCurrency.GREATER_CHAOS
        && action != WorkbenchCurrency.PERFECT_CHAOS)
      throw new IllegalArgumentException("Only Chaos currencies can repeat");
    if (maximumUses <= 0) throw new IllegalArgumentException("Maximum uses must be positive");

    var remaining = Set.copyOf(activeOmens);
    var consumed = new HashMap<String, Integer>();
    int completed = 0;
    WorkbenchSimulator.Result last = null;
    WorkbenchSimulator.Result lastApplied = null;
    FailureStage stage = FailureStage.STOP_SIGNAL;
    try {
      while (completed < maximumUses) {
        stage = FailureStage.STOP_SIGNAL;
        if (stopRequested.getAsBoolean())
          return new Outcome(
              state,
              completed,
              consumed,
              remaining,
              last,
              lastApplied,
              Termination.STOP_REQUESTED,
              null);

        stage = FailureStage.APPLY;
        last = Objects.requireNonNull(apply.apply(state, action, remaining, random));
        if (!last.applied())
          return new Outcome(
              state,
              completed,
              consumed,
              remaining,
              last,
              lastApplied,
              Termination.INAPPLICABLE,
              null);

        lastApplied = last;
        state = last.state();
        completed++;
        remaining = Set.copyOf(last.remainingOmens());
        for (var id : last.consumedOmens()) consumed.merge(id, 1, Integer::sum);

        stage = FailureStage.MATCHER;
        if (matcher.test(state))
          return new Outcome(
              state, completed, consumed, remaining, last, lastApplied, Termination.MATCH, null);
      }
      return new Outcome(
          state, completed, consumed, remaining, last, lastApplied, Termination.LIMIT, null);
    } catch (RuntimeException failure) {
      return new Outcome(
          state, completed, consumed, remaining, last, lastApplied, Termination.ERROR, stage);
    }
  }

  public enum Termination {
    MATCH,
    LIMIT,
    INAPPLICABLE,
    STOP_REQUESTED,
    ERROR
  }

  public enum FailureStage {
    STOP_SIGNAL,
    APPLY,
    MATCHER
  }

  /**
   * lastResult is the last returned single-use result, including an unapplied final attempt; it is
   * null if apply never returned. lastAppliedResult is the last result with applied=true, or null
   * when completedUses is zero. It witnesses the final state even if a later attempt is
   * inapplicable or throws. Each result's events and assumptions describe that one use only. The
   * final state, completedUses and consumedOmens describe all completed uses, including before
   * ERROR/STOP.
   */
  public record Outcome(
      ItemState state,
      int completedUses,
      Map<String, Integer> consumedOmens,
      Set<String> remainingOmens,
      WorkbenchSimulator.Result lastResult,
      WorkbenchSimulator.Result lastAppliedResult,
      Termination termination,
      FailureStage failureStage) {
    public Outcome {
      consumedOmens = Map.copyOf(consumedOmens);
      remainingOmens = Set.copyOf(remainingOmens);
    }
  }
}
