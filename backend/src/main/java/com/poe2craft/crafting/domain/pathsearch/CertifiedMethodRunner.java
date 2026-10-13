package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.util.*;
import java.util.function.Function;

/**
 * Finite-horizon Markov transport with deterministic enclosing arithmetic and primitive first hit.
 */
public final class CertifiedMethodRunner<S, A> {
  public static final String VERSION = "certified-method-markov-v1";

  public record Observation<S, A>(
      long cycles,
      Map<Exit, Map<S, CertifiedInterval>> exits,
      Map<S, CertifiedInterval> active,
      Map<A, CertifiedInterval> initialUses,
      Map<A, CertifiedInterval> cycleUses,
      CertifiedInterval mass) {
    public Observation<S, A> atLaterAbsorbedBoundary(long later) {
      if (!active.isEmpty() || later < cycles || !mass.contains(Fraction.ONE))
        throw new IllegalArgumentException("Complete absorbed observation required");
      return new Observation<>(later, exits, active, initialUses, cycleUses, mass);
    }
  }

  private final Template<S, A> template;
  private final Transition<S, A> transition;
  private final Function<S, Optional<Exit>> classify;
  private final int bits;
  private final Map<Exit, Map<S, CertifiedInterval>> exits = new EnumMap<>(Exit.class);
  private final LinkedHashMap<Execution<S>, CertifiedInterval> pending = new LinkedHashMap<>();
  private final LinkedHashMap<S, CertifiedInterval> boundary = new LinkedHashMap<>();
  private final Map<A, CertifiedInterval> initialUses = new LinkedHashMap<>(),
      cycleUses = new LinkedHashMap<>();
  private CertifiedInterval absorbedMass, boundaryMass;
  private boolean initializing, atBoundary;
  private long cycles;
  private Flight flight;

  private final class Flight {
    final Execution<S> execution;
    final CertifiedInterval mass;
    final Kernel<S> kernel;
    final int phases;
    int cursor;

    Flight(Execution<S> execution, CertifiedInterval mass, Kernel<S> kernel, int phases) {
      this.execution = execution;
      this.mass = mass;
      this.kernel = kernel;
      this.phases = phases;
    }
  }

  public CertifiedMethodRunner(
      S root,
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classify,
      CertifiedInterval.Precision precision) {
    this.template = template;
    this.transition = transition;
    this.classify = classify;
    bits = precision.fractionalBits();
    absorbedMass = CertifiedInterval.zero(bits);
    boundaryMass = CertifiedInterval.zero(bits);
    for (var kind : Exit.values()) exits.put(kind, new LinkedHashMap<>());
    var first = classify.apply(root);
    if (first.isPresent()) {
      absorb(first.orElseThrow(), root, CertifiedInterval.one(bits));
      atBoundary = true;
    } else if (!template.initial().isEmpty()) {
      initializing = true;
      pending.put(new Execution<>(root, 0), CertifiedInterval.one(bits));
    } else {
      boundary.put(root, CertifiedInterval.one(bits));
      boundaryMass = CertifiedInterval.one(bits);
      atBoundary = true;
    }
  }

  public long retainedStates() {
    return (long) pending.size()
        + boundary.size()
        + exits.values().stream().mapToLong(Map::size).sum();
  }

  public record Retention(long pending, long boundary, long exits, long flightOutcomes) {}

  public Retention retention() {
    return new Retention(
        pending.size(),
        boundary.size(),
        exits.values().stream().mapToLong(Map::size).sum(),
        flight == null ? 0 : flight.kernel.outcomes().size());
  }

  public boolean atBoundary() {
    return atBoundary;
  }

  public long cycles() {
    return cycles;
  }

  public boolean absorbed() {
    if (!atBoundary) throw new IllegalStateException("Partial cycle");
    return boundary.isEmpty();
  }

  public void beginCycle() {
    if (!atBoundary || cycles == Long.MAX_VALUE)
      throw new IllegalStateException("Cannot begin cycle");
    boundary.forEach((state, mass) -> pending.put(new Execution<>(state, 0), mass));
    boundary.clear();
    boundaryMass = CertifiedInterval.zero(bits);
    atBoundary = false;
    if (pending.isEmpty()) finish();
  }

  private static CertifiedInterval probabilitySum(CertifiedInterval a, CertifiedInterval b) {
    return a.add(b).probability();
  }

  private void absorb(Exit kind, S state, CertifiedInterval mass) {
    if (mass.zero()) return;
    exits.get(kind).merge(state, mass, CertifiedMethodRunner::probabilitySum);
    absorbedMass = absorbedMass.add(mass).probability();
  }

  public void step() {
    step(32);
  }

  public void step(int allowance) {
    if (allowance < 1 || atBoundary)
      throw new IllegalStateException("Active phase and positive allowance required");
    if (flight == null) {
      var entry = pending.firstEntry();
      var execution = entry.getKey();
      var mass = entry.getValue();
      var actions = initializing ? template.initial() : template.cycle();
      var action = Objects.requireNonNull(actions.get(execution.phase()).apply(execution.state()));
      var kernel = transition.apply(execution.state(), action);
      // A suspended lookup changes neither cost nor the execution cursor.
      pending.remove(execution);
      if (kernel.unavailable()) {
        absorb(Exit.DEAD, execution.state(), mass);
        if (pending.isEmpty()) finish();
        return;
      }
      (initializing ? initialUses : cycleUses).merge(action, mass, CertifiedInterval::add);
      absorb(
          Exit.UNRESOLVED,
          execution.state(),
          mass.multiply(CertifiedInterval.enclose(kernel.unresolved(), bits)).probability());
      flight = new Flight(execution, mass, kernel, actions.size());
    }
    for (int n = 0; n < allowance && flight.cursor < flight.kernel.outcomes().size(); n++) {
      var outcome = flight.kernel.outcomes().get(flight.cursor++);
      var mass =
          flight
              .mass
              .multiply(CertifiedInterval.enclose(outcome.probability(), bits))
              .probability();
      if (mass.zero()) continue;
      var terminal = classify.apply(outcome.state());
      if (terminal.isPresent()) absorb(terminal.orElseThrow(), outcome.state(), mass);
      else if (flight.execution.phase() + 1 == flight.phases) {
        boundary.merge(outcome.state(), mass, CertifiedMethodRunner::probabilitySum);
        boundaryMass = boundaryMass.add(mass).probability();
      } else
        pending.merge(
            new Execution<>(outcome.state(), flight.execution.phase() + 1),
            mass,
            CertifiedMethodRunner::probabilitySum);
    }
    if (flight.cursor == flight.kernel.outcomes().size()) {
      flight = null;
      if (pending.isEmpty()) finish();
    }
  }

  private void finish() {
    if (!absorbedMass.add(boundaryMass).contains(Fraction.ONE))
      throw new IllegalStateException("Mass outside arithmetic enclosure");
    if (initializing) initializing = false;
    else cycles++;
    atBoundary = true;
  }

  public Observation<S, A> observation() {
    if (!atBoundary) throw new IllegalStateException("Partial cycle");
    var saved = new EnumMap<Exit, Map<S, CertifiedInterval>>(Exit.class);
    exits.forEach((kind, law) -> saved.put(kind, Map.copyOf(law)));
    return new Observation<>(
        cycles,
        Map.copyOf(saved),
        Map.copyOf(boundary),
        Map.copyOf(initialUses),
        Map.copyOf(cycleUses),
        absorbedMass.add(boundaryMass));
  }
}
