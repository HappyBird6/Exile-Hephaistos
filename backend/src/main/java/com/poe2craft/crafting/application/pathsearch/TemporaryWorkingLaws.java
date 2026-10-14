package com.poe2craft.crafting.application.pathsearch;

import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.Execution;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.function.*;

/** Job-owned scratch laws. Durable recovery comes from the separately committed job cursor. */
final class TemporaryWorkingLaws implements AutoCloseable {
  static final String VERSION = "ordered-working-laws-sqlite-v1";
  private final Path directory;
  private final int bits, cacheLimit;
  private final LinkedHashMap<Key, CertifiedInterval> cache = new LinkedHashMap<>(16, .75f, true);
  private final Set<Law<?>> laws = Collections.newSetFromMap(new IdentityHashMap<>());

  private record Key(long law, long key) {}

  private Connection connection;
  private PreparedStatement get, put, remove, next, clear;
  private Path path;
  private long nextLaw, rows, reads, writes;
  private boolean closed;

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
