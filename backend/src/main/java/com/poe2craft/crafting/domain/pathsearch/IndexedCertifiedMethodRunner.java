package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.WorkbenchCurrency;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import java.util.*;
import java.util.function.Function;

/** Shared exact primitive kernels, with arithmetic precision scoped to one cooperative method. */
public final class IndexedCertifiedMethodRunner implements AutoCloseable {
  private static final class Missing extends RuntimeException {
    Missing() {
      super(null, null, false, false);
    }
  }

  private record Key(int source, WorkbenchCurrency action) {}

  private final IndexedKernelStore store;
  private final int source;
  private final ExactMethodRunner.Template<Integer, WorkbenchCurrency> template;
  private final Function<Integer, Optional<ExactMethodRunner.Exit>> classify;
  private final CertifiedInterval.Precision precision;
  private CertifiedMethodRunner<Integer, WorkbenchCurrency> runner;
  private IndexedKernelStore.Request pending;
  private Key pendingKey;
  private final List<CertifiedMutableLawStore.Law> absorptionLaws = new ArrayList<>();

  public IndexedCertifiedMethodRunner(
      IndexedKernelStore store,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      CertifiedInterval.Precision precision) {
    this(store, source, template, classify, precision, null);
  }

  public IndexedCertifiedMethodRunner(
      IndexedKernelStore store,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      CertifiedInterval.Precision precision,
      CertifiedMutableLawStore absorptionStore) {
    this(
        store,
        source,
        template,
        classify,
        precision,
        absorptionStore,
        CertifiedMethodRunner.WorkingMaps.memory());
  }

  public IndexedCertifiedMethodRunner(
      IndexedKernelStore store,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      CertifiedInterval.Precision precision,
      CertifiedMutableLawStore absorptionStore,
      CertifiedMethodRunner.WorkingMaps<Integer> workingMaps) {
    this(store, source, template, classify, precision, absorptionStore, workingMaps, true);
  }

  private IndexedCertifiedMethodRunner(
      IndexedKernelStore store,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      CertifiedInterval.Precision precision,
      CertifiedMutableLawStore absorptionStore,
      CertifiedMethodRunner.WorkingMaps<Integer> workingMaps,
      boolean initialize) {
    this.store = Objects.requireNonNull(store);
    this.source = source;
    this.template = template;
    this.classify = classify;
    this.precision = precision;
    store.index().key(source);
    if (!initialize) return;
    runner =
        absorptionStore == null
            ? new CertifiedMethodRunner<>(
                source,
                template,
                this::kernel,
                classify,
                precision,
                LinkedHashMap::new,
                workingMaps)
            : new CertifiedMethodRunner<>(
                source,
                template,
                this::kernel,
                classify,
                precision,
                () -> {
                  var law = absorptionStore.create();
                  absorptionLaws.add(law);
                  return law;
                },
                workingMaps);
  }

  public CertifiedMethodRunner<Integer, WorkbenchCurrency>.CursorLease leaseCursor() {
    return runner.leaseCursor();
  }

  /**
   * The caller restores the same class index and versioned method semantics before reading rows.
   */
  public static Restoration restoreCursor(
      CertifiedMethodRunner.CursorHeader<Integer> header,
      IndexedKernelStore store,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      CertifiedInterval.Precision precision,
      CertifiedMutableLawStore absorptionStore) {
    return restoreCursor(
        header,
        store,
        source,
        template,
        classify,
        precision,
        absorptionStore,
        CertifiedMethodRunner.WorkingMaps.memory());
  }

