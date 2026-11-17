package com.poe2craft.craftingsupport.search.application;

import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.CertifiedWorkingLaw;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.Execution;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.function.*;

/**
 * 계산 중 임시 분포를 관리한다. 영속 복구 기준은 별도 커밋된 커서다.
 *
 * <p>기존 계약: Job-owned scratch laws. Durable recovery comes from the separately committed job
 * cursor.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "ordered-working-laws-sqlite-v1"} ({@code String}).
 *   <li>{@code directory}: 디렉터리 ({@code Path}).
 *   <li>{@code bits}: 비트수 ({@code int}).
 *   <li>{@code cacheLimit}: 캐시·한도 ({@code int}).
 *   <li>{@code cache}: 캐시 ({@code LinkedHashMap<Key, CertifiedInterval>}).
 *   <li>{@code laws}: 분포목록 ({@code Set<Law<?>>}).
 *   <li>{@code connection}: DB연결 ({@code Connection}).
 *   <li>{@code get}: get ({@code PreparedStatement}).
 *   <li>{@code put}: put ({@code PreparedStatement}).
 *   <li>{@code remove}: remove ({@code PreparedStatement}).
 *   <li>{@code next}: 다음 ({@code PreparedStatement}).
 *   <li>{@code clear}: clear ({@code PreparedStatement}).
 *   <li>{@code path}: 경로 ({@code Path}).
 *   <li>{@code nextLaw}: 다음·분포 ({@code long}).
 *   <li>{@code rows}: 행목록 ({@code long}).
 *   <li>{@code reads}: 읽기횟수 ({@code long}).
 *   <li>{@code writes}: 쓰기횟수 ({@code long}).
 *   <li>{@code closed}: 닫힘 ({@code boolean}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code TemporaryWorkingLaws(directory, bits, cacheLimit)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       directory}(디렉터리), {@code bits}(비트수), {@code cacheLimit}(캐시·한도). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code open()}: 조회할 자원 또는 작업 범위를 연다. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code boundary()}: 경계 값을 조회·구성한다. 반환: {@code CertifiedWorkingLaw<Integer>}.
 *   <li>{@code pending()}: 대기 값을 조회·구성한다. 반환: {@code CertifiedWorkingLaw<Execution<Integer>>}.
 *   <li>{@code create(encode, decode)}: 새 작업 또는 값을 생성한다. 입력: {@code encode}(encode), {@code
 *       decode}(decode). 반환: {@code Law<K>}. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code residentEntries()}: 메모리상주·항목목록 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code metrics()}: 측정값 값을 조회·구성한다. 반환: {@code Metrics}. 사용하는 예외: {@code
 *       UncheckedIOException}.
 *   <li>{@code retain(key, value)}: retain 처리에서 {@code cache.put}, {@code cache.size}을(를) 사용한다. 입력:
 *       {@code key}(키), {@code value}(값).
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다. 사용하는 예외: {@code IllegalStateException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Key}: {@code TemporaryWorkingLaws}에서 사용하는 키의 불변 값이다.
 *   <li>{@code Metrics}: {@code TemporaryWorkingLaws}에서 사용하는 측정값의 불변 값이다.
 *   <li>{@code Law}: {@code TemporaryWorkingLaws} 내부의 분포 처리를 맡는다.
 * </ul>
 */
final class TemporaryWorkingLaws implements AutoCloseable {
  static final String VERSION = "ordered-working-laws-sqlite-v1";
  private final Path directory;
  private final int bits, cacheLimit;
  private final LinkedHashMap<Key, CertifiedInterval> cache = new LinkedHashMap<>(16, .75f, true);
  private final Set<Law<?>> laws = Collections.newSetFromMap(new IdentityHashMap<>());

  /**
   * {@code TemporaryWorkingLaws}에서 사용하는 키의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code law}: 분포 ({@code long}).
   *   <li>{@code key}: 키 ({@code long}).
   * </ul>
   */
  private record Key(long law, long key) {}

