package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import java.util.*;
import java.util.function.Function;

/** Finite-observation Markov calculation. S must be the full immutable transition state. */
public final class ExactMethodRunner<S, A> {
  public static final String VERSION = "method-cycle-markov-compaction-v5";

  /** A law-preserving normalization of one mutually exclusive distribution, never alternatives. */
  public interface Compaction<S> {
    boolean complete();

    void step(int maxInputs);

    Map<S, Fraction> result();
  }

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
    public Observation<S, A> atLaterAbsorbedBoundary(long boundaryCycles) {
      if (!active.isEmpty() || boundaryCycles < cycles || !mass().equals(Fraction.ONE))
        throw new IllegalArgumentException("Only a later absorbed observation may be filled");
      return new Observation<>(boundaryCycles, exits, active, initialUses, cycleUses);
    }

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
  private final Function<Map<S, Fraction>, Compaction<S>> compact;
  private final ArrayDeque<Map<S, Fraction>> compactionQueue = new ArrayDeque<>();
  private Compaction<S> compaction;
  private final Map<Exit, Map<S, Fraction>> exits = new EnumMap<>(Exit.class);
  private final Map<A, Fraction> initialUses = new LinkedHashMap<>();
  private final Map<A, Fraction> cycleUses = new LinkedHashMap<>();
  private LinkedHashMap<Execution<S>, Fraction> pending = new LinkedHashMap<>();
  private final Map<S, Fraction> boundary = new LinkedHashMap<>();
  private boolean initializing;
  private boolean atBoundary;
  private long cycles;
  private Flight flight;
  private final Map<WorkKey, Flight> phaseGroups = new LinkedHashMap<>();
  private boolean phaseReady;
  private long groupedInputs;

  // The job's proven kernel cache supplies the same immutable object only for equal complete
  // transition laws. Reference equality avoids repeatedly hashing a large outcome distribution.
  private final class WorkKey {
    final int phase;
    final A action;
    final Kernel<S> kernel;

    WorkKey(int phase, A action, Kernel<S> kernel) {
      this.phase = phase;
      this.action = action;
      this.kernel = kernel;
    }

    @Override
    public int hashCode() {
      return Objects.hash(phase, action, System.identityHashCode(kernel));
    }

    @Override
    public boolean equals(Object other) {
      if (other == null || other.getClass() != getClass()) return false;
      @SuppressWarnings("unchecked")
      var key = (ExactMethodRunner<S, A>.WorkKey) other;
      return phase == key.phase && action.equals(key.action) && kernel == key.kernel;
    }
  }

  public long groupedInputs() {
    return groupedInputs;
  }

  private final class Flight {
    final Execution<S> execution;
    Fraction mass;
    final Kernel<S> kernel;
    final int phases;
    int cursor;

    Flight(Execution<S> execution, Fraction mass, Kernel<S> kernel, int phases) {
      this.execution = execution;
      this.mass = mass;
      this.kernel = kernel;
      this.phases = phases;
    }
  }

  public ExactMethodRunner(
      S root,
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classify) {
    this(root, template, transition, classify, null);
  }

  public ExactMethodRunner(
      S root,
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classify,
      Function<Map<S, Fraction>, Compaction<S>> compact) {
    this.template = Objects.requireNonNull(template);
    this.transition = Objects.requireNonNull(transition);
    this.classify = Objects.requireNonNull(classify);
    this.compact = compact;
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

  /** A bounded portion of one full-state/phase expansion, retaining its outcome cursor. */
  public void step() {
    step(32);
  }

  public void step(int maxOutcomes) {
    if (maxOutcomes < 1) throw new IllegalArgumentException("Positive outcome slice required");
    if (atBoundary) throw new IllegalStateException("Begin a cycle before stepping");
    if (!compactionQueue.isEmpty()) {
      if (compaction == null) compaction = compact.apply(compactionQueue.getFirst());
      if (!compaction.complete()) compaction.step(maxOutcomes);
      if (compaction.complete()) {
        var target = compactionQueue.removeFirst();
        var result = compaction.result();
        if (result != target) {
          target.clear();
          target.putAll(result);
        }
        compaction = null;
        if (compactionQueue.isEmpty()) endBoundary();
      }
      return;
    }
    if (flight == null && !phaseReady) {
      var entry = pending.entrySet().iterator().next();
      var execution = entry.getKey();
      var mass = entry.getValue();
      var actions = initializing ? template.initial() : template.cycle();
      var action = Objects.requireNonNull(actions.get(execution.phase()).apply(execution.state()));
      // A suspended/failed kernel lookup commits neither cost nor phase progress.
      var kernel = transition.apply(execution.state(), action);
      pending.remove(execution);
      if (kernel.unavailable()) {
        absorb(Exit.DEAD, execution.state(), mass);
      } else {
        var key = new WorkKey(execution.phase(), action, kernel);
        var group = phaseGroups.get(key);
        if (group == null)
          phaseGroups.put(key, new Flight(execution, mass, kernel, actions.size()));
        else {
          group.mass = group.mass.add(mass);
          groupedInputs++;
        }
        (initializing ? initialUses : cycleUses).merge(action, mass, Fraction::add);
        // Keep source-conditioned unknown exits separate even when their known transitions share.
        absorb(Exit.UNRESOLVED, execution.state(), mass.multiply(kernel.unresolved()));
      }
      phaseReady =
          pending.isEmpty() || pending.keySet().iterator().next().phase() != execution.phase();
      if (!phaseReady) return;
      if (phaseGroups.isEmpty()) {
        phaseReady = false;
        if (pending.isEmpty()) finishBoundary();
        return;
      }
    }
    if (flight == null) {
      var group = phaseGroups.entrySet().iterator().next();
      flight = group.getValue();
      phaseGroups.remove(group.getKey());
    }
    int consumed = 0;
    while (flight.cursor < flight.kernel.outcomes().size() && consumed < maxOutcomes) {
      var outcome = flight.kernel.outcomes().get(flight.cursor);
      var terminal = classify.apply(outcome.state());
      var weight = flight.mass.multiply(outcome.probability());
      if (!weight.equals(Fraction.ZERO)) {
        // First hit precedes the next primitive, including between slices.
        if (terminal.isPresent()) absorb(terminal.get(), outcome.state(), weight);
        else if (flight.execution.phase() + 1 == flight.phases)
          boundary.merge(outcome.state(), weight, Fraction::add);
        else
          pending.merge(
              new Execution<>(outcome.state(), flight.execution.phase() + 1),
              weight,
              Fraction::add);
      }
      flight.cursor++;
      consumed++;
    }
    if (flight.cursor == flight.kernel.outcomes().size()) {
      flight = null;
      if (phaseGroups.isEmpty()) {
        phaseReady = false;
        if (pending.isEmpty()) finishBoundary();
      }
    }
  }

  private void finishBoundary() {
    if (compact != null) {
      if (boundary.size() > 1) compactionQueue.add(boundary);
      for (var values : exits.values()) if (values.size() > 1) compactionQueue.add(values);
    }
    if (compactionQueue.isEmpty()) endBoundary();
  }

  private void endBoundary() {
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
