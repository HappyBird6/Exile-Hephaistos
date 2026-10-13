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
}
