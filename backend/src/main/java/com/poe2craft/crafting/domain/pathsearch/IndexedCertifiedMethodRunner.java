package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.WorkbenchCurrency;
import java.util.*;
import java.util.function.Function;

/** Shared exact primitive kernels, with arithmetic precision scoped to one cooperative method. */
public final class IndexedCertifiedMethodRunner {
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
  private final CertifiedMethodRunner<Integer, WorkbenchCurrency> runner;
  private IndexedKernelStore.Request pending;
  private Key pendingKey;

  public IndexedCertifiedMethodRunner(
      IndexedKernelStore store,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      CertifiedInterval.Precision precision) {
    this.store = Objects.requireNonNull(store);
    this.source = source;
    this.template = template;
    this.classify = classify;
    this.precision = precision;
    store.index().key(source);
    runner = new CertifiedMethodRunner<>(source, template, this::kernel, classify, precision);
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

    public Snapshot atLaterAbsorbedBoundary(long cycles) {
      return new Snapshot(
          index, source, template, classify, precision, point.atLaterAbsorbedBoundary(cycles));
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