  private Connection connection;
  private PreparedStatement get, put, remove, next, clear;
  private Path path;
  private long nextLaw, rows, reads, writes;
  private boolean closed;

  /**
   * {@code TemporaryWorkingLaws}에서 사용하는 측정값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code rows}: 행목록 ({@code long}).
   *   <li>{@code laws}: 분포목록 ({@code int}).
   *   <li>{@code cachedRows}: 캐시된·행목록 ({@code int}).
   *   <li>{@code diskBytes}: 디스크·바이트수 ({@code long}).
   *   <li>{@code reads}: 읽기횟수 ({@code long}).
   *   <li>{@code writes}: 쓰기횟수 ({@code long}).
   *   <li>{@code sqliteCacheKiB}: sqlite·캐시·Ki·B ({@code int}).
   * </ul>
   */
  record Metrics(
      long rows,
      int laws,
      int cachedRows,
      long diskBytes,
      long reads,
      long writes,
      int sqliteCacheKiB) {}

  TemporaryWorkingLaws(Path directory, int bits, int cacheLimit) {
    new CertifiedInterval.Precision(bits, 0);
    if (cacheLimit < 0) throw new IllegalArgumentException("Nonnegative cache required");
    this.directory = directory;
    this.bits = bits;
    this.cacheLimit = cacheLimit;
  }

  private void open() {
    if (closed) throw new IllegalStateException("Working law store closed");
    if (connection != null) return;
    try {
      Files.createDirectories(directory);
      path = Files.createTempFile(directory, "hephaistos-working-laws-", ".sqlite");
      connection = DriverManager.getConnection("jdbc:sqlite:" + path);
      try (var statement = connection.createStatement()) {
        statement.execute("PRAGMA cache_size=-1024");
        statement.execute("PRAGMA mmap_size=0");
        statement.execute("PRAGMA temp_store=FILE");
        // This file is disposable working memory, never the committed recovery checkpoint.
        statement.execute("PRAGMA journal_mode=OFF");
        statement.execute("PRAGMA synchronous=OFF");
        statement.execute(
            "CREATE TABLE law_rows(law INTEGER NOT NULL,k INTEGER NOT NULL,ordinal INTEGER NOT NULL,lo BLOB NOT NULL,hi BLOB NOT NULL,PRIMARY KEY(law,k)) WITHOUT ROWID");
        statement.execute("CREATE UNIQUE INDEX law_order ON law_rows(law,ordinal)");
      }
      connection.setAutoCommit(false);
      get = connection.prepareStatement("SELECT lo,hi FROM law_rows WHERE law=? AND k=?");
      put =
          connection.prepareStatement(
              "INSERT INTO law_rows VALUES(?,?,?,?,?) ON CONFLICT(law,k) DO UPDATE SET lo=excluded.lo,hi=excluded.hi");
      remove = connection.prepareStatement("DELETE FROM law_rows WHERE law=? AND k=?");
      next =
          connection.prepareStatement(
              "SELECT k,ordinal,lo,hi FROM law_rows WHERE law=? AND ordinal>? ORDER BY ordinal LIMIT 1");
      clear = connection.prepareStatement("DELETE FROM law_rows WHERE law=?");
    } catch (SQLException | IOException failure) {
      close();
      throw new IllegalStateException("Cannot open working laws", failure);
    }
  }

  CertifiedWorkingLaw<Integer> boundary() {
    return create(Integer::longValue, Math::toIntExact);
  }

  CertifiedWorkingLaw<Execution<Integer>> pending() {
    return create(
        e -> ((long) e.phase() << 32) | Integer.toUnsignedLong(e.state()),
        key -> new Execution<>((int) (long) key, (int) (key >>> 32)));
  }

  private <K> Law<K> create(ToLongFunction<K> encode, LongFunction<K> decode) {
    if (closed) throw new IllegalStateException("Working law store closed");
    var law = new Law<K>(++nextLaw, encode, decode);
    laws.add(law);
    return law;
  }

  long residentEntries() {
    return cache.size() + (long) laws.size();
  }

