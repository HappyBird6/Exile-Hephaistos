package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.WorkbenchCurrency;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.zip.*;

/**
 * Job-local, bounded completed kernels with shared cooperative work and explicit delivery handles.
 * Access is serialized by the job scheduler; request sharing is not concurrent-thread mutation.
 */
public final class IndexedKernelStore {
  public static final String VERSION = "shared-indexed-kernel-store-v2-lossless-cold-cache";

  private record Key(int source, WorkbenchCurrency action) {}

  private final BasicClassIndex index;
  private final int kernelLimit;
  private final int outcomeLimit;
  private final long packedLimit;

  private record Packed(byte[] bytes, int rows) {}

  private final LinkedHashMap<Key, Packed> packed = new LinkedHashMap<>(16, .75f, true);
  private long packedBytes, packedRows, cacheHits, packedHits, pressureEvictions;
  private boolean closed;
  private final LinkedHashMap<Key, ExactMethodRunner.Kernel<Integer>> completed =
      new LinkedHashMap<>(16, .75f, true);
  private final Map<Key, Request> pending = new HashMap<>();
  private int outcomes;
  private long calculated, evicted, shared;
  private final Set<com.poe2craft.crafting.domain.WorkbenchSimulator.Assumption> assumptions =
      new LinkedHashSet<>();

  public List<com.poe2craft.crafting.domain.WorkbenchSimulator.Assumption> assumptions() {
    return List.copyOf(assumptions);
  }

  public IndexedKernelStore(BasicClassIndex index, int kernelLimit, int outcomeLimit) {
    this(index, kernelLimit, outcomeLimit, 0);
  }

  public IndexedKernelStore(
      BasicClassIndex index, int kernelLimit, int outcomeLimit, long packedLimit) {
    this.index = Objects.requireNonNull(index);
    if (kernelLimit < 1 || outcomeLimit < 1 || packedLimit < 0)
      throw new IllegalArgumentException("Positive cache budgets required");
    this.kernelLimit = kernelLimit;
    this.outcomeLimit = outcomeLimit;
    this.packedLimit = packedLimit;
  }

  private void open() {
    if (closed) throw new IllegalStateException("Kernel store closed");
  }

  public int cachedEntries() {
    return outcomes + completed.size() + packed.size();
  }

  public record CacheMetrics(
      long hits,
      long packedHits,
      int packedKernels,
      long packedRows,
      long packedBytes,
      long packedByteLimit,
      long pressureEvictions) {}

  public CacheMetrics cacheMetrics() {
    return new CacheMetrics(
        cacheHits,
        packedHits,
        packed.size(),
        packedRows,
        packedBytes,
        packedLimit,
        pressureEvictions);
  }

  /** Recomputable caches yield to working state; existing delivery handles remain valid. */
  public void trimToEntries(int limit) {
    open();
    if (limit < 0) throw new IllegalArgumentException("Negative cache allowance");
    while (cachedEntries() > limit && !completed.isEmpty()) {
      var removed = completed.pollFirstEntry();
      outcomes -= removed.getValue().outcomes().size();
      evicted++;
      pressureEvictions++;
      remember(removed.getKey(), removed.getValue());
    }
    while (cachedEntries() > limit && !packed.isEmpty()) {
      forget(packed.pollFirstEntry().getValue());
      pressureEvictions++;
    }
  }

  private void forget(Packed value) {
    packedBytes -= value.bytes.length;
    packedRows -= value.rows;
  }

  private void keep(Key key, ExactMethodRunner.Kernel<Integer> result) {
    completed.put(key, result);
    outcomes += result.outcomes().size();
    while (completed.size() > kernelLimit || outcomes > outcomeLimit) {
      var removed = completed.pollFirstEntry();
      outcomes -= removed.getValue().outcomes().size();
      evicted++;
      remember(removed.getKey(), removed.getValue());
    }
  }

