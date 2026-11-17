package com.poe2craft.craftingsupport.search.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.search.application.CertifiedJobRepository;
import com.poe2craft.craftingsupport.search.application.MethodGraphStore;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * 작업별 DB와 공개 입력 파일을 설정된 로컬 디렉터리에 보관한다.
 *
 * <p>기존 계약: A job's database and public-input descriptor stay in the configured local directory.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code directory}: 디렉터리 ({@code Path}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code LocalCertifiedJobRepository(directory, json)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       directory}(디렉터리), {@code json}(JSON). 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code path(id, suffix)}: 경로 처리에서 {@code UUID.fromString}, {@code directory.resolve}을(를)
 *       사용한다. 입력: {@code id}(식별자), {@code suffix}(접미). 반환: {@code Path}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code create(descriptor)}: 새 작업 또는 값을 생성한다. 입력: {@code descriptor}(기술정보). 반환: {@code
 *       MethodGraphStore}. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code open(descriptor)}: 조회할 자원 또는 작업 범위를 연다. 입력: {@code descriptor}(기술정보). 반환: {@code
 *       MethodGraphStore}.
 *   <li>{@code list()}: 조건에 맞는 목록을 조회한다. 반환: {@code List<Descriptor>}. 사용하는 예외: {@code
 *       IllegalStateException}.
 * </ul>
 */
public final class LocalCertifiedJobRepository implements CertifiedJobRepository {
  private final Path directory;
  private final ObjectMapper json;

  public LocalCertifiedJobRepository(Path directory, ObjectMapper json) {
    this.directory = directory.toAbsolutePath().normalize();
    this.json = json.copy();
    try {
      Files.createDirectories(this.directory);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create job directory", e);
    }
  }

  private Path path(String id, String suffix) {
    if (!UUID.fromString(id).toString().equals(id))
      throw new IllegalArgumentException("Invalid job ID");
    return directory.resolve(id + suffix);
  }

  public MethodGraphStore create(Descriptor descriptor) {
    var store = open(descriptor);
    try {
      var destination = path(descriptor.id(), ".job.json");
      if (Files.exists(destination))
        throw new IllegalStateException("Job descriptor already exists");
      var pending = path(descriptor.id(), ".job.pending");
      Files.writeString(
          pending, json.writeValueAsString(descriptor), StandardOpenOption.CREATE_NEW);
      Files.move(pending, destination, StandardCopyOption.ATOMIC_MOVE);
      return store;
    } catch (IOException | RuntimeException failure) {
      store.close();
      throw new IllegalStateException("Cannot publish job descriptor", failure);
    }
  }

  public MethodGraphStore open(Descriptor descriptor) {
    return new SqliteMethodGraphStore(
        path(descriptor.id(), ".sqlite"), descriptor.identity(), 1024);
  }

  public List<Descriptor> list() {
    try (var files = Files.list(directory)) {
      var result = new ArrayList<Descriptor>();
      for (var file :
          files.filter(p -> p.getFileName().toString().endsWith(".job.json")).sorted().toList()) {
        try {
          var descriptor = json.readValue(Files.readString(file), Descriptor.class);
          if (!file.equals(path(descriptor.id(), ".job.json")))
            throw new IllegalStateException("Job descriptor filename mismatch");
          result.add(descriptor);
        } catch (IOException | RuntimeException failure) {
          java.util.logging.Logger.getLogger(LocalCertifiedJobRepository.class.getName())
              .warning(
                  "Unreadable local job descriptor retained for recovery: " + file.getFileName());
        }
      }
      return List.copyOf(result);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read job descriptors", e);
    }
  }
}
