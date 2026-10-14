package com.poe2craft.crafting.application.pathsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.pathsearch.BasicClassIndex;
import com.poe2craft.crafting.domain.pathsearch.BasicTransitionClasses;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.function.ToIntFunction;

/** Exact class interning with a bounded witness cache; hashes only select candidates. */
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
