package com.poe2craft.craftingsupport.search.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.search.domain.BasicClassIndex;
import com.poe2craft.craftingsupport.search.domain.BasicTransitionClasses;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.function.ToIntFunction;

/**
 * 동치류를 정확히 식별하고 자주 사용하는 증명 상태만 메모리에 유지한다.
 *
 * <p>기존 계약: Exact class interning with a bounded witness cache; hashes only select candidates.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code directory}: 디렉터리 ({@code Path}).
 *   <li>{@code classes}: 동치류목록 ({@code BasicTransitionClasses}).
 *   <li>{@code limit}: 한도 ({@code int}).
 *   <li>{@code hash}: 해시 ({@code ToIntFunction<BasicTransitionClasses.Key>}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code cache}: 캐시 ({@code LinkedHashMap<Integer, BasicClassIndex.Entry>}).
 *   <li>{@code hot}: hot ({@code Map<BasicTransitionClasses.Key, Integer>}).
 *   <li>{@code connection}: DB연결 ({@code Connection}).
 *   <li>{@code find}: find ({@code PreparedStatement}).
 *   <li>{@code get}: get ({@code PreparedStatement}).
 *   <li>{@code add}: add ({@code PreparedStatement}).
 *   <li>{@code path}: 경로 ({@code Path}).
 *   <li>{@code size}: 크기 ({@code int}).
 *   <li>{@code reads}: 읽기횟수 ({@code long}).
 *   <li>{@code writes}: 쓰기횟수 ({@code long}).
 *   <li>{@code hits}: 도달목록 ({@code long}).
 *   <li>{@code closed}: 닫힘 ({@code boolean}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code TemporaryClassIndex(directory, classes, limit)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
 *       directory}(디렉터리), {@code classes}(동치류목록), {@code limit}(한도).
 *   <li>{@code TemporaryClassIndex(directory, classes, limit, hash)}: 전달받은 값으로 구성원을 초기화한다. 입력:
 *       {@code directory}(디렉터리), {@code classes}(동치류목록), {@code limit}(한도), {@code hash}(해시). 사용하는
 *       예외: {@code IllegalArgumentException}.
 *   <li>{@code accessible()}: accessible의 입력에 따른 분기·결과를 처리한다. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code open()}: 조회할 자원 또는 작업 범위를 연다. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code retain(id, entry)}: retain 처리에서 {@code cache.put}, {@code hot.put}을(를) 사용한다. 입력:
 *       {@code id}(식별자), {@code entry}(항목).
 *   <li>{@code find(key)}: 조건에 맞는 항목을 찾는다. 입력: {@code key}(키). 반환: {@code Integer}. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code add(key, witness)}: 값 또는 항목을 더한다. 입력: {@code key}(키), {@code witness}(증명상태). 반환:
 *       {@code int}. 사용하는 예외: {@code IllegalArgumentException}, {@code IllegalStateException}.
 *   <li>{@code entry(id)}: 항목 처리에서 {@code cache.get}, {@code get.setInt}을(를) 사용한다. 입력: {@code
 *       id}(식별자). 반환: {@code BasicClassIndex.Entry}. 사용하는 예외: {@code IllegalArgumentException},
 *       {@code IllegalStateException}.
 *   <li>{@code size()}: 보유 항목의 개수를 조회한다. 반환: {@code int}.
 *   <li>{@code residentEntries()}: 메모리상주·항목목록 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code metrics()}: 측정값 값을 조회·구성한다. 반환: {@code Metrics}. 사용하는 예외: {@code
 *       UncheckedIOException}.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다. 사용하는 예외: {@code IllegalStateException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Metrics}: {@code TemporaryClassIndex}에서 사용하는 측정값의 불변 값이다.
 * </ul>
 */
final class TemporaryClassIndex implements BasicClassIndex.Storage {
  private final Path directory;
  private final BasicTransitionClasses classes;
  private final int limit;
  private final ToIntFunction<BasicTransitionClasses.Key> hash;
  private final ObjectMapper json = new ObjectMapper();
  private final LinkedHashMap<Integer, BasicClassIndex.Entry> cache =
      new LinkedHashMap<>(16, .75f, true);
  private final Map<BasicTransitionClasses.Key, Integer> hot = new HashMap<>();
  private Connection connection;
  private PreparedStatement find, get, add;
  private Path path;
  private int size;
  private long reads, writes, hits;
  private boolean closed;

  /**
   * {@code TemporaryClassIndex}에서 사용하는 측정값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code rows}: 행목록 ({@code int}).
   *   <li>{@code cachedRows}: 캐시된·행목록 ({@code int}).
   *   <li>{@code diskBytes}: 디스크·바이트수 ({@code long}).
   *   <li>{@code reads}: 읽기횟수 ({@code long}).
   *   <li>{@code writes}: 쓰기횟수 ({@code long}).
   *   <li>{@code hits}: 도달목록 ({@code long}).
   *   <li>{@code sqliteCacheKiB}: sqlite·캐시·Ki·B ({@code int}).
   * </ul>
   */
  record Metrics(
      int rows,
      int cachedRows,
      long diskBytes,
      long reads,
      long writes,
      long hits,
      int sqliteCacheKiB) {}

