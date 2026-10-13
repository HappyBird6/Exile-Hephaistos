package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.WorkbenchCurrency;
import java.util.*;
import java.util.function.Function;

/** Cooperative method marginals on proven classes; observations are not incoming item laws. */
public final class IndexedMethodRunner {
  public static final String VERSION = "cooperative-class-method-marginals-v1";

  private record Key(int source, WorkbenchCurrency action) {}

  private static final class MissingKernel extends RuntimeException {
    final Key key;

    MissingKernel(Key key) {
      super(null, null, false, false);
      this.key = key;
    }
  }

  private final BasicClassIndex index;
  private final int cacheLimit;
  private final LinkedHashMap<Key, ExactMethodRunner.Kernel<Integer>> kernels =
      new LinkedHashMap<>(16, .75f, true);
  private final ExactMethodRunner<Integer, WorkbenchCurrency> runner;
  private Key pendingKey;
  private BasicClassIndex.Cursor pending;
  private long calculatedKernels, evictedKernels;

  public IndexedMethodRunner(
      BasicClassIndex index,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      int cacheLimit) {
    if (cacheLimit < 1) throw new IllegalArgumentException("Positive kernel cache limit required");
    this.index = Objects.requireNonNull(index);
    index.key(source);
    this.cacheLimit = cacheLimit;
    runner = new ExactMethodRunner<>(source, template, this::kernel, classify);
  }

  private ExactMethodRunner.Kernel<Integer> kernel(int source, WorkbenchCurrency action) {
    var key = new Key(source, action);
    var result = kernels.get(key);
    if (result == null) throw new MissingKernel(key);
    return result;
  }

  public boolean atBoundary() {
    return runner.atBoundary();
  }

  public void beginCycle() {
    runner.beginCycle();
  }

  public void step() {
    if (pending != null) {
      if (!pending.complete()) pending.step();
      if (pending.complete()) {
        kernels.put(pendingKey, pending.result());
        calculatedKernels++;
        pendingKey = null;
        pending = null;
        while (kernels.size() > cacheLimit) {
          kernels.pollFirstEntry();
          evictedKernels++;
        }
      }
      return;
    }
    try {
      runner.step();
    } catch (MissingKernel missing) {
      pendingKey = missing.key;
      pending = index.cursor(pendingKey.source(), pendingKey.action());
    }
  }

  public ExactMethodRunner.Observation<Integer, WorkbenchCurrency> observation() {
    return runner.observation();
  }

  public int cachedKernels() {
    return kernels.size();
  }

  public long calculatedKernels() {
    return calculatedKernels;
  }

  public long evictedKernels() {
    return evictedKernels;
  }
}
