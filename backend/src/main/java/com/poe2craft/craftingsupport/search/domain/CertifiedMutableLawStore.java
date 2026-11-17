package com.poe2craft.craftingsupport.search.domain;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.zip.*;

/**
 * 작업별 가변 흡수 분포를 저장하고 제한된 해독 페이지 캐시를 공유한다.
 *
 * <p>기존 계약: Job-local, lossless mutable absorption laws with a shared bounded decoded-page cache.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "mutable-certified-law-pages-v1"} ({@code String}).
 *   <li>{@code bits}: 비트수 ({@code int}).
 *   <li>{@code cacheLimit}: 캐시·한도 ({@code int}).
 *   <li>{@code oneUnits}: 일·단위목록 ({@code BigInteger}).
 *   <li>{@code laws}: 분포목록 ({@code Set<Law>}).
 *   <li>{@code cache}: 캐시 ({@code LinkedHashMap<Page, CertifiedInterval[]>}).
 *   <li>{@code rows}: 행목록 ({@code long}).
 *   <li>{@code bytes}: 바이트수 ({@code long}).
 *   <li>{@code evictions}: 캐시제거수 ({@code long}).
 *   <li>{@code pages}: 페이지목록 ({@code int}).
 *   <li>{@code decodedRows}: 복원값·행목록 ({@code int}).
 *   <li>{@code closed}: 닫힘 ({@code boolean}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CertifiedMutableLawStore(bits, cacheLimit)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       bits}(비트수), {@code cacheLimit}(캐시·한도). 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code open()}: 조회할 자원 또는 작업 범위를 연다. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code metrics()}: 측정값 값을 조회·구성한다. 반환: {@code Metrics}.
 *   <li>{@code residentEntries()}: 메모리상주·항목목록 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code packedBytes()}: {@code bytes} 값을 조회한다. 반환: {@code long}.
 *   <li>{@code create()}: 새 작업 또는 값을 생성한다. 반환: {@code Law}.
 *   <li>{@code decoded(page)}: 복원값 처리에서 {@code cache.get}, {@code data.readUnsignedShort}을(를) 사용한다.
 *       입력: {@code page}(페이지). 반환: {@code CertifiedInterval[]}. 사용하는 예외: {@code IOException},
 *       {@code UncheckedIOException}.
 *   <li>{@code pack(page, values)}: pack 처리에서 {@code data.writeShort}, {@code data.writeByte}을(를)
 *       사용한다. 입력: {@code page}(페이지), {@code values}(값목록). 사용하는 예외: {@code UncheckedIOException}.
 *   <li>{@code integer(data, value)}: 정수 처리에서 {@code value.toByteArray}, {@code data.writeByte}을(를)
 *       사용한다. 입력: {@code data}(데이터), {@code value}(값). 사용하는 예외: {@code IOException}.
 *   <li>{@code integer(data)}: 정수 처리에서 {@code data.readUnsignedByte}, {@code data.readNBytes}을(를)
 *       사용한다. 입력: {@code data}(데이터). 반환: {@code BigInteger}. 사용하는 예외: {@code IOException}, {@code
 *       EOFException}.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Page}: {@code CertifiedMutableLawStore} 내부의 페이지 처리를 맡는다.
 *   <li>{@code Metrics}: {@code CertifiedMutableLawStore}에서 사용하는 측정값의 불변 값이다.
 *   <li>{@code Law}: {@code CertifiedMutableLawStore} 내부의 분포 처리를 맡는다.
 * </ul>
 */
public final class CertifiedMutableLawStore implements AutoCloseable {
  public static final String VERSION = "mutable-certified-law-pages-v1";
  private final int bits, cacheLimit;
  private final BigInteger oneUnits;
  private final Set<Law> laws = Collections.newSetFromMap(new IdentityHashMap<>());
  private final LinkedHashMap<Page, CertifiedInterval[]> cache =
      new LinkedHashMap<>(16, .75f, true);
  private long rows, bytes, evictions;
  private int pages, decodedRows;
  private boolean closed;

