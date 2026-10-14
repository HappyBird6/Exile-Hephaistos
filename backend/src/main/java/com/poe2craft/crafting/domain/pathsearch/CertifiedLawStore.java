package com.poe2craft.crafting.domain.pathsearch;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.zip.*;

/** Lossless class-law pages. Interval objects are materialized only in a bounded read cache. */
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

  /** Job-owned immutable payload storage; returned byte arrays are borrowed read-only views. */
  public interface PackedPages extends AutoCloseable {
    interface Payload {
      byte[] read();
    }

    Payload store(byte[] bytes);

    long residentBytes();

    long diskBytes();

    @Override
    void close();
  }

  private static final class MemoryPages implements PackedPages {
    private final Map<Payload, byte[]> data = new IdentityHashMap<>();
    private long bytes;

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

  private record Page(PackedPages.Payload bytes, int first, int last, int size) {}

  private record Decoded(int[] keys, CertifiedInterval[] values) {
    Map.Entry<Integer, CertifiedInterval> entry(int index) {
      return Map.entry(keys[index], values[index]);
    }
  }

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

  /** One bounded page per step; the transient source is released when the descriptor is ready. */
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
