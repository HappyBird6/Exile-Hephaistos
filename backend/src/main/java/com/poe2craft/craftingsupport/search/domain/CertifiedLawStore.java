package com.poe2craft.craftingsupport.search.domain;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.zip.*;

/**
 * 확률 분포를 손실 없이 페이지로 저장하고 읽기 캐시의 객체 수를 제한한다.
 *
 * <p>기존 계약: Lossless class-law pages. Interval objects are materialized only in a bounded read
 * cache.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "certified-law-pages-v2-backed-payloads"} ({@code String}).
 *   <li>{@code PAGE_ROWS}: 행목록; 초기값 {@code 256} ({@code int}).
 *   <li>{@code bits}: 비트수 ({@code int}).
 *   <li>{@code pages}: 페이지목록 ({@code Map<Integer, List<Page>>}).
 *   <li>{@code backing}: backing ({@code PackedPages}).
 *   <li>{@code pageCount}: 페이지·개수 ({@code int}).
 *   <li>{@code cache}: 캐시 ({@code LinkedHashMap<Page, Decoded>}).
 *   <li>{@code bytes}: 바이트수 ({@code long}).
 *   <li>{@code rows}: 행목록 ({@code long}).
 *   <li>{@code reused}: reused ({@code long}).
 *   <li>{@code decoded}: 짧은 ID에서 원본 키로 가는 도메인별 불변 색인 ({@code long}).
 *   <li>{@code cachedRows}: 캐시된·행목록 ({@code int}).
 *   <li>{@code descriptors}: descriptors ({@code long}).
 *   <li>{@code closed}: 닫힘 ({@code boolean}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code page(law, offset, limit)}: 페이지 처리에서 {@code law.size}, {@code paged.page}을(를) 사용한다.
 *       입력: {@code law}(분포), {@code offset}(시작위치), {@code limit}(한도). 반환: {@code List<Map.Entry<K,
 *       CertifiedInterval>>}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code CertifiedLawStore(bits)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code bits}(비트수).
 *   <li>{@code CertifiedLawStore(bits, backing)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code bits}(비트수),
 *       {@code backing}(backing).
 *   <li>{@code metrics()}: 측정값 값을 조회·구성한다. 반환: {@code Metrics}.
 *   <li>{@code materializedRows()}: {@code cachedRows} 값을 조회한다. 반환: {@code int}.
 *   <li>{@code packedBytes()}: {@code bytes} 값을 조회한다. 반환: {@code long}.
 *   <li>{@code pageCount()}: {@code pageCount} 값을 조회한다. 반환: {@code int}.
 *   <li>{@code residentPackedBytes()}: 메모리상주·압축된·바이트수 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code descriptors()}: {@code descriptors} 값을 조회한다. 반환: {@code long}.
 *   <li>{@code open()}: 조회할 자원 또는 작업 범위를 연다. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code freeze(input)}: 변경 중인 값을 불변 조회·저장 표현으로 만든다. 입력: {@code input}(입력). 반환: {@code
 *       Map<Integer, CertifiedInterval>}.
 *   <li>{@code prepare(input)}: prepare의 입력에 따른 분기·결과를 처리한다. 입력: {@code input}(입력). 반환: {@code
 *       Freeze}.
 *   <li>{@code pack(entries)}: pack 처리에서 {@code data.writeInt}, {@code entries.size}을(를) 사용한다. 입력:
 *       {@code entries}(항목목록). 반환: {@code Page}. 사용하는 예외: {@code UncheckedIOException}.
 *   <li>{@code write(data, value)}: 쓰기 처리에서 {@code value.toByteArray}, {@code data.writeByte}을(를)
 *       사용한다. 입력: {@code data}(데이터), {@code value}(값). 사용하는 예외: {@code IOException}.
 *   <li>{@code read(page)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code page}(페이지). 반환: {@code Decoded}. 사용하는
 *       예외: {@code IllegalStateException}, {@code UncheckedIOException}.
 *   <li>{@code integer(data)}: 정수 처리에서 {@code data.readUnsignedByte}, {@code data.readNBytes}을(를)
 *       사용한다. 입력: {@code data}(데이터). 반환: {@code BigInteger}. 사용하는 예외: {@code IOException}, {@code
 *       EOFException}.
 *   <li>{@code total(law, bits)}: 합계 처리에서 {@code packed.total}, {@code law.values}을(를) 사용한다. 입력:
 *       {@code law}(분포), {@code bits}(비트수). 반환: {@code CertifiedInterval}.
 *   <li>{@code total(values, bits)}: 합계 처리에서 {@code law.total}, {@code CertifiedInterval.zero}을(를)
 *       사용한다. 입력: {@code values}(값목록), {@code bits}(비트수). 반환: {@code CertifiedInterval}.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code PagedLaw}: {@code CertifiedLawStore} 내부의 Paged·분포 처리를 맡는다.
 *   <li>{@code PackedPages}: {@code CertifiedLawStore} 내부의 압축된·페이지목록 처리를 맡는다.
 *   <li>{@code MemoryPages}: {@code CertifiedLawStore} 내부의 메모리·페이지목록 처리를 맡는다.
 *   <li>{@code Page}: {@code CertifiedLawStore}에서 사용하는 페이지의 불변 값이다.
 *   <li>{@code Decoded}: {@code CertifiedLawStore}에서 사용하는 복원값의 불변 값이다.
 *   <li>{@code Metrics}: {@code CertifiedLawStore}에서 사용하는 측정값의 불변 값이다.
 *   <li>{@code Freeze}: {@code CertifiedLawStore} 내부의 불변복사 처리를 맡는다.
 *   <li>{@code Values}: {@code CertifiedLawStore} 내부의 값목록 처리를 맡는다.
 *   <li>{@code Law}: {@code CertifiedLawStore} 내부의 분포 처리를 맡는다.
 * </ul>
 */
