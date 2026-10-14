package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

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
  private long absorbedEntries;
  private Flight flight;
  private boolean observationLeased;

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
    this(root, template, transition, classify, precision, LinkedHashMap::new);
  }

  public CertifiedMethodRunner(
      S root,
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      Supplier<? extends Map<S, CertifiedInterval>> absorptionMaps) {
    this(template, transition, classify, precision, absorptionMaps);
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

  private CertifiedMethodRunner(
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      Supplier<? extends Map<S, CertifiedInterval>> absorptionMaps) {
    this.template = template;
    this.transition = transition;
    this.classify = classify;
    bits = precision.fractionalBits();
    absorbedMass = CertifiedInterval.zero(bits);
    boundaryMass = CertifiedInterval.zero(bits);
    for (var kind : Exit.values()) exits.put(kind, absorptionMaps.get());
  }

  public static final String CURSOR_VERSION = VERSION + ":streaming-cursor-v1";

  public enum CursorKind {
    PENDING,
    BOUNDARY,
    EXIT,
    INITIAL_USE,
    CYCLE_USE,
    KERNEL
  }

  public record CursorRow<S, A>(
      CursorKind kind,
      Exit exit,
      S state,
      int phase,
      A action,
      CertifiedInterval mass,
      Fraction probability) {}

  public record FlightCursor<S>(
      Execution<S> execution,
      CertifiedInterval mass,
      int cursor,
      int outcomeCount,
      Fraction unresolved,
      int phases) {}

  public record CursorHeader<S>(
      String version,
      String templateId,
      int bits,
      long cycles,
      int initialPhases,
      int cyclePhases,
      boolean initializing,
      boolean atBoundary,
      CertifiedInterval absorbedMass,
      CertifiedInterval boundaryMass,
      long absorbedEntries,
      FlightCursor<S> flight) {}

  /** Streaming stable views; acquiring a cursor does not clone the retained state maps. */
  public final class CursorLease implements AutoCloseable {
    private boolean closed;
    private final CursorHeader<S> header;

    private CursorLease() {
      if (observationLeased) throw new IllegalStateException("Runner is already leased");
      observationLeased = true;
      header =
          new CursorHeader<>(
              CURSOR_VERSION,
              template.id(),
              bits,
              cycles,
              template.initial().size(),
              template.cycle().size(),
              initializing,
              atBoundary,
              absorbedMass,
              boundaryMass,
              absorbedEntries,
              flight == null
                  ? null
                  : new FlightCursor<>(
                      flight.execution,
                      flight.mass,
                      flight.cursor,
                      flight.kernel.outcomes().size(),
                      flight.kernel.unresolved(),
                      flight.phases));
    }

    public CursorHeader<S> header() {
      ensureOpen();
      return header;
    }

    public Iterator<CursorRow<S, A>> rows() {
      ensureOpen();
      var parts = new ArrayList<Iterator<CursorRow<S, A>>>();
      parts.add(
          pending.entrySet().stream()
              .map(
                  e ->
                      new CursorRow<S, A>(
                          CursorKind.PENDING,
                          null,
                          e.getKey().state(),
                          e.getKey().phase(),
                          null,
                          e.getValue(),
                          null))
              .iterator());
      parts.add(
          boundary.entrySet().stream()
              .map(
                  e ->
                      new CursorRow<S, A>(
                          CursorKind.BOUNDARY, null, e.getKey(), 0, null, e.getValue(), null))
              .iterator());
      exits.forEach(
          (kind, law) ->
              parts.add(
                  law.entrySet().stream()
                      .map(
                          e ->
                              new CursorRow<S, A>(
                                  CursorKind.EXIT, kind, e.getKey(), 0, null, e.getValue(), null))
                      .iterator()));
      parts.add(
          initialUses.entrySet().stream()
              .map(
                  e ->
                      new CursorRow<S, A>(
                          CursorKind.INITIAL_USE, null, null, 0, e.getKey(), e.getValue(), null))
              .iterator());
      parts.add(
          cycleUses.entrySet().stream()
              .map(
                  e ->
                      new CursorRow<S, A>(
                          CursorKind.CYCLE_USE, null, null, 0, e.getKey(), e.getValue(), null))
              .iterator());
      if (flight != null)
        parts.add(
            flight.kernel.outcomes().stream()
                .map(
                    o ->
                        new CursorRow<S, A>(
                            CursorKind.KERNEL, null, o.state(), 0, null, null, o.probability()))
                .iterator());
      return new Iterator<>() {
        int part;

        public boolean hasNext() {
          ensureOpen();
          while (part < parts.size() && !parts.get(part).hasNext()) part++;
          return part < parts.size();
        }

        public CursorRow<S, A> next() {
          if (!hasNext()) throw new NoSuchElementException();
          return parts.get(part).next();
        }
      };
    }

    private void ensureOpen() {
      if (closed) throw new IllegalStateException("Cursor lease closed");
    }

    public void close() {
      if (!closed) {
        closed = true;
        observationLeased = false;
      }
    }
  }

  public CursorLease leaseCursor() {
    return new CursorLease();
  }

  /** Restores rows in their saved order, without executing or charging another primitive action. */
  public static <S, A> CursorRestore<S, A> restoreCursor(
      CursorHeader<S> header,
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      Supplier<? extends Map<S, CertifiedInterval>> absorptionMaps) {
    return new CursorRestore<>(header, template, transition, classify, precision, absorptionMaps);
  }

  public static final class CursorRestore<S, A> implements AutoCloseable {
    private final CursorHeader<S> header;
    private final CertifiedMethodRunner<S, A> runner;
    private final List<Outcome<S>> outcomes = new ArrayList<>();
    private boolean finished;

    private CursorRestore(
        CursorHeader<S> header,
        Template<S, A> template,
        Transition<S, A> transition,
        Function<S, Optional<Exit>> classify,
        CertifiedInterval.Precision precision,
        Supplier<? extends Map<S, CertifiedInterval>> maps) {
      if (!CURSOR_VERSION.equals(header.version())
          || !template.id().equals(header.templateId())
          || precision.fractionalBits() != header.bits()
          || header.cycles() < 0
          || template.initial().size() != header.initialPhases()
          || template.cycle().size() != header.cyclePhases()
          || header.initializing()
              && (header.cycles() != 0 || header.initialPhases() == 0 || header.atBoundary()))
        throw new IllegalArgumentException("Incompatible method cursor");
      this.header = header;
      runner = new CertifiedMethodRunner<>(template, transition, classify, precision, maps);
      runner.cycles = header.cycles();
      runner.initializing = header.initializing();
      runner.atBoundary = header.atBoundary();
      probability(header.absorbedMass());
      probability(header.boundaryMass());
    }

    private void probability(CertifiedInterval mass) {
      if (mass == null
          || mass.fractionalBits() != header.bits()
          || mass.upperUnits().compareTo(java.math.BigInteger.ONE.shiftLeft(header.bits())) > 0)
        throw new IllegalArgumentException("Invalid cursor probability");
    }

    private <K> void put(Map<K, CertifiedInterval> target, K key, CertifiedInterval mass) {
      if (key == null
          || mass == null
          || mass.fractionalBits() != header.bits()
          || target.putIfAbsent(key, mass) != null)
        throw new IllegalArgumentException("Invalid or duplicate cursor row");
    }

    public void accept(CursorRow<S, A> row) {
      if (finished) throw new IllegalStateException("Cursor restoration finished");
      switch (row.kind()) {
        case PENDING -> {
          probability(row.mass());
          int phases = header.initializing() ? header.initialPhases() : header.cyclePhases();
          if (row.state() == null || row.phase() < 0 || row.phase() >= phases)
            throw new IllegalArgumentException("Invalid pending phase");
          put(runner.pending, new Execution<>(row.state(), row.phase()), row.mass());
        }
        case BOUNDARY -> {
          probability(row.mass());
          put(runner.boundary, row.state(), row.mass());
        }
        case EXIT -> {
          probability(row.mass());
          if (row.exit() == null) throw new IllegalArgumentException("Missing exit kind");
          put(runner.exits.get(row.exit()), row.state(), row.mass());
          runner.absorbedEntries++;
        }
        case INITIAL_USE -> put(runner.initialUses, row.action(), row.mass());
        case CYCLE_USE -> put(runner.cycleUses, row.action(), row.mass());
        case KERNEL -> {
          if (header.flight() == null || outcomes.size() >= header.flight().outcomeCount())
            throw new IllegalArgumentException("Unexpected kernel row");
          outcomes.add(new Outcome<>(row.state(), row.probability()));
        }
      }
    }

    public long retainedStates() {
      return runner.retainedStates() + outcomes.size();
    }

    @Override
    public void close() {
      if (finished) return;
      finished = true;
      runner.pending.clear();
      runner.boundary.clear();
      outcomes.clear();
      for (var law : runner.exits.values()) {
        if (law instanceof CertifiedMutableLawStore.Law backed) backed.close();
        else law.clear();
      }
    }

    private CertifiedInterval sum(Collection<CertifiedInterval> values) {
      var total = CertifiedInterval.zero(header.bits());
      for (var value : values) total = total.add(value).probability();
      return total;
    }

    public CertifiedMethodRunner<S, A> finish() {
      if (finished) throw new IllegalStateException("Cursor restoration finished");
      var absorbed = CertifiedInterval.zero(header.bits());
      for (var law : runner.exits.values())
        absorbed = absorbed.add(sum(law.values())).probability();
      var boundary = sum(runner.boundary.values());
      if (!absorbed.equals(header.absorbedMass())
          || !boundary.equals(header.boundaryMass())
          || runner.absorbedEntries != header.absorbedEntries())
        throw new IllegalArgumentException("Cursor accumulator mismatch");
      runner.absorbedMass = absorbed;
      runner.boundaryMass = boundary;
      var total = absorbed.add(boundary).add(sum(runner.pending.values())).probability();
      if (header.flight() != null) {
        var saved = header.flight();
        int phases = header.initializing() ? header.initialPhases() : header.cyclePhases();
        probability(saved.mass());
        if (saved.execution() == null
            || saved.execution().state() == null
            || saved.execution().phase() < 0
            || saved.execution().phase() >= phases
            || saved.phases() != phases
            || saved.cursor() < 0
            || saved.cursor() >= saved.outcomeCount()
            || outcomes.size() != saved.outcomeCount())
          throw new IllegalArgumentException("Invalid flight cursor");
        var kernel = new Kernel<>(outcomes, saved.unresolved(), false);
        runner.flight = runner.new Flight(saved.execution(), saved.mass(), kernel, phases);
        runner.flight.cursor = saved.cursor();
        for (int i = saved.cursor(); i < outcomes.size(); i++)
          total =
              total
                  .add(
                      saved
                          .mass()
                          .multiply(
                              CertifiedInterval.enclose(
                                  outcomes.get(i).probability(), header.bits()))
                          .probability())
                  .probability();
      }
      if (!total.contains(Fraction.ONE)
          || header.atBoundary() && (!runner.pending.isEmpty() || runner.flight != null)
          || !header.atBoundary() && runner.pending.isEmpty() && runner.flight == null)
        throw new IllegalArgumentException("Invalid cursor mass or phase");
      outcomes.clear();
      finished = true;
      return runner;
    }
  }

  public long retainedStates() {
    long residentExits = 0;
    for (var law : exits.values())
      if (!(law instanceof CertifiedMutableLawStore.Law)) residentExits += law.size();
    return (long) pending.size() + boundary.size() + residentExits;
  }

  public record Retention(long pending, long boundary, long exits, long flightOutcomes) {}

  public Retention retention() {
    return new Retention(
        pending.size(),
        boundary.size(),
        absorbedEntries,
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
    if (observationLeased) throw new IllegalStateException("Observation is being frozen");
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
    var law = exits.get(kind);
    int before = law.size();
    law.merge(state, mass, CertifiedMethodRunner::probabilitySum);
    absorbedEntries += law.size() - before;
    absorbedMass = absorbedMass.add(mass).probability();
  }

  public void step() {
    step(32);
  }

  public void step(int allowance) {
    if (observationLeased) throw new IllegalStateException("Observation is being frozen");
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

  /** Internal immutable view lease: advancement is blocked until the reader releases it. */
  final class ObservationLease implements AutoCloseable {
    private final Observation<S, A> point;
    private boolean closed;

    private ObservationLease() {
      if (!atBoundary || observationLeased)
        throw new IllegalStateException("Boundary lease unavailable");
      observationLeased = true;
      var view = new EnumMap<Exit, Map<S, CertifiedInterval>>(Exit.class);
      exits.forEach((kind, law) -> view.put(kind, Collections.unmodifiableMap(law)));
      point =
          new Observation<>(
              cycles,
              Map.copyOf(view),
              Collections.unmodifiableMap(boundary),
              Map.copyOf(initialUses),
              Map.copyOf(cycleUses),
              absorbedMass.add(boundaryMass));
    }

    Observation<S, A> point() {
      if (closed) throw new IllegalStateException("Observation lease closed");
      return point;
    }

    @Override
    public void close() {
      if (!closed) {
        closed = true;
        observationLeased = false;
      }
    }
  }

  ObservationLease leaseObservation() {
    return new ObservationLease();
  }
}