  Metrics metrics() {
    try {
      return new Metrics(
          rows,
          laws.size(),
          cache.size(),
          path == null ? 0 : Files.size(path),
          reads,
          writes,
          1024);
    } catch (IOException failure) {
      throw new UncheckedIOException(failure);
    }
  }

  private void retain(Key key, CertifiedInterval value) {
    if (cacheLimit == 0) return;
    cache.put(key, value);
    while (cache.size() > cacheLimit) cache.pollFirstEntry();
  }

  /**
   * {@code TemporaryWorkingLaws} 내부의 분포 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code long}).
   *   <li>{@code encode}: encode ({@code ToLongFunction<K>}).
   *   <li>{@code decode}: decode ({@code LongFunction<K>}).
   *   <li>{@code size}: 크기 ({@code int}).
   *   <li>{@code ordinal}: ordinal ({@code long}).
   *   <li>{@code released}: released ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Law(id, encode, decode)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code id}(식별자), {@code
   *       encode}(encode), {@code decode}(decode).
   *   <li>{@code accessible()}: accessible의 입력에 따른 분기·결과를 처리한다. 사용하는 예외: {@code
   *       IllegalStateException}.
   *   <li>{@code size()}: 보유 항목의 개수를 조회한다. 반환: {@code int}.
   *   <li>{@code residentEntries()}: {@code 0} 값을 조회한다. 반환: {@code long}.
   *   <li>{@code encoded(key)}: 압축값 처리에서 {@code encode.applyAsLong}을(를) 사용한다. 입력: {@code key}(키).
   *       반환: {@code long}.
   *   <li>{@code get(key)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code key}(키). 반환: {@code
   *       CertifiedInterval}. 사용하는 예외: {@code IllegalStateException}.
   *   <li>{@code put(key, value)}: 키에 해당하는 값을 기록한다. 입력: {@code key}(키), {@code value}(값). 반환:
   *       {@code CertifiedInterval}. 사용하는 예외: {@code IllegalArgumentException}, {@code
   *       IllegalStateException}.
   *   <li>{@code remove(key)}: 지정한 항목을 제거한다. 입력: {@code key}(키). 반환: {@code CertifiedInterval}.
   *       사용하는 예외: {@code IllegalStateException}.
   *   <li>{@code firstEntry()}: 최초·항목 값을 조회·구성한다. 반환: {@code Map.Entry<K, CertifiedInterval>}.
   *   <li>{@code entrySet()}: 항목·집합 값을 조회·구성한다. 반환: {@code Set<Map.Entry<K, CertifiedInterval>>}.
   *       사용하는 예외: {@code IllegalStateException}, {@code NoSuchElementException}.
   *   <li>{@code clear()}: 보유한 항목을 비운다. 사용하는 예외: {@code IllegalStateException}.
   *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
   * </ul>
   */
  private final class Law<K> extends AbstractMap<K, CertifiedInterval>
      implements CertifiedWorkingLaw<K> {
    private final long id;
    private final ToLongFunction<K> encode;
    private final LongFunction<K> decode;
    private int size;
    private long ordinal;
    private boolean released;

    Law(long id, ToLongFunction<K> encode, LongFunction<K> decode) {
      this.id = id;
      this.encode = encode;
      this.decode = decode;
    }

    private void accessible() {
      if (closed || released) throw new IllegalStateException("Working law released");
    }

    public int size() {
      accessible();
      return size;
    }

    public long residentEntries() {
      return 0;
    }

    @SuppressWarnings("unchecked")
    private long encoded(Object key) {
      return encode.applyAsLong((K) Objects.requireNonNull(key));
    }

    public CertifiedInterval get(Object key) {
      accessible();
      long k = encoded(key);
      var cached = cache.get(new Key(id, k));
      if (cached != null) return cached;
      if (size == 0) return null;
      open();
      try {
        get.setLong(1, id);
        get.setLong(2, k);
        reads++;
        try (var result = get.executeQuery()) {
          if (!result.next()) return null;
          var value =
              new CertifiedInterval(
                  bits, new BigInteger(result.getBytes(1)), new BigInteger(result.getBytes(2)));
          retain(new Key(id, k), value);
          return value;
        }
      } catch (SQLException failure) {
        throw new IllegalStateException("Cannot read working law", failure);
      }
    }

    public CertifiedInterval put(K key, CertifiedInterval value) {
      accessible();
      Objects.requireNonNull(value);
      if (value.fractionalBits() != bits
          || value.upperUnits().compareTo(BigInteger.ONE.shiftLeft(bits)) > 0)
        throw new IllegalArgumentException("Matching probability required");
      long k = encoded(key);
      var before = get(key);
      open();
      long position = Math.incrementExact(ordinal);
      try {
        put.setLong(1, id);
        put.setLong(2, k);
        put.setLong(3, position);
        put.setBytes(4, value.lowerUnits().toByteArray());
        put.setBytes(5, value.upperUnits().toByteArray());
        put.executeUpdate();
        writes++;
        ordinal = position;
        if (before == null) {
          size++;
          rows++;
        }
        retain(new Key(id, k), value);
        return before;
      } catch (SQLException failure) {
        throw new IllegalStateException("Cannot write working law", failure);
      }
    }

    public CertifiedInterval remove(Object key) {
      accessible();
      var before = get(key);
      if (before == null) return null;
      long k = encoded(key);
      try {
        remove.setLong(1, id);
        remove.setLong(2, k);
        remove.executeUpdate();
        writes++;
        cache.remove(new Key(id, k));
        size--;
        rows--;
        return before;
      } catch (SQLException failure) {
        throw new IllegalStateException("Cannot remove working row", failure);
      }
    }

    public Map.Entry<K, CertifiedInterval> firstEntry() {
      var iterator = entrySet().iterator();
      return iterator.hasNext() ? iterator.next() : null;
    }

    public Set<Map.Entry<K, CertifiedInterval>> entrySet() {
      accessible();
      return new AbstractSet<>() {
        public int size() {
          return Law.this.size();
        }

        public Iterator<Map.Entry<K, CertifiedInterval>> iterator() {
          return new Iterator<>() {
            long after;
            Map.Entry<K, CertifiedInterval> found;
            boolean ended;

            public boolean hasNext() {
              accessible();
              if (found != null) return true;
              if (ended || size == 0) return false;
              open();
              try {
                next.setLong(1, id);
                next.setLong(2, after);
                reads++;
                try (var result = next.executeQuery()) {
                  if (!result.next()) {
                    ended = true;
                    return false;
                  }
                  long k = result.getLong(1);
                  after = result.getLong(2);
                  found =
                      Map.entry(
                          decode.apply(k),
                          new CertifiedInterval(
                              bits,
                              new BigInteger(result.getBytes(3)),
                              new BigInteger(result.getBytes(4))));
                  return true;
                }
              } catch (SQLException failure) {
                throw new IllegalStateException("Cannot scan working law", failure);
              }
            }

            public Map.Entry<K, CertifiedInterval> next() {
              if (!hasNext()) throw new NoSuchElementException();
              var value = found;
              found = null;
              return value;
            }
          };
        }
      };
    }

    public void clear() {
      accessible();
      if (size == 0) return;
      try {
        clear.setLong(1, id);
        clear.executeUpdate();
        writes++;
        cache.keySet().removeIf(key -> key.law() == id);
        rows -= size;
        size = 0;
        ordinal = 0;
      } catch (SQLException failure) {
        throw new IllegalStateException("Cannot clear working law", failure);
      }
    }

    public void close() {
      if (released || closed) return;
      clear();
      released = true;
      laws.remove(this);
    }
  }

  public void close() {
    if (closed) return;
    closed = true;
    cache.clear();
    laws.clear();
    rows = 0;
    try {
      if (connection != null) connection.close();
      if (path != null) Files.deleteIfExists(path);
    } catch (SQLException | IOException failure) {
      throw new IllegalStateException("Cannot close working laws", failure);
    }
  }
}