public final class CertifiedLawStore implements AutoCloseable {
  public static final String VERSION = "certified-law-pages-v2-backed-payloads";
  private static final int PAGE_ROWS = 256;
  private final int bits;
  private final Map<Integer, List<Page>> pages = new HashMap<>();
  private final PackedPages backing;
  private int pageCount;
  private final LinkedHashMap<Page, Decoded> cache = new LinkedHashMap<>(16, .75f, true);
  private long bytes, rows, reused, decoded;
  private int cachedRows;
  private long descriptors;
  private boolean closed;

  /**
   * {@code CertifiedLawStore} 내부의 Paged·분포 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code total()}: 합계 값을 조회·구성한다. 반환: {@code CertifiedInterval}.
   *   <li>{@code page(offset, limit)}: 페이지의 실행 계약을 정의한다. 입력: {@code offset}(시작위치), {@code
   *       limit}(한도). 반환: {@code List<Map.Entry<K, CertifiedInterval>>}.
   * </ul>
   */
  public interface PagedLaw<K> extends Map<K, CertifiedInterval> {
    CertifiedInterval total();

    List<Map.Entry<K, CertifiedInterval>> page(int offset, int limit);
  }

  public static <K> List<Map.Entry<K, CertifiedInterval>> page(
      Map<K, CertifiedInterval> law, int offset, int limit) {
    if (offset < 0 || offset > law.size() || limit < 0)
      throw new IllegalArgumentException("Invalid law page");
    if (law instanceof PagedLaw<K> paged) return paged.page(offset, limit);
    return law.entrySet().stream()
        .skip(offset)
        .limit(limit)
        .map(e -> Map.entry(e.getKey(), e.getValue()))
        .toList();
  }

  public CertifiedLawStore(int bits) {
    this(bits, new MemoryPages());
  }

  public CertifiedLawStore(int bits, PackedPages backing) {
    new CertifiedInterval.Precision(bits, 0);
    this.bits = bits;
    this.backing = Objects.requireNonNull(backing);
  }

