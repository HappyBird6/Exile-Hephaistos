package com.poe2craft.craftingsupport.search.application;

import com.poe2craft.craftingsupport.search.domain.CertifiedLawStore.PackedPages;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/**
 * 작업 소유의 불변 분포 페이지를 파일에 두고 작은 캐시만 메모리에 유지한다.
 *
 * <p>기존 계약: Job-owned immutable spill pages. Only the bounded hot cache retains packed byte arrays.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code directory}: 디렉터리 ({@code Path}).
 *   <li>{@code limit}: 한도 ({@code long}).
 *   <li>{@code cache}: 캐시 ({@code LinkedHashMap<Stored, byte[]>}).
 *   <li>{@code resident}: 메모리상주 ({@code long}).
 *   <li>{@code disk}: 디스크 ({@code long}).
 *   <li>{@code path}: 경로 ({@code Path}).
 *   <li>{@code file}: 파일 ({@code FileChannel}).
 *   <li>{@code closed}: 닫힘 ({@code boolean}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code TemporaryLawPages(directory, limit)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       directory}(디렉터리), {@code limit}(한도). 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code open()}: 조회할 자원 또는 작업 범위를 연다. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code store(data)}: 저장소의 입력에 따른 분기·결과를 처리한다. 입력: {@code data}(데이터). 반환: {@code Payload}.
 *   <li>{@code retain(stored, data)}: retain 처리에서 {@code cache.put}, {@code cache.firstEntry}을(를)
 *       사용한다. 입력: {@code stored}(저장된), {@code data}(데이터).
 *   <li>{@code spill(stored, data)}: spill 처리에서 {@code Files.createDirectories}, {@code
 *       Files.createTempFile}을(를) 사용한다. 입력: {@code stored}(저장된), {@code data}(데이터). 사용하는 예외: {@code
 *       UncheckedIOException}.
 *   <li>{@code residentBytes()}: {@code resident} 값을 조회한다. 반환: {@code long}.
 *   <li>{@code diskBytes()}: {@code disk} 값을 조회한다. 반환: {@code long}.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다. 사용하는 예외: {@code UncheckedIOException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Stored}: {@code TemporaryLawPages} 내부의 저장된 처리를 맡는다.
 * </ul>
 */
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

  /**
   * {@code TemporaryLawPages} 내부의 저장된 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code length}: 길이 ({@code int}).
   *   <li>{@code offset}: 시작위치; 초기값 {@code -1} ({@code long}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Stored(length)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code length}(길이).
   *   <li>{@code read()}: 지정한 입력 또는 저장된 값을 읽는다. 반환: {@code byte[]}. 사용하는 예외: {@code
   *       IllegalStateException}, {@code EOFException}, {@code UncheckedIOException}.
   * </ul>
   */
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
