package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.probability.Fraction;
import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.zip.*;

/**
 * 완료된 전이 커널과 진행 작업을 제한된 크기로 공유한다.
 *
 * <p>기존 계약: Job-local, bounded completed kernels with shared cooperative work and explicit delivery
 * handles. Access is serialized by the job scheduler; request sharing is not concurrent-thread
 * mutation.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "shared-indexed-kernel-store-v2-lossless-cold-cache"}
 *       ({@code String}).
 *   <li>{@code index}: 색인 ({@code BasicClassIndex}).
 *   <li>{@code kernelLimit}: 전이커널·한도 ({@code int}).
 *   <li>{@code outcomeLimit}: 결과·한도 ({@code int}).
 *   <li>{@code packedLimit}: 압축된·한도 ({@code long}).
 *   <li>{@code packed}: 압축된 ({@code LinkedHashMap<Key, Packed>}).
 *   <li>{@code packedBytes}: 압축된·바이트수 ({@code long}).
 *   <li>{@code packedRows}: 압축된·행목록 ({@code long}).
 *   <li>{@code cacheHits}: 캐시·도달목록 ({@code long}).
 *   <li>{@code packedHits}: 압축된·도달목록 ({@code long}).
 *   <li>{@code pressureEvictions}: pressure·캐시제거수 ({@code long}).
 *   <li>{@code closed}: 닫힘 ({@code boolean}).
 *   <li>{@code completed}: 완료 ({@code LinkedHashMap<Key, ExactMethodRunner.Kernel<Integer>>}).
 *   <li>{@code pending}: 대기 ({@code Map<Key, Request>}).
 *   <li>{@code outcomes}: 결과목록 ({@code int}).
 *   <li>{@code calculated}: calculated ({@code long}).
 *   <li>{@code evicted}: evicted ({@code long}).
 *   <li>{@code shared}: 공유 ({@code long}).
 *   <li>{@code diagnosticKeys}: diagnostic·키목록 ({@code Map<WorkbenchCurrency, BitSet>}).
 *   <li>{@code diagnosticStarts}: diagnostic·Starts ({@code long}).
 *   <li>{@code diagnosticUnique}: diagnostic·고유 ({@code long}).
 *   <li>{@code assumptions}: 확률·수치 계산에 사용한 명시적 가정 ({@code Set<WorkbenchSimulator.Assumption>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code enableDiagnostics()}: enable·진단값의 입력에 따른 분기·결과를 처리한다.
 *   <li>{@code workload()}: 작업량 값을 조회·구성한다. 반환: {@code Workload}.
 *   <li>{@code assumptions()}: 계산가정 값을 조회·구성한다. 반환: {@code List<WorkbenchSimulator.Assumption>}.
 *   <li>{@code IndexedKernelStore(index, kernelLimit, outcomeLimit)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력:
 *       {@code index}(색인), {@code kernelLimit}(전이커널·한도), {@code outcomeLimit}(결과·한도).
 *   <li>{@code IndexedKernelStore(index, kernelLimit, outcomeLimit, packedLimit)}: 전달받은 값으로 구성원을
 *       초기화한다. 입력: {@code index}(색인), {@code kernelLimit}(전이커널·한도), {@code outcomeLimit}(결과·한도),
 *       {@code packedLimit}(압축된·한도). 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code open()}: 조회할 자원 또는 작업 범위를 연다. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code cachedEntries()}: 캐시된·항목목록 값을 조회·구성한다. 반환: {@code int}.
 *   <li>{@code cacheMetrics()}: 캐시·측정값 값을 조회·구성한다. 반환: {@code CacheMetrics}.
 *   <li>{@code trimToEntries(limit)}: trim·대상·항목목록 처리에서 {@code completed.isEmpty}, {@code
 *       completed.pollFirstEntry}을(를) 사용한다. 입력: {@code limit}(한도). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code forget(value)}: forget의 입력에 따른 분기·결과를 처리한다. 입력: {@code value}(값).
 *   <li>{@code keep(key, result)}: keep 처리에서 {@code completed.put}, {@code result.outcomes}을(를)
 *       사용한다. 입력: {@code key}(키), {@code result}(결과).
 *   <li>{@code remember(key, result)}: remember 처리에서 {@code data.writeBoolean}, {@code
 *       result.unavailable}을(를) 사용한다. 입력: {@code key}(키), {@code result}(결과). 사용하는 예외: {@code
 *       UncheckedIOException}.
 *   <li>{@code write(data, value)}: 쓰기 처리에서 {@code value.numerator}, {@code value.denominator}을(를)
 *       사용한다. 입력: {@code data}(데이터), {@code value}(값). 사용하는 예외: {@code IOException}.
 *   <li>{@code integer(data)}: 정수 처리에서 {@code data.readInt}, {@code data.readNBytes}을(를) 사용한다. 입력:
 *       {@code data}(데이터). 반환: {@code BigInteger}. 사용하는 예외: {@code IOException}, {@code
 *       EOFException}.
 *   <li>{@code fraction(data)}: 유리수의 입력에 따른 분기·결과를 처리한다. 입력: {@code data}(데이터). 반환: {@code
 *       Fraction}. 사용하는 예외: {@code IOException}.
 *   <li>{@code restore(value)}: 저장된 상태로 실행 또는 조회 상태를 복원한다. 입력: {@code value}(값). 반환: {@code
 *       ExactMethodRunner.Kernel<Integer>}. 사용하는 예외: {@code IOException}, {@code
 *       UncheckedIOException}.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
 *   <li>{@code index()}: {@code index} 값을 조회한다. 반환: {@code BasicClassIndex}.
 *   <li>{@code request(source, action)}: 계산·실행에 사용할 요청 값을 구성한다. 입력: {@code source}(출처), {@code
 *       action}(행동). 반환: {@code Request}.
 *   <li>{@code cachedKernels()}: 캐시된·전이커널목록 값을 조회·구성한다. 반환: {@code int}.
 *   <li>{@code cachedOutcomes()}: {@code outcomes} 값을 조회한다. 반환: {@code int}.
 *   <li>{@code pendingKernels()}: 대기·전이커널목록 값을 조회·구성한다. 반환: {@code int}.
 *   <li>{@code calculatedKernels()}: {@code calculated} 값을 조회한다. 반환: {@code long}.
 *   <li>{@code evictedKernels()}: {@code evicted} 값을 조회한다. 반환: {@code long}.
 *   <li>{@code sharedRequests()}: {@code shared} 값을 조회한다. 반환: {@code long}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Key}: {@code IndexedKernelStore}에서 사용하는 키의 불변 값이다.
 *   <li>{@code Packed}: {@code IndexedKernelStore}에서 사용하는 압축된의 불변 값이다.
 *   <li>{@code Workload}: {@code IndexedKernelStore}에서 사용하는 작업량의 불변 값이다.
 *   <li>{@code CacheMetrics}: {@code IndexedKernelStore}에서 사용하는 캐시·측정값의 불변 값이다.
 *   <li>{@code Request}: {@code IndexedKernelStore} 내부의 요청 처리를 맡는다.
 * </ul>
 */