  /**
   * {@code CertifiedLawStore} 내부의 압축된·페이지목록 처리를 맡는다.
   *
   * <p>기존 계약: Job-owned immutable payload storage; returned byte arrays are borrowed read-only
   * views.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code store(bytes)}: 저장소의 실행 계약을 정의한다. 입력: {@code bytes}(바이트수). 반환: {@code Payload}.
   *   <li>{@code residentBytes()}: 메모리상주·바이트수 값을 조회·구성한다. 반환: {@code long}.
   *   <li>{@code diskBytes()}: 디스크·바이트수 값을 조회·구성한다. 반환: {@code long}.
   *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
   * </ul>
   *
   * <p>중첩 타입:
   *
   * <ul>
   *   <li>{@code Payload}: {@code PackedPages} 내부의 저장내용 처리를 맡는다.
   * </ul>
   */
  public interface PackedPages extends AutoCloseable {
    /**
     * {@code PackedPages} 내부의 저장내용 처리를 맡는다.
     *
     * <p>생성자·메서드:
     *
     * <ul>
     *   <li>{@code read()}: 지정한 입력 또는 저장된 값을 읽는다. 반환: {@code byte[]}.
     * </ul>
     */
    interface Payload {
      byte[] read();
    }

    Payload store(byte[] bytes);

    long residentBytes();

    long diskBytes();

    @Override
    void close();
  }

  /**
   * {@code CertifiedLawStore} 내부의 메모리·페이지목록 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code data}: 데이터 ({@code Map<Payload, byte[]>}).
   *   <li>{@code bytes}: 바이트수 ({@code long}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code store(value)}: 저장소 처리에서 {@code data.put}을(를) 사용한다. 입력: {@code value}(값). 반환:
   *       {@code Payload}.
   *   <li>{@code residentBytes()}: {@code bytes} 값을 조회한다. 반환: {@code long}.
   *   <li>{@code diskBytes()}: {@code 0} 값을 조회한다. 반환: {@code long}.
   *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
   * </ul>
   *
   * <p>중첩 타입:
   *
   * <ul>
   *   <li>{@code MemoryPayload}: {@code MemoryPages} 내부의 메모리·저장내용 처리를 맡는다.
   * </ul>
   */
  private static final class MemoryPages implements PackedPages {
    private final Map<Payload, byte[]> data = new IdentityHashMap<>();
    private long bytes;

    /**
     * {@code MemoryPages} 내부의 메모리·저장내용 처리를 맡는다.
     *
     * <p>생성자·메서드:
     *
     * <ul>
     *   <li>{@code read()}: 지정한 입력 또는 저장된 값을 읽는다. 반환: {@code byte[]}. 사용하는 예외: {@code
     *       IllegalStateException}.
     * </ul>
     */
    private final class MemoryPayload implements Payload {
      public byte[] read() {
        var found = data.get(this);
        if (found == null) throw new IllegalStateException("Page storage closed");
        return found;
      }
    }

    public Payload store(byte[] value) {
      var payload = new MemoryPayload();
      data.put(payload, value);
      bytes += value.length;
      return payload;
    }

    public long residentBytes() {
      return bytes;
    }

    public long diskBytes() {
      return 0;
    }

    public void close() {
      data.clear();
      bytes = 0;
    }
  }

  /**
   * {@code CertifiedLawStore}에서 사용하는 페이지의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code bytes}: 바이트수 ({@code PackedPages.Payload}).
   *   <li>{@code first}: 최초 ({@code int}).
   *   <li>{@code last}: 마지막 ({@code int}).
   *   <li>{@code size}: 크기 ({@code int}).
   * </ul>
   */
  private record Page(PackedPages.Payload bytes, int first, int last, int size) {}