  public static Restoration restoreCursor(
      CertifiedMethodRunner.CursorHeader<Integer> header,
      IndexedKernelStore store,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      CertifiedInterval.Precision precision,
      CertifiedMutableLawStore absorptionStore,
      CertifiedMethodRunner.WorkingMaps<Integer> workingMaps) {
    var target =
        new IndexedCertifiedMethodRunner(
            store, source, template, classify, precision, absorptionStore, workingMaps, false);
    return new Restoration(
        target,
        CertifiedMethodRunner.restoreCursor(
            header,
            template,
            target::kernel,
            classify,
            precision,
            () -> {
              if (absorptionStore == null) return new LinkedHashMap<Integer, CertifiedInterval>();
              var law = absorptionStore.create();
              target.absorptionLaws.add(law);
              return law;
            },
            workingMaps));
  }

  public static final class Restoration implements AutoCloseable {
    private final IndexedCertifiedMethodRunner target;
    private final CertifiedMethodRunner.CursorRestore<Integer, WorkbenchCurrency> restoration;
    private boolean finished;

    private Restoration(
        IndexedCertifiedMethodRunner target,
        CertifiedMethodRunner.CursorRestore<Integer, WorkbenchCurrency> restoration) {
      this.target = target;
      this.restoration = restoration;
    }

    public void accept(CertifiedMethodRunner.CursorRow<Integer, WorkbenchCurrency> row) {
      restoration.accept(row);
    }

    public long retainedStates() {
      return restoration.retainedStates();
    }

    public IndexedCertifiedMethodRunner finish() {
      target.runner = restoration.finish();
      finished = true;
      return target;
    }

    @Override
    public void close() {
      if (finished) return;
      restoration.close();
      target.absorptionLaws.clear();
    }
  }

  @Override
  public void close() {
    if (runner != null) runner.closeWorkingMaps();
    absorptionLaws.forEach(CertifiedMutableLawStore.Law::close);
    absorptionLaws.clear();
    pending = null;
    pendingKey = null;
  }

  private ExactMethodRunner.Kernel<Integer> kernel(int source, WorkbenchCurrency action) {
    var key = new Key(source, action);
    if (pending != null && pendingKey.equals(key)) {
      if (!pending.complete()) throw new Missing();
      var result = pending.result();
      pending = null;
      pendingKey = null;
      return result;
    }
    var request = store.request(source, action);
    if (request.complete()) return request.result();
    pending = request;
    pendingKey = key;
    throw new Missing();
  }

  public long retainedStates() {
    return runner.retainedStates();
  }

  public CertifiedMethodRunner.Retention retention() {
    return runner.retention();
  }

  public boolean atBoundary() {
    return runner.atBoundary();
  }

  public boolean absorbed() {
    return runner.absorbed();
  }

  public long cycles() {
    return runner.cycles();
  }

  public void beginCycle() {
    runner.beginCycle();
  }

  public void step() {
    if (pending != null && !pending.complete()) {
      pending.step();
      return;
    }
    try {
      runner.step();
    } catch (Missing ignored) {
      /* Suspended before changing phase or cost. */
    }
  }

  public CertifiedMethodRunner.Observation<Integer, WorkbenchCurrency> observation() {
    return runner.observation();
  }

  public Snapshot snapshot() {
    return new Snapshot(store.index(), source, template, classify, precision, runner.observation());
  }

  public static Snapshot restoredSnapshot(
      BasicClassIndex index,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      CertifiedInterval.Precision precision,
      CertifiedMethodRunner.Observation<Integer, WorkbenchCurrency> point) {
    index.key(source);
    var total = CertifiedInterval.zero(precision.fractionalBits());
    if (point.cycles() < 0 || point.exits().size() != ExactMethodRunner.Exit.values().length)
      throw new IllegalArgumentException("Invalid saved observation");
    var parts = new ArrayList<>(point.exits().values());
    for (var law : parts)
      for (var entry : law.entrySet()) {
        index.key(entry.getKey());
        if (entry.getValue().fractionalBits() != precision.fractionalBits())
          throw new IllegalArgumentException("Observation precision mismatch");
        total = total.add(entry.getValue()).probability();
      }
    var active = CertifiedInterval.zero(precision.fractionalBits());
    for (var entry : point.active().entrySet()) {
      index.key(entry.getKey());
      if (entry.getValue().fractionalBits() != precision.fractionalBits())
        throw new IllegalArgumentException("Observation precision mismatch");
      active = active.add(entry.getValue()).probability();
    }
    total = total.add(active);
    if (!total.equals(point.mass()) || !total.contains(Fraction.ONE))
      throw new IllegalArgumentException("Saved observation mass mismatch");
    return new Snapshot(index, source, template, classify, precision, point);
  }