public final class IndexedKernelStore {
  public static final String VERSION = "shared-indexed-kernel-store-v2-lossless-cold-cache";

  /**
   * {@code IndexedKernelStore}에서 사용하는 키의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code source}: 출처 ({@code int}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   * </ul>
   */
  private record Key(int source, WorkbenchCurrency action) {}

  private final BasicClassIndex index;
  private final int kernelLimit;
  private final int outcomeLimit;
  private final long packedLimit;

  /**
   * {@code IndexedKernelStore}에서 사용하는 압축된의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code bytes}: 바이트수 ({@code byte[]}).
   *   <li>{@code rows}: 행목록 ({@code int}).
   * </ul>
   */
  private record Packed(byte[] bytes, int rows) {}

  private final LinkedHashMap<Key, Packed> packed = new LinkedHashMap<>(16, .75f, true);
  private long packedBytes, packedRows, cacheHits, packedHits, pressureEvictions;
  private boolean closed;
  private final LinkedHashMap<Key, ExactMethodRunner.Kernel<Integer>> completed =
      new LinkedHashMap<>(16, .75f, true);
  private final Map<Key, Request> pending = new HashMap<>();
  private int outcomes;
  private long calculated, evicted, shared;
  private Map<WorkbenchCurrency, BitSet> diagnosticKeys;
  private long diagnosticStarts, diagnosticUnique;