  /**
   * {@code CertifiedLawStore}에서 사용하는 복원값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code keys}: 키목록 ({@code int[]}).
   *   <li>{@code values}: 출처 단위로 보존하는 수치 값 ({@code CertifiedInterval[]}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code entry(index)}: 항목의 입력에 따른 분기·결과를 처리한다. 입력: {@code index}(색인). 반환: {@code
   *       Map.Entry<Integer, CertifiedInterval>}.
   * </ul>
   */
  private record Decoded(int[] keys, CertifiedInterval[] values) {
    Map.Entry<Integer, CertifiedInterval> entry(int index) {
      return Map.entry(keys[index], values[index]);
    }
  }

  /**
   * {@code CertifiedLawStore}에서 사용하는 측정값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code storedRows}: 저장된·행목록 ({@code long}).
   *   <li>{@code packedBytes}: 압축된·바이트수 ({@code long}).
   *   <li>{@code uniquePages}: 고유·페이지목록 ({@code int}).
   *   <li>{@code materializedRows}: materialized·행목록 ({@code int}).
   *   <li>{@code reusedPages}: reused·페이지목록 ({@code long}).
   *   <li>{@code descriptors}: descriptors ({@code long}).
   *   <li>{@code decodedPages}: 복원값·페이지목록 ({@code long}).
   *   <li>{@code residentPackedBytes}: 메모리상주·압축된·바이트수 ({@code long}).
   *   <li>{@code diskBytes}: 디스크·바이트수 ({@code long}).
   * </ul>
   */
  public record Metrics(
      long storedRows,
      long packedBytes,
      int uniquePages,
      int materializedRows,
      long reusedPages,
      long descriptors,
      long decodedPages,
      long residentPackedBytes,
      long diskBytes) {}

  public Metrics metrics() {
    return new Metrics(
        rows,
        bytes,
        pageCount,
        materializedRows(),
        reused,
        descriptors,
        decoded,
        backing.residentBytes(),
        backing.diskBytes());
  }

  public int materializedRows() {
    return cachedRows;
  }

  public long packedBytes() {
    return bytes;
  }

  public int pageCount() {
    return pageCount;
  }

  public long residentPackedBytes() {
    return backing.residentBytes();
  }

  public long descriptors() {
    return descriptors;
  }

  private void open() {
    if (closed) throw new IllegalStateException("Law store closed");
  }

  public Map<Integer, CertifiedInterval> freeze(Map<Integer, CertifiedInterval> input) {
    var cursor = prepare(input);
    while (!cursor.complete()) cursor.step();
    return cursor.result();
  }

  /** Package-internal borrowed input, protected by the runner's observation lease. */
  Freeze prepare(Map<Integer, CertifiedInterval> input) {
    open();
    return new Freeze(input);
  }

  /**
   * {@code CertifiedLawStore} 내부의 불변복사 처리를 맡는다.
   *
   * <p>기존 계약: One bounded page per step; the transient source is released when the descriptor is
   * ready.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code input}: 입력 ({@code Map<Integer, CertifiedInterval>}).
   *   <li>{@code keys}: 키목록 ({@code int[]}).
   *   <li>{@code scratch}: scratch ({@code int[]}).
   *   <li>{@code inputKeys}: 입력·키목록 ({@code Iterator<Integer>}).
   *   <li>{@code counts}: 개수목록 ({@code int[]}).
   *   <li>{@code phase}: 단계 ({@code int}).
   *   <li>{@code shift}: shift ({@code int}).
   *   <li>{@code position}: 진행위치 ({@code int}).
   *   <li>{@code result}: 결과 ({@code List<Page>}).
   *   <li>{@code total}: 합계 ({@code CertifiedInterval}).
   *   <li>{@code finished}: 종료 ({@code Map<Integer, CertifiedInterval>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Freeze(input)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code input}(입력).
   *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code transientBytes()}: 임시·바이트수 값을 조회·구성한다. 반환: {@code long}.
   *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다. 사용하는 예외: {@code IllegalArgumentException}.
   *   <li>{@code result()}: 최종 또는 현재 계산 결과를 구성한다. 반환: {@code Map<Integer, CertifiedInterval>}. 사용하는
   *       예외: {@code IllegalStateException}.
   * </ul>
   */
  public final class Freeze {
    private Map<Integer, CertifiedInterval> input;
    private int[] keys;
    private int[] scratch;
    private Iterator<Integer> inputKeys;
    private final int[] counts = new int[256];
    private int phase, shift;
    private int position;
    private final List<Page> result = new ArrayList<>();
    private CertifiedInterval total = CertifiedInterval.zero(bits);
    private Map<Integer, CertifiedInterval> finished;