  public FrozenSnapshot freezeSnapshot(CertifiedLawStore store) {
    return new FrozenSnapshot(store);
  }

  /** Cooperative storage of a leased boundary; no duplicate row maps are captured. */
  public final class FrozenSnapshot implements AutoCloseable {
    private final CertifiedLawStore store;
    private CertifiedMethodRunner<Integer, WorkbenchCurrency>.ObservationLease lease;
    private final Map<ExactMethodRunner.Exit, Map<Integer, CertifiedInterval>> exits =
        new EnumMap<>(ExactMethodRunner.Exit.class);
    private CertifiedLawStore.Freeze current;
    private int part;
    private Snapshot result;
    private boolean closed;

    private FrozenSnapshot(CertifiedLawStore store) {
      this.store = store;
      lease = runner.leaseObservation();
    }

    public boolean complete() {
      return result != null;
    }

    public long transientBytes() {
      return current == null ? 0 : current.transientBytes();
    }

    public void step() {
      if (closed) throw new IllegalStateException("Snapshot freeze closed");
      if (complete()) return;
      var point = lease.point();
      var kinds = ExactMethodRunner.Exit.values();
      if (current == null) {
        current =
            store.prepare(part < kinds.length ? point.exits().get(kinds[part]) : point.active());
        return;
      }
      if (!current.complete()) {
        current.step();
        return;
      }
      if (part < kinds.length) {
        exits.put(kinds[part++], current.result());
        current = null;
        return;
      }
      result =
          new Snapshot(
              storeIndex(),
              source,
              template,
              classify,
              precision,
              new CertifiedMethodRunner.Observation<>(
                  point.cycles(),
                  Map.copyOf(exits),
                  current.result(),
                  point.initialUses(),
                  point.cycleUses(),
                  point.mass()));
      lease.close();
      lease = null;
      current = null;
      exits.clear();
    }

    private BasicClassIndex storeIndex() {
      return IndexedCertifiedMethodRunner.this.store.index();
    }

    public Snapshot result() {
      if (!complete()) throw new IllegalStateException("Partial snapshot freeze");
      return result;
    }

    @Override
    public void close() {
      closed = true;
      if (lease != null) lease.close();
      lease = null;
      current = null;
      exits.clear();
    }
  }

  /** Certified from a completed shared primitive request, never from an asserted hash. */
  public static final class InitialLaw {
    private final BasicClassIndex index;
    private final int source;
    private final WorkbenchCurrency action;
    private final Map<Integer, Fraction> outcomes;
    private final Fraction unresolved;
    private final boolean unavailable;

    public InitialLaw(IndexedKernelStore.Request request) {
      var kernel = request.result();
      index = request.store().index();
      source = request.source();
      action = request.action();
      var law = new HashMap<Integer, Fraction>();
      kernel.outcomes().forEach(o -> law.merge(o.state(), o.probability(), Fraction::add));
      outcomes = Map.copyOf(law);
      unresolved = kernel.unresolved();
      unavailable = kernel.unavailable();
    }

    public Map<Integer, Fraction> outcomes() {
      return outcomes;
    }

    public Fraction unresolved() {
      return unresolved;
    }

    public boolean unavailable() {
      return unavailable;
    }

    public InitialEquivalence equivalentTo(InitialLaw other) {
      if (index != other.index
          || unavailable != other.unavailable
          || !unresolved.equals(other.unresolved)
          || !outcomes.equals(other.outcomes))
        throw new IllegalArgumentException("Initial class laws differ");
      return new InitialEquivalence(index, source, other.source, action, other.action);
    }
  }

