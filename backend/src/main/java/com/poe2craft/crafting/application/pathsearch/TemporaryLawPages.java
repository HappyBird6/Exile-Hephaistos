package com.poe2craft.crafting.application.pathsearch;

import com.poe2craft.crafting.domain.pathsearch.CertifiedLawStore.PackedPages;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Job-owned immutable spill pages. Only the bounded hot cache retains packed byte arrays. */
final class TemporaryLawPages implements PackedPages {
  private final Path directory;
  private final long limit;
  private final LinkedHashMap<Stored, byte[]> cache = new LinkedHashMap<>(16, .75f, true);
  private long resident, disk;
  private Path path;
  private FileChannel file;
  private boolean closed;

  TemporaryLawPages(Path directory, long limit) {
    if (limit < 0) throw new IllegalArgumentException("Nonnegative packed cache limit required");
    this.directory = Objects.requireNonNull(directory);
    this.limit = limit;
  }

  private final class Stored implements Payload {
    private final int length;
    private long offset = -1;

    Stored(int length) {
      this.length = length;
    }

    @Override
    public byte[] read() {
      open();
      var found = cache.get(this);
      if (found != null) return found;
      if (offset < 0 || file == null) throw new IllegalStateException("Missing packed page");
      var data = new byte[length];
      var buffer = ByteBuffer.wrap(data);
      try {
        while (buffer.hasRemaining()) {
          int count = file.read(buffer, offset + buffer.position());
          if (count < 0) throw new EOFException("Truncated packed page");
        }
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
      retain(this, data);
      return data;
    }
  }

  private void open() {
    if (closed) throw new IllegalStateException("Page storage closed");
  }

  @Override
  public Payload store(byte[] data) {
    open();
    var stored = new Stored(data.length);
    retain(stored, data);
    return stored;
  }

  private void retain(Stored stored, byte[] data) {
    cache.put(stored, data);
    resident += data.length;
    while (resident > limit) {
      var oldest = cache.firstEntry();
      spill(oldest.getKey(), oldest.getValue());
      cache.pollFirstEntry();
      resident -= oldest.getValue().length;
    }
  }

  private void spill(Stored stored, byte[] data) {
    if (stored.offset >= 0) return;
    try {
      if (file == null) {
        Files.createDirectories(directory);
        path = Files.createTempFile(directory, "hephaistos-method-laws-", ".pages");
        file = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
      }
      long offset = disk;
      var buffer = ByteBuffer.wrap(data);
      try {
        while (buffer.hasRemaining()) file.write(buffer, offset + buffer.position());
      } catch (IOException ex) {
        try {
          file.truncate(offset);
        } catch (IOException cleanup) {
          ex.addSuppressed(cleanup);
        }
        throw ex;
      }
      stored.offset = offset;
      disk += data.length;
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public long residentBytes() {
    return resident;
  }

  @Override
  public long diskBytes() {
    return disk;
  }

  @Override
  public void close() {
    if (closed) return;
    closed = true;
    cache.clear();
    resident = 0;
    IOException failure = null;
    try {
      if (file != null) file.close();
    } catch (IOException ex) {
      failure = ex;
    }
    try {
      if (path != null) Files.deleteIfExists(path);
    } catch (IOException ex) {
      if (failure == null) failure = ex;
      else failure.addSuppressed(ex);
    }
    disk = 0;
    if (failure != null) throw new UncheckedIOException(failure);
  }
}