  /**
   * {@code CertifiedMutableLawStore} 내부의 페이지 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code packed}: 압축된 ({@code byte[]}).
   *   <li>{@code size}: 크기 ({@code int}).
   * </ul>
   */
  private static final class Page {
    byte[] packed;
    int size;
  }

  /**
   * {@code CertifiedMutableLawStore}에서 사용하는 측정값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code rows}: 행목록 ({@code long}).
   *   <li>{@code pages}: 페이지목록 ({@code int}).
   *   <li>{@code laws}: 분포목록 ({@code int}).
   *   <li>{@code decodedRows}: 복원값·행목록 ({@code int}).
   *   <li>{@code packedBytes}: 압축된·바이트수 ({@code long}).
   *   <li>{@code evictions}: 캐시제거수 ({@code long}).
   * </ul>
   */
  public record Metrics(
      long rows, int pages, int laws, int decodedRows, long packedBytes, long evictions) {}

  public CertifiedMutableLawStore(int bits, int cacheLimit) {
    new CertifiedInterval.Precision(bits, 0);
    if (cacheLimit < 1) throw new IllegalArgumentException("Positive page cache required");
    this.bits = bits;
    oneUnits = BigInteger.ONE.shiftLeft(bits);
    this.cacheLimit = cacheLimit;
  }

  private void open() {
    if (closed) throw new IllegalStateException("Mutable law store closed");
  }

  public Metrics metrics() {
    return new Metrics(rows, pages, laws.size(), decodedRows, bytes, evictions);
  }

  public long residentEntries() {
    return (long) decodedRows + pages + laws.size();
  }

  public long packedBytes() {
    return bytes;
  }

  public Law create() {
    open();
    var law = new Law();
    laws.add(law);
    return law;
  }