  public static final class InitialEquivalence {
    private final BasicClassIndex index;
    private final int source, targetSource;
    private final WorkbenchCurrency from, to;

    private InitialEquivalence(
        BasicClassIndex index,
        int source,
        int targetSource,
        WorkbenchCurrency from,
        WorkbenchCurrency to) {
      this.index = index;
      this.source = source;
      this.targetSource = targetSource;
      this.from = from;
      this.to = to;
    }
  }

  public static final class Snapshot {
    private final BasicClassIndex index;
    private final int source;
    private final ExactMethodRunner.Template<Integer, WorkbenchCurrency> template;
    private final Function<Integer, Optional<ExactMethodRunner.Exit>> classify;
    private final CertifiedInterval.Precision precision;
    private final CertifiedMethodRunner.Observation<Integer, WorkbenchCurrency> point;

    private Snapshot(
        BasicClassIndex index,
        int source,
        ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
        Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
        CertifiedInterval.Precision precision,
        CertifiedMethodRunner.Observation<Integer, WorkbenchCurrency> point) {
      this.index = index;
      this.source = source;
      this.template = template;
      this.classify = classify;
      this.precision = precision;
      this.point = point;
    }

    public CertifiedMethodRunner.Observation<Integer, WorkbenchCurrency> observation() {
      return point;
    }

    public Snapshot freeze(CertifiedLawStore store) {
      var exits =
          new EnumMap<ExactMethodRunner.Exit, Map<Integer, CertifiedInterval>>(
              ExactMethodRunner.Exit.class);
      point.exits().forEach((kind, law) -> exits.put(kind, store.freeze(law)));
      return new Snapshot(
          index,
          source,
          template,
          classify,
          precision,
          new CertifiedMethodRunner.Observation<>(
              point.cycles(),
              Map.copyOf(exits),
              store.freeze(point.active()),
              point.initialUses(),
              point.cycleUses(),
              point.mass()));
    }

    public Snapshot atLaterAbsorbedBoundary(long cycles) {
      return new Snapshot(
          index, source, template, classify, precision, point.atLaterAbsorbedBoundary(cycles));
    }

    /**
     * Change only an equivalent one-action preparation; retain the real action for exact replay.
     */
    public Snapshot withEquivalentInitial(InitialEquivalence proof) {
      if (proof.index != index
          || proof.source != source
          || template.initial().size() != 1
          || template.initial().getFirst().apply(source) != proof.from)
        throw new IllegalArgumentException("Equivalence does not match this method");
      if (source != proof.targetSource
          && (classify.apply(source).isPresent() || classify.apply(proof.targetSource).isPresent()))
        throw new IllegalArgumentException("Cannot change an already terminal origin");
      var uses = new HashMap<>(point.initialUses());
      var count = uses.remove(proof.from);
      if (count != null) uses.merge(proof.to, count, CertifiedInterval::add);
      var rebound =
          new CertifiedMethodRunner.Observation<>(
              point.cycles(),
              point.exits(),
              point.active(),
              Map.copyOf(uses),
              point.cycleUses(),
              point.mass());
      var reboundTemplate =
          new ExactMethodRunner.Template<Integer, WorkbenchCurrency>(
              template.id() + ":equivalent-initial:" + proof.to,
              List.of(s -> proof.to),
              template.cycle());
      return new Snapshot(index, proof.targetSource, reboundTemplate, classify, precision, rebound);
    }

    public ConditionalMethodLaw condition(
        ConditionalMethodLaw input, Optional<ExactMethodRunner.Exit> exit, int target) {
      var values = exit.map(point.exits()::get).orElse(point.active());
      return ConditionalMethodLaw.boundedOutput(
          input,
          index,
          source,
          template,
          classify,
          point.cycles(),
          exit,
          target,
          values.getOrDefault(target, CertifiedInterval.zero(precision.fractionalBits())));
    }
  }
}
