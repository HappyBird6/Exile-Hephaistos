package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import java.util.*;
import java.util.function.Function;

/** Finite-observation Markov calculation. S must be the full immutable transition state. */
public final class ExactMethodRunner<S, A> {
  public static final String VERSION = "method-cycle-markov-v2";

  public enum Exit {
    GOAL,
    CHECKPOINT,
    LOSS,
    DEAD,
    UNRESOLVED
  }

  public record Outcome<S>(S state, Fraction probability) {
    public Outcome {
      Objects.requireNonNull(state);
      ExactMethodRunner.probability(probability);
    }
  }

  /** Missing support is explicit mass, not a failure or an omitted display row. */
  public record Kernel<S>(List<Outcome<S>> outcomes, Fraction unresolved, boolean unavailable) {
    public Kernel {
      outcomes = List.copyOf(outcomes);
      probability(unresolved);
      var total = unresolved;
      for (var outcome : outcomes) total = total.add(outcome.probability());
      if (unavailable
          ? !outcomes.isEmpty() || !unresolved.equals(Fraction.ZERO)
          : !total.equals(Fraction.ONE)) {
        throw new IllegalArgumentException("Kernel must conserve mass");
      }
    }
  }

  @FunctionalInterface
  public interface Transition<S, A> {
    Kernel<S> apply(S state, A action);
  }

  /** An action selector may branch on the exact result of the preceding primitive action. */
  public record Template<S, A>(
      String id, List<Function<S, A>> initial, List<Function<S, A>> cycle) {
    public Template {
      if (id == null || id.isBlank() || cycle.isEmpty())
        throw new IllegalArgumentException("Template identity and nonempty cycle required");
      initial = List.copyOf(initial);
      cycle = List.copyOf(cycle);
    }
  }

  public record Execution<S>(S state, int phase) {}

  /** Usage is expected actual consumption, including paths absorbed before the cycle ends. */
  public record Observation<S, A>(
      long cycles,
      Map<Exit, Map<S, Fraction>> exits,
      Map<S, Fraction> active,
      Map<A, Fraction> initialUses,
      Map<A, Fraction> cycleUses) {
    public Fraction mass() {
      var total = Fraction.ZERO;
      for (var distribution : exits.values())
        for (var value : distribution.values()) total = total.add(value);
      for (var value : active.values()) total = total.add(value);
      return total;
    }
  }

  private final Template<S, A> template;
  private final Transition<S, A> transition;
  private final Function<S, Optional<Exit>> classify;
  private final Map<Exit, Map<S, Fraction>> exits = new EnumMap<>(Exit.class);
  private final Map<A, Fraction> initialUses = new LinkedHashMap<>();
  private final Map<A, Fraction> cycleUses = new LinkedHashMap<>();
  private LinkedHashMap<Execution<S>, Fraction> pending = new LinkedHashMap<>();
  private final Map<S, Fraction> boundary = new LinkedHashMap<>();
  private boolean initializing;
  private boolean atBoundary;
  private long cycles;

  public ExactMethodRunner(
      S root,
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classify) {
    this.template = Objects.requireNonNull(template);
    this.transition = Objects.requireNonNull(transition);
    this.classify = Objects.requireNonNull(classify);
    for (var exit : Exit.values()) exits.put(exit, new LinkedHashMap<>());
    initializing = !template.initial().isEmpty();
    var terminal = classify.apply(root);
    if (terminal.isPresent()) {
      absorb(terminal.get(), root, Fraction.ONE);
      initializing = false;
      atBoundary = true;
    } else if (initializing) {
      pending.put(new Execution<>(root, 0), Fraction.ONE);
    } else {
      boundary.put(root, Fraction.ONE);
      atBoundary = true;
    }
  }

  /** Start one additional cycle. Observation 100 means 100 cycles after initial preparation. */
  public void beginCycle() {
    if (!atBoundary)
      throw new IllegalStateException("Finish current cycle or initialization first");
    if (cycles == Long.MAX_VALUE) throw new IllegalStateException("Cycle counter overflow");
    boundary.forEach((state, mass) -> pending.put(new Execution<>(state, 0), mass));
    boundary.clear();
    atBoundary = false;
    if (pending.isEmpty()) finishBoundary();
  }

  /**
   * One pending full-state/phase expansion. Callers time-slice between steps and preserve cursor.
   */
  public void step() {
    if (atBoundary) throw new IllegalStateException("Begin a cycle before stepping");
    var entry = pending.entrySet().iterator().next();
    var execution = entry.getKey();
    var mass = entry.getValue();
    var actions = initializing ? template.initial() : template.cycle();
    var action = Objects.requireNonNull(actions.get(execution.phase()).apply(execution.state()));
    // Compute and validate before committing the cursor. Failed computation can safely be retried.
    var kernel = transition.apply(execution.state(), action);
    var classified = new ArrayList<Optional<Exit>>();
    for (var outcome : kernel.outcomes()) classified.add(classify.apply(outcome.state()));
    pending.remove(execution);
    if (kernel.unavailable()) {
      absorb(Exit.DEAD, execution.state(), mass);
    } else {
      (initializing ? initialUses : cycleUses).merge(action, mass, Fraction::add);
      absorb(Exit.UNRESOLVED, execution.state(), mass.multiply(kernel.unresolved()));
      for (int i = 0; i < kernel.outcomes().size(); i++) {
        var outcome = kernel.outcomes().get(i);
        var weight = mass.multiply(outcome.probability());
        if (weight.equals(Fraction.ZERO)) continue;
        var terminal = classified.get(i);
        // First-hit precedes the next primitive: an Exalted hit never pays for a later Annulment.
        if (terminal.isPresent()) absorb(terminal.get(), outcome.state(), weight);
        else if (execution.phase() + 1 == actions.size())
          boundary.merge(outcome.state(), weight, Fraction::add);
        else
          pending.merge(
              new Execution<>(outcome.state(), execution.phase() + 1), weight, Fraction::add);
      }
    }
    if (pending.isEmpty()) finishBoundary();
  }

  private void finishBoundary() {
    if (initializing) initializing = false;
    else cycles++;
    atBoundary = true;
  }

  public boolean atBoundary() {
    return atBoundary;
  }

  public Observation<S, A> observation() {
    if (!atBoundary) throw new IllegalStateException("Partial cycle is not an observation");
    var saved = new EnumMap<Exit, Map<S, Fraction>>(Exit.class);
    exits.forEach((exit, distribution) -> saved.put(exit, Map.copyOf(distribution)));
    var result =
        new Observation<>(
            cycles,
            Map.copyOf(saved),
            Map.copyOf(boundary),
            Map.copyOf(initialUses),
            Map.copyOf(cycleUses));
    if (!result.mass().equals(Fraction.ONE))
      throw new IllegalStateException("Method mass not conserved");
    return result;
  }

  private void absorb(Exit exit, S state, Fraction mass) {
    if (!mass.equals(Fraction.ZERO)) exits.get(exit).merge(state, mass, Fraction::add);
  }

  private static void probability(Fraction value) {
    Objects.requireNonNull(value);
    if (value.numerator().compareTo(value.denominator()) > 0)
      throw new IllegalArgumentException("Probability exceeds one");
  }
}