    Freeze(Map<Integer, CertifiedInterval> input) {
      this.input = input;
      if (input instanceof Law law && law.owner() == CertifiedLawStore.this) finished = input;
      else if (input.isEmpty()) finished = Map.of();
      if (finished != null) {
        this.input = null;
        return;
      }
      keys = new int[input.size()];
      scratch = new int[input.size()];
      inputKeys = input.keySet().iterator();
    }

    public boolean complete() {
      open();
      return finished != null;
    }

    public long transientBytes() {
      return keys == null ? 0 : 8L * keys.length + 1024;
    }

    public void step() {
      open();
      if (complete()) return;
      if (phase == 0) {
        for (int n = 0; n < PAGE_ROWS && inputKeys.hasNext(); n++) {
          int key = inputKeys.next();
          if (key < 0) throw new IllegalArgumentException("Negative class id");
          keys[position++] = key;
        }
        if (!inputKeys.hasNext()) {
          inputKeys = null;
          position = 0;
          phase = 1;
        }
        return;
      }
      if (phase == 1) {
        int end = Math.min(position + PAGE_ROWS, keys.length);
        for (; position < end; position++) counts[(keys[position] >>> shift) & 255]++;
        if (position == keys.length) {
          position = 0;
          phase = 2;
        }
        return;
      }
      if (phase == 2) {
        int offset = 0;
        for (int i = 0; i < counts.length; i++) {
          int count = counts[i];
          counts[i] = offset;
          offset += count;
        }
        phase = 3;
        return;
      }
      if (phase == 3) {
        int end = Math.min(position + PAGE_ROWS, keys.length);
        for (; position < end; position++) {
          int key = keys[position];
          scratch[counts[(key >>> shift) & 255]++] = key;
        }
        if (position == keys.length) {
          var swap = keys;
          keys = scratch;
          scratch = swap;
          shift += 8;
          position = 0;
          Arrays.fill(counts, 0);
          phase = shift == 32 ? 4 : 1;
        }
        return;
      }
      var batch = new ArrayList<Map.Entry<Integer, CertifiedInterval>>(PAGE_ROWS);
      int end = Math.min(position + PAGE_ROWS, keys.length);
      for (; position < end; position++) {
        int key = keys[position];
        var value = input.get(key);
        if (key < 0
            || value.fractionalBits() != bits
            || value.upperUnits().compareTo(BigInteger.ONE.shiftLeft(bits)) > 0)
          throw new IllegalArgumentException("Invalid probability class law");
        total = total.add(value);
        batch.add(Map.entry(key, value));
      }
      result.add(pack(batch));
      if (position == keys.length) {
        descriptors += 1L + result.size();
        finished = new Law(List.copyOf(result), keys.length, total.probability());
        input = null;
        keys = null;
        scratch = null;
        result.clear();
      }
    }

    public Map<Integer, CertifiedInterval> result() {
      if (!complete()) throw new IllegalStateException("Partial law freeze");
      return finished;
    }
  }

