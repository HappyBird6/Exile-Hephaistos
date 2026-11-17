package com.poe2craft.craftingsupport.search.infrastructure;

import com.poe2craft.craftingsupport.search.application.AutomaticJobRepository;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * 크기가 제한된 자동 탐색 JSON을 로컬 파일에 원자적으로 저장한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code MAX_BYTES}: 바이트수; 초기값 {@code 64L * 1024 * 1024} ({@code long}).
 *   <li>{@code MAX_TOTAL_BYTES}: 바이트수; 초기값 {@code 1024L * 1024 * 1024} ({@code long}).
 *   <li>{@code MAX_DOCUMENTS}: 문서목록; 초기값 {@code 256} ({@code int}).
 *   <li>{@code directory}: 디렉터리 ({@code Path}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code LocalAutomaticJobRepository(directory)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       directory}(디렉터리). 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code path(id)}: 경로 처리에서 {@code UUID.fromString}, {@code directory.resolve}을(를) 사용한다. 입력:
 *       {@code id}(식별자). 반환: {@code Path}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code ids()}: 식별자목록 값을 조회·구성한다. 반환: {@code List<String>}. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code read(id)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code id}(식별자). 반환: {@code byte[]}. 사용하는 예외:
 *       {@code AutomaticJobRepository.BudgetExceeded}, {@code IllegalStateException}.
 *   <li>{@code open(id)}: 조회할 자원 또는 작업 범위를 연다. 입력: {@code id}(식별자). 반환: {@code
 *       java.io.InputStream}. 사용하는 예외: {@code AutomaticJobRepository.BudgetExceeded}, {@code
 *       IllegalStateException}.
 *   <li>{@code save(id, value)}: 현재 값을 저장 경계에 기록한다. 입력: {@code id}(식별자), {@code value}(값). 사용하는 예외:
 *       {@code AutomaticJobRepository.BudgetExceeded}, {@code IllegalStateException}.
 *   <li>{@code delete(id)}: 삭제 처리에서 {@code Files.deleteIfExists}을(를) 사용한다. 입력: {@code id}(식별자).
 *       사용하는 예외: {@code IllegalStateException}.
 * </ul>
 */
public final class LocalAutomaticJobRepository implements AutomaticJobRepository {
  private static final long MAX_BYTES = 64L * 1024 * 1024;
  private static final long MAX_TOTAL_BYTES = 1024L * 1024 * 1024;
  private static final int MAX_DOCUMENTS = 256;
  private final Path directory;

  public LocalAutomaticJobRepository(Path directory) {
    this.directory = directory.toAbsolutePath().normalize();
    try {
      Files.createDirectories(this.directory);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create automatic job storage", e);
    }
  }

  private Path path(String id) {
    if (!UUID.fromString(id).toString().equals(id))
      throw new IllegalArgumentException("Invalid job id");
    return directory.resolve(id + ".json");
  }

  public List<String> ids() {
    try (var files = Files.list(directory)) {
      return files
          .map(p -> p.getFileName().toString())
          .filter(n -> n.matches("[0-9a-f-]{36}\\.json"))
          .map(n -> n.substring(0, 36))
          .sorted()
          .toList();
    } catch (IOException e) {
      throw new IllegalStateException("Cannot list automatic jobs", e);
    }
  }

  public byte[] read(String id) {
    try {
      var path = path(id);
      if (Files.size(path) > MAX_BYTES) throw new AutomaticJobRepository.BudgetExceeded();
      return Files.readAllBytes(path);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read automatic job", e);
    }
  }

  @Override
  public java.io.InputStream open(String id) {
    try {
      var target = path(id);
      if (Files.size(target) > MAX_BYTES) throw new AutomaticJobRepository.BudgetExceeded();
      return Files.newInputStream(target);
    } catch (IOException error) {
      throw new IllegalStateException("Cannot open automatic job", error);
    }
  }

  public synchronized void save(String id, byte[] value) {
    if (value.length > MAX_BYTES) throw new AutomaticJobRepository.BudgetExceeded();
    var target = path(id);
    var temporary = directory.resolve(id + ".pending");
    try {
      var ids = ids();
      if (!Files.exists(target) && ids.size() >= MAX_DOCUMENTS)
        throw new AutomaticJobRepository.BudgetExceeded();
      long total = value.length;
      for (var existing : ids) {
        if (!existing.equals(id)) total += Files.size(path(existing));
      }
      if (total > MAX_TOTAL_BYTES) throw new AutomaticJobRepository.BudgetExceeded();
      Files.write(temporary, value);
      try {
        Files.move(
            temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Cannot publish automatic job", e);
    }
  }

  public void delete(String id) {
    try {
      Files.deleteIfExists(path(id));
    } catch (IOException e) {
      throw new IllegalStateException("Cannot expire automatic job", e);
    }
  }
}
