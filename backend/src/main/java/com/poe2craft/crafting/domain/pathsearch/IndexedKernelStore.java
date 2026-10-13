package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.WorkbenchCurrency;
import java.util.*;

/**
 * Job-local, bounded completed kernels with shared cooperative work and explicit delivery handles.
 * Access is serialized by the job scheduler; request sharing is not concurrent-thread mutation.
 */
public final class IndexedKernelStore {
  public static final String VERSION = "shared-indexed-kernel-store-v1";

  private record Key(int source, WorkbenchCurrency action) {}

  private final BasicClassIndex index;
  private final int kernelLimit;
  private final int outcomeLimit;
  private final LinkedHashMap<Key, ExactMethodRunner.Kernel<Integer>> completed =
      new LinkedHashMap<>(16, .75f, true);
  private final Map<Key, Request> pending = new HashMap<>();
  private int outcomes;
  private long calculated, evicted, shared;

  public IndexedKernelStore(BasicClassIndex index, int kernelLimit, int outcomeLimit) {
    this.index = Objects.requireNonNull(index);
    if (kernelLimit < 1 || outcomeLimit < 1)
      throw new IllegalArgumentException("Positive cache budgets required");
    this.kernelLimit = kernelLimit;
    this.outcomeLimit = outcomeLimit;
  }

  BasicClassIndex index() {
    return index;
  }

  public Request request(int source, WorkbenchCurrency action) {
    index.key(source);
    var key = new Key(source, Objects.requireNonNull(action));
    var cached = completed.get(key);
    if (cached != null) return new Request(key, cached);
    var existing = pending.get(key);
    if (existing != null) {
      shared++;
      return existing;
    }
    var created = new Request(key, null);
    pending.put(key, created);
    return created;
  }

  public int cachedKernels() {
    return completed.size();
  }

  public int cachedOutcomes() {
    return outcomes;
  }

  public int pendingKernels() {
    return pending.size();
  }

  public long calculatedKernels() {
    return calculated;
  }

  public long evictedKernels() {
    return evicted;
  }

  public long sharedRequests() {
    return shared;
  }

  /** A retained handle delivers a completed law even after its cache entry is evicted. */
  public final class Request {
    private final Key key;
    private BasicClassIndex.Cursor cursor;
    private ExactMethodRunner.Kernel<Integer> result;

    private Request(Key key, ExactMethodRunner.Kernel<Integer> result) {
      this.key = key;
      this.result = result;
      if (result == null) cursor = index.cursor(key.source(), key.action());
    }

    public boolean complete() {
      return result != null;
    }

    public void step() {
      if (complete()) return;
      if (!cursor.complete()) cursor.step();
      if (!cursor.complete()) return;
      result = cursor.result();
      cursor = null;
      pending.remove(key, this);
      calculated++;
      completed.put(key, result);
      outcomes += result.outcomes().size();
      while (completed.size() > kernelLimit || outcomes > outcomeLimit) {
        var removed = completed.pollFirstEntry();
        outcomes -= removed.getValue().outcomes().size();
        evicted++;
      }
    }

    public ExactMethodRunner.Kernel<Integer> result() {
      if (!complete()) throw new IllegalStateException("Partial shared indexed kernel");
      return result;
    }
  }
}
