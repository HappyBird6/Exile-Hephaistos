package com.poe2craft.crafting.domain.pathsearch;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.zip.*;

/** Lossless class-law pages. Interval objects are materialized only in a bounded read cache. */
public final class CertifiedLawStore implements AutoCloseable {
  public static final String VERSION = "certified-law-pages-v1";
  private static final int PAGE_ROWS = 256;
  private final int bits;
  private final Map<Bytes, Page> pages = new HashMap<>();
  private final LinkedHashMap<Page, Map<Integer, CertifiedInterval>> cache =
      new LinkedHashMap<>(16, .75f, true);
  private long bytes, rows, reused;
  private int cachedRows;
  private long descriptors;
  private boolean closed;

  public CertifiedLawStore(int bits) {
    new CertifiedInterval.Precision(bits, 0);
    this.bits = bits;
  }

  private static final class Bytes {
    byte[] data;
    final int hash;

    Bytes(byte[] data) {
      this.data = data;
      hash = Arrays.hashCode(data);
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Bytes b && Arrays.equals(data, b.data);
    }
  }

  private record Page(Bytes bytes, int first, int last, int size) {}

  public record Metrics(
      long storedRows,
      long packedBytes,
      int uniquePages,
      int materializedRows,
      long reusedPages,
      long descriptors) {}

  public Metrics metrics() {
    return new Metrics(rows, bytes, pages.size(), materializedRows(), reused, descriptors);
  }

  public int materializedRows() {
    return cachedRows;
  }

  public long packedBytes() {
    return bytes;
  }

  public int pageCount() {
    return pages.size();
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

  public Freeze prepare(Map<Integer, CertifiedInterval> input) {
    open();
    return new Freeze(input);
  }

  /** One bounded page per step; the transient source is released when the descriptor is ready. */
  public final class Freeze {
    private Map<Integer, CertifiedInterval> input;
    private int[] keys;
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
      keys = input.keySet().stream().mapToInt(Integer::intValue).toArray();
      Arrays.sort(keys);
    }

    public boolean complete() {
      open();
      return finished != null;
    }

    public void step() {
      open();
      if (complete()) return;
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
      var key = new Bytes(buffer.toByteArray());
      var old = pages.get(key);
      if (old != null) {
        reused++;
        return old;
      }
      var page =
          new Page(key, entries.getFirst().getKey(), entries.getLast().getKey(), entries.size());
      pages.put(key, page);
      bytes += key.data.length;
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

  private Map<Integer, CertifiedInterval> read(Page page) {
    open();
    var found = cache.get(page);
    if (found != null) return found;
    var result = new LinkedHashMap<Integer, CertifiedInterval>();
    try (var data =
        new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(page.bytes.data)))) {
      int count = data.readInt();
      if (count != page.size) throw new IllegalStateException("Corrupt page");
      for (int i = 0; i < count; i++)
        result.put(data.readInt(), new CertifiedInterval(bits, integer(data), integer(data)));
      if (data.read() != -1) throw new IllegalStateException("Trailing page data");
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    var immutable = Collections.unmodifiableMap(result);
    cache.put(page, immutable);
    cachedRows += immutable.size();
    while (cache.size() > 4) cachedRows -= cache.remove(cache.keySet().iterator().next()).size();
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
    if (law instanceof CertifiedLawStore.Law packed) return packed.total;
    return total(law.values(), bits);
  }

  public static CertifiedInterval total(Collection<CertifiedInterval> values, int bits) {
    if (values instanceof CertifiedLawStore.Values packed) return packed.law.total;
    var sum = CertifiedInterval.zero(bits);
    for (var value : values) sum = sum.add(value);
    return sum.probability();
  }

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

  private final class Law extends AbstractMap<Integer, CertifiedInterval> {
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
        else return read(page).get(target);
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
                int page;
                Iterator<Map.Entry<Integer, CertifiedInterval>> current =
                    Collections.emptyIterator();

                @Override
                public boolean hasNext() {
                  open();
                  return current.hasNext() || page < parts.size();
                }

                @Override
                public Map.Entry<Integer, CertifiedInterval> next() {
                  if (!hasNext()) throw new NoSuchElementException();
                  if (!current.hasNext()) current = read(parts.get(page++)).entrySet().iterator();
                  return current.next();
                }
              };
            }
          });
    }
  }

  @Override
  public void close() {
    closed = true;
    pages.values().forEach(page -> page.bytes.data = new byte[0]);
    pages.clear();
    cache.clear();
    bytes = 0;
    rows = 0;
    cachedRows = 0;
    descriptors = 0;
  }
}