  private Page pack(List<Map.Entry<Integer, CertifiedInterval>> entries) {
    try {
      var buffer = new ByteArrayOutputStream();
      var compressor = new Deflater(Deflater.BEST_SPEED);
      try (var data = new DataOutputStream(new DeflaterOutputStream(buffer, compressor))) {
        data.writeInt(entries.size());
        for (var entry : entries) {
          data.writeInt(entry.getKey());
          write(data, entry.getValue().lowerUnits());
          write(data, entry.getValue().upperUnits());
        }
      } finally {
        compressor.end();
      }
      var payload = buffer.toByteArray();
      int hash = Arrays.hashCode(payload);
      var matches = pages.computeIfAbsent(hash, ignored -> new ArrayList<>());
      for (var old : matches)
        if (Arrays.equals(old.bytes.read(), payload)) {
          reused++;
          return old;
        }
      var page =
          new Page(
              backing.store(payload),
              entries.getFirst().getKey(),
              entries.getLast().getKey(),
              entries.size());
      matches.add(page);
      pageCount++;
      bytes += payload.length;
      rows += entries.size();
      return page;
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private void write(DataOutputStream data, BigInteger value) throws IOException {
    var encoded = value.toByteArray();
    data.writeByte(encoded.length);
    data.write(encoded);
  }

  private Decoded read(Page page) {
    open();
    var found = cache.get(page);
    if (found != null) return found;
    var keys = new int[page.size];
    var values = new CertifiedInterval[page.size];
    try (var data =
        new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(page.bytes.read())))) {
      int count = data.readInt();
      if (count != page.size) throw new IllegalStateException("Corrupt page");
      for (int i = 0; i < count; i++) {
        keys[i] = data.readInt();
        values[i] = new CertifiedInterval(bits, integer(data), integer(data));
      }
      if (data.read() != -1) throw new IllegalStateException("Trailing page data");
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    var immutable = new Decoded(keys, values);
    decoded++;
    cache.put(page, immutable);
    cachedRows += keys.length;
    while (cache.size() > 4)
      cachedRows -= cache.remove(cache.keySet().iterator().next()).keys.length;
    return immutable;
  }

  private BigInteger integer(DataInputStream data) throws IOException {
    int length = data.readUnsignedByte();
    if (length < 1 || length > bits / 8 + 2) throw new IOException("Invalid interval width");
    var value = data.readNBytes(length);
    if (value.length != length) throw new EOFException();
    return new BigInteger(value);
  }

  public static CertifiedInterval total(Map<?, CertifiedInterval> law, int bits) {
    if (law instanceof PagedLaw<?> packed) return packed.total();
    return total(law.values(), bits);
  }

  public static CertifiedInterval total(Collection<CertifiedInterval> values, int bits) {
    if (values instanceof CertifiedLawStore.Values packed) return packed.law.total();
    var sum = CertifiedInterval.zero(bits);
    for (var value : values) sum = sum.add(value);
    return sum.probability();
  }

  /**
   * {@code CertifiedLawStore} 내부의 값목록 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code law}: 분포 ({@code Law}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Values(law)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code law}(분포).
   *   <li>{@code size()}: 보유 항목의 개수를 조회한다. 반환: {@code int}.
   *   <li>{@code iterator()}: 항목을 순서대로 읽는 반복자를 제공한다. 반환: {@code Iterator<CertifiedInterval>}.
   * </ul>
   */
  private final class Values extends AbstractCollection<CertifiedInterval> {
    final Law law;

    Values(Law law) {
      this.law = law;
    }

    @Override
    public int size() {
      return law.size();
    }

    @Override
    public Iterator<CertifiedInterval> iterator() {
      var entries = law.entrySet().iterator();
      return new Iterator<>() {
        @Override
        public boolean hasNext() {
          return entries.hasNext();
        }

        @Override
        public CertifiedInterval next() {
          return entries.next().getValue();
        }
      };
    }
  }

  /**
   * {@code CertifiedLawStore} 내부의 분포 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code parts}: 부분목록 ({@code List<Page>}).
   *   <li>{@code size}: 크기 ({@code int}).
   *   <li>{@code total}: 합계 ({@code CertifiedInterval}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Law(parts, size, total)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code parts}(부분목록), {@code
   *       size}(크기), {@code total}(합계).
   *   <li>{@code owner()}: 요청 또는 작업의 소유자를 판정한다. 반환: {@code CertifiedLawStore}.
   *   <li>{@code total()}: 합계 값을 조회·구성한다. 반환: {@code CertifiedInterval}.
   *   <li>{@code page(offset, limit)}: 페이지 처리에서 {@code parts.get}, {@code result.add}을(를) 사용한다. 입력:
   *       {@code offset}(시작위치), {@code limit}(한도). 반환: {@code List<Map.Entry<Integer,
   *       CertifiedInterval>>}. 사용하는 예외: {@code IllegalArgumentException}.
   *   <li>{@code values()}: 값목록 값을 조회·구성한다. 반환: {@code Collection<CertifiedInterval>}.
   *   <li>{@code size()}: 보유 항목의 개수를 조회한다. 반환: {@code int}.
   *   <li>{@code get(key)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code key}(키). 반환: {@code
   *       CertifiedInterval}.
   *   <li>{@code entrySet()}: 항목·집합 값을 조회·구성한다. 반환: {@code Set<Map.Entry<Integer,
   *       CertifiedInterval>>}. 사용하는 예외: {@code NoSuchElementException}.
   * </ul>
   */
  private final class Law extends AbstractMap<Integer, CertifiedInterval>
      implements PagedLaw<Integer> {
    final List<Page> parts;
    final int size;
    final CertifiedInterval total;

    Law(List<Page> parts, int size, CertifiedInterval total) {
      this.parts = parts;
      this.size = size;
      this.total = total;
    }

    CertifiedLawStore owner() {
      return CertifiedLawStore.this;
    }

    @Override
    public CertifiedInterval total() {
      open();
      return total;
    }

    @Override
    public List<Map.Entry<Integer, CertifiedInterval>> page(int offset, int limit) {
      open();
      if (offset < 0 || offset > size || limit < 0)
        throw new IllegalArgumentException("Invalid law page");
      var result = new ArrayList<Map.Entry<Integer, CertifiedInterval>>();
      int end = (int) Math.min(size, (long) offset + limit);
      for (int part = offset / PAGE_ROWS; offset < end; part++) {
        var decoded = read(parts.get(part));
        for (int row = offset % PAGE_ROWS;
            row < decoded.keys.length && offset < end;
            row++, offset++) result.add(decoded.entry(row));
      }
      return List.copyOf(result);
    }

    @Override
    public Collection<CertifiedInterval> values() {
      open();
      return new Values(this);
    }

    @Override
    public int size() {
      open();
      return size;
    }

    @Override
    public CertifiedInterval get(Object key) {
      open();
      if (!(key instanceof Integer target)) return null;
      int low = 0, high = parts.size() - 1;
      while (low <= high) {
        int mid = (low + high) >>> 1;
        var page = parts.get(mid);
        if (target < page.first) high = mid - 1;
        else if (target > page.last) low = mid + 1;
        else {
          var decoded = read(page);
          int found = Arrays.binarySearch(decoded.keys, target);
          return found < 0 ? null : decoded.values[found];
        }
      }
      return null;
    }

    @Override
    public Set<Map.Entry<Integer, CertifiedInterval>> entrySet() {
      open();
      return Collections.unmodifiableSet(
          new AbstractSet<>() {
            @Override
            public int size() {
              return Law.this.size();
            }

            @Override
            public Iterator<Map.Entry<Integer, CertifiedInterval>> iterator() {
              return new Iterator<>() {
                int position;

                @Override
                public boolean hasNext() {
                  open();
                  return position < size;
                }

                @Override
                public Map.Entry<Integer, CertifiedInterval> next() {
                  if (!hasNext()) throw new NoSuchElementException();
                  int row = position++;
                  return read(parts.get(row / PAGE_ROWS)).entry(row % PAGE_ROWS);
                }
              };
            }
          });
    }
  }

  @Override
  public void close() {
    if (closed) return;
    closed = true;
    pages.clear();
    cache.clear();
    bytes = 0;
    rows = 0;
    cachedRows = 0;
    descriptors = 0;
    pageCount = 0;
    backing.close();
  }
}
