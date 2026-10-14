package com.poe2craft.crafting.domain.pathsearch;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.zip.*;

/** Job-local, lossless mutable absorption laws with a shared bounded decoded-page cache. */
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

  private static final class Page {
    byte[] packed;
    int size;
  }

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
