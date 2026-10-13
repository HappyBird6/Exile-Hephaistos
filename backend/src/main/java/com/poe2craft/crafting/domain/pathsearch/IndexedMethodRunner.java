package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.WorkbenchCurrency;
import java.util.*;
import java.util.function.Function;

/** Cooperative method marginals on proven classes; observations are not incoming item laws. */
public final class IndexedMethodRunner {
  public static final String VERSION = "cooperative-shared-class-method-marginals-v2";

  private record Key(int source, WorkbenchCurrency action) {}

  private static final class MissingKernel extends RuntimeException {
    final Key key;

    MissingKernel(Key key) {
      super(null, null, false, false);
      this.key = key;
    }
  }

  private final BasicClassIndex index;
  private final int source;
  private final ExactMethodRunner.Template<Integer, WorkbenchCurrency> template;
  private final Function<Integer, Optional<ExactMethodRunner.Exit>> classify;
  private final IndexedKernelStore store;
  private final ExactMethodRunner<Integer, WorkbenchCurrency> runner;
  private Key pendingKey;
  private IndexedKernelStore.Request pending;

  public IndexedMethodRunner(
      BasicClassIndex index,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      int cacheLimit) {
    this(new IndexedKernelStore(index, cacheLimit, Integer.MAX_VALUE), source, template, classify);
  }

  public IndexedMethodRunner(
      IndexedKernelStore store,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify) {
    this.store = Objects.requireNonNull(store);
    this.index = store.index();
    index.key(source);
    this.source = source;
    this.template = template;
    this.classify = classify;
    runner = new ExactMethodRunner<>(source, template, this::kernel, classify);
  }

  private ExactMethodRunner.Kernel<Integer> kernel(int source, WorkbenchCurrency action) {
    var key = new Key(source, action);
    if (pending != null && pendingKey.equals(key)) {
      if (!pending.complete()) throw new MissingKernel(key);
      var result = pending.result();
      pending = null;
      pendingKey = null;
      return result;
    }
    var request = store.request(source, action);
    if (request.complete()) return request.result();
    pendingKey = key;
    pending = request;
    throw new MissingKernel(key);
  }

  public boolean atBoundary() {
    return runner.atBoundary();
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
    } catch (MissingKernel missing) {
      // The handle was registered before suspension and survives shared-cache eviction.
    }
  }

  public ExactMethodRunner.Observation<Integer, WorkbenchCurrency> observation() {
    return runner.observation();
  }

  public Snapshot snapshot() {
    return new Snapshot(index, source, template, classify, runner.observation());
  }

  public ConditionalMethodLaw condition(
      ConditionalMethodLaw input, Optional<ExactMethodRunner.Exit> exit, int target) {
    return snapshot().condition(input, exit, target);
  }

  /** A completed observation can outlive its runner without losing its actual-input operator. */
  public static final class Snapshot {
    private final BasicClassIndex index;
    private final int source;
    private final ExactMethodRunner.Template<Integer, WorkbenchCurrency> template;
    private final Function<Integer, Optional<ExactMethodRunner.Exit>> classify;
    private final ExactMethodRunner.Observation<Integer, WorkbenchCurrency> point;

    private Snapshot(
        BasicClassIndex index,
        int source,
        ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
        Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
        ExactMethodRunner.Observation<Integer, WorkbenchCurrency> point) {
      this.index = index;
      this.source = source;
      this.template = template;
      this.classify = classify;
      this.point = point;
    }

    public ExactMethodRunner.Observation<Integer, WorkbenchCurrency> observation() {
      return point;
    }

    public Snapshot atLaterAbsorbedBoundary(long cycles) {
      return new Snapshot(index, source, template, classify, point.atLaterAbsorbedBoundary(cycles));
    }

    public ConditionalMethodLaw condition(
        ConditionalMethodLaw input, Optional<ExactMethodRunner.Exit> exit, int target) {
      var values = exit.map(point.exits()::get).orElse(point.active());
      var probability =
          values.getOrDefault(
              target,
              com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction.ZERO);
      return ConditionalMethodLaw.output(
          input, index, source, template, classify, point.cycles(), exit, target, probability);
    }
  }

  public int cachedKernels() {
    return store.cachedKernels();
  }

  public long calculatedKernels() {
    return store.calculatedKernels();
  }

  public long evictedKernels() {
    return store.evictedKernels();
  }
}