  TemporaryClassIndex(Path directory, BasicTransitionClasses classes, int limit) {
    this(directory, classes, limit, BasicTransitionClasses.Key::hashCode);
  }

  TemporaryClassIndex(
      Path directory,
      BasicTransitionClasses classes,
      int limit,
      ToIntFunction<BasicTransitionClasses.Key> hash) {
    if (limit < 0) throw new IllegalArgumentException("Nonnegative cache required");
    this.directory = directory;
    this.classes = classes;
    this.limit = limit;
    this.hash = hash;
  }

  private void accessible() {
    if (closed) throw new IllegalStateException("Class storage closed");
  }

  private void open() {
    accessible();
    if (connection != null) return;
    try {
      Files.createDirectories(directory);
      path = Files.createTempFile(directory, "hephaistos-class-index-", ".sqlite");
      connection = DriverManager.getConnection("jdbc:sqlite:" + path);
      try (var statement = connection.createStatement()) {
        statement.execute("PRAGMA cache_size=-1024");
        statement.execute("PRAGMA mmap_size=0");
        statement.execute("PRAGMA temp_store=FILE");
        statement.execute("PRAGMA journal_mode=OFF");
        statement.execute("PRAGMA synchronous=OFF");
        statement.execute(
            "CREATE TABLE classes(id INTEGER PRIMARY KEY,hash INTEGER NOT NULL,witness TEXT NOT NULL)");
        statement.execute("CREATE INDEX class_hash ON classes(hash,id)");
      }
      connection.setAutoCommit(false);
      find = connection.prepareStatement("SELECT id FROM classes WHERE hash=? ORDER BY id");
      get = connection.prepareStatement("SELECT witness FROM classes WHERE id=?");
      add = connection.prepareStatement("INSERT INTO classes VALUES(?,?,?)");
    } catch (SQLException | IOException failure) {
      close();
      throw new IllegalStateException("Cannot open class storage", failure);
    }
  }

  private void retain(int id, BasicClassIndex.Entry entry) {
    if (limit == 0) return;
    cache.put(id, entry);
    hot.put(entry.key(), id);
    while (cache.size() > limit) {
      var removed = cache.pollFirstEntry();
      hot.remove(removed.getValue().key());
    }
  }

  public Integer find(BasicTransitionClasses.Key key) {
    accessible();
    var cached = hot.get(key);
    if (cached != null) {
      cache.get(cached);
      hits++;
      return cached;
    }
    if (size == 0) return null;
    try {
      find.setInt(1, hash.applyAsInt(key));
      reads++;
      try (var rows = find.executeQuery()) {
        while (rows.next()) {
          int id = rows.getInt(1);
          if (entry(id).key().equals(key)) return id;
        }
      }
      return null;
    } catch (SQLException failure) {
      throw new IllegalStateException("Cannot find class", failure);
    }
  }

  public int add(BasicTransitionClasses.Key key, BasicCurrencyState witness) {
    accessible();
    if (!classes.key(witness).equals(key))
      throw new IllegalArgumentException("Witness does not match class");
    open();
    int id = size;
    int count = Math.incrementExact(size);
    try {
      add.setInt(1, id);
      add.setInt(2, hash.applyAsInt(key));
      add.setString(3, json.writeValueAsString(witness));
      add.executeUpdate();
      writes++;
      size = count;
      retain(id, new BasicClassIndex.Entry(key, witness));
      return id;
    } catch (SQLException | IOException failure) {
      throw new IllegalStateException("Cannot store class", failure);
    }
  }

  public BasicClassIndex.Entry entry(int id) {
    accessible();
    if (id < 0 || id >= size) throw new IllegalArgumentException("Unknown class");
    var cached = cache.get(id);
    if (cached != null) {
      hits++;
      return cached;
    }
    try {
      get.setInt(1, id);
      reads++;
      try (var row = get.executeQuery()) {
        if (!row.next()) throw new IllegalStateException("Missing class witness");
        var witness = json.readValue(row.getString(1), BasicCurrencyState.class);
        var entry = new BasicClassIndex.Entry(classes.key(witness), witness);
        retain(id, entry);
        return entry;
      }
    } catch (SQLException | IOException failure) {
      throw new IllegalStateException("Cannot read class", failure);
    }
  }

  public int size() {
    accessible();
    return size;
  }

  public long residentEntries() {
    return cache.size();
  }

  Metrics metrics() {
    try {
      return new Metrics(
          size, cache.size(), path == null ? 0 : Files.size(path), reads, writes, hits, 1024);
    } catch (IOException failure) {
      throw new UncheckedIOException(failure);
    }
  }

  public void close() {
    if (closed) return;
    closed = true;
    cache.clear();
    hot.clear();
    try {
      if (connection != null) connection.close();
      if (path != null) Files.deleteIfExists(path);
    } catch (SQLException | IOException failure) {
      throw new IllegalStateException("Cannot close class storage", failure);
    }
  }
}