  /** Opt-in measurement only; never used to admit, merge or discard a transition. */
  public void enableDiagnostics() {
    if (diagnosticKeys == null) diagnosticKeys = new EnumMap<>(WorkbenchCurrency.class);
  }

  /**
   * {@code IndexedKernelStore}에서 사용하는 작업량의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code starts}: starts ({@code long}).
   *   <li>{@code uniqueKeys}: 고유·키목록 ({@code long}).
   *   <li>{@code repeatedStarts}: repeated·Starts ({@code long}).
   *   <li>{@code keyBytes}: 키·바이트수 ({@code long}).
   * </ul>
   */
  public record Workload(long starts, long uniqueKeys, long repeatedStarts, long keyBytes) {}

  public Workload workload() {
    return new Workload(
        diagnosticStarts,
        diagnosticUnique,
        diagnosticStarts - diagnosticUnique,
        diagnosticKeys == null
            ? 0
            : diagnosticKeys.values().stream().mapToLong(b -> b.size() / 8L).sum());
  }

  private final Set<WorkbenchSimulator.Assumption> assumptions = new LinkedHashSet<>();

  public List<WorkbenchSimulator.Assumption> assumptions() {
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

  /**
   * {@code IndexedKernelStore}에서 사용하는 캐시·측정값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code hits}: 도달목록 ({@code long}).
   *   <li>{@code packedHits}: 압축된·도달목록 ({@code long}).
   *   <li>{@code packedKernels}: 압축된·전이커널목록 ({@code int}).
   *   <li>{@code packedRows}: 압축된·행목록 ({@code long}).
   *   <li>{@code packedBytes}: 압축된·바이트수 ({@code long}).
   *   <li>{@code packedByteLimit}: 압축된·Byte·한도 ({@code long}).
   *   <li>{@code pressureEvictions}: pressure·캐시제거수 ({@code long}).
   * </ul>
   */
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
    if (diagnosticKeys != null) diagnosticKeys.clear();
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
    if (diagnosticKeys != null) {
      diagnosticStarts++;
      var keys = diagnosticKeys.computeIfAbsent(action, ignored -> new BitSet());
      if (!keys.get(source)) {
        keys.set(source);
        diagnosticUnique++;
      }
    }
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

  /**
   * {@code IndexedKernelStore} 내부의 요청 처리를 맡는다.
   *
   * <p>기존 계약: A retained handle delivers a completed law even after its cache entry is evicted.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code key}: 키 ({@code Key}).
   *   <li>{@code cursor}: 진행커서 ({@code BasicClassIndex.Cursor}).
   *   <li>{@code result}: 결과 ({@code ExactMethodRunner.Kernel<Integer>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code store()}: 저장소 값을 조회·구성한다. 반환: {@code IndexedKernelStore}.
   *   <li>{@code source()}: 출처 값을 조회·구성한다. 반환: {@code int}.
   *   <li>{@code action()}: 행동 값을 조회·구성한다. 반환: {@code WorkbenchCurrency}.
   *   <li>{@code Request(key, result)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code key}(키), {@code
   *       result}(결과).
   *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
   *   <li>{@code result()}: 최종 또는 현재 계산 결과를 구성한다. 반환: {@code ExactMethodRunner.Kernel<Integer>}.
   *       사용하는 예외: {@code IllegalStateException}.
   * </ul>
   */
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