  private CertifiedInterval[] decoded(Page page) {
    open();
    var found = cache.get(page);
    if (found != null) return found;
    var values = new CertifiedInterval[256];
    if (page.packed != null) {
      try (var data =
          new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(page.packed)))) {
        int count = data.readUnsignedShort();
        if (count != page.size) throw new IOException("Invalid mutable law size");
        int previous = -1;
        for (int i = 0; i < count; i++) {
          int offset = data.readUnsignedByte();
          if (offset <= previous) throw new IOException("Unordered mutable law");
          previous = offset;
          values[offset] = new CertifiedInterval(bits, integer(data), integer(data));
        }
        if (data.read() != -1) throw new IOException("Trailing mutable law bytes");
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }
    cache.put(page, values);
    decodedRows += page.size;
    while (cache.size() > cacheLimit) {
      var removed = cache.pollFirstEntry();
      pack(removed.getKey(), removed.getValue());
      decodedRows -= removed.getKey().size;
      evictions++;
    }
    return values;
  }

  private void pack(Page page, CertifiedInterval[] values) {
    if (page.packed != null) return;
    var buffer = new ByteArrayOutputStream();
    var compressor = new Deflater(Deflater.BEST_SPEED);
    try (var data = new DataOutputStream(new DeflaterOutputStream(buffer, compressor))) {
      data.writeShort(page.size);
      for (int i = 0; i < values.length; i++)
        if (values[i] != null) {
          data.writeByte(i);
          integer(data, values[i].lowerUnits());
          integer(data, values[i].upperUnits());
        }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    } finally {
      compressor.end();
    }
    page.packed = buffer.toByteArray();
    bytes += page.packed.length;
  }

  private void integer(DataOutputStream data, BigInteger value) throws IOException {
    var encoded = value.toByteArray();
    data.writeByte(encoded.length);
    data.write(encoded);
  }

  private BigInteger integer(DataInputStream data) throws IOException {
    int length = data.readUnsignedByte();
    if (length < 1 || length > (bits + 8) / 8)
      throw new IOException("Invalid mutable law endpoint");
    var encoded = data.readNBytes(length);
    if (encoded.length != length) throw new EOFException();
    return new BigInteger(encoded);
  }

  /**
   * {@code CertifiedMutableLawStore} 내부의 분포 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code parts}: 부분목록 ({@code NavigableMap<Integer, Page>}).
   *   <li>{@code size}: 크기 ({@code int}).
   *   <li>{@code released}: released ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code accessible()}: accessible의 입력에 따른 분기·결과를 처리한다. 사용하는 예외: {@code
   *       IllegalStateException}.
   *   <li>{@code size()}: 보유 항목의 개수를 조회한다. 반환: {@code int}.
   *   <li>{@code get(key)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code key}(키). 반환: {@code
   *       CertifiedInterval}.
   *   <li>{@code put(key, value)}: 키에 해당하는 값을 기록한다. 입력: {@code key}(키), {@code value}(값). 반환:
   *       {@code CertifiedInterval}. 사용하는 예외: {@code IllegalArgumentException}.
   *   <li>{@code entrySet()}: 항목·집합 값을 조회·구성한다. 반환: {@code Set<Map.Entry<Integer,
   *       CertifiedInterval>>}. 사용하는 예외: {@code NoSuchElementException}.
   *   <li>{@code clear()}: 보유한 항목을 비운다.
   *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
   * </ul>
   */
  public final class Law extends AbstractMap<Integer, CertifiedInterval> implements AutoCloseable {
    private final NavigableMap<Integer, Page> parts = new TreeMap<>();
    private int size;
    private boolean released;

    private void accessible() {
      open();
      if (released) throw new IllegalStateException("Mutable law released");
    }

    @Override
    public int size() {
      accessible();
      return size;
    }

    @Override
    public CertifiedInterval get(Object key) {
      accessible();
      if (!(key instanceof Integer id) || id < 0) return null;
      var page = parts.get(id >>> 8);
      return page == null ? null : decoded(page)[id & 255];
    }

    @Override
    public CertifiedInterval put(Integer key, CertifiedInterval value) {
      accessible();
      Objects.requireNonNull(key);
      Objects.requireNonNull(value);
      if (key < 0 || value.fractionalBits() != bits || value.upperUnits().compareTo(oneUnits) > 0)
        throw new IllegalArgumentException("Matching class probability required");
      var page =
          parts.computeIfAbsent(
              key >>> 8,
              ignored -> {
                pages++;
                return new Page();
              });
      var values = decoded(page);
      var before = values[key & 255];
      if (page.packed != null) {
        bytes -= page.packed.length;
        page.packed = null;
      }
      if (before == null) {
        size++;
        page.size++;
        rows++;
        decodedRows++;
      }
      values[key & 255] = value;
      return before;
    }

    @Override
    public Set<Map.Entry<Integer, CertifiedInterval>> entrySet() {
      accessible();
      return new AbstractSet<>() {
        @Override
        public int size() {
          return Law.this.size();
        }

        @Override
        public Iterator<Map.Entry<Integer, CertifiedInterval>> iterator() {
          accessible();
          return new Iterator<>() {
            private final Iterator<Map.Entry<Integer, Page>> source = parts.entrySet().iterator();
            private Map.Entry<Integer, Page> part;
            private int offset;
            private Map.Entry<Integer, CertifiedInterval> next;

            @Override
            public boolean hasNext() {
              accessible();
              if (next != null) return true;
              while (true) {
                if (part != null) {
                  var values = decoded(part.getValue());
                  while (offset < 256) {
                    int current = offset++;
                    if (values[current] != null) {
                      next = Map.entry((part.getKey() << 8) | current, values[current]);
                      return true;
                    }
                  }
                }
                if (!source.hasNext()) return false;
                part = source.next();
                offset = 0;
              }
            }

            @Override
            public Map.Entry<Integer, CertifiedInterval> next() {
              if (!hasNext()) throw new NoSuchElementException();
              var result = next;
              next = null;
              return result;
            }
          };
        }
      };
    }

    @Override
    public void clear() {
      accessible();
      for (var page : parts.values()) {
        if (cache.remove(page) != null) decodedRows -= page.size;
        if (page.packed != null) bytes -= page.packed.length;
        page.packed = null;
        pages--;
      }
      rows -= size;
      size = 0;
      parts.clear();
    }

    @Override
    public void close() {
      if (released || closed) return;
      clear();
      released = true;
      laws.remove(this);
    }
  }

  @Override
  public void close() {
    if (closed) return;
    for (var law : List.copyOf(laws)) law.close();
    cache.clear();
    closed = true;
  }
}