  private void remember(Key key, ExactMethodRunner.Kernel<Integer> result) {
    if (packedLimit == 0) return;
    try {
      var buffer = new ByteArrayOutputStream();
      var compressor = new Deflater(Deflater.BEST_SPEED);
      try (var data = new DataOutputStream(new DeflaterOutputStream(buffer, compressor))) {
        data.writeBoolean(result.unavailable());
        write(data, result.unresolved());
        data.writeInt(result.outcomes().size());
        for (var outcome : result.outcomes()) {
          data.writeInt(outcome.state());
          write(data, outcome.probability());
        }
      } finally {
        compressor.end();
      }
      var bytes = buffer.toByteArray();
      if (bytes.length > packedLimit) return;
      var old = packed.put(key, new Packed(bytes, result.outcomes().size()));
      if (old != null) forget(old);
      packedBytes += bytes.length;
      packedRows += result.outcomes().size();
      while (packedBytes > packedLimit) forget(packed.pollFirstEntry().getValue());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static void write(DataOutputStream data, Fraction value) throws IOException {
    for (var integer : List.of(value.numerator(), value.denominator())) {
      var bytes = integer.toByteArray();
      data.writeInt(bytes.length);
      data.write(bytes);
    }
  }

  private static BigInteger integer(DataInputStream data) throws IOException {
    int length = data.readInt();
    if (length < 1) throw new IOException("Invalid cached integer");
    var bytes = data.readNBytes(length);
    if (bytes.length != length) throw new EOFException();
    return new BigInteger(bytes);
  }

  private static Fraction fraction(DataInputStream data) throws IOException {
    return new Fraction(integer(data), integer(data));
  }

  private static ExactMethodRunner.Kernel<Integer> restore(Packed value) {
    try (var data =
        new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(value.bytes)))) {
      boolean unavailable = data.readBoolean();
      var unresolved = fraction(data);
      int count = data.readInt();
      if (count != value.rows) throw new IOException("Invalid cached kernel size");
      var outcomes = new ArrayList<ExactMethodRunner.Outcome<Integer>>();
      for (int i = 0; i < count; i++)
        outcomes.add(new ExactMethodRunner.Outcome<>(data.readInt(), fraction(data)));
      if (data.read() != -1) throw new IOException("Trailing cached kernel data");
      return new ExactMethodRunner.Kernel<>(outcomes, unresolved, unavailable);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public void close() {
    closed = true;
    completed.clear();
    packed.clear();
    pending.clear();
    outcomes = 0;
    packedRows = 0;
    packedBytes = 0;
  }

  BasicClassIndex index() {
    return index;
  }

  public Request request(int source, WorkbenchCurrency action) {
    open();
    index.key(source);
    var key = new Key(source, Objects.requireNonNull(action));
    var cached = completed.get(key);
    if (cached != null) {
      cacheHits++;
      return new Request(key, cached);
    }
    var cold = packed.remove(key);
    if (cold != null) {
      forget(cold);
      var restored = restore(cold);
      packedHits++;
      keep(key, restored);
      return new Request(key, restored);
    }
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

    public IndexedKernelStore store() {
      return IndexedKernelStore.this;
    }

    public int source() {
      return key.source();
    }

    public WorkbenchCurrency action() {
      return key.action();
    }

    private Request(Key key, ExactMethodRunner.Kernel<Integer> result) {
      this.key = key;
      this.result = result;
      if (result == null) {
        cursor = index.cursor(key.source(), key.action());
        assumptions.addAll(cursor.assumptions());
      }
    }

    public boolean complete() {
      return result != null;
    }

    public void step() {
      open();
      if (complete()) return;
      if (!cursor.complete()) cursor.step();
      if (!cursor.complete()) return;
      result = cursor.result();
      cursor = null;
      pending.remove(key, this);
      calculated++;
      keep(key, result);
    }

    public ExactMethodRunner.Kernel<Integer> result() {
      if (!complete()) throw new IllegalStateException("Partial shared indexed kernel");
      return result;
    }
  }
}
